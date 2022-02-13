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
import org.apache.empire.db.DBRecord;
import org.apache.empire.db.DBSQLScript;
import org.apache.empire.db.context.DBContextStatic;
import org.apache.empire.db.context.DBRollbackManager;
import org.apache.empire.db.context.DBRollbackManager.ReleaseAction;
import org.apache.empire.db.exceptions.QueryFailedException;
import org.apache.empire.dbms.DBMSHandler;
import org.apache.empire.dbms.hsql.DBMSHandlerHSql;
import org.apache.empire.dbms.mysql.DBMSHandlerMySQL;
import org.apache.empire.dbms.oracle.DBMSHandlerOracle;
import org.apache.empire.dbms.sqlserver.DBMSHandlerMSSQL;
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
		DBMSHandler dbmsHandler = getDBMSHandler(driverProvider);
        log.info("Opening database '{}' using '{}'", sampleDB.getClass().getSimpleName(), dbmsHandler.getClass().getSimpleName());
		Connection conn = null;
		DBContext context = null;
		try {
			conn = getConnection(sampleDB);
			context = new DBContextStatic(dbmsHandler, conn, false, false);
			sampleDB.open(context);
			if (!databaseExists(context)) {
				// STEP 4: Create Database
				log.info("Creating database {}", sampleDB.getClass().getSimpleName());
				createSampleDatabase(context);
			}
		} finally {
		    context.discard();
			releaseConnection(conn, true, null);
		}
	}

	/*
	 * getDatabaseDriver
	 */
	private DBMSHandler getDBMSHandler(String provider) {
		if (provider.equalsIgnoreCase("mysql")) {
			DBMSHandlerMySQL dbms = new DBMSHandlerMySQL();
			// Set Driver specific properties (if any)
			dbms.setDatabaseName(config.getSchemaName());
			return dbms;
		} else if (provider.equalsIgnoreCase("oracle")) {
			DBMSHandlerOracle dbms = new DBMSHandlerOracle();
			// Set Driver specific properties (if any)
			return dbms;
		} else if (provider.equalsIgnoreCase("sqlserver")) {
			DBMSHandlerMSSQL dbms = new DBMSHandlerMSSQL();
			// Set Driver specific properties (if any)
			dbms.setDatabaseName(config.getSchemaName());
			return dbms;
		} else if (provider.equalsIgnoreCase("hsqldb")) {
			DBMSHandlerHSql dbms = new DBMSHandlerHSql();
			// Set Driver specific properties (if any)
			return dbms;
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

	private boolean databaseExists(DBContext context) {
		// Check wether DB exists
		DBCommand cmd = sampleDB.createCommand();
		cmd.select(sampleDB.T_DEPARTMENTS.count());
		try {
			return (context.getUtils().querySingleInt(cmd, -1) >= 0);
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
			sampleDB.open(context);
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
		rec.set(sampleDB.T_DEPARTMENTS.NAME, department_name);
		rec.set(sampleDB.T_DEPARTMENTS.BUSINESS_UNIT, businessUnit);
		try {
			rec.update();
		} catch (Exception e) {
			log.error(e.getLocalizedMessage());
			return 0;
		}
		// Return Department ID
		return rec.getInt(sampleDB.T_DEPARTMENTS.ID);
	}

	/*
	 * Insert a person
	 */
	private int insertEmployeeSampleRecord(DBContext context, String salutation, String firstName, String lastName, String gender, int depID) {
		// Insert an Employee
		DBRecord rec = new DBRecord(context, sampleDB.T_EMPLOYEES);
		rec.create();
		rec.set(sampleDB.T_EMPLOYEES.SALUTATION, salutation);
		rec.set(sampleDB.T_EMPLOYEES.FIRST_NAME, firstName);
		rec.set(sampleDB.T_EMPLOYEES.LAST_NAME, lastName);
		rec.set(sampleDB.T_EMPLOYEES.GENDER, gender);
		rec.set(sampleDB.T_EMPLOYEES.DEPARTMENT_ID, depID);
		try {
			rec.update();
		} catch (Exception e) {
			log.error(e.getLocalizedMessage());
			return 0;
		}
		// Return Employee ID
		return rec.getInt(sampleDB.T_EMPLOYEES.ID);
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
    protected void releaseConnection(Connection conn, boolean commit, DBRollbackManager dbrm)
    {
        try
        { // release connection
            if (conn == null)
            {
                return;
            }
            log.trace("releasing Connection {}", conn.hashCode());
            // Commit or rollback connection depending on the exit code
            if (commit)
            {   // success: commit all changes
                if (dbrm!=null)
                    dbrm.releaseConnection(conn, ReleaseAction.Discard);  // before commit
                conn.commit();
                log.debug("REQUEST commited.");
            }
            else
            {   // failure: rollback all changes
                conn.rollback();
                if (dbrm!=null)
                    dbrm.releaseConnection(conn, ReleaseAction.Rollback); // after rollback
                log.debug("REQUEST rolled back.");
            }
            // Release Connection (don't do that here!)
            // conn.close();
            // done
            if (log.isDebugEnabled())
                log.debug("Connection released but not closed.");
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
