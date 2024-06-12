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
package org.apache.empire.jakarta.websample.web;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Locale;

import jakarta.servlet.ServletContext;
import javax.sql.DataSource;

import org.apache.empire.commons.StringUtils;
import org.apache.empire.db.DBCommand;
import org.apache.empire.db.DBContext;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.DBRecord;
import org.apache.empire.db.context.DBContextStatic;
import org.apache.empire.db.context.DBRollbackManager;
import org.apache.empire.db.context.DBRollbackManager.ReleaseAction;
import org.apache.empire.dbms.DBMSHandler;
import org.apache.empire.jakarta.app.WebApplication;
import org.apache.empire.jakarta.controls.InputControlManager;
import org.apache.empire.jakarta.custom.controls.FileInputControl;
import org.apache.empire.jakarta.websample.db.SampleDB;
import org.apache.empire.jakarta.websample.db.SampleDBwithMeta;
import org.apache.empire.jakarta.websample.web.pages.SamplePages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SampleApplication extends WebApplication {
	// Logger
	private static final Logger log = LoggerFactory
			.getLogger(SampleApplication.class);

	protected static final String MANAGED_BEAN_NAME = "sampleApplication";
	protected static final Locale DEFAULT_LOCALE = Locale.ENGLISH;
	// Non-Static
	private SampleConfig config = null; // SampleAppStartupListener.config;
	private SampleDB sampleDB = null;

	private Connection conn = null;

	public static SampleApplication get() {
		return (SampleApplication)WebApplication.getInstance();
	}

	public SampleApplication() { 
		
		// trace
		SampleApplication.log.trace("SampleApplication created");
	}
	
	/**
	 * Sets the Configuration
	 * Called only from SampleAppStartupListener
	 */
	void setConfig(SampleConfig config) 
	{
	    this.config = config;
	}

	@Override
	public void init(ServletContext servletContext) {
		// register all controls
		// InputControlManager.registerControl("myType", new
		// MyTypeInputControl());

		// Get a JDBC Connection
		log.info("*** getJDBCConnection() ***");
		conn = getJDBCConnection(servletContext);

		log.info("*** initDatabase() ***");
		initDatabase();
		
        log.info("*** initControls() ***");
		initControls();

		log.info("*** initPages() ***");
		initPages(servletContext);

		// Set Database to Servlet Context
		servletContext.setAttribute("db", sampleDB);

		// Done
	}
	
    /**
     * initializes JSF control components
     */
    private void initControls()
    {
        // register custom control types
        InputControlManager.registerControl(new FileInputControl());
	}

    /*
     * getJDBCConnection
     */
    private Connection getJDBCConnection(ServletContext appContext)
    {
        // Establish a new database connection
        Connection conn = null;
        String jdbcURL = config.getJdbcURL();
        if (jdbcURL.indexOf("file:") > 0)
            jdbcURL = StringUtils.replace(jdbcURL, "file:", "file:" + appContext.getRealPath("/"));
        // Connect
        log.info("Connecting to Database'" + jdbcURL + "' / User=" + config.getJdbcUser());
        try
        { // Connect to the database
            Class.forName(config.getJdbcClass()).newInstance();
            conn = DriverManager.getConnection(jdbcURL, config.getJdbcUser(), config.getJdbcPwd());
            log.info("Connected successfully");
            // set the AutoCommit to false this session. You must commit
            // explicitly now
            conn.setAutoCommit(false);
            log.info("AutoCommit is " + conn.getAutoCommit());

        }
        catch (Exception e)
        {
            log.error("Failed to connect directly to '" + config.getJdbcURL() + "' / User=" + config.getJdbcUser());
            log.error(e.toString());
            throw new RuntimeException(e);
        }
        return conn;
    }

	private void initDatabase() {
	    // Create Database with Metadata!
		sampleDB = new SampleDBwithMeta();

		// Open Database (and create if not existing)
        DBMSHandler dbmsHandler = getDBMSHandler(config.getDatabaseProvider(), conn);
        log.info("Opening database '{}' using '{}'", sampleDB.getClass().getSimpleName(), dbmsHandler.getClass().getSimpleName());
		Connection conn = null;
		DBContext context = null;
		try {
			conn = getConnection(sampleDB);
			context = new DBContextStatic(dbmsHandler, conn);
			sampleDB.open(context);
	        // check if database was just created 
	        DBCommand cmd = context.createCommand();
	        cmd.select(sampleDB.EMPLOYEES.count());
	        if (context.getUtils().querySingleInt(cmd)==0)
	        {   // Populate Database
	            populateDatabase(context);
	        }
		} finally {
		    context.discard();
			releaseConnection(conn, true, null);
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
            config.readProperties(dbms, "properties-"+provider, "dbmsHandlerProperties");

            // done
            return dbms;
            
        } catch (Exception e)
        {   // catch any checked exception and forward it
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

	private void initPages(ServletContext sc) {
		// register Page Beans
		new SamplePages().registerPageBeans(this.getFacesImplementation());
	}

	public SampleDB getDatabase() {
		return sampleDB;
	}

	/*
	 * creates a DDL Script for the entire SampleDB Database then checks if the
	 * department table exists by running "select count(*) from DEPARTMENTS" if
	 * the department tables does not exist, the entire dll-script is executed
	 * line by line
	 */
	private void populateDatabase(DBContext context) {
		// Insert Sample Departments
		insertDepartmentSampleRecord(context, "Procurement", "ITTK");
		int idDevDep = insertDepartmentSampleRecord(context, "Development", "ITTK");
		int idSalDep = insertDepartmentSampleRecord(context, "Sales", 		 "ITTK");
		// Insert Sample Employees
		insertEmployeeSampleRecord(context, "Mr.", "Eugen", "Miller", "M",		30000, idDevDep);
		insertEmployeeSampleRecord(context, "Mr.", "Max", "Mc. Callahan", "M",	22500, idDevDep);
		insertEmployeeSampleRecord(context, "Mrs.", "Anna", "Smith", "F", 		44250, idSalDep);
		// Commit
        context.commit();
	}

	/*
	 * Insert a department
	 */
	private int insertDepartmentSampleRecord(DBContext context, String department_name, String businessUnit) {
		// Insert a Department
		DBRecord rec = new DBRecord(context, sampleDB.DEPARTMENTS);
		rec.create()
		   .set(sampleDB.DEPARTMENTS.NAME, department_name)
		   .set(sampleDB.DEPARTMENTS.BUSINESS_UNIT, businessUnit)
		   .update();
		// Return Department ID
		return rec.getInt(sampleDB.DEPARTMENTS.ID);
	}

	/*
	 * Insert a person
	 */
	private int insertEmployeeSampleRecord(DBContext context, String salutation, String firstName, String lastName, String gender, int salary, int depID) {
		// Insert an Employee
	    SampleDB.TEmployees EMP = sampleDB.EMPLOYEES;
		DBRecord rec = new DBRecord(context, sampleDB.EMPLOYEES);
		rec.create()
		   .set(EMP.SALUTATION, salutation)
		   .set(EMP.FIRST_NAME, firstName)
		   .set(EMP.LAST_NAME, lastName)
		   .set(EMP.GENDER, gender)
		   .set(EMP.DEPARTMENT_ID, depID)
           .set(EMP.SALARY, salary)
		   .update();
		// insert Payments
		insertPayments(rec);		
		// Return Employee ID
		return rec.getInt(sampleDB.EMPLOYEES.ID);
	}

    /**
     * <PRE>
     * Inserts an Payments for a particular Employee
     * </PRE>
     */
    private void insertPayments(DBRecord employee)
    {
        if (employee.isNull(sampleDB.EMPLOYEES.SALARY))
            return; // No salary
        // monthlySalary
        BigDecimal monthlySalary = employee.getDecimal(sampleDB.EMPLOYEES.SALARY).divide(new BigDecimal(12), 2, RoundingMode.HALF_UP);
        // Insert an Employee
        LocalDate date = LocalDate.now();
        date = date.minusDays(date.getDayOfMonth()-1); // first day of this month
        // Add Payment for each month
        SampleDB.TPayments PAY = sampleDB.PAYMENTS;
        DBRecord rec = new DBRecord(employee.getContext(), PAY);
        int months = (int)(Math.random()*6)+11;
        for (LocalDate month=date.minusMonths(months); !month.isAfter(date); month=month.plusMonths(1))
        {
            BigDecimal variation = new BigDecimal((Math.random()*200) - 100.0);
            variation = variation.setScale(2, RoundingMode.HALF_UP);
            // insert
            rec.create(DBRecord.key(employee.getIdentity(), month.getYear(), month.getMonth()));
            rec.set(PAY.AMOUNT, monthlySalary.add(variation));
            rec.update();
        }
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
