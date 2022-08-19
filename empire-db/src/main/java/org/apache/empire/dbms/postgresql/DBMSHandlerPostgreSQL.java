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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import org.apache.empire.commons.StringUtils;
import org.apache.empire.data.DataType;
import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBColumnExpr;
import org.apache.empire.db.DBDDLGenerator;
import org.apache.empire.db.DBDDLGenerator.DDLActionType;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.DBObject;
import org.apache.empire.db.DBSQLBuilder;
import org.apache.empire.db.DBSQLScript;
import org.apache.empire.db.DBTable;
import org.apache.empire.db.DBTableColumn;
import org.apache.empire.db.expr.column.DBValueExpr;
import org.apache.empire.dbms.DBMSFeature;
import org.apache.empire.dbms.DBMSHandler;
import org.apache.empire.dbms.DBMSHandlerBase;
import org.apache.empire.dbms.DBSqlPhrase;
import org.apache.empire.exceptions.InvalidArgumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class provides support for the PostgreSQL database system.
 * 
 *
 */
public class DBMSHandlerPostgreSQL extends DBMSHandlerBase
{
    // *Deprecated* private static final long serialVersionUID = 1L;
  
    private static final Logger log = LoggerFactory.getLogger(DBMSHandlerPostgreSQL.class);
    
    
    private String databaseName;
    
    private boolean usePostgresSerialType = true;

    private DBDDLGenerator<?> ddlGenerator = null; // lazy creation
    
    /**
     * Constructor for the PostgreSQL database dbms.<br>
     */
    public DBMSHandlerPostgreSQL()
    {
        setReservedKeywords();
    }

    /**
     * returns the name for the database / schema
     * @return the database / schema name
     */
    public String getDatabaseName()
    {
        return databaseName;
    }

    /**
     * Returns whether or not the Postgres Serial Type is used for Identity columns
     * @return true if Postgres Serial Type is used or false if INTEGER is used
     */
    public boolean isUsePostgresSerialType()
    {
        return usePostgresSerialType;
    }

    /**
     * Sets whether or not the Postgres Serial Type is used for Identity columns
     * @param usePostgresSerialType true if Postgres Serial Type should be used or false if INTEGER should be used
     */
    public void setUsePostgresSerialType(boolean usePostgresSerialType)
    {
        this.usePostgresSerialType = usePostgresSerialType;
    }

    /**
     * Sets the name for the database / schema<br>
     * This names is required for creating a database.<br>
     * When a name is set, the dbms will automatically execute 'USE dbname' when the database is opened.
     * @param databaseName the name of the database
     */
    public void setDatabaseName(String databaseName)
    {
        this.databaseName = databaseName;
    }
    
    /**
     * Initialize Database on open
     */
    @Override
    public void attachDatabase(DBDatabase db, Connection conn)
    {
        super.attachDatabase(db, conn);
        // set Sequence names
        if (isUsePostgresSerialType())
            initSerialSequenceNames(db, conn);
    }
    
    /**
     * Creates a new PostgreSQL command object.
     * 
     * @return the new DBCommandPostgreSQL object
     */
    @Override
    public DBCommandPostgres createCommand(boolean autoPrepareStmt)
    {   // create command object
        return new DBCommandPostgres(autoPrepareStmt);
    }

    /**
     * Returns whether or not a particular feature is supported by this dbms
     * @param type type of requested feature. @see DBMSFeature
     * @return true if the features is supported or false otherwise
     */
    @Override
    public boolean isSupported(DBMSFeature type)
    {
        switch (type)
        {   // return support info 
            case CREATE_SCHEMA:     return true;
            case SEQUENCES:         return true;    
            case SEQUENCE_NEXTVAL:  return true;
            case QUERY_LIMIT_ROWS:  return true;
            case QUERY_SKIP_ROWS:   return true;
            default:
                // All other features are not supported by default
                return false;
        }
    }
    
    /**
     * Gets an sql phrase template for this database system.<br>
     * @see DBMSHandler#getSQLPhrase(int)
     * @return the phrase template
     */
    @Override
    public String getSQLPhrase(DBSqlPhrase phrase)
    {
        switch (phrase)
        {
            // sql-phrases
            case SQL_NULL:              return "null";
            case SQL_PARAMETER:               return " ? ";
            case SQL_RENAME_TABLE:            return " ";
            case SQL_RENAME_COLUMN:           return " AS ";
            case SQL_DATABASE_LINK:           return "@";
            case SQL_QUOTES_OPEN:             return "\"";
            case SQL_QUOTES_CLOSE:            return "\"";
            case SQL_CONCAT_EXPR:             return "? || {0}";
            // data types
            case SQL_BOOLEAN_TRUE:            return "TRUE";
            case SQL_BOOLEAN_FALSE:           return "FALSE";
            case SQL_CURRENT_DATE:            return "CURRENT_DATE";
            case SQL_DATE_PATTERN:            return "yyyy-MM-dd";
            case SQL_DATE_TEMPLATE:           return "'{0}'";
            case SQL_CURRENT_TIME:            return "CURRENT_TIME";
            case SQL_TIME_PATTERN:            return "HH:mm:ss";
            case SQL_TIME_TEMPLATE:           return "'{0}'";
            case SQL_DATETIME_PATTERN:        return "yyyy-MM-dd HH:mm:ss.SSS";
            case SQL_DATETIME_TEMPLATE:       return "'{0}'";
            case SQL_CURRENT_TIMESTAMP:       return "NOW()";
            case SQL_TIMESTAMP_PATTERN:       return "yyyy-MM-dd HH:mm:ss.SSS";
            case SQL_TIMESTAMP_TEMPLATE:      return "'{0}'";
            // functions
            case SQL_FUNC_COALESCE:           return "coalesce(?, {0})";
            case SQL_FUNC_SUBSTRING:          return "substring(?, {0:INTEGER})";
            case SQL_FUNC_SUBSTRINGEX:        return "substring(?, {0:INTEGER}, {1:INTEGER})";
            case SQL_FUNC_REPLACE:            return "replace(?, {0}, {1})";
            case SQL_FUNC_REVERSE:            return "reverse(?)"; // In order to use this function createReverseFunction() must be called first!
            case SQL_FUNC_STRINDEX:           return "strpos(?, {0})"; 
            case SQL_FUNC_STRINDEXFROM:       return null;         // strindexfrom not available in pgsql 
            case SQL_FUNC_LENGTH:             return "length(?)";
            case SQL_FUNC_UPPER:              return "upper(?)";
            case SQL_FUNC_LOWER:              return "lower(?)";
            case SQL_FUNC_TRIM:               return "trim(?)";
            case SQL_FUNC_LTRIM:              return "ltrim(?)";
            case SQL_FUNC_RTRIM:              return "rtrim(?)";
            case SQL_FUNC_ESCAPE:             return "? escape {0:VARCHAR}";
            // Numeric
            case SQL_FUNC_ABS:                return "abs(?)";
            case SQL_FUNC_ROUND:              return "round(?,{0})";
            case SQL_FUNC_TRUNC:              return "truncate(?,{0})";
            case SQL_FUNC_CEILING:            return "ceiling(?)";
            case SQL_FUNC_FLOOR:              return "floor(?)";
            case SQL_FUNC_MOD:                return "mod(?,{0})";
            case SQL_FUNC_FORMAT:             return "format({0:VARCHAR}, ?)";
            // Date
			case SQL_FUNC_DAY:                return "extract(day from ?)";
			case SQL_FUNC_MONTH:              return "extract(month from ?)";
			case SQL_FUNC_YEAR:               return "extract(year from ?)";
            // Aggregation
            case SQL_FUNC_SUM:                return "sum(?)";
            case SQL_FUNC_MAX:                return "max(?)";
            case SQL_FUNC_MIN:                return "min(?)";
            case SQL_FUNC_AVG:                return "avg(?)";
            case SQL_FUNC_STRAGG:             return "STRING_AGG(DISTINCT ? {0} ORDER BY {1})";
            // Others
            case SQL_FUNC_DECODE:             return "case ? {0} end";
            case SQL_FUNC_DECODE_SEP:         return " ";
            case SQL_FUNC_DECODE_PART:        return "when {0} then {1}";
            case SQL_FUNC_DECODE_ELSE:        return "else {0}";
            // Not defined
            default:
                // log.warn("SQL phrase " + phrase.name() + " is not defined!");
                return phrase.getSqlDefault();
        }
    }

    /**
     * @see DBMSHandler#getConvertPhrase(DataType, DataType, Object)
     */
    @Override
    public String getConvertPhrase(DataType destType, DataType srcType, Object format)
    {
        switch (destType)
        {
            case BOOL:     return "CAST(? AS BOOL)";
            case INTEGER:  return "CAST(? AS INTEGER)";
            case DECIMAL:  return "CAST(? AS DECIMAL)";
            case FLOAT:    return "CAST(? AS DOUBLE PRECISION)";
            case DATE:     return "CAST(? AS DATE)";
            case TIME:     return "CAST(? AS TIME)";
            case DATETIME:
            case TIMESTAMP:return "CAST(? AS TIMESTAMP)";
                // Convert to text
            case VARCHAR:  
            case CHAR:     return "CAST(? AS CHAR)";
            case BLOB:     return "CAST(? AS bytea)";
            case CLOB:     return "CAST(? AS TEXT)";
                // Unknown Type
            default:
                log.error("getConvertPhrase: unknown type (" + String.valueOf(destType));
                return "?";
        }
    }

    
    /**
     * @see DBMSHandler#getNextSequenceValue(DBDatabase, String, int, Connection)
     */
    @Override
    public Object getNextSequenceValue(DBDatabase db, String seqName, int minValue, Connection conn)
    { 
		// Use PostgreSQL Sequences
		String sqlCmd = "SELECT nextval(?)";
		Object[] sqlParams = { seqName };
		Object val = querySingleValue(sqlCmd, sqlParams, DataType.INTEGER, conn);
		if (val == null)
		{
			log.error("getNextSequenceValue: Invalid sequence value for sequence " + seqName);
		}
		return val;
    }

    /**
     * @see DBMSHandler#getNextSequenceValueExpr(DBTableColumn col)
     */
    @Override
    public DBColumnExpr getNextSequenceValueExpr(DBTableColumn column)
    {
        String seqName = StringUtils.toString(column.getDefaultValue());
        if (StringUtils.isEmpty(seqName))
        {
            throw new InvalidArgumentException("column", column);
        }
        DBSQLBuilder sql = new DBSQLBuilder(this);
        sql.append("nextval('");
        column.getDatabase().appendQualifiedName(sql, seqName, false);
        sql.append("')");
        return new DBValueExpr(column.getDatabase(), sql.toString(), DataType.INTEGER);
    }

    /**
     * Overridden. Returns a timestamp that is used for record updates created by the database server.
     * 
     * @return the current date and time of the database server.
     */
    @Override
    public java.sql.Timestamp getUpdateTimestamp(Connection conn)
    {
        // Default implementation
        GregorianCalendar cal = new GregorianCalendar();
        return new java.sql.Timestamp(cal.getTimeInMillis());
    }
    
    /**
     * Returns the Postgres DDL Generator
     */
    public PostgresDDLGenerator getDDLGenerator()
    {
        if (this.ddlGenerator==null)
            this.ddlGenerator = new PostgresDDLGenerator(this);
        return (PostgresDDLGenerator)this.ddlGenerator;
    }

    /**
     * @see DBMSHandler#getDDLScript(DDLActionType, DBObject, DBSQLScript)  
     */
    @Override
    public void getDDLScript(DDLActionType type, DBObject dbo, DBSQLScript script)
    {
        // forward request
        getDDLGenerator().getDDLScript(type, dbo, script); 
    }
    
    /**
     * Postgre needs special handling for CLOBs and BLOB's
     */
    @Override
    public Object getResultValue(ResultSet rset, int columnIndex, DataType dataType)
        throws SQLException
    {
    	switch(dataType)
    	{
    		case BLOB: return rset.getBytes(columnIndex);
    		case CLOB:	return rset.getString(columnIndex);
    		default:   return super.getResultValue(rset, columnIndex, dataType);
    	}
    }

    
    protected void addReservedKeyWord(final String keyWord){
        boolean added = reservedSQLKeywords.add(keyWord.toLowerCase());
        if(!added){
            log.debug("Existing keyWord added: " + keyWord);
        }
    }
    
    protected void setReservedKeywords(){
        // list of reserved keywords
        // http://www.postgresql.org/docs/current/static/sql-keywords-appendix.html
        addReservedKeyWord("ALL".toLowerCase());
        addReservedKeyWord("ANALYSE".toLowerCase());
        addReservedKeyWord("ANALYZE".toLowerCase());
        addReservedKeyWord("AND".toLowerCase());
        addReservedKeyWord("ANY".toLowerCase());
        addReservedKeyWord("ARRAY".toLowerCase());
        addReservedKeyWord("AS".toLowerCase());
        addReservedKeyWord("ASC".toLowerCase());
        addReservedKeyWord("ASYMMETRIC".toLowerCase());
        addReservedKeyWord("AUTHORIZATION".toLowerCase());
        addReservedKeyWord("BETWEEN".toLowerCase());
        addReservedKeyWord("BINARY".toLowerCase());
        addReservedKeyWord("BOTH".toLowerCase());
        addReservedKeyWord("CASE".toLowerCase());
        addReservedKeyWord("CAST".toLowerCase());
        addReservedKeyWord("CHECK".toLowerCase());
        addReservedKeyWord("COLLATE".toLowerCase());
        //addReservedKeyWord("COLUMN".toLowerCase());
        //addReservedKeyWord("CONSTRAINT".toLowerCase());
        addReservedKeyWord("CREATE".toLowerCase());
        addReservedKeyWord("CROSS".toLowerCase());
        addReservedKeyWord("CURRENT_DATE".toLowerCase());
        addReservedKeyWord("CURRENT_ROLE".toLowerCase());
        addReservedKeyWord("CURRENT_TIME".toLowerCase());
        addReservedKeyWord("CURRENT_TIMESTAMP".toLowerCase());
        addReservedKeyWord("CURRENT_USER".toLowerCase());
        addReservedKeyWord("DEFAULT".toLowerCase());
        addReservedKeyWord("DEFERRABLE".toLowerCase());
        addReservedKeyWord("DESC".toLowerCase());
        addReservedKeyWord("DISTINCT".toLowerCase());
        addReservedKeyWord("DO".toLowerCase());
        addReservedKeyWord("ELSE".toLowerCase());
        addReservedKeyWord("END".toLowerCase());
        addReservedKeyWord("EXCEPT".toLowerCase());
        addReservedKeyWord("FALSE".toLowerCase());
        addReservedKeyWord("FOR".toLowerCase());
        addReservedKeyWord("FOREIGN".toLowerCase());
        addReservedKeyWord("FREEZE".toLowerCase());
        addReservedKeyWord("FROM".toLowerCase());
        addReservedKeyWord("FULL".toLowerCase());
        addReservedKeyWord("GRANT".toLowerCase());
        //addReservedKeyWord("GROUP".toLowerCase());
        addReservedKeyWord("HAVING".toLowerCase());
        addReservedKeyWord("ILIKE".toLowerCase());
        addReservedKeyWord("IN".toLowerCase());
        addReservedKeyWord("INITIALLY".toLowerCase());
        addReservedKeyWord("INNER".toLowerCase());
        addReservedKeyWord("INTERSECT".toLowerCase());
        addReservedKeyWord("INTO".toLowerCase());
        addReservedKeyWord("IS".toLowerCase());
        addReservedKeyWord("ISNULL".toLowerCase());
        addReservedKeyWord("JOIN".toLowerCase());
        addReservedKeyWord("LEADING".toLowerCase());
        addReservedKeyWord("LEFT".toLowerCase());
        addReservedKeyWord("LIKE".toLowerCase());
        addReservedKeyWord("LIMIT".toLowerCase());
        addReservedKeyWord("LOCALTIME".toLowerCase());
        addReservedKeyWord("LOCALTIMESTAMP".toLowerCase());
        addReservedKeyWord("NATURAL".toLowerCase());
        addReservedKeyWord("NEW".toLowerCase());
        addReservedKeyWord("NOT".toLowerCase());
        addReservedKeyWord("NOTNULL".toLowerCase());
        addReservedKeyWord("NULL".toLowerCase());
        addReservedKeyWord("OFF".toLowerCase());
        addReservedKeyWord("OFFSET".toLowerCase());
        addReservedKeyWord("OLD".toLowerCase());
        addReservedKeyWord("ON".toLowerCase());
        addReservedKeyWord("ONLY".toLowerCase());
        addReservedKeyWord("OR".toLowerCase());
        addReservedKeyWord("ORDER".toLowerCase());
        addReservedKeyWord("OUTER".toLowerCase());
        addReservedKeyWord("OVERLAPS".toLowerCase());
        addReservedKeyWord("PLACING".toLowerCase());
        addReservedKeyWord("PRIMARY".toLowerCase());
        addReservedKeyWord("REFERENCES".toLowerCase());
        addReservedKeyWord("RETURNING".toLowerCase());
        addReservedKeyWord("RIGHT".toLowerCase());
        //addReservedKeyWord("SELECT".toLowerCase());
        addReservedKeyWord("SESSION_USER".toLowerCase());
        addReservedKeyWord("SIMILAR".toLowerCase());
        addReservedKeyWord("SOME".toLowerCase());
        addReservedKeyWord("SYMMETRIC".toLowerCase());
        //addReservedKeyWord("TABLE".toLowerCase());
        addReservedKeyWord("THEN".toLowerCase());
        addReservedKeyWord("TO".toLowerCase());
        addReservedKeyWord("TRAILING".toLowerCase());
        addReservedKeyWord("TRUE".toLowerCase());
        addReservedKeyWord("UNION".toLowerCase());
        addReservedKeyWord("UNIQUE".toLowerCase());
        //addReservedKeyWord("USER".toLowerCase());
        addReservedKeyWord("USING".toLowerCase());
        addReservedKeyWord("VERBOSE".toLowerCase());
        addReservedKeyWord("WHEN".toLowerCase());
        addReservedKeyWord("WHERE".toLowerCase());
        addReservedKeyWord("WITH".toLowerCase()); 
    }
    
    /**
     * Initializes the Sequence names of SERIAL and BIGSERIAL columns
     * @param db the database for which to set the sequence names
     * @param conn the connection
     */
    protected void initSerialSequenceNames(DBDatabase db, Connection conn)
    {
        // Find all identity columns
        Map<String, DBTableColumn> identiyColumns = new HashMap<String, DBTableColumn>();
        for (DBTable t : db.getTables())
        {
            DBColumn[] key = t.getKeyColumns();
            if (key!=null && key.length>0 && key[0].getDataType()==DataType.AUTOINC)
            {   // add to map
                String name = key[0].getFullName();
                DBTableColumn col = (DBTableColumn)key[0];
                identiyColumns.put(name, col);
                // show currently assigned sequence name
                log.info("Initial sequence name for {} is {}", name, col.getDefaultValue());
            }
        }
        // Query from database
        if (conn == null)
        {
        	// no connection, no way to ask database
        	return;
        }
        for (DBTableColumn col : identiyColumns.values())
        {
        	String seqName = getSequenceName(col, conn);
        	col.setDefaultValue(seqName);  // set the sequence name
        	log.info("New sequence name for {} is {}", col.getFullName(), seqName);
        }

    }
    
	private String getSequenceName(DBTableColumn column, Connection conn)
	{
		// Use find PostgreSQL auto-generated sequence
		String sqlCmd = "SELECT pg_get_serial_sequence(?, ?)";
		Object[] sqlParams = { column.getRowSet().getName().toLowerCase(), column.getName().toLowerCase() };
		String seqName = StringUtils.toString(querySingleValue(sqlCmd, sqlParams, DataType.VARCHAR, conn));
		if (seqName == null) { // Error!
			log.error("getNextSequenceName: Invalid sequence value for column " + column.getName());
		}
		return seqName;
	}
    
}
