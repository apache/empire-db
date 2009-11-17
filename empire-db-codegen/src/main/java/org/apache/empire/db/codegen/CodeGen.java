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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.sql.Connection;
import java.sql.DriverManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.empire.commons.ErrorObject;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.DBTable;
import org.apache.empire.db.codegen.util.FileUtils;
import org.apache.empire.db.codegen.util.ParserUtil;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;

/**
 * This is the entry class for generating the java persistence model based on a
 * database schema. It uses the Empire DB open-source framework to build a java
 * persistence layer for an application. The Apache Velocity template engine is
 * used to create the output interfaces and classes.
 * 
 * The Empire DB framework doesn't try to hide the underlying database and data
 * model but instead embraces its power by modeling it within java. The result
 * is a persistence layer that uses a more "object-oriented, type safe" SQL to
 * access persistent data.
 * 
 * NOTE: THIS VERSION HAS SEVERE RESTRICTIONS: 1. Only tables are currently
 * modeled (we'll add views to a later version). 2. Table indexes are not yet
 * modeled (exception is primary key). Again, this will be added to later
 * editions. 3. It is assumed that each table has a single INTEGER
 * auto-generated primary key column that has the same name for all tables. 4.
 * It is assumed that each table has a single TIMESTAMP optimistic locking
 * column that has the same name for all tables.
 */

public class CodeGen {
	private static final Log log = LogFactory.getLog(CodeGen.class);

	// Templates
	public static final String TEMPLATE_PATH = "src/main/resources/templates/";
	public static final String DATABASE_TEMPLATE = "Database.vm";
	public static final String BASE_TABLE_TEMPLATE = "BaseTable.vm";
	public static final String TABLE_TEMPLATE = "Table.vm";
	public static final String BASE_RECORD_TEMPLATE = "BaseRecord.vm";
	public static final String RECORD_TEMPLATE = "Record.vm";

	// Properties
	private CodeGenConfig config;
	private File baseDir;
	private File tableDir;
	private File recordDir;

	/**
	 * Constructor
	 */
	public CodeGen() {
		try {
			Velocity.init();
		} catch (Exception e) {
			log.fatal(e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * <PRE>
	 * This is the entry point of the Empire-DB Sample Application
	 * Please check the config.xml configuration file for Database and Connection settings.
	 * </PRE>
	 * 
	 * @param args
	 *            arguments
	 */
	public static void main(String[] args) {
		Connection conn = null;
		try {
			// Init Configuration
			CodeGenConfig config = new CodeGenConfig();
			config.init((args.length > 0 ? args[0] : "config.xml"));

			// Enable Exceptions
			ErrorObject.setExceptionsEnabled(true);

			// Get a JDBC Connection
			conn = getJDBCConnection(config);

			// List options
			log.info("Database connection successful. Config options are:");
			log.info("SchemaName=" + String.valueOf(config.getDbSchema()));
			log.info("TimestampColumn="
					+ String.valueOf(config.getTimestampColumn()));
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
			log.info("CreateRecordProperties="
					+ config.isCreateRecordProperties());

			if (config.getTableClassPrefix() == null)
				config.setTableClassPrefix("");

			if (config.getTableClassSuffix() == null)
				config.setTableClassSuffix("");

			CodeGen codeGen = new CodeGen();
			// create the database in the memory
			DBDatabase db = codeGen.parseDataModel(conn, config);

			// create the source-code for that database
			codeGen.generateCodeFiles(db, config);

			log.info("Code generation completed sucessfully!");

		} catch (Exception e) {
			// Error
			log.error(e.getMessage(), e);
		} finally {
			// done
			if (conn != null)
				close(conn);
		}
	}

	/**
	 * <PRE>
	 * Opens and returns a JDBC-Connection.
	 * JDBC url, user and password for the connection are obained from the SampleConfig bean
	 * Please use the config.xml file to change connection params.
	 * </PRE>
	 */
	private static Connection getJDBCConnection(CodeGenConfig config) {
		// Establish a new database connection
		Connection conn = null;
		log.info("Connecting to Database'" + config.getJdbcURL() + "' / User="
				+ config.getJdbcUser());
		try {
			// Connect to the databse
			Class.forName(config.getJdbcClass()).newInstance();
			conn = DriverManager.getConnection(config.getJdbcURL(), config
					.getJdbcUser(), config.getJdbcPwd());
			log.info("Connected successfully");
			// set the AutoCommit to false this session. You must commit
			// explicitly now
			conn.setAutoCommit(true);
			log.info("AutoCommit is " + conn.getAutoCommit());

		} catch (Exception e) {
			log.fatal("Failed to connect directly to '" + config.getJdbcURL()
					+ "' / User=" + config.getJdbcUser(), e);
			throw new RuntimeException(e);
		}
		return conn;
	}

	/**
	 * Closes a JDBC-Connection.
	 */
	private static void close(Connection conn) {
		log.info("Closing database connection");
		try {
			conn.close();
		} catch (Exception e) {
			log.fatal("Error closing connection", e);
		}
	}

	public DBDatabase parseDataModel(Connection conn, CodeGenConfig config) {
		this.config = config;
		CodeGenParser cgp = new CodeGenParser(conn, config);
		DBDatabase memoryDB = cgp.getDb();
		return memoryDB;
	}

	public void generateCodeFiles(DBDatabase db, CodeGenConfig config) {
		this.config = config;

		// Prepare directories for generated source files
		this.initDirectories(config.getTargetFolder(), config.getPackageName());

		// Create the DB class
		this.createDatabaseClass(db);

		// Create base table class
		this.createBaseTableClass(db);

		// Create base record class
		this.createBaseRecordClass(db);
		// Create table classes, record interfaces and record classes
		for (DBTable table : db.getTables()) {
			this.createTableClass(db, table);
			this.createRecordClass(db, table);
		}
	}

	private void initDirectories(String srcLocation, String packageName) {
		// Create the directory structure for the generated source code.
		File baseDir = new File(srcLocation);
		if (!baseDir.exists()) {
			baseDir.mkdirs();
		}
		StringBuilder sb = new StringBuilder();
		sb.append(srcLocation).append("/");
		sb.append(packageName.replaceAll("\\.", "/"));
		this.baseDir = new File(sb.toString());
		if (!this.baseDir.exists()) {
			this.baseDir.mkdirs();
		}

		// Clean out the directory so old code is wiped out.
		FileUtils.cleanDirectory(this.baseDir);

		// Create the table package directory
		this.tableDir = new File(this.baseDir, "tables");
		this.tableDir.mkdir();

		// Create the record package directory
		this.recordDir = new File(this.baseDir, "records");
		this.recordDir.mkdir();
	}

	private void createDatabaseClass(DBDatabase db) {
		ParserUtil pUtil = new ParserUtil(config);
		File file = new File(baseDir, config.getDbClassName() + ".java");
		VelocityContext context = new VelocityContext();
		context.put("parser", pUtil);
		context.put("tableClassSuffix", config.getTableClassSuffix());
		context.put("basePackageName", config.getPackageName());
		context.put("dbClassName", config.getDbClassName());
		context.put("tableSubPackage", "tables");
		context.put("database", db);
		writeFile(file, TEMPLATE_PATH + "/" + DATABASE_TEMPLATE, context);
	}

	private void createBaseTableClass(DBDatabase db) {
		File file = new File(tableDir, config.getTableBaseName() + ".java");
		VelocityContext context = new VelocityContext();
		context.put("tablePackageName", config.getPackageName() + ".tables");
		context.put("baseTableClassName", config.getTableBaseName());
		writeFile(file, TEMPLATE_PATH + "/" + BASE_TABLE_TEMPLATE, context);
	}

	private void createTableClass(DBDatabase db, DBTable table) {
		ParserUtil pUtil = new ParserUtil(config);
		File file = new File(tableDir, pUtil.getTableClassName(table.getName())
				+ ".java");
		VelocityContext context = new VelocityContext();
		context.put("parser", pUtil);
		context.put("basePackageName", config.getPackageName());
		context.put("tablePackageName", config.getPackageName() + ".tables");
		context.put("baseTableClassName", config.getTableBaseName());
		context.put("dbClassName", config.getDbClassName());
		context.put("table", table);
		writeFile(file, TEMPLATE_PATH + "/" + TABLE_TEMPLATE, context);
	}

	private void createBaseRecordClass(DBDatabase db) {
		File file = new File(recordDir, config.getRecordBaseName() + ".java");
		VelocityContext context = new VelocityContext();
		context.put("baseRecordClassName", config.getRecordBaseName());
		context.put("basePackageName", config.getPackageName());
		context.put("tablePackageName", config.getPackageName() + ".tables");
		context.put("recordPackageName", config.getPackageName() + ".records");
		context.put("baseTableClassName", config.getTableBaseName());
		writeFile(file, TEMPLATE_PATH + "/" + BASE_RECORD_TEMPLATE, context);
	}

	private void createRecordClass(DBDatabase db, DBTable table) {
		ParserUtil pUtil = new ParserUtil(config);
		File file = new File(recordDir, pUtil.getRecordClassName(table
				.getName())
				+ ".java");
		VelocityContext context = new VelocityContext();
		context.put("parser", pUtil);
		context.put("basePackageName", config.getPackageName());
		context.put("tablePackageName", config.getPackageName() + ".tables");
		context.put("recordPackageName", config.getPackageName() + ".records");
		context.put("baseRecordClassName", config.getRecordBaseName());
		context.put("dbClassName", config.getDbClassName());
		context
				.put("createRecordProperties", config
						.isCreateRecordProperties());

		context.put("table", table);
		writeFile(file, TEMPLATE_PATH + "/" + RECORD_TEMPLATE, context);
	}

	private static void writeFile(File file, String templatePath,
			VelocityContext context) {
		try {
			Template template = Velocity.getTemplate(templatePath);
			Writer writer = new FileWriter(file);
			template.merge(context, writer);
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ResourceNotFoundException e) {
			e.printStackTrace();
		} catch (ParseErrorException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
