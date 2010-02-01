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
package org.apache.empire.db.maven;

import java.io.File;

import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.codegen.CodeGenConfig;
import org.apache.empire.db.codegen.CodeGenParser;
import org.apache.empire.db.codegen.CodeGenWriter;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

/**
 * Generates code by reading an existing database schema
 * 
 * @goal codegen
 * 
 * @phase generate-sources
 */
public class CodeGenMojo extends AbstractMojo {

	/**
	 * @parameter expression="${project}" 
	 * @readonly
	 */
	private MavenProject project;
	
	/**
	 * Codegen configuration file, if the file is provided, only that file
	 * is used to configure code generation
	 * 
	 * @parameter expression="${empiredb.configFile}"
	 */
	private File configFile;

	/**
	 * Location of the generated sources.
	 * 
	 * @parameter 
	 *     expression="${empiredb.generatedsources}" 
	 *     default-value="${project.build.directory}/generated-sources/empiredb"
	 * @required
	 */
	private File targetDirectory;
	
	/**
	 * JDBC url
	 * 
	 * @parameter expression="${empiredb.jdbcURL}"
	 * @required
	 */
	private String jdbcURL;
	
	/**
	 * JDBC Driver class
	 * 
	 * @parameter expression="${empiredb.jdbcClass}"
	 * @required
	 */
	private String jdbcClass;
	
	/**
	 * JDBC Database user
	 * 
	 * @parameter expression="${empiredb.jdbcUser}"
	 */
	private String jdbcUser;
	
	/**
	 * JDBC Database password
	 * 
	 * @parameter expression="${empiredb.jdbcPwd}"
	 */
	private String jdbcPwd;
	
	/**
	 * Code generator template directory, if not set the default templates
	 * are loaded from the classpath
	 * 
	 * @parameter expression="${empiredb.templateDirectory}"
	 */
	private String templateDirectory;
	
	/**
	 * The package for the generated sources
	 * 
	 * @parameter expression="${empiredb.packageName}"
	 */
	private String packageName;

	public void execute() throws MojoExecutionException {
		
		setupLogging();
		
		CodeGenConfig config = new CodeGenConfig();
		if(configFile != null){
			getLog().info("Loading configuration file: " + configFile);
			config.init(configFile.getAbsolutePath());
		}else{
			config.setJdbcURL(jdbcURL);
			config.setJdbcClass(jdbcClass);
			config.setJdbcUser(jdbcUser);
			config.setJdbcPwd(jdbcPwd);
			config.setTargetFolder(targetDirectory.getAbsolutePath());
			config.setTemplateFolder(templateDirectory);
			config.setPackageName(packageName);
		}
		
		//config.setExceptionsEnabled(true);
		
		getLog().info("Generating code for " + jdbcURL + " ...");
		
		CodeGenParser parser = new CodeGenParser(config);
		DBDatabase db = parser.loadDbModel();
		
		CodeGenWriter codeGen = new CodeGenWriter(config);
		codeGen.generateCodeFiles(db);
		
		getLog().info("Code successfully generated in: " + targetDirectory);
		
		// we want the generate sources to be available in the project itself
		// TODO see if this is correct by loking at other codegen plugins
		// TODO add some code in the test project that uses the generated code
		project.addCompileSourceRoot(targetDirectory.getAbsolutePath());
		
	}

	private void setupLogging() {
		Logger logger = Logger.getRootLogger();
		logger.addAppender(new MavenAppender(this));
	}
	
	/**
	 * Forwards Log4j logging to maven logging
	 * 
	 */
	private static final class MavenAppender extends AppenderSkeleton {
		
		private final AbstractMojo mojo;
		
		public MavenAppender(final AbstractMojo mojo) {
			this.mojo = mojo;
		}
		
		@Override
		public boolean requiresLayout() {
			return false;
		}

		@Override
		public void close() {
			// nothing to do here
		}

		@Override
		protected void append(LoggingEvent event) {
			if(Level.INFO.equals(event.getLevel())){
				mojo.getLog().info(String.valueOf(event.getMessage()));
			}else if(Level.ERROR.equals(event.getLevel())){
				// TODO support throwables?
				mojo.getLog().error(String.valueOf(event.getMessage()));
			}else if(Level.WARN.equals(event.getLevel())){
				mojo.getLog().warn(String.valueOf(event.getMessage()));
			}
		}
	}
}
