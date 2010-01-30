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
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

/**
 * Codegen goal
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

	public void execute() throws MojoExecutionException {

		CodeGenConfig config = new CodeGenConfig();
		config.setJdbcURL(jdbcURL);
		config.setJdbcClass(jdbcClass);
		config.setJdbcUser(jdbcUser);
		config.setJdbcPwd(jdbcPwd);
		config.setTargetFolder(targetDirectory.getAbsolutePath());
		
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
	
}
