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
package org.apache.empire.samples.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.logging.Logger;

import org.apache.empire.commons.ErrorObject;
import org.apache.empire.db.DBColumnExpr;
import org.apache.empire.db.DBCommand;
import org.apache.empire.db.DBDatabaseDriver;
import org.apache.empire.db.DBReader;
import org.apache.empire.db.DBRecord;
import org.apache.empire.db.DBSQLScript;
import org.apache.empire.db.derby.DBDatabaseDriverDerby;
import org.apache.empire.db.h2.DBDatabaseDriverH2;
import org.apache.empire.db.hsql.DBDatabaseDriverHSql;
import org.apache.empire.db.mysql.DBDatabaseDriverMySQL;
import org.apache.empire.db.oracle.DBDatabaseDriverOracle;
import org.apache.empire.db.postgresql.DBDatabaseDriverPostgreSQL;
import org.apache.empire.db.sqlserver.DBDatabaseDriverMSSQL;
import org.apache.empire.xml.XMLWriter;
import org.w3c.dom.Document;


public class SampleApp 
{
	private static final Logger logger = Logger.getLogger(SampleApp.class.getName());

	private static final SampleDB db = new SampleDB();

	private static SampleConfig config = new SampleConfig();

	private enum QueryType
    {
	    Reader,
        BeanList,
	    XmlDocument
	}

	/**
     * <PRE>
	 * This is the entry point of the Empire-DB Sample Application
	 * Please check the config.xml configuration file for Database and Connection settings.
     * </PRE>
	 * @param args arguments
	 */
	public static void main(String[] args)
    {
		try
        {
			// Init Configuration
			config.init((args.length > 0 ? args[0] : "config.xml" ));

			// Enable Exceptions
            ErrorObject.setExceptionsEnabled(true);

			System.out.println("Running DB Sample...");

			// STEP 1: Get a JDBC Connection
			System.out.println("*** Step 1: getJDBCConnection() ***");
			
			Connection conn = getJDBCConnection();

			// STEP 2: Choose a driver
			System.out.println("*** Step 2: getDatabaseProvider() ***");
			DBDatabaseDriver driver = getDatabaseDriver(config.getDatabaseProvider(), conn);

            // STEP 3: Open Database (and create if not existing)
            System.out.println("*** Step 3: openDatabase() ***");
			try {
			    db.open(driver, conn);
			    databaseExists(conn);
                System.out.println("*** Database already exists. Skipping Step4 ***");
                
			} catch(Exception e) {
                // STEP 4: Create Database
                System.out.println("*** Step 4: createDDL() ***");
                // postgre does not support DDL in transaction
                if(db.getDriver() instanceof DBDatabaseDriverPostgreSQL)
                {
                	conn.setAutoCommit(true);
                }
                createDatabase(driver, conn);
                if(db.getDriver() instanceof DBDatabaseDriverPostgreSQL)
                {
                	conn.setAutoCommit(false);
                }
                // Open again
                if (db.isOpen()==false)
                    db.open(driver, conn);
			}

			// STEP 5: Clear Database (Delete all records)
			System.out.println("*** Step 5: clearDatabase() ***");
			clearDatabase(conn);

			// STEP 6: Insert Departments
			System.out.println("*** Step 6: insertDepartment() & insertEmployee() ***");
			int idDevDep = insertDepartment(conn, "Development", "ITTK");
			int idSalDep = insertDepartment(conn, "Sales", "ITTK");
			// Insert Employees
			int idPers1 = insertEmployee(conn, "Peter", "Sharp", "M", idDevDep);
			int idPers2 = insertEmployee(conn, "Fred", "Bloggs", "M", idDevDep);
			int idPers3 = insertEmployee(conn, "Emma", "White", "F", idSalDep);

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

			// Done
			System.out.println("DB Sample finished successfully.");

		} catch (Exception e)
        {
			// Error
			System.out.println(e.toString());
			e.printStackTrace();
		}

	}

	/**
     * <PRE>
	 * Opens and returns a JDBC-Connection.
	 * JDBC url, user and password for the connection are obained from the SampleConfig bean
	 * Please use the config.xml file to change connection params.
     * </PRE>
	 */
	private static Connection getJDBCConnection()
    {
		// Establish a new database connection
		Connection conn = null;
		logger.info("Connecting to Database'" + config.getJdbcURL() + "' / User=" + config.getJdbcUser());
		try
        {
            // Connect to the databse
			Class.forName(config.getJdbcClass()).newInstance();
			conn = DriverManager.getConnection(config.getJdbcURL(), config.getJdbcUser(), config.getJdbcPwd());
			logger.info("Connected successfully");
			// set the AutoCommit to false this session. You must commit
			// explicitly now
			conn.setAutoCommit(true);
			logger.info("AutoCommit is " + conn.getAutoCommit());

		} catch (Exception e)
        {
			logger.severe("Failed to connect directly to '" + config.getJdbcURL() + "' / User=" + config.getJdbcUser());
			logger.severe(e.toString());
			throw new RuntimeException(e);
		}
		return conn;
	}

    /**
     * <PRE>
     * Returns the correspondig DatabaseDriver for a given database provider / vendor
     * Valid Providers are "oracle", "sqlserver" and "hsqldb".
     * </PRE>
     */
    private static DBDatabaseDriver getDatabaseDriver(String provider, Connection conn)
    {
        if (provider.equalsIgnoreCase("mysql"))
        {
            DBDatabaseDriverMySQL driver = new DBDatabaseDriverMySQL();
            // Set Driver specific properties (if any)
            driver.setDatabaseName(config.getSchemaName());
            return driver;
        }
        else if (provider.equalsIgnoreCase("oracle"))
        {
            DBDatabaseDriverOracle driver = new DBDatabaseDriverOracle();
            // Set Driver specific properties (if any)
            return driver;
        }
        else if (provider.equalsIgnoreCase("sqlserver"))
        {
            DBDatabaseDriverMSSQL driver = new DBDatabaseDriverMSSQL();
            // Set Driver specific properties (if any)
            driver.setDatabaseName(config.getSchemaName());
            return driver;
        }
        else if (provider.equalsIgnoreCase("hsqldb"))
        {
            DBDatabaseDriverHSql driver = new DBDatabaseDriverHSql();
            // Set Driver specific properties (if any)
            return driver;
        }
        else if (provider.equalsIgnoreCase("postgresql"))
        {
            DBDatabaseDriverPostgreSQL driver = new DBDatabaseDriverPostgreSQL();
            // Set Driver specific properties (if any)
            driver.setDatabaseName(config.getSchemaName());
            // Create the reverse function that is needed by this sample
            driver.createReverseFunction(conn);
            return driver;
        }
        else if (provider.equalsIgnoreCase("h2"))
        {
            DBDatabaseDriverH2 driver = new DBDatabaseDriverH2();
            // Set Driver specific properties (if any)
            driver.setDatabaseName(config.getSchemaName());
            return driver;
        }
        else if (provider.equalsIgnoreCase("derby"))
        {
            DBDatabaseDriverDerby driver = new DBDatabaseDriverDerby();
            // Set Driver specific properties (if any)
            driver.setDatabaseName(config.getSchemaName());
            return driver;
        }
        else
        {   // Unknown Provider
            throw new RuntimeException("Unknown Database Provider " + provider);
        }
    }

	/**
     * <PRE>
	 * Checks whether the database exists or not by executing
	 *     select count(*) from DEPARTMENTS
	 * If the Departments table does not exist the querySingleInt() function return -1 for failure.
	 * Please note that in this case an error will appear in the log wich can be ingored.
     * </PRE>
	 */
	private static boolean databaseExists(Connection conn)
    {
		// Check wether DB exists
		DBCommand cmd = db.createCommand();
		cmd.select(db.DEPARTMENTS.count());
		// Check using "select count(*) from DEPARTMENTS"
		System.out.println("Checking whether table DEPARTMENTS exists (SQLException will be logged if not - please ignore) ...");
		return (db.querySingleInt(cmd.getSelect(), -1, conn) >= 0);
	}

	/**
     * <PRE>
	 * Creates a DDL Script for entire SampleDB Database and executes it line by line.
	 * Please make sure you uses the correct DatabaseDriver for your target dbms.
     * </PRE>
	 */
	private static void createDatabase(DBDatabaseDriver driver, Connection conn)
    {
		// create DLL for Database Definition
	    DBSQLScript script = new DBSQLScript();
		db.getCreateDDLScript(driver, script);
		// Show DLL Statement
		System.out.println(script.toString());
		// Execute Script
		script.run(driver, conn, false);
		// Commit
		db.commit(conn);
	}

	/**
     * <PRE>
	 * Empties all Tables.
     * </PRE>
	 */
	private static void clearDatabase(Connection conn)
    {
		DBCommand cmd = db.createCommand();
		// Delete all Employees (no constraints)
		db.executeSQL(cmd.getDelete(db.EMPLOYEES), conn);
		// Delete all Departments (no constraints)
		db.executeSQL(cmd.getDelete(db.DEPARTMENTS), conn);
	}

	/**
     * <PRE>
	 * Insert a Department into the Departments table.
     * </PRE>
	 */
	private static int insertDepartment(Connection conn, String departmentName, String businessUnit)
    {
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
	private static int insertEmployee(Connection conn, String firstName, String lastName, String gender, int departmentId)
    {
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
	private static void updateEmployee(Connection conn, int idPers, String phoneNumber)
    {
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
	 *     Interates through all rows and prints field values as tabbed text.
	 *      
     *   QueryType.BeanList:
     *     Obtains the query result as a list of JavaBean objects of type SampleBean.
     *     It then iterates throuh the list of beans and uses bean.toString() for printing.
     *     
     *   QueryType.XmlDocument:
     *     Obtains the query result as an XML-Document and prints the document.
     *     Please note, that the XML not only contains the data but also the field metadata.
     * </PRE>
	 */
	private static void queryRecords(Connection conn, QueryType queryType)
    {
	    // Define the query
	    DBCommand cmd = db.createCommand();
	    // Define shortcuts for tables used - not necessary but convenient
	    SampleDB.Employees   EMP = db.EMPLOYEES;
	    SampleDB.Departments DEP = db.DEPARTMENTS;

	    // The following expression concats lastname + ', ' + firstname
        DBColumnExpr EMPLOYEE_FULLNAME = EMP.LASTNAME.append(", ").append(EMP.FIRSTNAME).as("FULL_NAME");
        
        // The following expression extracts the extension number from the phone field
        // e.g. substr(PHONE_NUMBER, length(PHONE_NUMBER)-instr(reverse(PHONE_NUMBER), '-')+2) AS PHONE_EXTENSION
        // Hint: Since the reverse() function is not supported by HSQLDB there is special treatment for HSQL
        DBColumnExpr PHONE_LAST_DASH;
        if ( db.getDriver() instanceof DBDatabaseDriverHSql 
        		|| db.getDriver() instanceof DBDatabaseDriverDerby
        		|| db.getDriver() instanceof DBDatabaseDriverH2)
             PHONE_LAST_DASH = EMP.PHONE_NUMBER.indexOf("-", EMP.PHONE_NUMBER.indexOf("-").plus(1)).plus(1); // HSQLDB only
        else PHONE_LAST_DASH = EMP.PHONE_NUMBER.length().minus(EMP.PHONE_NUMBER.reverse().indexOf("-")).plus(2);  
        DBColumnExpr PHONE_EXT_NUMBER = EMP.PHONE_NUMBER.substring(PHONE_LAST_DASH).as("PHONE_EXTENSION");
        
        // DBColumnExpr genderExpr = cmd.select(EMP.GENDER.decode(EMP.GENDER.getOptions()).as(EMP.GENDER.getName()));
		// Select requried columns
		cmd.select(EMP.EMPLOYEE_ID, EMPLOYEE_FULLNAME);
		if(db.getDriver() instanceof DBDatabaseDriverPostgreSQL)
		{
			// postgres does not support the substring expression
			cmd.select(EMP.GENDER, EMP.PHONE_NUMBER);
		}else{
			cmd.select(EMP.GENDER, EMP.PHONE_NUMBER, PHONE_EXT_NUMBER);
			
		}
		cmd.select(DEP.NAME.as("DEPARTMENT"));
		cmd.select(DEP.BUSINESS_UNIT);
		cmd.join(EMP.DEPARTMENT_ID, DEP.DEPARTMENT_ID);
        // Set contraints and order
        cmd.where(EMP.LASTNAME.length().isGreaterThan(0));
        cmd.orderBy(EMP.LASTNAME);
        cmd.orderBy(EMP.FIRSTNAME);

        cmd.where(EMP.DEPARTMENT_ID.is("Some Strange Value"));
        cmd.where(EMP.SALARY.is("Some Weird Value"));
        cmd.where(EMP.EMPLOYEE_ID.is("Some Strange Value"));
        System.out.print(cmd.getSelect());
        
		// Query Records and print output
		DBReader reader = new DBReader();
		try
        {
			// Open Reader
			System.out.println("Running Query:");
			System.out.println(cmd.getSelect());
			reader.open(cmd, conn);
			// Print output
			System.out.println("---------------------------------");
			switch(queryType)
			{
			    case Reader:
			        // Text-Output by iterating through all records.
	                while (reader.moveNext())
                    {
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
                    for (SampleBean b : beanList)
                    {
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

		} finally
        {
			// always close Reader
			reader.close();
		}
	}
	
}
