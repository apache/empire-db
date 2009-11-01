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
import org.apache.empire.db.codegen.util.DBUtil;

public class Database {
    private static final Log log = LogFactory.getLog(Database.class);

    private DatabaseMetaData metaData;
	private String catalogName;
	private String schemaPattern;
	private String tablePattern;
	private Connection con;

	private Map<String, Table> tableMap = new HashMap<String, Table>();

	public Database(Connection con, String schemaPattern,
			String catalogName, String tablePattern) {

		this.con = con;
		this.schemaPattern = schemaPattern;
		this.catalogName = catalogName;
		this.tablePattern = tablePattern;
		
		try {
			this.metaData = this.con.getMetaData();
		} catch (SQLException e) {
			throw new RuntimeException("Unable to retrieve database metadata!",
					e);
		}

	}

	/**
	 * Populates meta data for tables in the database.
	 * 
	 */
	public void populateTableMetaData(String lockColName) {
		DatabaseMetaData dbMeta = this.metaData;
		ResultSet tables = null;
		try {
			tables = dbMeta.getTables(catalogName, schemaPattern, tablePattern,
					new String[] { "TABLE" });
			while (tables.next()) {
				String tableName = tables.getString("TABLE_NAME");
				// Ignore system tables containing a '$' symbol (required for Oracle!)
				if (tableName.indexOf('$')>=0) {
	                log.info("Ignoring system table " + tableName);
	                continue;
				}
				// end system table exclusion
				log.info("Adding table " + tableName);
				Table table = new Table(tableName, lockColName, schemaPattern, catalogName, dbMeta);
				this.tableMap.put(tableName.toUpperCase(), table);
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		} finally {
			DBUtil.closeResultSet(tables, log);
		}
	}
	
	public Collection<Table> getTables() {
		return tableMap.values();
	}

	public Table getTable(String name) {
		return this.tableMap.get(name.toUpperCase());
	}
}
