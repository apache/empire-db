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
package org.apache.empire.db.driver.sqlite;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.empire.data.DataType;
import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBDDLGenerator;
import org.apache.empire.db.DBExpr;
import org.apache.empire.db.DBIndex;
import org.apache.empire.db.DBRelation;
import org.apache.empire.db.DBSQLScript;
import org.apache.empire.db.DBTable;
import org.apache.empire.db.DBTableColumn;

public class SQLiteDDLGenerator extends DBDDLGenerator<DBDatabaseDriverSQLite>
{
	public SQLiteDDLGenerator(DBDatabaseDriverSQLite driver)
	{
		super(driver);
		// set SQLite specific data types
		initDataTypes();
	}

	/**
	 * sets SQLite specific data types
	 * 
	 * @param driver
	 */
	private void initDataTypes()
	{ // Override data types
		DATATYPE_INTEGER = "INTEGER";
		DATATYPE_BOOLEAN = "BOOLEAN";
		DATATYPE_TIMESTAMP = "DATETIME DEFAULT CURRENT_TIMESTAMP";
	}


	@Override
	protected void createTable(DBTable t, DBSQLScript script)
	{
		StringBuilder sql = new StringBuilder();
		sql.append("-- creating table ");
		sql.append(t.getName());
		sql.append(" --\r\n");
		sql.append("CREATE TABLE ");
		t.addSQL(sql, DBExpr.CTX_FULLNAME);
		sql.append(" (");
		boolean addSeparator = false;
		Iterator<DBColumn> columns = t.getColumns().iterator();
		while (columns.hasNext())
		{
			DBTableColumn c = (DBTableColumn) columns.next();
			if (c.getDataType() == DataType.UNKNOWN)
				continue; // Ignore and continue;
			// Append column
			sql.append((addSeparator) ? ",\r\n   " : "\r\n   ");
			appendColumnDesc(c, false, sql);
			addSeparator = true;

		}
		// Foreign Key
		Map<DBColumn, DBColumn> referencesMap = t.getColumnReferences();
		if (referencesMap != null)
		{
			DBColumn source = null,target = null;
			for (Entry<DBColumn, DBColumn> entry : referencesMap.entrySet())
			{
				source = entry.getKey();
				target = entry.getValue();
				sql.append((addSeparator) ? ",\r\n   " : "\r\n   ");
				sql.append("FOREIGN KEY (");
				source.addSQL(sql, DBExpr.CTX_NAME);
				sql.append(") REFERENCES ");
				sql.append(target.getRowSet().getName());
				sql.append(" (");
				target.addSQL(sql, DBExpr.CTX_NAME);
				sql.append(")");
			}
		}

		// Primary Key
		DBIndex pk = t.getPrimaryKey();
		if (pk != null)
		{ // add the primary key
			sql.append(",\r\n");
			if (namePrimaryKeyConstraint)
			{
				sql.append(" CONSTRAINT ");
				appendElementName(sql, pk.getName());
			}
			sql.append(" PRIMARY KEY (");
			addSeparator = false;
			// columns
			DBColumn[] keyColumns = pk.getColumns();
			for (int i = 0; i < keyColumns.length; i++)
			{
				sql.append((addSeparator) ? ", " : "");
				keyColumns[i].addSQL(sql, DBExpr.CTX_NAME);
				addSeparator = true;
			}
			sql.append(")");
		}
		sql.append(")");
		// Create the table
		addCreateTableStmt(t, sql, script);
		// Create all Indexes
		createTableIndexes(t, pk, script);
	}

	
	
	@Override
	protected void createRelation(DBRelation r, DBSQLScript script)
	{
		// http://www.sqlite.org/foreignkeys.html
	}
}