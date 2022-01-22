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

import org.apache.empire.commons.StringUtils;
import org.apache.empire.data.bean.BeanResult;
import org.apache.empire.db.DBColumnExpr;
import org.apache.empire.db.DBCommand;
import org.apache.empire.db.DBContext;
import org.apache.empire.db.DBDatabaseDriver;
import org.apache.empire.db.DBQuery;
import org.apache.empire.db.DBReader;
import org.apache.empire.db.DBRecord;
import org.apache.empire.db.DBRowSet.PartialMode;
import org.apache.empire.db.DBSQLScript;
import org.apache.empire.db.context.DBContextStatic;
import org.apache.empire.db.driver.derby.DBDatabaseDriverDerby;
import org.apache.empire.db.driver.h2.DBDatabaseDriverH2;
import org.apache.empire.db.driver.hsql.DBDatabaseDriverHSql;
import org.apache.empire.db.driver.postgresql.DBDatabaseDriverPostgreSQL;
import org.apache.empire.samples.db.SampleDB.Gender;
import org.apache.empire.xml.XMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;


public class SampleApp 
{
    // Logger
    private static final Logger log = LoggerFactory.getLogger(SampleApp.class);

	private static final SampleDB db = new SampleDB();

    private static SampleConfig config = new SampleConfig();
	
	private static DBContext context;

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

			System.out.println("Running DB Sample...");

			// STEP 1: Get a JDBC Connection
			System.out.println("*** Step 1: getJDBCConnection() ***");
			
			Connection conn = getJDBCConnection();

			// STEP 2: Choose a driver
			System.out.println("*** Step 2: getDatabaseProvider() ***");
			DBDatabaseDriver driver = getDatabaseDriver(config.getDatabaseProvider(), conn);
			
            // STEP 2.2: Create a Context
			context = new DBContextStatic(driver, conn, true, true); 

            // STEP 3: Open Database (and create if not existing)
            System.out.println("*** Step 3: openDatabase() ***");
			try {
			    // Open the database
                db.open(context);
                // Check whether database exists
			    databaseExists();
                System.out.println("*** Database already exists. Skipping Step4 ***");
                
			} catch(Exception e) {
                // STEP 4: Create Database
                System.out.println("*** Step 4: createDDL() ***");
                // postgre does not support DDL in transaction
                if(db.getDriver() instanceof DBDatabaseDriverPostgreSQL)
                {
                	conn.setAutoCommit(true);
                }
                createDatabase();
                if(db.getDriver() instanceof DBDatabaseDriverPostgreSQL)
                {
                	conn.setAutoCommit(false);
                }
                // Open again
                if (db.isOpen()==false)
                    db.open(context);
			}

			// STEP 5: Clear Database (Delete all records)
			System.out.println("*** Step 5: clearDatabase() ***");
			clearDatabase();

			// STEP 6: Insert Departments
			System.out.println("*** Step 6: insertDepartment() & insertEmployee() ***");
			int idDevDep = insertDepartment("Development", "ITTK");
			int idSalDep = insertDepartment("Sales", "ITTK");
			// Insert Employees
			int idEmp1 = insertEmployee("Peter", "Sharp", Gender.M, idDevDep);
			int idEmp2 = insertEmployee("Fred", "Bloggs", Gender.M, idDevDep);
			int idEmp3 = insertEmployee("Emma", "White",  Gender.F, idSalDep);

			// commit
			context.commit();
			
            int idEmp = testTransactionCreate(idDevDep);
            testTransactionUpdate(idEmp);
            testTransactionDelete(idEmp);

			// STEP 7: Update Records (by setting the phone Number)
			System.out.println("*** Step 7: updateEmployee() ***");
			updateEmployee(idEmp1, "+49-7531-457160");
			updateEmployee(idEmp2, "+49-5555-505050");
			// Partial Record
            updatePartialRecord(idEmp3, "+49-040-125486");
			// Update Joined Records (Make Fred Bloggs head of department and set salary)
			updateJoinedRecords(idEmp2, 100000);

			// commit
			context.commit();

			// STEP 8: Option 1: Query Records and print tab-separated
			System.out.println("*** Step 8 Option 1: queryRecords() / Tab-Output ***");
			queryRecords(QueryType.Reader); // Tab-Output

            // STEP 8: Option 2: Query Records as a list of java beans
            System.out.println("*** Step 8 Option 2: queryRecords() / Bean-List-Output ***");
            queryRecords(QueryType.BeanList); // Bean-List-Output

			// STEP 8: Option 3: Query Records as XML
			System.out.println("*** Step 8 Option 3: queryRecords() / XML-Output ***");
			queryRecords(QueryType.XmlDocument); // XML-Output

			// STEP 9: Use Bean Result to query beans
			queryBeans(conn);
			
			// Done
			System.out.println("DB Sample finished successfully.");

		} catch (Exception e)
        {
			// Error
			System.out.println(e.toString());
			e.printStackTrace();
			
		} finally {
		    context.discard();
		}
	}

	/**
     * <PRE>
	 * Opens and returns a JDBC-Connection.
	 * JDBC url, user and password for the connection are obtained from the SampleConfig bean
	 * Please use the config.xml file to change connection params.
     * </PRE>
	 */
	private static Connection getJDBCConnection()
    {
		// Establish a new database connection
		Connection conn = null;
		log.info("Connecting to Database'" + config.getJdbcURL() + "' / User=" + config.getJdbcUser());
		try
        {
            // Connect to the database
			Class.forName(config.getJdbcClass()).newInstance();
			conn = DriverManager.getConnection(config.getJdbcURL(), config.getJdbcUser(), config.getJdbcPwd());
			log.info("Connected successfully");
			// set the AutoCommit to false for this connection. 
			// commit must be called explicitly! 
			conn.setAutoCommit(false);
			log.info("AutoCommit is " + conn.getAutoCommit());

		} catch (Exception e)
        {
			log.error("Failed to connect directly to '" + config.getJdbcURL() + "' / User=" + config.getJdbcUser());
			log.error(e.toString());
			throw new RuntimeException(e);
		}
		return conn;
	}

	/**
	 * Creates an Empire-db DatabaseDriver for the given provider and applies driver specific configuration 
	 */
    private static DBDatabaseDriver getDatabaseDriver(String provider, Connection conn)
    {
        try
        {   // Get Driver Class Name
            String driverClassName = config.getEmpireDBDriverClass();
            if (StringUtils.isEmpty(driverClassName))
                throw new RuntimeException("Configuration error: Element 'empireDBDriverClass' not found in node 'properties-"+provider+"'");

            // Create driver
            DBDatabaseDriver driver = (DBDatabaseDriver) Class.forName(driverClassName).newInstance();

            // Configure driver
            config.readProperties(driver, "properties-"+provider, "empireDBDriverProperites");

            // Special cases
            if (driver instanceof DBDatabaseDriverPostgreSQL)
            {   // Create the reverse function that is needed by this sample
                ((DBDatabaseDriverPostgreSQL)driver).createReverseFunction(conn);
            }

            // done
            return driver;
            
        } catch (Exception e)
        {   // catch any checked exception and forward it
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

	/**
     * <PRE>
	 * Checks whether the database exists or not by executing
	 *     select count(*) from DEPARTMENTS
	 * If the Departments table does not exist the querySingleInt() function return -1 for failure.
	 * Please note that in this case an error will appear in the log which can be ignored.
     * </PRE>
	 */
	private static boolean databaseExists()
    {
		// Check whether DB exists
		DBCommand cmd = db.createCommand();
		cmd.select(db.DEPARTMENTS.count());
		// Check using "select count(*) from DEPARTMENTS"
		System.out.println("Checking whether table DEPARTMENTS exists (SQLException will be logged if not - please ignore) ...");
		return (context.getUtils().querySingleInt(cmd, -1) >= 0);
	}

	/**
     * <PRE>
	 * Creates a DDL Script for entire SampleDB Database and executes it line by line.
	 * Please make sure you uses the correct DatabaseDriver for your target DBMS.
     * </PRE>
	 */
	private static void createDatabase()
    {
		// create DDL for Database Definition
	    DBSQLScript script = new DBSQLScript(context);
		db.getCreateDDLScript(script);
		// Show DDL Statement
		System.out.println(script.toString());
		// Execute Script
		script.executeAll(false);
		// Commit
		context.commit();
	}

	/**
     * <PRE>
	 * Empties all Tables.
     * </PRE>
	 */
	private static void clearDatabase()
    {
		DBCommand cmd = context.createCommand(db);
		// Delete all Employees (no constraints)
		context.executeDelete(db.EMPLOYEES, cmd);
		// Delete all Departments (no constraints)
		context.executeDelete(db.DEPARTMENTS, cmd);
	}

	/**
     * <PRE>
	 * Insert a Department into the Departments table.
     * </PRE>
	 */
	private static int insertDepartment(String departmentName, String businessUnit)
    {
        SampleDB.Departments DEP = db.DEPARTMENTS;
		// Insert a Department
		DBRecord rec = new DBRecord(context, DEP);
		rec.create();
		rec.setValue(DEP.NAME, departmentName);
		rec.setValue(DEP.BUSINESS_UNIT, businessUnit);
		rec.update();
		// Return Department ID
		return rec.getInt(DEP.DEPARTMENT_ID);
	}

	/**
     * <PRE>
	 * Inserts an Employee into the Employees table.
     * </PRE>
	 */
	private static int insertEmployee(String firstName, String lastName, Gender gender, int departmentId)
    {
        SampleDB.Employees EMP = db.EMPLOYEES;
		// Insert an Employee
		DBRecord rec = new DBRecord(context, EMP);
		rec.create();
		rec.setValue(EMP.FIRSTNAME, firstName);
		rec.setValue(EMP.LASTNAME, lastName);
		rec.setValue(EMP.GENDER, gender);
		rec.setValue(EMP.DEPARTMENT_ID, departmentId);
		rec.update();
		// Return Employee ID
		return rec.getInt(EMP.EMPLOYEE_ID);
	}

	/**
     * <PRE>
	 * Updates an employee record by setting the phone number.
     * </PRE>
	 */
	private static void updateEmployee(int idEmp, String phoneNumber)
    {
		// Update an Employee
		DBRecord rec = new DBRecord(context, db.EMPLOYEES);
		rec.read(idEmp);
		// Set
		rec.setValue(db.EMPLOYEES.PHONE_NUMBER, phoneNumber);
		rec.update();
	}

    /**
     * <PRE>
     * Updates an employee record by setting the phone number.
     * </PRE>
     */
    private static void updatePartialRecord(int idEmp, String phoneNumber)
    {
        // Shortcut for convenience
        SampleDB.Employees EMP = db.EMPLOYEES;
        // Update an Employee with partial record
        // this will only load the EMPLOYEE_ID and the PHONE_NUMBER
        DBRecord rec = new DBRecord(context, EMP);
        EMP.readRecord(rec, DBRecord.key(idEmp), PartialMode.INCLUDE, EMP.PHONE_NUMBER);
        // Set
        rec.setValue(db.EMPLOYEES.PHONE_NUMBER, phoneNumber);
        rec.update();
    }

    /**
     * <PRE>
     * Updates an employee record by setting the phone number.
     * </PRE>
     */
    private static void updateJoinedRecords(int idEmp, int salary)
    {
        // Shortcuts for convenience
        SampleDB.Employees EMP = db.EMPLOYEES;
        SampleDB.Departments DEP = db.DEPARTMENTS;

        // Create DBQuery from command
        DBCommand cmd = db.createCommand();
        cmd.select(EMP.getColumns());
        cmd.select(DEP.getColumns());
        cmd.join(EMP.DEPARTMENT_ID, DEP.DEPARTMENT_ID);
        DBQuery query = new DBQuery(cmd, EMP.EMPLOYEE_ID);

        // Make employee Head of Department and update salary
        DBRecord rec = new DBRecord(context, query);
        rec.read(idEmp);
        rec.setValue(EMP.SALARY, salary);
        rec.setValue(DEP.HEAD, rec.getString(EMP.LASTNAME));
        rec.update();
    }

	/**
	 * @param context
	 * @param idDep
	 */
	private static int testTransactionCreate(int idDep)
    {
        // Shortcut for convenience
        SampleDB.Employees EMP = db.EMPLOYEES;

        DBRecord rec = new DBRecord(context, EMP);
        rec.create();
        rec.setValue(EMP.FIRSTNAME, "Foo");
        rec.setValue(EMP.LASTNAME, "Manchoo");
        rec.setValue(EMP.GENDER, Gender.M);
        rec.setValue(EMP.DEPARTMENT_ID, idDep);
        rec.update();
        log.info("Timestamp {}", rec.getString(EMP.UPDATE_TIMESTAMP));
        
        rec.setValue(EMP.FIRSTNAME, "Foo 2");
        rec.setValue(EMP.LASTNAME, "Manchu");
        rec.setValue(EMP.PHONE_NUMBER, "0815/4711");
        rec.update();
        log.info("Timestamp {}", rec.getString(EMP.UPDATE_TIMESTAMP));
        
        context.rollback();
        
        rec.setValue(EMP.FIRSTNAME, "Dr. Foo");
        rec.update();
        log.info("Timestamp {}", rec.getString(EMP.UPDATE_TIMESTAMP));

        rec.delete();
        
        context.rollback();

        // insert final
        rec.update();
        log.info("Timestamp {}", rec.getString(EMP.UPDATE_TIMESTAMP));
        
        log.info("testTransactionCreate performed OK");
        context.commit();
        
        return rec.getInt(EMP.EMPLOYEE_ID);
    }
    /**
     * @param context
     * @param idDep
     */
    private static void testTransactionUpdate(int idEmp)
    {
        // Shortcut for convenience
        SampleDB.Employees EMP = db.EMPLOYEES;
        
        DBRecord rec = new DBRecord(context, EMP);        
        rec.read(idEmp);
        rec.setValue(EMP.PHONE_NUMBER, null);
        rec.setValue(EMP.SALARY, "100.000");
        rec.update();

        log.info("Timestamp {}", rec.getString(EMP.UPDATE_TIMESTAMP));
        
        context.rollback();
        
        rec.setValue(EMP.PHONE_NUMBER, "07531-45716-0");
        rec.update();

        log.info("Timestamp {}", rec.getString(EMP.UPDATE_TIMESTAMP));
        
        context.rollback();

        rec.update();

        log.info("Timestamp {}", rec.getString(EMP.UPDATE_TIMESTAMP));
        log.info("testTransactionUpdate performed OK");
        context.commit();
        
    }
    /**
     * @param context
     * @param idDep
     */
    private static void testTransactionDelete(int idEmp)
    {
        // Shortcut for convenience
        SampleDB.Employees T = db.EMPLOYEES;

        DBRecord rec = new DBRecord(context, T);
        rec.read(idEmp);
        /*
        log.info("Timestamp {}", rec.getString(T.UPDATE_TIMESTAMP));
        rec.setValue(T.SALARY, "100.001");
        rec.update();
        log.info("Timestamp {}", rec.getString(T.UPDATE_TIMESTAMP));
        */
        rec.delete();
        
        context.rollback();

        /*
        DBCommand cmd = db.createCommand();
        cmd.select(T.UPDATE_TIMESTAMP);
        cmd.where (T.EMPLOYEE_ID.is(idEmp));
        log.info("Timestamp {}", db.querySingleString(cmd, context.getConnection()));
        */
        
        rec.update();
        
        log.info("Transaction performed OK");
        
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
	private static void queryRecords(QueryType queryType)
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
		DBReader reader = new DBReader(context);
		try
        {
			// Open Reader
			System.out.println("Running Query:");
			System.out.println(cmd.getSelect());
			reader.open(cmd);
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
	
	private static void queryBeans(Connection conn)
	{
	    SampleDB.Employees EMP = db.EMPLOYEES;
        // Query all males
	    BeanResult<SampleBean> result = new BeanResult<SampleBean>(SampleBean.class, EMP);
        result.getCommand().where(EMP.GENDER.is(Gender.M));
	    result.fetch(context);
	    
	    System.out.println("Number of male employees is: "+result.size());

	    // And now, the females
	    result.getCommand().where(EMP.GENDER.is(Gender.F));
	    result.fetch(context);
	    
        System.out.println("Number of female employees is: "+result.size());
	}
	
}
