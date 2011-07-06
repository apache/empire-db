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

package org.apache.empire.samples.cxf.wssample.server;

import java.sql.Connection;
import java.sql.DriverManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.empire.db.DBCommand;
import org.apache.empire.db.DBDatabaseDriver;
import org.apache.empire.db.DBRecord;
import org.apache.empire.db.DBSQLScript;
import org.apache.empire.db.hsql.DBDatabaseDriverHSql;
import org.apache.empire.db.mysql.DBDatabaseDriverMySQL;
import org.apache.empire.db.oracle.DBDatabaseDriverOracle;
import org.apache.empire.db.sqlserver.DBDatabaseDriverMSSQL;
import org.apache.empire.samples.cxf.wssample.common.EmployeeService;
import org.apache.empire.samples.cxf.wssample.server.db.SampleDB;

public class ServerControl
{
    private static final Log               log            = LogFactory.getLog(EmployeeServiceImpl.class);
    
    public static final String             serviceName    = "employeeService";
    public static final String             serviceAddress = "http://localhost:8081/" + serviceName;
    
    private SampleDB                       db             = null;
    private Connection                     conn           = null;
    private SampleConfig                   config         = null;
    private DBDatabaseDriver               driver         = null;


    private String                         configPath     = "config.xml";
    private org.apache.cxf.endpoint.Server endpoint       = null;
    private JaxWsServerFactoryBean svrFactory = null;

    public ServerControl()
    {
        init();
    }
    
    // stops the service
    public void stop()
    {
        db.commit(conn);
        log.info("[stop] commit to database");
        db.close(conn);
        log.info("[stop] close database connection");
        endpoint.stop();
        log.info("[stop] stopped werbservice endpoint");
    }

    // starts the service
    public void start()
    {
        // create the server
        endpoint = svrFactory.create();

        // service is now created and may be started
        log.info("[init] " + serviceName + " successfully created ...");
        
        if (endpoint != null)
        {
            db.open(driver, conn);
            endpoint.start();
            log.info("[start] " + serviceName + " successfully started!");
        } else
        {
            log.error("[start] The webservice seems not to be initialized correctly!");
        }
    }
    
    public void appendLogger(LoggingOutInterceptor out, LoggingInInterceptor in)
    {
        svrFactory.getOutInterceptors().add(out);
        svrFactory.getInInterceptors().add(in);
    }

    private void init()
    {
        initDB();
        initWS();
    }

    /*
     * init database, create tables if necessary ...
     */
    private void initDB()
    {
        config = new SampleConfig();
        db = new SampleDB();

        config.init(configPath);
        driver = getDatabaseDriver(config.getDatabaseProvider());
        conn = getJDBCConnection();

        // Open Database (and create if not existing)
        log.info("*** open database ***");
        if (!db.open(driver, conn) || !databaseExists(conn))
        {
            // STEP 4: Create Database
            log.info("*** create Database ***");
            createSampleDatabase(driver, conn);
        }
        
        log.info("[init] Database successfully initialized!");
    }

    /*
     * init webservice, create implemantion, connect to server ...
     */
    
    
    private void initWS()
    {
        // create new instance of the service implmentation
        EmployeeService impl = new EmployeeServiceImpl(db, conn);

        // construct/configure factory to create our specific service
        svrFactory = new JaxWsServerFactoryBean();
        //svrFactory.getInInterceptors().add(new LoggingInInterceptor());
        //svrFactory.getOutInterceptors().add(new LoggingOutInterceptor());
        // specify our implementation class
        svrFactory.setServiceClass(EmployeeService.class);
        // specify the address
        svrFactory.setAddress(serviceAddress);
        // set the instance to be used
        svrFactory.setServiceBean(impl);
    }

    private Connection getJDBCConnection()
    {
        // Establish a new database connection
        Connection conn = null;
        String jdbcURL = config.getJdbcURL();

        // Connect
        log.info("Connecting to Database'" + jdbcURL + "' / User=" + config.getJdbcUser());
        try
        { // Connect to the databse
            Class.forName(config.getJdbcClass()).newInstance();
            conn = DriverManager.getConnection(jdbcURL, config.getJdbcUser(), config.getJdbcPwd());
            log.info("Connected successfully");
            // Set the AutoCommit to false this session. 
            // You must commit explicitly now.
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

    private DBDatabaseDriver getDatabaseDriver(String provider)
    {
        if (provider.equalsIgnoreCase("mysql"))
        {
            DBDatabaseDriverMySQL driver = new DBDatabaseDriverMySQL();
            // Set Driver specific properties (if any)
            driver.setDatabaseName(config.getSchemaName());
            return driver;
        } else if (provider.equalsIgnoreCase("oracle"))
        {
            DBDatabaseDriverOracle driver = new DBDatabaseDriverOracle();
            // Set Driver specific properties (if any)
            return driver;
        } else if (provider.equalsIgnoreCase("sqlserver"))
        {
            DBDatabaseDriverMSSQL driver = new DBDatabaseDriverMSSQL();
            // Set Driver specific properties (if any)
            driver.setDatabaseName(config.getSchemaName());
            return driver;
        } else if (provider.equalsIgnoreCase("hsqldb"))
        {
            DBDatabaseDriverHSql driver = new DBDatabaseDriverHSql();
            // Set Driver specific properties (if any)
            return driver;
        } else
        { // Unknown Provider
            throw new RuntimeException("Unknown Database Provider " + provider);
        }
    }

    private boolean databaseExists(Connection conn)
    {
        // Check wether DB exists
        DBCommand cmd = db.createCommand();
        cmd.select(db.DEPARTMENTS.count());
        int deps = db.querySingleInt(cmd.getSelect(), -1, conn);
        return (deps >= 0);
    }

    /*
     * creates a DDL Script for the entire SampleDB Database then checks if the department table exists by running
     * "select count(*) from DEPARTMENTS" if the department tables does not exist, the entire dll-script is executed line by
     * line
     */
    private void createSampleDatabase(DBDatabaseDriver driver, Connection conn)
    {
        // create DLL for Database Definition
        DBSQLScript script = new DBSQLScript();
        db.getCreateDDLScript(driver, script);
        // Show DLL Statements
        System.out.println(script.toString());
        // Execute Script
        script.run(driver, conn, false);
        db.commit(conn);
        // Open again
        if (!db.isOpen() && !db.open(driver, conn)){
            throw new RuntimeException(driver.getErrorMessage());
        }
        // Insert Sample Departments
        int idDevDep = insertDepartmentSampleRecord(conn, "Development", "ITTK");
        int idSalDep = insertDepartmentSampleRecord(conn, "Sales", "ITTK");
        // Insert Sample Employees
        insertEmployeeSampleRecord(conn, "Mr.", "Eugen", "Miller", "M", idDevDep);
        insertEmployeeSampleRecord(conn, "Mr.", "Max", "Mc. Callahan", "M", idDevDep);
        insertEmployeeSampleRecord(conn, "Mrs.", "Anna", "Smith", "F", idSalDep);
        // Commit
        db.commit(conn);
    }

    /*
     * Insert a department
     */
    private int insertDepartmentSampleRecord(Connection conn, String department_name, String businessUnit)
    {
        // Insert a Department
        DBRecord rec = new DBRecord();
        rec.create(db.DEPARTMENTS);
        rec.setValue(db.DEPARTMENTS.NAME, department_name);
        rec.setValue(db.DEPARTMENTS.BUSINESS_UNIT, businessUnit);
        if (!rec.update(conn))
        {
            log.error(rec.getErrorMessage());
            return 0;
        }
        // Return Department ID
        return rec.getInt(db.DEPARTMENTS.DEPARTMENT_ID);
    }

    /*
     * Insert a person
     */
    private int insertEmployeeSampleRecord(Connection conn, String salutation, String firstName, String lastName,
                                           String gender, int depID)
    {
        // Insert an Employee
        DBRecord rec = new DBRecord();
        rec.create(db.EMPLOYEES);
        rec.setValue(db.EMPLOYEES.SALUTATION, salutation);
        rec.setValue(db.EMPLOYEES.FIRSTNAME, firstName);
        rec.setValue(db.EMPLOYEES.LASTNAME, lastName);
        rec.setValue(db.EMPLOYEES.GENDER, gender);
        rec.setValue(db.EMPLOYEES.DEPARTMENT_ID, depID);
        if (!rec.update(conn))
        {
            log.error(rec.getErrorMessage());
            return 0;
        }
        // Return Employee ID
        return rec.getInt(db.EMPLOYEES.EMPLOYEE_ID);
    }
}
