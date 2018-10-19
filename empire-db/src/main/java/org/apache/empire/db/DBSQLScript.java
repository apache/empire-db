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

import org.apache.empire.db.exceptions.EmpireSQLException;
import org.apache.empire.exceptions.InvalidArgumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DBSQLScript<br>
 * This class is a collection of sql command strings.<br>
 * The class is used for obtaining and executing DDL commands supplied by the
 * database driver (@see {@link DBDatabaseDriver#getDDLScript(DBCmdType, DBObject, DBSQLScript)})
 */
public class DBSQLScript implements Iterable<String>
{
    // Logger
    private static final Logger log = LoggerFactory.getLogger(DBSQLScript.class);
    private static final String DEFAULT_COMMAND_SEPARATOR = ";\r\n\r\n";

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

    public DBSQLScript()
    {
        // nothing
    }

    public DBSQLScript(String commandSeparator)
    {
        this.commandSeparator = commandSeparator;
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
    public final void addStmt(StringBuilder sql)
    {
        addStmt(sql.toString());
        // Clear Builder
        sql.setLength(0);
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
     * Executes all SQL Statements one by one using the supplied driver and connection.
     * 
     * @param driver the driver used for statement execution
     * @param conn the connection
     * @param ignoreErrors true if errors should be ignored
     * @return number of records affected
     */
    public int executeAll(DBDatabaseDriver driver, Connection conn, boolean ignoreErrors)
    {
        log.debug("Running script containing " + String.valueOf(getCount()) + " statements.");
        int result = 0;
        for (SQLStmt stmt : sqlStmtList)
        {
            try
            {
                // Execute Statement
                log.debug("Executing: {}", stmt.getCmd());
                int count = driver.executeSQL(stmt.getCmd(), stmt.getParams(), conn, null);
                result += (count >= 0 ? count : 0);
            }
            catch (SQLException e)
            {
                // SQLException
                log.error(e.toString(), e);
                if (ignoreErrors == false)
                { // forward exception
                    throw new EmpireSQLException(driver, e);
                }
                // continue
                log.debug("Ignoring error. Continuing with script...");
            }
        }
        log.debug("Script completed. {} records affected.", result);
        return result;
    }

    /**
     * Executes all SQL Statements one by one using the supplied driver and connection.
     * 
     * @param driver the driver used for statement execution
     * @param conn the connection
     * @return number of records affected
     */
    public final int executeAll(DBDatabaseDriver driver, Connection conn)
    {
        return executeAll(driver, conn, false);
    }

    /**
     * Executes all SQL Statements as a JDBC Batch Job.
     * 
     * @param driver the driver used for statement execution
     * @param conn the connection
     * @param ignoreErrors true if errors should be ignored
     */
    public int executeBatch(DBDatabaseDriver driver, Connection conn)
    {
        log.debug("Running batch containing " + String.valueOf(getCount()) + " statements.");
        try
        {
            // Execute Statement
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
            int[] res = driver.executeBatch(cmdList, paramList, conn);
            for (count = 0, i = 0; i < (res != null ? res.length : 0); i++)
                count += (res[i] >= 0 ? res[i] : 0);
            log.debug("Script completed. {} records affected.", count);
            return count;
        }
        catch (SQLException e)
        {
            // SQLException
            log.error(e.toString(), e);
            throw new EmpireSQLException(driver, e);
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
}
