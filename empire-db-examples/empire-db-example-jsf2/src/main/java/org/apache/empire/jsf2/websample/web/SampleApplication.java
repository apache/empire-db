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
package org.apache.empire.jsf2.websample.web;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Locale;

import javax.servlet.ServletContext;
import javax.sql.DataSource;

import org.apache.empire.commons.StringUtils;
import org.apache.empire.db.DBCommand;
import org.apache.empire.db.DBContext;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.DBDatabaseDriver;
import org.apache.empire.db.DBRecord;
import org.apache.empire.db.DBSQLScript;
import org.apache.empire.db.context.DBContextStatic;
import org.apache.empire.db.exceptions.QueryFailedException;
import org.apache.empire.db.hsql.DBDatabaseDriverHSql;
import org.apache.empire.db.mysql.DBDatabaseDriverMySQL;
import org.apache.empire.db.oracle.DBDatabaseDriverOracle;
import org.apache.empire.db.sqlserver.DBDatabaseDriverMSSQL;
import org.apache.empire.jsf2.app.WebApplication;
import org.apache.empire.jsf2.controls.InputControlManager;
import org.apache.empire.jsf2.custom.controls.FileInputControl;
import org.apache.empire.jsf2.websample.db.SampleDB;
import org.apache.empire.jsf2.websample.web.pages.SamplePages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SampleApplication extends WebApplication {
	// Logger
	private static final Logger log = LoggerFactory
			.getLogger(SampleApplication.class);

	protected static final String MANAGED_BEAN_NAME = "sampleApplication";
	protected static final Locale DEFAULT_LOCALE = Locale.ENGLISH;
	// Non-Static
	private SampleConfig config = new SampleConfig();
	private SampleDB sampleDB = null;

	private Connection conn = null;

	public static SampleApplication get() {
		return (SampleApplication)WebApplication.getInstance();
	}

	public SampleApplication() { 
		
		// trace
		SampleApplication.log.trace("SampleApplication created");

		// register custom control types
		InputControlManager.registerControl(new FileInputControl());
	}

	@Override
	public void init(ServletContext servletContext) {
		// register all controls
		// InputControlManager.registerControl("myType", new
		// MyTypeInputControl());
		config.init(servletContext.getRealPath("WEB-INF/config.xml"));

		// Get a JDBC Connection
		log.info("*** getJDBCConnection() ***");
		conn = getJDBCConnection(servletContext);

		log.info("*** initDatabase() ***");
		initDatabase();

		log.info("*** initPages() ***");
		initPages(servletContext);

		// Set Database to Servlet Context
		servletContext.setAttribute("db", sampleDB);

		// Done
	}

	/*
	 * getJDBCConnection
	 */
	private Connection getJDBCConnection(ServletContext appContext) {
		// Establish a new database connection
		Connection conn = null;
		String jdbcURL = config.getJdbcURL();
		if (jdbcURL.indexOf("file:") > 0)
			jdbcURL = StringUtils.replace(jdbcURL, "file:", "file:"
					+ appContext.getRealPath("/"));
		// Connect
		log.info("Connecting to Database'" + jdbcURL + "' / User="
				+ config.getJdbcUser());
		try { // Connect to the databse
			Class.forName(config.getJdbcClass()).newInstance();
			conn = DriverManager.getConnection(jdbcURL, config.getJdbcUser(),
					config.getJdbcPwd());
			log.info("Connected successfully");
			// set the AutoCommit to false this session. You must commit
			// explicitly now
			conn.setAutoCommit(false);
			log.info("AutoCommit is " + conn.getAutoCommit());

		} catch (Exception e) {
			log.error("Failed to connect directly to '" + config.getJdbcURL()
					+ "' / User=" + config.getJdbcUser());
			log.error(e.toString());
			throw new RuntimeException(e);
		}
		return conn;
	}

	private void initDatabase() {
		sampleDB = new SampleDB();

		// Open Database (and create if not existing)
		String driverProvider = config.getDatabaseProvider();
		DBDatabaseDriver driver = getDatabaseDriver(driverProvider);
        log.info("Opening database '{}' using driver '{}'", sampleDB.getClass().getSimpleName(), driver.getClass().getSimpleName());
		Connection conn = null;
		DBContext context = null;
		try {
			conn = getConnection(sampleDB);
			context = new DBContextStatic(driver, conn);
			sampleDB.open(driver, conn);
			if (!databaseExists(conn)) {
				// STEP 4: Create Database
				log.info("Creating database {}", sampleDB.getClass().getSimpleName());
				createSampleDatabase(context);
			}
		} finally {
		    context.discard();
			releaseConnection(sampleDB, conn, true);
		}
	}

	/*
	 * getDatabaseDriver
	 */
	private DBDatabaseDriver getDatabaseDriver(String provider) {
		if (provider.equalsIgnoreCase("mysql")) {
			DBDatabaseDriverMySQL driver = new DBDatabaseDriverMySQL();
			// Set Driver specific properties (if any)
			driver.setDatabaseName(config.getSchemaName());
			return driver;
		} else if (provider.equalsIgnoreCase("oracle")) {
			DBDatabaseDriverOracle driver = new DBDatabaseDriverOracle();
			// Set Driver specific properties (if any)
			return driver;
		} else if (provider.equalsIgnoreCase("sqlserver")) {
			DBDatabaseDriverMSSQL driver = new DBDatabaseDriverMSSQL();
			// Set Driver specific properties (if any)
			driver.setDatabaseName(config.getSchemaName());
			return driver;
		} else if (provider.equalsIgnoreCase("hsqldb")) {
			DBDatabaseDriverHSql driver = new DBDatabaseDriverHSql();
			// Set Driver specific properties (if any)
			return driver;
		} else { // Unknown Provider
			throw new RuntimeException("Unknown Database Provider " + provider);
		}
	}

	private void initPages(ServletContext sc) {
		// register Page Beans
		new SamplePages().registerPageBeans(this.getFacesImplementation());
	}

	public SampleDB getDatabase() {
		return sampleDB;
	}

	private boolean databaseExists(Connection conn) {
		// Check wether DB exists
		DBCommand cmd = sampleDB.createCommand();
		cmd.select(sampleDB.T_DEPARTMENTS.count());
		try {
			return (sampleDB.querySingleInt(cmd, -1, conn) >= 0);
		} catch (QueryFailedException e) {
			return false;
		}
	}

	/*
	 * creates a DDL Script for the entire SampleDB Database then checks if the
	 * department table exists by running "select count(*) from DEPARTMENTS" if
	 * the department tables does not exist, the entire dll-script is executed
	 * line by line
	 */
	private void createSampleDatabase(DBContext context) {
		// create DLL for Database Definition
		DBSQLScript script = new DBSQLScript(context);
		sampleDB.getCreateDDLScript(script);
		// Show DLL Statements
		System.out.println(script.toString());
		// Execute Script
		script.executeAll(false);
		context.commit();
		// Open again
		if (!sampleDB.isOpen()) {
			sampleDB.open(context.getDriver(), conn);
		}
		// Insert Sample Departments
		insertDepartmentSampleRecord(context, "Procurement", "ITTK");
		int idDevDep = insertDepartmentSampleRecord(context, "Development", "ITTK");
		int idSalDep = insertDepartmentSampleRecord(context, "Sales", 		 "ITTK");
		// Insert Sample Employees
		insertEmployeeSampleRecord(context, "Mr.", "Eugen", "Miller", "M",		idDevDep);
		insertEmployeeSampleRecord(context, "Mr.", "Max", "Mc. Callahan", "M",	idDevDep);
		insertEmployeeSampleRecord(context, "Mrs.", "Anna", "Smith", "F", 		idSalDep);
		// Commit
        context.commit();
	}

	/*
	 * Insert a department
	 */
	private int insertDepartmentSampleRecord(DBContext context, String department_name, String businessUnit) {
		// Insert a Department
		DBRecord rec = new DBRecord(context, sampleDB.T_DEPARTMENTS);
		rec.create();
		rec.setValue(sampleDB.T_DEPARTMENTS.NAME, department_name);
		rec.setValue(sampleDB.T_DEPARTMENTS.BUSINESS_UNIT, businessUnit);
		try {
			rec.update();
		} catch (Exception e) {
			log.error(e.getLocalizedMessage());
			return 0;
		}
		// Return Department ID
		return rec.getInt(sampleDB.T_DEPARTMENTS.DEPARTMENT_ID);
	}

	/*
	 * Insert a person
	 */
	private int insertEmployeeSampleRecord(DBContext context, String salutation, String firstName, String lastName, String gender, int depID) {
		// Insert an Employee
		DBRecord rec = new DBRecord(context, sampleDB.T_EMPLOYEES);
		rec.create();
		rec.setValue(sampleDB.T_EMPLOYEES.SALUTATION, salutation);
		rec.setValue(sampleDB.T_EMPLOYEES.FIRST_NAME, firstName);
		rec.setValue(sampleDB.T_EMPLOYEES.LAST_NAME, lastName);
		rec.setValue(sampleDB.T_EMPLOYEES.GENDER, gender);
		rec.setValue(sampleDB.T_EMPLOYEES.DEPARTMENT_ID, depID);
		try {
			rec.update();
		} catch (Exception e) {
			log.error(e.getLocalizedMessage());
			return 0;
		}
		// Return Employee ID
		return rec.getInt(sampleDB.T_EMPLOYEES.EMPLOYEE_ID);
	}

	@Override
	/**
	 * returns null as connection pooling is not used
	 */
	protected DataSource getAppDataSource(DBDatabase db) {
		return null;
	}

    /**
     * Usually returns a connection from the connection pool
     * As the sample does not use connection pooling, a static connection is returned
     * @return a connection for the requested db
     */
    @Override
    protected Connection getConnection(DBDatabase db)
    {
    	return conn;
    }

    /**
     * Usually releases a connection from the connection pool
     * As the sample does not use connection pooling, only a commit or rollback is performed
     */
    @Override
    protected void releaseConnection(DBDatabase db, Connection conn, boolean commit)
    {
        try
        { // release connection
            if (conn == null)
            {
                return;
            }
            // Commit or rollback connection depending on the exit code
            if (commit)
            { // success: commit all changes
                conn.commit();
                log.debug("REQUEST commited.");
            }
            else
            { // failure: rollback all changes
                conn.rollback();
                log.debug("REQUEST rolled back.");
            }
            // Don't Release Connection
            // conn.close();
        }
        catch (SQLException e)
        {
            log.error("Error releasing connection", e);
            e.printStackTrace();
        }
    }
	
	protected SampleConfig getSampleConfig() {
		if (config == null) {
			SampleApplication.log
					.error("Application configuration not initialized!");
		}
		return config;
	}

}
