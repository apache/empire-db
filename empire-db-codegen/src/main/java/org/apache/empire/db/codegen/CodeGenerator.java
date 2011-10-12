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

import org.apache.empire.db.DBDatabase;
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
	 * Starts the actual generation according to the provided configuration
	 */
	public void generate(CodeGenConfig config) {
		// log all options
		listOptions(config);
		
		// read the database model
		CodeGenParser parser = new CodeGenParser(config);
		DBDatabase db = parser.loadDbModel();
		
		// create the source-code for that database
		CodeGenWriter codeGen = new CodeGenWriter(config);
		codeGen.generateCodeFiles(db);
		
		log.info("Code generation completed successfully!");
	}

	/**
	 * Starts the actual generation according to the provided config file
	 */
	public void generate(final String file) {
		// load configuration file
		CodeGenConfig config = loadConfig(file);
		generate(config);
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

		if (config.getTableClassPrefix() == null)
			config.setTableClassPrefix("");

		if (config.getTableClassSuffix() == null)
			config.setTableClassSuffix("");
		
		return config;
	}
	
	protected void listOptions(CodeGenConfig config){
		// List options
		log.info("Database connection successful. Config options are:");
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

}
