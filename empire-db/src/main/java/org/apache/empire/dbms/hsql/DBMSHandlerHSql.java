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
package org.apache.empire.dbms.hsql;

import java.sql.Connection;
import java.util.GregorianCalendar;

import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.data.DataType;
import org.apache.empire.db.DBColumnExpr;
import org.apache.empire.db.DBCommand;
import org.apache.empire.db.DBDDLGenerator;
import org.apache.empire.db.DBDDLGenerator.DDLActionType;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.DBObject;
import org.apache.empire.db.DBSQLScript;
import org.apache.empire.db.DBTableColumn;
import org.apache.empire.db.exceptions.QueryNoResultException;
import org.apache.empire.db.validation.DBModelChecker;
import org.apache.empire.dbms.DBMSFeature;
import org.apache.empire.dbms.DBMSHandler;
import org.apache.empire.dbms.DBMSHandlerBase;
import org.apache.empire.dbms.DBSqlPhrase;
import org.apache.empire.exceptions.NotSupportedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class provides support for the HSQLDB database system.<br>
 * 
 *
 * 
 */
public class DBMSHandlerHSql extends DBMSHandlerBase
{
    // *Deprecated* private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(DBMSHandlerHSql.class);
    
    private DBDDLGenerator<?> ddlGenerator = null; // lazy creation
	
    /**
     * Constructor for the HSQLDB database dbms.
     */
    public DBMSHandlerHSql()
    {
        // Add "count" to list of reserved keywords
        reservedSQLKeywords.add("count");
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
            case CREATE_SCHEMA:     return false;
            case SEQUENCES:         return true;
            case QUERY_LIMIT_ROWS:  return true;
            case QUERY_SKIP_ROWS:   return true;
            default:
                // All other features are not supported by default
                return false;
        }
    }

    /**
     * Override standard command
     */
    @Override
    public DBCommand createCommand(boolean autoPrepareStmt)
    {
        return new DBCommandHSql(autoPrepareStmt);
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
            case SQL_PARAMETER:         return " ? ";
            case SQL_RENAME_TABLE:      return " ";
            case SQL_RENAME_COLUMN:     return " AS ";
            case SQL_DATABASE_LINK:     return "@";
            case SQL_QUOTES_OPEN:       return "\"";
            case SQL_QUOTES_CLOSE:      return "\"";
            case SQL_CONCAT_EXPR:       return "concat(?, {0})"; // " + " leads to problems if operands are case when statements that return empty string 
            // data types
            case SQL_BOOLEAN_TRUE:      return String.valueOf(Boolean.TRUE);
            case SQL_BOOLEAN_FALSE:     return String.valueOf(Boolean.FALSE);
            case SQL_CURRENT_DATE:      return "CURRENT_DATE";
            case SQL_DATE_PATTERN:      return "yyyy-MM-dd";
            case SQL_DATE_TEMPLATE:     return "'{0}'";
            case SQL_DATETIME_PATTERN:  return "yyyy-MM-dd HH:mm:ss.S";
            case SQL_DATETIME_TEMPLATE: return "'{0}'";
            case SQL_CURRENT_TIMESTAMP: return "CURRENT_TIMESTAMP";
            case SQL_TIMESTAMP_PATTERN: return "yyyy-MM-dd HH:mm:ss.S";
            case SQL_TIMESTAMP_TEMPLATE:return "'{0}'";
            // functions
            case SQL_FUNC_COALESCE:     return "coalesce(?, {0})";
            case SQL_FUNC_SUBSTRING:    return "substr(?, {0})";
            case SQL_FUNC_SUBSTRINGEX:  return "substr(?, {0}, {1})";
            case SQL_FUNC_REPLACE:      return "replace(?, {0}, {1})";
            case SQL_FUNC_REVERSE:      return "?"; // "reverse(?)"; 
            case SQL_FUNC_STRINDEX:     return "locate({0}, ?)"; 
            case SQL_FUNC_STRINDEXFROM: return "locate({0}, ?, {1})"; 
            case SQL_FUNC_UPPER:        return "ucase(?)";
            case SQL_FUNC_LOWER:        return "lcase(?)";
            case SQL_FUNC_LENGTH:       return "length(?)";
            case SQL_FUNC_TRIM:         return "trim(?)";
            case SQL_FUNC_LTRIM:        return "ltrim(?)";
            case SQL_FUNC_RTRIM:        return "rtrim(?)";
            case SQL_FUNC_ESCAPE:       return "? escape '{0}'";
            // Numeric
            case SQL_FUNC_ABS:          return "abs(?)";
            case SQL_FUNC_ROUND:        return "round(?,{0})";
            case SQL_FUNC_TRUNC:        return "truncate(?,{0})";
            case SQL_FUNC_CEILING:      return "ceiling(?)";
            case SQL_FUNC_FLOOR:        return "floor(?)";
            case SQL_FUNC_MODULO:       return "mod(?,{0})";
            case SQL_FUNC_FORMAT:       return "TO_CHAR(?, {0:VARCHAR})";
            // Date
            case SQL_FUNC_DAY:          return "day(?)";
            case SQL_FUNC_MONTH:        return "month(?)";
            case SQL_FUNC_YEAR:         return "year(?)";
            // Aggregation
            case SQL_FUNC_SUM:          return "sum(?)";
            case SQL_FUNC_MAX:          return "max(?)";
            case SQL_FUNC_MIN:          return "min(?)";
            case SQL_FUNC_AVG:          return "avg(?)";
            // Others
            case SQL_FUNC_DECODE:       return "case ?{0} end";
            case SQL_FUNC_DECODE_SEP:   return " ";
            case SQL_FUNC_DECODE_PART:  return "when {0} then {1}";
            case SQL_FUNC_DECODE_ELSE:  return "else {0}";
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
            /*
             * case DBExpr.DT_BOOL: return "convert(bit, ?)"; case DBExpr.DT_INTEGER: return "convert(int, ?)"; case
             * DBExpr.DT_DECIMAL: return "convert(decimal, ?)"; case DBExpr.DT_NUMBER: return "convert(float, ?)"; case
             * DBExpr.DT_DATE: return "convert(datetime, ?, 111)"; case DBExpr.DT_DATETIME: return "convert(datetime, ?, 120)";
             */
            // Convert to text
            case VARCHAR:
            case CHAR:
                if (format instanceof String)
                { // Convert using a format string
                    if (srcType == DataType.INTEGER || srcType == DataType.AUTOINC)
                    {
                        log.error("getConvertPhrase: unknown type " + destType);
                        return "?";
                    }
                    else
                    {
                        return "to_char(?, '"+format.toString()+"')";
                    }
                }
                else if (format instanceof Number)
                {   // size given
                    int size = ((Number)format).intValue();
                    return "convert(?, "+destType.name()+"("+String.valueOf(size)+"))";
                }
                else
                {   // default
                    return "convert(?, "+destType.name()+"(255))";
                }
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
     * @see DBMSHandler#getNextSequenceValue(DBDatabase, String, int, Connection)
     */
    @Override
    public Object getNextSequenceValue(DBDatabase db, String seqName, int minValue, Connection conn)
    { 	//Use Oracle Sequences
        StringBuilder sql = new StringBuilder(80);
        sql.append("SELECT ");
        sql.append("NEXT VALUE FOR ");
        db.appendQualifiedName(sql, seqName, null);
        sql.append(" FROM INFORMATION_SCHEMA.SYSTEM_SEQUENCES WHERE SEQUENCE_NAME='").append(seqName).append("'");
        // Query next sequence value
        String sqlCmd = sql.toString();
        if (log.isDebugEnabled())
            log.debug("Executing: " + sqlCmd);
        Object val = querySingleValue(sqlCmd, null, DataType.INTEGER, conn);
        if (ObjectUtils.isEmpty(val))
        {   // Error!
            log.error("getNextSequenceValue: Invalid sequence value for sequence " + seqName);
            throw new QueryNoResultException(sqlCmd);
        }
        // Done
        return val;
    }

    /**
     * @see DBMSHandler#getNextSequenceValueExpr(DBTableColumn col)
     */
    @Override
    public DBColumnExpr getNextSequenceValueExpr(DBTableColumn column)
    {
        throw new NotSupportedException(this, "getNextSequenceValueExpr");
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
     * @see DBMSHandler#getDDLScript(DDLActionType, DBObject, DBSQLScript)  
     */
    @Override
    public void getDDLScript(DDLActionType type, DBObject dbo, DBSQLScript script)
    {
        if (ddlGenerator==null)
            ddlGenerator = new HSqlDDLGenerator(this);
        // forward request
        ddlGenerator.getDDLScript(type, dbo, script); 
    }

    /**
     * Creates a DBModelChecker instance of this DBMSHandler
     * @return the DBModelChecker
     */
    @Override
    public DBModelChecker createModelChecker(DBDatabase db)
    {
        // the default model checker
        return new DBModelChecker(createModelParser("PUBLIC", "PUBLIC")); 
    }
    
}

