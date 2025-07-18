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
package org.apache.empire.dbms.sqlite;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.apache.empire.data.DataType;
import org.apache.empire.db.DBColumnExpr;
import org.apache.empire.db.DBCommand;
import org.apache.empire.db.DBDDLGenerator;
import org.apache.empire.db.DBDDLGenerator.DDLActionType;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.DBJoinType;
import org.apache.empire.db.DBObject;
import org.apache.empire.db.DBSQLScript;
import org.apache.empire.db.DBTableColumn;
import org.apache.empire.db.expr.join.DBColumnJoinExpr;
import org.apache.empire.db.expr.join.DBJoinExpr;
import org.apache.empire.dbms.DBMSHandler;
import org.apache.empire.dbms.DBMSHandlerBase;
import org.apache.empire.dbms.DBMSFeature;
import org.apache.empire.dbms.DBSqlPhrase;
import org.apache.empire.exceptions.NotImplementedException;
import org.apache.empire.exceptions.NotSupportedException;
import org.apache.empire.exceptions.UnexpectedReturnValueException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides support for the SQLite database system.<br>
 * 
 */
public class DBMSHandlerSQLite extends DBMSHandlerBase
{
    // *Deprecated* private static final long serialVersionUID = 1L;
    private static final Logger log              = LoggerFactory.getLogger(DBMSHandlerSQLite.class);
    
    protected static final String[] SQLITE_KEYWORDS = new String[] {        
        "ABORT", "ACTION", "ADD", "AFTER", "ALL", "ALTER", "ANALYZE", "AND", "AS", "ASC", "ATTACH", "AUTOINCREMENT", "BEFORE", "BEGIN", "BETWEEN", "BY", 
        "CASCADE", "CASE", "CAST", "CHECK", "COLLATE", "COLUMN", "COMMIT", "CONFLICT", "CONSTRAINT", "CREATE", "CROSS", "CURRENT_DATE", "CURRENT_TIME", "CURRENT_DATETIME", 
        "DATABASE", "DEFAULT", "DEFERRABLE", "DEFERRED", "DELETE", "DESC", "DETACH", "DISTINCT", "DROP", "EACH", "ELSE", "END", "ESCAPE", "EXCEPT", "EXCLUSIVE", "EXISTS", "EXPLAIN", 
        "FAIL", "FOR", "FOREIGN", "FROM", "FULL", "GLOB", "GROUP", "HAVING", "IF", "IGNORE", "IMMEDIATE", "IN", "INDEX", "INDEXED", "INITIALLY", "INNER", "INSERT", "INSTEAD", "INTERSECT", "INTO", "IS", "ISNULL", 
        "JOIN", "KEY", "LEFT", "LIKE", "LIMIT", "MATCH", "NATURAL", "NO", "NOT", "NOTNULL", "NULL", "OF", "OFFSET", "ON", "OR", "ORDER", "OUTER", "PLAN", "PRAGMA", "PRIMARY", 
        "QUERY", "RAISE", "REFERENCES", "REGEXP", "REINDEX", "RELEASE", "RENAME", "REPLACE", "RESTRICT", "RIGHT", "ROLLBACK", "ROW", "SAVEPOINT", "SELECT", "SET", 
        "TABLE", "TEMP", "TEMPORARY", "THEN", "TO", "TRANSACTION", "TRIGGER", "UNION", "UNIQUE", "UPDATE", "USING", "VACUUM", "VALUES", "VIEW", "VIRTUAL", "WHEN", "WHERE",
    };
    
    /**
     * Defines the SQLite command type.
     */
    public static class DBCommandSQLite extends DBCommand
    {
        // *Deprecated* private static final long serialVersionUID = 1L;
        
        /**
         * @param dmbs the SQLLite database handler
         * @param autoPrepareStmt flag whether to automatically prepare statements
         * @see org.apache.empire.db.DBCommand
         */
        public DBCommandSQLite(DBMSHandlerSQLite dmbs, boolean autoPrepareStmt)
        {
            super(dmbs, autoPrepareStmt);
        }
        
        @Override
		public DBCommandSQLite join(DBJoinExpr join)
        {
            // http://www.sqlite.org/omitted.html
            if (join.getType() != DBJoinType.LEFT) {
                throw new NotImplementedException(join.getType(), join.getLeftTable().getName() + " join " + join.getRightTable().getName()); 
            }
            super.join(join);
            return this;
        }
        
        @Override
        public void addJoins(List<DBJoinExpr> joinExprList)
        {
            for (DBJoinExpr joinExpr : joinExprList)
            {
                if ((joinExpr instanceof DBColumnJoinExpr) && 
                    (joinExpr.getType() != DBJoinType.LEFT)) { 
                    DBColumnJoinExpr join = (DBColumnJoinExpr)joinExpr;
                    throw new NotImplementedException(joinExpr.getType(), join.getLeft() + " join " + join.getRight()); 
                }
            }
            /*
             * Iterator<DBJoinExpr> iterator = joinExprList.iterator(); for
             * (DBJoinExpr joinExpr = null; iterator.hasNext(); joinExpr =
             * iterator.next()) { if(joinExpr.getType() != DBJoinType.LEFT) {
             * iterator.remove(); } }
             */
            super.addJoins(joinExprList);
            
        }
        
    }
    
    private DBDDLGenerator<?> ddlGenerator = null; // lazy creation
                                                   
    /**
     * Constructor for the SQLite database dbms.
     */
    public DBMSHandlerSQLite()
    {
        // Add additional Keywords
        super(SQLITE_KEYWORDS);
    }
    
    /**
     * Creates a new SQLite command object.
     * 
     * @return the new DBCommandSQLite object
     */
    @Override
    public DBCommand createCommand(boolean autoPrepareStmt)
    {
        // create command object
        return new DBCommandSQLite(this, autoPrepareStmt);
    }
    
    /**
     * Returns whether or not a particular feature is supported by this dbms
     * 
     * @param type
     *            type of requested feature. @see DBMSFeature
     * @return true if the features is supported or false otherwise
     */
    @Override
    public boolean isSupported(DBMSFeature type)
    {
        switch (type)
        { // return support info
            case QUERY_LIMIT_ROWS:
                return true;
            case QUERY_SKIP_ROWS:
                return true;
            default:
                // All other features are not supported by default
                return false;
        }
    }
    
    /**
     * Gets an sql phrase template for this database system.<br>
     * 
     * @see DBMSHandler#getSQLPhrase(DBSqlPhrase)
     * @return the phrase template
     */
    @Override
    public String getSQLPhrase(DBSqlPhrase phrase)
    {
        switch (phrase)
        {
            // sql-phrases
            case SQL_NULL:            return "null";
            case SQL_PARAMETER:             return " ? ";
            case SQL_RENAME_TABLE:          return " ";
            case SQL_RENAME_COLUMN:         return " AS ";
            case SQL_DATABASE_LINK:         return "@";
            case SQL_QUOTES_OPEN:           return "`";
            case SQL_QUOTES_CLOSE:          return "`";
            case SQL_CONCAT_EXPR:           return "concat(?, {0})";
            // data types
            case SQL_BOOLEAN_TRUE:          return "1";
            case SQL_BOOLEAN_FALSE:         return "0";
            case SQL_CURRENT_DATE:          return "date('now','localtime');";
            case SQL_DATE_TEMPLATE:         return "date('{0}')";
            case SQL_CURRENT_TIME:          return "time('now');";
            case SQL_CURRENT_DATETIME:      return "NOW()";
            case SQL_DATETIME_TEMPLATE:     return "'{0}'";
            case SQL_CURRENT_TIMESTAMP:     return "NOW()";
            case SQL_TIMESTAMP_TEMPLATE:    return "'{0}'";
            // functions
            case SQL_FUNC_COALESCE:         return "coalesce(?, {0})";
            case SQL_FUNC_SUBSTRING:        return "substring(?, {0})";
            case SQL_FUNC_SUBSTRINGEX:      return "substring(?, {0}, {1})";
            case SQL_FUNC_REPLACE:          return "replace(?, {0}, {1})";
            case SQL_FUNC_REVERSE:          return "reverse(?)";
            case SQL_FUNC_STRINDEX:         return "instr(?, {0})";
            case SQL_FUNC_STRINDEXFROM:     return "locate({0}, ?, {1})";
            case SQL_FUNC_LENGTH:           return "length(?)";
            case SQL_FUNC_UPPER:            return "upper(?)";
            case SQL_FUNC_LOWER:            return "lcase(?)";
            case SQL_FUNC_TRIM:             return "trim(?)";
            case SQL_FUNC_LTRIM:            return "ltrim(?)";
            case SQL_FUNC_RTRIM:            return "rtrim(?)";
            case SQL_FUNC_ESCAPE:           return "? escape {0:VARCHAR}";
            // Numeric
            case SQL_FUNC_ABS:              return "abs(?)";
            case SQL_FUNC_ROUND:            return "round(?,{0})";
            case SQL_FUNC_TRUNC:            return "truncate(?,{0})";
            case SQL_FUNC_CEILING:          return "ceiling(?)";
            case SQL_FUNC_FLOOR:            return "floor(?)";
            case SQL_FUNC_MOD:              return "mod(?,{0})";
            case SQL_FUNC_FORMAT:           return "printf({0:VARCHAR}, ?)";
            // Date
            case SQL_FUNC_DAY:              return "day(?)";
            case SQL_FUNC_MONTH:            return "month(?)";
            case SQL_FUNC_YEAR:             return "year(?)";
            // Aggregation
            case SQL_FUNC_SUM:              return "sum(?)";
            case SQL_FUNC_MAX:              return "max(?)";
            case SQL_FUNC_MIN:              return "min(?)";
            case SQL_FUNC_AVG:              return "avg(?)";
            // Others
            case SQL_FUNC_DECODE:           return "case ? {0} end";
            case SQL_FUNC_DECODE_SEP:       return " ";
            case SQL_FUNC_DECODE_PART:      return "when {0} then {1}";
            case SQL_FUNC_DECODE_ELSE:      return "else {0}";
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
        // Convert to text
            case VARCHAR:
            case CHAR:
                if (format != null)
                { // Convert using a format string
                    if (srcType == DataType.INTEGER || srcType == DataType.AUTOINC)
                    {
                        log.error("getConvertPhrase: unknown type " + destType);
                        return "?";
                    }
                    else
                    {
                        return "to_char(?, '" + format.toString() + "')";
                    }
                }
                return "convert(?, CHAR)";
            case INTEGER:
            {
                return "convert(?, BIGINT)";
            }
            case DECIMAL:
            {
                return "convert(?, DECIMAL)";
            }
            case FLOAT:
            {
                return "convert(?, DOUBLE)";
            }
            // Unknown Type
            default:
                log.error("getConvertPhrase: unknown type (" + String.valueOf(destType));
                return "?";
        }
    }

    /**
     * Override since 
     *      conn.prepareStatement(sqlCmd, Statement.RETURN_GENERATED_KEYS) 
     * is not supported by SQLLite dbms
     */
    @Override
    public int executeSQL(String sqlCmd, Object[] sqlParams, Connection conn, DBSetGenKeys genKeys) throws SQLException
    {
        Statement stmt = null;
        int count = 0;
        try
        {
            if (sqlParams != null)
            { // Use a prepared statement
                PreparedStatement pstmt = conn.prepareStatement(sqlCmd);
                stmt = pstmt;
                prepareStatement(pstmt, sqlParams);
                count = pstmt.executeUpdate();
            }
            else
            { // Execute a simple statement
                stmt = conn.createStatement();
                count = stmt.executeUpdate(sqlCmd);
            }
            // Retrieve any auto-generated keys
            if (genKeys != null && count > 0)
            { // Return Keys
                ResultSet rs = stmt.getGeneratedKeys();
                try
                {   int rownum = 0;
                    while (rs.next())
                    {
                        genKeys.set(rownum++, rs.getObject(1));
                    }
                }
                finally
                {
                    rs.close();
                }
            }
        }
        finally
        {
            closeStatement(stmt);
        }
        return count;
    }
    
    
    @Override
    public Object getResultValue(ResultSet rset, int columnIndex, DataType dataType) throws SQLException
    {
        if (dataType == DataType.DATETIME || dataType == DataType.TIMESTAMP)
        {
            try {
                // try timestamp
                return rset.getTimestamp(columnIndex);
            } catch(Exception ex) {
                try
                {   // try Convert from String
                    String datePattern = getSQLPhrase(DBSqlPhrase.SQL_DATETIME_PATTERN);
                    DateFormat dateFormat = new SimpleDateFormat(datePattern);
                    Date timestamp = dateFormat.parse(rset.getString(columnIndex));
                    return new java.sql.Timestamp(timestamp.getTime());
                }
                catch (ParseException e)
                {   
                    throw new UnexpectedReturnValueException(rset.getString(columnIndex), "getResultValue");
                }
            }
        }
        else if (dataType == DataType.CLOB)
        {
            java.sql.Clob clob = rset.getClob(columnIndex);
            return ((clob != null) ? clob.getSubString(1, (int) clob.length()) : null);
        }
        else if (dataType == DataType.BLOB)
        { // Get bytes of a binary large object
            java.sql.Blob blob = rset.getBlob(columnIndex);
            return ((blob != null) ? blob.getBytes(1, (int) blob.length()) : null);
        }
        else
        {
            return rset.getObject(columnIndex);
        }
    }
    
    /**
     * Overridden. Returns a timestamp that is used for record updates created
     * by the database server.
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
     * @see DBMSHandler#getDDLScript(DDLActionType, DBObject, DBSQLScript)
     */
    @Override
    public void getDDLScript(DDLActionType type, DBObject dbo, DBSQLScript script)
    {
        if (ddlGenerator == null)
            ddlGenerator = new SQLiteDDLGenerator(this);
        // forward request
        ddlGenerator.getDDLScript(type, dbo, script);
    }
    
    @Override
    public Object getNextSequenceValue(DBDatabase db, String SeqName, int minValue, Connection conn)
    {
        throw new NotImplementedException(db, " sequence values are assigned dynamicaly from sqlite ");
    }

    /**
     * @see DBMSHandlerBase#getNextSequenceValueExpr(DBTableColumn col)
     */
    @Override
    public DBColumnExpr getNextSequenceValueExpr(DBTableColumn column)
    {
        throw new NotSupportedException(this, "getNextSequenceValueExpr");
    }
    
}
