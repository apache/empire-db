package org.apache.empire.dbms.postgresql;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.empire.data.DataType;
import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBColumnExpr;
import org.apache.empire.db.DBDatabase;
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
	
	@Override
	public DataType getDataType()
	{
		return DataType.UNKNOWN;
	}

	@Override
	public String getName()
	{
		return "interval";
	}

	@Override
	public boolean isAggregate()
	{
		return false;
	}

	@Override
	public DBColumn getUpdateColumn()
	{
		return null;
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

	@SuppressWarnings("unchecked")
	@Override
	public final DBDatabase getDatabase()
	{
		return this.db;
	}

	/**
	 * Not an Enum. Returns null
	 */
	@Override
	public Class<Enum<?>> getEnumType()
	{
		return null;
	}

	@Override
	public DBColumn getSourceColumn()
	{
		return null;
	}
	
}
