package org.apache.empire.struts2.websample.web;

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
	 */
	public boolean init(String filename) {
		// Read the properties file
		if (super.init(filename, false, true) == false)
			return false;
		// Done
		if (readProperties(this, "properties")==false)
			return false;
		// Reader Provider Properties
		return readProperties(this, "properties-" + databaseProvider);
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
