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

import org.apache.empire.xml.XMLConfiguration;

public class CodeGenConfig extends XMLConfiguration {
	private String jdbcClass;

	private String jdbcURL;

	private String jdbcUser;

	private String jdbcPwd;

	// generation options
	/**
	 * name of the database catalog (may be null)
	 */
	private String dbCatalog = null;

	/**
	 * name of the database schema (may be null)
	 */
	private String dbSchema = null;

	/**
	 * name of the table pattern (may be null)
	 */
	private String dbTablePattern = null;
	/**
	 * Name of the timestamp column used for optimistic locking (may be null)
	 * e.g. "UPDATE_TIMESTAMP";
	 */
	private String timestampColumn = null;

	/**
	 * name of the target folder
	 */
	private String targetFolder = "target/generated/db";
	
	/**
	 * name of the template folder
	 */
	private String templateFolder;	
	
	/**
	 * name of the target package
	 */
	private String packageName = "org.foo.db";
	
	/**
	 * name of the table target package
	 */
	private String tablePackageName;
	
	/**
	 * name of the record target package
	 */
	private String recordPackageName;
	
	/**
	 * name of the view target package
	 */
	private String viewPackageName;

	/**
	 * Target name of the generated database class. This class extends
	 * DBDatabase.
	 */
	private String dbClassName = "MyDB";
	
	/**
	 * Target name of the generated table class. This class extends DBTable and
	 * is the base class for all generated individual table classes.
	 */
	private String tableBaseName = "BaseTable";
	
	/**
	 * Target name of the generated view class. This class extends DBView and is
	 * the base class for all generated individual view classes.
	 */
	private String viewBaseName = "BaseView";
	
	/**
	 * Target name of the generated record class. This is a template class that
	 * extends DBRecord as follows:<br/>
	 * 
	 * <pre>
	 * XXRecord&lt;T extends XXTable&gt; extends DBRecord
	 * </pre>
	 * 
	 * <br/>
	 */
	private String recordBaseName = "BaseRecord";
	
	/**
	 * Prefix used for generating table class names.<br/>
	 * The Table name is appended after the prefix starting with captial letter
	 * followed by lower case letters.<br/>
	 * Occurrence an of underscore indicates a new word which will again start
	 * with a capital letter.<br/>
	 * e.g.<br/>
	 * <ul>
	 * <li>Table "names" -> Class "XXNames"</li>
	 * <li>Table "flight_bookings" -> Class "XXFlightBookings"</li>
	 * </ul>
	 * Where XX is the prefix.
	 */
	private String tableClassPrefix = "";

	/**
	 * Suffix used for generating table class names.<br/>
	 * The Table name is appended before the suffix starting with captial letter
	 * followed by lower case letters.<br/>
	 * Occurrence an of underscore indicates a new word which will again start
	 * with a capital letter.<br/>
	 * e.g.<br/>
	 * <ul>
	 * <li>Table "names" -> Class "NamesTable"</li>
	 * <li>Table "flight_bookings" -> Class "FlightBookingsTable"</li>
	 * </ul>
	 * Where "Table" is the suffix.
	 */
	private String tableClassSuffix = "";
	
	/**
	 * Prefix used for generating view class names.<br/>
	 * The Table name is appended after the prefix starting with captial letter
	 * followed by lower case letters.<br/>
	 * Occurrence an of underscore indicates a new word which will again start
	 * with a capital letter.<br/>
	 * See naming of table classes above.
	 */
	private String viewClassPrefix = "";
	
	/**
	 * Suffix used for generating view class names.<br/>
	 * The View name is appended before the suffix starting with captial letter
	 * followed by lower case letters.<br/>
	 * Occurrence an of underscore indicates a new word which will again start
	 * with a capital letter.<br/>
	 * e.g.<br/>
	 * <ul>
	 * <li>View "names" -> Class "NamesView"</li>
	 * <li>View "flight_bookings" -> Class "FlightBookingsView"</li>
	 * </ul>
	 * Where "View" is the suffix.
	 */
	private String viewClassSuffix = "";

	/**
	 * if TRUE table classes should be declared as inner classes of DBDatabase.<br/>
	 * if FALSE table classes should be declared as top level classes.
	 */
	private boolean nestTables;
	
	/**
	 * if TRUE view classes should be declared as inner classes of DBDatabase.<br/>
	 * if FALSE view classes should be declared as top level classes.
	 */
	private boolean nestViews;
	
	/**
	 * if TRUE record classes should have a getter and setter for each field.<br/>
	 * Otherwiese getters / setters are omitted.
	 */
	private boolean createRecordProperties;

	/**
	 * Initialize the configuration.
	 * 
	 * @param filename
	 *            the file to read
	 * 
	 * @return true on succes
	 */
	public boolean init(String filename) {
		// Read the properties file
		if (super.init(filename, false, true) == false)
			return false;
		// Done
		if (readProperties(this, "properties") == false)
			return false;
		// Reader Provider Properties
		return true;
	}

	public String getJdbcClass() {
		return jdbcClass;
	}

	public void setJdbcClass(String jdbcClass) {
		this.jdbcClass = jdbcClass;
	}

	public String getJdbcURL() {
		return jdbcURL;
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

	public String getJdbcPwd() {
		return jdbcPwd;
	}

	public void setJdbcPwd(String jdbcPwd) {
		this.jdbcPwd = jdbcPwd;
	}

	// ------- generation options -------

	public String getDbCatalog() {
		return dbCatalog;
	}

	public void setDbCatalog(String dbCatalog) {
		this.dbCatalog = dbCatalog;
	}

	public String getDbSchema() {
		return dbSchema;
	}

	public void setDbSchema(String dbSchema) {
		this.dbSchema = dbSchema;
	}

	public String getDbTablePattern() {
		return dbTablePattern;
	}

	public void setDbTablePattern(String dbTablePattern) {
		this.dbTablePattern = dbTablePattern;
	}

	public String getTimestampColumn() {
		return timestampColumn;
	}

	public void setTimestampColumn(String timestampColumn) {
		this.timestampColumn = timestampColumn;
	}

	public String getTargetFolder() {
		return targetFolder;
	}

	public void setTargetFolder(String targetFolder) {
		this.targetFolder = targetFolder;
	}
	
	public String getTemplateFolder() {
		return templateFolder;
	}

	public void setTemplateFolder(String templateFolder) {
		this.templateFolder = templateFolder;
	}
	
	public String getPackageName() {
		return packageName;
	}

	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}
	
	public String getTablePackageName() {
		return fallback(tablePackageName, "tables");
	}

	public void setTablePackageName(String tablePackageName) {
		this.tablePackageName = tablePackageName;
	}
	public String getRecordPackageName() {
		return fallback(recordPackageName, "records");
	}

	public void setRecordPackageName(String recordPackageName) {
		this.recordPackageName = recordPackageName;
	}
	public String getViewPackageName() {
		return fallback(viewPackageName, "views");
	}

	public void setViewPackageName(String viewPackageName) {
		this.viewPackageName = viewPackageName;
	}

	public String getDbClassName() {
		return dbClassName;
	}

	public void setDbClassName(String dbClassName) {
		this.dbClassName = dbClassName;
	}

	public String getTableBaseName() {
		return tableBaseName;
	}

	public void setTableBaseName(String tableBaseName) {
		this.tableBaseName = tableBaseName;
	}

	public String getViewBaseName() {
		return viewBaseName;
	}

	public void setViewBaseName(String viewBaseName) {
		this.viewBaseName = viewBaseName;
	}

	public String getRecordBaseName() {
		return recordBaseName;
	}

	public void setRecordBaseName(String recordBaseName) {
		this.recordBaseName = recordBaseName;
	}

	public String getTableClassPrefix() {
		return tableClassPrefix;
	}

	public void setTableClassPrefix(String tableClassPrefix) {
		this.tableClassPrefix = tableClassPrefix;
	}

	public String getTableClassSuffix() {
		return tableClassSuffix;
	}

	public void setTableClassSuffix(String tableClassSuffix) {
		this.tableClassSuffix = tableClassSuffix;
	}

	public String getViewClassPrefix() {
		return viewClassPrefix;
	}

	public void setViewClassPrefix(String viewClassPrefix) {
		this.viewClassPrefix = viewClassPrefix;
	}
	
	public String getViewClassSuffix() {
		return viewClassSuffix;
	}

	public void setViewClassSuffix(String viewClassSuffix) {
		this.viewClassSuffix = viewClassSuffix;
	}

	public boolean isNestTables() {
		return nestTables;
	}

	public void setNestTables(boolean nestTables) {
		this.nestTables = nestTables;
	}

	public boolean isNestViews() {
		return nestViews;
	}

	public void setNestViews(boolean nestViews) {
		this.nestViews = nestViews;
	}

	public boolean isCreateRecordProperties() {
		return createRecordProperties;
	}

	public void setCreateRecordProperties(boolean createRecordProperties) {
		this.createRecordProperties = createRecordProperties;
	}

	private String fallback(String packageName, String defaultSubpackage){
		String pkg = packageName;
		if( pkg == null && this.packageName != null){
			pkg = this.packageName + "." + defaultSubpackage;
		}
		return pkg;
	}
}
