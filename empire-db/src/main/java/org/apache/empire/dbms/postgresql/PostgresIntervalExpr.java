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
package org.apache.empire.dbms.postgresql;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.empire.data.DataType;
import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBColumnExpr;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.DBRowSet;
import org.apache.empire.db.DBSQLBuilder;
import org.w3c.dom.Element;

public class PostgresIntervalExpr extends DBColumnExpr {

	public enum PostgresIntervalUnitField {

		YEARS,

		MONTHS,

		WEEKS,

		DAYS,

		HOURS,

		MINUTES,

		SECONDS;

	}
	
	private final DBDatabase								db;
	private final Map<PostgresIntervalUnitField, Integer>	fields	= new HashMap<>();

	public PostgresIntervalExpr(DBDatabase db, int quantity, PostgresIntervalUnitField unit)
	{
		this.db = db;
		this.fields.put(unit, quantity);
	}

    @SuppressWarnings("unchecked")
    @Override
    public final DBDatabase getDatabase()
    {
        return this.db;
    }
	
	@Override
	public DataType getDataType()
	{
		return DataType.UNKNOWN;
	}

    @Override
    public Class<Enum<?>> getEnumType()
    {
        return null;
    }

	@Override
	public String getName()
	{
		return "interval";
	}

	@Override
    public DBRowSet getRowSet()
    {
        return null;
    }

	@Override
	public DBColumn getUpdateColumn()
	{
		return null;
	}

    @Override
    public boolean isAggregate()
    {
        return false;
    }

	@Override
	public Element addXml(Element parent, long flags)
	{
		return null;
	}

	@Override
	public void addSQL(DBSQLBuilder sql, long context)
	{
		sql.append("interval");
		sql.append(" '");
		appendIfAdded(PostgresIntervalUnitField.YEARS, sql, context);
		appendIfAdded(PostgresIntervalUnitField.MONTHS, sql, context);
		appendIfAdded(PostgresIntervalUnitField.WEEKS, sql, context);
		appendIfAdded(PostgresIntervalUnitField.DAYS, sql, context);
		appendIfAdded(PostgresIntervalUnitField.HOURS, sql, context);
		appendIfAdded(PostgresIntervalUnitField.MINUTES, sql, context);
		appendIfAdded(PostgresIntervalUnitField.SECONDS, sql, context);
		sql.reset(sql.length() - 1); // remove last blank
		sql.append("' ");
	}

	private void appendIfAdded(PostgresIntervalUnitField unit, DBSQLBuilder sql, long context)
	{
		if (this.fields.containsKey(unit) && this.fields.get(unit) != null) {
			sql.append(this.fields.get(unit));
			sql.append(" ");
			sql.append(unit.name());
			sql.append(" ");
		}
	}

	public PostgresIntervalExpr add(int quantity, PostgresIntervalUnitField unit)
	{
		this.fields.put(unit, quantity);
		return this;
	}

	@Override
	public void addReferencedColumns(Set<DBColumn> list)
	{
	}
	
}
