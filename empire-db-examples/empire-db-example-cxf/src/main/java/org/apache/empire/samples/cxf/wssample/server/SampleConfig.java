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
package org.apache.empire.samples.cxf.wssample.server;

import org.apache.empire.xml.XMLConfiguration;

public class SampleConfig extends XMLConfiguration {

	private String databaseProvider = "oracle";

	private String jdbcClass = "oracle.jdbc.driver.OracleDriver";

	private String jdbcURL = "jdbc:oracle:thin:@192.168.0.2:1521:ora10";

	private String jdbcUser = "DBSAMPLE";

	private String jdbcPwd = "DBSAMPLE";

	private String schemaName = "DBSAMPLE";

	/**
	 * Initialize the configuration
	 * 
	 * @param filename the file
	 * 
	 * @return true on success
	 */
	public void init(String filename) {
		// Read the properties file
		super.init(filename, false);
		// Done
		readProperties(this, "properties");
		// Reader Provider Properties
		readProperties(this, "properties-" + databaseProvider);
	}

	public String getDatabaseProvider() {
		return databaseProvider;
	}

	public String getJdbcClass() {
		return jdbcClass;
	}

	public void setJdbcClass(String jdbcClass) {
		this.jdbcClass = jdbcClass;
	}

	public String getJdbcPwd() {
		return jdbcPwd;
	}

	public void setJdbcPwd(String jdbcPwd) {
		this.jdbcPwd = jdbcPwd;
	}

	public String getJdbcURL() {
		return jdbcURL;
	}

	public String getSchemaName() {
		return schemaName;
	}

	// ------- Setters -------

	public void setDatabaseProvider(String databaseProvider) {
		this.databaseProvider = databaseProvider;
	}
	
	public void setJdbcURL(String jdbcURL) {
		this.jdbcURL = jdbcURL;
	}

	public String getJdbcUser() {
		return jdbcUser;
	}

	public void setJdbcUser(String jdbcUser) {
		this.jdbcUser = jdbcUser;
	}

	public void setSchemaName(String schemaName) {
		this.schemaName = schemaName;
	}

}
