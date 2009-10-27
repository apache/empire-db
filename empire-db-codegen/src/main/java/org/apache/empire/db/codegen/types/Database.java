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
package org.apache.empire.db.codegen.types;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.empire.db.codegen.util.StringUtils;

public class Database {
    private static final Log log = LogFactory.getLog(Database.class);
    
    private Connection connection;
	private String schemaName;
	
	private Map<String, Table> tableMap = new HashMap<String, Table>();

	public Database(Connection connection, String schemaName) {
		// Set properties
		this.connection = connection;
        this.schemaName = schemaName;
	}
	
	public Connection getConnection() {
	    return connection;
	}
	
	private void close(ResultSet rs) {
	    try {
	        rs.close();
	    } catch(SQLException e) {
            throw new RuntimeException(e);
	    }
	}

	/**
	 * Populates meta data for tables in the database.
	 * @param pkColName Primary key column name.  Note: We assume a single
	 * 		auto-generated PK column with the same name is used for every
	 * 		table in the DB.
	 * @param lockColName Lock column name used for optimistic locking.
	 * 		Note: We assume a single timestamp column with the same name 
	 * 		is used for every table in the DB.
	 */
	public void populateTableMetaData(String pkColName, String lockColName) {
		Table.setPkColName(pkColName);
		Table.setLockColName(lockColName);
		DatabaseMetaData dbMeta = this.getDbMetaData();
		ResultSet tables = null;
		try {
            log.info("Reading Tables for schema " + schemaName);
			tables = dbMeta.getTables(null, this.schemaName, null, new String[] {"TABLE"});
			while (tables.next()) {
				String tableName = tables.getString("TABLE_NAME");
				// rd: required for oracle
				if (tableName.indexOf('$')>=0)
				    continue; // ignore table names with a "$"
				// end oracle
				log.info("Reading Table metadata for " + tableName);
				Table table = new Table(tableName, this.schemaName, dbMeta);
				this.tableMap.put(tableName.toUpperCase(), table);				
			}
			
			for (String tableName: this.tableMap.keySet()) {
				Table table = this.tableMap.get(tableName);
				for (Column fkCol: table.getFkCols()) {
					Table parentTable = this.tableMap.get(
							fkCol.getFkTableName());
					parentTable.addChildTable(fkCol.getName(), table);
				}
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		} finally {
		    close(tables);
		}
	}
	/**
	 * Gets the database meta data.
	 * @return The database meta data.
	 */
	public DatabaseMetaData getDbMetaData() {
		try {
			return this.getConnection().getMetaData();
		} catch (SQLException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
	
	public String getClassName() {
		return StringUtils.javaClassName(this.schemaName) + "Database";
	}

	public String getBaseTableClassName() {
		return StringUtils.javaClassName(this.schemaName) + "Table";
	}

	public String getInstanceName() {
		return StringUtils.deriveAttributeNameFromClass(this.getClassName());
	}

	public Collection<Table> getTables() {
		return tableMap.values();
	}
	public Table getTable(String name) {
		return this.tableMap.get(name.toUpperCase());
	}
	
	public String getSchemaName() {
		return schemaName;
	}
}
