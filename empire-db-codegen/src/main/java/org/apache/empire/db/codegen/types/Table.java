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

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.empire.data.DataType;
import org.apache.empire.db.codegen.util.DBUtil;
import org.apache.empire.db.codegen.util.StringUtils;

public class Table {
    private static final Log log = LogFactory.getLog(Database.class);

    private String tableName;
	private List<String> pkCols;
	private List<Column> columns;
	private Column lockCol;
	private String lockColName;

	private Map<String, Column> columnMap = new HashMap<String, Column>();

	public Table(String tableName,String lockColName, String schemaPattern, String catalogName,
			DatabaseMetaData dbMeta) {
		this.tableName = tableName.toUpperCase();
		this.lockColName=lockColName;
		this.createColumns(dbMeta, schemaPattern, catalogName);
	}

	public String getTableName() {
		return tableName.toUpperCase();
	}

	public List<Column> getColumns() {
		return columns;
	}
	
	public boolean getHasLockColumn() {
		return lockCol!=null;
	}
	
	public Column getLockColumn() {
		return lockCol;
	}

	public String getClassName() {
		return StringUtils.javaClassName(this.tableName) + "Table";
	}

	public String getRecordClassName() {
		return StringUtils.javaClassName(this.tableName) + "Record";
	}

	public boolean hasBigDecimalField() {
		boolean bdField = false;
		for (Column col : this.columns) {
			if (col.getEmpireType() == DataType.DECIMAL) {
				bdField = true;
				break;
			}
		}
		return bdField;
	}

	public Column getColumn(String name) {
		return this.columnMap.get(name.toUpperCase());
	}

	// ------------------------------------------------------------------------
	// Private members
	// ------------------------------------------------------------------------
	private void createColumns(DatabaseMetaData dbMeta, String schemaPattern,
			String catalogName) {
		this.pkCols = this.findPkColumns(dbMeta, schemaPattern, catalogName);
		this.columns = new ArrayList<Column>();
		ResultSet rs = null;
		try {
			rs = dbMeta.getColumns(catalogName, schemaPattern, tableName, null);
			boolean isKeyCol;
			String colName;
			while (rs.next()) {
				colName = rs.getString("COLUMN_NAME").toUpperCase();
				log.info("\tCOLUMN:\t" + colName);
				isKeyCol = pkCols.contains(colName);

				Column col = new Column(rs, isKeyCol);
				this.columns.add(col);
				this.columnMap.put(col.getName().toUpperCase(), col);
				if(lockColName!=null && col.getName().toUpperCase().equals(lockColName.toUpperCase()))
					lockCol=col;
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		} finally {
			DBUtil.closeResultSet(rs, log);
		}
	}

	private List<String> findPkColumns(DatabaseMetaData dbMeta,
			String schemaPattern, String catalogName) {
		List<String> cols = new ArrayList<String>();
		ResultSet rs = null;
		try {
			rs = dbMeta.getPrimaryKeys(catalogName, schemaPattern, tableName);
			while (rs.next()) {
				cols.add(rs.getString("COLUMN_NAME").toUpperCase());
			}

		} catch (SQLException e) {
			throw new RuntimeException(e);
		} finally {
			DBUtil.closeResultSet(rs, log);
		}
		return cols;
	}
}
