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
package org.apache.empire.db.eclipse;

import javax.xml.bind.annotation.XmlRootElement;

import org.apache.empire.exceptions.ItemNotFoundException;
import org.apache.empire.exceptions.ObjectNotValidException;
import org.apache.empire.xml.XMLConfiguration;
import org.apache.empire.xml.XMLUtil;
import org.apache.log4j.xml.DOMConfigurator;
import org.slf4j.Logger;
import org.w3c.dom.Element;

@XmlRootElement
public class CodeGenConfig extends XMLConfiguration {
	private static final Logger log = org.slf4j.LoggerFactory.getLogger(CodeGenConfig.class);

	// the logging configuration root node name
	private final String loggingNodeName = "log4j:configuration";

	private String configTitle;

	private String jdbcType;

	private String jdbcClass;

	private String jdbcURL;

	private String jdbcServer;

	private String jdbcPort;

	private String jdbcSID;

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
	private String targetFolder;

	/**
	 * name of the template folder
	 */
	private String templateFolder = "configuration\\org.apache.empire\\templates\\";

	/**
	 * name of the target package
	 */
	private String packageName;

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
	private String dbClassName;

	/**
	 * Target name of the generated table class. This class extends DBTable and
	 * is the base class for all generated individual table classes.
	 */
	private String tableBaseName;

	/**
	 * Target name of the generated view class. This class extends DBView and is
	 * the base class for all generated individual view classes.
	 */
	private String viewBaseName;

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
	private String recordBaseName;

	/**
	 * Prefix used for table member declarations
	 */
	private String tableNamePrefix = "";

	/**
	 * Prefix used for view member declarations
	 */
	private String viewNamePrefix = "";

	/**
	 * Prefix used for column member declarations
	 */
	private String columnNamePrefix = "";

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
	 * if TRUE table classes should be declared as inner classes of DBDatabase.
	 * <br/>
	 * if FALSE table classes should be declared as top level classes.
	 */
	private boolean nestTables = false;

	/**
	 * if TRUE view classes should be declared as inner classes of DBDatabase.
	 * <br/>
	 * if FALSE view classes should be declared as top level classes.
	 */
	private boolean nestViews = false;

	/**
	 * if TRUE record classes should have a getter and setter for each field.
	 * <br/>
	 * Otherwise getters / setters are omitted.
	 */
	private boolean createRecordProperties;

	/**
	 * true if names of tables and views should not be camel-cased
	 */
	private boolean preserverCharacterCase = false;

	/**
	 * true if names of foreign-key-relations should be preserved
	 */
	private boolean preserveRelationNames = false;

	/**
	 * Initialize the configuration.
	 * 
	 * @param filename
	 *            the file to read
	 */
	public void init(String filename) {
		// Read the properties file
		super.init(filename, false);

		// Init Logging
		initLogging();

		// Reader Provider Properties
		readProperties(this, "properties");
	}

	/**
	 * Init logging using Log4J's DOMConfigurator
	 * 
	 * @return
	 */
	private void initLogging() {
		// Get configuration root node
		Element rootNode = getRootNode();
		if (rootNode == null) {
			throw new ObjectNotValidException(getClass().getName());
		}
		// Find log configuration node
		Element loggingNode = XMLUtil.findFirstChild(rootNode, this.loggingNodeName);
		if (loggingNode == null) { // log configuration node not found
			CodeGenConfig.log.error("Log configuration node {} has not been found. Logging has not been configured.",
					this.loggingNodeName);
			throw new ItemNotFoundException(this.loggingNodeName);
		}
		// Init Log4J
		DOMConfigurator.configure(loggingNode);
		// done
		CodeGenConfig.log.info("Logging sucessfully configured from node {}.", this.loggingNodeName);
	}

	public String getJdbcClass() {
		return this.jdbcClass;
	}

	public void setJdbcClass(String jdbcClass) {
		this.jdbcClass = jdbcClass;
	}

	public String getJdbcURL() {
		return this.jdbcURL;
	}

	public void setJdbcURL(String jdbcURL) {
		this.jdbcURL = jdbcURL;
	}

	public String getJdbcUser() {
		return this.jdbcUser;
	}

	public void setJdbcUser(String jdbcUser) {
		this.jdbcUser = jdbcUser;
	}

	public String getJdbcPwd() {
		return this.jdbcPwd;
	}

	public void setJdbcPwd(String jdbcPwd) {
		this.jdbcPwd = jdbcPwd;
	}

	// ------- generation options -------

	public String getDbCatalog() {
		if(this.dbCatalog == "") {
			this.dbCatalog = null;
		}
		return this.dbCatalog;
	}

	public void setDbCatalog(String dbCatalog) {
		this.dbCatalog = dbCatalog;
	}

	public String getDbSchema() {
		if(this.dbSchema == "") {
			this.dbSchema = null;
		}
		return this.dbSchema;
	}

	public void setDbSchema(String dbSchema) {
		this.dbSchema = dbSchema;
	}

	public String getDbTablePattern() {
		return this.dbTablePattern;
	}

	public void setDbTablePattern(String dbTablePattern) {
		this.dbTablePattern = dbTablePattern;
	}

	public String getTimestampColumn() {
		return this.timestampColumn;
	}

	public void setTimestampColumn(String timestampColumn) {
		this.timestampColumn = timestampColumn;
	}

	public String getTargetFolder() {
		return this.targetFolder;
	}

	public void setTargetFolder(String targetFolder) {
		this.targetFolder = targetFolder;
	}

	public String getTemplateFolder() {
		return this.templateFolder;
	}

	public void setTemplateFolder(String templateFolder) {
		this.templateFolder = templateFolder;
	}

	public String getPackageName() {
		return this.packageName;
	}

	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}

	public String getTablePackageName() {
		return fallback(this.tablePackageName, "tables");
	}

	public void setTablePackageName(String tablePackageName) {
		this.tablePackageName = tablePackageName;
	}

	public String getRecordPackageName() {
		return fallback(this.recordPackageName, "records");
	}

	public void setRecordPackageName(String recordPackageName) {
		this.recordPackageName = recordPackageName;
	}

	public String getViewPackageName() {
		return fallback(this.viewPackageName, "views");
	}

	public void setViewPackageName(String viewPackageName) {
		this.viewPackageName = viewPackageName;
	}

	public String getDbClassName() {
		return this.dbClassName;
	}

	public void setDbClassName(String dbClassName) {
		this.dbClassName = dbClassName;
	}

	public String getTableBaseName() {
		return this.tableBaseName;
	}

	public void setTableBaseName(String tableBaseName) {
		this.tableBaseName = tableBaseName;
	}

	public String getViewBaseName() {
		return this.viewBaseName;
	}

	public void setViewBaseName(String viewBaseName) {
		this.viewBaseName = viewBaseName;
	}

	public String getRecordBaseName() {
		return this.recordBaseName;
	}

	public void setRecordBaseName(String recordBaseName) {
		this.recordBaseName = recordBaseName;
	}

	public String getTableNamePrefix() {
		return this.tableNamePrefix;
	}

	public void setTableNamePrefix(String tableNamePrefix) {
		this.tableNamePrefix = tableNamePrefix;
	}

	public String getTableClassPrefix() {
		return this.tableClassPrefix;
	}

	public void setTableClassPrefix(String tableClassPrefix) {
		this.tableClassPrefix = tableClassPrefix;
	}

	public String getTableClassSuffix() {
		return this.tableClassSuffix;
	}

	public void setTableClassSuffix(String tableClassSuffix) {
		this.tableClassSuffix = tableClassSuffix;
	}

	public String getViewNamePrefix() {
		return this.viewNamePrefix;
	}

	public void setViewNamePrefix(String viewNamePrefix) {
		this.viewNamePrefix = viewNamePrefix;
	}

	public String getViewClassPrefix() {
		return this.viewClassPrefix;
	}

	public void setViewClassPrefix(String viewClassPrefix) {
		this.viewClassPrefix = viewClassPrefix;
	}

	public String getViewClassSuffix() {
		return this.viewClassSuffix;
	}

	public void setViewClassSuffix(String viewClassSuffix) {
		this.viewClassSuffix = viewClassSuffix;
	}

	public String getColumnNamePrefix() {
		return this.columnNamePrefix;
	}

	public void setColumnNamePrefix(String columnNamePrefix) {
		this.columnNamePrefix = columnNamePrefix;
	}

	public boolean isNestTables() {
		return this.nestTables;
	}

	public void setNestTables(boolean nestTables) {
		this.nestTables = nestTables;
	}

	public boolean isNestViews() {
		return this.nestViews;
	}

	public void setNestViews(boolean nestViews) {
		this.nestViews = nestViews;
	}

	public boolean isCreateRecordProperties() {
		return this.createRecordProperties;
	}

	public void setCreateRecordProperties(boolean createRecordProperties) {
		this.createRecordProperties = createRecordProperties;
	}

	public boolean isPreserverCharacterCase() {
		return this.preserverCharacterCase;
	}

	public void setPreserverCharacterCase(boolean preserverCharacterCase) {
		this.preserverCharacterCase = preserverCharacterCase;
	}

	public boolean isPreserveRelationNames() {
		return this.preserveRelationNames;
	}

	public void setPreserveRelationNames(boolean preserveRelationNames) {
		this.preserveRelationNames = preserveRelationNames;
	}

	private String fallback(String packageName, String defaultSubpackage) {
		String pkg = packageName;
		if (pkg == null && this.packageName != null) {
			pkg = this.packageName + "." + defaultSubpackage;
		}
		return pkg;
	}

	public String getConfigTitle() {
		return this.configTitle;
	}

	public void setConfigTitle(String configTitle) {
		this.configTitle = configTitle;
	}

	public String getJdbcType() {
		return this.jdbcType;
	}

	public void setJdbcType(String jdbcType) {
		this.jdbcType = jdbcType;
	}

	public String getJdbcServer() {
		return this.jdbcServer;
	}

	public void setJdbcServer(String jdbcServer) {
		this.jdbcServer = jdbcServer;
	}

	public String getJdbcPort() {
		return this.jdbcPort;
	}

	public void setJdbcPort(String jdbcPort) {
		this.jdbcPort = jdbcPort;
	}

	public String getJdbcSID() {
		return this.jdbcSID;
	}

	public void setJdbcSID(String jdbcSID) {
		this.jdbcSID = jdbcSID;
	}
}
