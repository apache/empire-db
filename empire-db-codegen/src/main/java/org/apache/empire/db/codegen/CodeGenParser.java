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

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.empire.commons.ErrorObject;
import org.apache.empire.commons.Errors;
import org.apache.empire.data.DataType;
import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBCommandExpr;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.DBTable;
import org.apache.empire.db.DBTableColumn;
import org.apache.empire.db.DBView;
import org.apache.empire.db.DBView.DBViewColumn;
import org.apache.empire.db.codegen.util.DBUtil;

/**
 * This class is used to create a in memory DBDatabase of a given SQLConnection
 * and Configuration
 * 
 * @author Benjamin Venditti
 */
public class CodeGenParser extends ErrorObject {

	public static class InMemoryDatabase extends DBDatabase {
	}
	
	public static class InMemoryView extends DBView {
		public InMemoryView(String name, DBDatabase db) {
			super(name, db);
		}
		
		public DBViewColumn addCol(String columnName,DataType dataType)
		{
			return addColumn(columnName, dataType);
		}
		
		@Override
		public DBCommandExpr createCommand() {
			return null;
		}
	}

	private static final Log log = LogFactory.getLog(CodeGenParser.class);
	
	private DatabaseMetaData dbMeta;
	private Connection con;
	private CodeGenConfig config;

	/**
	 * create a empty in memory Database and populates it
	 */
	public CodeGenParser(CodeGenConfig config) {
	    this.config = config;
	}

	/**
	 * returns the populated DBDatabase
	 */
	public DBDatabase loadDbModel() {
		DBDatabase db = new InMemoryDatabase();
	    try {           
            con = openJDBCConnection(config);
            populateDatabase(db);
        } 
        catch (SQLException e) 
        {
            throw new RuntimeException("Unable to read database metadata: " + e.getMessage(), e);
        }
        finally 
        {
            DBUtil.close(con, log);
        }
        return db;
	}

	/**
     * Opens and returns a JDBC-Connection.
     * JDBC url, user and password for the connection are obained from the SampleConfig bean
     * Please use the config.xml file to change connection params.
     */
    private Connection openJDBCConnection(CodeGenConfig config) throws SQLException{
        log.info("Connecting to Database'" + config.getJdbcURL() + "' / User=" + config.getJdbcUser());
        Connection conn = null;
        try {
            Class.forName(config.getJdbcClass()).newInstance();
        }catch(Exception ex){
        	throw new SQLException("Could not load database driver: " + config.getJdbcClass());
        }
        conn = DriverManager.getConnection(config.getJdbcURL(), config.getJdbcUser(), config.getJdbcPwd());
        log.info("Connected successfully");
        return conn;
    }
	
	/**
	 * Queries the metadata of the database for tables and vies and populates the
	 * database with those
	 * @throws SQLException 
	 */
	private void populateDatabase(DBDatabase db) throws SQLException {
		ResultSet tables = null;
		ResultSet views = null;
		try{
            this.dbMeta = con.getMetaData();
		    // Get table metadata
            tables = dbMeta.getTables(
		            config.getDbCatalog(), 
		            config.getDbSchema(), 
		            config.getDbTablePattern(),
					new String[] { "TABLE" });
            // Get view metadata
            views = dbMeta.getTables(
		            config.getDbCatalog(), 
		            config.getDbSchema(), 
		            config.getDbTablePattern(),
					new String[] { "VIEW" });
            
         // Add all tables
            int tableCount = 0;
			while (tables.next()) {
				String tableName = tables.getString("TABLE_NAME");
				// Ignore system tables containing a '$' symbol (required for Oracle!)
				if (tableName.indexOf('$') >= 0) {
					log.info("Ignoring system table " + tableName);
					continue;
				}
				log.info("TABLE: " + tableName);
				DBTable table = new DBTable(tableName, db);
				populateTable(table);
				tableCount++;
			}
			
			// Add all views
            int viewCount = 0;
			while (views.next()) {
				String viewName = views.getString("TABLE_NAME");
				// Ignore system tables containing a '$' symbol (required for Oracle!)
				if (viewName.indexOf('$') >= 0) {
					log.info("Ignoring system table " + viewName);
					continue;
				}
				log.info("VIEW: " + viewName);
				InMemoryView view = new InMemoryView(viewName, db);
				populateView(view);
				viewCount++;
			}

			if (tableCount==0) {
			    // getTables returned no result
			    String info = "catalog="+config.getDbCatalog(); 
                info += "/ schema="+config.getDbSchema(); 
                info += "/ pattern="+config.getDbTablePattern(); 
			    log.warn("DatabaseMetaData.getTables() returned no tables! Please check parameters: "+info);
			}
		} finally {
			DBUtil.close(tables, log);
		}
	}

	/**
	 * queries the metadata for columns of a specific table and populates the
	 * table with that information
	 * @throws SQLException 
	 */
	private void populateTable(DBTable t) throws SQLException {
		List<String> pkCols = this.findPkColumns(t.getName());
		String lockColName = config.getTimestampColumn();
		DBColumn[] keys = new DBColumn[pkCols.size()];
		ResultSet rs = null;
		try {
			rs = dbMeta.getColumns(config.getDbCatalog(), config.getDbSchema(),
					t.getName(), null);
	        int i=0;
			while (rs.next()) {
				DBTableColumn c = addColumn(t, rs);
				// check if it is a KeyColumn
				if (pkCols.contains(c.getName()))
					keys[i++] = c;
				
				// check if it is the Timestamp/Locking Column
				if (lockColName!=null && c.getName().equalsIgnoreCase(lockColName))
					t.setTimestampColumn(c);
			}
	        // Check whether all key columns have been set
	        for (i=0; i<keys.length; i++)
	            if (keys[i]==null)
	                error(Errors.ItemNotFound, pkCols.get(i));
	        if(keys.length > 0){
	        	t.setPrimaryKey(keys);
	        }
		} finally {
			DBUtil.close(rs, log);
		}
	}
	
	/**
	 * queries the metadata for columns of a specific table and populates the
	 * table with that information
	 * @throws SQLException 
	 */
	private void populateView(InMemoryView v) throws SQLException {
		ResultSet rs = null;
		try {
			rs = dbMeta.getColumns(config.getDbCatalog(), config.getDbSchema(),
					v.getName(), null);
			while (rs.next()) {
				addColumn(v, rs);
			}
		} finally {
			DBUtil.close(rs, log);
		}
	}

	/**
	 * Returns a list of column names that define the primarykey of the given
	 * table.
	 * @throws SQLException 
	 */
	private List<String> findPkColumns(String tableName) throws SQLException {
		List<String> cols = new ArrayList<String>();
		ResultSet rs = null;
		try {
			rs = dbMeta.getPrimaryKeys(config.getDbCatalog(), config
					.getDbSchema(), tableName);
			while (rs.next()) {
				cols.add(rs.getString("COLUMN_NAME"));
			}
		} finally {
			DBUtil.close(rs, log);
		}
		return cols;
	}

	/**
	 * Adds DBColumn object to the given DBTable. The DBColumn is created from
	 * the given ResultSet
	 */
	private DBTableColumn addColumn(DBTable t, ResultSet rs)
			throws SQLException {
		String name = rs.getString("COLUMN_NAME");
		DataType empireType = getEmpireDataType(rs.getInt("DATA_TYPE"));
		int colSize = rs.getInt("COLUMN_SIZE");
		boolean required = false;
		String defaultValue = rs.getString("COLUMN_DEF");
		if (rs.getString("IS_NULLABLE").equalsIgnoreCase("NO"))
			required = true;
		
		log.info("\tCOLUMN:\t" + name + " ("+empireType+")");
		return t.addColumn(name, empireType, colSize, required, defaultValue);
	}
	
	/**
	 * Adds DBColumn object to the given DBTable. The DBColumn is created from
	 * the given ResultSet
	 */
	private DBViewColumn addColumn(InMemoryView v, ResultSet rs)
			throws SQLException {
		String name = rs.getString("COLUMN_NAME");
		DataType empireType = getEmpireDataType(rs.getInt("DATA_TYPE"));
		
		log.info("\tCOLUMN:\t" + name + " ("+empireType+")");
		return v.addCol(name, empireType);
	}

	/**
	 * converts a SQL DataType to a EmpireDataType
	 */
	private DataType getEmpireDataType(int sqlType) {
		DataType empireType = DataType.UNKNOWN;
		switch (sqlType) {
		case Types.INTEGER:
		case Types.SMALLINT:
		case Types.TINYINT:
		case Types.BIGINT:
			empireType = DataType.INTEGER;
			break;
		case Types.VARCHAR:
			empireType = DataType.TEXT;
			break;
		case Types.DATE:
			empireType = DataType.DATE;
			break;
		case Types.TIMESTAMP:
		case Types.TIME:
			empireType = DataType.DATETIME;
			break;
		case Types.CHAR:
			empireType = DataType.CHAR;
			break;
		case Types.DOUBLE:
		case Types.FLOAT:
		case Types.REAL:
			empireType = DataType.DOUBLE;
			break;
		case Types.DECIMAL:
		case Types.NUMERIC:
			empireType = DataType.DECIMAL;
			break;
		case Types.BIT:
		case Types.BOOLEAN:
			empireType = DataType.BOOL;
			break;
		case Types.CLOB:
		case Types.LONGVARCHAR:
			empireType = DataType.CLOB;
			break;
		case Types.BINARY:
		case Types.VARBINARY:
		case Types.LONGVARBINARY:
		case Types.BLOB:
			empireType = DataType.BLOB;
			break;
		default:
			empireType = DataType.UNKNOWN;
			log.warn("SQL column type " + sqlType + " not supported.");
		}
		log.debug("Mapping date type " + String.valueOf(sqlType) + " to "
				+ empireType);
		return empireType;
	}

}
