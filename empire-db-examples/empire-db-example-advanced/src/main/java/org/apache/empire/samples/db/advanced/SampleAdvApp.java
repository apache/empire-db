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
package org.apache.empire.samples.db.advanced;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Date;
import java.util.HashMap;

import org.apache.empire.commons.DateUtils;
import org.apache.empire.commons.Options;
import org.apache.empire.commons.StringUtils;
import org.apache.empire.data.DataType;
import org.apache.empire.db.DBCmdParam;
import org.apache.empire.db.DBColumnExpr;
import org.apache.empire.db.DBCommand;
import org.apache.empire.db.DBContext;
import org.apache.empire.db.DBDDLGenerator.DDLActionType;
import org.apache.empire.db.DBQuery;
import org.apache.empire.db.DBReader;
import org.apache.empire.db.DBRecord;
import org.apache.empire.db.DBSQLScript;
import org.apache.empire.db.DBTableColumn;
import org.apache.empire.db.context.DBContextStatic;
import org.apache.empire.db.exceptions.ConstraintViolationException;
import org.apache.empire.db.exceptions.StatementFailedException;
import org.apache.empire.dbms.DBMSHandler;
import org.apache.empire.dbms.h2.DBMSHandlerH2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SampleAdvApp 
{
    // Logger
    private static final Logger log = LoggerFactory.getLogger(SampleAdvApp.class);
    
    private static SampleAdvConfig config = new SampleAdvConfig();

    private final SampleAdvDB db = new SampleAdvDB();

    private final CarSalesDB carSales = new CarSalesDB();
    
    private DBContext context;
    
    // Shortcuts
    private SampleAdvDB.Employees T_EMP = db.T_EMPLOYEES;
    private SampleAdvDB.Departments T_DEP = db.T_DEPARTMENTS;
    private SampleAdvDB.EmployeeDepartmentHistory T_EDH = db.T_EMP_DEP_HIST;

    /**
     * <PRE>
     * This is the entry point of the Empire-DB Sample Application
     * Please check the config.xml configuration file for Database and Connection settings.
     * </PRE>
     * @param args arguments
     */
    public static void main(String[] args)
    {
        // Init Configuration
        config.init((args.length > 0 ? args[0] : "config.xml" ));
        // create the app
        SampleAdvApp app = new SampleAdvApp();
        try
        {   // Run
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
        // STEP 1: Get a JDBC Connection
        System.out.println("*** Step 1: getJDBCConnection() ***");
        Connection conn = getJDBCConnection();

        // STEP 2: Choose a dbms
        System.out.println("*** Step 2: getDatabaseProvider() ***");
        DBMSHandler dbms = getDBMSHandler(config.getDatabaseProvider());
        
        // STEP 2.2: Create a Context
        context = new DBContextStatic(dbms, conn); 

        // STEP 3: Open Database (and create if not existing)
        System.out.println("*** Step 3: openDatabase() ***");
        db.open(context);
        carSales.open(context);
        carSales.queryDemo(context);
        carSales.updateDemo(context);

        // STEP 5: Clear Database (Delete all records)
        System.out.println("*** Step 5: clearDatabase() ***");
        clearDatabase();

        // STEP 6: Insert Records
        // Insert Departments
        System.out.println("*** Step 6: inserting departments, employees and employee_department_history records ***");
        int idDevDep  = insertDepartment("Development", "ITTK");
        int idProdDep = insertDepartment("Production", "ITTK");
        int idSalDep  = insertDepartment("Sales", "ITTK");

        // Insert Employees
        int idEmp1 = insertEmployee("Peter", "Sharp", "M");
        int idEmp2 = insertEmployee("Fred", "Bloggs", "M");
        int idEmp3 = insertEmployee("Emma", "White", "F");
        
        // Insert History as batch
        DBSQLScript batch = new DBSQLScript(context);
        insertEmpDepHistory(batch, idEmp1,  idDevDep,  DateUtils.getDate(2007, 12,  1));            
        insertEmpDepHistory(batch, idEmp1,  idProdDep, DateUtils.getDate(2008,  9,  1));           
        insertEmpDepHistory(batch, idEmp1,  idSalDep,  DateUtils.getDate(2009,  5, 15));           

        insertEmpDepHistory(batch, idEmp2,  idSalDep,  DateUtils.getDate(2006,  3,  1));            
        insertEmpDepHistory(batch, idEmp2,  idDevDep,  DateUtils.getDate(2008, 11, 15));
        
        insertEmpDepHistory(batch, idEmp3,  idDevDep,  DateUtils.getDate(2006,  9, 15));            
        insertEmpDepHistory(batch, idEmp3,  idSalDep,  DateUtils.getDate(2007,  6,  1));           
        insertEmpDepHistory(batch, idEmp3,  idProdDep, DateUtils.getDate(2008,  7, 31));
        batch.executeBatch();
        
        // commit
        context.commit();
        
        // STEP 7: read from Employee_Info_View
        System.out.println("--------------------------------------------------------");
        System.out.println("*** read from EMPLOYEE_INFO_VIEW ***");
        DBCommand cmd = db.createCommand();
        cmd.select (db.V_EMPLOYEE_INFO.getColumns());
        cmd.orderBy(db.V_EMPLOYEE_INFO.C_NAME_AND_DEP);
        printQueryResults(cmd);

        // STEP 8: prepared Statement sample
        System.out.println("--------------------------------------------------------");
        System.out.println("*** commandParamsSample: shows how to use command parameters for the generation of prepared statements ***");
        commandParamsSample(idProdDep, idDevDep);

        // STEP 9: bulkReadRecords
        System.out.println("--------------------------------------------------------");
        System.out.println("*** bulkReadRecords: reads employee records into a hashmap, reads employee from hashmap and updates employee ***");
        HashMap<Integer, DBRecord> employeeMap = bulkReadRecords(conn);
        DBRecord rec = employeeMap.get(idEmp2);
        rec.set(db.T_EMPLOYEES.C_SALUTATION, "Mr.");
        rec.update();

        // STEP 10: bulkProcessRecords
        System.out.println("--------------------------------------------------------");
        System.out.println("*** bulkProcessRecords: creates a checksum for every employee in the employees table ***");
        bulkProcessRecords();

        // STEP 11: querySample
        System.out.println("--------------------------------------------------------");
        System.out.println("*** querySample: shows how to use DBQuery class for subqueries and multi table records ***");
        querySample(idEmp2);

        // STEP 12: ddlSample
        System.out.println("--------------------------------------------------------");
        System.out.println("*** ddlSample: shows how to add a column at runtime and update a record with the added column ***");
        if (db.getDbms() instanceof DBMSHandlerH2) {
        	log.info("As H2 does not support changing a table with a view defined we remove the view");
        	System.out.println("*** drop EMPLOYEE_INFO_VIEW ***");
        	DBSQLScript script = new DBSQLScript(context);
        	db.getDbms().getDDLScript(DDLActionType.DROP, db.V_EMPLOYEE_INFO, script);
        	script.executeAll();
        }
        ddlSample(idEmp2);
        if (db.getDbms() instanceof DBMSHandlerH2) {
        	log.info("And put back the view");
        	System.out.println("*** create EMPLOYEE_INFO_VIEW ***");
        	DBSQLScript script = new DBSQLScript(context);
        	db.getDbms().getDDLScript(DDLActionType.CREATE, db.V_EMPLOYEE_INFO, script);
        	script.executeAll();
        }

        // STEP 13: delete records
        System.out.println("--------------------------------------------------------");
        System.out.println("*** deleteRecordSample: shows how to delete records (with and without cascade) ***");
        deleteRecordSample(idEmp3, idSalDep);
        
        // Done
        System.out.println("--------------------------------------------------------");
        System.out.println("DB Sample Advanced finished successfully.");
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
        try
        {
            // Connect to the database
            String jdbcDriverClass = config.getJdbcClass();
            log.info("Creating JDBC-Driver of type \"{}\"", jdbcDriverClass);
            Class.forName(jdbcDriverClass).newInstance();

            log.info("Connecting to Database'" + config.getJdbcURL() + "' / User=" + config.getJdbcUser());
            conn = DriverManager.getConnection(config.getJdbcURL(), config.getJdbcUser(), config.getJdbcPwd());
            log.info("Connected successfully");
            // set the AutoCommit to false this session. You must commit
            // explicitly now
            conn.setAutoCommit(false);
            log.info("AutoCommit is " + conn.getAutoCommit());

        } catch (Exception e)
        {
            log.error("Failed to connect to '" + config.getJdbcURL() + "' / User=" + config.getJdbcUser());
            log.error(e.toString());
            throw new RuntimeException(e);
        }
        return conn;
    }

    /**
     * Creates an Empire-db DatabaseDriver for the given provider and applies dbms specific configuration 
     */
    private static DBMSHandler getDBMSHandler(String provider)
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
     * Empties all Tables.
     * </PRE>
     */
    private void clearDatabase()
    {
        DBCommand cmd = context.createCommand(db);
        // Delete all Employee Department History records
        context.executeDelete(T_EDH, cmd);
        // Delete all Employees (no constraints)
        context.executeDelete(T_EMP, cmd);
        // Delete all Departments (no constraints)
        context.executeDelete(T_DEP, cmd);
    }

    /**
     * <PRE>
     * Insert a Department into the Departments table.
     * </PRE>
     */
    private int insertDepartment(String departmentName, String businessUnit)
    {
        // Insert a Department
        DBRecord rec = new DBRecord(context, T_DEP);
        rec.create();
        rec.set(T_DEP.C_NAME, departmentName);
        rec.set(T_DEP.C_BUSINESS_UNIT, businessUnit);
        rec.update();
        // Return Department ID
        return rec.getInt(T_DEP.C_DEPARTMENT_ID);
    }

    /**
     * <PRE>
     * Inserts an Employee into the Employees table.
     * </PRE>
     */
    private int insertEmployee(String firstName, String lastName, String gender)
    {
        // Insert an Employee
        DBRecord rec = new DBRecord(context, T_EMP);
        rec.create();
        rec.set(T_EMP.C_FIRSTNAME, firstName);
        rec.set(T_EMP.C_LASTNAME, lastName);
        rec.set(T_EMP.C_GENDER, gender);
        rec.update();
        // Return Employee ID
        return rec.getInt(T_EMP.C_EMPLOYEE_ID);
    }

    /**
     * <PRE>
     * Inserts an Employee into the Employees table.
     * </PRE>
     */
    private void insertEmpDepHistory(DBSQLScript script, int employeeId, int departmentId, Date dateFrom)
    {
        // Insert an Employee
    	/*
        DBRecord rec = new DBRecord(context);
        rec.create(T_EDH);
        rec.setValue(T_EDH.C_EMPLOYEE_ID, employeeId);
        rec.setValue(T_EDH.C_DEPARTMENT_ID, departmentId);
        rec.setValue(T_EDH.C_DATE_FROM, dateFrom);
        rec.update();
        */
        DBCommand cmd = db.createCommand();
    	cmd.set(T_EDH.C_EMPLOYEE_ID.to(employeeId));
    	cmd.set(T_EDH.C_DEPARTMENT_ID.to(departmentId));
    	cmd.set(T_EDH.C_DATE_FROM.to(dateFrom));
    	// Add to script for batch execution
    	script.addInsert(cmd);
    }

    /* This procedure demonstrates the use of command parameter for prepared statements */
    private void commandParamsSample(int idProdDep, int idDevDep)
    {
        // create a command
        DBCommand cmd = db.createCommand();
        // Create cmd parameters
        DBCmdParam curDepParam = cmd.addParam(); // Current Department
        DBCmdParam genderParam = cmd.addParam(); // Gender ('M' or 'F')
        // Define the query
        cmd.select(T_EMP.C_FULLNAME);
        cmd.join  (T_EMP.C_EMPLOYEE_ID, db.V_EMPLOYEE_INFO.C_EMPLOYEE_ID);
        cmd.where (T_EMP.C_GENDER.is(genderParam));
        cmd.where (db.V_EMPLOYEE_INFO.C_CURRENT_DEP_ID.is(curDepParam));

        System.out.println("Perfoming two queries using a the same command with different parameter values.");
        
        DBReader r = new DBReader(context);
        try {
            // Query all females currently working in the Production department
            System.out.println("1. Query all females currently working in the production department");
            // Set command parameter values
            genderParam.setValue('F'); // set gender to female
            curDepParam.setValue(idProdDep); // set department id to production department
            // Open reader using a prepared statement (due to command parameters!)
            r.open(cmd);
            // print all results
            System.out.println("Females working in the production department are:");
            while (r.moveNext())
                System.out.println("    " + r.getString(T_EMP.C_FULLNAME));
            r.close();   

            // Second query
            // Now query all males currently working in the development department
            System.out.println("2. Query all males currently working in the development department");
            // Set command parameter values
            genderParam.setValue('M'); // set gender to female
            curDepParam.setValue(idDevDep); // set department id to production department
            // Open reader using a prepared statement (due to command parameters!)
            r.open(cmd);
            // print all results
            System.out.println("Males currently working in the development department are:");
            while (r.moveNext())
                System.out.println("    " + r.getString(T_EMP.C_FULLNAME));

        } finally {
            r.close();
        }
        
    }

    /**
     * This function performs a query to select non-retired employees,<BR>
     * then it calculates a checksum for every record<BR>
     * and writes that checksum back to the database.<BR>
     * <P>
     * @param conn a connection to the database
     */
    private void bulkProcessRecords()
    {
        // Define the query
        DBCommand cmd = db.createCommand();
        // Define shortcuts for tables used - not necessary but convenient
        SampleAdvDB.Employees EMP = T_EMP;
        // Select required columns
        cmd.select(T_EMP.getColumns());
        // Set Constraints
        cmd.where(T_EMP.C_RETIRED.is(false));

        // Query Records and print output
        DBReader reader = new DBReader(context);
        try
        {
            // Open Reader
            System.out.println("Running Query:");
            System.out.println(cmd.getSelect());
            reader.open(cmd);
            // Print output
            DBRecord record = new DBRecord(context, EMP);
            // Disable rollback handling to improve performance
            record.setRollbackHandlingEnabled(false);
            while (reader.moveNext())
            {
                // Calculate sum
                int sum = 0;
                for (int i=0; i<reader.getFieldCount(); i++)
                    sum += calcCharSum(reader.getString(i));
                // Init updateable record
                reader.initRecord(record);
                // reader
                record.set(T_EMP.C_CHECKSUM, sum);
                record.update();
            }
            // Done
            context.commit();

        } finally
        {
            // always close Reader
            reader.close();
        }
    }
    
	private int calcCharSum(String value)
    {
        int sum = 0;
        if (value!=null)
        {	// calcCharSum
            int len = value.length();
            for (int i=0; i<len; i++)
                sum += value.charAt(i);
        }
        return sum;    
    }
	
    private HashMap<Integer, DBRecord> bulkReadRecords(Connection conn)
    {
        // Define the query
        DBCommand cmd = db.createCommand();
        // Select required columns
        cmd.select(T_EMP.getColumns());
        // Set Constraints
        cmd.where(T_EMP.C_RETIRED.is(false));

        // Query Records and print output
        DBReader reader = new DBReader(context);
        try
        {   // Open Reader
            System.out.println("Running Query:");
            System.out.println(cmd.getSelect());
            reader.open(cmd);
            // Print output
            HashMap<Integer, DBRecord> employeeMap = new HashMap<Integer, DBRecord>();
            while (reader.moveNext())
            {
                DBRecord rec = new DBRecord(context, T_EMP);
                reader.initRecord(rec);
                employeeMap.put(reader.getInt(T_EMP.C_EMPLOYEE_ID), rec);
            }
            return employeeMap;

        } finally
        {   // always close Reader
            reader.close();
        }
    }
    
    /**
     * This method demonstrates how to add, modify and delete a database column.<BR>
     * This function demonstrates the use of the {@link DBMSHandler#getDDLScript(org.apache.empire.db.DDLActionType, org.apache.empire.db.DBObject, DBSQLScript)}<BR>
     * 
     */
    private void ddlSample(int idTestPerson)
    {
        // First, add a new column to the Table object
        DBTableColumn C_FOO = db.T_EMPLOYEES.addColumn("FOO", DataType.VARCHAR, 20, false);

        // Now create the corresponding DDL statement 
        System.out.println("Creating new column named FOO as varchar(20) for the EMPLOYEES table:");
        DBSQLScript script = new DBSQLScript(context);
        db.getDbms().getDDLScript(DDLActionType.CREATE, C_FOO, script);
        script.executeAll();
        
        // Now load a record from that table and set the value for foo
        System.out.println("Changing the value for the FOO field of a particular employee:");
        DBRecord rec = new DBRecord(context, db.T_EMPLOYEES);
        rec.read(idTestPerson);
        rec.set(C_FOO, "Hello World");
        rec.update();
        
        // Now extend the size of the field from 20 to 40 characters
        System.out.println("Extending size of column FOO to 40 characters:");
        C_FOO.setSize(40); 
        script.clear();
        db.getDbms().getDDLScript(DDLActionType.ALTER, C_FOO, script);
        script.executeAll();

        // Now set a longer value for the record
        System.out.println("Changing the value for the FOO field for the above employee to a longer string:");
        rec.set(C_FOO, "This is a very long field value!");
        rec.update();

        // Finally, drop the column again
        System.out.println("Dropping the FOO column from the employee table:");
        script.clear();
        db.getDbms().getDDLScript(DDLActionType.DROP, C_FOO, script);
        script.executeAll();
    }

    /**
     * This function demonstrates the use of the DBQuery object.<BR>
     * First a DBQuery is used to define a subquery that gets the latest employee department history record.<BR>
     * This subquery is then used inside another query to list all employees with the current department.<BR>
     * <P>
     * In the second part, another DBQuery object is used to read a record that holds information from both 
     * the employee and department table. When the information is modified and the record's update method is
     * called, then both tables are updated.
     * <P>
     * @param conn
     * @param employeeId
     */
    private void querySample(int employeeId)
    {
        // Define the sub query
        DBCommand subCmd = db.createCommand();
        DBColumnExpr MAX_DATE_FROM = T_EDH.C_DATE_FROM.max().as(T_EDH.C_DATE_FROM);
        subCmd.select(T_EDH.C_EMPLOYEE_ID, MAX_DATE_FROM);
        subCmd.groupBy(T_EDH.C_EMPLOYEE_ID);
        DBQuery Q_MAX_DATE = new DBQuery(subCmd);

        // Define the query
        DBCommand cmd = db.createCommand();
        // Select required columns
        cmd.select(T_EMP.C_EMPLOYEE_ID, T_EMP.C_FULLNAME);
        cmd.select(T_EMP.C_GENDER, T_EMP.C_PHONE_NUMBER);
        cmd.select(T_DEP.C_DEPARTMENT_ID, T_DEP.C_NAME, T_DEP.C_BUSINESS_UNIT);
        cmd.select(T_EMP.C_UPDATE_TIMESTAMP, T_DEP.C_UPDATE_TIMESTAMP);
        // Set Joins
        cmd.join(T_EDH.C_EMPLOYEE_ID, Q_MAX_DATE.column(T_EDH.C_EMPLOYEE_ID),
                 T_EDH.C_DATE_FROM.is(Q_MAX_DATE.column(MAX_DATE_FROM)));
        cmd.join(T_EMP.C_EMPLOYEE_ID, T_EDH.C_EMPLOYEE_ID);
        cmd.join(T_DEP.C_DEPARTMENT_ID, T_EDH.C_DEPARTMENT_ID);
        // Set Constraints
        cmd.where(T_EMP.C_RETIRED.is(false));
        // Set Order
        cmd.orderBy(T_EMP.C_LASTNAME);
        cmd.orderBy(T_EMP.C_FIRSTNAME);

        // Query Records and print output
        printQueryResults(cmd);
        
        // Define an updateable query
        DBQuery Q_EMP_DEP = new DBQuery(cmd, T_EMP.C_EMPLOYEE_ID);
        DBRecord rec = new DBRecord(context, Q_EMP_DEP);
        rec.read(employeeId);
        // Modify and Update fields from both Employee and Department
        rec.set(T_EMP.C_PHONE_NUMBER, "0815-4711");
        rec.set(T_DEP.C_BUSINESS_UNIT, "AUTO");
        rec.update();
        // Successfully updated
        System.out.println("The employee has been sucessfully updated");
    }    
    
    /**
     * This function demonstrates cascaded deletes.
     * See DBRelation.setOnDeleteAction()
     *  
     * @param idEmployee the id of the employee to delete
     * @param idDepartment the id of the department to delete
     * @param conn the connection
     */
    private void deleteRecordSample(int idEmployee, int idDepartment)
    {
        context.commit();
        // Delete an employee
        // This statement is designed to succeed since cascaded deletes are enabled for this relation.
        db.T_EMPLOYEES.deleteRecord(idEmployee, context);
        System.out.println("The employee has been sucessfully deleted");

        // Delete a department
        // This statement is designed to fail since cascaded deletes are not on!
        try {
            db.T_DEPARTMENTS.deleteRecord(idDepartment, context);
        } catch(ConstraintViolationException e) {
            System.out.println("Delete of department failed as expected due to existing depending records.");
        } catch(StatementFailedException e) {
            // Oops, the driver threw a SQLException instead
            System.out.println("Delete of department failed as expected due to existing depending records.");
        }
    }

    /**
     * This functions prints the results of a query which is performed using the supplied command
     * @param cmd the command to be used for performing the query
     * @param conn the connection
     */
    private void printQueryResults(DBCommand cmd)
    {
        // Query Records and print output
        DBReader reader = new DBReader(context);
        try
        {   // Open Reader
            System.out.println("Running Query:");
            System.out.println(cmd.getSelect());
            reader.open(cmd);
            // Print column titles 
            System.out.println("---------------------------------");
            int count = reader.getFieldCount();
            for (int i=0; i<count; i++)
            {   // Print all column names
                DBColumnExpr c = reader.getColumn(i);
                if (i>0)
                    System.out.print("\t");
                System.out.print(c.getName());
            }
            // Print output
            System.out.println("");
            // Text-Output by iterating through all records.
            while (reader.moveNext())
            {
                for (int i=0; i<count; i++)
                {   // Print all field values
                    if (i>0)
                        System.out.print("\t");
                    // Check if conversion is necessary
                    DBColumnExpr c = reader.getColumn(i);
                    Options opt = c.getOptions();
                    if (opt!=null)
                    {   // Option Lookup
                        System.out.print(opt.get(reader.getValue(i)));
                    }
                    else
                    {   // Print String
                        System.out.print(reader.getString(i));
                    }
                }
                System.out.println("");
            }

        } finally
        {   // always close Reader
            reader.close();
        }
    }
}
