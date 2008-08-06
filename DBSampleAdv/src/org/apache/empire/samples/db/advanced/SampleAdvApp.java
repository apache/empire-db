package org.apache.empire.samples.db.advanced;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Date;
import java.util.HashMap;
import java.util.logging.Logger;

import org.apache.empire.commons.DateUtils;
import org.apache.empire.commons.ErrorObject;
import org.apache.empire.commons.Options;
import org.apache.empire.data.DataType;
import org.apache.empire.db.DBCmdType;
import org.apache.empire.db.DBColumnExpr;
import org.apache.empire.db.DBCommand;
import org.apache.empire.db.DBDatabaseDriver;
import org.apache.empire.db.DBQuery;
import org.apache.empire.db.DBReader;
import org.apache.empire.db.DBRecord;
import org.apache.empire.db.DBSQLScript;
import org.apache.empire.db.DBTableColumn;
import org.apache.empire.db.hsql.DBDatabaseDriverHSql;
import org.apache.empire.db.mysql.DBDatabaseDriverMySQL;
import org.apache.empire.db.oracle.DBDatabaseDriverOracle;
import org.apache.empire.db.sqlserver.DBDatabaseDriverMSSQL;


public class SampleAdvApp 
{
    public static Logger logger = Logger.getLogger(SampleAdvApp.class.getName());

    private static final SampleAdvDB db = new SampleAdvDB();

    private static SampleAdvConfig config = new SampleAdvConfig();
    
    // Shortcuts
    private static SampleAdvDB.Employees T_EMP = db.T_EMPLOYEES;
    private static SampleAdvDB.Departments T_DEP = db.T_DEPARTMENTS;
    private static SampleAdvDB.EmployeeDepartmentHistory T_EDH = db.T_EMP_DEP_HIST;

    /**
     * <PRE>
     * This is the entry point of the Empire-DB Sample Application
     * Please check the config.xml configuration file for Database and Connection settings.
     * </PRE>
     */
    public static void main(String[] args)
    {
        try
        {

            // Init Configuration
            config.init((args.length > 0 ? args[0] : "config.xml" ));

            // Enable Exceptions
            ErrorObject.setExceptionsEnabled(true);

            System.out.println("Running DB Sample Advanced...");

            // STEP 1: Get a JDBC Connection
            System.out.println("*** Step 1: getJDBCConnection() ***");
            Connection conn = getJDBCConnection();

            // STEP 2: Choose a driver
            System.out.println("*** Step 2: getDatabaseProvider() ***");
            DBDatabaseDriver driver = getDatabaseDriver(config.getDatabaseProvider());

            // STEP 3: Open Database (and create if not existing)
            System.out.println("*** Step 3: openDatabase() ***");
            try {
                db.open(driver, conn);
                databaseExists(conn);
                System.out.println("*** Database already exists. Skipping Step4 ***");
                
            } catch(Exception e) {
                // STEP 4: Create Database
                System.out.println("*** Step 4: createDDL() ***");
                createDatabase(driver, conn);
                // Open again
                if (db.isOpen()==false)
                    db.open(driver, conn);
            }

            // STEP 5: Clear Database (Delete all records)
            System.out.println("*** Step 5: clearDatabase() ***");
            clearDatabase(conn);

            // STEP 6: Insert Records
            // Insert Departments
            System.out.println("*** Step 6: inserting departments, employees and employee_department_history records ***");
            int idDevDep = insertDepartment(conn, "Development", "ITTK");
            int idProdDep = insertDepartment(conn, "Production", "ITTK");
            int idSalDep = insertDepartment(conn, "Sales", "ITTK");

            // Insert Employees
            int idPers1 = insertEmployee(conn, "Peter", "Sharp", "M");
            int idPers2 = insertEmployee(conn, "Fred", "Bloggs", "M");
            int idPers3 = insertEmployee(conn, "Emma", "White", "F");
            
            insertEmpDepHistory(conn, idPers1,  idDevDep,  DateUtils.getDate(2005, 11,  1));            
            insertEmpDepHistory(conn, idPers1,  idProdDep, DateUtils.getDate(2006,  8,  1));           
            insertEmpDepHistory(conn, idPers1,  idSalDep,  DateUtils.getDate(2007,  4, 15));           

            insertEmpDepHistory(conn, idPers2,  idSalDep,  DateUtils.getDate(2004,  2,  1));            
            insertEmpDepHistory(conn, idPers2,  idDevDep,  DateUtils.getDate(2006, 10, 15));           

            insertEmpDepHistory(conn, idPers3,  idDevDep,  DateUtils.getDate(2004,  8, 15));            
            insertEmpDepHistory(conn, idPers3,  idSalDep,  DateUtils.getDate(2005,  7,  1));           
            insertEmpDepHistory(conn, idPers3,  idProdDep, DateUtils.getDate(2006,  6, 15));           
            
            // commit
            db.commit(conn);

            // STEP 7: read from Employee_Info_View
            if (true) {
                System.out.println("*** read from EMPLOYEE_INFO_VIEW ***");
                DBCommand cmd = db.createCommand();
                cmd.select (db.V_EMPLOYEE_INFO.getColumns());
                cmd.orderBy(db.V_EMPLOYEE_INFO.C_NAME_AND_DEP);
                printQueryResults(cmd, conn);
            }
            // STEP 8: bulkReadRecords
            if (true) {
                System.out.println("*** bulkReadRecords: reads employee records into a hashmap, reads employee from hashmap and updates employee ***");
                HashMap<Integer, DBRecord> employeeMap = bulkReadRecords(conn);
                DBRecord rec = employeeMap.get(idPers2);
                rec.setValue(db.T_EMPLOYEES.C_SALUTATION, "Mr.");
                rec.update(conn);
            }
            // STEP 9: bulkProcessRecords
            if (true) {
                System.out.println("*** bulkProcessRecords: creates a checksum for every employee in the employees table ***");
                bulkProcessRecords(conn);
            }
            // STEP 10: querySample
            if (true) {
                System.out.println("*** querySample: shows how to use DBQuery class for subqueries and multi table records ***");
                querySample(conn, idPers2);
            }
            // STEP 11: ddlSample
            if (true) {
                System.out.println("*** ddlSample: shows how to add a column at runtime and update a record with the added column ***");
                ddlSample(conn, idPers2);                
            }

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
            conn.setAutoCommit(false);
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
    private static DBDatabaseDriver getDatabaseDriver(String provider)
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
        cmd.select(T_DEP.count());
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
        // Delete all Employee Department History records
        db.executeSQL(cmd.getDelete(T_EDH), conn);
        // Delete all Employees (no constraints)
        db.executeSQL(cmd.getDelete(T_EMP), conn);
        // Delete all Departments (no constraints)
        db.executeSQL(cmd.getDelete(T_DEP), conn);
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
        rec.create(T_DEP);
        rec.setValue(T_DEP.C_NAME, departmentName);
        rec.setValue(T_DEP.C_BUSINESS_UNIT, businessUnit);
        if (!rec.update(conn))
        {
            logger.severe(rec.getErrorMessage());
            return 0;
        }
        // Return Department ID
        return rec.getInt(T_DEP.C_DEPARTMENT_ID);
    }

    /**
     * <PRE>
     * Inserts an Employee into the Employees table.
     * </PRE>
     */
    private static int insertEmployee(Connection conn, String firstName, String lastName, String gender)
    {
        // Insert an Employee
        DBRecord rec = new DBRecord();
        rec.create(T_EMP);
        rec.setValue(T_EMP.C_FIRSTNAME, firstName);
        rec.setValue(T_EMP.C_LASTNAME, lastName);
        rec.setValue(T_EMP.C_GENDER, gender);
        if (!rec.update(conn))
        {
            logger.severe(rec.getErrorMessage());
            return 0;
        }
        // Return Employee ID
        return rec.getInt(T_EMP.C_EMPLOYEE_ID);
    }

    /**
     * <PRE>
     * Inserts an Employee into the Employees table.
     * </PRE>
     */
    private static void insertEmpDepHistory(Connection conn, int employeeId, int departmentId, Date dateFrom)
    {
        // Insert an Employee
        DBRecord rec = new DBRecord();
        rec.create(T_EDH);
        rec.setValue(T_EDH.C_EMPLOYEE_ID, employeeId);
        rec.setValue(T_EDH.C_DEPARTMENT_ID, departmentId);
        rec.setValue(T_EDH.C_DATE_FROM, dateFrom);
        if (!rec.update(conn))
        {
            logger.severe(rec.getErrorMessage());
        }
    }

    /**
     * This function performs a query to select non-retired employees,<BR>
     * then it calculates a checksum for every record<BR>
     * and writes that checksum back to the database.<BR>
     * <P>
     * @param conn a connection to the database
     */
    private static void bulkProcessRecords(Connection conn)
    {
        // Define the query
        DBCommand cmd = db.createCommand();
        // Define shortcuts for tables used - not necessary but convenient
        SampleAdvDB.Employees EMP = T_EMP;
        // Select requried columns
        cmd.select(T_EMP.getColumns());
        // Set Constraints
        cmd.where(T_EMP.C_RETIRED.is(false));

        // Query Records and print output
        DBReader reader = new DBReader();
        try
        {
            // Open Reader
            System.out.println("Running Query:");
            System.out.println(cmd.getSelect());
            if (reader.open(cmd, conn) == false)
                throw new RuntimeException(reader.getErrorMessage());
            // Print output
            DBRecord record = new DBRecord();
            while (reader.moveNext())
            {
                // Calculate sum
                int sum = 0;
                for (int i=0; i<reader.getFieldCount(); i++)
                    sum += calcCharSum(reader.getString(i));
                // Init updateable record
                reader.initRecord(EMP, record);
                // reader
                record.setValue(T_EMP.C_CHECKSUM, sum);
                record.update(conn);
            }
            // Done
            db.commit(conn);

        } finally
        {
            // always close Reader
            reader.close();
        }
    }
    
	private static int calcCharSum(String value)
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
    
    private static HashMap<Integer, DBRecord> bulkReadRecords(Connection conn)
    {
        // Define the query
        DBCommand cmd = db.createCommand();
        // Select requried columns
        cmd.select(T_EMP.getColumns());
        // Set Constraints
        cmd.where(T_EMP.C_RETIRED.is(false));

        // Query Records and print output
        DBReader reader = new DBReader();
        try
        {   // Open Reader
            System.out.println("Running Query:");
            System.out.println(cmd.getSelect());
            if (reader.open(cmd, conn) == false)
                throw new RuntimeException(reader.getErrorMessage());
            // Print output
            HashMap<Integer, DBRecord> employeeMap = new HashMap<Integer, DBRecord>();
            while (reader.moveNext())
            {
                DBRecord rec = new DBRecord();
                reader.initRecord(T_EMP, rec);
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
     * This function demonstrates the use of the {@link DBDatabaseDriver#getDDLScript(org.apache.empire.db.DBCmdType, org.apache.empire.db.DBObject, DBSQLScript)}<BR>
     * 
     */
    private static void ddlSample(Connection conn, int idTestPerson)
    {
        // Enable Column default for the database 
        // This is needed for adding required fields to non-empty tables
        db.getDriver().setDDLColumnDefaults(true);

        // First, add a new column to the Table object
        DBTableColumn C_FOO = db.T_EMPLOYEES.addColumn("FOO", DataType.TEXT, 20, false);

        // Now create the corresponding DDL statement 
        System.out.println("Creating new column named FOO as varchar(20) for the EMPLOYEES table:");
        DBSQLScript script = new DBSQLScript();
        db.getDriver().getDDLScript(DBCmdType.CREATE, C_FOO, script);
        script.run(db.getDriver(), conn, false);
        
        // Now load a record from that table and set the value for foo
        System.out.println("Changing the value for the FOO field of a particular employee:");
        DBRecord rec = new DBRecord();
        rec.read(db.T_EMPLOYEES, idTestPerson, conn);
        rec.setValue(C_FOO, "Hello World");
        rec.update(conn);
        
        // Now extend the size of the field from 20 to 40 characters
        System.out.println("Extending size of column FOO to 40 characters:");
        C_FOO.setSize(40); 
        script.clear();
        db.getDriver().getDDLScript(DBCmdType.ALTER, C_FOO, script);
        script.run(db.getDriver(), conn, false);

        // Now set a longer value for the record
        System.out.println("Changing the value for the FOO field for the above employee to a longer string:");
        rec.setValue(C_FOO, "This is a very long field value!");
        rec.update(conn);

        // Finally, drop the column again
        System.out.println("Dropping the FOO column from the employee table:");
        script.clear();
        db.getDriver().getDDLScript(DBCmdType.DROP, C_FOO, script);
        script.run(db.getDriver(), conn, false);
    }

    /**
     * This function demostrates the use of the DBQuery object.<BR>
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
    private static void querySample(Connection conn, int employeeId)
    {
        // Define the sub query
        DBCommand subCmd = db.createCommand();
        DBColumnExpr MAX_DATE_FROM = T_EDH.C_DATE_FROM.max().as(T_EDH.C_DATE_FROM);
        subCmd.select(T_EDH.C_EMPLOYEE_ID, MAX_DATE_FROM);
        subCmd.groupBy(T_EDH.C_EMPLOYEE_ID);
        DBQuery Q_MAX_DATE = new DBQuery(subCmd);

        // Define the query
        DBCommand cmd = db.createCommand();
        // Select requried columns
        cmd.select(T_EMP.C_EMPLOYEE_ID, T_EMP.C_FULLNAME);
        cmd.select(T_EMP.C_GENDER, T_EMP.C_PHONE_NUMBER);
        cmd.select(T_DEP.C_DEPARTMENT_ID, T_DEP.C_NAME, T_DEP.C_BUSINESS_UNIT);
        cmd.select(T_EMP.C_UPDATE_TIMESTAMP, T_DEP.C_UPDATE_TIMESTAMP);
        // Set Joins
        cmd.join(T_EDH.C_EMPLOYEE_ID, Q_MAX_DATE.findQueryColumn(T_EDH.C_EMPLOYEE_ID))
          .where(T_EDH.C_DATE_FROM.is(Q_MAX_DATE.findQueryColumn(MAX_DATE_FROM)));
        cmd.join(T_EMP.C_EMPLOYEE_ID, T_EDH.C_EMPLOYEE_ID);
        cmd.join(T_DEP.C_DEPARTMENT_ID, T_EDH.C_DEPARTMENT_ID);
        // Set Constraints
        cmd.where(T_EMP.C_RETIRED.is(false));
        // Set Order
        cmd.orderBy(T_EMP.C_LASTNAME);
        cmd.orderBy(T_EMP.C_FIRSTNAME);

        // Query Records and print output
        printQueryResults(cmd, conn);
        
        // Define an updateable query
        DBQuery Q_EMP_DEP = new DBQuery(cmd, T_EMP.C_EMPLOYEE_ID);
        DBRecord rec = new DBRecord();
        rec.read(Q_EMP_DEP, employeeId, conn);
        // Modify and Update fields from both Employee and Department
        rec.setValue(T_EMP.C_PHONE_NUMBER, "0815-4711");
        rec.setValue(T_DEP.C_BUSINESS_UNIT, "AUTO");
        rec.update(conn);
        // Sucessfully updated
        System.out.println("The employee has been sucessfully updated");
    }    

    /**
     * This functions prints the results of a query which is performed using the supplied command
     * @param cmd the command to be used for performing the query
     * @param conn the connection
     */
    private static void printQueryResults(DBCommand cmd, Connection conn)
    {
        // Query Records and print output
        DBReader reader = new DBReader();
        try
        {   // Open Reader
            System.out.println("Running Query:");
            System.out.println(cmd.getSelect());
            if (reader.open(cmd, conn) == false)
                throw new RuntimeException(reader.getErrorMessage());
            // Print column titles 
            System.out.println("---------------------------------");
            int count = reader.getFieldCount();
            for (int i=0; i<count; i++)
            {   // Print all column names
                DBColumnExpr c = reader.getColumnExpr(i);
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
                    DBColumnExpr c = reader.getColumnExpr(i);
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
