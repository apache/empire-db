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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.commons.Options;
import org.apache.empire.commons.StringUtils;
import org.apache.empire.data.Column;
import org.apache.empire.data.DataType;
import org.apache.empire.data.list.DataListEntry;
import org.apache.empire.data.list.DataListFactory;
import org.apache.empire.data.list.DataListFactoryImpl;
import org.apache.empire.data.list.DataListHead;
import org.apache.empire.db.context.DBContextAware;
import org.apache.empire.db.exceptions.CommandWithoutSelectException;
import org.apache.empire.db.exceptions.ConstraintViolationException;
import org.apache.empire.db.exceptions.QueryFailedException;
import org.apache.empire.db.exceptions.QueryNoResultException;
import org.apache.empire.db.exceptions.StatementFailedException;
import org.apache.empire.db.exceptions.UnknownBeanTypeException;
import org.apache.empire.db.expr.compare.DBCompareExpr;
import org.apache.empire.db.list.DBBeanFactoryCache;
import org.apache.empire.db.list.DBBeanListFactory;
import org.apache.empire.db.list.DBBeanListFactoryImpl;
import org.apache.empire.db.list.DBRecordListFactory;
import org.apache.empire.db.list.DBRecordListFactoryImpl;
import org.apache.empire.db.list.DataBean;
import org.apache.empire.dbms.DBMSFeature;
import org.apache.empire.dbms.DBMSHandler;
import org.apache.empire.exceptions.InternalException;
import org.apache.empire.exceptions.UnexpectedReturnValueException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DBUtils
 * This class provides various query functions and functions for command execution. 
 * It also performs logging
 * @author doebele
 */
public class DBUtils implements DBContextAware
{
    // Logger (Use logger from DBDatabase.class)
    protected static final Logger log = LoggerFactory.getLogger(DBUtils.class);
    
    // Threshold for long running queries in milliseconds
    protected long longRunndingStmtThreshold = 30000;
    // Default list capacity
    protected int DEFAULT_LIST_CAPACITY = 10; // Max-capacity before using ArrayList.DEFAULT_CAPACITY
    // Max-Rows for list queries
    protected int MAX_QUERY_ROWS  = 999;
    // Log max String length
    protected int LOG_MAX_STRING_LENGTH = 40;
    // Log New-Line
    protected String LOG_NEW_LINE = "\r\n";
    
    // the context
    protected final DBContext context;
    // the dbms
    protected final DBMSHandler dbms;
    
    /**
     * DBUtils constructor
     * @param context the database context 
     */
    public DBUtils(DBContext context)
    {
        this.context = context;
        this.dbms = context.getDbms();
    }

    /**
     * Returns the current Context
     * @return the database context
     */
    @Override
    public DBContext getContext()
    {
        return context;
    }

    /**
     * Get single parameter as string (for logging only)
     * @param param the parameter
     * @return the formatted parameter value
     */
    protected String paramValueToString(Object param)
    {
        if (param==null)
            return "NULL";
        DataType dataType = DataType.fromJavaType(param.getClass());
        if (dataType.isText())
        {   // text handling
            String str = param.toString();
            // limit length
            if (str.length()>LOG_MAX_STRING_LENGTH)
            {   StringBuilder b = new StringBuilder(LOG_MAX_STRING_LENGTH+10);
                b.append(str.substring(0, LOG_MAX_STRING_LENGTH));
                b.append("~(");
                b.append(String.valueOf(str.length()));
                b.append(")");
                str = b.toString();
            }
            // make sure param does not contain the separator
            if (str.indexOf('|')>=0)
                str = str.replace('|', '?');
            // done
            return str;
        }
        if (dataType==DataType.UNKNOWN ||
            dataType==DataType.BLOB ||
            dataType==DataType.CLOB)
        {   // get the class name
            return param.getClass().getName();
        }
        // just convert to String
        return String.valueOf(param);
    }
    
    /**
     * Get all parameters as string (for logging only)
     * @param params the parameter
     * @return the formatted parameters
     */
    protected String paramsToString(Object[] params)
    {
        if (params == null || params.length < 1)
            return null; // Empty
        if (params.length > 1)
        {   // more than one
            StringBuilder buf = new StringBuilder();
            for (int i = 0; i < params.length; i++)
            {
                if (i>0)
                    buf.append('|');
                // append
                buf.append(paramValueToString(params[i]));
            }
            return buf.toString();
        }
        // Only one parameter
        return paramValueToString(params[0]);
    }

    /**
     * Log Query Statement
     * @param sqlCmd the sql command
     * @param sqlParams the command params
     */
    protected void logQueryStatement(String sqlCmd, Object[] sqlParams)
    {
        if (log.isDebugEnabled())
        {   // Log with or without parameters   
            if (sqlParams!=null && sqlParams.length>0)
                log.debug("Executing DQL: {}{}{}with params: [{}]", LOG_NEW_LINE, sqlCmd, LOG_NEW_LINE, paramsToString(sqlParams));
            else
                log.debug("Executing DQL: {}{}", LOG_NEW_LINE, sqlCmd);
        }
    }

    /**
     * Log Update Statement
     * @param sqlCmd the sql command
     * @param sqlParams the command params
     */
    protected void logUpdateStatement(String sqlCmd, Object[] sqlParams)
    {
        if (log.isInfoEnabled())
        {   // Log with or without parameters   
            if (sqlParams!=null && sqlParams.length>0)
                log.info("Executing DML: {}{}{}with params: [{}]", LOG_NEW_LINE, sqlCmd, LOG_NEW_LINE, paramsToString(sqlParams));
            else
                log.info("Executing DML: {}{}", LOG_NEW_LINE, sqlCmd);
        }
    }

    /**
     * Executes an update, insert or delete SQL-Statement.<BR>
     * We recommend to use a DBCommand object in order to build the sqlCmd.<BR>
     * <P>
     * @param sqlCmd the SQL-Command
     * @param sqlParams a list of objects to replace sql parameters
     * @param setGenKeys callback to set the generated key for a each new record
     * @return the row count for insert, update or delete or 0 for SQL statements that return nothing
     */
    public int executeSQL(String sqlCmd, Object[] sqlParams, DBMSHandler.DBSetGenKeys setGenKeys)
    {
        try 
        {   // Debug
            logUpdateStatement(sqlCmd, sqlParams);
            // execute SQL
            long start = System.currentTimeMillis();
            int affected = dbms.executeSQL(sqlCmd, sqlParams, context.getConnection(), setGenKeys);
            // number of affected records
            if (affected < 0)
                log.warn("Unexpected return value {} from dbms.executeSQL(\"{}\")", affected, sqlCmd);
            // Log
            long execTime = (System.currentTimeMillis() - start);
            if (log.isInfoEnabled())
                log.info("executeSQL affected {} Records in {} ms ", affected, execTime);
            else if (execTime>=longRunndingStmtThreshold)
                log.warn("Long running statement took {} seconds for statement {}.", execTime / 1000, sqlCmd);
            // Return number of affected records
            return affected;
            
        } catch (SQLIntegrityConstraintViolationException sqle) {
            // ConstraintViolation
            throw new ConstraintViolationException(dbms, sqlCmd, sqle);
        } catch (SQLException sqle) {
            // Other error
            throw new StatementFailedException(dbms, sqlCmd, sqle);
        }    
    }
    
    /**
     * Executes a select SQL-Statement and returns a ResultSet containing the query results.<BR>
     * This function returns a JDBC ResultSet.<BR>
     * Instead of using this function directly you should use a DBReader object instead.<BR>
     * <P>
     * @param sqlCmd the SQL-Command
     * @param sqlParams a list of parameters for parameter queries (may depend on dbms)
     * @param scrollable true if the reader should be scrollable or false if not
     * @return the JDBC ResutSet
     */
    public ResultSet executeQuery(String sqlCmd, Object[] sqlParams, boolean scrollable)
    {
        try
        {   // Debug
            logQueryStatement(sqlCmd, sqlParams);
            // Execute the Statement
            long start = System.currentTimeMillis();
            ResultSet rs = dbms.executeQuery(sqlCmd, sqlParams, scrollable, context.getConnection());
            if (rs == null)
                throw new UnexpectedReturnValueException(rs, "dbms.executeQuery()");
            // Debug
            long queryTime = (System.currentTimeMillis() - start);
            if (log.isDebugEnabled())
                log.debug("executeQuery successful in {} ms", queryTime);
            else if (queryTime>=longRunndingStmtThreshold)
                log.warn("Long running query took {} seconds for statement {}.", queryTime / 1000, sqlCmd);
            // Return number of affected records
            return rs;
    
        } catch (SQLException sqle) 
        {   // Error
            throw new QueryFailedException(dbms, sqlCmd, paramsToString(sqlParams), sqle);
        } 
    }
    
    /**
     * Returns the value of the first row/column of a sql-query as an object.
     * If the query does not return a result the value ObjectUtils.NO_VALUE is returned.
     * 
     * @param sqlCmd the SQL-Command
     * @param sqlParams list of query parameter values
     * @param dataType the expected data type
     * @param failOnNoResult if true a QueryNoResultException result is thrown if no record exists otherwise null is returned
     * 
     * @return the value of the first column in the first row of the query 
     */
    public Object querySingleValue(String sqlCmd, Object[] sqlParams, DataType dataType, boolean failOnNoResult)
    {
        // Debug
        logQueryStatement(sqlCmd, sqlParams);
        // Read value
        try {
            long start = System.currentTimeMillis();
            Object result = dbms.querySingleValue(sqlCmd, sqlParams, dataType, context.getConnection());
            if (result==ObjectUtils.NO_VALUE)
            {   // Query returned no result
                if (failOnNoResult)
                    throw new QueryNoResultException(sqlCmd);
                else
                    result = null;
            }
            // Debug
            long queryTime = (System.currentTimeMillis() - start);
            if (log.isDebugEnabled())
                log.debug("querySingleValue successful in {} ms. Result value={}.", queryTime, result);
            else if (queryTime>=longRunndingStmtThreshold)
                log.warn("Long running query took {} seconds for statement {}.", queryTime / 1000, sqlCmd);
            // done
            return result;
        } catch (SQLException sqle) 
        {   // Error
            throw new QueryFailedException(dbms, sqlCmd, paramsToString(sqlParams), sqle);
        } 
    }
    
    /**
     * Returns the value of the first row/column of a sql-query as an object.
     * If the query does not return a result a QueryNoResultException is thrown
     * 
     * @param cmd the Command object that contains the select statement
     * @param resultType the expected data type
     * @param failOnNoResult flag whether to fail on empty resultset
     * 
     * @return the value of the first column in the first row of the query 
     */
    public final <T> T querySingleValue(DBCommandExpr cmd, Class<T> resultType, boolean failOnNoResult)
    {
        Object value = querySingleValue(cmd.getSelect(), cmd.getParamValues(), cmd.getDataType(), failOnNoResult); 
        return ObjectUtils.convert(resultType, value);
    }
    
    /**
     * Returns the value of the first row/column of a sql-query as an object.
     * If the query does not return a result a QueryNoResultException is thrown
     * 
     * @param cmd the Command object that contains the select statement
     * @param failOnNoResult flag whether to fail on empty resultset
     * 
     * @return the value of the first column in the first row of the query 
     */
    public final Object querySingleValue(DBCommandExpr cmd, boolean failOnNoResult)
    {
        return querySingleValue(cmd.getSelect(), cmd.getParamValues(), cmd.getDataType(), failOnNoResult);  
    }
    
    /**
     * Returns the value of the first row/column of a sql-query as an object.
     * If the query does not return a result a QueryNoResultException is thrown
     * 
     * @param cmd the Command object that contains the select statement
     * 
     * @return the value of the first column in the first row of the query 
     */
    public final Object querySingleValue(DBCommandExpr cmd)
    {
        return querySingleValue(cmd, true);  
    }

    /**
     * Returns the value of the first row/column of a sql-query as an int.
     * If the query does not return a result or if the query result is NULL, then the defaultValue is returned
     * 
     * @param sqlCmd the sql command
     * @param sqlParams the command params
     * @param defaultValue the default value 
     *
     * @return the result as a int value
     */
    public final int querySingleInt(String sqlCmd, Object[] sqlParams, int defaultValue)
    { 
        Object value = querySingleValue(sqlCmd, sqlParams, DataType.INTEGER, false);
        return ObjectUtils.getInteger(value, defaultValue);
    }

    /**
     * Returns the value of the first row/column of a sql-query as an int.
     * If the query does not return a result or if the query result is NULL, then the defaultValue is returned
     * 
     * @param cmd the Command object that contains the select statement
     * @param defaultValue the default value if no value was returned by the database
     *
     * @return the result as a int value
     */
    public final int querySingleInt(DBCommandExpr cmd, int defaultValue)
    { 
        Object value = querySingleValue(cmd.getSelect(), cmd.getParamValues(), DataType.INTEGER, false);
        return ObjectUtils.getInteger(value, defaultValue);
    }

    /**
     * Returns the value of the first row/column of a sql-query as an int.
     * If the query does not return a result a QueryNoResultException is thrown
     * 
     * @param cmd the Command object that contains the select statement
     *
     * @return the result as a int value
     */
    public final int querySingleInt(DBCommandExpr cmd)
    { 
        Object value = querySingleValue(cmd.getSelect(), cmd.getParamValues(), DataType.INTEGER, true);
        return ObjectUtils.getInteger(value);
    }

    /**
     * Returns the value of the first row/column of a sql-query as an int.
     * If the query does not return a result or if the query result is NULL, then the defaultValue is returned
     * 
     * @param sqlCmd the sql command
     * @param sqlParams the command params
     * @param defaultValue the default value 
     *
     * @return the result as a int value
     */
    public final long querySingleLong(String sqlCmd, Object[] sqlParams, long defaultValue)
    { 
        Object value = querySingleValue(sqlCmd, sqlParams, DataType.INTEGER, false);
        return ObjectUtils.getLong(value, defaultValue);
    }
    
    /**
     * Returns the value of the first row/column of a sql-query as a long.
     * If the query does not return a result or if the query result is NULL, then the defaultValue is returned
     * 
     * @param cmd the Command object that contains the select statement
     * @param defaultValue the default value
     * 
     * @return the result as a long value
     */
    public final long querySingleLong(DBCommandExpr cmd, long defaultValue)
    { 
        Object value = querySingleValue(cmd.getSelect(), cmd.getParamValues(), DataType.INTEGER, false);
        return ObjectUtils.getLong(value, defaultValue);
    }

    /**
     * Returns the value of the first row/column of a sql-query as a long.
     * If the query does not return a result a QueryNoResultException is thrown
     * 
     * @param cmd the Command object that contains the select statement
     *
     * @return the result as a long value
     */
    public final long querySingleLong(DBCommandExpr cmd)
    { 
        Object value = querySingleValue(cmd.getSelect(), cmd.getParamValues(), DataType.INTEGER, true);
        return ObjectUtils.getLong(value);
    }
    
    /**
     * Returns the value of the first row/column of a sql-query as a string.
     * If the query does not return a result or if the query result is NULL, then the defaultValue is returned
     * 
     * @param cmd the Command object that contains the select statement
     * @param defaultValue the default value if no value was returned by the database
     *
     * @return the result as a String object, if no result a empty String
     */
    public final String querySingleString(DBCommandExpr cmd, String defaultValue)
    { 
        Object value = querySingleValue(cmd.getSelect(), cmd.getParamValues(), DataType.VARCHAR, false);
        return StringUtils.toString(value, defaultValue);
    }
    
    /**
     * Returns the value of the first row/column of a sql-query as a string.
     * If the query does not return a result a QueryNoResultException is thrown.
     * If the query result is NULL an empty string is returned.
     * 
     * @param cmd the Command object that contains the select statement
     *
     * @return the result as a String object, if no result a empty String
     */
    public final String querySingleString(DBCommandExpr cmd)
    { 
        Object value = querySingleValue(cmd.getSelect(), cmd.getParamValues(), DataType.VARCHAR, true);
        return StringUtils.toString(value);
    }

    /**
     * Returns the number of rows returned by executing the select statement
     * @param cmd the select command
     * @return the number of rows that will be returned
     */
    public int queryRowCount(DBCommand cmd)
    {   // execute Native Query
        if (cmd==null || !cmd.isValid())
            return 0;
        // Check for aggregation
        boolean aggregate = false;
        DBColumnExpr[] exprList = cmd.getSelectExprList();
        for (int i=0; i<exprList.length; i++)
        {
            if (exprList[i].isAggregate())
            {   aggregate = true;
                break;
            }
        }
        // check if aggregate 
        if (aggregate)
        {   // For Aggregations: Wrap  
            String sql = "SELECT COUNT(*) FROM ("+cmd.getSelect(DBCommandExpr.SF_SKIP_ORDER) + ") q";
            return querySingleInt(sql, cmd.getParamValues(), 0);
        }
        // simple expression
        String sql = "SELECT COUNT(*) "+cmd.getSelect(DBCommandExpr.SF_SKIP_SELECT | DBCommandExpr.SF_SKIP_ORDER);
        return querySingleInt(sql, cmd.getParamValues(), 0);
    }

    /**
     * Adds the first column of a query result to a collection.
     * If the query has no result, an empty list is returned.
     * 
     * @param <T> the type for the list
     * @param c the class type for the list 
     * @param sqlCmd the sql command
     * @param sqlParams the command params
     * @param dataType the expected data type
     * @param result the reusult colletion
     * @param maxRows maximum number of rows or -1 for all rows
     * 
     * @return the number of elements that have been added to the collection 
     */
    public <T> int querySimpleList(Class<T> c, String sqlCmd, Object[] sqlParams, DataType dataType, Collection<T> result, int maxRows)
    {   // Start query
        ResultSet rs = null;
        try
        {
            logQueryStatement(sqlCmd, sqlParams);
            // Log performance
            long start = System.currentTimeMillis();
            // Get the next Value
            rs = dbms.executeQuery(sqlCmd, sqlParams, false, context.getConnection());
            if (rs == null)
                throw new UnexpectedReturnValueException(rs, "dbms.executeQuery()");
            // Check Result
            int count=0;
            while (rs.next() && (maxRows<0 || count<maxRows))
            {   
                T item = ObjectUtils.convert(c, dbms.getResultValue(rs, 1, dataType));
                result.add(item);
                count++;
            }
            // Debug
            long queryTime = (System.currentTimeMillis() - start);
            if (log.isDebugEnabled())
                log.debug("querySimpleList retured {} items in {} ms.", count, queryTime);
            else if (queryTime>=longRunndingStmtThreshold)
                log.warn("Long running query took {} seconds for statement {}.", queryTime / 1000, sqlCmd);
            // done
            return count;
        } catch (ClassCastException e) 
        {   log.error("querySingleValue cast exception: ", e);
            throw new InternalException(e);
        } catch (SQLException sqle) 
        {   // Error
            throw new QueryFailedException(dbms, sqlCmd, paramsToString(sqlParams), sqle);
        } finally
        { // Cleanup
            dbms.closeResultSet(rs);
        }
    }
    
    /**
     * Adds the first column of a query result to a collection.
     * If the query has no result, an empty list is returned.
     * 
     * @param c the class type for the list 
     * @param <T> the type for the list
     * @param cmd the Command object that contains the select statement
     * @param result the collection to which to add the result
     * 
     * @return the number of elements that have been added to the collection 
     */
    public final <T> int querySimpleList(Class<T> c, DBCommandExpr cmd, Collection<T> result)
    {
        return querySimpleList(c, cmd.getSelect(), cmd.getParamValues(), cmd.getDataType(), result, MAX_QUERY_ROWS); 
    }

    /**
     * Returns a one dimensional array from an sql query.
     * The array is filled with the values of the first column.
     * 
     * @param c the class type for the list 
     * @param <T> the type for the list
     * @param cmd the Command object that contains the select statement
     * 
     * @return a list of the values of the first column of an sql query 
     */
    public final <T> List<T> querySimpleList(Class<T> c, DBCommandExpr cmd)
    {   // Execute the  Statement
        List<T> result = new ArrayList<T>();
        if (querySimpleList(c, cmd, result)<0)
            return null;
        return result;
    }
    
    /**
     * Returns a one dimensional array from an sql query.
     * The array is filled with the values of the first column.
     * 
     * @param cmd the Command object that contains the select statement
     * @return a list of values of type Object 
     */
    public final List<Object> querySimpleList(DBCommandExpr cmd)
    {   // Execute the  Statement
        return querySimpleList(Object.class, cmd);
    }
    
    /**
     * Fills an option list provided with the result from a query.
     * The option list is filled with the values of the first and second column.
     * Optionally a third column may provide a boolean value for active or disabled.
     *
     * IMPORTANT: The query must contain unique values in the first column!
     * 
     * @param sqlCmd the sql command
     * @param sqlParams the command params
     * @param options the option list to where the options are added
     * @return an Options object containing a set a of values and their corresponding names 
     */
    public int queryOptionList(String sqlCmd, Object[] sqlParams, Options options)
    {   // Execute the  Statement
        ResultSet rs = null;
        try
        {
            logQueryStatement(sqlCmd, sqlParams);
            // Debug
            long start = System.currentTimeMillis();
            // Get the next Value
            rs = dbms.executeQuery(sqlCmd, sqlParams, false, context.getConnection());
            if (rs == null)
                throw new UnexpectedReturnValueException(rs, "dbms.executeQuery()");
            // Load options
            int colCount = rs.getMetaData().getColumnCount();
            int count = 0;
            while (rs.next())
            {
                Object value = rs.getObject(1);
                String text  = rs.getString((colCount>=2) ? 2 : 1);
                boolean active = (colCount>=3) ? ObjectUtils.getBoolean(rs.getObject(3)) : true;
                options.append(value, text, active);
                count++;
            }
            // Debug
            long queryTime = (System.currentTimeMillis() - start);
            if (log.isDebugEnabled())
                log.debug("queryOptionList retured {} items in {} ms.", count, queryTime);
            else if (queryTime>=longRunndingStmtThreshold)
                log.warn("Long running query took {} seconds for statement {}.", queryTime / 1000, sqlCmd);
            // done
            return count;
        } catch (SQLException sqle) 
        {   // Error
            throw new QueryFailedException(dbms, sqlCmd, paramsToString(sqlParams), sqle);
        } finally
        { // Cleanup
            dbms.closeResultSet(rs);
        }
    }
    
    /**
     * Fills an option list provided with the result from a query.
     * The option list is filled with the values of the first and second column.
     * 
     * @param cmd the Command object that contains the select statement
     * @param options the option list to where the options are added
     * @return an Options object containing a set a of values and their corresponding names 
     */
    public final int queryOptionList(DBCommandExpr cmd, Options options)
    {   // Execute the  Statement
        return queryOptionList(cmd.getSelect(), cmd.getParamValues(), options); 
    }

    /**
     * Returns a list of key value pairs from an sql query.
     * The option list is filled with the values of the first and second column.
     * 
     * @param cmd the Command object that contains the select statement
     * @return an Options object containing a set a of values and their corresponding names 
     */
    public final Options queryOptionList(DBCommandExpr cmd)
    {   // Execute the  Statement
        Options options = new Options();
        queryOptionList(cmd.getSelect(), cmd.getParamValues(), options);
        return options; 
    }
    
    /**
     * Adds the result of a query to a given collection.<br>
     * The individual rows will be added as an array of objects (object[])
     * <p>This function should only be used for small lists.
     * Otherwise a DBReader should be used!</p>
     * 
     * @param sqlCmd the sql command
     * @param sqlParams the command params
     * @param result the result colleciton
     * @param maxRows the maximum number of rows
     * @return the number of rows queried
     */
    public int queryObjectList(String sqlCmd, Object[] sqlParams, Collection<Object[]> result, int maxRows)
    {   // Perform query
        ResultSet rs = null;
        try
        {
            logQueryStatement(sqlCmd, sqlParams);
            // Log performance
            long start = System.currentTimeMillis();
            // Get the next Value
            rs = dbms.executeQuery(sqlCmd, sqlParams, false, context.getConnection());
            if (rs == null)
                throw new UnexpectedReturnValueException(rs, "dbms.executeQuery()");
            // Read List
            int colCount = rs.getMetaData().getColumnCount();
            int count = 0;
            while (rs.next() && (maxRows<0 || count<maxRows))
            {   // Read row
                Object[] item = new Object[colCount];
                for (int i=0; i<colCount; i++)
                {   // Read from Resultset
                    item[i] = dbms.getResultValue(rs, i+1, DataType.UNKNOWN);
                }
                result.add(item);
                count++;
            }
            // Debug
            long queryTime = (System.currentTimeMillis() - start);
            if (log.isDebugEnabled())
                log.debug("queryObjectList retured {} items in {} ms.", count, queryTime);
            else if (queryTime>=longRunndingStmtThreshold)
                log.warn("Long running query took {} seconds for statement {}.", queryTime / 1000, sqlCmd);
            // done
            return count;
        } catch (SQLException sqle) 
        {   // Error
            throw new QueryFailedException(dbms, sqlCmd, paramsToString(sqlParams), sqle);
        } finally
        { // Cleanup
            dbms.closeResultSet(rs);
        }
    } 

    /**
     * Returns the result of a query as a list Object-Arrays 
     * This function should only be used for small lists.
     * 
     * @param cmd the Command object that contains the select statement
     * @return a list of object arrays
     */
    public final List<Object[]> queryObjectList(DBCommandExpr cmd)
    {   // Execute the  Statement
        List<Object[]> result = new ArrayList<Object[]>();
        queryObjectList(cmd.getSelect(), cmd.getParamValues(), result, MAX_QUERY_ROWS);
        return result;
    }

    /**
     * Returns all values of the first row of a sql-query as an array.
     * If the query does not return a result a QueryNoResultException is thrown
     * 
     * @param sqlCmd the SQL-Command
     * @param sqlParams list of query parameter values
     * @param failOnNoResult if true a QueryNoResultException is thrown when no result otherwise null is returned 
     * 
     * @return the values of the first row or null
     */
    public Object[] querySingleRow(String sqlCmd, Object[] sqlParams, boolean failOnNoResult)
    {
        List<Object[]> result = new ArrayList<Object[]>(1);
        queryObjectList(sqlCmd, sqlParams, result, 1);
        if (result.size()<1) {
            // no result
            if (failOnNoResult)
                throw new QueryNoResultException(sqlCmd);
            // null
            return null;
        }
        return result.get(0);
    }

    /**
     * Returns all values of the first row of a sql-query as an array.
     * If the query does not return a result a QueryNoResultException is thrown
     * 
     * @param sqlCmd the SQL-Command
     * @param sqlParams list of query parameter values
     * 
     * @return the values of the first row
     */
    public final Object[] querySingleRow(String sqlCmd, Object[] sqlParams)
    {
        return querySingleRow(sqlCmd, sqlParams, true);
    }
    
    /**
     * Returns all values of the first row of a sql-query as an array.
     * If the query does not return a result a QueryNoResultException is thrown
     * 
     * @param cmd the Command object that contains the select statement
     * @param failOnNoResult if true a QueryNoResultException is thrown when no result otherwise null is returned 
     * 
     * @return the values of the first row 
     */
    public final Object[] querySingleRow(DBCommandExpr cmd, boolean failOnNoResult)
    {
        return querySingleRow(cmd.getSelect(), cmd.getParamValues(), failOnNoResult); 
    }
    
    /**
     * Returns all values of the first row of a sql-query as an array.
     * If the query does not return a result a QueryNoResultException is thrown
     * 
     * @param cmd the Command object that contains the select statement
     * 
     * @return the values of the first row 
     */
    public final Object[] querySingleRow(DBCommandExpr cmd)
    {
        return querySingleRow(cmd, true); 
    }
    
    /**
     * Returns the initial array list capacity. 
     * Usually returns 0 in order to use the ArrayList default.
     * @param pageSize the designated page size
     * @return the initial array list capacity or 0 to use the default
     */
    protected int getInitialListCapacity(int pageSize)
    {
        // return 0 in order to use ArrayList.DEFAULT_CAPACITY on first insert
        return (pageSize>0 && pageSize<DEFAULT_LIST_CAPACITY) ? pageSize : 0;
    }

    /**
     * Called to inform that the limit for DataList, Record and Bean queries has exceeded the maximum value 
     */
    protected void queryRowLimitExeeded()
    {
        log.warn("********************************************************");
        log.warn("Query Result was limited to {} by MAX_QUERY_ROWS", MAX_QUERY_ROWS);
        log.warn("********************************************************");
    }

    /**
     * Called when a query has no result
     * @param cmd the command
     * @param resultType the result type class
     */
    protected void handleQueryNoResult(DBCommandExpr cmd, Class<?> resultType)
    {
        // throw new QueryNoResultException(cmd.getSelect());
        log.debug("The query for \"{}\" returned no result.", resultType.getName());
    }
    
    /**
     * Crates a default DataListFactory for a DataListEntry class
     * The DataListEntry class must provide the following constructor
     *      DataListEntry(DataListFactory&lt;? extends DataListEntry&gt; head, int rownum, Object values[])
     * @param entryClass the entryClass for which to create the list head 
     * @return the data list factory
     */
    protected <T extends DataListEntry> DataListFactory<T> createDefaultDataListFactory(Class<T> entryClass, DataListHead head) 
    {
        return new DataListFactoryImpl<T>(entryClass, head);
    }

    /**
     * Crates a default DataListHead for a DataListEntry class
     * @param cmd the cmd for which to create the DataListHead
     * @param entryClass the entry type class
     * @return the DataListHead instance
     */
    protected DataListHead createDefaultDataListHead(DBCommandExpr cmd, Class<? extends DataListEntry> entryClass) 
    {
        return new DataListHead(cmd.getSelectExprList());
    }
    
    /**
     * Executes a query and returns a list of DataListEntry items
     * @param cmd the command
     * @param factory the Factory to be used for each list item
     * @param first the number of records to skip from the beginning of the result
     * @param pageSize the maximum number of items to add to the list or -1 (default) for a maximum of MAX_QUERY_ROWS
     * @return the list 
     */
    public <T extends DataListEntry> List<T> queryDataList(DBCommandExpr cmd, DataListFactory<T> factory, int first, int pageSize)
    {
        List<T> list = null;
        DBReader r = new DBReader(context);
        try
        {   // prepare
            factory.prepareQuery(cmd, context);
            // check pageSize
            if (pageSize==0)
            {   log.warn("PageSize should not be 0. Setting to -1 for a maximum of MAX_QUERY_ROWS records!");
                pageSize = -1;
            }
            // set range
            DBMSHandler dbms = context.getDbms();
            if (pageSize>0 && pageSize<Integer.MAX_VALUE && dbms.isSupported(DBMSFeature.QUERY_LIMIT_ROWS))
            {   // let the database limit the rows
                if (first>0 && dbms.isSupported(DBMSFeature.QUERY_SKIP_ROWS))
                {   // let the database skip the rows
                    cmd.skipRows(first);
                    // no need to skip rows ourself
                    first = 0;
                }
                cmd.limitRows(first+pageSize);
            }
            // Runquery
            r.open(cmd);
            if (first>0) 
            {   // skip rows
                r.skipRows(first);
            }
            // Create a list of data entries
            int maxCount = (pageSize>0) ? pageSize : MAX_QUERY_ROWS;
            list = factory.newList(getInitialListCapacity(pageSize));
            // add data
            int rownum = 0;
            while (r.moveNext() && rownum<maxCount)
            {   // Create bean an init
                T entry = factory.newEntry(rownum, r);
                if (entry==null)
                    continue;
                // add entry
                list.add(entry);
                // next
                rownum++;
            }
            // check
            if (rownum==maxCount && rownum==MAX_QUERY_ROWS)
                queryRowLimitExeeded();
            // done
            return list;
        }
        finally
        {   // close reader
            r.close();
            // complete
            factory.completeQuery(list);
        }
    }

    /**
     * Queries a list of DataListEntry items
     * @param cmd the query command
     * @param entryClass the entry type class
     * @param head the list head
     * @return the data list
     */
    public final <T extends DataListEntry> List<T> queryDataList(DBCommandExpr cmd, Class<T> entryClass, DataListHead head)
    {
        return queryDataList(cmd, createDefaultDataListFactory(entryClass, head), 0, -1);
    }
    
    /**
     * Queries a list of DataListEntry items
     * @param cmd the query command
     * @param entryClass the entry type class
     * @return the data list
     */
    public final <T extends DataListEntry> List<T> queryDataList(DBCommandExpr cmd, Class<T> entryClass)
    {
        return queryDataList(cmd, entryClass, createDefaultDataListHead(cmd, entryClass));
    }
    
    /**
     * Queries a list of DataListEntry items
     * @param cmd the query command
     * @param entryClass the entry type class
     * @param first the first record to add
     * @param maxItems the maximum number of records to add
     * @return the data list
     */
    public final <T extends DataListEntry> List<T> queryDataList(DBCommandExpr cmd, Class<T> entryClass, int first, int maxItems)
    {
        return queryDataList(cmd, createDefaultDataListFactory(entryClass, createDefaultDataListHead(cmd, entryClass)), first, maxItems);
    }
    
    /**
     * Queries a list of DataListEntry items
     * @param cmd the query command
     * @return the data list
     */
    public final List<DataListEntry> queryDataList(DBCommandExpr cmd)
    {
        return queryDataList(cmd, DataListEntry.class);
    }
    
    /**
     * Queries a list of DataListEntry items
     * @param cmd the query command
     * @param first the first record to add
     * @param maxItems the maximum number of records to add
     * @return the data list
     */
    public final List<DataListEntry> queryDataList(DBCommandExpr cmd, int first, int maxItems)
    {
        return queryDataList(cmd, DataListEntry.class, first, maxItems);
    }
    
    /**
     * Queries a single DataListEntry item
     * @param cmd the query command
     * @param entryClass the result class
     * @param head the list head
     * @return the data entry
     */
    public final <T extends DataListEntry> T queryDataEntry(DBCommandExpr cmd, Class<T> entryClass, DataListHead head)
    {
        List<T> dle = queryDataList(cmd, createDefaultDataListFactory(entryClass, head), 0, 1);
        if (dle.isEmpty())
        {   // No result
            handleQueryNoResult(cmd, entryClass);
            return null;
        }
        return dle.get(0);
    }
    
    /**
     * Queries a single DataListEntry item
     * @param cmd the query command
     * @param entryClass the result class
     * @return the data entry
     */
    public final <T extends DataListEntry> T queryDataEntry(DBCommandExpr cmd, Class<T> entryClass)
    {
        DataListHead head = createDefaultDataListHead(cmd, entryClass);
        return queryDataEntry(cmd, entryClass, head);
    }

    /**
     * Queries a single DataListEntry item
     * Deprecated. Please consider calling without the "failOnNoResult" parameter and overriding handleQueryNoResult()
     * @param cmd the query command
     * @param entryClass the result class
     * @param failOnNoResult flag whether to fail on empty resultset
     * @return the data entry
     */
    @Deprecated
    public final <T extends DataListEntry> T queryDataEntry(DBCommandExpr cmd, Class<T> entryClass, boolean failOnNoResult)
    {
        T result = queryDataEntry(cmd, entryClass);
        if (result==null && failOnNoResult)
            throw new QueryNoResultException(cmd.getSelect());
        return result;
    }
    
    /**
     * Queries a single DataListEntry item
     * @param cmd the query command
     * @return the data entry
     */
    public final DataListEntry queryDataEntry(DBCommandExpr cmd)
    {
        return queryDataEntry(cmd, DataListEntry.class);
    }

    /**
     * Crates a default DBRecordListFactory for a DBRecord class
     * The DBRecord class must provide the following constructor
     *      DBRecord(DBContext context, DBRowSet rowset)
     * @param recordClass the recordClass for which to create the list head 
     * @return the record factory
     */
    protected <R extends DBRecordBase> DBRecordListFactory<R> createDefaultRecordListFactory(Class<R> recordClass, DBRowSet rowset) 
    {
        return new DBRecordListFactoryImpl<R>(recordClass, context.getClass(), rowset);
    }
    
    /**
     * Executes a query and returns a list of DBRecord items
     * @param cmd the command
     * @param factory the factory for creating record objects
     * @param first the number of records to skip from the beginning of the result
     * @param pageSize the maximum number of items to add to the list or -1 (default) for a maximum of MAX_QUERY_ROWS
     * @return the list 
     */
    public <R extends DBRecordBase> List<R> queryRecordList(DBCommand cmd, DBRecordListFactory<R> factory, int first, int pageSize)
    {
        List<R> list = null;
        DBReader r = new DBReader(context);
        try
        {   // prepare
            factory.prepareQuery(cmd, context);
            // check pageSize
            if (pageSize==0)
            {   log.warn("PageSize should not be 0. Setting to -1 for a maximum of MAX_QUERY_ROWS records!");
                pageSize = -1;
            }
            // set range
            DBMSHandler dbms = context.getDbms();
            if (pageSize>0 && pageSize<Integer.MAX_VALUE && dbms.isSupported(DBMSFeature.QUERY_LIMIT_ROWS))
            {   // let the database limit the rows
                if (first>0 && dbms.isSupported(DBMSFeature.QUERY_SKIP_ROWS))
                {   // let the database skip the rows
                    cmd.skipRows(first);
                    // no need to skip rows ourself
                    first = 0;
                }
                cmd.limitRows(first+pageSize);
            }
            // Runquery
            r.open(cmd);
            if (first>0) 
            {   // skip rows
                r.skipRows(first);
            }
            // Create a list of data entries
            int maxCount = (pageSize>0) ? pageSize : MAX_QUERY_ROWS;
            list = factory.newList(getInitialListCapacity(pageSize));
            // add data
            int rownum = 0;
            while (r.moveNext() && rownum<maxCount)
            {   // Create bean an init
                R entry = factory.newRecord(rownum, r);
                if (entry==null)
                    continue;
                // add entry
                list.add(entry);
                // next
                rownum++;
            }
            // check
            if (rownum==maxCount && rownum==MAX_QUERY_ROWS)
                queryRowLimitExeeded();
            // done
            return list;
        }
        finally
        {   // close reader
            r.close();
            // complete
            if (list!=null)
                factory.completeQuery(list);
        }
    }

    /**
     * Executes a query and returns a list of DBRecord items
     * @param cmd the command holding the constraints and order or the query
     * @param rowset the rowset for which to query the records
     * @param recordType the record class
     * @return the list of DBRecord items
     */
    public final <R extends DBRecordBase> List<R> queryRecordList(DBCommand cmd, DBRowSet rowset, Class<R> recordType)
    {
        @SuppressWarnings("cast")
        DBRecordListFactory<R> factory = (DBRecordListFactory<R>)createDefaultRecordListFactory(recordType, rowset);
        return queryRecordList(cmd, factory, 0, -1);
    }

    /**
     * Executes a query and returns a list of DBRecord items
     * @param cmd the command holding the constraints and order or the query
     * @param rowset the rowset for which to query the records
     * @return the list of DBRecord items
     */
    public final List<DBRecord> queryRecordList(DBCommand cmd, DBRowSet rowset)
    {
        return queryRecordList(cmd, rowset, DBRecord.class);
    }

    /**
     * Crates a default DBBeanListFactory for Java bean class
     * The DBRecord class must provide   
     *      either a standard construtor with correspondig property set fundtions
     *      or a constructor using the fields of the query
     * @param beanType the beanType for which to create the list head 
     * @param keyColumns the key columns
     * @param selectColumns the select columns 
     * @return the bean factory
     */
    protected <T> DBBeanListFactory<T> createDefaultBeanListFactory(Class<T> beanType, Column[] keyColumns, List<? extends DBColumnExpr> selectColumns) 
    {
        return new DBBeanListFactoryImpl<T>(beanType, keyColumns, selectColumns);
    }

    /**
     * gets or creates DBBeanListFactory for the given rowset
     * @param beanType the beanType for which to create the list head 
     * @param rowset the rowset for which to return the factory 
     * @return the bean factory
     */
    public synchronized <T> DBBeanListFactory<T> getRowsetBeanListFactory(Class<T> beanType, DBRowSet rowset) 
    {
        DBBeanListFactory<T> factory = DBBeanFactoryCache.getFactoryForType(beanType);
        if (factory==null)
        {   // Create default factory
            log.debug("No factory found for bean type '{}' and rowset {}. Creating default", beanType.getName(), rowset.getName());
            factory= createDefaultBeanListFactory(beanType, rowset.getKeyColumns(), rowset.getColumns());
            DBBeanFactoryCache.setFactoryForType(beanType, factory);
        }
        return factory;
    }

    /**
     * gets or creates DBBeanListFactory for the given rowset
     * @param beanType the beanType for which to create the list head 
     * @param cmd the command 
     * @return the bean factory
     */
    public synchronized <T> DBBeanListFactory<T> getCommandBeanListFactory(Class<T> beanType, DBCommandExpr cmd) 
    {
        DBBeanListFactory<T> factory = DBBeanFactoryCache.getFactoryForType(beanType);
        if (factory==null) 
        {   // Check command: Must have select!
            if (!cmd.hasSelectExpr())
                throw new CommandWithoutSelectException(cmd);
            // Create default factory
            log.debug("No factory found for bean type '{}'. Creating default", beanType.getName());
            factory= createDefaultBeanListFactory(beanType, null, cmd.getSelectExpressions());
            DBBeanFactoryCache.setFactoryForType(beanType, factory);
        }
        return factory;
    }
    
    /**
     * Query a list of simple Java objects (beans)
     * @param cmd the command
     * @param factory the bean factory
     * @param parent the parent object for the created beans (optional)
     * @param first the first row
     * @param pageSize the maximum number of items to add to the list or -1 (default) for a maximum of MAX_QUERY_ROWS
     * @return the bean list
     */
    public <T> List<T> queryBeanList(DBCommandExpr cmd, DBBeanListFactory<T> factory, Object parent, int first, int pageSize)
    {
        List<T> list = null;
        DBReader r = new DBReader(context);
        try
        {   // prepare
            factory.prepareQuery(cmd, context);
            // check pageSize
            if (pageSize==0)
            {   log.warn("PageSize should not be 0. Setting to -1 for a maximum of MAX_QUERY_ROWS records!");
                pageSize = -1;
            }
            // set range
            DBMSHandler dbms = context.getDbms();
            if (pageSize>0 && pageSize<Integer.MAX_VALUE && dbms.isSupported(DBMSFeature.QUERY_LIMIT_ROWS))
            {   // let the database limit the rows
                if (first>0 && dbms.isSupported(DBMSFeature.QUERY_SKIP_ROWS))
                {   // let the database skip the rows
                    cmd.skipRows(first);
                    // no need to skip rows ourself
                    first = 0;
                }
                cmd.limitRows(first+pageSize);
            }
            // Runquery
            r.open(cmd);
            if (first>0) 
            {   // skip rows
                r.skipRows(first);
            }
            // Create a list of data entries
            int maxCount = (pageSize>0) ? pageSize : MAX_QUERY_ROWS;
            list = factory.newList(getInitialListCapacity(pageSize));
            // add data
            int rownum = 0;
            while (r.moveNext() && rownum<maxCount)
            {   // Create bean an init
                T item = factory.newItem(rownum, r);
                if (item==null)
                    continue;
                // add entry
                list.add(item);
                // post processing
                if (item instanceof DataBean<?>)
                    ((DataBean<?>)item).initialize(((DBObject)r).getDatabase(), context, rownum, parent);
                // next
                rownum++;
            }
            // check
            if (rownum==maxCount && rownum==MAX_QUERY_ROWS)
                queryRowLimitExeeded();
            // done
            return list;
        }
        finally
        {
            r.close();
            // complete
            if (list!=null)
                factory.completeQuery(list);
        }
    }

    /**
     * Queries a list of Java beans for a given command
     * @param cmd the query command
     * @param factory the beanType factory used to instantiate the bean
     * @param parent (optional) the parent bean if any 
     * @return the list of java beans
     */
    public final <T> List<T> queryBeanList(DBCommandExpr cmd, DBBeanListFactory<T> factory, Object parent)
    {
        return queryBeanList(cmd, factory, parent, 0, -1);
    }

    /**
     * Queries a list of Java beans for a given command
     * @param cmd the query command
     * @param beanType the beanType
     * @param rowset the rowset
     * @param parent (optional) the parent bean if any 
     * @return the list of java beans
     */
    public <T> List<T> queryBeanList(DBCommandExpr cmd, Class<T> beanType, DBRowSet rowset, Object parent)
    {
        return queryBeanList(cmd, getRowsetBeanListFactory(beanType, rowset), parent, 0, -1);
    }

    /**
     * Queries a list of Java beans for a given command
     * @param cmd the query command
     * @param beanType the beanType
     * @param parent (optional) the parent bean if any 
     * @return the list of java beans
     */
    public <T> List<T> queryBeanList(DBCommandExpr cmd, Class<T> beanType, Object parent)
    {
        return queryBeanList(cmd, getCommandBeanListFactory(beanType, cmd), parent);
    }
    
    /**
     * queries a single Java Bean for a given command 
     * @param cmd the query command
     * @param factory the factory to create the bean instance
     * @return the bean instance
     */
    public <T> T queryBean(DBCommandExpr cmd, DBBeanListFactory<T> factory)
    {
        DBReader r = new DBReader(context);
        try
        {   // prepare
            factory.prepareQuery(cmd, context);
            // Runquery
            r.open(cmd);
            // Get First Record
            if (!r.moveNext())
            {   // No Result
                handleQueryNoResult(cmd, factory.getBeanType());
                return null;
            }
            // add data
            T item = factory.newItem(-1, r);
            // post processing
            if (item instanceof DataBean<?>)
                ((DataBean<?>)item).initialize(((DBObject)r).getDatabase(), context, -1, null);
            // done
            return item;
        }
        finally
        {
            r.close();
            // complete
            factory.completeQuery(null);
        }
    }

    /**
     * Queries a single Java Bean for a given command
     * @param cmd the query command
     * @param beanType the beanType
     * @return the list of java beans
     */
    public <T> T queryBean(DBCommandExpr cmd, Class<T> beanType)
    {
        return queryBean(cmd, getCommandBeanListFactory(beanType, cmd));
    }
    
    /**
     * Queries a single bean based on a where constraint
     * @param beanType the beanType
     * @param rowset the rowset used for the query 
     * @param whereConstraints the constraints for the query
     * @return the entity bean
     */
    public final <T> T queryBean(Class<T> beanType, DBRowSet rowset, DBCompareExpr whereConstraints)
    {
        DBObject.checkParamNull("rowset", rowset);
        DBObject.checkParamNull("whereConstraints", whereConstraints);
        // find
        DBCommand cmd = context.createCommand();
        cmd.where(whereConstraints);
        // use factory of rowset
        return queryBean(cmd, getRowsetBeanListFactory(beanType, rowset));
    }
    
    /**
     * Queries a single bean based on a where constraint
     * @param beanType the beanType
     * @param whereConstraints the constraints for the query
     * @return the entity bean
     */
    public final <T> T queryBean(Class<T> beanType, DBCompareExpr whereConstraints)
    {
        DBObject.checkParamNull("whereConstraints", whereConstraints);
        // must have a factory
        DBBeanListFactory<T> factory = DBBeanFactoryCache.getFactoryForType(beanType);
        if (factory==null)
            throw new UnknownBeanTypeException(beanType);
        // add constraints
        DBCommand cmd = context.createCommand();
        cmd.where(whereConstraints);
        // query now
        return queryBean(cmd, factory);
    }
    
    /**
     * Queries a single bean based on primary key values
     * @param beanType the beanType 
     * @param rowset the rowset used for the query 
     * @param key the primary key
     * @return the entity bean
     */
    public final <T> T queryBean(Class<T> beanType, DBRowSet rowset, Object[] key)
    {
        DBObject.checkParamNull("rowset", rowset);
        DBObject.checkParamNull("key", key);
        // set key constraints 
        DBCommand cmd = context.createCommand();
        cmd.where(rowset.getKeyConstraints(key));
        // use factory of rowset
        return queryBean(cmd, getRowsetBeanListFactory(beanType, rowset));
    }
}
