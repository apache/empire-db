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
package org.apache.empire.db;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.apache.empire.data.DataType;

/**
 * The DBDatabaseDriver interface implements all RDBMS specific logic
 */
public interface DBDatabaseDriver
{
    // sql-phrases
    public static final int SQL_NULL_VALUE       = 1;   // Oracle: null
    public static final int SQL_PARAMETER        = 2;   // Oracle: ?
    public static final int SQL_RENAME_TABLE     = 3;   // Oracle: AS
    public static final int SQL_RENAME_COLUMN    = 4;   // Oracle: AS
    public static final int SQL_DATABASE_LINK    = 5;   // Oracle: @
    public static final int SQL_QUOTES_OPEN      = 6;   // Oracle: "; MSSQL: [
    public static final int SQL_QUOTES_CLOSE     = 7;   // Oracle: "; MSSQL: ]
    public static final int SQL_CONCAT_EXPR      = 8;   // Oracle: ||
    public static final int SQL_PSEUDO_TABLE     = 9;   // Oracle: "DUAL"
    // data types
    public static final int SQL_BOOLEAN_TRUE      = 10; // Oracle: "'Y'"; MSSQL: "1"
    public static final int SQL_BOOLEAN_FALSE     = 11; // Oracle: "'N'"; MSSQL: "0"
    public static final int SQL_CURRENT_DATE      = 20; // Oracle: "sysdate"
    public static final int SQL_DATE_PATTERN      = 21; // "yyyy-MM-dd"  // SimpleDateFormat
    public static final int SQL_DATE_TEMPLATE     = 22; // Oracle: "TO_DATE('{0}', 'YYYY-MM-DD')"
    public static final int SQL_DATETIME_PATTERN  = 23; // "yyyy-MM-dd HH:mm:ss.SSS"  // SimpleDateFormat
    public static final int SQL_DATETIME_TEMPLATE = 24; // Oracle: "TO_DATE('{0}', 'YYYY-MM-DD HH24:MI:SS')"
    public static final int SQL_CURRENT_TIMESTAMP = 25; // Oracle: "systimestamp"
    public static final int SQL_TIMESTAMP_PATTERN = 26; // "yyyy-MM-dd HH:mm:ss.SSS" // SimpleDateFormat
    public static final int SQL_TIMESTAMP_TEMPLATE= 27; // Oracle: "TO_TIMESTAMP('{0}', 'YYYY.MM.DD HH24:MI:SS.FF')";
    // functions
    public static final int SQL_FUNC_COALESCE    = 100; // Oracle: nvl(?, {0})
    public static final int SQL_FUNC_SUBSTRING   = 101; // Oracle: substr(?,{0})
    public static final int SQL_FUNC_SUBSTRINGEX = 102; // Oracle: substr(?,{0},{1})
    public static final int SQL_FUNC_REPLACE     = 103; // Oracle: replace(?,{0},{1})
    public static final int SQL_FUNC_REVERSE     = 104; // Oracle: reverse(?) 
    public static final int SQL_FUNC_STRINDEX    = 105; // Oracle: instr(?, {0})
    public static final int SQL_FUNC_STRINDEXFROM= 106; // Oracle: instr(?, {0}, {1}) 
    public static final int SQL_FUNC_LENGTH      = 107; // Oracle: length(?,{0})
    public static final int SQL_FUNC_UPPER       = 110; // Oracle: upper(?)
    public static final int SQL_FUNC_LOWER       = 111; // Oracle: lower(?)
    public static final int SQL_FUNC_TRIM        = 112; // Oracle: trim(?)
    public static final int SQL_FUNC_LTRIM       = 113; // Oracle: ltrim(?)
    public static final int SQL_FUNC_RTRIM       = 114; // Oracle: rtrim(?)
    public static final int SQL_FUNC_ESCAPE      = 119; // Oracle: ? escape '{0}'
    // Numeric
    public static final int SQL_FUNC_ABS         = 120; // Oracle: abs(?,{0})
    public static final int SQL_FUNC_ROUND       = 121; // Oracle: round(?, {0})
    public static final int SQL_FUNC_TRUNC       = 122; // Oracle: trunc(?, {0})
    public static final int SQL_FUNC_FLOOR       = 123; // Oracle: floor(?)
    public static final int SQL_FUNC_CEILING     = 124; // Oracle: ceil(?)
    public static final int SQL_FUNC_MODULO      = 125; // Oracle: mod(?)
    public static final int SQL_FUNC_FORMAT      = 126; // Oracle: TO_CHAR(?)
    // Date
    public static final int SQL_FUNC_DAY         = 132; // MSSQL: month(?)
    public static final int SQL_FUNC_MONTH       = 133; // MSSQL: month(?)
    public static final int SQL_FUNC_YEAR        = 134; // MSSQL: year (?)
    // Aggregation
    public static final int SQL_FUNC_SUM         = 140; // Oracle: sum(?)
    public static final int SQL_FUNC_MAX         = 142; // Oracle: max(?)
    public static final int SQL_FUNC_MIN         = 143; // Oracle: min(?)
    public static final int SQL_FUNC_AVG         = 144; // Oracle: avg(?)
    // Decode
    public static final int SQL_FUNC_DECODE      = 150; // Oracle: "decode(? {0})" SQL: "case ?{0} end"
    public static final int SQL_FUNC_DECODE_SEP  = 151; // Oracle: ","             SQL: " "
    public static final int SQL_FUNC_DECODE_PART = 152; // Oracle: "{0}, {1}"      SQL: "when {0} then {1}"
    public static final int SQL_FUNC_DECODE_ELSE = 153; // Oracle: "{0}"           SQL: "else {0}"
    
    
    /**
     * Called when a database is opened
     */
    void attachDatabase(DBDatabase db, Connection conn);

    /**
     * Called when a database is closed
     */
    void detachDatabase(DBDatabase db, Connection conn);
    
    /**
     * This function creates a DBCommand derived object this database
     * @param db the database for which to create a command object for
     * @return a DBCommand object
     */
    DBCommand createCommand(DBDatabase db);

    /**
     * This function gives the driver a chance to provide a custom implementation 
     * for a combined command such as UNION or INTERSECT 
     * @param left the left command
     * @param keyWord the key word (either "UNION" or "INTERSECT")
     * @param left the right command
     * @return a DBCommandExpr object
     */
    DBCommandExpr createCombinedCommand(DBCommandExpr left, String keyWord, DBCommandExpr right);

    /**
     * Returns whether or not a particular feature is supported by this driver
     * @param type type of requested feature. @see DBDriverFeature
     * @return true if the features is supported or false otherwise
     */
    boolean isSupported(DBDriverFeature type);

    /**
     * Appends a table, view or column name to an SQL phrase. 
     * 
     * @param sql the StringBuilder containing the SQL phrase.
     * @param name the name of the object (table, view or column)
     * @param useQuotes use quotes or not. When null is passed then detectQuoteName() is called
     */
    void appendElementName(StringBuilder sql, String name, Boolean useQuotes);
    
    /**
     * Returns an sql phrase template for this database system.<br>
     * Templates for sql function expressions must contain a '?' character which will be 
     * replaced by the current column expression.<br>
     * If other parameters are necessary the template must contain placeholders like {0}, {1} etc. 
     * @param phrase the identifier of the phrase  
     * @return the phrase template
     */
    String getSQLPhrase(int phrase);

    /**
     * Returns a data type convertion phrase template for this driver<br>
     * The returned template must contain a '?' which will be replaced by a column expression.
     * @param destType the target data type
     * @param srcType the source data type
     * @param format additional formatting information (optional) 
     * @return the data conversion phrase template
     */
    String getConvertPhrase(DataType destType, DataType srcType, Object format);

    /**
     * Creates a sql string for a given value. 
     * Text will be enclosed in single quotes and existing single quotes will be doubled.
     * Empty strings are treated as null.
     * Syntax of Date, Datetime and Boolean values are vendor specific.
     * 
     * @param value the value which is inserted to the new String
     * @param type the sql data type of the supplied value
     * @return the sql string representing this value
     */
    String getValueString(Object value, DataType type);

    /**
     * Returns a DMBS-Timestamp that is used for record updates.
     * @param conn the connection that might be used 
     * @return the current date and time.
     */
    Timestamp getUpdateTimestamp(Connection conn);
    
    /**
     * Returns an auto-generated value for a particular column
     * 
     * @param db the database
     * @param column the column for which a value is required
     * @param conn a valid database connection
     * @return the auto-generated value
     */
    Object getColumnAutoValue(DBDatabase db, DBTableColumn column, Connection conn);
    
    /**
     * Executes an select SQL-command that returns only one scalar value
     * 
     * @param sqlCmd the SQL-Command
     * @param sqlParams array of sql command parameters used for prepared statements (Optional).
     * @param dataType the requested return type
     * @param conn a valid connection to the database.
     * @return the scalar result value
     */
    public Object querySingleValue(String sqlCmd, Object[] sqlParams, DataType dataType, Connection conn);
    
    /**
     * This interface is used to set the auto generated keys when executing insert statements.
     */
    public interface DBSetGenKeys
    {
        void set(Object value);
    }
    
    /**
     * Executes an insert, update or delete SQL-command
     * 
     * @param sqlCmd the SQL-Command
     * @param sqlParams array of sql command parameters used for prepared statements (Optional).
     * @param conn a valid connection to the database.
     * @param genKeys allows to set the auto generated key of a record (INSERT statements only)
     * 
     * @return the row count for insert, update or delete or 0 for SQL statements that return nothing
     * 
     * @throws SQLException if a database access error occurs
     */
    int executeSQL(String sqlCmd, Object[] sqlParams, Connection conn, DBSetGenKeys genKeys)
        throws SQLException;

    /**
     * Executes a list of sql statements as batch
     * @param sqlCmd
     * @param sqlCmdParams
     * @param conn
     * @return
     * @throws SQLException
     */
    public int[] executeBatch(String[] sqlCmd, Object[][] sqlCmdParams, Connection conn)
        throws SQLException;
    
    /**
     * Executes an select SQL-command and returns the query results
     * 
     * @param sqlCmd the SQL-Command
     * @param sqlParams array of sql command parameters used for prepared statements (Optional).
     * @param scrollable true if scrollable or false otherwise
     * @param conn a valid connection to the database.
     * @return the JDBC resultset
     * @throws SQLException
     */
    public ResultSet executeQuery(String sqlCmd, Object[] sqlParams, boolean scrollable, Connection conn)
        throws SQLException;

    /**
     * <P>
     * Reads a single column value from the given JDBC ResultSet and returns a value object of desired data type.<BR> 
     * See {@link DBExpr#getValueClass(DataType)} for java class type mapping.
     * <P>
     * This gives the driver the opportunity to change the value
     * i.e. to simulate missing data types with other types.
     * <P>
     * @param rset the sql Resultset with the current data row
     * @param columnIndex one based column Index of the desired column
     * @param dataType the required data type
     * 
     * @return the value of the Column 
     * 
     * @throws SQLException if a database access error occurs
     */
    Object getResultValue(ResultSet rset, int columnIndex, DataType dataType)
        throws SQLException;

    /**
     * Closes the provided JDBC Resultset
     * Use it instead of rset.close() and stmt.close() 
     * <P>
     * @param rset a ResultSet object
     */
    void closeResultSet(ResultSet rset);

    /**
     * Appends the required DLL commands to create, drop or alter an object to the supplied DBDQLScript.
     * @param type operation to perform (CREATE, DROP, ALTER)
     * @param dbo the object for which to perform the operation (DBDatabase, DBTable, DBView, DBColumn, DBRelation) 
     * @param script the script to which to add the DDL command(s)
     */
    void getDDLScript(DBCmdType type, DBObject dbo, DBSQLScript script);
    
    /**
     * Appends a statement to enable or disable a foreign key relation.<br>
     * The default is to drop or create the relation 
     * Override this method to provide different behavior for your database.
     * @param r the foreign key relation which should be enabled or disabled
     * @param enable true to enable the relation or false to disable
     * @param script the script to which to add the DDL command(s)
     */
    void addEnableRelationStmt(DBRelation r, boolean enable, DBSQLScript script);
     
    /**
     * Extracts native error message of an sqlExeption.
     * 
     * @param e the SQLException
     * @return the error message of the database 
     */
    String extractErrorMessage(SQLException e);
 
}