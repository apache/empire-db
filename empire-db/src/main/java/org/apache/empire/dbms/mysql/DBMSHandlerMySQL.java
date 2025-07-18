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
package org.apache.empire.dbms.mysql;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.GregorianCalendar;

import org.apache.empire.commons.StringUtils;
import org.apache.empire.data.DataType;
import org.apache.empire.db.DBColumnExpr;
import org.apache.empire.db.DBCombinedCmd;
import org.apache.empire.db.DBCommand;
import org.apache.empire.db.DBCommandExpr;
import org.apache.empire.db.DBDDLGenerator;
import org.apache.empire.db.DBDDLGenerator.DDLActionType;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.DBObject;
import org.apache.empire.db.DBRowSet;
import org.apache.empire.db.DBSQLBuilder;
import org.apache.empire.db.DBSQLScript;
import org.apache.empire.db.DBTable;
import org.apache.empire.db.DBTableColumn;
import org.apache.empire.db.exceptions.EmpireSQLException;
import org.apache.empire.dbms.DBMSFeature;
import org.apache.empire.dbms.DBMSHandler;
import org.apache.empire.dbms.DBMSHandlerBase;
import org.apache.empire.dbms.DBSqlPhrase;
import org.apache.empire.exceptions.NotSupportedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class provides support for the MySQL database system.
 * 
 *
 */
public class DBMSHandlerMySQL extends DBMSHandlerBase
{
    // *Deprecated* private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(DBMSHandlerMySQL.class);

    // Additional MySQL Keywords
    protected static final String[] MYSQL_KEYWORDS = new String[] {     
        "ACCESSIBLE", "ACCOUNT", "ACTION", "ADD", "AFTER", "AGAINST", "AGGREGATE", "ALGORITHM", "ALL", "ALTER", "ALWAYS", "ANALYSE", "ANALYZE", "AND", "ANY", "AS", "ASC", "ASCII", 
        "ASENSITIVE", "AT", "AUTOEXTEND_SIZE", "AUTO_INCREMENT", "AVG", "AVG_ROW_LENGTH", "BACKUP", "BEFORE", "BEGIN", "BETWEEN", "BIGINT", "BINARY", "BINLOG", "BIT", "BLOB", "BLOCK", 
        "BOOL", "BOOLEAN", "BOTH", "BTREE", "BY", "BYTE", "CACHE", "CALL", "CASCADE", "CASCADED", "CASE", "CATALOG_NAME", "CHAIN", "CHANGE", "CHANGED", "CHANNEL", "CHAR", "CHARACTER", 
        "CHARSET", "CHECK", "CHECKSUM", "CIPHER", "CLASS_ORIGIN", "CLIENT", "CLOSE", "COALESCE", "CODE", "COLLATE", "COLLATION", "COLUMN", "COLUMNS", "COLUMN_FORMAT", "COLUMN_NAME", 
        "COMMENT", "COMMIT", "COMMITTED", "COMPACT", "COMPLETION", "COMPRESSED", "COMPRESSION", "CONCURRENT", "CONDITION", "CONNECTION", "CONSISTENT", "CONSTRAINT", "CONSTRAINT_CATALOG", 
        "CONSTRAINT_NAME", "CONSTRAINT_SCHEMA", "CONTAINS", "CONTEXT", "CONTINUE", "CONVERT", "CPU", "CREATE", "CROSS", "CUBE", "CURRENT", "CURRENT_DATE", "CURRENT_TIME", "CURRENT_TIMESTAMP", 
        "CURRENT_USER", "CURSOR", "CURSOR_NAME", "DATA", "DATABASE", "DATABASES", "DATAFILE", "DATE", "DATETIME", "DAY", "DAY_HOUR", "DAY_MICROSECOND", "DAY_MINUTE", "DAY_SECOND", 
        "DEALLOCATE", "DEC", "DECIMAL", "DECLARE", "DEFAULT", "DEFAULT_AUTH", "DEFINER", "DELAYED", "DELAY_KEY_WRITE", "DELETE", "DESC", "DESCRIBE", "DES_KEY_FILE", "DETERMINISTIC", 
        "DIAGNOSTICS", "DIRECTORY", "DISABLE", "DISCARD", "DISK", "DISTINCT", "DISTINCTROW", "DIV", "DO", "DOUBLE", "DROP", "DUAL", "DUMPFILE", "DUPLICATE", "DYNAMIC", "EACH", "ELSE", 
        "ELSEIF", "ENABLE", "ENCLOSED", "ENCRYPTION", "END", "ENDS", "ENGINE", "ENGINES", "ENUM", "ERROR", "ERRORS", "ESCAPE", "ESCAPED", "EVENT", "EVENTS", "EVERY", "EXCHANGE", "EXECUTE", 
        "EXISTS", "EXIT", "EXPANSION", "EXPIRE", "EXPLAIN", "EXPORT", "EXTENDED", "EXTENT_SIZE", "FALSE", "FAST", "FAULTS", "FETCH", "FIELDS", "FILE", "FILE_BLOCK_SIZE", "FILTER", "FIRST", 
        "FIXED", "FLOAT", "FLOAT4", "FLOAT8", "FLUSH", "FOLLOWS", "FOR", "FORCE", "FOREIGN", "FORMAT", "FOUND", "FROM", "FULL", "FULLTEXT", "FUNCTION", "GENERAL", "GENERATED", "GEOMETRY", 
        "GEOMETRYCOLLECTION", "GET", "GET_FORMAT", "GLOBAL", "GRANT", "GRANTS", "GROUP", "GROUP_REPLICATION", "HANDLER", "HASH", "HAVING", "HELP", "HIGH_PRIORITY", "HOST", "HOSTS", "HOUR", 
        "HOUR_MICROSECOND", "HOUR_MINUTE", "HOUR_SECOND", "IDENTIFIED", "IF", "IGNORE", "IGNORE_SERVER_IDS", "IMPORT", "IN", "INDEX", "INDEXES", "INFILE", "INITIAL_SIZE", "INNER", "INOUT", 
        "INSENSITIVE", "INSERT", "INSERT_METHOD", "INSTALL", "INSTANCE", "INT", "INT1", "INT2", "INT3", "INT4", "INT8", "INTEGER", "INTERVAL", "INTO", "INVOKER", "IO", "IO_AFTER_GTIDS", 
        "IO_BEFORE_GTIDS", "IO_THREAD", "IPC", "IS", "ISOLATION", "ISSUER", "ITERATE", "JOIN", "JSON", "KEY", "KEYS", "KEY_BLOCK_SIZE", "KILL", "LANGUAGE", "LAST", "LEADING", "LEAVE", "LEAVES", 
        "LEFT", "LESS", "LEVEL", "LIKE", "LIMIT", "LINEAR", "LINES", "LINESTRING", "LIST", "LOAD", "LOCAL", "LOCALTIME", "LOCALTIMESTAMP", "LOCK", "LOCKS", "LOGFILE", "LOGS", "LONG", "LONGBLOB", 
        "LONGTEXT", "LOOP", "LOW_PRIORITY", "MASTER", "MASTER_AUTO_POSITION", "MASTER_BIND", "MASTER_CONNECT_RETRY", "MASTER_DELAY", "MASTER_HEARTBEAT_PERIOD", "MASTER_HOST", "MASTER_LOG_FILE", 
        "MASTER_LOG_POS", "MASTER_PASSWORD", "MASTER_PORT", "MASTER_RETRY_COUNT", "MASTER_SERVER_ID", "MASTER_SSL", "MASTER_SSL_CA", "MASTER_SSL_CAPATH", "MASTER_SSL_CERT", "MASTER_SSL_CIPHER", 
        "MASTER_SSL_CRL", "MASTER_SSL_CRLPATH", "MASTER_SSL_KEY", "MASTER_SSL_VERIFY_SERVER_CERT", "MASTER_TLS_VERSION", "MASTER_USER", "MATCH", "MAXVALUE", "MAX_CONNECTIONS_PER_HOUR", 
        "MAX_QUERIES_PER_HOUR", "MAX_ROWS", "MAX_SIZE", "MAX_STATEMENT_TIME", "MAX_UPDATES_PER_HOUR", "MAX_USER_CONNECTIONS", "MEDIUM", "MEDIUMBLOB", "MEDIUMINT", "MEDIUMTEXT", "MEMORY", "MERGE", 
        "MESSAGE_TEXT", "MICROSECOND", "MIDDLEINT", "MIGRATE", "MINUTE", "MINUTE_MICROSECOND", "MINUTE_SECOND", "MIN_ROWS", "MOD", "MODE", "MODIFIES", "MODIFY", "MONTH", "MULTILINESTRING", 
        "MULTIPOINT", "MULTIPOLYGON", "MUTEX", "MYSQL_ERRNO", "NAME", "NAMES", "NATIONAL", "NATURAL", "NCHAR", "NDB", "NDBCLUSTER", "NEVER", "NEW", "NEXT", "NO", "NODEGROUP", "NONBLOCKING", "NONE", 
        "NOT", "NO_WAIT", "NO_WRITE_TO_BINLOG", "NULL", "NUMBER", "NUMERIC", "NVARCHAR", "OFFSET", "OLD_PASSWORD", "ON", "ONE", "ONLY", "OPEN", "OPTIMIZE", "OPTIMIZER_COSTS", "OPTION", "OPTIONALLY", 
        "OPTIONS", "OR", "ORDER", "OUT", "OUTER", "OUTFILE", "OWNER", "PACK_KEYS", "PAGE", "PARSER", "PARSE_GCOL_EXPR", "PARTIAL", "PARTITION", "PARTITIONING", "PARTITIONS", "PASSWORD", "PHASE", 
        "PLUGIN", "PLUGINS", "PLUGIN_DIR", "POINT", "POLYGON", "PORT", "PRECEDES", "PRECISION", "PREPARE", "PRESERVE", "PREV", "PRIMARY", "PRIVILEGES", "PROCEDURE", "PROCESSLIST", "PROFILE", 
        "PROFILES", "PROXY", "PURGE", "QUARTER", "QUERY", "QUICK", "RANGE", "READ", "READS", "READ_ONLY", "READ_WRITE", "REAL", "REBUILD", "RECOVER", "REDOFILE", "REDO_BUFFER_SIZE", "REDUNDANT", 
        "REFERENCES", "REGEXP", "RELAY", "RELAYLOG", "RELAY_LOG_FILE", "RELAY_LOG_POS", "RELAY_THREAD", "RELEASE", "RELOAD", "REMOVE", "RENAME", "REORGANIZE", "REPAIR", "REPEAT", "REPEATABLE", 
        "REPLACE", "REPLICATE_DO_DB", "REPLICATE_DO_TABLE", "REPLICATE_IGNORE_DB", "REPLICATE_IGNORE_TABLE", "REPLICATE_REWRITE_DB", "REPLICATE_WILD_DO_TABLE", "REPLICATE_WILD_IGNORE_TABLE", 
        "REPLICATION", "REQUIRE", "RESET", "RESIGNAL", "RESTORE", "RESTRICT", "RESUME", "RETURN", "RETURNED_SQLSTATE", "RETURNS", "REVERSE", "REVOKE", "RIGHT", "RLIKE", "ROLLBACK", "ROLLUP", 
        "ROTATE", "ROUTINE", "ROW", "ROWS", "ROW_COUNT", "ROW_FORMAT", "RTREE", "SAVEPOINT", "SCHEDULE", "SCHEMA", "SCHEMAS", "SCHEMA_NAME", "SECOND", "SECOND_MICROSECOND", "SECURITY", "SELECT", 
        "SENSITIVE", "SEPARATOR", "SERIAL", "SERIALIZABLE", "SERVER", "SESSION", "SET", "SHARE", "SHOW", "SHUTDOWN", "SIGNAL", "SIGNED", "SIMPLE", "SLAVE", "SLOW", "SMALLINT", "SNAPSHOT", "SOCKET", 
        "SOME", "SONAME", "SOUNDS", "SOURCE", "SPATIAL", "SPECIFIC", "SQL", "SQLEXCEPTION", "SQLSTATE", "SQLWARNING", "SQL_AFTER_GTIDS", "SQL_AFTER_MTS_GAPS", "SQL_BEFORE_GTIDS", "SQL_BIG_RESULT", 
        "SQL_BUFFER_RESULT", "SQL_CACHE", "SQL_CALC_FOUND_ROWS", "SQL_NO_CACHE", "SQL_SMALL_RESULT", "SQL_THREAD", "SQL_TSI_DAY", "SQL_TSI_HOUR", "SQL_TSI_MINUTE", "SQL_TSI_MONTH", "SQL_TSI_QUARTER", 
        "SQL_TSI_SECOND", "SQL_TSI_WEEK", "SQL_TSI_YEAR", "SSL", "STACKED", "START", "STARTING", "STARTS", "STATS_AUTO_RECALC", "STATS_PERSISTENT", "STATS_SAMPLE_PAGES", "STATUS", "STOP", "STORAGE", 
        "STORED", "STRAIGHT_JOIN", "STRING", "SUBCLASS_ORIGIN", "SUBJECT", "SUBPARTITION", "SUBPARTITIONS", "SUPER", "SUSPEND", "SWAPS", "SWITCHES", "TABLE", "TABLES", "TABLESPACE", "TABLE_CHECKSUM", 
        "TABLE_NAME", "TEMPORARY", "TEMPTABLE", "TERMINATED", "TEXT", "THAN", "THEN", "TIME", "TIMESTAMP", "TIMESTAMPADD", "TIMESTAMPDIFF", "TINYBLOB", "TINYINT", "TINYTEXT", "TO", "TRAILING", 
        "TRANSACTION", "TRIGGER", "TRIGGERS", "TRUE", "TRUNCATE", "TYPE", "TYPES", "UNCOMMITTED", "UNDEFINED", "UNDO", "UNDOFILE", "UNDO_BUFFER_SIZE", "UNICODE", "UNINSTALL", "UNION", "UNIQUE", 
        "UNKNOWN", "UNLOCK", "UNSIGNED", "UNTIL", "UPDATE", "UPGRADE", "USAGE", "USE", "USER", "USER_RESOURCES", "USE_FRM", "USING", "UTC_DATE", "UTC_TIME", "UTC_TIMESTAMP", "VALIDATION", "VALUE", 
        "VALUES", "VARBINARY", "VARCHAR", "VARCHARACTER", "VARIABLES", "VARYING", "VIEW", "VIRTUAL", "WAIT", "WARNINGS", "WEEK", "WEIGHT_STRING", "WHEN", "WHERE", "WHILE", "WITH", "WITHOUT", "WORK", 
        "WRAPPER", "WRITE", "X509", "XA", "XID", "XML", "XOR", "YEAR", "YEAR_MONTH", "ZEROFILL"
    };
    
    /**
     * Defines the MySQL command type.
     */ 
    public static class DBCommandMySQL extends DBCommand
    {
        // *Deprecated* private static final long serialVersionUID = 1L;
      
        protected int limit = -1;
        protected int skip  = -1;
        
        public DBCommandMySQL(DBMSHandlerMySQL dbms, boolean autoPrepareStmt)
        {
            super(dbms, autoPrepareStmt);
        }
        
        @Override
        public DBCommand limitRows(int numRows)
        {
            limit = numRows;
            return this;
        }

        @Override
        public DBCommand skipRows(int numRows)
        {
            skip = numRows;
            return this;
        }
         
        @Override
        public void clearLimit()
        {
            limit = -1;
            skip  = -1;
        }
        
        @Override
        public void getSelect(DBSQLBuilder sql, int flags)
        {   // call base class
            super.getSelect(sql, flags);
            // add limit and offset
            if (limit>=0 && not(flags, SF_SKIP_LIMIT))
            {   sql.append("\r\nLIMIT ");
                sql.append(String.valueOf(limit));
                // Offset
                if (skip>=0) 
                {   sql.append(" OFFSET ");
                    sql.append(String.valueOf(skip));
                }    
            }
        }
        
        @Override
        protected void addDeleteWithJoins(DBSQLBuilder sql, DBRowSet table)
        {
            // DELETE with Multiple-Table Syntax
            // http://dev.mysql.com/doc/refman/5.7/en/delete.html
            sql.append(table.getAlias());
            addFrom(sql);
            addWhere(sql);
        }
    }
    
    /**
     * Provides a DBSQLBuilder implementation for MySQL
     */
    public static class DBSQLBuilderMySQL extends DBSQLBuilder 
    {
        public DBSQLBuilderMySQL(DBMSHandlerMySQL dbms)
        {
            super(dbms);
        }
        
        @Override
        protected void escapeAndAppendLiteral(String value)
        {
            if (value.indexOf('\'') >= 0 || value.indexOf('\\') >= 0)
            {
                int len = value.length();
                for (int i = 0; i < len; i++)
                {
                    if (value.charAt(i) == '\'')
                    {   // a routine to double up single quotes for SQL
                        sql.append("''");
                    }
                    else if (value.charAt(i) == '\\')
                    {   // a routine to double up backslashes for MySQL
                        sql.append("\\\\");
                    } 
                    else
                    {   // normal
                        sql.append(value.charAt(i));
                    }
                }
            } else {
                sql.append(value);
            }
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
     * Constructor for the MySQL database dbms.<br>
     */
    public DBMSHandlerMySQL()
    {
        // Add additional Keywords
        super(MYSQL_KEYWORDS);
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
     * When a name is set, the dbms will automatically execute 'USE dbname' when the database is opened.
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
     * @see org.apache.empire.db.DBMSHandler#openDatabase(org.apache.empire.db.DBDatabase, java.sql.Connection)
     */
    @SuppressWarnings("unused")
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
    public DBCommand createCommand(boolean autoPrepareStmt)
    {
        // create command object
        return new DBCommandMySQL(this, autoPrepareStmt);
    }

    /**
     * Creates a new MySQL SQL-Builder.
     * @return the new DBSQLBuilder object
     */
    @Override
    public DBSQLBuilder createSQLBuilder()
    {
        return new DBSQLBuilderMySQL(this);
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
			// *Deprecated* private static final long serialVersionUID = 1L;
			protected int limit = -1;
            protected int skip  = -1;
            @Override
            public DBCommandExpr limitRows(int numRows)
            {
                limit = numRows;
                return this;
            }
            @Override
            public DBCommandExpr skipRows(int numRows)
            {
                skip = numRows;
                return this;
            }
            @Override
            public void clearLimit()
            {
                limit = -1;
                skip  = -1;
            }
            @Override
            public void getSelect(DBSQLBuilder sql, int flags)
            {   // Prepares statement
            	super.getSelect(sql, flags);
                // add limit and offset
                if (limit>=0 && not(flags, SF_SKIP_LIMIT))
                {   sql.append("\r\nLIMIT ");
                    sql.append(String.valueOf(limit));
                    // Offset
                    if (skip>=0) 
                    {   sql.append(" OFFSET ");
                        sql.append(String.valueOf(skip));
                    }    
                }
            }
    	};
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
            case SQL_QUOTES_OPEN:             return "`";
            case SQL_QUOTES_CLOSE:            return "`";
            case SQL_CONCAT_EXPR:             return "concat(?, {0})";
            // data types
            case SQL_BOOLEAN_TRUE:            return "1";
            case SQL_BOOLEAN_FALSE:           return "0";
            case SQL_CURRENT_DATE:            return "CURRENT_DATE()";
            case SQL_DATE_TEMPLATE:           return "STR_TO_DATE('{0}','%Y-%m-%d')";
            case SQL_CURRENT_TIME:            return "CURRENT_TIME()";
            case SQL_TIME_TEMPLATE:           return "STR_TO_DATE('{0}','%H:%i:%s')";
            case SQL_CURRENT_DATETIME:        return "CURRENT_TIMESTAMP()";
            case SQL_DATETIME_TEMPLATE:       return "STR_TO_DATE('{0}','%Y-%m-%d %H:%i:%s')";
            case SQL_CURRENT_TIMESTAMP:       return "CURRENT_TIMESTAMP()";
            case SQL_TIMESTAMP_TEMPLATE:      return "STR_TO_DATE('{0}','%Y-%m-%d %H:%i:%s.%f')";
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
            case SQL_FUNC_ESCAPE:             return "? escape {0:VARCHAR}";
            // Numeric
            case SQL_FUNC_ABS:                return "abs(?)";
            case SQL_FUNC_ROUND:              return "round(?,{0})";
            case SQL_FUNC_TRUNC:              return "truncate(?,{0})";
            case SQL_FUNC_CEILING:            return "ceiling(?)";
            case SQL_FUNC_FLOOR:              return "floor(?)";
            case SQL_FUNC_MOD:                return "mod(?,{0})";
            case SQL_FUNC_FORMAT:             return "format(?, {0:INTEGER})"; /* TODO: supports only decimal places. Add support for a format string */
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
           case TIMESTAMP: return "CAST(? AS DATETIME)";
           // Convert to text
           case VARCHAR:
           case CHAR:
               if (format != null)
               { // Convert using a format string
                   return "CAST(? AS CHAR " + format.toString() + ")";
               } else
               {
            	   return "CAST(? AS CHAR)";
               }
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
            ddlGenerator = new MySQLDDLGenerator(this);
        // forward request
        ddlGenerator.getDDLScript(type, dbo, script); 
    }

    
}
