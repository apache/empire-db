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
package org.apache.empire.rest.service.listener;

import java.sql.Connection;
import java.sql.DriverManager;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.empire.commons.StringUtils;
import org.apache.empire.db.DBCommand;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.DBDatabaseDriver;
import org.apache.empire.db.DBRecord;
import org.apache.empire.db.DBSQLScript;
import org.apache.empire.db.exceptions.QueryFailedException;
import org.apache.empire.db.hsql.DBDatabaseDriverHSql;
import org.apache.empire.db.mysql.DBDatabaseDriverMySQL;
import org.apache.empire.vuesample.model.EmpireServiceConsts;
import org.apache.empire.vuesample.model.db.SampleDB;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppListener implements ServletContextListener {

	private static final Logger log = LoggerFactory.getLogger(AppListener.class);

	@Override
	public void contextInitialized(ServletContextEvent sce) {

		log.debug("contextInitialized");

		// Logging
		initLogging();

		//
		Connection conn = getJDBCConnection(sce.getServletContext());

		// DB
		SampleDB db = initDatabase(sce.getServletContext());
		DBDatabaseDriver driver = new DBDatabaseDriverMySQL();
		db.open(driver, conn);

		// Add to context
		sce.getServletContext().setAttribute(EmpireServiceConsts.ATTRIBUTE_DB, db);
		// sce.getServletContext().setAttribute(MobileImportServiceConsts.ATTRIBUTE_DATASOURCE, ds);
		// sce.getServletContext().setAttribute(MobileImportServiceConsts.ATTRIBUTE_CONFIG, config);

		log.debug("contextInitialized done");

	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		log.debug("contextDestroyed");
		log.debug("contextDestroyed done");
	}

	private SampleDB initDatabase(ServletContext ctx) {
		SampleDB db = new SampleDB();

		// Open Database (and create if not existing)
		DBDatabaseDriver driver = new DBDatabaseDriverHSql();
		log.info("Opening database '{}' using driver '{}'", db.getClass().getSimpleName(), driver.getClass().getSimpleName());
		Connection conn = null;
		try {
			conn = getJDBCConnection(ctx);
			db.open(driver, conn);
			if (!databaseExists(db, conn)) {
				// STEP 4: Create Database
				log.info("Creating database {}", db.getClass().getSimpleName());
				createSampleDatabase(db, driver, conn);
			}
		} finally {
			releaseConnection(db, conn, true);
		}

		return db;
	}

	private static boolean databaseExists(SampleDB db, Connection conn) {
		// Check wether DB exists
		DBCommand cmd = db.createCommand();
		cmd.select(db.T_DEPARTMENTS.count());
		try {
			return (db.querySingleInt(cmd, -1, conn) >= 0);
		} catch (QueryFailedException e) {
			return false;
		}
	}

	private static void createSampleDatabase(SampleDB db, DBDatabaseDriver driver, Connection conn) {
		// create DLL for Database Definition
		DBSQLScript script = new DBSQLScript();
		db.getCreateDDLScript(driver, script);
		// Show DLL Statements
		System.out.println(script.toString());
		// Execute Script
		script.executeAll(driver, conn, false);
		db.commit(conn);
		// Open again
		if (!db.isOpen()) {
			db.open(driver, conn);
		}
		// Insert Sample Departments
		insertDepartmentSampleRecord(db, conn, "Procurement", "ITTK");
		int idDevDep = insertDepartmentSampleRecord(db, conn, "Development", "ITTK");
		int idSalDep = insertDepartmentSampleRecord(db, conn, "Sales", "ITTK");
		// Insert Sample Employees
		insertEmployeeSampleRecord(db, conn, "Mr.", "Eugen", "Miller", "M", idDevDep);
		insertEmployeeSampleRecord(db, conn, "Mr.", "Max", "Mc. Callahan", "M", idDevDep);
		insertEmployeeSampleRecord(db, conn, "Mrs.", "Anna", "Smith", "F", idSalDep);
		// Commit
		db.commit(conn);
	}

	private static int insertDepartmentSampleRecord(SampleDB db, Connection conn, String department_name, String businessUnit) {
		// Insert a Department
		DBRecord rec = new DBRecord();
		rec.create(db.T_DEPARTMENTS);
		rec.setValue(db.T_DEPARTMENTS.NAME, department_name);
		rec.setValue(db.T_DEPARTMENTS.BUSINESS_UNIT, businessUnit);
		try {
			rec.update(conn);
		} catch (Exception e) {
			log.error(e.getLocalizedMessage());
			return 0;
		}
		// Return Department ID
		return rec.getInt(db.T_DEPARTMENTS.DEPARTMENT_ID);
	}

	/*
	 * Insert a person
	 */
	private static int insertEmployeeSampleRecord(SampleDB db, Connection conn, String salutation, String firstName, String lastName, String gender, int depID) {
		// Insert an Employee
		DBRecord rec = new DBRecord();
		rec.create(db.T_EMPLOYEES);
		rec.setValue(db.T_EMPLOYEES.SALUTATION, salutation);
		rec.setValue(db.T_EMPLOYEES.FIRST_NAME, firstName);
		rec.setValue(db.T_EMPLOYEES.LAST_NAME, lastName);
		rec.setValue(db.T_EMPLOYEES.GENDER, gender);
		rec.setValue(db.T_EMPLOYEES.DEPARTMENT_ID, depID);
		try {
			rec.update(conn);
		} catch (Exception e) {
			log.error(e.getLocalizedMessage());
			return 0;
		}
		// Return Employee ID
		return rec.getInt(db.T_EMPLOYEES.EMPLOYEE_ID);
	}

	public static Connection getJDBCConnection(ServletContext appContext) {
		// Establish a new database connection
		Connection conn = null;

		String jdbcURL = "jdbc:hsqldb:file:hsqldb/sample;shutdown=true";
		String jdbcUser = "sa";
		String jdbcPwd = "";

		if (jdbcURL.indexOf("file:") > 0) {
			jdbcURL = StringUtils.replace(jdbcURL, "file:", "file:" + appContext.getRealPath("/"));
		}
		// Connect
		log.info("Connecting to Database'" + jdbcURL + "' / User=" + jdbcUser);
		try { // Connect to the databse
			Class.forName("org.hsqldb.jdbcDriver").newInstance();
			conn = DriverManager.getConnection(jdbcURL, jdbcUser, jdbcPwd);
			log.info("Connected successfully");
			// set the AutoCommit to false this session. You must commit
			// explicitly now
			conn.setAutoCommit(false);
			log.info("AutoCommit is " + conn.getAutoCommit());

		} catch (Exception e) {
			log.error("Failed to connect directly to '" + jdbcURL + "' / User=" + jdbcUser);
			log.error(e.toString());
			throw new RuntimeException(e);
		}
		return conn;
	}

	protected void releaseConnection(DBDatabase db, Connection conn, boolean commit) {
		// release connection
		if (conn == null) {
			return;
		}
		// Commit or rollback connection depending on the exit code
		if (commit) { // success: commit all changes
			db.commit(conn);
			log.debug("REQUEST {}: commited.");
		} else { // failure: rollback all changes
			db.rollback(conn);
			log.debug("REQUEST {}: rolled back.");
		}
	}

	private void initLogging() {

		// Init Logging
		ConsoleAppender consoleAppender = new ConsoleAppender();
		String pattern = "%-5p [%d{yyyy/MM/dd HH:mm}]: %m at %l %n";
		consoleAppender.setLayout(new PatternLayout(pattern));
		consoleAppender.activateOptions();

		org.apache.log4j.Logger.getRootLogger().addAppender(consoleAppender);
		org.apache.log4j.Logger.getRootLogger().setLevel(Level.ALL);

		Level loglevel = Level.DEBUG;
		log.info("Setting LogLevel to {}", loglevel);

		// RootLogger
		org.apache.log4j.Logger.getRootLogger().setLevel(Level.INFO);

		// Empire-db Logs
		org.apache.log4j.Logger empireLog = org.apache.log4j.Logger.getLogger("org.apache.empire.db.DBDatabase");
		empireLog.setLevel(loglevel);

		// Vue.js Sample
		org.apache.log4j.Logger miLog = org.apache.log4j.Logger.getLogger("org.apache.empire.rest");
		miLog.setLevel(loglevel);

	}

}
