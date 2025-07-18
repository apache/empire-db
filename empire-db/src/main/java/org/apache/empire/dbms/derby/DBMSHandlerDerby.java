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
package org.apache.empire.dbms.derby;

import java.sql.Connection;
import java.util.GregorianCalendar;

import org.apache.empire.data.DataType;
import org.apache.empire.db.DBColumnExpr;
import org.apache.empire.db.DBDDLGenerator;
import org.apache.empire.db.DBDDLGenerator.DDLActionType;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.DBObject;
import org.apache.empire.db.DBSQLScript;
import org.apache.empire.db.DBTable;
import org.apache.empire.db.DBTableColumn;
import org.apache.empire.dbms.DBMSFeature;
import org.apache.empire.dbms.DBMSHandler;
import org.apache.empire.dbms.DBMSHandlerBase;
import org.apache.empire.dbms.DBSqlPhrase;
import org.apache.empire.exceptions.NotSupportedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class provides support for the Derby database system.
 * 
 *
 */
public class DBMSHandlerDerby extends DBMSHandlerBase
{
	// *Deprecated* private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(DBMSHandlerDerby.class);
    
    protected static final String[] DERBY_SQL_KEYWORDS = new String[] { "count", "year" };        
    
    // Properties
    private String databaseName = null;
    // Sequence treatment
    // When set to 'false' (default) Derby's autoincrement feature is used.
    private boolean useSequenceTable = false;
    private String sequenceTableName = "Sequences";

    private DBDDLGenerator<?> ddlGenerator = null; // lazy creation
    
    /**
     * Constructor for the Derby database handler.<br>
     */
    public DBMSHandlerDerby()
    {
        // Add additional reserved keywords
        super(DERBY_SQL_KEYWORDS);
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
     * When a name is set, the handler will automatically execute 'USE dbname' when the database is opened.
     * @param databaseName the name of the database
     */
    public void setDatabaseName(String databaseName)
    {
        this.databaseName = databaseName;
    }

    /**
     * returns whether a sequence table is used for record identiy management.<br>
     * Default is false. In this case the AutoIncrement feature of Derby is used.
     * @return true if a sequence table is used instead of identity columns.
     */
    public boolean isUseSequenceTable()
    {
        return useSequenceTable;
    }

    /**
     * If set to true a special table is used for sequence number generation.<br>
     * Otherwise the AutoIncrement feature of Derby is used identiy fields. 
     * @param useSequenceTable true to use a sequence table or false otherwise.
     */
    public void setUseSequenceTable(boolean useSequenceTable)
    {
        this.useSequenceTable = useSequenceTable;
    }

    /**
     * returns the name of the sequence table
     * @return the name of the table used for sequence number generation
     */
    public String getSequenceTableName()
    {
        return sequenceTableName;
    }

    /**
     * Sets the name of the sequence table.
     * Only applicable if useSequenceTable is set to true.
     * @param sequenceTableName the name of the table used for sequence number generation
     */
    public void setSequenceTableName(String sequenceTableName)
    {
        this.sequenceTableName = sequenceTableName;
    }

    /**
     * Returns whether or not a particular feature is supported by this dbms
     * @param type type of requrested feature. @see DBMSFeature
     * @return true if the features is supported or false otherwise
     */
    @Override
    public boolean isSupported(DBMSFeature type)
    {
        switch (type)
        {   // return support info 
            case CREATE_SCHEMA: return true;
            case SEQUENCES:     return useSequenceTable;
            default:            return false;
        }
    }
    
    /**
     * Gets an sql phrase template for this database system.<br>
     * @see DBMSHandler#getSQLPhrase(DBSqlPhrase)
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
            case SQL_BOOLEAN_TRUE:            return "1";
            case SQL_BOOLEAN_FALSE:           return "0";
            case SQL_CURRENT_DATE:            return "CURRENT_DATE";
            case SQL_DATE_TEMPLATE:           return "'{0}'";
            case SQL_CURRENT_TIME:            return "CURRENT_TIME";
            case SQL_TIME_TEMPLATE:           return "'{0}'";            
            case SQL_CURRENT_DATETIME:        return "CURRENT_TIMESTAMP";
            case SQL_DATETIME_TEMPLATE:       return "'{0}'";
            case SQL_CURRENT_TIMESTAMP:       return "CURRENT_TIMESTAMP";
            case SQL_TIMESTAMP_TEMPLATE:      return "'{0}'";
            // DateTimeFormatter
            case SQL_TIMESTAMP_PATTERN:       return "yyyy-MM-dd HH:mm:ss.SSS ";  // limit to 3 fractions
            // functions
            case SQL_FUNC_COALESCE:           return "coalesce(?, {0})";
            case SQL_FUNC_SUBSTRING:          return "substr(?, {0})";
            case SQL_FUNC_SUBSTRINGEX:        return "substr(?, {0}, {1})";
            case SQL_FUNC_REPLACE:            return "replace(?, {0}, {1})";
            case SQL_FUNC_REVERSE:            return "reverse_not_available_in_derby(?)"; 
            case SQL_FUNC_STRINDEX:           return "locate({0}, ?)"; 
            case SQL_FUNC_STRINDEXFROM:       return "locate({0}, ?, {1})"; 
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
            case SQL_FUNC_MOD:                return "mod(?,{0})";
            case SQL_FUNC_FORMAT:             return "format(?, {0:VARCHAR})";
            // Date
            case SQL_FUNC_DAY:                return "day(?)";
            case SQL_FUNC_MONTH:              return "month(?)";
            case SQL_FUNC_YEAR:               return "year(?)";
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
        switch(destType)
        {
           case BOOL:      return "CAST(? AS UNSIGNED)";
           case INTEGER:   return "CAST(? AS SIGNED)";
           case DECIMAL:   return "CAST(? AS DECIMAL)";
           case FLOAT:     return "CAST(? AS DECIMAL)";
           case DATE:      return "CAST(? AS DATE)";
           case TIME:      return "CAST(? AS TIME)";
           case DATETIME:
           case TIMESTAMP: return "CAST(? AS TIMESTAMP)";
           // Convert to text
           case VARCHAR:
           case CHAR:
                return "CAST(? AS CHAR)";
           case BLOB:
                return "CAST(? AS BLOB)";
           // Unknown Type                                       
           default:
                log.error("getConvertPhrase: unknown type " + destType);
                return "?";
        }
    }
    
    /**
     * @see DBMSHandlerBase#getNextSequenceValue(DBDatabase, String, int, Connection)
     */
    @Override
    public Object getNextSequenceValue(DBDatabase db, String seqName, int minValue, Connection conn)
    {   //Use Oracle Sequences
        if (useSequenceTable)
        {   // Use a sequence Table to generate Sequences
            DBTable t = db.getTable(sequenceTableName);
            return ((DBSeqTable)t).getNextValue(seqName, minValue, conn);
        }
        else
        {   // Post Detection
            return null;
        }
    }

    /**
     * @see DBMSHandlerBase#getNextSequenceValueExpr(DBTableColumn col)
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
            ddlGenerator = new DerbyDDLGenerator(this);
        // forward request
        ddlGenerator.getDDLScript(type, dbo, script); 
    }

}
