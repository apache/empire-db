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

import org.apache.empire.EmpireException;
import org.apache.empire.commons.ErrorObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * DBSQLScript<br>
 * This class is a collection of sql command strings.<br>
 * The class is used for obtaining and executing DDL commands supplied
 * by the database driver (@see {@link DBDatabaseDriver#getDDLScript(DBCmdType, DBObject, DBSQLScript)}) 
 */
public class DBSQLScript extends ErrorObject implements Iterable<String>
{
    // Logger
    private static final Logger log = LoggerFactory.getLogger(DBSQLScript.class);
    private static final String DEFAULT_COMMAND_SEPARATOR = ";\r\n\r\n";

    // Properties
    protected String COMMAND_SEPARATOR = DEFAULT_COMMAND_SEPARATOR; 
    protected ArrayList<String> sqlCmdList = new ArrayList<String>();
    
    /**
     * Adds a statement to the script.
     * @param sql the statement
     * @return true if successful
     */
    public void addStmt(String sql)
    {
        sqlCmdList.add(sql);
    }
    
    /**
     * Adds a statement to the script.<br>
     * The supplied StringBuilder will be reset to a length of 0
     * @param sql the statement
     * @return true if successful
     */
    public final void addStmt(StringBuilder sql)
    {
        addStmt(sql.toString());
        // Clear Builder
        sql.setLength(0);
    }
    
    /**
     * Returns the number of statements in this script
     * @return number of statements in this script
     */
    public int getCount()
    {
        return sqlCmdList.size();
    }
    
    /**
     * Returns the statement at the given index
     * @param i index of the statement to retrieve
     * @return the sql statement
     */
    public String getStmt(int i)
    {
        return sqlCmdList.get(i);
    }
    
    /**
     * Clears the script and delets all statements
     */
    public void clear()
    {
        sqlCmdList.clear();
    }
    
    /**
     * Runs all SQL Statements using the supplied driver and connection.
     * @param driver the driver used for statement execution
     * @param conn the connection
     * @param ignoreErrors true if errors should be ignored
     * @return true if the script has been run successful or false otherwise 
     */
    public void run(DBDatabaseDriver driver, Connection conn, boolean ignoreErrors)
    {
        log.debug("Running script containing " + String.valueOf(getCount()) + " statements.");
        for(String stmt : sqlCmdList)
        {
            try {
                // Execute Statement
                log.debug("Executing: " + stmt);
                driver.executeSQL(stmt, null, conn, null);
            } catch(SQLException e) {
                // SQLException
                log.error(e.toString(), e);
                if (ignoreErrors==false)
                {   // forward exception
                    String msg = driver.extractErrorMessage(e);
                    throw new EmpireException(DBErrors.SQLException, new Object[] { msg }, e);
                }    
                // continue
                log.debug("Ignoring error. Continuing with script...");
            }
        }
        log.debug("Script completed.");
    }
    
    /**
     * Returns an iterator
     */
    public Iterator<String> iterator()
    {
        return sqlCmdList.iterator();
    }
    
    /**
     * Returns the sql script as a string
     */
    @Override
    public String toString()
    {
        StringBuilder script = new StringBuilder();
        for(String stmt : sqlCmdList)
        {
            script.append(stmt);
            script.append(COMMAND_SEPARATOR);
        }
        return script.toString();
    }
}
