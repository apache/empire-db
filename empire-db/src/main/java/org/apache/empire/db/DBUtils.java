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
import org.apache.empire.data.DataType;
import org.apache.empire.db.exceptions.ConstraintViolationException;
import org.apache.empire.db.exceptions.QueryFailedException;
import org.apache.empire.db.exceptions.QueryNoResultException;
import org.apache.empire.db.exceptions.StatementFailedException;
import org.apache.empire.exceptions.InternalException;
import org.apache.empire.exceptions.InvalidArgumentException;
import org.apache.empire.exceptions.UnexpectedReturnValueException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DBUtils implements DBContextAware
{
    // Logger
    protected static final Logger log = LoggerFactory.getLogger(DBUtils.class);
    
    // Threshold for long running queries in milliseconds
    protected long longRunndingStmtThreshold = 30000;
    // the context
    protected final DBContext context;
    // the driver
    protected final DBDatabaseDriver driver;
    
    /**
     * Constructs an empty DBRecordSet object.
     * @param useFieldIndexMap 
     */
    public DBUtils(DBContext context)
    {
        this.context = context;
        this.driver = context.getDriver();
    }

    /**
     * Returns the current Context
     * @return
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T extends DBContext> T  getContext()
    {
        return ((T)context);
    }
    
    /**
     * Creates a new Command object for the given database
     * 
     * @return the command object.
     */
    public final DBCommand createCommand(DBDatabase db)
    {
        return driver.createCommand(db);
    }
    /**
     * Executes an update, insert or delete SQL-Statement.<BR>
     * We recommend to use a DBCommand object in order to build the sqlCmd.<BR>
     * <P>
     * @param sqlCmd the SQL-Command
     * @param sqlParams a list of objects to replace sql parameters
     * @param setGenKeys object to set the generated keys for
     * @return the row count for insert, update or delete or 0 for SQL statements that return nothing
     */
    public int executeSQL(String sqlCmd, Object[] sqlParams, DBDatabaseDriver.DBSetGenKeys setGenKeys)
    {
        try 
        {   // Debug
            if (log.isInfoEnabled())
                log.info("Executing: " + sqlCmd);
            // execute SQL
            long start = System.currentTimeMillis();
            int affected = driver.executeSQL(sqlCmd, sqlParams, context.getConnection(), setGenKeys);
            // number of affected records
            if (affected < 0)
                throw new UnexpectedReturnValueException(affected, "driver.executeSQL()");
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
            throw new ConstraintViolationException(driver, sqlCmd, sqle);
        } catch (SQLException sqle) {
            // Other error
            throw new StatementFailedException(driver, sqlCmd, sqle);
        }    
    }

    /**
     * Executes an SQLStatment
     * @param sqlCmd the SQL-Command
     * @param sqlParams a list of objects to replace sql parameters
     */
    public final int executeSQL(String sqlCmd, Object[] sqlParams)
    {
        return executeSQL(sqlCmd, sqlParams, null); 
    }

    /**
     * Executes an Insert statement from a command object
     * @param cmd the command object containing the insert command
     * @return the number of records that have been inserted with the supplied statement
     */
    public final int executeInsert(DBCommand cmd)
    {
        return executeSQL(cmd.getInsert(), cmd.getParamValues()); 
    }

    /**
     * Executes an InsertInfo statement from a command object
     * @param table the table into which to insert the selected data
     * @param cmd the command object containing the selection command 
     * @return the number of records that have been inserted with the supplied statement
     */
    public final int executeInsertInto(DBTable table, DBCommand cmd)
    {
        return executeSQL(cmd.getInsertInto(table), cmd.getParamValues()); 
    }

    /**
     * Executes an Update statement from a command object
     * @param cmd the command object containing the update command
     * @return the number of records that have been updated with the supplied statement
     */
    public final int executeUpdate(DBCommand cmd)
    {
        return executeSQL(cmd.getUpdate(), cmd.getParamValues()); 
    }

    /**
     * Executes a Delete statement from a command object
     * @param from the database table from which to delete records
     * @param cmd the command object containing the delete constraints
     * @return the number of records that have been deleted with the supplied statement
     */
    public final int executeDelete(DBTable from, DBCommand cmd)
    {
        return executeSQL(cmd.getDelete(from), cmd.getParamValues()); 
    }

    /**
     * Returns the value of the first row/column of a sql-query as an object.
     * If the query does not return a result the value ObjectUtils.NO_VALUE is returned.
     * 
     * @param sqlCmd the SQL-Command
     * @param sqlParams list of query parameter values
     * @param dataType the expected data type
     * 
     * @return the value of the first column in the first row of the query 
     */
    public Object querySingleValue(String sqlCmd, Object[] sqlParams, DataType dataType, boolean forceResult)
    {
        // Debug
        long start = System.currentTimeMillis();
        if (log.isDebugEnabled())
            log.debug("Executing: " + sqlCmd);
        // Read value
        Object result = driver.querySingleValue(sqlCmd, sqlParams, dataType, context.getConnection());
        if (result==ObjectUtils.NO_VALUE)
        {   if (forceResult)
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
    }
    
    /**
     * Returns the value of the first row/column of a sql-query as an object.
     * If the query does not return a result a QueryNoResultException is thrown
     * 
     * @param cmd the Command object that contains the select statement
     * @param dataType the expected data type
     * 
     * @return the value of the first column in the first row of the query 
     */
    public final Object querySingleValue(DBCommand cmd, DataType dataType, boolean forceResult)
    {
        return querySingleValue(cmd.getSelect(), cmd.getParamValues(), dataType, forceResult);
    }
    
    /**
     * Returns the value of the first row/column of a sql-query as an object.
     * If the query does not return a result a QueryNoResultException is thrown
     * 
     * @param cmd the Command object that contains the select statement
     * 
     * @return the value of the first column in the first row of the query 
     */
    public final Object querySingleValue(DBCommand cmd, boolean forceResult)
    {
        return querySingleValue(cmd, DataType.UNKNOWN, forceResult);  
    }
    
    /**
     * Returns the value of the first row/column of a sql-query as an object.
     * If the query does not return a result a QueryNoResultException is thrown
     * 
     * @param cmd the Command object that contains the select statement
     * 
     * @return the value of the first column in the first row of the query 
     */
    public final Object querySingleValue(DBCommand cmd)
    {
        return querySingleValue(cmd, DataType.UNKNOWN, true);  
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
    public final int querySingleInt(DBCommand cmd, int defaultValue)
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
    public final int querySingleInt(DBCommand cmd)
    { 
        Object value = querySingleValue(cmd.getSelect(), cmd.getParamValues(), DataType.INTEGER, true);
        return ObjectUtils.getInteger(value);
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
    public final long querySingleLong(DBCommand cmd, long defaultValue)
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
    public final long querySingleLong(DBCommand cmd)
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
    public final String querySingleString(DBCommand cmd, String defaultValue)
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
    public final String querySingleString(DBCommand cmd)
    { 
        Object value = querySingleValue(cmd.getSelect(), cmd.getParamValues(), DataType.VARCHAR, true);
        return StringUtils.toString(value);
    }
    
    /**
     * Adds the first column of a query result to a collection.
     * If the query has no result, an empty list is returned.
     * 
     * @param c the class type for the list 
     * @param <T> the type for the list
     * @param sqlCmd the SQL statement
     * @param dataType the expected data type
     * @param maxRows maximum number of rows or -1 for all rows
     * 
     * @return the number of elements that have been added to the collection 
     */
    public <T> int querySimpleList(Class<T> c, String sqlCmd, Object[] sqlParams, DataType dataType, Collection<T> result, int maxRows)
    {   // Start query
        ResultSet rs = null;
        try
        {   // Log performance
            long start = System.currentTimeMillis();
            if (log.isDebugEnabled())
                log.debug("Executing: " + sqlCmd);
            // Get the next Value
            rs = driver.executeQuery(sqlCmd, sqlParams, false, context.getConnection());
            if (rs == null)
                throw new UnexpectedReturnValueException(rs, "driver.executeQuery()");
            // Check Result
            int count=0;
            while (rs.next() && (maxRows<0 || count<maxRows))
            {   
                T item = ObjectUtils.convert(c, driver.getResultValue(rs, 1, dataType));
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
            throw new QueryFailedException(driver, sqlCmd, sqle);
        } finally
        { // Cleanup
            driver.closeResultSet(rs);
        }
    }
    
    /**
     * Adds the first column of a query result to a collection.
     * If the query has no result, an empty list is returned.
     * 
     * @param c the class type for the list 
     * @param <T> the type for the list
     * @param cmd the Command object that contains the select statement
     * 
     * @return the number of elements that have been added to the collection 
     */
    public final <T> int querySimpleList(Class<T> c, DBCommand cmd, Collection<T> result)
    {
        return querySimpleList(c, cmd.getSelect(), cmd.getParamValues(), DataType.UNKNOWN, result, -1); 
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
    public final <T> List<T> querySimpleList(Class<T> c, DBCommand cmd)
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
    public final List<Object> querySimpleList(DBCommand cmd)
    {   // Execute the  Statement
        return querySimpleList(Object.class, cmd);
    }
    
    /**
     * Fills an option list provided with the result from a query.
     * The option list is filled with the values of the first and second column.
     * 
     * @param sqlCmd the SQL statement
     * @return an Options object containing a set a of values and their corresponding names 
     */
    public int queryOptionList(String sqlCmd, Object[] sqlParams, Options result)
    {   // Execute the  Statement
        ResultSet rs = null;
        try
        {   // Debug
            long start = System.currentTimeMillis();
            if (log.isDebugEnabled())
                log.debug("Executing: " + sqlCmd);
            // Get the next Value
            rs = driver.executeQuery(sqlCmd, sqlParams, false, context.getConnection());
            if (rs == null)
                throw new UnexpectedReturnValueException(rs, "driver.executeQuery()");
            if (rs.getMetaData().getColumnCount()<2)
                throw new InvalidArgumentException("sqlCmd", sqlCmd);
            // Check Result
            int count = 0;
            while (rs.next())
            {
                Object value = rs.getObject(1);
                String text  = rs.getString(2);
                result.add(value, text, true);
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
            throw new QueryFailedException(driver, sqlCmd, sqle);
        } finally
        { // Cleanup
            driver.closeResultSet(rs);
        }
    }
    
    /**
     * Fills an option list provided with the result from a query.
     * The option list is filled with the values of the first and second column.
     * 
     * @param cmd the Command object that contains the select statement
     * @return an Options object containing a set a of values and their corresponding names 
     */
    public final int queryOptionList(DBCommand cmd, Options result)
    {   // Execute the  Statement
        return queryOptionList(cmd.getSelect(), cmd.getParamValues(), result); 
    }

    /**
     * Returns a list of key value pairs from an sql query.
     * The option list is filled with the values of the first and second column.
     * 
     * @param cmd the Command object that contains the select statement
     * @return an Options object containing a set a of values and their corresponding names 
     */
    public final Options queryOptionList(DBCommand cmd)
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
     * @param sqlCmd the SQL statement
     * @return a list of object arrays 
     */
    public int queryObjectList(String sqlCmd, Object[] sqlParams, Collection<Object[]> result, int maxRows)
    {   // Perform query
        ResultSet rs = null;
        try
        {   // Log performance
            long start = System.currentTimeMillis();
            if (log.isDebugEnabled())
                log.debug("Executing: " + sqlCmd);
            // Get the next Value
            rs = driver.executeQuery(sqlCmd, sqlParams, false, context.getConnection());
            if (rs == null)
                throw new UnexpectedReturnValueException(rs, "driver.executeQuery()");
            // Read List
            int colCount = rs.getMetaData().getColumnCount();
            int count = 0;
            while (rs.next() && (maxRows<0 || count<maxRows))
            {   // Read row
                Object[] item = new Object[colCount];
                for (int i=0; i<colCount; i++)
                {   // Read from Resultset
                    item[i] = driver.getResultValue(rs, i+1, DataType.UNKNOWN);
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
            throw new QueryFailedException(driver, sqlCmd, sqle);
        } finally
        { // Cleanup
            driver.closeResultSet(rs);
        }
    } 

    /**
     * Adds the result of a query to a given collection.<br>
     * The individual rows will be added as an array of objects (object[])
     * <p>This function should only be used for small lists.
     * Otherwise a DBReader should be used!</p>
     * 
     * @param cmd the Command object that contains the select statement
     * @return a list of object arrays 
     */
    public final int queryObjectList(DBCommand cmd, Collection<Object[]> result)
    {   // Perform query
        return queryObjectList(cmd.getSelect(), cmd.getParamValues(), result, -1); 
    }

    /**
     * Returns the result of a query as a list Object-Arrays 
     * This function should only be used for small lists.
     * 
     * @param cmd the Command object that contains the select statement
     * @return a list of object arrays 
     */
    public final List<Object[]> queryObjectList(DBCommand cmd)
    {   // Execute the  Statement
        List<Object[]> result = new ArrayList<Object[]>();
        queryObjectList(cmd.getSelect(), cmd.getParamValues(), result, -1);
        return result;
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
    public Object[] querySingleRow(String sqlCmd, Object[] sqlParams)
    {
        List<Object[]> result = new ArrayList<Object[]>();
        queryObjectList(sqlCmd, sqlParams, result, 1);
        if (result.size()<1)
            throw new QueryNoResultException(sqlCmd);
        return result.get(0);
    }
    
    /**
     * Returns all values of the first row of a sql-query as an array.
     * If the query does not return a result a QueryNoResultException is thrown
     * 
     * @param cmd the Command object that contains the select statement
     * 
     * @return the values of the first row 
     */
    public final Object[] querySingleRow(DBCommand cmd)
    {
        return querySingleRow(cmd.getSelect(), cmd.getParamValues()); 
    }
        
    /**
     * Executes a select SQL-Statement and returns a ResultSet containing the query results.<BR>
     * This function returns a JDBC ResultSet.<BR>
     * Instead of using this function directly you should use a DBReader object instead.<BR>
     * <P>
     * @param sqlCmd the SQL-Command
     * @param sqlParams a list of parameters for parameter queries (may depend on driver)
     * @param scrollable true if the reader should be scrollable or false if not
     * @return the JDBC ResutSet
     */
    public ResultSet executeQuery(String sqlCmd, Object[] sqlParams, boolean scrollable)
    {
        try
        {   // Debug
            if (log.isDebugEnabled())
                log.debug("Executing: " + sqlCmd);
            // Execute the Statement
            long start = System.currentTimeMillis();
            ResultSet rs = driver.executeQuery(sqlCmd, sqlParams, scrollable, context.getConnection());
            if (rs == null)
                throw new UnexpectedReturnValueException(rs, "driver.executeQuery()");
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
            throw new QueryFailedException(driver, sqlCmd, sqle);
        } 
    }


}
