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
import org.apache.empire.data.DataType;
import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.DBTable;
import org.apache.empire.db.DBTableColumn;
import org.apache.empire.db.codegen.util.DBUtil;

/**
 * This class is used to create a in memory DBDatabase of a given SQLConnection
 * and Configuration
 * 
 * @author Benjamin Venditti
 */
public class CodeGenParser {

	public static class InMemoryDatabase extends DBDatabase {
	}

	private static final Log log = LogFactory.getLog(CodeGenParser.class);
	
	private DatabaseMetaData dbMeta;
	private Connection con;
	private CodeGenConfig config;
	private DBDatabase db;

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
	    this.db = new InMemoryDatabase();
	    try {           
            // Get a JDBC Connection
            con = getJDBCConnection(config);
            
            // create the database in memory

            this.dbMeta = con.getMetaData();
            populateDatabase();
                        
        } 
        catch (SQLException e) 
        {
            throw new RuntimeException("Unable to read database metadata!", e);
        }
        catch (Exception e) 
        {
            log.error(e.getMessage(), e);
        } 
        finally 
        {
            DBUtil.close(con, log);
        }
		return db;
	}

	// ----------- private members

	   /**
     * <PRE>
     * Opens and returns a JDBC-Connection.
     * JDBC url, user and password for the connection are obained from the SampleConfig bean
     * Please use the config.xml file to change connection params.
     * </PRE>
     */
    private Connection getJDBCConnection(CodeGenConfig config) {
        // Establish a new database connection
        Connection conn = null;
        log.info("Connecting to Database'" + config.getJdbcURL() + "' / User="
                + config.getJdbcUser());
        try {
            // Connect to the databse
            Class.forName(config.getJdbcClass()).newInstance();
            conn = DriverManager.getConnection(config.getJdbcURL(), config
                    .getJdbcUser(), config.getJdbcPwd());
            log.info("Connected successfully");
            // set the AutoCommit to false this session. You must commit
            // explicitly now
            conn.setAutoCommit(true);
            log.info("AutoCommit is " + conn.getAutoCommit());

        } catch (Exception e) {
            log.fatal("Failed to connect directly to '" + config.getJdbcURL()
                    + "' / User=" + config.getJdbcUser(), e);
            throw new RuntimeException(e);
        }
        return conn;
    }
	
	/**
	 * queries the metadata of the database for tables and populates the
	 * database with those
	 */
	private void populateDatabase() {
		ResultSet tables = null;
		try {
			tables = dbMeta.getTables(config.getDbCatalog(), config
					.getDbSchema(), config.getDbTablePattern(),
					new String[] { "TABLE" });
			while (tables.next()) {
				String tableName = tables.getString("TABLE_NAME");
				// Ignore system tables containing a '$' symbol (required for
				// Oracle!)
				if (tableName.indexOf('$') >= 0) {
					log.info("Ignoring system table " + tableName);
					continue;
				}
				// end system table exclusion
				log.info("Adding table " + tableName);
				addTable(tableName);
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		} finally {
			DBUtil.close(tables, log);
		}
	}

	/**
	 * queries the metadata for columns of a specific table and populates the
	 * table with that information
	 */
	private void addTable(String name) {
		DBTable t = new DBTable(name.toUpperCase(), db);
		List<String> pkCols = this.findPkColumns(name);
		String lockColName = config.getTimestampColumn();
		DBColumn[] keys = new DBColumn[pkCols.size()];
		int i = 0;
		ResultSet rs = null;
		try {
			rs = dbMeta.getColumns(config.getDbCatalog(), config.getDbSchema(),
					name, null);
			while (rs.next()) {
				DBTableColumn c = addColumn(t, rs);
				// check if it is a KeyColumn
				if (pkCols.contains(c.getName()))
					keys[i++] = c;

				// check if it is the Timestamp/Locking Column
				if (lockColName != null
						&& c.getName().toUpperCase().equals(
								lockColName.toUpperCase()))
					t.setTimestampColumn(c);
			}
			t.setPrimaryKey(keys);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		} finally {
			DBUtil.close(rs, log);
		}
	}

	/**
	 * Returns a list of column names that define the primarykey of the given
	 * table.
	 */
	private List<String> findPkColumns(String tableName) {
		List<String> cols = new ArrayList<String>();
		ResultSet rs = null;
		try {
			rs = dbMeta.getPrimaryKeys(config.getDbCatalog(), config
					.getDbSchema(), tableName);
			while (rs.next()) {
				cols.add(rs.getString("COLUMN_NAME").toUpperCase());
			}

		} catch (SQLException e) {
			throw new RuntimeException(e);
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

		log.info("\tCOLUMN:\t" + name);
		return t.addColumn(name, empireType, colSize, required, defaultValue);
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
		log.info("Mapping date type " + String.valueOf(sqlType) + " to "
				+ empireType);
		return empireType;
	}

}
