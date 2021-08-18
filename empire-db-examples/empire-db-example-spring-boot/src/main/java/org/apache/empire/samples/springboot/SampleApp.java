/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.empire.samples.springboot;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.commons.StringUtils;
import org.apache.empire.data.bean.BeanResult;
import org.apache.empire.db.DBColumnExpr;
import org.apache.empire.db.DBCommand;
import org.apache.empire.db.DBDatabaseDriver;
import org.apache.empire.db.DBReader;
import org.apache.empire.db.DBRecord;
import org.apache.empire.db.DBSQLScript;
import org.apache.empire.db.derby.DBDatabaseDriverDerby;
import org.apache.empire.db.h2.DBDatabaseDriverH2;
import org.apache.empire.db.hsql.DBDatabaseDriverHSql;
import org.apache.empire.db.postgresql.DBDatabaseDriverPostgreSQL;
import org.apache.empire.exceptions.InvalidArgumentException;
import org.apache.empire.samples.springboot.SampleDB.Gender;
import org.apache.empire.xml.XMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;
import org.w3c.dom.Document;

/**
 * Implementing ApplicationRunner interface tells Spring Boot to automatically call the run method AFTER the application context has been loaded.
 */
@SpringBootApplication
public class SampleApp implements ApplicationRunner {

  private static final Logger LOGGER = LoggerFactory.getLogger(SampleApp.class);

  @Autowired
  private EmpireDBConfigProperties empireDBConfigProperties;

  @Autowired
  private DataSource dataSource;

  @Autowired
  private Environment environment;

  @Autowired
  private SampleDB db;

  private enum QueryType {
    Reader,
    BeanList,
    XmlDocument
  }

  public static void main(String[] args) {
    SpringApplication.run(SampleApp.class, args);
  }

  @Override
  public void run(ApplicationArguments args) throws Exception {
    LOGGER.info("STARTING THE APPLICATION");

    System.out.println("Running DB Sample...");

    // STEP 1: Get a JDBC Connection
    System.out.println("*** Step 1: getJDBCConnection() ***");
    Connection conn = getJDBCConnection();

    // STEP 2: Choose a driver
    System.out.println("*** Step 2: getDatabaseDriver() ***");
    DBDatabaseDriver driver = getDatabaseDriver(conn);

    try {
      // STEP 3: Open Database (and create if not existing)
      System.out.println("*** Step 3: openDatabase() ***");
      // Open the database
      db.open(driver, conn);
      // Check whether database exists
      databaseExists(conn);
      System.out.println("*** Database already exists. Skipping Step4 ***");
    } catch (Exception e) {
      // STEP 4: Create Database
      System.out.println("*** Step 4: createDDL() ***");
      // postgre does not support DDL in transaction
      if (db.getDriver() instanceof DBDatabaseDriverPostgreSQL) {
        conn.setAutoCommit(true);
      }
      createDatabase(driver, conn);
      if (db.getDriver() instanceof DBDatabaseDriverPostgreSQL) {
        conn.setAutoCommit(false);
      }
      // Open again
      if (db.isOpen() == false) {
        db.open(driver, conn);
      }
    }

    // STEP 5: Clear Database (Delete all records)
    System.out.println("*** Step 5: clearDatabase() ***");
    clearDatabase(conn);

    // STEP 6: Insert Departments
    System.out.println("*** Step 6: insertDepartment() & insertEmployee() ***");
    int idDevDep = insertDepartment(conn, "Development", "ITTK");
    int idSalDep = insertDepartment(conn, "Sales", "ITTK");
    // Insert Employees
    int idPers1 = insertEmployee(conn, "Peter", "Sharp", Gender.M, idDevDep);
    int idPers2 = insertEmployee(conn, "Fred", "Bloggs", Gender.M, idDevDep);
    int idPers3 = insertEmployee(conn, "Emma", "White", Gender.F, idSalDep);

    // STEP 7: Update Records (by setting the phone Number)
    System.out.println("*** Step 7: updateEmployee() ***");
    updateEmployee(conn, idPers1, "+49-7531-457160");
    updateEmployee(conn, idPers2, "+49-5555-505050");
    updateEmployee(conn, idPers3, "+49-040-125486");

    // commit
    db.commit(conn);

    // STEP 8: Option 1: Query Records and print tab-separated
    System.out.println("*** Step 8 Option 1: queryRecords() / Tab-Output ***");
    queryRecords(conn, QueryType.Reader); // Tab-Output

    // STEP 8: Option 2: Query Records as a list of java beans
    System.out.println("*** Step 8 Option 2: queryRecords() / Bean-List-Output ***");
    queryRecords(conn, QueryType.BeanList); // Bean-List-Output

    // STEP 8: Option 3: Query Records as XML
    System.out.println("*** Step 8 Option 3: queryRecords() / XML-Output ***");
    queryRecords(conn, QueryType.XmlDocument); // XML-Output

    // STEP 9: Use Bean Result to query beans
    queryBeans(conn);

    // Done
    System.out.println("DB Sample finished successfully.");
  }

  /**
   * Creates an Empire-db DatabaseDriver for the given provider and applies driver specific configuration
   */
  private DBDatabaseDriver getDatabaseDriver(Connection conn) {
    try {   // Get Driver Class Name
      String driverClassName = empireDBConfigProperties.getDriverClass();
      if (StringUtils.isEmpty(driverClassName)) {
        throw new RuntimeException("Configuration error: Element 'empiredb.driverClass' not found in properties of profile '" + environment.getActiveProfiles().toString() + "'");
      }

      // Create driver
      DBDatabaseDriver driver = (DBDatabaseDriver) Class.forName(driverClassName).getDeclaredConstructor().newInstance();

      // Configure driver
      readProperties(driver);

      // Special cases
      if (driver instanceof DBDatabaseDriverPostgreSQL) {
        // Create the reverse function that is needed by this sample
        ((DBDatabaseDriverPostgreSQL) driver).createReverseFunction(conn);
      }

      // done
      return driver;
    } catch (Exception e) {
      // catch any checked exception and forward it
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  /**
   * <PRE>
   * Empties all Tables.
   * </PRE>
   */
  private void clearDatabase(Connection conn) {
    DBCommand cmd = db.createCommand();
    // Delete all Employees (no constraints)
    db.executeDelete(db.EMPLOYEES, cmd, conn);
    // Delete all Departments (no constraints)
    db.executeDelete(db.DEPARTMENTS, cmd, conn);
  }

  /**
   * <PRE>
   * Creates a DDL Script for entire SampleDB Database and executes it line by line.
   * Please make sure you uses the correct DatabaseDriver for your target DBMS.
   * </PRE>
   */
  private void createDatabase(DBDatabaseDriver driver, Connection conn) {
    // create DDL for Database Definition
    DBSQLScript script = new DBSQLScript();
    db.getCreateDDLScript(driver, script);
    // Show DDL Statement
    System.out.println(script.toString());
    // Execute Script
    script.executeAll(driver, conn, false);
    // Commit
    db.commit(conn);
  }

  /**
   * <PRE>
   * Checks whether the database exists or not by executing
   *     select count(*) from DEPARTMENTS
   * If the Departments table does not exist the querySingleInt() function return -1 for failure.
   * Please note that in this case an error will appear in the log which can be ignored.
   * </PRE>
   */
  private boolean databaseExists(Connection conn) {
    // Check whether DB exists
    DBCommand cmd = db.createCommand();
    cmd.select(db.DEPARTMENTS.count());
    // Check using "select count(*) from DEPARTMENTS"
    System.out.println("Checking whether table DEPARTMENTS exists (SQLException will be logged if not - please ignore) ...");
    return (db.querySingleInt(cmd, -1, conn) >= 0);
  }

  /**
   * <PRE>
   * Opens and returns a JDBC-Connection.
   * JDBC url, user and password for the connection are obtained from the SampleConfig bean
   * Please use the config.xml file to change connection params.
   * </PRE>
   */
  private Connection getJDBCConnection() {
    // Establish a new database connection
    Connection conn = null;
    try {
      conn = dataSource.getConnection();
      LOGGER.info("Connected successfully");
      // set the AutoCommit to false for this connection. 
      // commit must be called explicitly! 
      conn.setAutoCommit(false);
      LOGGER.info("AutoCommit is " + conn.getAutoCommit());
    } catch (Exception e) {
//      LOGGER.error("Failed to connect directly to '" + config.getJdbcURL() + "' / User=" + config.getJdbcUser());
      LOGGER.error(e.toString());
      throw new RuntimeException(e);
    }
    return conn;
  }

  private void readProperties(Object bean) {
    // Check arguments
    if (bean == null) {
      throw new InvalidArgumentException("bean", bean);
    }

    Map<String, String> driverProperties = empireDBConfigProperties.getDriverProperties();
    if (driverProperties != null) {
      for (Map.Entry<String, String> entry : driverProperties.entrySet()) {
        String name = entry.getKey();
        String newValue = entry.getValue();
        try {
          BeanUtils.setProperty(bean, name, newValue);

          Object value = BeanUtils.getProperty(bean, name);
          if (ObjectUtils.compareEqual(newValue, value)) {
            LOGGER.info("Configuration property '{}' set to \"{}\"", name, newValue);
          } else {
            LOGGER.error("Failed to set property '{}'. Value is \"{}\"", name, value);
          }
        } catch (IllegalAccessException ex) {
          LOGGER.error(null, ex);
        } catch (InvocationTargetException ex) {
          LOGGER.error(null, ex);
        } catch (NoSuchMethodException ex) {
          LOGGER.error("Property '{}' not found in {}", name, bean.getClass().getName());
        }
      }
    }
  }

  /**
   * <PRE>
   * Insert a Department into the Departments table.
   * </PRE>
   */
  private int insertDepartment(Connection conn, String departmentName, String businessUnit) {
    // Insert a Department
    DBRecord rec = new DBRecord();
    rec.create(db.DEPARTMENTS);
    rec.setValue(db.DEPARTMENTS.NAME, departmentName);
    rec.setValue(db.DEPARTMENTS.BUSINESS_UNIT, businessUnit);
    rec.update(conn);
    // Return Department ID
    return rec.getInt(db.DEPARTMENTS.DEPARTMENT_ID);
  }

  /**
   * <PRE>
   * Inserts an Employee into the Employees table.
   * </PRE>
   */
  private int insertEmployee(Connection conn, String firstName, String lastName, Gender gender, int departmentId) {
    // Insert an Employee
    DBRecord rec = new DBRecord();
    rec.create(db.EMPLOYEES);
    rec.setValue(db.EMPLOYEES.FIRSTNAME, firstName);
    rec.setValue(db.EMPLOYEES.LASTNAME, lastName);
    rec.setValue(db.EMPLOYEES.GENDER, gender);
    rec.setValue(db.EMPLOYEES.DEPARTMENT_ID, departmentId);
    rec.update(conn);
    // Return Employee ID
    return rec.getInt(db.EMPLOYEES.EMPLOYEE_ID);
  }

  /**
   * <PRE>
   * Updates an employee record by setting the phone number.
   * </PRE>
   */
  private void updateEmployee(Connection conn, int idPers, String phoneNumber) {
    // Update an Employee
    DBRecord rec = new DBRecord();
    rec.read(db.EMPLOYEES, idPers, conn);
    // Set
    rec.setValue(db.EMPLOYEES.PHONE_NUMBER, phoneNumber);
    rec.update(conn);
  }

  /**
   * <PRE>
   * Performs an SQL-Query and prints the result to System.out
   *
   * First a DBCommand object is used to create the following SQL-Query (Oracle-Syntax):
   *
   *   SELECT t2.EMPLOYEE_ID, t2.LASTNAME || ', ' || t2.FIRSTNAME AS FULL_NAME, t2.GENDER, t2.PHONE_NUMBER,
   *          substr(t2.PHONE_NUMBER, length(t2.PHONE_NUMBER)-instr(reverse(t2.PHONE_NUMBER), '-')+2) AS PHONE_EXTENSION,
   *          t1.NAME AS DEPARTMENT, t1.BUSINESS_UNIT
   *   FROM EMPLOYEES t2 INNER JOIN DEPARTMENTS t1 ON t1.DEPARTMENT_ID = t2.DEPARTMENT_ID
   *   WHERE length(t2.LASTNAME)>0
   *   ORDER BY t2.LASTNAME, t2.FIRSTNAME
   *
   * For processing the rows there are three options available:
   *
   *   QueryType.Reader:
   *     Iterates through all rows and prints field values as tabbed text.
   *
   *   QueryType.BeanList:
   *     Obtains the query result as a list of JavaBean objects of type SampleBean.
   *     It then iterates through the list of beans and uses bean.toString() for printing.
   *
   *   QueryType.XmlDocument:
   *     Obtains the query result as an XML-Document and prints the document.
   *     Please note, that the XML not only contains the data but also the field metadata.
   * </PRE>
   */
  private void queryRecords(Connection conn, QueryType queryType) {
    // Define the query
    DBCommand cmd = db.createCommand();
    // Define shortcuts for tables used - not necessary but convenient
    SampleDB.Employees EMP = db.EMPLOYEES;
    SampleDB.Departments DEP = db.DEPARTMENTS;

    // The following expression concats lastname + ', ' + firstname
    DBColumnExpr EMPLOYEE_FULLNAME = EMP.LASTNAME.append(", ").append(EMP.FIRSTNAME).as("FULL_NAME");

    // The following expression extracts the extension number from the phone field
    // e.g. substr(PHONE_NUMBER, length(PHONE_NUMBER)-instr(reverse(PHONE_NUMBER), '-')+2) AS PHONE_EXTENSION
    // Hint: Since the reverse() function is not supported by HSQLDB there is special treatment for HSQL
    DBColumnExpr PHONE_LAST_DASH;
    if (db.getDriver() instanceof DBDatabaseDriverHSql
            || db.getDriver() instanceof DBDatabaseDriverDerby
            || db.getDriver() instanceof DBDatabaseDriverH2) {
      PHONE_LAST_DASH = EMP.PHONE_NUMBER.indexOf("-", EMP.PHONE_NUMBER.indexOf("-").plus(1)).plus(1); // HSQLDB only
    } else {
      PHONE_LAST_DASH = EMP.PHONE_NUMBER.length().minus(EMP.PHONE_NUMBER.reverse().indexOf("-")).plus(2);
    }
    DBColumnExpr PHONE_EXT_NUMBER = EMP.PHONE_NUMBER.substring(PHONE_LAST_DASH).as("PHONE_EXTENSION");

    // DBColumnExpr genderExpr = cmd.select(EMP.GENDER.decode(EMP.GENDER.getOptions()).as(EMP.GENDER.getName()));
    // Select required columns
    cmd.select(EMP.EMPLOYEE_ID, EMPLOYEE_FULLNAME);
    cmd.select(EMP.GENDER, EMP.PHONE_NUMBER, PHONE_EXT_NUMBER);
    cmd.select(DEP.NAME.as("DEPARTMENT"));
    cmd.select(DEP.BUSINESS_UNIT);
    cmd.join(EMP.DEPARTMENT_ID, DEP.DEPARTMENT_ID);
    // Set constraints and order
    cmd.where(EMP.LASTNAME.length().isGreaterThan(0));
    cmd.orderBy(EMP.LASTNAME, EMP.FIRSTNAME);

    /*
        // Example for limitRows() and skipRows()
        if (db.getDriver().isSupported(DBDriverFeature.QUERY_LIMIT_ROWS))
        {	// set maximum number of rows
        	cmd.limitRows(20);
            if (db.getDriver().isSupported(DBDriverFeature.QUERY_SKIP_ROWS))
                cmd.skipRows(1);
        }
     */
    // Query Records and print output
    DBReader reader = new DBReader();
    try {
      // Open Reader
      System.out.println("Running Query:");
      System.out.println(cmd.getSelect());
      reader.open(cmd, conn);
      // Print output
      System.out.println("---------------------------------");
      switch (queryType) {
        case Reader:
          // Text-Output by iterating through all records.
          while (reader.moveNext()) {
            System.out.println(reader.getString(EMP.EMPLOYEE_ID)
                    + "\t" + reader.getString(EMPLOYEE_FULLNAME)
                    + "\t" + EMP.GENDER.getOptions().get(reader.getString(EMP.GENDER))
                    + "\t" + reader.getString(PHONE_EXT_NUMBER)
                    + "\t" + reader.getString(DEP.NAME));
          }
          break;
        case BeanList:
          // Text-Output using a list of Java Beans supplied by the DBReader
          List<SampleBean> beanList = reader.getBeanList(SampleBean.class);
          System.out.println(String.valueOf(beanList.size()) + " SampleBeans returned from Query.");
          for (SampleBean b : beanList) {
            System.out.println(b.toString());
          }
          break;
        case XmlDocument:
          // XML Output
          Document doc = reader.getXmlDocument();
          // Print XML Document to System.out
          XMLWriter.debug(doc);
          break;
      }

    } finally {
      // always close Reader
      reader.close();
    }
  }

  private void queryBeans(Connection conn) {
    // Query all males
    BeanResult<SampleBean> result = new BeanResult<SampleBean>(SampleBean.class, db.EMPLOYEES);
    result.getCommand().where(db.EMPLOYEES.GENDER.is(Gender.M));
    result.fetch(conn);

    System.out.println("Number of male employees is: " + result.size());

    // And now, the females
    result.getCommand().where(db.EMPLOYEES.GENDER.is(Gender.F));
    result.fetch(conn);

    System.out.println("Number of female employees is: " + result.size());
  }
}
