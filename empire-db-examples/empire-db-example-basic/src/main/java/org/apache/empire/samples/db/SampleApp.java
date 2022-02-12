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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import org.apache.empire.commons.StringUtils;
import org.apache.empire.data.bean.BeanResult;
import org.apache.empire.data.list.DataListEntry;
import org.apache.empire.db.DBColumnExpr;
import org.apache.empire.db.DBCommand;
import org.apache.empire.db.DBContext;
import org.apache.empire.db.DBQuery;
import org.apache.empire.db.DBReader;
import org.apache.empire.db.DBRecord;
import org.apache.empire.db.DBRecordBean;
import org.apache.empire.db.DBRowSet.PartialMode;
import org.apache.empire.db.DBSQLScript;
import org.apache.empire.db.context.DBContextStatic;
import org.apache.empire.db.generic.TRecord;
import org.apache.empire.db.validation.DBModelChecker;
import org.apache.empire.db.validation.DBModelErrorLogger;
import org.apache.empire.dbms.DBMSHandler;
import org.apache.empire.dbms.derby.DBMSHandlerDerby;
import org.apache.empire.dbms.h2.DBMSHandlerH2;
import org.apache.empire.dbms.hsql.DBMSHandlerHSql;
import org.apache.empire.dbms.postgresql.DBMSHandlerPostgreSQL;
import org.apache.empire.samples.db.SampleDB.Gender;
import org.apache.empire.samples.db.beans.Department;
import org.apache.empire.samples.db.beans.Employee;
import org.apache.empire.samples.db.beans.EmployeeQuery;
import org.apache.empire.samples.db.beans.Payment;
import org.apache.empire.xml.XMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

public class SampleApp 
{
    // Logger
    private static final Logger log = LoggerFactory.getLogger(SampleApp.class);

    private static SampleConfig config = new SampleConfig();
    
	private final SampleDB db = new SampleDB();

	private DBContext context = null;

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
        SampleApp app = new SampleApp();
        try
        {   // Init Configuration
            config.init((args.length > 0 ? args[0] : "config.xml" ));
            // Run
            log.info("Running DB Sample...");
            app.run();
            // Done
            log.info("DB Sample finished successfully.");
        } catch (Exception e) {
            // Error
            log.error("Running SampleApp failed with Exception" + e.toString(), e);
            if (app.context!=null)
                app.context.rollback();
        } finally {
            if (app.context!=null)
                app.context.discard();
        }
    }

    /**
     * This method runs all the example code
     */
    public void run()
    {
        // SECTION 1: Get a JDBC Connection
        log.info("Step 1: getJDBCConnection()");
        
        Connection conn = getJDBCConnection();

        // SECTION 2: Choose a DBMSHandler
        log.info("Step 2: getDatabaseProvider()");
        DBMSHandler dbms = getDBMSHandler(config.getDatabaseProvider(), conn);
        
        // SECTION 2.2: Create a Context
        context = new DBContextStatic(dbms, conn, false, true); 

        // SECTION 3: Open Database (and create if not existing)
        log.info("Step 3: openDatabase()");
        boolean clearExistingData = true;
        try {
            // Open the database
            db.open(context);
            // Check whether database exists
            databaseExists();
            log.info("Database already exists. Checking data model...");
            checkDataModel();
            
        } catch(Exception e) {
            // SECTION 4: Create Database
            log.info("Step 4: createDDL()");
            // postgre does not support DDL in transaction
            if(db.getDbms() instanceof DBMSHandlerPostgreSQL)
            {
                setAutoCommit(conn, true);
            }
            createDatabase();
            if(db.getDbms() instanceof DBMSHandlerPostgreSQL)
            {
                setAutoCommit(conn, false);
            }
            // Open again
            if (db.isOpen()==false)
                db.open(context);
            // initial load
            clearExistingData = false;
        }

        // SECTION 5 AND 6: Populate Database and modify Data
        populateAndModify(clearExistingData);

        // SECTION 7: Option 1: Query Records and print tab-separated
        log.info("Step 8 Option 1: queryRecords() / Tab-Output");
        queryExample(QueryType.Reader); // Tab-Output

        // SECTION 7: Option 2: Query Records as a list of java beans
        log.info("Step 8 Option 2: queryRecords() / Bean-List-Output");
        queryExample(QueryType.BeanList); // Bean-List-Output

        // SECTION 7: Option 3: Query Records as XML
        log.info("Step 8 Option 3: queryRecords() / XML-Output");
        queryExample(QueryType.XmlDocument); // XML-Output

        // SECTION 8: Use DataList query
        queryDataList();

        // SECTION 9: Use RecordList query
        queryRecordList();

        // SECTION 10: Use Bean Result to query beans
        queryBeans();
        

        /*
        int idEmp = testTransactionCreate(idDevDep);
        testTransactionUpdate(idEmp);
        testTransactionDelete(idEmp);
        */
        
        // Finally, commit any changes
        context.commit();        
    }
	
	/**
     * <PRE>
	 * Opens and returns a JDBC-Connection.
	 * JDBC url, user and password for the connection are obtained from the SampleConfig bean
	 * Please use the config.xml file to change connection params.
     * </PRE>
	 */
	private Connection getJDBCConnection()
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
			log.info("AutoCommit has been set to " + conn.getAutoCommit());

		} catch (Exception e)
        {
			log.error("Failed to connect directly to '" + config.getJdbcURL() + "' / User=" + config.getJdbcUser());
			log.error(e.toString());
			throw new RuntimeException(e);
		}
		return conn;
	}
	
	private void setAutoCommit(Connection conn, boolean enable)
	{
        try {
            conn.setAutoCommit(enable);
        } catch (SQLException e) {
            log.error("Unable to set AutoCommit on Connection", e);
        }
	}

	/**
	 * Creates an Empire-db DatabaseDriver for the given provider and applies dbms specific configuration 
	 */
    private DBMSHandler getDBMSHandler(String provider, Connection conn)
    {
        try
        {   // Get Driver Class Name
            String dbmsHandlerClass = config.getDbmsHandlerClass();
            if (StringUtils.isEmpty(dbmsHandlerClass))
                throw new RuntimeException("Configuration error: Element 'dbmsHandlerClass' not found in node 'properties-"+provider+"'");

            // Create dbms
            DBMSHandler dbms = (DBMSHandler) Class.forName(dbmsHandlerClass).newInstance();

            // Configure dbms
            config.readProperties(dbms, "properties-"+provider, "dbmsHandlerProperites");

            // Special cases
            if (dbms instanceof DBMSHandlerPostgreSQL)
            {   // Create the reverse function that is needed by this sample
                ((DBMSHandlerPostgreSQL)dbms).createReverseFunction(conn);
            }

            // done
            return dbms;
            
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
	private boolean databaseExists()
    {
		// Check whether DB exists
		DBCommand cmd = db.createCommand();
		cmd.select(db.DEPARTMENTS.count());
		// Check using "select count(*) from DEPARTMENTS"
		log.info("Checking whether table DEPARTMENTS exists (SQLException will be logged if not - please ignore) ...");
		return (context.getUtils().querySingleInt(cmd, -1) >= 0);
	}

	/**
     * <PRE>
	 * Creates a DDL Script for entire SampleDB Database and executes it line by line.
	 * Please make sure you uses the correct DatabaseDriver for your target DBMS.
     * </PRE>
	 */
	private void createDatabase()
    {
		// create DDL for Database Definition
	    DBSQLScript script = new DBSQLScript(context);
		db.getCreateDDLScript(script);
		// Show DDL Statement
		log.info(script.toString());
		// Execute Script
		script.executeAll(false);
		// Commit
		context.commit();
	}
    
    private void checkDataModel()
    {
        try {
            DBModelChecker modelChecker = context.getDbms().createModelChecker(db);
            // Check data model   
            log.info("Checking DataModel for {} using {}", db.getClass().getSimpleName(), modelChecker.getClass().getSimpleName());
            // dbo schema
            DBModelErrorLogger logger = new DBModelErrorLogger();
            modelChecker.checkModel(db, context.getConnection(), logger);
            // show result
            log.info("Data model check done. Found {} errors and {} warnings.", logger.getErrorCount(), logger.getWarnCount());
        } catch(Exception e) {
            log.error("FATAL error when checking data model. Probably not properly implemented by DBMSHandler!");
        }
    }
    
    private void populateAndModify(boolean clearExisting)
    {
        if (clearExisting)
            clearDatabase();

        log.info("Step 5: insertDepartment() & insertEmployee()");
        long idDevDep = insertDepartment("Development", "ITTK");
        long idSalDep = insertDepartment("Sales", "ITTK");
        // Insert Employees
        long idEmp1 = insertEmployee("Peter", "Sharp",  Gender.M, idDevDep);
        long idEmp2 = insertEmployee("Fred",  "Bloggs", Gender.M, idDevDep);
        long idEmp3 = insertEmployee("Emma",  "White",  Gender.F, idSalDep);
        long idEmp4 = insertEmployee("John",  "Doe",    Gender.M, idSalDep);
        long idEmp5 = insertEmployee("Sarah", "Smith",  Gender.F, idDevDep);
        // Insert Payments
        insertPayments(idEmp1, new BigDecimal(2000));
        insertPayments(idEmp3, new BigDecimal(2500));
        insertPayments(idEmp4, new BigDecimal(2200));
        insertPayments(idEmp5, new BigDecimal(1500));

        // commit
        context.commit();
        
        // SECTION 6: Modify some data
        log.info("Step 6: updateEmployee()");
        updateEmployee(idEmp1, "+49-7531-457160");
        updateEmployee(idEmp2, "+49-5555-505050");
        // Partial Record
        updatePartialRecord(idEmp3, "+49-040-125486");
        // Update Joined Records (Make Fred Bloggs head of department and set salary)
        updateJoinedRecords(idEmp2, 100000);
    }
    
	/**
     * <PRE>
	 * Empties all Tables.
     * </PRE>
	 */
	private void clearDatabase()
    {
		DBCommand cmd = context.createCommand(db);
        // Delete all Payments (no constraints)
        context.executeDelete(db.PAYMENTS, cmd);
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
	private long insertDepartment(String departmentName, String businessUnit)
    {
        SampleDB.Departments DEP = db.DEPARTMENTS;
		// Insert a Department
		TRecord<SampleDB.Departments> rec = new TRecord<SampleDB.Departments>(context, DEP);
		rec.create()
		   .set(DEP.NAME, departmentName)
		   .set(DEP.BUSINESS_UNIT, businessUnit)
		   .update();
		// Return Department ID
        return rec.getId();
	}

	/**
     * <PRE>
	 * Inserts an Employee into the Employees table.
     * </PRE>
	 */
	private long insertEmployee(String firstName, String lastName, Gender gender, long departmentId)
    {
        SampleDB.Employees EMP = db.EMPLOYEES;
		// Insert an Employee
		DBRecord rec = new DBRecord(context, EMP);
		rec.create(null)
		   .set(EMP.FIRST_NAME, firstName)
		   .set(EMP.LAST_NAME, lastName)
		   .set(EMP.GENDER, gender)
		   .set(EMP.DEPARTMENT_ID, departmentId)
	       .update();
		// Return Employee ID
		return rec.getId();
	}

    /**
     * <PRE>
     * Inserts an Payments for a particular Employee
     * </PRE>
     */
    private void insertPayments(long employeeId, BigDecimal monthlySalary)
    {
        SampleDB.Payments PAY = db.PAYMENTS;
        // Insert an Employee
        LocalDate date = LocalDate.now();
        date = date.minusDays(date.getDayOfMonth()-1); // first day of this month
        // Add Payment for each month
        DBRecord rec = new DBRecord(context, PAY);
        for (LocalDate month=date.minusMonths(20); !month.isAfter(date); month=month.plusMonths(1))
        {
            BigDecimal variation = new BigDecimal((Math.random()*200) - 100.0);
            variation = variation.setScale(2, RoundingMode.HALF_UP);
            // insert
            rec.create(DBRecord.key(employeeId, month.getYear(), month.getMonth()));
            rec.set(PAY.AMOUNT, monthlySalary.add(variation));
            rec.update();
        }
    }

	/**
     * <PRE>
	 * Updates an employee record by setting the phone number.
     * </PRE>
	 */
	private void updateEmployee(long idEmp, String phoneNumber)
    {
	    /*
		// Update an Employee
		DBRecord rec = new DBRecord(context, db.EMPLOYEES);
		rec.read(idEmp);
		// Set
		rec.set(db.EMPLOYEES.PHONE_NUMBER, phoneNumber);
		rec.update();
		*/
	    
        DBRecordBean rec = new DBRecordBean();
        rec.read(context, db.EMPLOYEES, idEmp);
        // Set
        rec.set(db.EMPLOYEES.PHONE_NUMBER, phoneNumber);
        rec.update(context);
	    
	}

    /**
     * <PRE>
     * Updates an employee record by setting the phone number.
     * </PRE>
     */
    private void updatePartialRecord(long idEmp, String phoneNumber)
    {
        // Shortcut for convenience
        SampleDB.Employees EMP = db.EMPLOYEES;
        // Update an Employee with partial record
        // this will only load the EMPLOYEE ID and the PHONE_NUMBER
        DBRecord rec = new DBRecord(context, EMP);
        rec.read(DBRecord.key(idEmp), PartialMode.INCLUDE, EMP.PHONE_NUMBER);
        // Set
        rec.set(db.EMPLOYEES.PHONE_NUMBER, phoneNumber);
        rec.update();
    }

    /**
     * <PRE>
     * Updates an employee record by setting the phone number.
     * </PRE>
     */
    private void updateJoinedRecords(long idEmp, int salary)
    {
        // Shortcuts for convenience
        SampleDB.Employees EMP = db.EMPLOYEES;
        SampleDB.Departments DEP = db.DEPARTMENTS;

        // Create DBQuery from command
        DBCommand cmd = db.createCommand();
        cmd.select(EMP.getColumns());
        cmd.select(DEP.getColumns());
        cmd.join(EMP.DEPARTMENT_ID, DEP.ID);
        DBQuery query = new DBQuery(cmd, EMP.ID);

        // Make employee Head of Department and update salary
        DBRecord rec = new DBRecord(context, query);
        rec.read(idEmp);
        rec.set(EMP.SALARY, salary);
        rec.set(DEP.HEAD, rec.getString(EMP.LAST_NAME));
        rec.update();
    }

	/**
	 * testTransactionCreate
	 * @param context
	 * @param idDep
	 * 
	private int testTransactionCreate(long idDep)
    {
        // Shortcut for convenience
        SampleDB.Employees EMP = db.EMPLOYEES;

        DBRecord rec = new DBRecord(context, EMP);
        rec.create();
        rec.set(EMP.FIRSTNAME, "Foo");
        rec.set(EMP.LASTNAME, "Manchoo");
        rec.set(EMP.GENDER, Gender.M);
        rec.set(EMP.DEPARTMENT_ID, idDep);
        rec.update();
        log.info("Timestamp {}", rec.getString(EMP.UPDATE_TIMESTAMP));
        
        rec.set(EMP.FIRSTNAME, "Foo 2");
        rec.set(EMP.LASTNAME, "Manchu");
        rec.set(EMP.PHONE_NUMBER, "0815/4711");
        rec.update();
        log.info("Timestamp {}", rec.getString(EMP.UPDATE_TIMESTAMP));
        
        context.rollback();
        
        rec.set(EMP.FIRSTNAME, "Dr. Foo");
        rec.update();
        log.info("Timestamp {}", rec.getString(EMP.UPDATE_TIMESTAMP));

        rec.delete();
        
        context.rollback();

        // insert final
        rec.update();
        log.info("Timestamp {}", rec.getString(EMP.UPDATE_TIMESTAMP));
        
        log.info("testTransactionCreate performed OK");
        context.commit();
        
        return rec.getInt(EMP.ID);
    }
     */

    /**
     * @param context
     * @param idDep
     * 
    private void testTransactionUpdate(long idEmp)
    {
        // Shortcut for convenience
        SampleDB.Employees EMP = db.EMPLOYEES;
        
        DBRecord rec = new DBRecord(context, EMP);        
        rec.read(idEmp);
        rec.set(EMP.PHONE_NUMBER, null);
        rec.set(EMP.SALARY, "100.000");
        rec.update();

        log.info("Timestamp {}", rec.getString(EMP.UPDATE_TIMESTAMP));
        
        context.rollback();
        
        rec.set(EMP.PHONE_NUMBER, "07531-45716-0");
        rec.update();

        log.info("Timestamp {}", rec.getString(EMP.UPDATE_TIMESTAMP));
        
        context.rollback();

        rec.update();

        log.info("Timestamp {}", rec.getString(EMP.UPDATE_TIMESTAMP));
        log.info("testTransactionUpdate performed OK");
        context.commit();        
    }
     */

    /**
     * @param context
     * @param idDep
     *
    private void testTransactionDelete(long idEmp)
    {
        // Shortcut for convenience
        SampleDB.Employees T = db.EMPLOYEES;

        DBRecord rec = new DBRecord(context, T);
        rec.read(idEmp);

        // log.info("Timestamp {}", rec.getString(T.UPDATE_TIMESTAMP));
        // rec.set(T.SALARY, "100.001");
        // rec.update();
        // log.info("Timestamp {}", rec.getString(T.UPDATE_TIMESTAMP));
        
        rec.delete();
        
        context.rollback();

        // DBCommand cmd = db.createCommand();
        // cmd.select(T.UPDATE_TIMESTAMP);
        // cmd.where (T.EMPLOYEE_ID.is(idEmp));
        // log.info("Timestamp {}", db.querySingleString(cmd, context.getConnection()));
        
        rec.update();
        
        log.info("Transaction performed OK");        
    }
     */

	/**
	 * <PRE>
	 * Performs an SQL-Query and prints the result to System.out
	 * 
	 * First a DBCommand object is used to create the following SQL-Query (Oracle-Syntax):
     *     
     *   SELECT t2.EMPLOYEE_ID, t2.LASTNAME || ', ' || t2.FIRSTNAME AS FULL_NAME, t2.GENDER, t2.PHONE_NUMBER, 
     *          substr(t2.PHONE_NUMBER, length(t2.PHONE_NUMBER)-instr(reverse(t2.PHONE_NUMBER), '-')+2) AS PHONE_EXTENSION, 
     *          t1.NAME AS DEPARTMENT, t1.BUSINESS_UNIT
     *   FROM EMPLOYEES t2 INNER JOIN DEPARTMENTS t1 ON t1.DEPARTMENT_ID = t2.ID
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
	private void queryExample(QueryType queryType)
    {
        int lastYear = LocalDate.now().getYear()-1;
	    
	    // Define shortcuts for tables used - not necessary but convenient
	    SampleDB.Employees   EMP = db.EMPLOYEES;
	    SampleDB.Departments DEP = db.DEPARTMENTS;
        SampleDB.Payments    PAY = db.PAYMENTS;

	    // The following expression concats lastname + ', ' + firstname
        // DBColumnExpr EMPLOYEE_FULLNAME = EMP.LASTNAME.append(", ").append(EMP.FIRSTNAME).as("FULL_NAME");
        DBColumnExpr EMPLOYEE_FULLNAME = EMP.LAST_NAME.concat(", ", EMP.FIRST_NAME).as("FULL_NAME");
        DBColumnExpr PAYMENTS_LAST_YEAR = PAY.AMOUNT.sum().as("PAYMENTS_LAST_YEAR");
        
        // The following expression extracts the extension number from the phone field
        // e.g. substr(PHONE_NUMBER, length(PHONE_NUMBER)-instr(reverse(PHONE_NUMBER), '-')+2) AS PHONE_EXTENSION
        // Hint: Since the reverse() function is not supported by HSQLDB there is special treatment for HSQL
        DBColumnExpr PHONE_LAST_DASH;
        if ( db.getDbms() instanceof DBMSHandlerHSql 
        		|| db.getDbms() instanceof DBMSHandlerDerby
        		|| db.getDbms() instanceof DBMSHandlerH2)
             PHONE_LAST_DASH = EMP.PHONE_NUMBER.indexOf("-", EMP.PHONE_NUMBER.indexOf("-").plus(1)).plus(1); // HSQLDB only
        else PHONE_LAST_DASH = EMP.PHONE_NUMBER.length().minus(EMP.PHONE_NUMBER.reverse().indexOf("-")).plus(2);  
        DBColumnExpr PHONE_EXT_NUMBER = EMP.PHONE_NUMBER.substring(PHONE_LAST_DASH).as("PHONE_EXTENSION");

        // DBColumnExpr genderExpr = cmd.select(EMP.GENDER.decode(EMP.GENDER.getOptions()).as(EMP.GENDER.getName()));

        // Select Employee and Department columns
        DBCommand cmd = db.createCommand()
           .select(EMP.ID.as("EMPLOYEE_ID"), EMPLOYEE_FULLNAME)
           .select(EMP.GENDER, EMP.PHONE_NUMBER, PHONE_EXT_NUMBER)
           .select(DEP.NAME.as("DEPARTMENT"))
           .select(DEP.BUSINESS_UNIT)
           // Joins
           .join(EMP.DEPARTMENT_ID, DEP.ID)
           .joinLeft(EMP.ID, PAY.EMPLOYEE_ID, PAY.YEAR.is(lastYear))
           // Where constraints
           .where(EMP.LAST_NAME.length().isGreaterThan(0))
           .where(EMP.GENDER.in(Gender.M, Gender.F))
           .where(EMP.RETIRED.is(false))
           // Order by
           .orderBy(EMPLOYEE_FULLNAME);

        // Add payment of last year using a SUM aggregation
        cmd.groupBy(cmd.getSelectExpressions());
        cmd.select(PAYMENTS_LAST_YEAR);

        /*
         * Example for limitRows() and skipRows()
         * Uncomment if you wish
         *
        if (db.getDbms().isSupported(DBMSFeature.QUERY_LIMIT_ROWS))
        {	// set maximum number of rows
        	cmd.limitRows(20);
            if (db.getDbms().isSupported(DBMSFeature.QUERY_SKIP_ROWS))
                cmd.skipRows(1);
        }
        */
        
		// Query Records and print output
		DBReader reader = new DBReader(context);
		try
        {
		    // log select statement (but only once)
		    if (queryType==QueryType.Reader)
		        log.info("Running Query: {}", cmd.getSelect());
		    // Open Reader 
			reader.open(cmd);
			// Print output
			System.out.println("---------------------------------");
			switch(queryType)
			{
			    case Reader:
			        // Text-Output by iterating through all records.
	                while (reader.moveNext())
                    {
	                    System.out.println(reader.getString(EMP.ID)
	                            + "\t" + reader.getString(EMPLOYEE_FULLNAME)
	                            + "\t" + EMP.GENDER.getOptions().get(reader.getString(EMP.GENDER))
                                + "\t" + reader.getString(PHONE_EXT_NUMBER)
	                            + "\t" + reader.getString(DEP.NAME)
	                            + "\t" + reader.getString(PAYMENTS_LAST_YEAR));
	                }
			        break;
                case BeanList:
                    // Text-Output using a list of Java Beans supplied by the DBReader
                    List<EmployeeQuery> beanList = reader.getBeanList(EmployeeQuery.class);
                    // log.info(String.valueOf(beanList.size()) + " SampleBeans returned from Query.");
                    for (EmployeeQuery b : beanList)
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
            System.out.println("---------------------------------");
		} finally  {
			// Always close Reader!
			reader.close();
		}
	}
	
	private void queryBeans()
	{
	    SampleDB.Employees EMP = db.EMPLOYEES;
	    
	    DBCommand cmd = db.createCommand();
	    cmd.where(EMP.GENDER.is(Gender.M));
	    cmd.orderBy(EMP.LAST_NAME.desc());
	    List<Employee> list = context.getUtils().queryBeanList(cmd, Employee.class, null);
	    for (Employee emp : list)
	    {
	        System.out.println(emp.toString());
	    }
	    
	    // load department
	    Department department = context.getUtils().queryBean(Department.class, db.DEPARTMENTS.NAME.is("Sales"));
	    Payment first = department.getEmployees().get(0).getPayments().get(0);
	    log.info("First payment amount is {}", first.getAmount());

        // Query all males
	    BeanResult<Employee> result = new BeanResult<Employee>(Employee.class, EMP);
        result.getCommand().where(EMP.GENDER.is(Gender.M));
	    result.fetch(context);
	    
	    log.info("Number of male employees is: "+result.size());

	    // And now, the females
	    result.getCommand().where(EMP.GENDER.is(Gender.F));
	    result.fetch(context);
	    
        log.info("Number of female employees is: "+result.size());
	}
	
	private void queryDataList()
	{
        int lastYear = LocalDate.now().getYear()-1;
        
        // Define shortcuts for tables used - not necessary but convenient
        SampleDB.Employees   EMP = db.EMPLOYEES;
        SampleDB.Departments DEP = db.DEPARTMENTS;
        SampleDB.Payments    PAY = db.PAYMENTS;

        // Employee total query
        DBColumnExpr EMP_TOTAL = PAY.AMOUNT.sum().as("EMP_TOTAL");
        DBCommand cmdEmpTotal = db.createCommand()
           .select(PAY.EMPLOYEE_ID, EMP_TOTAL)
           .where (PAY.YEAR.is(lastYear))
           .groupBy(PAY.EMPLOYEE_ID);
        DBQuery Q_EMP_TOTAL = new DBQuery(cmdEmpTotal, "qet");
        
        // Department total query
        DBColumnExpr DEP_TOTAL = PAY.AMOUNT.sum().as("DEP_TOTAL");
        DBCommand cmdDepTotal  = db.createCommand()
           .select(EMP.DEPARTMENT_ID, DEP_TOTAL)
           .join  (PAY.EMPLOYEE_ID, EMP.ID)
           .where (PAY.YEAR.is(lastYear))
           .groupBy(EMP.DEPARTMENT_ID);
        DBQuery Q_DEP_TOTAL = new DBQuery(cmdDepTotal, "qdt");

        // Percentage of department
        DBColumnExpr PCT_OF_DEP_COST = Q_EMP_TOTAL.column(EMP_TOTAL).multiplyWith(100).divideBy(Q_DEP_TOTAL.column(DEP_TOTAL));
        // Create the employee query
        DBCommand cmd = db.createCommand()
           .select(EMP.ID, EMP.FIRST_NAME, EMP.LAST_NAME, DEP.NAME.as("DEPARTMENT"))
           .select(Q_EMP_TOTAL.column(EMP_TOTAL))
           .select(PCT_OF_DEP_COST.as("PCT_OF_DEPARTMENT_COST"))
           // join Employee with Department
           .join(EMP.DEPARTMENT_ID, DEP.ID)
           // Join with Subqueries
           .joinLeft(EMP.ID, Q_EMP_TOTAL.column(PAY.EMPLOYEE_ID))
           .joinLeft(DEP.ID, Q_DEP_TOTAL.column(EMP.DEPARTMENT_ID))
           // Order by
           .orderBy(DEP.NAME.desc())
           .orderBy(EMP.LAST_NAME);
	    
        List<DataListEntry> list = context.getUtils().queryDataList(cmd);
        /* uncomment this to print full list
        for (DataListEntry dle : list)
            System.out.println(dle.toString());
        */    
        for (DataListEntry dle : list)
        {
            long empId = dle.getRecordId(EMP);
            // int depId = dle.getId(DEP);
            String empName = StringUtils.concat(", ", dle.getString(EMP.LAST_NAME), dle.getString(EMP.FIRST_NAME));
            String depName = dle.getString(DEP.NAME);
            boolean hasPayments =!dle.isNull(Q_EMP_TOTAL.column(EMP_TOTAL));
            if (hasPayments)
            {   // report
                BigDecimal empTotal = dle.getDecimal(Q_EMP_TOTAL.column(EMP_TOTAL));
                BigDecimal pctOfDep = dle.getDecimal(PCT_OF_DEP_COST).setScale(1, RoundingMode.HALF_UP);
                log.info("Eployee[{}]: {}\tDepartment: {}\tPayments: {} ({}% of Department)", empId, empName, depName, empTotal, pctOfDep);
            }
            else
                log.info("Eployee[{}]: {}\tDepartment: {}\tPayments: [No data avaiable]", empId, empName, depName);
        }
        
        /*
        cmd.where(EMP.ID.is(list.get(0).getRecordId(EMP)));
        DataListEntry emp1 = context.getUtils().queryDataEntry(cmd);
        System.out.println(emp1.toString());

        cmd.where(EMP.ID.is(list.get(1).getRecordId(EMP)));
        DataListEntry emp2 = context.getUtils().queryDataEntry(cmd);
        System.out.println(emp2.toString());
        */
	}

	private void queryRecordList()
	{
        SampleDB.Departments DEP = db.DEPARTMENTS;
        SampleDB.Employees EMP = db.EMPLOYEES;
        /*
         * Test RecordList
         */
        DBCommand cmd = db.createCommand();
        cmd.join(EMP.DEPARTMENT_ID, DEP.ID);
        cmd.where(DEP.NAME.is("Development"));
        // query now
        List<DBRecordBean> list = context.getUtils().queryRecordList(cmd, EMP, DBRecordBean.class);
        log.info("RecordList query found {} employees in Development department", list.size());
        for (DBRecordBean record : list)
        {
            Object[] key = record.getKey();
            // print info
            String empName = StringUtils.concat(", ", record.getString(EMP.LAST_NAME), record.getString(EMP.FIRST_NAME));
            String phone   = record.getString(EMP.PHONE_NUMBER);
            BigDecimal salary = record.getDecimal(EMP.SALARY);
            log.info("Eployee[{}]: {}\tPhone: {}\tSalary: {}", StringUtils.toString(key), empName, phone, salary);
            // modify salary
            BigDecimal newSalary = new BigDecimal(2000 + ((Math.random()*200) - 100.0));
            record.set(EMP.SALARY, newSalary);
            // check
            if (record.wasModified(EMP.SALARY))
            {   // Salary was modified
                log.info("Salary was modified for {}. New salary is {}", empName, record.getDecimal(EMP.SALARY));
            }
            // udpate the record
            record.update(context);
            
            // convert to bean
            Employee employee = new Employee();
            record.setBeanProperties(employee);
            System.out.println(employee.toString());
        }
	}
}
