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
package org.apache.empire.db.mysql;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.GregorianCalendar;

import org.apache.empire.commons.StringUtils;
import org.apache.empire.data.DataType;
import org.apache.empire.db.DBCmdType;
import org.apache.empire.db.DBCombinedCmd;
import org.apache.empire.db.DBCommand;
import org.apache.empire.db.DBCommandExpr;
import org.apache.empire.db.DBDDLGenerator;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.DBDatabaseDriver;
import org.apache.empire.db.DBDriverFeature;
import org.apache.empire.db.DBObject;
import org.apache.empire.db.DBSQLScript;
import org.apache.empire.db.DBTable;
import org.apache.empire.db.exceptions.EmpireSQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class provides support for the MySQL database system.
 * 
 *
 */
public class DBDatabaseDriverMySQL extends DBDatabaseDriver
{
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(DBDatabaseDriverMySQL.class);

    /**
     * Defines the MySQL command type.
     */ 
    public static class DBCommandMySQL extends DBCommand
    {
        private final static long serialVersionUID = 1L;
      
        protected int limit = -1;
        protected int skip  = -1;
        
        public DBCommandMySQL(DBDatabase db)
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
        
        /**
         * Creates an MySQL specific delete statement.
         * @return the delete SQL-Command
         */
        @Override
        public synchronized String getDelete(DBTable table)
        {
        	if (joins == null) {
        		// Default
        		return super.getDelete(table);
        	}
        	
        	// DELETE with Multiple-Table Syntax
        	// http://dev.mysql.com/doc/refman/5.7/en/delete.html
            resetParamUsage();
            StringBuilder buf = new StringBuilder("DELETE ");
            buf.append(table.getAlias());
            addFrom(buf);
            addWhere(buf);
            return buf.toString();
        }
    }
    
    // Properties
    private String databaseName = null;
    private String characterSet = "utf8";

    // Sequence treatment
    // When set to 'false' (default) MySQL's autoincrement feature is used.
    private boolean useSequenceTable = false;
    private String sequenceTableName = "Sequences";
    private String engine; // The database engine to use when creating new tables

    private DBDDLGenerator<?> ddlGenerator = null; // lazy creation
    
    /**
     * Constructor for the MySQL database driver.<br>
     */
    public DBDatabaseDriverMySQL()
    {
        // Default Constructor
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
        // http://dev.mysql.com/doc/refman/5.5/en/reserved-words.html
    	addReservedKeyWord("ACCESSIBLE".toLowerCase());
    	addReservedKeyWord("ADD".toLowerCase());
    	addReservedKeyWord("ALL".toLowerCase());
    	addReservedKeyWord("ALTER".toLowerCase());
    	addReservedKeyWord("ANALYZE".toLowerCase());
    	addReservedKeyWord("AND".toLowerCase());
    	addReservedKeyWord("AS".toLowerCase());
    	addReservedKeyWord("ASC".toLowerCase());
    	addReservedKeyWord("ASENSITIVE".toLowerCase());
    	addReservedKeyWord("BEFORE".toLowerCase());
    	addReservedKeyWord("BETWEEN".toLowerCase());
    	addReservedKeyWord("BIGINT".toLowerCase());
    	addReservedKeyWord("BINARY".toLowerCase());
    	addReservedKeyWord("BLOB".toLowerCase());
    	addReservedKeyWord("BOTH".toLowerCase());
    	addReservedKeyWord("BY".toLowerCase());
    	addReservedKeyWord("CALL".toLowerCase());
    	addReservedKeyWord("CASCADE".toLowerCase());
    	addReservedKeyWord("CASE".toLowerCase());
    	addReservedKeyWord("CHANGE".toLowerCase());
    	addReservedKeyWord("CHAR".toLowerCase());
    	addReservedKeyWord("CHARACTER".toLowerCase());
    	addReservedKeyWord("CHECK".toLowerCase());
    	addReservedKeyWord("COLLATE".toLowerCase());
    	addReservedKeyWord("COLUMN".toLowerCase());
    	addReservedKeyWord("CONDITION".toLowerCase());
    	addReservedKeyWord("CONSTRAINT".toLowerCase());
    	addReservedKeyWord("CONTINUE".toLowerCase());
    	addReservedKeyWord("CONVERT".toLowerCase());
    	addReservedKeyWord("CREATE".toLowerCase());
    	addReservedKeyWord("CROSS".toLowerCase());
    	addReservedKeyWord("CURRENT_DATE".toLowerCase());
    	addReservedKeyWord("CURRENT_TIME".toLowerCase());
    	addReservedKeyWord("CURRENT_TIMESTAMP".toLowerCase());
    	addReservedKeyWord("CURRENT_USER".toLowerCase());
    	addReservedKeyWord("CURSOR".toLowerCase());
    	addReservedKeyWord("DATABASE".toLowerCase());
    	addReservedKeyWord("DATABASES".toLowerCase());
    	addReservedKeyWord("DAY_HOUR".toLowerCase());
    	addReservedKeyWord("DAY_MICROSECOND".toLowerCase());
    	addReservedKeyWord("DAY_MINUTE".toLowerCase());
    	addReservedKeyWord("DAY_SECOND".toLowerCase());
    	addReservedKeyWord("DEC".toLowerCase());
    	addReservedKeyWord("DECIMAL".toLowerCase());
    	addReservedKeyWord("DECLARE".toLowerCase());
    	addReservedKeyWord("DEFAULT".toLowerCase());
    	addReservedKeyWord("DELAYED".toLowerCase());
    	addReservedKeyWord("DELETE".toLowerCase());
    	addReservedKeyWord("DESC".toLowerCase());
    	addReservedKeyWord("DESCRIBE".toLowerCase());
    	addReservedKeyWord("DETERMINISTIC".toLowerCase());
    	addReservedKeyWord("DISTINCT".toLowerCase());
    	addReservedKeyWord("DISTINCTROW".toLowerCase());
    	addReservedKeyWord("DIV".toLowerCase());
    	addReservedKeyWord("DOUBLE".toLowerCase());
    	addReservedKeyWord("DROP".toLowerCase());
    	addReservedKeyWord("DUAL".toLowerCase());
    	addReservedKeyWord("EACH".toLowerCase());
    	addReservedKeyWord("ELSE".toLowerCase());
    	addReservedKeyWord("ELSEIF".toLowerCase());
    	addReservedKeyWord("ENCLOSED".toLowerCase());
    	addReservedKeyWord("ESCAPED".toLowerCase());
    	addReservedKeyWord("EXISTS".toLowerCase());
    	addReservedKeyWord("EXIT".toLowerCase());
    	addReservedKeyWord("EXPLAIN".toLowerCase());
    	addReservedKeyWord("FALSE".toLowerCase());
    	addReservedKeyWord("FETCH".toLowerCase());
    	addReservedKeyWord("FLOAT".toLowerCase());
    	addReservedKeyWord("FLOAT4".toLowerCase());
    	addReservedKeyWord("FLOAT8".toLowerCase());
    	addReservedKeyWord("FOR".toLowerCase());
    	addReservedKeyWord("FORCE".toLowerCase());
    	addReservedKeyWord("FOREIGN".toLowerCase());
    	addReservedKeyWord("FROM".toLowerCase());
    	addReservedKeyWord("FULLTEXT".toLowerCase());
    	addReservedKeyWord("GRANT".toLowerCase());
    	addReservedKeyWord("GROUP".toLowerCase());
    	addReservedKeyWord("HAVING".toLowerCase());
    	addReservedKeyWord("HIGH_PRIORITY".toLowerCase());
    	addReservedKeyWord("HOUR_MICROSECOND".toLowerCase());
    	addReservedKeyWord("HOUR_MINUTE".toLowerCase());
    	addReservedKeyWord("HOUR_SECOND".toLowerCase());
    	addReservedKeyWord("IF".toLowerCase());
    	addReservedKeyWord("IGNORE".toLowerCase());
    	addReservedKeyWord("IN".toLowerCase());
    	addReservedKeyWord("INDEX".toLowerCase());
    	addReservedKeyWord("INFILE".toLowerCase());
    	addReservedKeyWord("INNER".toLowerCase());
    	addReservedKeyWord("INOUT".toLowerCase());
    	addReservedKeyWord("INSENSITIVE".toLowerCase());
    	addReservedKeyWord("INSERT".toLowerCase());
    	addReservedKeyWord("INT".toLowerCase());
    	addReservedKeyWord("INT1".toLowerCase());
    	addReservedKeyWord("INT2".toLowerCase());
    	addReservedKeyWord("INT3".toLowerCase());
    	addReservedKeyWord("INT4".toLowerCase());
    	addReservedKeyWord("INT8".toLowerCase());
    	addReservedKeyWord("INTEGER".toLowerCase());
    	addReservedKeyWord("INTERVAL".toLowerCase());
    	addReservedKeyWord("INTO".toLowerCase());
    	addReservedKeyWord("IS".toLowerCase());
    	addReservedKeyWord("ITERATE".toLowerCase());
    	addReservedKeyWord("JOIN".toLowerCase());
    	addReservedKeyWord("KEY".toLowerCase());
    	addReservedKeyWord("KEYS".toLowerCase());
    	addReservedKeyWord("KILL".toLowerCase());
    	addReservedKeyWord("LEADING".toLowerCase());
    	addReservedKeyWord("LEAVE".toLowerCase());
    	addReservedKeyWord("LEFT".toLowerCase());
    	addReservedKeyWord("LIKE".toLowerCase());
    	addReservedKeyWord("LIMIT".toLowerCase());
    	addReservedKeyWord("LINEAR".toLowerCase());
    	addReservedKeyWord("LINES".toLowerCase());
    	addReservedKeyWord("LOAD".toLowerCase());
    	addReservedKeyWord("LOCALTIME".toLowerCase());
    	addReservedKeyWord("LOCALTIMESTAMP".toLowerCase());
    	addReservedKeyWord("LOCK".toLowerCase());
    	addReservedKeyWord("LONG".toLowerCase());
    	addReservedKeyWord("LONGBLOB".toLowerCase());
    	addReservedKeyWord("LONGTEXT".toLowerCase());
    	addReservedKeyWord("LOOP".toLowerCase());
    	addReservedKeyWord("LOW_PRIORITY".toLowerCase());
    	addReservedKeyWord("MASTER_SSL_VERIFY_SERVER_CERT".toLowerCase());
    	addReservedKeyWord("MATCH".toLowerCase());
    	addReservedKeyWord("MAXVALUE".toLowerCase());
    	addReservedKeyWord("MEDIUMBLOB".toLowerCase());
    	addReservedKeyWord("MEDIUMINT".toLowerCase());
    	addReservedKeyWord("MEDIUMTEXT".toLowerCase());
    	addReservedKeyWord("MIDDLEINT".toLowerCase());
    	addReservedKeyWord("MINUTE_MICROSECOND".toLowerCase());
    	addReservedKeyWord("MINUTE_SECOND".toLowerCase());
    	addReservedKeyWord("MOD".toLowerCase());
    	addReservedKeyWord("MODIFIES".toLowerCase());
    	addReservedKeyWord("NATURAL".toLowerCase());
    	addReservedKeyWord("NOT".toLowerCase());
    	addReservedKeyWord("NO_WRITE_TO_BINLOG".toLowerCase());
    	addReservedKeyWord("NULL".toLowerCase());
    	addReservedKeyWord("NUMERIC".toLowerCase());
    	addReservedKeyWord("ON".toLowerCase());
    	addReservedKeyWord("OPTIMIZE".toLowerCase());
    	addReservedKeyWord("OPTION".toLowerCase());
    	addReservedKeyWord("OPTIONALLY".toLowerCase());
    	addReservedKeyWord("OR".toLowerCase());
    	addReservedKeyWord("ORDER".toLowerCase());
    	addReservedKeyWord("OUT".toLowerCase());
    	addReservedKeyWord("OUTER".toLowerCase());
    	addReservedKeyWord("OUTFILE".toLowerCase());
    	addReservedKeyWord("PRECISION".toLowerCase());
    	addReservedKeyWord("PRIMARY".toLowerCase());
    	addReservedKeyWord("PROCEDURE".toLowerCase());
    	addReservedKeyWord("PURGE".toLowerCase());
    	addReservedKeyWord("RANGE".toLowerCase());
    	addReservedKeyWord("READ".toLowerCase());
    	addReservedKeyWord("READS".toLowerCase());
    	addReservedKeyWord("READ_WRITE".toLowerCase());
    	addReservedKeyWord("REAL".toLowerCase());
    	addReservedKeyWord("REFERENCES".toLowerCase());
    	addReservedKeyWord("REGEXP".toLowerCase());
    	addReservedKeyWord("RELEASE".toLowerCase());
    	addReservedKeyWord("RENAME".toLowerCase());
    	addReservedKeyWord("REPEAT".toLowerCase());
    	addReservedKeyWord("REPLACE".toLowerCase());
    	addReservedKeyWord("REQUIRE".toLowerCase());
    	addReservedKeyWord("RESIGNAL".toLowerCase());
    	addReservedKeyWord("RESTRICT".toLowerCase());
    	addReservedKeyWord("RETURN".toLowerCase());
    	addReservedKeyWord("REVOKE".toLowerCase());
    	addReservedKeyWord("RIGHT".toLowerCase());
    	addReservedKeyWord("RLIKE".toLowerCase());
    	addReservedKeyWord("SCHEMA".toLowerCase());
    	addReservedKeyWord("SCHEMAS".toLowerCase());
    	addReservedKeyWord("SECOND_MICROSECOND".toLowerCase());
    	addReservedKeyWord("SELECT".toLowerCase());
    	addReservedKeyWord("SENSITIVE".toLowerCase());
    	addReservedKeyWord("SEPARATOR".toLowerCase());
    	addReservedKeyWord("SET".toLowerCase());
    	addReservedKeyWord("SHOW".toLowerCase());
    	addReservedKeyWord("SIGNAL".toLowerCase());
    	addReservedKeyWord("SMALLINT".toLowerCase());
    	addReservedKeyWord("SPATIAL".toLowerCase());
    	addReservedKeyWord("SPECIFIC".toLowerCase());
    	addReservedKeyWord("SQL".toLowerCase());
    	addReservedKeyWord("SQLEXCEPTION".toLowerCase());
    	addReservedKeyWord("SQLSTATE".toLowerCase());
    	addReservedKeyWord("SQLWARNING".toLowerCase());
    	addReservedKeyWord("SQL_BIG_RESULT".toLowerCase());
    	addReservedKeyWord("SQL_CALC_FOUND_ROWS".toLowerCase());
    	addReservedKeyWord("SQL_SMALL_RESULT".toLowerCase());
    	addReservedKeyWord("SSL".toLowerCase());
    	addReservedKeyWord("STARTING".toLowerCase());
    	addReservedKeyWord("STRAIGHT_JOIN".toLowerCase());
    	addReservedKeyWord("TABLE".toLowerCase());
    	addReservedKeyWord("TERMINATED".toLowerCase());
    	addReservedKeyWord("THEN".toLowerCase());
    	addReservedKeyWord("TINYBLOB".toLowerCase());
    	addReservedKeyWord("TINYINT".toLowerCase());
    	addReservedKeyWord("TINYTEXT".toLowerCase());
    	addReservedKeyWord("TO".toLowerCase());
    	addReservedKeyWord("TRAILING".toLowerCase());
    	addReservedKeyWord("TRIGGER".toLowerCase());
    	addReservedKeyWord("TRUE".toLowerCase());
    	addReservedKeyWord("UNDO".toLowerCase());
    	addReservedKeyWord("UNION".toLowerCase());
    	addReservedKeyWord("UNIQUE".toLowerCase());
    	addReservedKeyWord("UNLOCK".toLowerCase());
    	addReservedKeyWord("UNSIGNED".toLowerCase());
    	addReservedKeyWord("UPDATE".toLowerCase());
    	addReservedKeyWord("USAGE".toLowerCase());
    	addReservedKeyWord("USE".toLowerCase());
    	addReservedKeyWord("USING".toLowerCase());
    	addReservedKeyWord("UTC_DATE".toLowerCase());
    	addReservedKeyWord("UTC_TIME".toLowerCase());
    	addReservedKeyWord("UTC_TIMESTAMP".toLowerCase());
    	addReservedKeyWord("VALUES".toLowerCase());
    	addReservedKeyWord("VARBINARY".toLowerCase());
    	addReservedKeyWord("VARCHAR".toLowerCase());
    	addReservedKeyWord("VARCHARACTER".toLowerCase());
    	addReservedKeyWord("VARYING".toLowerCase());
    	addReservedKeyWord("WHEN".toLowerCase());
    	addReservedKeyWord("WHERE".toLowerCase());
    	addReservedKeyWord("WHILE".toLowerCase());
    	addReservedKeyWord("WITH".toLowerCase());
    	addReservedKeyWord("WRITE".toLowerCase());
    	addReservedKeyWord("XOR".toLowerCase());
    	addReservedKeyWord("YEAR_MONTH".toLowerCase());
    	addReservedKeyWord("ZEROFILL".toLowerCase());
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

    public String getCharacterSet()
    {
        return characterSet;
    }

    public void setCharacterSet(String characterSet)
    {
        this.characterSet = characterSet;
    }

    /**
     * Get the table engine that is used when creating new tables
     * @return the table engine
     */
    public String getEngine()
    {
        return engine;
    }

    /**
     * Set the table engine that is used when creating new tables
     * @param engine the table engine
     */
    public void setEngine(String engine)
    {
        this.engine = engine;
    }
    
    /**
     * returns whether a sequence table is used for record identity management.<br>
     * Default is false. In this case the AutoIncrement feature of MySQL is used.
     * @return true if a sequence table is used instead of identity columns.
     */
    public boolean isUseSequenceTable()
    {
        return useSequenceTable;
    }

    /**
     * If set to true a special table is used for sequence number generation.<br>
     * Otherwise the AutoIncrement feature of MySQL is used identity fields. 
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

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.empire.db.DBDatabaseDriver#openDatabase(org.apache.empire.db.DBDatabase, java.sql.Connection)
     */
    @Override
    public void attachDatabase(DBDatabase db, Connection conn)
    {
        // Prepare
        try
        {   // Set Database
            if (StringUtils.isNotEmpty(databaseName))
                executeSQL("USE " + databaseName, null, conn, null);
            // Sequence Table
            if (useSequenceTable && db.getTable(sequenceTableName)==null)
                new DBSeqTable(sequenceTableName, db);
            // call Base implementation
            super.attachDatabase(db, conn);
            
        } catch (SQLException e) {
            // throw exception
            throw new EmpireSQLException(this, e);
        }
    }

    /**
     * Creates a new MySQL command object.
     * 
     * @return the new DBCommandMySQL object
     */
    @Override
    public DBCommand createCommand(DBDatabase db)
    {
        if (db == null)
            return null;
        // create command object
        return new DBCommandMySQL(db);
    }

    @Override
    /**
     * Creates a combined command that supports limit() and skip()
	 * @param left the first DBCommandExpr object
	 * @param keyWord the key word between the two DBCommandExpr objects
	 * @param right the second DBCommandExpr object
     * @return the new DBCommandExpr object
     */
    public DBCommandExpr createCombinedCommand(DBCommandExpr left, String keyWord, DBCommandExpr right)
    {
    	// Override CombinedCmd
    	return new DBCombinedCmd(left, keyWord, right) {
			private static final long serialVersionUID = 1L;
			protected int limit = -1;
            protected int skip  = -1;
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
            {   // Prepares statement
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
    	};
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
            case SQL_QUOTES_OPEN:             return "`";
            case SQL_QUOTES_CLOSE:            return "`";
            case SQL_CONCAT_EXPR:             return "concat(?, {0})";
            // data types
            case SQL_BOOLEAN_TRUE:            return "1";
            case SQL_BOOLEAN_FALSE:           return "0";
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
            case SQL_FUNC_REVERSE:            return "reverse(?)"; 
            case SQL_FUNC_STRINDEX:           return "instr(?, {0})"; 
            case SQL_FUNC_STRINDEXFROM:       return "locate({0}, ?, {1})"; 
            case SQL_FUNC_LENGTH:             return "length(?)";
            case SQL_FUNC_UPPER:              return "upper(?)";
            case SQL_FUNC_LOWER:              return "lcase(?)";
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
                return "?";
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
           case BOOL:      return "CAST(? AS UNSIGNED)";
           case INTEGER:   return "CAST(? AS SIGNED)";
           case DECIMAL:   return "CAST(? AS DECIMAL)";
           case FLOAT:     return "CAST(? AS DECIMAL)";
           case DATE:      return "CAST(? AS DATE)";
           case DATETIME:  return "CAST(? AS DATETIME)";
           // Convert to text
           case TEXT:
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
            ddlGenerator = new MySQLDDLGenerator(this);
        // forward request
        ddlGenerator.getDDLScript(type, dbo, script); 
    }

    /** 
     * this helper function doubles up single quotes for SQL 
     */
    @Override
    protected void appendSQLTextValue(StringBuilder buf, String value)
    {
    	boolean escape = false;
        if (value.indexOf('\'') >= 0)
        { // a routine to double up single quotes for SQL
        	escape = true;
            int len = value.length();
            for (int i = 0; i < len; i++)
            {
                if (value.charAt(i) == '\'')
                    buf.append("''");
                else
                    buf.append(value.charAt(i));
            }
        }
        if (value.indexOf('\\') >= 0)
        { // a routine to double up backslashes for MySQL
        	escape = true;
            int len = value.length();
            for (int i = 0; i < len; i++)
            {
                if (value.charAt(i) == '\\')
                    buf.append("\\\\");
                else
                    buf.append(value.charAt(i));
            }
        }
        if (!escape) {
            buf.append(value);
        }
    }
    
}
