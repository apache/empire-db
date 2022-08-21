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
package org.apache.empire.dbms;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.apache.empire.data.DataType;
import org.apache.empire.db.DBCommand;
import org.apache.empire.db.DBCommandExpr;
import org.apache.empire.db.DBDDLGenerator.DDLActionType;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.DBExpr;
import org.apache.empire.db.DBObject;
import org.apache.empire.db.DBRelation;
import org.apache.empire.db.DBSQLBuilder;
import org.apache.empire.db.DBSQLScript;
import org.apache.empire.db.DBTableColumn;
import org.apache.empire.db.validation.DBModelChecker;
import org.apache.empire.db.validation.DBModelParser;

/**
 * The DBMSHandler interface implements all RDBMS specific logic
 */
public interface DBMSHandler
{
    /**
     * Checks if a database exists
     */
    boolean checkExists(DBDatabase db, Connection conn);
    
    /**
     * Called when a database is opened
     */
    void attachDatabase(DBDatabase db, Connection conn);

    /**
     * Called when a database is closed
     */
    void detachDatabase(DBDatabase db, Connection conn);
    
    /**
     * This function creates a DBSQLBuilder for this DBMS
     * @return a DBMS specific DBSQLBuilder object
     */
    DBSQLBuilder createSQLBuilder();

    /**
     * This function creates a DBCommand for this DBMS
     * @param autoPrepareStmt whether or not the Command should automatically generate a prepared statement (using ?)
     * @return a DBMS specific DBCommand object
     */
    DBCommand createCommand(boolean autoPrepareStmt);

    /**
     * This function gives the dbms a chance to provide a custom implementation 
     * for a combined command such as UNION or INTERSECT 
     * @param left the left command
     * @param keyWord the key word (either "UNION" or "INTERSECT")
     * @param left the right command
     * @return a DBCommandExpr object
     */
    DBCommandExpr createCombinedCommand(DBCommandExpr left, String keyWord, DBCommandExpr right);

    /**
     * Returns whether or not a particular feature is supported by this dbms
     * @param type type of requested feature. @see DBMSFeature
     * @return true if the features is supported or false otherwise
     */
    boolean isSupported(DBMSFeature type);

    /**
     * Appends a table, view or column name to an SQL phrase. 
     * 
     * @param sql the StringBuilder containing the SQL phrase.
     * @param name the name of the object (table, view or column)
     * @param useQuotes use quotes or not. When null is passed then detectQuoteName() is called
     */
    void appendObjectName(DBSQLBuilder sql, String name, Boolean useQuotes);
    
    /**
     * Returns an sql phrase template for this database system.<br>
     * Templates for sql function expressions must contain a '?' character which will be 
     * replaced by the current column expression.<br>
     * If other parameters are necessary the template must contain placeholders like {0}, {1} etc. 
     * @param phrase the identifier of the phrase  
     * @return the phrase template
     */
    String getSQLPhrase(DBSqlPhrase phrase);

    /**
     * Returns a data type convertion phrase template for this dbms<br>
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
     * if no row are returned by the query then ObjectUtils.NO_VALUE is returned
     * 
     * @param sqlCmd the SQL-Command
     * @param sqlParams array of sql command parameters used for prepared statements (Optional).
     * @param dataType the requested return type
     * @param conn a valid connection to the database.
     * @return the scalar result value or ObjectUtils.NO_VALUE if no row are returned by the query
     */
    public Object querySingleValue(String sqlCmd, Object[] sqlParams, DataType dataType, Connection conn);
    
    /**
     * This interface is used to set the auto generated keys when executing insert statements.
     */
    public interface DBSetGenKeys
    {
        void set(int rownum, Object value);
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
     * This gives the dbms the opportunity to change the value
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
    void getDDLScript(DDLActionType type, DBObject dbo, DBSQLScript script);
    
    /**
     * Appends a statement to enable or disable a foreign key relation.<br>
     * The default is to drop or create the relation 
     * Override this method to provide different behavior for your database.
     * @param r the foreign key relation which should be enabled or disabled
     * @param enable true to enable the relation or false to disable
     * @param script the script to which to add the DDL command(s)
     */
    void appendEnableRelationStmt(DBRelation r, boolean enable, DBSQLScript script);
    
    /**
     * Creates a DataModelParser instance of this DBMSHandler
     * @return
     */
    DBModelParser createModelParser(String catalog, String schema);
 
    /**
     * Creates a DataModelChecker instance of this DBMSHandler
     * @return
     */
    DBModelChecker createModelChecker(DBDatabase db);
    
    /**
     * Extracts native error message of an sqlExeption.
     * 
     * @param e the SQLException
     * @return the error message of the database 
     */
    String extractErrorMessage(SQLException e);
 
}