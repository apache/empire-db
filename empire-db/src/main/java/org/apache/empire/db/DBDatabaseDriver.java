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

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.apache.empire.commons.DateUtils;
import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.commons.StringUtils;
import org.apache.empire.data.DataMode;
import org.apache.empire.data.DataType;
import org.apache.empire.db.exceptions.EmpireSQLException;
import org.apache.empire.exceptions.InvalidArgumentException;
import org.apache.empire.exceptions.NotImplementedException;
import org.apache.empire.exceptions.NotSupportedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The DBDatabaseDriver class is an abstract base class for all database drivers.
 * Its purpose is to handle everything that is - or might be - database vendor specific. 
 */
public abstract class DBDatabaseDriver implements Serializable
{
    private final static long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(DBDatabaseDriver.class);
  
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
    public static final int SQL_BOOLEAN_TRUE     = 10;  // Oracle: "'Y'"; MSSQL: "1"
    public static final int SQL_BOOLEAN_FALSE    = 11;  // Oracle: "'N'"; MSSQL: "0"
    public static final int SQL_CURRENT_DATE     = 20;  // Oracle: "sysdate"
    public static final int SQL_DATE_PATTERN     = 21;  // "yyyy.MM.dd"
    public static final int SQL_DATE_TEMPLATE    = 22;  // Oracle: "TO_DATE('{0}', 'YYYY-MM-DD')"
    public static final int SQL_CURRENT_DATETIME = 25;  // Oracle: "sysdate"
    public static final int SQL_DATETIME_PATTERN = 26;  // "yyyy.MM.dd HH:mm:ss"
    public static final int SQL_DATETIME_TEMPLATE= 27;  // Oracle: "TO_DATE('{0}', 'YYYY-MM-DD HH24:MI:SS')"
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
    
    // Flag whether or not to set column defaults when crating DDL statements
    protected boolean ddlColumnDefaults = false;

    // Illegal name chars and reserved SQL keywords
    protected static final char[]   ILLEGAL_NAME_CHARS   = new char[] { '@', '?', '>', '=', '<', ';', ':', 
                                                                    '/', '.', '-', ',', '+', '*', ')', '(',
                                                                    '\'', '&', '%', '!', ' '
                                                                  };        
    protected static final String[] GENERAL_SQL_KEYWORDS = new String[] { "user", "group", 
                                                           "table", "column", "view", "index", "constraint", 
                                                           "select", "udpate", "insert", "alter", "delete", 
                                                           "order" };        
    protected final Set<String> reservedSQLKeywords;

    /**
     * This interface is used to set the auto generated keys when executing insert statements.
     */
    public interface DBSetGenKeys
    {
        void set(Object value);
    }
    
    /**
     * This class is used to emulate sequences by using a sequence table.
     * It is used with the executeSQL function and only required for insert statements
     */
    public static class DBSeqTable extends DBTable
    {
        private final static long serialVersionUID = 1L;
      
        public DBColumn C_SEQNAME;
        public DBColumn C_SEQVALUE;
        public DBColumn C_TIMESTAMP;

        /**
         * Constructor
         * 
         * @param tableName the table name
         * @param db the database object
         */
        public DBSeqTable(String tableName, DBDatabase db)
        {
            super(tableName, db);
            // Add all Colums
            C_SEQNAME   = addColumn("SeqName",  DataType.TEXT,      40, DataMode.NotNull);
            C_SEQVALUE  = addColumn("SeqValue", DataType.INTEGER,    0, DataMode.NotNull);
            C_TIMESTAMP = addColumn("SeqTime",  DataType.DATETIME,   0, DataMode.NotNull);
            // Primary Key
            setPrimaryKey(new DBColumn[] { C_SEQNAME });
        }

        // Overrideable
        public Object getNextValue(String SeqName, long minValue, Connection conn)
        {
            DBDatabaseDriver driver = db.getDriver();
            // Create a Command
            PreparedStatement stmt = null;
            try
            {   // The select Statement
                DBCommand cmd = driver.createCommand(db);
                DBCmdParam nameParam = cmd.addParam(SeqName);
                cmd.select(C_SEQVALUE);
                cmd.select(C_TIMESTAMP);
                cmd.where (C_SEQNAME.is(nameParam));
                String selectCmd = cmd.getSelect();
                // Get the next Value
                long seqValue = 0;
                while (seqValue == 0)
                {
                    // stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
                    stmt = conn.prepareStatement(selectCmd, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
                    stmt.setString(1, SeqName);
                    // Query existing value
                    ResultSet rs = stmt.executeQuery();
                    if (rs.next())
                    { // Read the Sequence Value
                        seqValue = Math.max(rs.getLong(1) + 1, minValue);
                        java.sql.Timestamp current = rs.getTimestamp(2);
                        db.closeResultSet(rs);
                        // Update existing Record
                        cmd.clear();
                        DBCmdParam name = cmd.addParam(SeqName);
                        DBCmdParam time = cmd.addParam(current);
                        cmd.set(C_SEQVALUE.to(seqValue));
                        cmd.set(C_TIMESTAMP.to(DBDatabase.SYSDATE));
                        cmd.where(C_SEQNAME.is(name));
                        cmd.where(C_TIMESTAMP.is(time));
                        if (driver.executeSQL(cmd.getUpdate(), cmd.getParamValues(), conn, null) < 1)
                            seqValue = 0; // Try again
                    } 
                    else
                    { // Close Reader
                        db.closeResultSet(rs);
                        // sequence does not exist
                        seqValue = minValue;
                        log.warn("Sequence {} does not exist! Creating sequence with start-value of {}", SeqName, seqValue);
                        // create a new sequence entry
                        cmd.clear();
                        cmd.set(C_SEQNAME.to(SeqName));
                        cmd.set(C_SEQVALUE.to(seqValue));
                        cmd.set(C_TIMESTAMP.to(DBDatabase.SYSDATE));
                        if (driver.executeSQL(cmd.getInsert(), cmd.getParamValues(), conn, null) < 1)
                            seqValue = 0; // Try again
                    }
                    // check for concurrency problem
                    if (seqValue == 0)
                        log.warn("Failed to increment sequence {}. Trying again!", SeqName);
                    // close
                    db.closeStatement(stmt);
                    cmd.clear();
                    rs = null;
                }
                if (log.isInfoEnabled())
                    log.info("Sequence {} incremented to {}.", SeqName, seqValue);
                return new Long(seqValue);
            } catch (SQLException e) {
                // throw exception
                throw new EmpireSQLException(this, e);
            } finally
            { // Cleanup
                db.closeStatement(stmt);
            }
        }
    }
    
    /**
     * Constructor
     */
    public DBDatabaseDriver()
    {
        // Initialize List of reserved Keywords
        reservedSQLKeywords = new HashSet<String>(GENERAL_SQL_KEYWORDS.length);
        for (String keyWord:GENERAL_SQL_KEYWORDS){
             reservedSQLKeywords.add(keyWord);
        }
    }

    /**
     * This function creates a DBCommand derived object this database
     * @param db the database for which to create a command object for
     * @return a DBCommand object
     */
    public abstract DBCommand createCommand(DBDatabase db);

    /**
     * This function gives the driver a chance to provide a custom implementation 
     * for a combined command such as UNION or INTERSECT 
     * @param left the left command
     * @param keyWord the key word (either "UNION" or "INTERSECT")
     * @param left the right command
     * @return a DBCommandExpr object
     */
    public DBCommandExpr createCombinedCommand(DBCommandExpr left, String keyWord, DBCommandExpr right)
    {
    	return new DBCombinedCmd(left, keyWord, right);
    }

    /**
     * Returns whether or not a particular feature is supported by this driver
     * @param type type of requested feature. @see DBDriverFeature
     * @return true if the features is supported or false otherwise
     */
    public abstract boolean isSupported(DBDriverFeature type);

    /**
     * Detects whether a table or column name needs to be quoted or not<br>
     * By default all reserved SQL keywords as well as names 
     * containing a "-", "/", "+" or " " require quoting.<br>
     * Overrides this function to add database specific keywords like "user" or "count"  
     */
    protected boolean detectQuoteName(String name)
    {
        // Check for reserved names
        if (reservedSQLKeywords.contains(name.toLowerCase()))
            return true;
        // Check for illegalNameChars
        int len = name.length();
        for (int i=0; i<len; i++)
        {   char c = name.charAt(i);
            for (int j=0; j<ILLEGAL_NAME_CHARS.length; j++)
            {   char ic = ILLEGAL_NAME_CHARS[j]; 
                if (c>ic)
                    break;
                if (c==ic)
                    return true;
            }    
        }
        // Quoting not necessary
        return false;
    }

    /**
     * Appends a table, view or column name to an SQL phrase. 
     * 
     * @param sql the StringBuilder containing the SQL phrase.
     * @param name the name of the object (table, view or column)
     * @param useQuotes use quotes or not
     */
    public void appendElementName(StringBuilder sql, String name, boolean useQuotes)
    {
        // Check whether to use quotes or not
        if (useQuotes)
            sql.append(getSQLPhrase(DBDatabaseDriver.SQL_QUOTES_OPEN));
        // Append Name
        sql.append(name);
        // End Quotes
        if (useQuotes)
            sql.append(getSQLPhrase(DBDatabaseDriver.SQL_QUOTES_CLOSE));
    }

    /**
     * Appends a table, view or column name to an SQL phrase. 
     * @param sql the StringBuilder containing the SQL phrase.
     * @param name the name of the object (table, view or column)
     */
    public final void appendElementName(StringBuilder sql, String name)
    {
        appendElementName(sql, name, detectQuoteName(name));
    }
    
    /**
     * Returns an sql phrase template for this database system.<br>
     * Templates for sql function expressions must contain a '?' character which will be 
     * replaced by the current column expression.<br>
     * If other parameters are necessary the template must contain placeholders like {0}, {1} etc. 
     * @param phrase the identifier of the phrase  
     * @return the phrase template
     */
    public abstract String getSQLPhrase(int phrase);

    /**
     * Returns a data type convertion phrase template for this driver<br>
     * The returned template must contain a '?' which will be replaced by a column expression.
     * @param destType the target data type
     * @param srcType the source data type
     * @param format additional formatting information (optional) 
     * @return the data conversion phrase template
     */
    public abstract String getConvertPhrase(DataType destType, DataType srcType, Object format);

    /**
     * Returns the next value of a named sequence The numbers are used for fields of type DBExpr.DT_AUTOINC.<BR>
     * If a driver supports this function it must return true for isSupported(DBDriverFeature.SEQUENCES).
     * 
     * @param db the database
     * @param SeqName the name of the sequence
     * @param minValue the minimum value of the sequence
     * @param conn a valid database connection
     * @return a new unique sequence value or null if an error occurred
     */
    public abstract Object getNextSequenceValue(DBDatabase db, String SeqName, int minValue, Connection conn);
    
    /**
     * Returns an expression for creating a sequence value.
     * This is intended for the use with INSERT INTO statements where many records are affected. 
     * @param col the column for which to obtain an expression providing the next sequence value
     * @return an expression for the next sequence value
     */
    public abstract DBColumnExpr getNextSequenceValueExpr(DBTableColumn column);
    
    /**
     * Returns an auto-generated value for a particular column
     * 
     * @param db the database
     * @param column the column for which a value is required
     * @param conn a valid database connection
     * @return the auto-generated value
     */
    public Object getColumnAutoValue(DBDatabase db, DBTableColumn column, Connection conn)
    {
        // Supports sequences?
        DataType type = column.getDataType();
        if (type == DataType.AUTOINC)
        {   // Use a numeric sequence
            if (isSupported(DBDriverFeature.SEQUENCES)==false)
                return null; // Create Later
            String SeqName = column.getSequenceName();
            return db.getNextSequenceValue(SeqName, conn);
        }
        else if (type== DataType.UNIQUEID)
        {   // emulate using java.util.UUID
            return UUID.randomUUID();
        }
        else if ((type==DataType.DATE || type==DataType.DATETIME))
        {   // Get database system's date and time
            Date ts = db.getUpdateTimestamp(conn);
            return (type==DataType.DATE ? DateUtils.getDateOnly(ts) : ts);
        }
        // Other types
        throw new NotSupportedException(this, "getColumnAutoValue() for "+type);
    }

    /**
     * Prepares an sql statement by setting the supplied objects as parameters.
     * 
     * @param pstmt the prepared statement
     * @param sqlParams list of objects
     */
    protected void prepareStatement(PreparedStatement pstmt, Object[] sqlParams) 
    	throws SQLException
	{
        for (int i=0; i<sqlParams.length; i++)
        {
            Object value = sqlParams[i];
            try {
                addStatementParam(pstmt, i+1, value); // , conn
            } catch(SQLException e) {
                log.error("SQLException: Unable to set prepared statement parameter {} to '{}'", i+1, StringUtils.toString(value));
                throw e;
            }
        }
	}

    /**
     * Adds a statement parameter to a prepared statement
     * 
     * @param pstmt the prepared statement
     * @param paramIndex the parameter index
     * @param value the parameter value
     */
    protected void addStatementParam(PreparedStatement pstmt, int paramIndex, Object value)
		throws SQLException
	{
        if (value instanceof DBBlobData)
        {
            // handling for blobs
            DBBlobData blobData = (DBBlobData)value;
            pstmt.setBinaryStream(paramIndex, blobData.getInputStream(), blobData.getLength());
            // log
            if (log.isDebugEnabled())
                log.debug("Statement param {} set to BLOB data", paramIndex);
        }
        else if(value instanceof DBClobData)
        {
            // handling for clobs
            DBClobData clobData = (DBClobData)value;
            pstmt.setCharacterStream(paramIndex, clobData.getReader(), clobData.getLength());
            // log
            if (log.isDebugEnabled())
                log.debug("Statement param {} set to CLOB data", paramIndex);
        }
        else if(value instanceof Date && !(value instanceof Timestamp))
        {
            // handling for dates
            Timestamp ts = new Timestamp(((Date)value).getTime());
            pstmt.setObject(paramIndex, ts);
            // log
            if (log.isDebugEnabled())
                log.debug("Statement param {} set to date '{}'", paramIndex, ts);
        }
        else if((value instanceof Character) 
        	 || (value instanceof Enum<?>))
        {
            // Objects that need String conversion
            String strval = value.toString();
            pstmt.setObject(paramIndex, strval);
            // log
            if (log.isDebugEnabled())
                log.debug("Statement param {} set to '{}'", paramIndex, strval);
        }
        else
        {   // simple parameter value 
            pstmt.setObject(paramIndex, value);
            // log
            if (log.isDebugEnabled())
                log.debug("Statement param {} set to '{}'", paramIndex, value);
        }
	}
    
    /**
     * Extracts native error message of an sqlExeption.
     * 
     * @param e the SQLException
     * @return the error message of the database 
     */
    public String extractErrorMessage(SQLException e)
    {
        return e.getMessage();
    }
    
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
    public Object getResultValue(ResultSet rset, int columnIndex, DataType dataType)
        throws SQLException
    {
        if (dataType == DataType.DATETIME)
        { // Get Timestamp (do not use getObject()!) 
            return rset.getTimestamp(columnIndex);
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
     * Executes the select, update or delete SQL-Command with a Statement object.
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
    public int executeSQL(String sqlCmd, Object[] sqlParams, Connection conn, DBSetGenKeys genKeys)
        throws SQLException
    {   // Execute the Statement
        Statement stmt = null;
        try
        {
            int count = 0;
            if (sqlParams!=null)
            {   // Use a prepared statement
                PreparedStatement pstmt = (genKeys!=null) 
                    ? conn.prepareStatement(sqlCmd, Statement.RETURN_GENERATED_KEYS)
                    : conn.prepareStatement(sqlCmd);
    	        stmt = pstmt;
	            prepareStatement(pstmt, sqlParams); 
	            count = pstmt.executeUpdate(); 
            }
            else
            {   // Execute a simple statement
                stmt = conn.createStatement();
                count = (genKeys!=null)
                    ? stmt.executeUpdate(sqlCmd, Statement.RETURN_GENERATED_KEYS)
                    : stmt.executeUpdate(sqlCmd);
            }
            // Retrieve any auto-generated keys
            if (genKeys!=null && count>0)
            {   // Return Keys
                ResultSet rs = stmt.getGeneratedKeys();
                try {
                    while(rs.next())
                    {
                        genKeys.set(rs.getObject(1));
                    }
                } finally {
                    rs.close();
                }
            }
            // done
            return count;
        } finally
        {
            close(stmt);
        }
    }

    /**
     * Executes a list of sql statements as batch
     * @param sqlCmd
     * @param sqlCmdParams
     * @param conn
     * @return
     * @throws SQLException
     */
    public int[] executeBatch(String[] sqlCmd, Object[][] sqlCmdParams, Connection conn)
        throws SQLException
    {   // Execute the Statement
        if (sqlCmdParams!=null)
        {   // Use a prepared statement
        	PreparedStatement pstmt = null;
	        try
	        {
            	int pos=0;
            	String lastCmd = null;
            	int[] result = new int[sqlCmd.length];
            	for (int i=0; i<=sqlCmd.length; i++)
            	{	// get cmd
            		String cmd = (i<sqlCmd.length ? sqlCmd[i] : null);
            		if (StringUtils.compareEqual(cmd, lastCmd, true)==false)
            		{	// close last statement
            			if (pstmt!=null)
            			{	// execute and close
			        		log.debug("Executing batch containing {} statements", i-pos);
            				int[] res = pstmt.executeBatch();
            				for (int j=0; j<res.length; j++)
            					result[pos+j]=res[j];
            				pos+=res.length;
            				close(pstmt);
            				pstmt = null;
            			}
            			// has next?
	        			if (cmd==null)
	        				break;
	        			// new statement
	        			log.debug("Creating prepared statement for batch: "+cmd);
            			pstmt = conn.prepareStatement(cmd);
            			lastCmd = cmd;
            		}
            		// add batch
            		if (sqlCmdParams[i]!=null)
            		{	
            			prepareStatement(pstmt, sqlCmdParams[i]); 
            		}	
        			log.debug("Adding batch with {} params.", (sqlCmdParams[i]!=null ? sqlCmdParams[i].length : 0));
            		pstmt.addBatch();
            	}
	            return result; 
            } finally {
	            close(pstmt);
	        }
        }
        else
        {   // Execute a simple statement
            Statement stmt = conn.createStatement();
            try {
            	for (int i=0; i<sqlCmd.length; i++)
            	{
            		String cmd = sqlCmd[i];
            		log.debug("Adding statement to batch: "+cmd);
            		stmt.addBatch(cmd);
            	}
        		log.debug("Executing batch containing {} statements", sqlCmd.length);
	            int result[] = stmt.executeBatch();
	            return result;
            } finally {
	            close(stmt);
	        }
        }
    }
    
    // executeQuery
    public ResultSet executeQuery(String sqlCmd, Object[] sqlParams, boolean scrollable, Connection conn)
        throws SQLException
    {
        Statement stmt = null;
        try
        {   // Set scroll type
            int type = (scrollable ? ResultSet.TYPE_SCROLL_INSENSITIVE
                                   : ResultSet.TYPE_FORWARD_ONLY);
            // Create an execute a query statement
	        if (sqlParams!=null)
	        {	// Use prepared statement
	            PreparedStatement pstmt = conn.prepareStatement(sqlCmd, type, ResultSet.CONCUR_READ_ONLY);
	            stmt = pstmt;
	            prepareStatement(pstmt, sqlParams); 
	            return pstmt.executeQuery();
	        } else
	        {	// Use simple statement
	            stmt = conn.createStatement(type, ResultSet.CONCUR_READ_ONLY);
	            return stmt.executeQuery(sqlCmd);
	        }
        } catch(SQLException e) {
            // close statement (if not null)
            log.error("Error executing query '"+sqlCmd+"' --> "+e.getMessage(), e);
            close(stmt);
            throw e;
        }
    }
    
    // close
    protected void close(Statement stmt)
    {
        try
        { // Statement close
            if (stmt != null)
                stmt.close();
        } catch (SQLException sqle) 
        {
            log.error("close statement:" + sqle.toString());
        }
    }
    
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
    public String getValueString(Object value, DataType type)
    { 
        if (ObjectUtils.isEmpty(value))
        {
            return getSQLPhrase(SQL_NULL_VALUE);
        }
        // set string buffer
        switch (type)
        {
            case DATE:
                return getSQLDateTimeString(value, SQL_DATE_TEMPLATE, SQL_DATE_PATTERN, SQL_CURRENT_DATE);
            case DATETIME:
                // System date is special case
                if (!DBDatabase.SYSDATE.equals(value) && value.toString().length()<=10)
                    return getSQLDateTimeString(value, SQL_DATE_TEMPLATE, SQL_DATE_PATTERN, SQL_CURRENT_DATETIME);
                // Complete Date-Time Object with time 
                return getSQLDateTimeString(value, SQL_DATETIME_TEMPLATE, SQL_DATETIME_PATTERN, SQL_CURRENT_DATETIME);
            case TEXT:
            case CHAR:
            case CLOB:
            case UNIQUEID:
            {   // Text value
                return getSQLTextString(type, value);
            }
            case BOOL:
            {   // Get Boolean value   
                boolean boolVal = false;
                if (value instanceof Boolean)
                {   boolVal = ((Boolean) value).booleanValue();
                } 
                else
                { // Boolean from String
                    boolVal = stringToBoolean(value.toString());
                }
                return getSQLPhrase((boolVal) ? SQL_BOOLEAN_TRUE : SQL_BOOLEAN_FALSE);
            }
            case INTEGER:
            case DECIMAL:
            case FLOAT:
                return getSQLNumberString(value, type);
            case BLOB:
                throw new NotSupportedException(this, "getValueString(?, DataType.BLOB)"); 
            case AUTOINC:
            case UNKNOWN:
                /* Allow expressions */
                return value.toString();
            default:
                log.warn("Unknown DataType {} for getValueString().", type);
                return value.toString();
        }
    }
    
    /**
     * encodes a numeric value for an SQL command string. 
     * @param value the numeric value
     * @param type the number data type
     * @return the string reprentation of the number
     */
    protected String getSQLNumberString(Object value, DataType type)
    {
        // already a number
        if (value instanceof Number)
            return value.toString();
        
        // check if it is a number
        String s = value.toString();
        boolean integerOnly = (type==DataType.INTEGER);
        for (int i=0; i<s.length(); i++)
        {
            char c = s.charAt(i);
            if (c>='0' && c<='9')
                continue; // OK
            if (c=='-' || c=='+')
                continue; // OK
            if (c==' ' && i>0)
                return s.substring(0,i);
            // check 
            if (integerOnly || (c!='.' && c!=','))
                throw new NumberFormatException(s);
        }
        return s;
    }

    /**
     * encodes a Date value for an SQL command string. 
     * @param value
     * @param sqlTemplate
     * @param sqlPattern
     * @param sqlCurrentDate
     * @return
     */
    protected String getSQLDateTimeString(Object value, int sqlTemplate, int sqlPattern, int sqlCurrentDate)
    {
        // is it a sysdate expression
        if (DBDatabase.SYSDATE.equals(value))
            return getSQLPhrase(sqlCurrentDate);
        // Format the date (ymd)
        String datetime = value.toString(); 
        SimpleDateFormat sqlFormat = new SimpleDateFormat(getSQLPhrase(sqlPattern));
        if ((value instanceof Date)==false)
        {   // Convert String to Date
            try
            {	// init DateFormat
            	String dtValue   = value.toString().trim();
            	String dtPattern = DBDatabase.DATETIME_PATTERN.substring(0, Math.min(dtValue.length(), 24));  
                SimpleDateFormat sdFormat = new SimpleDateFormat(dtPattern);
                // Parse value
                sdFormat.setLenient(true);
                Date dt = sdFormat.parse(dtValue);
                // Format to SQL pattern
               	datetime = sqlFormat.format(dt);
            } catch (ParseException e) {
            	// Invalid date
                log.error("Unable to parse date value "+datetime, e);
                throw new InvalidArgumentException("value", value);
            }
        }
        else
        {   // Format the date as string
            datetime = sqlFormat.format((Date)value);
        }
        // Now Build String
        String template = getSQLPhrase(sqlTemplate);
        return StringUtils.replace(template, "{0}", datetime);
    }

    /**
     * encodes Text values for an SQL command string.
     * @param type date type (can only be TEXT, CHAR, CLOB and UNIQUEID)
     * @param value the text to be encoded
     * @return the encoded sql value
     */
    protected String getSQLTextString(DataType type, Object value)
    {
        StringBuilder valBuf = new StringBuilder();
        valBuf.append("'");
        if (DBDatabase.EMPTY_STRING.equals(value)==false)
            appendSQLTextValue(valBuf, value.toString());
        valBuf.append("'");
        return valBuf.toString();
    }

    /** 
     * this helper function doubles up single quotes for SQL 
     */
    protected void appendSQLTextValue(StringBuilder buf, String value)
    {
        if (value.indexOf('\'') >= 0)
        { // a routine to double up single quotes for SQL
            int len = value.length();
            for (int i = 0; i < len; i++)
            {
                if (value.charAt(i) == '\'')
                    buf.append("''");
                else
                    buf.append(value.charAt(i));
            }
        } 
        else
        {
            buf.append(value);
        }
    }

    /**
     * this function converts a string containing a boolean expression to a boolean. 
     * @param value the string containing a boolean expression
     * @return true if the string contains either "true", "y" or "1" or false otherwise
     */
    protected boolean stringToBoolean(final String value) 
    {
        return "1".equals(value) ||
               "true".equalsIgnoreCase(value) ||
               "y".equalsIgnoreCase(value);
    }
    
    /**
     * Called when a database is opened
     */
    protected void attachDatabase(DBDatabase db, Connection conn)
    {
        // Override to implement attaching behaviour
    }

    /**
     * Called when a database is closed
     */
    protected void detachDatabase(DBDatabase db, Connection conn)
    {
        // Override to implement closing behaviour
    }

    /**
     * Checks the database whether or not it is consistent with the description.
     * 
     * @param db the database
     * @param owner the owner
     * @param conn the connection
     */
    public void checkDatabase(DBDatabase db, String owner, Connection conn)
    {
        throw new NotImplementedException(this, "checkDatabase");
    }
    
    /**
     * Appends the required DLL commands to create, drop or alter an object to the supplied DBDQLScript.
     * @param type operation to perform (CREATE, DROP, ALTER)
     * @param dbo the object for which to perform the operation (DBDatabase, DBTable, DBView, DBColumn, DBRelation) 
     * @param script the script to which to add the DDL command(s)
     */
    public void getDDLScript(DBCmdType type, DBObject dbo, DBSQLScript script)
    {
        throw new NotImplementedException(this, "getDDLScript");
    }
    
    /**
     * Appends a statement to enable or disable a foreign key relation.<br>
     * The default is to drop or create the relation 
     * Override this method to provide different behavior for your database.
     * @param r the foreign key relation which should be enabled or disabled
     * @param enable true to enable the relation or false to disable
     * @param script the script to which to add the DDL command(s)
     */
    public void addEnableRelationStmt(DBRelation r, boolean enable, DBSQLScript script)
    {
        if (enable)
            getDDLScript(DBCmdType.CREATE, r, script);
        else
            getDDLScript(DBCmdType.DROP, r, script);
    }
    
    /**
     * @return <code>true</code> if column default values are created with dll statements or <code>false</code> if not
     */
    public boolean isDDLColumnDefaults()
    {
        return ddlColumnDefaults;
    }

    /**
     * Set true if column default values should be included in DDL Statements  
     * 
     * @param ddlColumnDefaults <code>true</code> if dll statements should include 
     *   column default values or <code>false</code> if not
     */
    public void setDDLColumnDefaults(boolean ddlColumnDefaults)
    {
        this.ddlColumnDefaults = ddlColumnDefaults;
    }

    /**
     * Returns a timestamp that is used for record updates.
     * 
     * @param conn the connection that might be used 
     * 
     * @return the current date and time.
     */
    public java.sql.Timestamp getUpdateTimestamp(Connection conn)
    {
        // Default implementation
        java.util.Date date = new java.util.Date();
        return new java.sql.Timestamp(date.getTime());
    }

}