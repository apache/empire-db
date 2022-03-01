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
import org.apache.empire.db.DBSQLScript;
import org.apache.empire.db.DBTable;
import org.apache.empire.db.DBTableColumn;
import org.apache.empire.db.exceptions.EmpireSQLException;
import org.apache.empire.dbms.DBMSHandler;
import org.apache.empire.dbms.DBMSHandlerBase;
import org.apache.empire.dbms.DBMSFeature;
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

    /**
     * Defines the MySQL command type.
     */ 
    public static class DBCommandMySQL extends DBCommand
    {
        // *Deprecated* private static final long serialVersionUID = 1L;
      
        protected int limit = -1;
        protected int skip  = -1;
        
        public DBCommandMySQL(boolean autoPrepareStmt)
        {
            super(autoPrepareStmt);
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
        protected void addDeleteWithJoins(StringBuilder buf, DBRowSet table)
        {
            // DELETE with Multiple-Table Syntax
            // http://dev.mysql.com/doc/refman/5.7/en/delete.html
            buf.append(table.getAlias());
            addFrom(buf);
            addWhere(buf);
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
        // https://dev.mysql.com/doc/refman/5.7/en/keywords.html
    	addReservedKeyWord("ACCESSIBLE".toLowerCase());
    	addReservedKeyWord("ACCOUNT".toLowerCase());
    	addReservedKeyWord("ACTION".toLowerCase());
    	addReservedKeyWord("ADD".toLowerCase());
    	addReservedKeyWord("AFTER".toLowerCase());
    	addReservedKeyWord("AGAINST".toLowerCase());
    	addReservedKeyWord("AGGREGATE".toLowerCase());
    	addReservedKeyWord("ALGORITHM".toLowerCase());
    	addReservedKeyWord("ALL".toLowerCase());
    	addReservedKeyWord("ALTER".toLowerCase());
    	addReservedKeyWord("ALWAYS".toLowerCase());
    	addReservedKeyWord("ANALYSE".toLowerCase());
    	addReservedKeyWord("ANALYZE".toLowerCase());
    	addReservedKeyWord("AND".toLowerCase());
    	addReservedKeyWord("ANY".toLowerCase());
    	addReservedKeyWord("AS".toLowerCase());
    	addReservedKeyWord("ASC".toLowerCase());
    	addReservedKeyWord("ASCII".toLowerCase());
    	addReservedKeyWord("ASENSITIVE".toLowerCase());
    	addReservedKeyWord("AT".toLowerCase());
    	addReservedKeyWord("AUTOEXTEND_SIZE".toLowerCase());
    	addReservedKeyWord("AUTO_INCREMENT".toLowerCase());
    	addReservedKeyWord("AVG".toLowerCase());
    	addReservedKeyWord("AVG_ROW_LENGTH".toLowerCase());
    	addReservedKeyWord("BACKUP".toLowerCase());
    	addReservedKeyWord("BEFORE".toLowerCase());
    	addReservedKeyWord("BEGIN".toLowerCase());
    	addReservedKeyWord("BETWEEN".toLowerCase());
    	addReservedKeyWord("BIGINT".toLowerCase());
    	addReservedKeyWord("BINARY".toLowerCase());
    	addReservedKeyWord("BINLOG".toLowerCase());
    	addReservedKeyWord("BIT".toLowerCase());
    	addReservedKeyWord("BLOB".toLowerCase());
    	addReservedKeyWord("BLOCK".toLowerCase());
    	addReservedKeyWord("BOOL".toLowerCase());
    	addReservedKeyWord("BOOLEAN".toLowerCase());
    	addReservedKeyWord("BOTH".toLowerCase());
    	addReservedKeyWord("BTREE".toLowerCase());
    	addReservedKeyWord("BY".toLowerCase());
    	addReservedKeyWord("BYTE".toLowerCase());
    	addReservedKeyWord("CACHE".toLowerCase());
    	addReservedKeyWord("CALL".toLowerCase());
    	addReservedKeyWord("CASCADE".toLowerCase());
    	addReservedKeyWord("CASCADED".toLowerCase());
    	addReservedKeyWord("CASE".toLowerCase());
    	addReservedKeyWord("CATALOG_NAME".toLowerCase());
    	addReservedKeyWord("CHAIN".toLowerCase());
    	addReservedKeyWord("CHANGE".toLowerCase());
    	addReservedKeyWord("CHANGED".toLowerCase());
    	addReservedKeyWord("CHANNEL".toLowerCase());
    	addReservedKeyWord("CHAR".toLowerCase());
    	addReservedKeyWord("CHARACTER".toLowerCase());
    	addReservedKeyWord("CHARSET".toLowerCase());
    	addReservedKeyWord("CHECK".toLowerCase());
    	addReservedKeyWord("CHECKSUM".toLowerCase());
    	addReservedKeyWord("CIPHER".toLowerCase());
    	addReservedKeyWord("CLASS_ORIGIN".toLowerCase());
    	addReservedKeyWord("CLIENT".toLowerCase());
    	addReservedKeyWord("CLOSE".toLowerCase());
    	addReservedKeyWord("COALESCE".toLowerCase());
    	addReservedKeyWord("CODE".toLowerCase());
    	addReservedKeyWord("COLLATE".toLowerCase());
    	addReservedKeyWord("COLLATION".toLowerCase());
    	addReservedKeyWord("COLUMN".toLowerCase());
    	addReservedKeyWord("COLUMNS".toLowerCase());
    	addReservedKeyWord("COLUMN_FORMAT".toLowerCase());
    	addReservedKeyWord("COLUMN_NAME".toLowerCase());
    	addReservedKeyWord("COMMENT".toLowerCase());
    	addReservedKeyWord("COMMIT".toLowerCase());
    	addReservedKeyWord("COMMITTED".toLowerCase());
    	addReservedKeyWord("COMPACT".toLowerCase());
    	addReservedKeyWord("COMPLETION".toLowerCase());
    	addReservedKeyWord("COMPRESSED".toLowerCase());
    	addReservedKeyWord("COMPRESSION".toLowerCase());
    	addReservedKeyWord("CONCURRENT".toLowerCase());
    	addReservedKeyWord("CONDITION".toLowerCase());
    	addReservedKeyWord("CONNECTION".toLowerCase());
    	addReservedKeyWord("CONSISTENT".toLowerCase());
    	addReservedKeyWord("CONSTRAINT".toLowerCase());
    	addReservedKeyWord("CONSTRAINT_CATALOG".toLowerCase());
    	addReservedKeyWord("CONSTRAINT_NAME".toLowerCase());
    	addReservedKeyWord("CONSTRAINT_SCHEMA".toLowerCase());
    	addReservedKeyWord("CONTAINS".toLowerCase());
    	addReservedKeyWord("CONTEXT".toLowerCase());
    	addReservedKeyWord("CONTINUE".toLowerCase());
    	addReservedKeyWord("CONVERT".toLowerCase());
    	addReservedKeyWord("CPU".toLowerCase());
    	addReservedKeyWord("CREATE".toLowerCase());
    	addReservedKeyWord("CROSS".toLowerCase());
    	addReservedKeyWord("CUBE".toLowerCase());
    	addReservedKeyWord("CURRENT".toLowerCase());
    	addReservedKeyWord("CURRENT_DATE".toLowerCase());
    	addReservedKeyWord("CURRENT_TIME".toLowerCase());
    	addReservedKeyWord("CURRENT_TIMESTAMP".toLowerCase());
    	addReservedKeyWord("CURRENT_USER".toLowerCase());
    	addReservedKeyWord("CURSOR".toLowerCase());
    	addReservedKeyWord("CURSOR_NAME".toLowerCase());
    	addReservedKeyWord("DATA".toLowerCase());
    	addReservedKeyWord("DATABASE".toLowerCase());
    	addReservedKeyWord("DATABASES".toLowerCase());
    	addReservedKeyWord("DATAFILE".toLowerCase());
    	addReservedKeyWord("DATE".toLowerCase());
    	addReservedKeyWord("DATETIME".toLowerCase());
    	addReservedKeyWord("DAY".toLowerCase());
    	addReservedKeyWord("DAY_HOUR".toLowerCase());
    	addReservedKeyWord("DAY_MICROSECOND".toLowerCase());
    	addReservedKeyWord("DAY_MINUTE".toLowerCase());
    	addReservedKeyWord("DAY_SECOND".toLowerCase());
    	addReservedKeyWord("DEALLOCATE".toLowerCase());
    	addReservedKeyWord("DEC".toLowerCase());
    	addReservedKeyWord("DECIMAL".toLowerCase());
    	addReservedKeyWord("DECLARE".toLowerCase());
    	addReservedKeyWord("DEFAULT".toLowerCase());
    	addReservedKeyWord("DEFAULT_AUTH".toLowerCase());
    	addReservedKeyWord("DEFINER".toLowerCase());
    	addReservedKeyWord("DELAYED".toLowerCase());
    	addReservedKeyWord("DELAY_KEY_WRITE".toLowerCase());
    	addReservedKeyWord("DELETE".toLowerCase());
    	addReservedKeyWord("DESC".toLowerCase());
    	addReservedKeyWord("DESCRIBE".toLowerCase());
    	addReservedKeyWord("DES_KEY_FILE".toLowerCase());
    	addReservedKeyWord("DETERMINISTIC".toLowerCase());
    	addReservedKeyWord("DIAGNOSTICS".toLowerCase());
    	addReservedKeyWord("DIRECTORY".toLowerCase());
    	addReservedKeyWord("DISABLE".toLowerCase());
    	addReservedKeyWord("DISCARD".toLowerCase());
    	addReservedKeyWord("DISK".toLowerCase());
    	addReservedKeyWord("DISTINCT".toLowerCase());
    	addReservedKeyWord("DISTINCTROW".toLowerCase());
    	addReservedKeyWord("DIV".toLowerCase());
    	addReservedKeyWord("DO".toLowerCase());
    	addReservedKeyWord("DOUBLE".toLowerCase());
    	addReservedKeyWord("DROP".toLowerCase());
    	addReservedKeyWord("DUAL".toLowerCase());
    	addReservedKeyWord("DUMPFILE".toLowerCase());
    	addReservedKeyWord("DUPLICATE".toLowerCase());
    	addReservedKeyWord("DYNAMIC".toLowerCase());
    	addReservedKeyWord("EACH".toLowerCase());
    	addReservedKeyWord("ELSE".toLowerCase());
    	addReservedKeyWord("ELSEIF".toLowerCase());
    	addReservedKeyWord("ENABLE".toLowerCase());
    	addReservedKeyWord("ENCLOSED".toLowerCase());
    	addReservedKeyWord("ENCRYPTION".toLowerCase());
    	addReservedKeyWord("END".toLowerCase());
    	addReservedKeyWord("ENDS".toLowerCase());
    	addReservedKeyWord("ENGINE".toLowerCase());
    	addReservedKeyWord("ENGINES".toLowerCase());
    	addReservedKeyWord("ENUM".toLowerCase());
    	addReservedKeyWord("ERROR".toLowerCase());
    	addReservedKeyWord("ERRORS".toLowerCase());
    	addReservedKeyWord("ESCAPE".toLowerCase());
    	addReservedKeyWord("ESCAPED".toLowerCase());
    	addReservedKeyWord("EVENT".toLowerCase());
    	addReservedKeyWord("EVENTS".toLowerCase());
    	addReservedKeyWord("EVERY".toLowerCase());
    	addReservedKeyWord("EXCHANGE".toLowerCase());
    	addReservedKeyWord("EXECUTE".toLowerCase());
    	addReservedKeyWord("EXISTS".toLowerCase());
    	addReservedKeyWord("EXIT".toLowerCase());
    	addReservedKeyWord("EXPANSION".toLowerCase());
    	addReservedKeyWord("EXPIRE".toLowerCase());
    	addReservedKeyWord("EXPLAIN".toLowerCase());
    	addReservedKeyWord("EXPORT".toLowerCase());
    	addReservedKeyWord("EXTENDED".toLowerCase());
    	addReservedKeyWord("EXTENT_SIZE".toLowerCase());
    	addReservedKeyWord("FALSE".toLowerCase());
    	addReservedKeyWord("FAST".toLowerCase());
    	addReservedKeyWord("FAULTS".toLowerCase());
    	addReservedKeyWord("FETCH".toLowerCase());
    	addReservedKeyWord("FIELDS".toLowerCase());
    	addReservedKeyWord("FILE".toLowerCase());
    	addReservedKeyWord("FILE_BLOCK_SIZE".toLowerCase());
    	addReservedKeyWord("FILTER".toLowerCase());
    	addReservedKeyWord("FIRST".toLowerCase());
    	addReservedKeyWord("FIXED".toLowerCase());
    	addReservedKeyWord("FLOAT".toLowerCase());
    	addReservedKeyWord("FLOAT4".toLowerCase());
    	addReservedKeyWord("FLOAT8".toLowerCase());
    	addReservedKeyWord("FLUSH".toLowerCase());
    	addReservedKeyWord("FOLLOWS".toLowerCase());
    	addReservedKeyWord("FOR".toLowerCase());
    	addReservedKeyWord("FORCE".toLowerCase());
    	addReservedKeyWord("FOREIGN".toLowerCase());
    	addReservedKeyWord("FORMAT".toLowerCase());
    	addReservedKeyWord("FOUND".toLowerCase());
    	addReservedKeyWord("FROM".toLowerCase());
    	addReservedKeyWord("FULL".toLowerCase());
    	addReservedKeyWord("FULLTEXT".toLowerCase());
    	addReservedKeyWord("FUNCTION".toLowerCase());
    	addReservedKeyWord("GENERAL".toLowerCase());
    	addReservedKeyWord("GENERATED".toLowerCase());
    	addReservedKeyWord("GEOMETRY".toLowerCase());
    	addReservedKeyWord("GEOMETRYCOLLECTION".toLowerCase());
    	addReservedKeyWord("GET".toLowerCase());
    	addReservedKeyWord("GET_FORMAT".toLowerCase());
    	addReservedKeyWord("GLOBAL".toLowerCase());
    	addReservedKeyWord("GRANT".toLowerCase());
    	addReservedKeyWord("GRANTS".toLowerCase());
    	addReservedKeyWord("GROUP".toLowerCase());
    	addReservedKeyWord("GROUP_REPLICATION".toLowerCase());
    	addReservedKeyWord("HANDLER".toLowerCase());
    	addReservedKeyWord("HASH".toLowerCase());
    	addReservedKeyWord("HAVING".toLowerCase());
    	addReservedKeyWord("HELP".toLowerCase());
    	addReservedKeyWord("HIGH_PRIORITY".toLowerCase());
    	addReservedKeyWord("HOST".toLowerCase());
    	addReservedKeyWord("HOSTS".toLowerCase());
    	addReservedKeyWord("HOUR".toLowerCase());
    	addReservedKeyWord("HOUR_MICROSECOND".toLowerCase());
    	addReservedKeyWord("HOUR_MINUTE".toLowerCase());
    	addReservedKeyWord("HOUR_SECOND".toLowerCase());
    	addReservedKeyWord("IDENTIFIED".toLowerCase());
    	addReservedKeyWord("IF".toLowerCase());
    	addReservedKeyWord("IGNORE".toLowerCase());
    	addReservedKeyWord("IGNORE_SERVER_IDS".toLowerCase());
    	addReservedKeyWord("IMPORT".toLowerCase());
    	addReservedKeyWord("IN".toLowerCase());
    	addReservedKeyWord("INDEX".toLowerCase());
    	addReservedKeyWord("INDEXES".toLowerCase());
    	addReservedKeyWord("INFILE".toLowerCase());
    	addReservedKeyWord("INITIAL_SIZE".toLowerCase());
    	addReservedKeyWord("INNER".toLowerCase());
    	addReservedKeyWord("INOUT".toLowerCase());
    	addReservedKeyWord("INSENSITIVE".toLowerCase());
    	addReservedKeyWord("INSERT".toLowerCase());
    	addReservedKeyWord("INSERT_METHOD".toLowerCase());
    	addReservedKeyWord("INSTALL".toLowerCase());
    	addReservedKeyWord("INSTANCE".toLowerCase());
    	addReservedKeyWord("INT".toLowerCase());
    	addReservedKeyWord("INT1".toLowerCase());
    	addReservedKeyWord("INT2".toLowerCase());
    	addReservedKeyWord("INT3".toLowerCase());
    	addReservedKeyWord("INT4".toLowerCase());
    	addReservedKeyWord("INT8".toLowerCase());
    	addReservedKeyWord("INTEGER".toLowerCase());
    	addReservedKeyWord("INTERVAL".toLowerCase());
    	addReservedKeyWord("INTO".toLowerCase());
    	addReservedKeyWord("INVOKER".toLowerCase());
    	addReservedKeyWord("IO".toLowerCase());
    	addReservedKeyWord("IO_AFTER_GTIDS".toLowerCase());
    	addReservedKeyWord("IO_BEFORE_GTIDS".toLowerCase());
    	addReservedKeyWord("IO_THREAD".toLowerCase());
    	addReservedKeyWord("IPC".toLowerCase());
    	addReservedKeyWord("IS".toLowerCase());
    	addReservedKeyWord("ISOLATION".toLowerCase());
    	addReservedKeyWord("ISSUER".toLowerCase());
    	addReservedKeyWord("ITERATE".toLowerCase());
    	addReservedKeyWord("JOIN".toLowerCase());
    	addReservedKeyWord("JSON".toLowerCase());
    	addReservedKeyWord("KEY".toLowerCase());
    	addReservedKeyWord("KEYS".toLowerCase());
    	addReservedKeyWord("KEY_BLOCK_SIZE".toLowerCase());
    	addReservedKeyWord("KILL".toLowerCase());
    	addReservedKeyWord("LANGUAGE".toLowerCase());
    	addReservedKeyWord("LAST".toLowerCase());
    	addReservedKeyWord("LEADING".toLowerCase());
    	addReservedKeyWord("LEAVE".toLowerCase());
    	addReservedKeyWord("LEAVES".toLowerCase());
    	addReservedKeyWord("LEFT".toLowerCase());
    	addReservedKeyWord("LESS".toLowerCase());
    	addReservedKeyWord("LEVEL".toLowerCase());
    	addReservedKeyWord("LIKE".toLowerCase());
    	addReservedKeyWord("LIMIT".toLowerCase());
    	addReservedKeyWord("LINEAR".toLowerCase());
    	addReservedKeyWord("LINES".toLowerCase());
    	addReservedKeyWord("LINESTRING".toLowerCase());
    	addReservedKeyWord("LIST".toLowerCase());
    	addReservedKeyWord("LOAD".toLowerCase());
    	addReservedKeyWord("LOCAL".toLowerCase());
    	addReservedKeyWord("LOCALTIME".toLowerCase());
    	addReservedKeyWord("LOCALTIMESTAMP".toLowerCase());
    	addReservedKeyWord("LOCK".toLowerCase());
    	addReservedKeyWord("LOCKS".toLowerCase());
    	addReservedKeyWord("LOGFILE".toLowerCase());
    	addReservedKeyWord("LOGS".toLowerCase());
    	addReservedKeyWord("LONG".toLowerCase());
    	addReservedKeyWord("LONGBLOB".toLowerCase());
    	addReservedKeyWord("LONGTEXT".toLowerCase());
    	addReservedKeyWord("LOOP".toLowerCase());
    	addReservedKeyWord("LOW_PRIORITY".toLowerCase());
    	addReservedKeyWord("MASTER".toLowerCase());
    	addReservedKeyWord("MASTER_AUTO_POSITION".toLowerCase());
    	addReservedKeyWord("MASTER_BIND".toLowerCase());
    	addReservedKeyWord("MASTER_CONNECT_RETRY".toLowerCase());
    	addReservedKeyWord("MASTER_DELAY".toLowerCase());
    	addReservedKeyWord("MASTER_HEARTBEAT_PERIOD".toLowerCase());
    	addReservedKeyWord("MASTER_HOST".toLowerCase());
    	addReservedKeyWord("MASTER_LOG_FILE".toLowerCase());
    	addReservedKeyWord("MASTER_LOG_POS".toLowerCase());
    	addReservedKeyWord("MASTER_PASSWORD".toLowerCase());
    	addReservedKeyWord("MASTER_PORT".toLowerCase());
    	addReservedKeyWord("MASTER_RETRY_COUNT".toLowerCase());
    	addReservedKeyWord("MASTER_SERVER_ID".toLowerCase());
    	addReservedKeyWord("MASTER_SSL".toLowerCase());
    	addReservedKeyWord("MASTER_SSL_CA".toLowerCase());
    	addReservedKeyWord("MASTER_SSL_CAPATH".toLowerCase());
    	addReservedKeyWord("MASTER_SSL_CERT".toLowerCase());
    	addReservedKeyWord("MASTER_SSL_CIPHER".toLowerCase());
    	addReservedKeyWord("MASTER_SSL_CRL".toLowerCase());
    	addReservedKeyWord("MASTER_SSL_CRLPATH".toLowerCase());
    	addReservedKeyWord("MASTER_SSL_KEY".toLowerCase());
    	addReservedKeyWord("MASTER_SSL_VERIFY_SERVER_CERT".toLowerCase());
    	addReservedKeyWord("MASTER_TLS_VERSION".toLowerCase());
    	addReservedKeyWord("MASTER_USER".toLowerCase());
    	addReservedKeyWord("MATCH".toLowerCase());
    	addReservedKeyWord("MAXVALUE".toLowerCase());
    	addReservedKeyWord("MAX_CONNECTIONS_PER_HOUR".toLowerCase());
    	addReservedKeyWord("MAX_QUERIES_PER_HOUR".toLowerCase());
    	addReservedKeyWord("MAX_ROWS".toLowerCase());
    	addReservedKeyWord("MAX_SIZE".toLowerCase());
    	addReservedKeyWord("MAX_STATEMENT_TIME".toLowerCase());
    	addReservedKeyWord("MAX_UPDATES_PER_HOUR".toLowerCase());
    	addReservedKeyWord("MAX_USER_CONNECTIONS".toLowerCase());
    	addReservedKeyWord("MEDIUM".toLowerCase());
    	addReservedKeyWord("MEDIUMBLOB".toLowerCase());
    	addReservedKeyWord("MEDIUMINT".toLowerCase());
    	addReservedKeyWord("MEDIUMTEXT".toLowerCase());
    	addReservedKeyWord("MEMORY".toLowerCase());
    	addReservedKeyWord("MERGE".toLowerCase());
    	addReservedKeyWord("MESSAGE_TEXT".toLowerCase());
    	addReservedKeyWord("MICROSECOND".toLowerCase());
    	addReservedKeyWord("MIDDLEINT".toLowerCase());
    	addReservedKeyWord("MIGRATE".toLowerCase());
    	addReservedKeyWord("MINUTE".toLowerCase());
    	addReservedKeyWord("MINUTE_MICROSECOND".toLowerCase());
    	addReservedKeyWord("MINUTE_SECOND".toLowerCase());
    	addReservedKeyWord("MIN_ROWS".toLowerCase());
    	addReservedKeyWord("MOD".toLowerCase());
    	addReservedKeyWord("MODE".toLowerCase());
    	addReservedKeyWord("MODIFIES".toLowerCase());
    	addReservedKeyWord("MODIFY".toLowerCase());
    	addReservedKeyWord("MONTH".toLowerCase());
    	addReservedKeyWord("MULTILINESTRING".toLowerCase());
    	addReservedKeyWord("MULTIPOINT".toLowerCase());
    	addReservedKeyWord("MULTIPOLYGON".toLowerCase());
    	addReservedKeyWord("MUTEX".toLowerCase());
    	addReservedKeyWord("MYSQL_ERRNO".toLowerCase());
    	addReservedKeyWord("NAME".toLowerCase());
    	addReservedKeyWord("NAMES".toLowerCase());
    	addReservedKeyWord("NATIONAL".toLowerCase());
    	addReservedKeyWord("NATURAL".toLowerCase());
    	addReservedKeyWord("NCHAR".toLowerCase());
    	addReservedKeyWord("NDB".toLowerCase());
    	addReservedKeyWord("NDBCLUSTER".toLowerCase());
    	addReservedKeyWord("NEVER".toLowerCase());
    	addReservedKeyWord("NEW".toLowerCase());
    	addReservedKeyWord("NEXT".toLowerCase());
    	addReservedKeyWord("NO".toLowerCase());
    	addReservedKeyWord("NODEGROUP".toLowerCase());
    	addReservedKeyWord("NONBLOCKING".toLowerCase());
    	addReservedKeyWord("NONE".toLowerCase());
    	addReservedKeyWord("NOT".toLowerCase());
    	addReservedKeyWord("NO_WAIT".toLowerCase());
    	addReservedKeyWord("NO_WRITE_TO_BINLOG".toLowerCase());
    	addReservedKeyWord("NULL".toLowerCase());
    	addReservedKeyWord("NUMBER".toLowerCase());
    	addReservedKeyWord("NUMERIC".toLowerCase());
    	addReservedKeyWord("NVARCHAR".toLowerCase());
    	addReservedKeyWord("OFFSET".toLowerCase());
    	addReservedKeyWord("OLD_PASSWORD".toLowerCase());
    	addReservedKeyWord("ON".toLowerCase());
    	addReservedKeyWord("ONE".toLowerCase());
    	addReservedKeyWord("ONLY".toLowerCase());
    	addReservedKeyWord("OPEN".toLowerCase());
    	addReservedKeyWord("OPTIMIZE".toLowerCase());
    	addReservedKeyWord("OPTIMIZER_COSTS".toLowerCase());
    	addReservedKeyWord("OPTION".toLowerCase());
    	addReservedKeyWord("OPTIONALLY".toLowerCase());
    	addReservedKeyWord("OPTIONS".toLowerCase());
    	addReservedKeyWord("OR".toLowerCase());
    	addReservedKeyWord("ORDER".toLowerCase());
    	addReservedKeyWord("OUT".toLowerCase());
    	addReservedKeyWord("OUTER".toLowerCase());
    	addReservedKeyWord("OUTFILE".toLowerCase());
    	addReservedKeyWord("OWNER".toLowerCase());
    	addReservedKeyWord("PACK_KEYS".toLowerCase());
    	addReservedKeyWord("PAGE".toLowerCase());
    	addReservedKeyWord("PARSER".toLowerCase());
    	addReservedKeyWord("PARSE_GCOL_EXPR".toLowerCase());
    	addReservedKeyWord("PARTIAL".toLowerCase());
    	addReservedKeyWord("PARTITION".toLowerCase());
    	addReservedKeyWord("PARTITIONING".toLowerCase());
    	addReservedKeyWord("PARTITIONS".toLowerCase());
    	addReservedKeyWord("PASSWORD".toLowerCase());
    	addReservedKeyWord("PHASE".toLowerCase());
    	addReservedKeyWord("PLUGIN".toLowerCase());
    	addReservedKeyWord("PLUGINS".toLowerCase());
    	addReservedKeyWord("PLUGIN_DIR".toLowerCase());
    	addReservedKeyWord("POINT".toLowerCase());
    	addReservedKeyWord("POLYGON".toLowerCase());
    	addReservedKeyWord("PORT".toLowerCase());
    	addReservedKeyWord("PRECEDES".toLowerCase());
    	addReservedKeyWord("PRECISION".toLowerCase());
    	addReservedKeyWord("PREPARE".toLowerCase());
    	addReservedKeyWord("PRESERVE".toLowerCase());
    	addReservedKeyWord("PREV".toLowerCase());
    	addReservedKeyWord("PRIMARY".toLowerCase());
    	addReservedKeyWord("PRIVILEGES".toLowerCase());
    	addReservedKeyWord("PROCEDURE".toLowerCase());
    	addReservedKeyWord("PROCESSLIST".toLowerCase());
    	addReservedKeyWord("PROFILE".toLowerCase());
    	addReservedKeyWord("PROFILES".toLowerCase());
    	addReservedKeyWord("PROXY".toLowerCase());
    	addReservedKeyWord("PURGE".toLowerCase());
    	addReservedKeyWord("QUARTER".toLowerCase());
    	addReservedKeyWord("QUERY".toLowerCase());
    	addReservedKeyWord("QUICK".toLowerCase());
    	addReservedKeyWord("RANGE".toLowerCase());
    	addReservedKeyWord("READ".toLowerCase());
    	addReservedKeyWord("READS".toLowerCase());
    	addReservedKeyWord("READ_ONLY".toLowerCase());
    	addReservedKeyWord("READ_WRITE".toLowerCase());
    	addReservedKeyWord("REAL".toLowerCase());
    	addReservedKeyWord("REBUILD".toLowerCase());
    	addReservedKeyWord("RECOVER".toLowerCase());
    	addReservedKeyWord("REDOFILE".toLowerCase());
    	addReservedKeyWord("REDO_BUFFER_SIZE".toLowerCase());
    	addReservedKeyWord("REDUNDANT".toLowerCase());
    	addReservedKeyWord("REFERENCES".toLowerCase());
    	addReservedKeyWord("REGEXP".toLowerCase());
    	addReservedKeyWord("RELAY".toLowerCase());
    	addReservedKeyWord("RELAYLOG".toLowerCase());
    	addReservedKeyWord("RELAY_LOG_FILE".toLowerCase());
    	addReservedKeyWord("RELAY_LOG_POS".toLowerCase());
    	addReservedKeyWord("RELAY_THREAD".toLowerCase());
    	addReservedKeyWord("RELEASE".toLowerCase());
    	addReservedKeyWord("RELOAD".toLowerCase());
    	addReservedKeyWord("REMOVE".toLowerCase());
    	addReservedKeyWord("RENAME".toLowerCase());
    	addReservedKeyWord("REORGANIZE".toLowerCase());
    	addReservedKeyWord("REPAIR".toLowerCase());
    	addReservedKeyWord("REPEAT".toLowerCase());
    	addReservedKeyWord("REPEATABLE".toLowerCase());
    	addReservedKeyWord("REPLACE".toLowerCase());
    	addReservedKeyWord("REPLICATE_DO_DB".toLowerCase());
    	addReservedKeyWord("REPLICATE_DO_TABLE".toLowerCase());
    	addReservedKeyWord("REPLICATE_IGNORE_DB".toLowerCase());
    	addReservedKeyWord("REPLICATE_IGNORE_TABLE".toLowerCase());
    	addReservedKeyWord("REPLICATE_REWRITE_DB".toLowerCase());
    	addReservedKeyWord("REPLICATE_WILD_DO_TABLE".toLowerCase());
    	addReservedKeyWord("REPLICATE_WILD_IGNORE_TABLE".toLowerCase());
    	addReservedKeyWord("REPLICATION".toLowerCase());
    	addReservedKeyWord("REQUIRE".toLowerCase());
    	addReservedKeyWord("RESET".toLowerCase());
    	addReservedKeyWord("RESIGNAL".toLowerCase());
    	addReservedKeyWord("RESTORE".toLowerCase());
    	addReservedKeyWord("RESTRICT".toLowerCase());
    	addReservedKeyWord("RESUME".toLowerCase());
    	addReservedKeyWord("RETURN".toLowerCase());
    	addReservedKeyWord("RETURNED_SQLSTATE".toLowerCase());
    	addReservedKeyWord("RETURNS".toLowerCase());
    	addReservedKeyWord("REVERSE".toLowerCase());
    	addReservedKeyWord("REVOKE".toLowerCase());
    	addReservedKeyWord("RIGHT".toLowerCase());
    	addReservedKeyWord("RLIKE".toLowerCase());
    	addReservedKeyWord("ROLLBACK".toLowerCase());
    	addReservedKeyWord("ROLLUP".toLowerCase());
    	addReservedKeyWord("ROTATE".toLowerCase());
    	addReservedKeyWord("ROUTINE".toLowerCase());
    	addReservedKeyWord("ROW".toLowerCase());
    	addReservedKeyWord("ROWS".toLowerCase());
    	addReservedKeyWord("ROW_COUNT".toLowerCase());
    	addReservedKeyWord("ROW_FORMAT".toLowerCase());
    	addReservedKeyWord("RTREE".toLowerCase());
    	addReservedKeyWord("SAVEPOINT".toLowerCase());
    	addReservedKeyWord("SCHEDULE".toLowerCase());
    	addReservedKeyWord("SCHEMA".toLowerCase());
    	addReservedKeyWord("SCHEMAS".toLowerCase());
    	addReservedKeyWord("SCHEMA_NAME".toLowerCase());
    	addReservedKeyWord("SECOND".toLowerCase());
    	addReservedKeyWord("SECOND_MICROSECOND".toLowerCase());
    	addReservedKeyWord("SECURITY".toLowerCase());
    	addReservedKeyWord("SELECT".toLowerCase());
    	addReservedKeyWord("SENSITIVE".toLowerCase());
    	addReservedKeyWord("SEPARATOR".toLowerCase());
    	addReservedKeyWord("SERIAL".toLowerCase());
    	addReservedKeyWord("SERIALIZABLE".toLowerCase());
    	addReservedKeyWord("SERVER".toLowerCase());
    	addReservedKeyWord("SESSION".toLowerCase());
    	addReservedKeyWord("SET".toLowerCase());
    	addReservedKeyWord("SHARE".toLowerCase());
    	addReservedKeyWord("SHOW".toLowerCase());
    	addReservedKeyWord("SHUTDOWN".toLowerCase());
    	addReservedKeyWord("SIGNAL".toLowerCase());
    	addReservedKeyWord("SIGNED".toLowerCase());
    	addReservedKeyWord("SIMPLE".toLowerCase());
    	addReservedKeyWord("SLAVE".toLowerCase());
    	addReservedKeyWord("SLOW".toLowerCase());
    	addReservedKeyWord("SMALLINT".toLowerCase());
    	addReservedKeyWord("SNAPSHOT".toLowerCase());
    	addReservedKeyWord("SOCKET".toLowerCase());
    	addReservedKeyWord("SOME".toLowerCase());
    	addReservedKeyWord("SONAME".toLowerCase());
    	addReservedKeyWord("SOUNDS".toLowerCase());
    	addReservedKeyWord("SOURCE".toLowerCase());
    	addReservedKeyWord("SPATIAL".toLowerCase());
    	addReservedKeyWord("SPECIFIC".toLowerCase());
    	addReservedKeyWord("SQL".toLowerCase());
    	addReservedKeyWord("SQLEXCEPTION".toLowerCase());
    	addReservedKeyWord("SQLSTATE".toLowerCase());
    	addReservedKeyWord("SQLWARNING".toLowerCase());
    	addReservedKeyWord("SQL_AFTER_GTIDS".toLowerCase());
    	addReservedKeyWord("SQL_AFTER_MTS_GAPS".toLowerCase());
    	addReservedKeyWord("SQL_BEFORE_GTIDS".toLowerCase());
    	addReservedKeyWord("SQL_BIG_RESULT".toLowerCase());
    	addReservedKeyWord("SQL_BUFFER_RESULT".toLowerCase());
    	addReservedKeyWord("SQL_CACHE".toLowerCase());
    	addReservedKeyWord("SQL_CALC_FOUND_ROWS".toLowerCase());
    	addReservedKeyWord("SQL_NO_CACHE".toLowerCase());
    	addReservedKeyWord("SQL_SMALL_RESULT".toLowerCase());
    	addReservedKeyWord("SQL_THREAD".toLowerCase());
    	addReservedKeyWord("SQL_TSI_DAY".toLowerCase());
    	addReservedKeyWord("SQL_TSI_HOUR".toLowerCase());
    	addReservedKeyWord("SQL_TSI_MINUTE".toLowerCase());
    	addReservedKeyWord("SQL_TSI_MONTH".toLowerCase());
    	addReservedKeyWord("SQL_TSI_QUARTER".toLowerCase());
    	addReservedKeyWord("SQL_TSI_SECOND".toLowerCase());
    	addReservedKeyWord("SQL_TSI_WEEK".toLowerCase());
    	addReservedKeyWord("SQL_TSI_YEAR".toLowerCase());
    	addReservedKeyWord("SSL".toLowerCase());
    	addReservedKeyWord("STACKED".toLowerCase());
    	addReservedKeyWord("START".toLowerCase());
    	addReservedKeyWord("STARTING".toLowerCase());
    	addReservedKeyWord("STARTS".toLowerCase());
    	addReservedKeyWord("STATS_AUTO_RECALC".toLowerCase());
    	addReservedKeyWord("STATS_PERSISTENT".toLowerCase());
    	addReservedKeyWord("STATS_SAMPLE_PAGES".toLowerCase());
    	addReservedKeyWord("STATUS".toLowerCase());
    	addReservedKeyWord("STOP".toLowerCase());
    	addReservedKeyWord("STORAGE".toLowerCase());
    	addReservedKeyWord("STORED".toLowerCase());
    	addReservedKeyWord("STRAIGHT_JOIN".toLowerCase());
    	addReservedKeyWord("STRING".toLowerCase());
    	addReservedKeyWord("SUBCLASS_ORIGIN".toLowerCase());
    	addReservedKeyWord("SUBJECT".toLowerCase());
    	addReservedKeyWord("SUBPARTITION".toLowerCase());
    	addReservedKeyWord("SUBPARTITIONS".toLowerCase());
    	addReservedKeyWord("SUPER".toLowerCase());
    	addReservedKeyWord("SUSPEND".toLowerCase());
    	addReservedKeyWord("SWAPS".toLowerCase());
    	addReservedKeyWord("SWITCHES".toLowerCase());
    	addReservedKeyWord("TABLE".toLowerCase());
    	addReservedKeyWord("TABLES".toLowerCase());
    	addReservedKeyWord("TABLESPACE".toLowerCase());
    	addReservedKeyWord("TABLE_CHECKSUM".toLowerCase());
    	addReservedKeyWord("TABLE_NAME".toLowerCase());
    	addReservedKeyWord("TEMPORARY".toLowerCase());
    	addReservedKeyWord("TEMPTABLE".toLowerCase());
    	addReservedKeyWord("TERMINATED".toLowerCase());
    	addReservedKeyWord("TEXT".toLowerCase());
    	addReservedKeyWord("THAN".toLowerCase());
    	addReservedKeyWord("THEN".toLowerCase());
    	addReservedKeyWord("TIME".toLowerCase());
    	addReservedKeyWord("TIMESTAMP".toLowerCase());
    	addReservedKeyWord("TIMESTAMPADD".toLowerCase());
    	addReservedKeyWord("TIMESTAMPDIFF".toLowerCase());
    	addReservedKeyWord("TINYBLOB".toLowerCase());
    	addReservedKeyWord("TINYINT".toLowerCase());
    	addReservedKeyWord("TINYTEXT".toLowerCase());
    	addReservedKeyWord("TO".toLowerCase());
    	addReservedKeyWord("TRAILING".toLowerCase());
    	addReservedKeyWord("TRANSACTION".toLowerCase());
    	addReservedKeyWord("TRIGGER".toLowerCase());
    	addReservedKeyWord("TRIGGERS".toLowerCase());
    	addReservedKeyWord("TRUE".toLowerCase());
    	addReservedKeyWord("TRUNCATE".toLowerCase());
    	addReservedKeyWord("TYPE".toLowerCase());
    	addReservedKeyWord("TYPES".toLowerCase());
    	addReservedKeyWord("UNCOMMITTED".toLowerCase());
    	addReservedKeyWord("UNDEFINED".toLowerCase());
    	addReservedKeyWord("UNDO".toLowerCase());
    	addReservedKeyWord("UNDOFILE".toLowerCase());
    	addReservedKeyWord("UNDO_BUFFER_SIZE".toLowerCase());
    	addReservedKeyWord("UNICODE".toLowerCase());
    	addReservedKeyWord("UNINSTALL".toLowerCase());
    	addReservedKeyWord("UNION".toLowerCase());
    	addReservedKeyWord("UNIQUE".toLowerCase());
    	addReservedKeyWord("UNKNOWN".toLowerCase());
    	addReservedKeyWord("UNLOCK".toLowerCase());
    	addReservedKeyWord("UNSIGNED".toLowerCase());
    	addReservedKeyWord("UNTIL".toLowerCase());
    	addReservedKeyWord("UPDATE".toLowerCase());
    	addReservedKeyWord("UPGRADE".toLowerCase());
    	addReservedKeyWord("USAGE".toLowerCase());
    	addReservedKeyWord("USE".toLowerCase());
    	addReservedKeyWord("USER".toLowerCase());
    	addReservedKeyWord("USER_RESOURCES".toLowerCase());
    	addReservedKeyWord("USE_FRM".toLowerCase());
    	addReservedKeyWord("USING".toLowerCase());
    	addReservedKeyWord("UTC_DATE".toLowerCase());
    	addReservedKeyWord("UTC_TIME".toLowerCase());
    	addReservedKeyWord("UTC_TIMESTAMP".toLowerCase());
    	addReservedKeyWord("VALIDATION".toLowerCase());
    	addReservedKeyWord("VALUE".toLowerCase());
    	addReservedKeyWord("VALUES".toLowerCase());
    	addReservedKeyWord("VARBINARY".toLowerCase());
    	addReservedKeyWord("VARCHAR".toLowerCase());
    	addReservedKeyWord("VARCHARACTER".toLowerCase());
    	addReservedKeyWord("VARIABLES".toLowerCase());
    	addReservedKeyWord("VARYING".toLowerCase());
    	addReservedKeyWord("VIEW".toLowerCase());
    	addReservedKeyWord("VIRTUAL".toLowerCase());
    	addReservedKeyWord("WAIT".toLowerCase());
    	addReservedKeyWord("WARNINGS".toLowerCase());
    	addReservedKeyWord("WEEK".toLowerCase());
    	addReservedKeyWord("WEIGHT_STRING".toLowerCase());
    	addReservedKeyWord("WHEN".toLowerCase());
    	addReservedKeyWord("WHERE".toLowerCase());
    	addReservedKeyWord("WHILE".toLowerCase());
    	addReservedKeyWord("WITH".toLowerCase());
    	addReservedKeyWord("WITHOUT".toLowerCase());
    	addReservedKeyWord("WORK".toLowerCase());
    	addReservedKeyWord("WRAPPER".toLowerCase());
    	addReservedKeyWord("WRITE".toLowerCase());
    	addReservedKeyWord("X509".toLowerCase());
    	addReservedKeyWord("XA".toLowerCase());
    	addReservedKeyWord("XID".toLowerCase());
    	addReservedKeyWord("XML".toLowerCase());
    	addReservedKeyWord("XOR".toLowerCase());
    	addReservedKeyWord("YEAR".toLowerCase());
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
        return new DBCommandMySQL(autoPrepareStmt);
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
            case SQL_QUOTES_OPEN:             return "`";
            case SQL_QUOTES_CLOSE:            return "`";
            case SQL_CONCAT_EXPR:             return "concat(?, {0})";
            // data types
            case SQL_BOOLEAN_TRUE:            return "1";
            case SQL_BOOLEAN_FALSE:           return "0";
            case SQL_CURRENT_DATE:            return "CURRENT_DATE()";
            case SQL_DATE_PATTERN:            return "yyyy-MM-dd";
            case SQL_DATE_TEMPLATE:           return "'{0}'";
            case SQL_DATETIME_PATTERN:        return "yyyy-MM-dd HH:mm:ss";
            case SQL_DATETIME_TEMPLATE:       return "'{0}'";
            case SQL_CURRENT_TIMESTAMP:       return "NOW()";
            case SQL_TIMESTAMP_PATTERN:       return "yyyy-MM-dd HH:mm:ss";
            case SQL_TIMESTAMP_TEMPLATE:      return "'{0}'";
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
     * @see DBMSHandler#getNextSequenceValue(DBDatabase, String, int, Connection)
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
        if (value.indexOf('\'') >= 0 || value.indexOf('\\') >= 0)
        {
        	int len = value.length();
            for (int i = 0; i < len; i++)
            {
                if (value.charAt(i) == '\'')
                { // a routine to double up single quotes for SQL
                    buf.append("''");
                }
                else if (value.charAt(i) == '\\')
                { // a routine to double up backslashes for MySQL
                	buf.append("\\\\");
                } else
                {
                	buf.append(value.charAt(i));
                }
            }
        } else 
        {
            buf.append(value);
        }
    }
    
}
