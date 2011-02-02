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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.DBTable;
import org.apache.empire.db.DBView;
import org.apache.empire.db.codegen.util.FileUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.log.CommonsLogLogChute;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;

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
 * <li>Only tables are currently modeled (we'll add views to a later version).</li>
 * <li>Table indexes are not yet modeled (exception is primary key). Again, this
 * will be added to later editions.</li>
 * <li>It is assumed that each table has a single INTEGER auto-generated primary
 * key column that has the same name for all tables.</li>
 * <li>It is assumed that each table has a single TIMESTAMP optimistic locking
 * column that has the same name for all tables.</li>
 * </ol>
 */

public class CodeGenWriter {
	private static final Logger log = LoggerFactory.getLogger(CodeGenWriter.class);

	// Templates
	public static final String DATABASE_TEMPLATE = "Database.vm";
	public static final String BASE_TABLE_TEMPLATE = "BaseTable.vm";
	public static final String TABLE_TEMPLATE = "Table.vm";
	public static final String BASE_VIEW_TEMPLATE = "BaseView.vm";
	public static final String VIEW_TEMPLATE = "View.vm";
	public static final String BASE_RECORD_TEMPLATE = "BaseRecord.vm";
	public static final String RECORD_TEMPLATE = "Record.vm";

	// Services
	private final WriterService writerService;
	private final VelocityEngine engine;
	
	// Properties
	private final CodeGenConfig config;
	private File baseDir;
	private File tableDir;
	private File recordDir;
	private File viewDir;

	/**
	 * Constructor
	 */
	public CodeGenWriter(CodeGenConfig config) {
		this.writerService = new WriterService(config);
		this.config = config;
		this.engine = new VelocityEngine();
		// we have to keep this in sync with our logging system
		// http://velocity.apache.org/engine/releases/velocity-1.5/developer-guide.html#simpleexampleofacustomlogger
		engine.setProperty(RuntimeConstants.RUNTIME_LOG_LOGSYSTEM,
				new CommonsLogLogChute());
		if(config.getTemplateFolder() == null){
			engine.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
			engine.setProperty("classpath." + VelocityEngine.RESOURCE_LOADER + ".class", ClasspathResourceLoader.class.getName());
			config.setTemplateFolder("templates");
		}else{
			File templateFolder = new File(config.getTemplateFolder());
			if(!templateFolder.canRead()){
				throw new RuntimeException("Provided template folder missing or not readable: " + config.getTemplateFolder());
			}
		}
		
		try {
			engine.init();
		} catch (Exception e) {
			log.error("A Exception occured on initializing the velocity-engine:", e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * Generates the java code files for the database
	 * 
	 * @param db
	 *            the DBDatabase to generate files for
	 */
	public List<File> generateCodeFiles(DBDatabase db) {
		List<File> generatedFiles = new ArrayList<File>();

		// Prepare directories for generated source files
		this.initDirectories(config);

		// Create the DB class
		generatedFiles.add(this.createDatabaseClass(db));

		// Create base table class
		generatedFiles.add(this.createBaseTableClass(db));
		
		// Create base view class
		generatedFiles.add(this.createBaseViewClass(db));

		// Create base record class
		generatedFiles.add(this.createBaseRecordClass(db));
		// Create table classes, record interfaces and record classes
		for (DBTable table : db.getTables()) {
			if (!config.isNestTables()) {
				// if table nesting is disabled, create separate table classes 
				generatedFiles.add(this.createTableClass(db, table));
			}
			generatedFiles.add(this.createRecordClass(db, table));
		}
		
		// Create view classes
		for (DBView view : db.getViews()) {
			if (!config.isNestViews()) {
				// if table nesting is disabled, create separate table classes 
				generatedFiles.add(this.createViewClass(db, view));
			}
		}
		return generatedFiles;
	}

	
	private void initDirectories(CodeGenConfig config) {
		// Create the directory structure for the generated source code.
		File targetDir = new File(config.getTargetFolder());
		if (!targetDir.exists()) {
			targetDir.mkdirs();
		}

		// Create the base package directory
		this.baseDir = FileUtils.getFileFromPackage(targetDir, config.getPackageName());

		// Clean out the directory so old code is wiped out.
		FileUtils.cleanDirectory(this.baseDir);

		// Create the table package directory
		this.tableDir = FileUtils.getFileFromPackage(targetDir, config.getTablePackageName());

		// Create the record package directory
		this.recordDir = FileUtils.getFileFromPackage(targetDir, config.getRecordPackageName());
		
		// Create the record package directory
		this.viewDir = FileUtils.getFileFromPackage(targetDir, config.getViewPackageName());
	}

	private File createDatabaseClass(DBDatabase db) {
		File file = new File(baseDir, config.getDbClassName() + ".java");
		VelocityContext context = new VelocityContext();
		// TODO fall back to getPackageName() is the other names are not set
		context.put("parser", writerService);
		context.put("tableClassSuffix", config.getTableClassSuffix());
		context.put("basePackageName", config.getPackageName());
		context.put("dbClassName", config.getDbClassName());
		context.put("tablePackageName", config.getTablePackageName());
		context.put("viewPackageName", config.getViewPackageName());
		context.put("database", db);
		context.put("nestTables", config.isNestTables());
		context.put("baseTableClassName", config.getTableBaseName());
		context.put("nestViews", config.isNestViews());
		context.put("templateFolder", config.getTemplateFolder());
		context.put("baseViewClassName", config.getViewBaseName());

		writeFile(file, DATABASE_TEMPLATE, context);
		return file;
	}

	private File createBaseTableClass(DBDatabase db) {
		File file = new File(tableDir, config.getTableBaseName() + ".java");
		VelocityContext context = new VelocityContext();
		context.put("tablePackageName", config.getTablePackageName());
		context.put("baseTableClassName", config.getTableBaseName());
		writeFile(file, BASE_TABLE_TEMPLATE, context);
		return file;
	}

	private File createTableClass(DBDatabase db, DBTable table) {
		File file = new File(tableDir, writerService.getTableClassName(table.getName())
				+ ".java");
		VelocityContext context = new VelocityContext();
		context.put("parser", writerService);
		context.put("basePackageName", config.getPackageName());
		context.put("tablePackageName", config.getTablePackageName());
		context.put("baseTableClassName", config.getTableBaseName());
		context.put("dbClassName", config.getDbClassName());
		context.put("nestTables", config.isNestTables());
		context.put("table", table);
		writeFile(file, TABLE_TEMPLATE, context);
		return file;
	}
	
	private File createBaseViewClass(DBDatabase db) {
		File file = new File(viewDir, config.getViewBaseName() + ".java");
		VelocityContext context = new VelocityContext();
		context.put("viewPackageName", config.getViewPackageName());
		context.put("baseViewClassName", config.getViewBaseName());
		writeFile(file, BASE_VIEW_TEMPLATE, context);
		return file;
	}

	private File createViewClass(DBDatabase db, DBView view) {
		File file = new File(viewDir, writerService.getViewClassName(view.getName())
				+ ".java");
		VelocityContext context = new VelocityContext();
		context.put("parser", writerService);
		context.put("basePackageName", config.getPackageName());
		context.put("viewPackageName", config.getViewPackageName());
		context.put("baseViewClassName", config.getViewBaseName());
		context.put("dbClassName", config.getDbClassName());
		context.put("nestViews", config.isNestViews());
		context.put("view", view);
		writeFile(file, VIEW_TEMPLATE, context);
		return file;
	}

	private File createBaseRecordClass(DBDatabase db) {
		File file = new File(recordDir, config.getRecordBaseName() + ".java");
		VelocityContext context = new VelocityContext();
		context.put("baseRecordClassName", config.getRecordBaseName());
		context.put("basePackageName", config.getPackageName());
		context.put("tablePackageName", config.getTablePackageName());
		context.put("recordPackageName", config.getRecordPackageName());
		context.put("baseTableClassName", config.getTableBaseName());
		writeFile(file, BASE_RECORD_TEMPLATE, context);
		return file;
	}

	private File createRecordClass(DBDatabase db, DBTable table) {
		File file = new File(recordDir, writerService.getRecordClassName(table.getName()) + ".java");
		VelocityContext context = new VelocityContext();
		context.put("parser", writerService);
		context.put("basePackageName", config.getPackageName());
		// If the tables shall be nested within the database classe, their include path for the records needs to be changed
		if (config.isNestTables())
			context.put("tablePackageName", config.getPackageName() + "." + config.getDbClassName());
		else
			context.put("tablePackageName", config.getTablePackageName());
		context.put("recordPackageName", config.getRecordPackageName());
		context.put("baseRecordClassName", config.getRecordBaseName());
		context.put("dbClassName", config.getDbClassName());
		context
				.put("createRecordProperties", config
						.isCreateRecordProperties());

		context.put("table", table);
		writeFile(file, RECORD_TEMPLATE, context);
		return file;
	}

	private void writeFile(File file, String template,
			VelocityContext context) {
		String templatePath = config.getTemplateFolder()+ System.getProperty("file.separator") +template;
		Writer writer = null;
		try {
			log.info("Writing " + file);
			Template velocityTemplate = engine.getTemplate(templatePath);
			writer = new FileWriter(file);
			velocityTemplate.merge(context, writer);
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
