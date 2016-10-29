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
package org.apache.empire.db.postgresql;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.GregorianCalendar;

import org.apache.empire.data.DataType;
import org.apache.empire.db.DBCmdType;
import org.apache.empire.db.DBCommand;
import org.apache.empire.db.DBDDLGenerator;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.DBDatabaseDriver;
import org.apache.empire.db.DBDriverFeature;
import org.apache.empire.db.DBObject;
import org.apache.empire.db.DBSQLScript;
import org.apache.empire.db.exceptions.EmpireSQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class provides support for the PostgreSQL database system.
 * 
 *
 */
public class DBDatabaseDriverPostgreSQL extends DBDatabaseDriver
{
    private final static long serialVersionUID = 1L;
  
    private static final Logger log = LoggerFactory.getLogger(DBDatabaseDriverPostgreSQL.class);
    
    private static final String CREATE_REVERSE_FUNCTION =
        "CREATE OR REPLACE FUNCTION reverse(TEXT) RETURNS TEXT AS $$\n" +
        "DECLARE\n" +
        "   original ALIAS FOR $1;\n" +
        "   reversed TEXT := '';\n" +
        "   onechar  VARCHAR;\n" +
        "   mypos    INTEGER;\n" +
        "BEGIN\n" +
        "   SELECT LENGTH(original) INTO mypos;\n" + 
        "   LOOP\n" +
        "      EXIT WHEN mypos < 1;\n" +
        "      SELECT substring(original FROM mypos FOR 1) INTO onechar;\n" +
        "      reversed := reversed || onechar;\n" +
        "      mypos := mypos -1;\n" +
        "   END LOOP;\n" +
        "   RETURN reversed;\n" +
        "END\n" +
        "$$ LANGUAGE plpgsql IMMUTABLE RETURNS NULL ON NULL INPUT";    
    
    /**
     * Defines the PostgreSQL command type.
     */ 
    public static class DBCommandPostreSQL extends DBCommand
    {
        private final static long serialVersionUID = 1L;
      
        protected int limit = -1;
        protected int skip  = -1;
        
        public DBCommandPostreSQL(DBDatabase db)
        {
            super(db);
        }
        
        @Override
        public void limitRows(int numRows)
        {
            limit = numRows;
        }

        @Override
        public void skipRows(int numRows)
        {
            skip = numRows;
        }
         
        @Override
        public void clearLimit()
        {
            limit = -1;
            skip  = -1;
        }
        
        @Override
        public void getSelect(StringBuilder buf)
        {   // call base class
            super.getSelect(buf);
            // add limit and offset
            if (limit>=0)
            {   buf.append("\r\nLIMIT ");
                buf.append(String.valueOf(limit));
                // Offset
                if (skip>=0) 
                {   buf.append(" OFFSET ");
                    buf.append(String.valueOf(skip));
                }    
            }
        }
    }
    
    private String databaseName;

    private DBDDLGenerator<?> ddlGenerator = null; // lazy creation
    
    /**
     * Constructor for the PostgreSQL database driver.<br>
     */
    public DBDatabaseDriverPostgreSQL()
    {
        setReservedKeywords();
    }
    
    private void addReservedKeyWord(final String keyWord){
        boolean added = reservedSQLKeywords.add(keyWord.toLowerCase());
        if(!added){
            log.debug("Existing keyWord added: " + keyWord);
        }
    }
    
    private void setReservedKeywords(){
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
     * returns the name for the database / schema
     * @return the database / schema name
     */
    public String getDatabaseName()
    {
        return databaseName;
    }

    /**
     * Sets the name for the database / schema<br>
     * This names is required for creating a database.<br>
     * When a name is set, the driver will automatically execute 'USE dbname' when the database is opened.
     * @param databaseName the name of the database
     */
    public void setDatabaseName(String databaseName)
    {
        this.databaseName = databaseName;
    }
    
    /**
     * Creates the reverse function in postgre sql that returns the reverse of a string value.
     * The reverse function may be helpful in SQL to analyze a text field from its end.
     * This function must be called manually by the application depending on whether it needs to use this function or not.<br>
     * The current implementation does not check, whether the reverse function already exists.
     * If the functions exists it will be replaced and true is returned.
     * @param conn a valid database connection
     */
    public void createReverseFunction(Connection conn)
    {
        try {
            log.info("Creating reverse function: " + CREATE_REVERSE_FUNCTION);
            executeSQL(CREATE_REVERSE_FUNCTION, null, conn, null);
        } catch(SQLException e) {
            log.error("Unable to create reverse function!", e);
            throw new EmpireSQLException(this, e);
        }
    }
    
    /**
     * Creates a new PostgreSQL command object.
     * 
     * @return the new DBCommandPostgreSQL object
     */
    @Override
    public DBCommand createCommand(DBDatabase db)
    {
        if (db == null)
            return null;
        // create command object
        return new DBCommandPostreSQL(db);
    }

    /**
     * Returns whether or not a particular feature is supported by this driver
     * @param type type of requested feature. @see DBDriverFeature
     * @return true if the features is supported or false otherwise
     */
    @Override
    public boolean isSupported(DBDriverFeature type)
    {
        switch (type)
        {   // return support info 
            case CREATE_SCHEMA: 	return true;
            case SEQUENCES:     	return true;    
            case QUERY_LIMIT_ROWS:  return true;
            case QUERY_SKIP_ROWS:   return true;
            default:
                // All other features are not supported by default
                return false;
        }
    }
    
    /**
     * Gets an sql phrase template for this database system.<br>
     * @see DBDatabaseDriver#getSQLPhrase(int)
     * @return the phrase template
     */
    @Override
    public String getSQLPhrase(int phrase)
    {
        switch (phrase)
        {
            // sql-phrases
            case SQL_NULL_VALUE:              return "null";
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
            case SQL_CURRENT_DATE:            return "CURRENT_DATE()";
            case SQL_DATE_PATTERN:            return "yyyy-MM-dd";
            case SQL_DATE_TEMPLATE:           return "'{0}'";
            case SQL_CURRENT_DATETIME:        return "NOW()";
            case SQL_DATETIME_PATTERN:        return "yyyy-MM-dd HH:mm:ss";
            case SQL_DATETIME_TEMPLATE:       return "'{0}'";
            // functions
            case SQL_FUNC_COALESCE:           return "coalesce(?, {0})";
            case SQL_FUNC_SUBSTRING:          return "substring(?, {0})";
            case SQL_FUNC_SUBSTRINGEX:        return "substring(?, {0}, {1})";
            case SQL_FUNC_REPLACE:            return "replace(?, {0}, {1})";
            case SQL_FUNC_REVERSE:            return "reverse(?)"; // In order to use this function createReverseFunction() must be called first!
            case SQL_FUNC_STRINDEX:           return "strpos(?, {0})"; 
            case SQL_FUNC_STRINDEXFROM:       return "strindexfrom_not_available_in_pgsql({0}, ?, {1})"; // "locate({0}, ?, {1})"; 
            case SQL_FUNC_LENGTH:             return "length(?)";
            case SQL_FUNC_UPPER:              return "upper(?)";
            case SQL_FUNC_LOWER:              return "lower(?)";
            case SQL_FUNC_TRIM:               return "trim(?)";
            case SQL_FUNC_LTRIM:              return "ltrim(?)";
            case SQL_FUNC_RTRIM:              return "rtrim(?)";
            case SQL_FUNC_ESCAPE:             return "? escape '{0}'";
            // Numeric
            case SQL_FUNC_ABS:                return "abs(?)";
            case SQL_FUNC_ROUND:              return "round(?,{0})";
            case SQL_FUNC_TRUNC:              return "truncate(?,{0})";
            case SQL_FUNC_CEILING:            return "ceiling(?)";
            case SQL_FUNC_FLOOR:              return "floor(?)";
            case SQL_FUNC_MODULO:             return "mod(?,{0})";
            // Date
			case SQL_FUNC_DAY:                return "extract(day from ?)";
			case SQL_FUNC_MONTH:              return "extract(month from ?)";
			case SQL_FUNC_YEAR:               return "extract(year from ?)";
            // Aggregation
            case SQL_FUNC_SUM:                return "sum(?)";
            case SQL_FUNC_MAX:                return "max(?)";
            case SQL_FUNC_MIN:                return "min(?)";
            case SQL_FUNC_AVG:                return "avg(?)";
            // Others
            case SQL_FUNC_DECODE:             return "case ? {0} end";
            case SQL_FUNC_DECODE_SEP:         return " ";
            case SQL_FUNC_DECODE_PART:        return "when {0} then {1}";
            case SQL_FUNC_DECODE_ELSE:        return "else {0}";
            // Not defined
            default:
                log.error("SQL phrase " + String.valueOf(phrase) + " is not defined!");
                return "?";
        }
    }

    /**
     * @see DBDatabaseDriver#getConvertPhrase(DataType, DataType, Object)
     */
    @Override
    public String getConvertPhrase(DataType destType, DataType srcType, Object format)
    {
        switch (destType)
        {
            case BOOL:     return "CAST(? AS BOOL)";
            case INTEGER:  return "CAST(? AS INTEGER)";
            case DECIMAL:  return "CAST(? AS DECIMAL)";
            case FLOAT:   return "CAST(? AS DOUBLE PRECISION)";
            case DATE:     return "CAST(? AS DATE)";
            case DATETIME: return "CAST(? AS TIMESTAMP)";
                // Convert to text
            case TEXT:     return "CAST(? AS CHAR)";
            case BLOB:     return "CAST(? AS bytea)";
            case CLOB:     return "CAST(? AS TEXT)";
                // Unknown Type
            default:
                log.error("getConvertPhrase: unknown type (" + String.valueOf(destType));
                return "?";
        }
    }

    
    /**
     * @see DBDatabaseDriver#getNextSequenceValue(DBDatabase, String, int, Connection)
     */
    @Override
    public Object getNextSequenceValue(DBDatabase db, String seqName, int minValue, Connection conn)
    { 
        // Use PostgreSQL Sequences
        StringBuilder sql = new StringBuilder(80);
        sql.append("SELECT nextval('");
        db.appendQualifiedName(sql, seqName, detectQuoteName(seqName));
        sql.append("')");
        Object val = db.querySingleValue(sql.toString(), null, conn);
        if (val == null)
        { // Error!
            log.error("getNextSequenceValue: Invalid sequence value for sequence " + seqName);
        }
        return val;
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
     * @see DBDatabaseDriver#getDDLScript(DBCmdType, DBObject, DBSQLScript)  
     */
    @Override
    public void getDDLScript(DBCmdType type, DBObject dbo, DBSQLScript script)
    {
        if (ddlGenerator==null)
            ddlGenerator = new PostgreDDLGenerator(this);
        // forward request
        ddlGenerator.getDDLScript(type, dbo, script); 
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

    
}
