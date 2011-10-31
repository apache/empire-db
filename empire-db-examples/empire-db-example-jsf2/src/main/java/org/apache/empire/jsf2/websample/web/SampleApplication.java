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

import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.servlet.ServletContext;

import org.apache.empire.commons.StringUtils;
import org.apache.empire.db.DBCommand;
import org.apache.empire.db.DBDatabaseDriver;
import org.apache.empire.db.DBRecord;
import org.apache.empire.db.DBSQLScript;
import org.apache.empire.db.exceptions.QueryFailedException;
import org.apache.empire.db.hsql.DBDatabaseDriverHSql;
import org.apache.empire.db.mysql.DBDatabaseDriverMySQL;
import org.apache.empire.db.oracle.DBDatabaseDriverOracle;
import org.apache.empire.db.sqlserver.DBDatabaseDriverMSSQL;
import org.apache.empire.jsf2.websample.db.SampleDB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ManagedBean(name = "application")
@ApplicationScoped
public class SampleApplication {
	// Logger
	private static final Logger log = LoggerFactory
			.getLogger(SampleApplication.class);

	// Non-Static
	private SampleDB db = new SampleDB();
	private SampleConfig config = new SampleConfig();

	private Connection conn = null;

	public void init(ServletContext servletContext) {
		try {
			// register all controls
			// InputControlManager.registerControl("myType", new
			// MyTypeInputControl());
			config.init(servletContext.getRealPath("WEB-INF/config.xml"));

			// Set Database to Servlet Context
			servletContext.setAttribute("db", db);

			// Get a JDBC Connection
			log.info("*** getJDBCConnection() ***");
			conn = getJDBCConnection(servletContext);

			// Choose a driver
			log.info("*** create DBDatabaseDriverOracle() ***");
			DBDatabaseDriver driver = getDatabaseDriver(config
					.getDatabaseProvider());

			// Open Database (and create if not existing)
			log.info("*** open database ***");
			db.open(driver, conn);
			if (!databaseExists(conn)) {
				// STEP 4: Create Database
				log.info("*** create Database ***");
				createSampleDatabase(driver, conn);
			}

			// Done
			log.info("Application initialized ");

		} catch (Exception e) {
			// Error
			log.info(e.toString());
			e.printStackTrace();
		}

	}

	public SampleDB getDatabase() {
		return db;
	}

	public Connection getPooledConnection() {
		return conn;
	}

	/**
	 * releases a connection from the connection pool
	 */
	public void releaseConnection(Connection conn, boolean commit) {
		// Return Connection to Connection Pool
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

	private boolean databaseExists(Connection conn) {
		// Check wether DB exists
		DBCommand cmd = db.createCommand();
		cmd.select(db.T_DEPARTMENTS.count());
		try {
			return (db.querySingleInt(cmd.getSelect(), -1, conn) >= 0);
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
	private void createSampleDatabase(DBDatabaseDriver driver, Connection conn) {
		// create DLL for Database Definition
		DBSQLScript script = new DBSQLScript();
		db.getCreateDDLScript(driver, script);
		// Show DLL Statements
		System.out.println(script.toString());
		// Execute Script
		script.run(driver, conn, false);
		db.commit(conn);
		// Open again
		if (!db.isOpen()) {
			db.open(driver, conn);
		}
		// Insert Sample Departments
		int idDevDep = insertDepartmentSampleRecord(conn, "Development", "ITTK");
		int idSalDep = insertDepartmentSampleRecord(conn, "Sales", "ITTK");
		// Insert Sample Employees
		insertEmployeeSampleRecord(conn, "Mr.", "Eugen", "Miller", "M",
				idDevDep);
		insertEmployeeSampleRecord(conn, "Mr.", "Max", "Mc. Callahan", "M",
				idDevDep);
		insertEmployeeSampleRecord(conn, "Mrs.", "Anna", "Smith", "F", idSalDep);
		// Commit
		db.commit(conn);
	}

	/*
	 * Insert a department
	 */
	private int insertDepartmentSampleRecord(Connection conn,
			String department_name, String businessUnit) {
		// Insert a Department
		DBRecord rec = new DBRecord();
		rec.create(db.T_DEPARTMENTS);
		rec.setValue(db.T_DEPARTMENTS.C_NAME, department_name);
		rec.setValue(db.T_DEPARTMENTS.C_BUSINESS_UNIT, businessUnit);
		try {
			rec.update(conn);
		} catch (Exception e) {
			log.error(e.getLocalizedMessage());
			return 0;
		}
		// Return Department ID
		return rec.getInt(db.T_DEPARTMENTS.C_DEPARTMENT_ID);
	}

	/*
	 * Insert a person
	 */
	private int insertEmployeeSampleRecord(Connection conn, String salutation,
			String firstName, String lastName, String gender, int depID) {
		// Insert an Employee
		DBRecord rec = new DBRecord();
		rec.create(db.T_EMPLOYEES);
		rec.setValue(db.T_EMPLOYEES.C_SALUTATION, salutation);
		rec.setValue(db.T_EMPLOYEES.C_FIRST_NAME, firstName);
		rec.setValue(db.T_EMPLOYEES.C_LAST_NAME, lastName);
		rec.setValue(db.T_EMPLOYEES.C_GENDER, gender);
		rec.setValue(db.T_EMPLOYEES.C_DEPARTMENT_ID, depID);
		try
		{
			rec.update(conn);
		}
		catch(Exception e)
		{
			log.error(e.getLocalizedMessage());
			return 0;
		}
		// Return Employee ID
		return rec.getInt(db.T_EMPLOYEES.C_EMPLOYEE_ID);
	}

}
