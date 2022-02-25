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
package org.apache.empire.db.codegen;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.apache.empire.commons.StringUtils;
import org.apache.empire.db.validation.DBModelParser;
import org.apache.empire.dbms.DBMSHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Console code generator application, takes the config file as first argument.
 *
 */
public class CodeGenerator {
	
	private static final Logger log = LoggerFactory.getLogger(CodeGenerator.class);
	
	private static final String DEFAULT_CONFIG_FILE = "config.xml";
	
	/**
	 * This is the entry point of the Code generator Sample Application.
	 * Please check the config.xml configuration file for Database and Connection settings.
	 * 
	 * @param args
	 *            arguments
	 */
	public static void main(String[] args) {
	    // Start code generator
		CodeGenerator app = new CodeGenerator();
		app.generate((args.length > 0 ? args[0] : DEFAULT_CONFIG_FILE));
	}	

    /**
     * Starts the actual generation according to the provided config file
     */
    public void generate(final String configFile) {
        // load configuration file
        CodeGenConfig config = loadConfig(configFile);
        // generate now
        generate(config);
    }   

    /**
     * Starts the actual generation according to the provided config file
     */
    public void generate(final CodeGenConfig config) {
        // get the DBMS
        DBMSHandler dbms = getDBMSHandler(config);
        // get the JDBC-Connection
        Connection conn = getJDBCConnection(config);
        // generate now
        generate(dbms, conn, config);
    }   
	
	/**
	 * Starts the actual generation according to the provided configuration
	 */
	public void generate(DBMSHandler dbms, Connection conn, CodeGenConfig config) {
		
	    // log all options
		listOptions(config);
		
		// read the database model
		// CodeGenParser parser = new CodeGenParser(config);
		DBModelParser modelParser = dbms.createModelParser(config.getDbCatalog(), config.getDbSchema());
		// set options
		modelParser.setStandardIdentityColumnName (config.getIdentityColumn());
		modelParser.setStandardTimestampColumnName(config.getTimestampColumn());
		// parse now
        modelParser.parseModel(conn);

        log.info("Model parsing completed successfully!");
        
		// create the source-code for that database
		CodeGenWriter codeGen = new CodeGenWriter(config);
		codeGen.generateCodeFiles(modelParser.getDatabase());
		
		log.info("Code generation completed successfully!");
	}
	
	/**
	 * Loads the configuration file and
	 * @param configFile
	 * @return
	 */
	protected CodeGenConfig loadConfig(String configFile){
		// Init Configuration
		CodeGenConfig config = new CodeGenConfig();
		config.init(configFile);
		return config;
	}
	
	protected void listOptions(CodeGenConfig config){
		// List options
		log.info("Config options are:");
		log.info("SchemaName=" + String.valueOf(config.getDbSchema()));
		log.info("TimestampColumn="	+ String.valueOf(config.getTimestampColumn()));
		log.info("TargetFolder=" + config.getTargetFolder());
		log.info("PackageName=" + config.getPackageName());
		log.info("DbClassName=" + config.getDbClassName());
		log.info("TableBaseName=" + config.getTableBaseName());
		log.info("ViewBaseName=" + config.getViewBaseName());
		log.info("RecordBaseName=" + config.getRecordBaseName());
		log.info("TableClassPrefix=" + config.getTableClassPrefix());
		log.info("ViewClassPrefi=" + config.getViewClassPrefix());
		log.info("NestTable=" + config.isNestTables());
		log.info("NestViews=" + config.isNestViews());
		log.info("CreateRecordProperties=" + config.isCreateRecordProperties());
	}
    
    /**
     * <PRE>
     * Opens and returns a JDBC-Connection.
     * JDBC url, user and password for the connection are obtained from the SampleConfig bean
     * Please use the config.xml file to change connection params.
     * </PRE>
     */
	protected Connection getJDBCConnection(CodeGenConfig config)
    {
        String jdbcDriverClass = config.getJdbcClass();
        try
        {   // Get Driver Class Name
            if (StringUtils.isEmpty(jdbcDriverClass))
                throw new CodeGenConfigInvalidException("jdbcClass", jdbcDriverClass);
            // Getting the JDBC-Driver
            Class.forName(jdbcDriverClass).newInstance();
            // Connect to the database
            String jdbcURL = config.getJdbcURL();
            if (StringUtils.isEmpty(jdbcURL))
                throw new CodeGenConfigInvalidException("jdbcURL", jdbcURL);
            String jdbcUser = config.getJdbcUser();
            if (StringUtils.isEmpty(jdbcUser))
                throw new CodeGenConfigInvalidException("jdbcUser", jdbcUser);
            log.info("Connecting to Database'" + jdbcURL + "' / User=" + jdbcUser);
            Connection conn = DriverManager.getConnection(config.getJdbcURL(), config.getJdbcUser(), config.getJdbcPwd());
            log.info("Connected successfully");
            // set the AutoCommit to false for this connection. 
            // commit must be called explicitly! 
            conn.setAutoCommit(false);
            log.info("AutoCommit has been set to " + conn.getAutoCommit());
            return conn;
        }
        catch (InstantiationException | IllegalAccessException | ClassNotFoundException e)
        {
            throw new CodeGenConfigInvalidException("jdbcClass", jdbcDriverClass, e);
        }
        catch (SQLException e)
        {
            throw new CodeGenConfigInvalidException("jdbcURL/jdbUser", config.getJdbcURL()+"/"+config.getJdbcUser(), e);
        }
    }

    /**
     * Creates an Empire-db DatabaseDriver for the given provider and applies dbms specific configuration 
     */
	protected DBMSHandler getDBMSHandler(CodeGenConfig config)
    {
        // Create dbms
        String dbmsHandlerClass = config.getDbmsHandlerClass();
        try
        {   // Get DBMSHandler class
            if (StringUtils.isEmpty(dbmsHandlerClass))
                throw new CodeGenConfigInvalidException("dbmsHandlerClass", dbmsHandlerClass);
            // Find class
            DBMSHandler dbms = (DBMSHandler) Class.forName(dbmsHandlerClass).newInstance();
            // Configure dbms
            try {
                config.readProperties(dbms, "dbmsHandlerClass-properties");
            } catch(Exception e) {
                log.info("No DbmsHandlerClass-properties provieded.");
            }
            // done
            return dbms;
        }
        catch (InstantiationException | IllegalAccessException | ClassNotFoundException e)
        {
            throw new CodeGenConfigInvalidException("dbmsHandlerClass", dbmsHandlerClass, e);
        }
    }

}
