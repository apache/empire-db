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
package org.apache.empire.rest.app;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.ResourceBundle;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.empire.commons.StringUtils;
import org.apache.empire.db.DBCommand;
import org.apache.empire.db.DBContext;
import org.apache.empire.db.DBRecord;
import org.apache.empire.db.context.DBContextStatic;
import org.apache.empire.dbms.DBMSHandler;
import org.apache.empire.rest.service.Service;
import org.apache.empire.vue.sample.db.SampleDB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SampleServiceApp
{
    private static final Logger log = LoggerFactory.getLogger(SampleServiceApp.class);
    
    /**
     * Implementation of ServletContextListener which create the SampleServiceApp Singleton
     * @author doebele
     */
    public static class ContextListener implements ServletContextListener {
    
        @Override
        public void contextInitialized(ServletContextEvent sce) {
    
            System.out.println("ServletContextListener:contextInitialized");
            // check singleton
            if (app!=null)
                throw new RuntimeException("FATAL: SampleServiceApp already created!");
            // create application
            ServletContext ctx = sce.getServletContext();
            app = new SampleServiceApp(ctx);
            // done
            log.debug("SampleServiceApp created successfully!");
        }
    
        @Override
        public void contextDestroyed(ServletContextEvent sce) {
            System.out.println("ServletContextListener:contextDestroyed");
        }
    }
        
    private static SampleServiceApp app;
    
    public static SampleServiceApp instance()
    {
        return app;
    }

    private SampleConfig config = new SampleConfig();
    
    private Map<Locale, ResourceTextResolver> textResolverMap = new HashMap<Locale, ResourceTextResolver>();
    
    protected SampleServiceApp(ServletContext ctx) 
    {
        // Logging
        config.init(ctx.getRealPath("WEB-INF/config.xml"));
        
        String messageBundle ="lang.messages";
        textResolverMap.put(Locale.ENGLISH, new ResourceTextResolver(ResourceBundle.getBundle(messageBundle, Locale.ENGLISH)));
        textResolverMap.put(Locale.GERMAN,  new ResourceTextResolver(ResourceBundle.getBundle(messageBundle, Locale.GERMAN)));
        
        // get connection
        Connection conn = getJDBCConnection(ctx);
        try {
            // DB
            DBMSHandler dbms = getDBMSHandler(config.getDatabaseProvider(), conn);
            DBContext context = new DBContextStatic(dbms, conn);
            SampleDB db = initDatabase(ctx, context);
            // Add to context
            ctx.setAttribute(Service.Consts.ATTRIBUTE_DB, db);
            // sce.getServletContext().setAttribute(MobileImportServiceConsts.ATTRIBUTE_DATASOURCE, ds);
            // sce.getServletContext().setAttribute(MobileImportServiceConsts.ATTRIBUTE_CONFIG, config);
        } finally {
            releaseConnection(conn, true);
        }
    }
    
    public TextResolver getTextResolver(Locale locale) {
        TextResolver tr = textResolverMap.get(locale);
        return (tr!=null ? tr : textResolverMap.get(Locale.ENGLISH));
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
    
    /*
     * Our own simple Connection pool 
     */
    private final Queue<Connection> connPool = new LinkedList<Connection>(); 
    
    /*
     * getJDBCConnection
     */
    public Connection getJDBCConnection(ServletContext appContext)
    {
        Connection conn = connPool.poll();
        if (conn!=null)
            return conn;
        
        // Establish a new database connection
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
            return conn;
        }
        catch (Exception e)
        {
            log.error("Failed to connect directly to '" + config.getJdbcURL() + "' / User=" + config.getJdbcUser());
            log.error(e.toString());
            throw new RuntimeException(e);
        }
    }

    public void releaseConnection(Connection conn, boolean commit) {
        // release connection
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
            // close connection / return to pool
            // conn.close();
            connPool.add(conn);
        }
        catch (SQLException e)
        {
            log.error("Error releasing connection", e);
            throw new RuntimeException(e);
        }
    }
    
    
    // ********************* private ********************* 

    private SampleDB initDatabase(ServletContext ctx, DBContext context) {
        SampleDB db = new SampleDB();
        // Open Database (and create if not existing)
        DBMSHandler dbms = context.getDbms();
        log.info("Opening database '{}' using handler '{}'", db.getClass().getSimpleName(), dbms.getClass().getSimpleName());
        db.open(context);
        // check if database was just created 
        DBCommand cmd = context.createCommand();
        cmd.select(db.EMPLOYEES.count());
        if (context.getUtils().querySingleInt(cmd)==0)
        {   // Populate Database
            populateDatabase(db, context);
        }
        return db;
    }

    private static void populateDatabase(SampleDB db, DBContext context) {
        // Insert Sample Departments
        insertDepartmentSampleRecord(db, context, "Procurement", "ITTK");
        int idDevDep = insertDepartmentSampleRecord(db, context, "Development", "ITTK");
        int idSalDep = insertDepartmentSampleRecord(db, context, "Sales", "ITTK");
        // Insert Sample Employees
        insertEmployeeSampleRecord(db, context, "Mr.", "Eugen", "Miller", "M", idDevDep);
        insertEmployeeSampleRecord(db, context, "Mr.", "Max", "Mc. Callahan", "M", idDevDep);
        insertEmployeeSampleRecord(db, context, "Mrs.", "Anna", "Smith", "F", idSalDep);
        // Commit
        context.commit();
    }

    private static int insertDepartmentSampleRecord(SampleDB db, DBContext context, String department_name, String businessUnit) {
        // Insert a Department
        DBRecord rec = new DBRecord(context, db.DEPARTMENTS);
        rec.create();
        rec.set(db.DEPARTMENTS.NAME, department_name);
        rec.set(db.DEPARTMENTS.BUSINESS_UNIT, businessUnit);
        try {
            rec.update();
        } catch (Exception e) {
            log.error(e.getLocalizedMessage());
            return 0;
        }
        // Return Department ID
        return rec.getInt(db.DEPARTMENTS.ID);
    }

    /*
     * Insert a person
     */
    private static int insertEmployeeSampleRecord(SampleDB db, DBContext context, String salutation, String firstName, String lastName, String gender, int depID) {
        // Insert an Employee
        DBRecord rec = new DBRecord(context, db.EMPLOYEES);
        rec.create();
        rec.set(db.EMPLOYEES.SALUTATION, salutation);
        rec.set(db.EMPLOYEES.FIRST_NAME, firstName);
        rec.set(db.EMPLOYEES.LAST_NAME, lastName);
        rec.set(db.EMPLOYEES.GENDER, gender);
        rec.set(db.EMPLOYEES.DEPARTMENT_ID, depID);
        try {
            rec.update();
        } catch (Exception e) {
            log.error(e.getLocalizedMessage());
            return 0;
        }
        // Return Employee ID
        return rec.getInt(db.EMPLOYEES.ID);
    }

    /*
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
    */
    
}
