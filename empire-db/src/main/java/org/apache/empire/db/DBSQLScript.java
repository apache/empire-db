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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.empire.db.DBDDLGenerator.DDLActionType;
import org.apache.empire.db.context.DBContextAware;
import org.apache.empire.db.exceptions.EmpireSQLException;
import org.apache.empire.dbms.DBMSHandler;
import org.apache.empire.exceptions.InvalidArgumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DBSQLScript<br>
 * This class is a collection of sql command strings.<br>
 * The class is used for obtaining and executing DDL commands supplied by the
 * database dbms (@see {@link DBMSHandler#getDDLScript(DDLActionType, DBObject, DBSQLScript)})
 */
public class DBSQLScript implements DBContextAware, Iterable<String>
{
    // Logger
    private static final Logger log = LoggerFactory.getLogger(DBSQLScript.class);
    private static final String DEFAULT_COMMAND_SEPARATOR = ";\r\n\r\n";
    
    private static final String SQL_COMMENT_LINE  = "--";
    private static final String SQL_COMMENT_LINE_END = "--\r\n";
    private static final String SQL_COMMENT_START = "/*";
    private static final String SQL_COMMENT_END   = "*/";

    /**
     * SQLCmd
     * @author doebele
     */
    protected static class SQLStmt
    {
        private String   cmd;
        private Object[] params;

        public SQLStmt(String cmd, Object[] params)
        {
            this.cmd = cmd;
            this.params = params;
        }

        public String getCmd()
        {
            return cmd;
        }

        public void setCmd(String cmd)
        {
            this.cmd = cmd;
        }

        public Object[] getParams()
        {
            return params;
        }

        public void setParams(Object[] params)
        {
            this.params = params;
        }
    }

    /**
     * SQLCmdIterator
     * @author doebele
     */
    private static class SQLStmtIterator implements Iterator<String>
    {
        private final Iterator<SQLStmt> iterator;

        private SQLStmtIterator(Iterator<SQLStmt> iterator)
        {
            this.iterator = iterator;
        }

        @Override
        public boolean hasNext()
        {
            return iterator.hasNext();
        }

        @Override
        public String next()
        {
            return iterator.next().getCmd();
        }

        @Override
        public void remove()
        {
            iterator.remove();
        }
    }

    // Properties
    protected String commandSeparator = DEFAULT_COMMAND_SEPARATOR;

    protected ArrayList<SQLStmt> sqlStmtList      = new ArrayList<SQLStmt>();
    
    // the context
    protected final DBContext context;

    public DBSQLScript(DBContext context)
    {
        // nothing
        this.context = context;
    }

    public DBSQLScript(DBContext context, String commandSeparator)
    {
        this(context);
        this.commandSeparator = commandSeparator;
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
     * Adds a statement to the script.
     * 
     * @param sql the statement
     */
    public void addStmt(String sql)
    {
        sqlStmtList.add(new SQLStmt(sql, null));
    }

    /**
     * Adds a statement to the script.
     * 
     * @param sql the statement
     * @param params the statement parameters
     */
    public void addStmt(String sql, Object[] params)
    {
        sqlStmtList.add(new SQLStmt(sql, params));
    }

    /**
     * Adds a statement to the script.<br>
     * The supplied StringBuilder will be reset to a length of 0
     * 
     * @param sql the statement
     */
    public final void addStmt(DBSQLBuilder sql)
    {
        addStmt(sql.toString());
        // Clear Builder
        sql.reset(0);
    }

    /**
     * Adds an insert statement 
     * @param cmd the insert command
     */
    public void addInsert(DBCommand cmd)
    {
        if (cmd == null)
            throw new InvalidArgumentException("cmd", cmd);
        addStmt(cmd.getInsert(), cmd.getParamValues());
    }

    /**
     * Adds an update statement 
     * @param cmd the insert command
     */
    public void addUpdate(DBCommand cmd)
    {
        if (cmd == null)
            throw new InvalidArgumentException("cmd", cmd);
        addStmt(cmd.getUpdate(), cmd.getParamValues());
    }

    /**
     * Adds an delete statement 
     * @param cmd the insert command
     * @param table the table to delete
     */
    public void addDelete(DBCommand cmd, DBTable table)
    {
        if (cmd == null)
            throw new InvalidArgumentException("cmd", cmd);
        addStmt(cmd.getDelete(table), cmd.getParamValues());
    }

    /**
     * Returns the number of statements in this script
     * 
     * @return number of statements in this script
     */
    public int getCount()
    {
        return sqlStmtList.size();
    }

    /**
     * Returns the statement command at the given index
     * @param i index of the statement to retrieve
     * @return the sql statement command
     */
    public String getStmt(int i)
    {
        if (i < 0 || i >= sqlStmtList.size())
            throw new InvalidArgumentException("index", i);
        // return statement command
        return sqlStmtList.get(i).getCmd();
    }

    /**
     * Returns the statement command at the given index
     * @param i index of the statement to retrieve
     * @return the sql statement params
     */
    public Object[] getStmtParams(int i)
    {
        if (i < 0 || i >= sqlStmtList.size())
            throw new InvalidArgumentException("index", i);
        // return statement params
        return sqlStmtList.get(i).getParams();
    }

    /**
     * Inserts an entry in the list
     * 
     * @param i index of the statement to replace
     * @param stmt the new statement for this index, or NULL to remove the statement
     * @param params the statement params
     */
    public void insertStmt(int i, String stmt, Object[] params)
    {
        if (stmt == null)
            throw new InvalidArgumentException("stmt", stmt);
        if (i < 0 || i > sqlStmtList.size())
            throw new InvalidArgumentException("index", i);
        // insert statement
        sqlStmtList.add(i, new SQLStmt(stmt, params));
    }

    /**
     * Inserts an entry in the list
     * 
     * @param i index of the statement to replace
     * @param stmt the new statement for this index, or NULL to remove the statement
     */
    public final void insertStmt(int i, String stmt)
    {
        // replace or remove statement
        insertStmt(i, stmt, null);
    }

    /**
     * Replaces an entry in the list
     * 
     * @param i index of the statement to replace
     * @param cmd the new statement for this index, or NULL to remove the statement
     * @param params the command params (optional)
     */
    public void replaceStmt(int i, String cmd, Object[] params)
    {
        if (cmd == null)
            throw new InvalidArgumentException("cmd", cmd);
        if (i < 0 || i >= sqlStmtList.size())
            throw new InvalidArgumentException("index", i);
        // replace statement
        SQLStmt stmt = sqlStmtList.get(i);
        stmt.setCmd(cmd);
        stmt.setParams(params);
    }

    /**
     * Replaces an entry in the list
     * 
     * @param i index of the statement to replace
     * @param cmd the new statement for this index, or NULL to remove the statement
     */
    public final void replaceStmt(int i, String cmd)
    {
        // replace
        replaceStmt(i, cmd, null);
    }

    /**
     * Removes a statement from the list
     * @param i index of the statement to replace
     */
    public void removeStmt(int i)
    {
        // check index
        if (i < 0 || i >= sqlStmtList.size())
            throw new InvalidArgumentException("index", i);
        // remove statement
        sqlStmtList.remove(i);
    }

    /**
     * Clears the script by removing all statements
     */
    public void clear()
    {
        sqlStmtList.clear();
    }

    /**
     * Executes all SQL Statements one by one using the supplied dbms and connection.
     * 
     * @param ignoreErrors true if errors should be ignored
     * 
     * @return number of records affected
     */
    public int executeAll(boolean ignoreErrors)
    {
        log.info("Running script containing {} statements.", getCount());
        int errors = 0;
        int result = 0;
        boolean comment = false; 
        DBMSHandler dbms = context.getDbms();
        Connection  conn = context.getConnection();
        DBUtils    utils = context.getUtils(); // Use DBUtils for log helpers only
        for (SQLStmt stmt : sqlStmtList)
        {   try
            {   // execute
                String sqlCmd = stmt.getCmd().trim();
                // Check for comment
                if (comment || (sqlCmd.startsWith(SQL_COMMENT_START) || (sqlCmd.startsWith(SQL_COMMENT_LINE) && sqlCmd.indexOf(SQL_COMMENT_LINE_END)<0)))
                {   // It's a comment
                    logStmt(utils, sqlCmd, null, true);
                    // start of comment
                    if (sqlCmd.startsWith(SQL_COMMENT_START) && !comment)
                        comment=true;
                    // check end of comment?
                    if (sqlCmd.endsWith(SQL_COMMENT_END))
                        comment=false;
                    // continue
                    continue;
                }
                // Execute Statement
                Object[] sqlParams = stmt.getParams();
                logStmt(utils, sqlCmd, sqlParams, false);
                int count = executeStmt(dbms, sqlCmd, sqlParams, conn);
                result += (count >= 0 ? count : 0);
            }
            catch (SQLException e)
            {   // SQLException
                log.warn("Statement '"+stmt.getCmd()+"' failed with SQLException "+e.toString(), e);
                if (ignoreErrors == false)
                { // forward exception
                    throw new EmpireSQLException(dbms, e);
                }
                // continue
                log.debug("Ignoring error. Continuing with script...");
                errors++;
            }
        }
        // done
        log.info("Script completed with {} errors. {} records affected.", errors, result);
        return result;
    }

    /**
     * Executes all SQL Statements one by one using the supplied dbms and connection.

     * @return number of records affected
     */
    public final int executeAll()
    {
        return executeAll(false);
    }

    /**
     * Executes all SQL Statements as a JDBC Batch Job.
     * @return the total number of affected records
     */
    public int executeBatch()
    {
        DBMSHandler dbms = context.getDbms();
        try
        {   // Execute Statement
            int count = sqlStmtList.size();
            String[] cmdList = new String[count];
            Object[][] paramList = null;
            int i = 0;
            for (SQLStmt stmt : sqlStmtList)
            {
                cmdList[i] = stmt.getCmd();
                // set params
                if (stmt.getParams() != null)
                {
                    if (paramList == null)
                        paramList = new Object[count][];
                    paramList[i] = stmt.getParams();
                }
                i++;
            }
            // Execute batch
            log.info("Running batch containing {} statements.", getCount());
            int[] res = dbms.executeBatch(cmdList, paramList, context.getConnection());
            for (count = 0, i = 0; i < (res != null ? res.length : 0); i++)
                 count+= (res[i] >= 0 ? res[i] : 0);
            log.info("Script completed. {} records affected.", count);
            return count;
        }
        catch (SQLException e)
        {   // SQLException
            log.warn("Execute Batch failed with SQLException "+e.toString(), e);
            throw new EmpireSQLException(dbms, e);
        }
    }

    /**
     * Returns an iterator
     */
    @Override
    public Iterator<String> iterator()
    {
        return new SQLStmtIterator(sqlStmtList.iterator());
    }

    /**
     * Returns the sql script as a string
     */
    @Override
    public String toString()
    {
        StringBuilder script = new StringBuilder();
        for (SQLStmt stmt : sqlStmtList)
        {
            script.append(stmt.getCmd());
            script.append(commandSeparator);
        }
        return script.toString();
    }

    /**
     * logs a statement before execution
     * @param utils the DBUils
     * @param sqlCmd the statement
     * @param sqlParams the params
     */
    protected void logStmt(DBUtils utils, String sqlCmd, Object[] sqlParams, boolean comment)
    {
        // Debug
        if (!log.isDebugEnabled())
            return;
        // Comment
        if (comment)
            log.debug("SQL Comment: {}{}", utils.LOG_NEW_LINE, sqlCmd);
        // Log with or without parameters
        else if (sqlParams!=null && sqlParams.length>0)
            log.debug("Executing Stmt: {}{}{}with params: [{}]", utils.LOG_NEW_LINE, sqlCmd, utils.LOG_NEW_LINE, utils.paramsToString(sqlParams)); 
        else
            log.debug("Executing Stmt: {}{}", utils.LOG_NEW_LINE, sqlCmd);
    }
    
    /**
     * Executes a single statement 
     * @param dbms the dbms
     * @param sqlCmd the sql statement
     * @param sqlParams the statement params
     * @param conn the connection
     * @return number of rows affected
     * @throws SQLException
     */
    protected int executeStmt(DBMSHandler dbms, String sqlCmd, Object[] sqlParams, Connection conn) throws SQLException
    {        
        // Execute Statement
        int count = dbms.executeSQL(sqlCmd, sqlParams, conn, null);
        return count;
    }
}
