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
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.DBTable;
import org.apache.empire.db.codegen.util.FileUtils;
import org.apache.empire.db.codegen.util.ParserUtil;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.RuntimeConstants;

/**
 * This is the entry class for generating the java persistence model based on a
 * database schema. It uses the Empire DB open-source framework to build a java
 * persistence layer for an application. The Apache Velocity template engine is
 * used to create the output interfaces and classes.
 * <p>
 * The Empire DB framework doesn't try to hide the underlying database and data
 * model but instead embraces its power by modeling it within java. The result
 * is a persistence layer that uses a more "object-oriented, type safe" SQL to
 * access persistent data.
 * <p>
 * NOTE: THIS VERSION HAS SEVERE RESTRICTIONS:
 * <ol>
 * <li> Only tables are currently modeled (we'll add views to a later version).</li> 
 * <li> Table indexes are not yet modeled (exception is primary key). Again, 
 * this will be added to later editions.</li>  
 * <li> It is assumed that each table has a single INTEGER auto-generated primary
 * key column that has the same name for all tables. </li> 
 * <li> It is assumed that each table has a single TIMESTAMP optimistic locking
 * column that has the same name for all tables.</li> 
 * </ol>
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

	// Services
	private final ParserUtil pUtil;
	
	// Properties
	private final CodeGenConfig config;
	private File baseDir;
	private File tableDir;
	private File recordDir;

	/**
	 * Constructor
	 */
	public CodeGen(CodeGenConfig config) {
		Velocity.setProperty(RuntimeConstants.RUNTIME_LOG_LOGSYSTEM_CLASS,
        	"org.apache.velocity.runtime.log.SimpleLog4JLogSystem" );
		Velocity.setProperty("runtime.log.logsystem.log4j.category", "org.apache.velocity");
		try {
			Velocity.init();
		} catch (Exception e) {
			log.fatal(e);
			throw new RuntimeException(e);
		}
		this.pUtil = new ParserUtil(config);
		this.config = config;
	}

	/**
	 * Generates the java code files for the database
	 * @param db the DBDatabase to generate files for
	 */
	public List<File> generateCodeFiles(DBDatabase db) {
	    List<File> generatedFiles = new ArrayList<File>();

		// Prepare directories for generated source files
		this.initDirectories(config.getTargetFolder(), config.getPackageName());

		// Create the DB class
		generatedFiles.add(this.createDatabaseClass(db));

		// Create base table class
		generatedFiles.add(this.createBaseTableClass(db));

		// Create base record class
		generatedFiles.add(this.createBaseRecordClass(db));
		// Create table classes, record interfaces and record classes
		for (DBTable table : db.getTables()) 
		{
		    generatedFiles.add(this.createTableClass(db, table));
		    generatedFiles.add(this.createRecordClass(db, table));
		}
		return generatedFiles;
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

	private File createDatabaseClass(DBDatabase db) {
		File file = new File(baseDir, config.getDbClassName() + ".java");
		VelocityContext context = new VelocityContext();
		context.put("parser", pUtil);
		context.put("tableClassSuffix", config.getTableClassSuffix());
		context.put("basePackageName", config.getPackageName());
		context.put("dbClassName", config.getDbClassName());
		context.put("tableSubPackage", "tables");
		context.put("database", db);
		writeFile(file, TEMPLATE_PATH + "/" + DATABASE_TEMPLATE, context);
		return file;
	}

	private File createBaseTableClass(DBDatabase db) {
		File file = new File(tableDir, config.getTableBaseName() + ".java");
		VelocityContext context = new VelocityContext();
		context.put("tablePackageName", config.getPackageName() + ".tables");
		context.put("baseTableClassName", config.getTableBaseName());
		writeFile(file, TEMPLATE_PATH + "/" + BASE_TABLE_TEMPLATE, context);
		return file;
	}

	private File createTableClass(DBDatabase db, DBTable table) {
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
		return file;
	}

	private File createBaseRecordClass(DBDatabase db) {
		File file = new File(recordDir, config.getRecordBaseName() + ".java");
		VelocityContext context = new VelocityContext();
		context.put("baseRecordClassName", config.getRecordBaseName());
		context.put("basePackageName", config.getPackageName());
		context.put("tablePackageName", config.getPackageName() + ".tables");
		context.put("recordPackageName", config.getPackageName() + ".records");
		context.put("baseTableClassName", config.getTableBaseName());
		writeFile(file, TEMPLATE_PATH + "/" + BASE_RECORD_TEMPLATE, context);
		return file;
	}

	private File createRecordClass(DBDatabase db, DBTable table) {
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
		return file;
	}

	private static void writeFile(File file, String templatePath,
			VelocityContext context) {
		Writer writer = null;
		try {
			Template template = Velocity.getTemplate(templatePath);
			writer = new FileWriter(file);
			template.merge(context, writer);
		} catch (IOException e) {
			log.error(e.getMessage(), e);
		} catch (ResourceNotFoundException e) {
			log.error(e.getMessage(), e);
		} catch (ParseErrorException e) {
			log.error(e.getMessage(), e);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			FileUtils.close(writer);
		}

	}

}
