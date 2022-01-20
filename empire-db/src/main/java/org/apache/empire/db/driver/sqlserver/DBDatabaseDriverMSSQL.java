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
package org.apache.empire.db.driver.sqlserver;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.GregorianCalendar;

import org.apache.empire.commons.StringUtils;
import org.apache.empire.data.DataType;
import org.apache.empire.db.DBCmdType;
import org.apache.empire.db.DBColumnExpr;
import org.apache.empire.db.DBCommand;
import org.apache.empire.db.DBDDLGenerator;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.DBDatabaseDriver;
import org.apache.empire.db.DBDriverFeature;
import org.apache.empire.db.DBExpr;
import org.apache.empire.db.DBObject;
import org.apache.empire.db.DBRelation;
import org.apache.empire.db.DBSQLScript;
import org.apache.empire.db.DBTable;
import org.apache.empire.db.DBTableColumn;
import org.apache.empire.db.exceptions.EmpireSQLException;
import org.apache.empire.exceptions.NotSupportedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides support for the Microsoft SQL-Server database system.
 * 
 *
 * 
 */
public class DBDatabaseDriverMSSQL extends DBDatabaseDriver
{
    private final static long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(DBDatabaseDriverMSSQL.class);
  
    /**
     * Defines the Microsoft SQL-Server command type.
     */ 
    public static class DBCommandMSSQL extends DBCommand
    {
        private final static long serialVersionUID = 1L;
        protected int limit = -1;

        public DBCommandMSSQL(DBDatabase db)
    	{
    		super(db);
    	}
        
        @Override
        public void limitRows(int numRows)
        {
            limit = numRows;
        }
         
        @Override
        public void clearLimit()
        {
            limit = -1;
        }
        
        @Override
        protected void addSelect(StringBuilder buf)
        {   
            // Prepares statement
            buf.append("SELECT ");
            if (selectDistinct)
                buf.append("DISTINCT ");
            // Add limit
            if (limit>=0)
            {   // Limit
                buf.append("TOP ");
                buf.append(String.valueOf(limit));
                buf.append(" ");
            }
            // Add Select Expressions
            addListExpr(buf, select, CTX_ALL, ", ");
        }
    }
    
    // Properties
    private String databaseName = null;
    private String objectOwner = "dbo";
    private String sequenceTableName = "Sequences";
    // Sequence treatment
    // When set to 'false' (default) MySQL's IDENTITY feature is used.
    private boolean useSequenceTable = false;
    private boolean useUnicodePrefix = true;
    private boolean useDateTime2 = true;
    
    protected DBDDLGenerator<?> ddlGenerator = null; // lazy creation

    protected static final String[] MSSQL_SQL_KEYWORDS = new String[] { "type", "key", "plan" };        
    
    /**
     * Constructor for the MSSQL database driver.<br>
     */
    public DBDatabaseDriverMSSQL()
    {
        // Initialize List of reserved Keywords
        for (String keyWord:MSSQL_SQL_KEYWORDS){
             reservedSQLKeywords.add(keyWord);
        }
    }

    public String getDatabaseName()
    {
        return databaseName;
    }

    public void setDatabaseName(String databaseName)
    {
        this.databaseName = databaseName;
    }

    public String getObjectOwner()
    {
        return objectOwner;
    }

    public void setObjectOwner(String objectOwner)
    {
        this.objectOwner = objectOwner;
    }

    /**
     * returns whether a sequence table is used for record identiy management.<br>
     * Default is false. In this case the AutoIncrement feature of MySQL is used.
     * @return true if a sequence table is used instead of identity columns.
     */
    public boolean isUseSequenceTable()
    {
        return useSequenceTable;
    }

    /**
     * If set to true a special table is used for sequence number generation.<br>
     * Otherwise the AutoIncrement feature of MySQL is used identiy fields. 
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
     * @param sequenceTableName the name of the table used for sequence number generation
     */
	public void setSequenceTableName(String sequenceTableName)
	{
		this.sequenceTableName = sequenceTableName;
	}

	/**
	 * Indicates whether or not a Unicode Prefix (N) is prepended to all text values
	 */
    public boolean isUseUnicodePrefix()
    {
        return useUnicodePrefix;
    }

    /**
     * Sets whether or not to use a Unicode Prefix (N) for all text values
     * Default is true 
     * @param useUnicodePrefix true if a Unicode Prefix (N) should be used for text values
     */
    public void setUseUnicodePrefix(boolean useUnicodePrefix)
    {
        this.useUnicodePrefix = useUnicodePrefix;
    }

    /**
     * returns whether the DATETIME2 datatype is used for timestamps (instead of DATETIME)
     */
    public boolean isUseDateTime2()
    {
        return useDateTime2;
    }

    /**
     * Sets whether or not to use the DATETIME2 datatype instead of DATETIME for timestamps
     * Default is true (set to false for existing databases using DATETIME!) 
     * @param useDateTime2 true if DATETIME2 or false if DATETIME is used
     */
    public void setUseDateTime2(boolean useDateTime2)
    {
        this.useDateTime2 = useDateTime2;
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unused")
    @Override
    protected void attachDatabase(DBDatabase db, Connection conn)
    {
        // Prepare
        try
        {   // Set Database
            if (StringUtils.isNotEmpty(databaseName))
                executeSQL("USE " + databaseName, null, conn, null);
            // Dateformat must be ymd!
            executeSQL("set dateformat ymd", null, conn, null);
            // Sequence Table
            if (useSequenceTable && db.getTable(sequenceTableName)==null)
                new DBSeqTable(sequenceTableName, db);
            // Check Schema
            String schema = db.getSchema();
            if (StringUtils.isNotEmpty(schema) && schema.indexOf('.')<0 && StringUtils.isNotEmpty(objectOwner))
            {   // append database owner
                db.setSchema(schema + "." + objectOwner);
            }
            // call Base implementation
            super.attachDatabase(db, conn);
            
        } catch (SQLException e) {
            // throw exception
            throw new EmpireSQLException(this, e);
        }
    }

    /**
     * Creates a new Microsoft SQL-Server command object.
     * 
     * @return the new DBCommandMSSQL object
     */
    @Override
    public DBCommand createCommand(DBDatabase db)
    {
        if (db == null)
            return null;
        // create command object
        return new DBCommandMSSQL(db);
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
            case CREATE_SCHEMA:     return true;
            case SEQUENCES:         return useSequenceTable;    
            case QUERY_LIMIT_ROWS:  return true;
            case QUERY_SKIP_ROWS:   return false;
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
            case SQL_QUOTES_OPEN:             return "[";
            case SQL_QUOTES_CLOSE:            return "]";
            case SQL_CONCAT_EXPR:             return " + ";
            case SQL_PSEUDO_TABLE:            return ""; // not necessary (FROM will be omitted)            
            // data types
            case SQL_BOOLEAN_TRUE:            return "1";
            case SQL_BOOLEAN_FALSE:           return "0";
            case SQL_CURRENT_DATE:            return "convert(char, getdate(), 111)";
            case SQL_DATE_PATTERN:            return "yyyy-MM-dd";
            case SQL_DATE_TEMPLATE:           return "convert(date, '{0}', 111)";
            case SQL_DATETIME_PATTERN:        return "yyyy-MM-dd HH:mm:ss.SSS";
            case SQL_DATETIME_TEMPLATE:       return isUseDateTime2() ? "convert(datetime2, '{0}', 121)"
                                                                      : "convert(datetime,  '{0}', 121)";
            case SQL_CURRENT_TIMESTAMP:       return "getdate()";
            case SQL_TIMESTAMP_PATTERN:       return "yyyy-MM-dd HH:mm:ss.SSS";
            case SQL_TIMESTAMP_TEMPLATE:      return isUseDateTime2() ? "convert(datetime2, '{0}', 121)"
                                                                      : "convert(datetime,  '{0}', 121)";
            // functions
            case SQL_FUNC_COALESCE:           return "coalesce(?, {0})";
            case SQL_FUNC_SUBSTRING:          return "substring(?, {0}, 4000)";
            case SQL_FUNC_SUBSTRINGEX:        return "substring(?, {0}, {1})";
            case SQL_FUNC_REPLACE:            return "replace(?, {0}, {1})";
            case SQL_FUNC_REVERSE:            return "reverse(?)"; 
            case SQL_FUNC_STRINDEX:           return "charindex({0}, ?)"; 
            case SQL_FUNC_STRINDEXFROM:       return "charindex({0}, ?, {1})"; 
            case SQL_FUNC_LENGTH:             return "len(?)";
            case SQL_FUNC_UPPER:              return "upper(?)";
            case SQL_FUNC_LOWER:              return "lower(?)";
            case SQL_FUNC_TRIM:               return "trim(?)";
            case SQL_FUNC_LTRIM:              return "ltrim(?)";
            case SQL_FUNC_RTRIM:              return "rtrim(?)";
            case SQL_FUNC_ESCAPE:             return "? escape '{0}'";
            // Numeric
            case SQL_FUNC_ABS:                return "abs(?)";
            case SQL_FUNC_ROUND:              return "round(?,{0})";
            case SQL_FUNC_TRUNC:              return "trunc(?,{0})";
            case SQL_FUNC_CEILING:            return "ceiling(?)";
            case SQL_FUNC_FLOOR:              return "floor(?)";
            case SQL_FUNC_MODULO:             return "((?) % {0})";
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
                log.error("SQL phrase " + String.valueOf(phrase) + " is not defined!");
                return "";
        }
    }

    /**
     * @see DBDatabaseDriver#getConvertPhrase(DataType, DataType, Object)
     */
    @Override
    public String getConvertPhrase(DataType destType, DataType srcType, Object format)
    {
        switch(destType)
        {
           case BOOL:      return "convert(bit, ?)";
           case INTEGER:   return "convert(int, ?)";
           case DECIMAL:   return "convert(decimal, ?)";
           case FLOAT:     return "convert(float, ?)";
           case DATE:      return "convert(date, ?, 111)";
           case DATETIME:
           case TIMESTAMP: return isUseDateTime2() ? "convert(datetime2, ?, 121)"
                                                   : "convert(datetime,  ?, 121)";
           // Convert to text
           case VARCHAR:
           case CHAR:
           case CLOB:
                // Date-Time-Format "YYYY-MM-DD"
                if (srcType==DataType.DATE)
                    return "convert(nvarchar, ?, 111)";
                // Date-Time-Format "YYYY-MM-DD hh.mm.ss"
                if (srcType==DataType.DATETIME || srcType==DataType.TIMESTAMP)
                    return "convert(nvarchar, ?, 120)";
                // other
                return "convert(nvarchar, ?)";
           case BLOB:
                return "convert(varbinary, ?)";
           // Unknown Type                                       
           default:
               	log.error("getConvertPhrase: unknown type " + destType);
                return "?";
        }
    }

    /**
     * @see DBDatabaseDriver#getNextSequenceValue(DBDatabase, String, int, Connection)
     */
    @Override
    public Object getNextSequenceValue(DBDatabase db, String seqName, int minValue, Connection conn)
    { 	
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
     * @see DBDatabaseDriver#getNextSequenceValueExpr(DBTableColumn col)
     */
    @Override
    public DBColumnExpr getNextSequenceValueExpr(DBTableColumn column)
    {
        if (isUseSequenceTable())
            throw new NotSupportedException(this, "getNextSequenceValueExpr");
        // automatic identity management
        return null;
    }

    /**
     * @see DBDatabaseDriver#getSQLTextString(DataType type, Object value)
     */
    @Override
    protected String getSQLTextString(DataType type, Object value)
    {
        StringBuilder valBuf = new StringBuilder();
        // for SQLSERVER utf8 support, see EMPIREDB-122
        valBuf.append((useUnicodePrefix) ? "N'" : "'");
        if (DBDatabase.EMPTY_STRING.equals(value)==false)
            appendSQLTextValue(valBuf, value.toString());
        valBuf.append("'");
        return valBuf.toString();
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
     * Returns an auto-generated value for a particular column
     * 
     * @param db the database
     * @param column the column for which a value is required
     * @param conn a valid database connection
     * @return the auto-generated value
     */
    @Override
    public Object getColumnAutoValue(DBDatabase db, DBTableColumn column, Connection conn)
    {
        // Supports sequences?
        if (column.getDataType()==DataType.UNIQUEID)
        {
            return querySingleValue("select newid()", null, DataType.UNIQUEID, conn);
        }
        return super.getColumnAutoValue(db, column, conn);
    }
    
    /**
     * Adds special behaviour for Timestamp columns with are declared as DATETIME
     */
    @Override
    protected void addStatementParam(PreparedStatement pstmt, int paramIndex, Object value)
        throws SQLException
    {
        if ((value instanceof Timestamp) && !this.isUseDateTime2()) 
        {   /*
             * For compatibility with databases using DATETIME instead of DATETIME2:
             * For constraints to work, the nanoseconds part must be reduced to milliseconds
             * otherwise the comparison with existing database values will fail. 
             */
            Timestamp ts = (Timestamp)value;
            if (ts.getNanos()!=0)
            {   // special
                String tsAsString = ts.toString();
                int nano = tsAsString.lastIndexOf('.');
                int milliLength = nano+4;
                if (nano>0 && tsAsString.length()>milliLength)
                    tsAsString = tsAsString.substring(0, milliLength);
                // Sets timestamp as string with Milliseconds only
                pstmt.setObject(paramIndex, tsAsString);
            }
            else
            {   // Sets the timestamp as provided
                pstmt.setTimestamp(paramIndex, ts);
            }
        }
        else
        {   // Default handling
            super.addStatementParam(pstmt, paramIndex, value);
        }
    }
    
    /**
     * @see DBDatabaseDriver#getDDLScript(DBCmdType, DBObject, DBSQLScript)  
     */
    @Override
    public void getDDLScript(DBCmdType type, DBObject dbo, DBSQLScript script)
    {
        if (ddlGenerator==null)
            ddlGenerator = new MSSqlDDLGenerator(this);
        // forward request
        ddlGenerator.getDDLScript(type, dbo, script); 
    }

    /**
     * @see DBDatabaseDriver#addEnableRelationStmt(DBRelation, boolean, DBSQLScript)  
     */
    @Override
    public void addEnableRelationStmt(DBRelation r, boolean enable, DBSQLScript script)
    {
        // ALTER TABLE {table.name} {CHECK|NOCHECK} CONSTRAINT {relation.name}
        StringBuilder b = new StringBuilder();
        b.append("ALTER TABLE ");
        r.getForeignKeyTable().addSQL(b, DBExpr.CTX_FULLNAME);
        b.append(enable ? " CHECK " : " NOCHECK ");
        b.append("CONSTRAINT ");
        b.append(r.getName());
        // add
        script.addStmt(b);
    }

}
