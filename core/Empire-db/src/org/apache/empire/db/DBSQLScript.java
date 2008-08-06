/*
 * ESTEAM Software GmbH, 05.04.2008
 */
package org.apache.empire.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.empire.commons.ErrorObject;


/**
 * DBSQLScript<br>
 * This class is a collection of sql command strings.<br>
 * The class is used for obtaining and executing DDL commands supplied
 * by the database driver (@see {@link DBDatabaseDriver#getDDLScript(DBCmdType, DBObject, DBSQLScript)}) 
 */
public class DBSQLScript extends ErrorObject implements Iterable<String>
{
    // Logger
    private static final Log log = LogFactory.getLog(DBSQLScript.class);
    
    private ArrayList<String> sqlCmdList = new ArrayList<String>();
    private static final String COMMAND_SEPARATOR = ";\r\n\r\n";
    
    /**
     * Adds a statement to the script.
     * @param sql the statement
     * @return true if successful
     */
    public boolean addStmt(String sql)
    {
        sqlCmdList.add(sql);
        return success();
    }
    
    /**
     * Adds a statement to the script.<br>
     * The supplied StringBuilder will be reset to a length of 0
     * @param sql the statement
     * @return true if successful
     */
    public final boolean addStmt(StringBuilder sql)
    {
        if (!addStmt(sql.toString()))
            return false;
        // Clear Builder
        sql.setLength(0);
        return true;
    }
    
    /**
     * Returns the number of statemetns in this script
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
    public boolean run(DBDatabaseDriver driver, Connection conn, boolean ignoreErrors)
    {
        log.debug("Running script containing " + String.valueOf(getCount()) + " statements.");
        for(String stmt : sqlCmdList)
        {
            try {
                // Execute Statement
                log.debug("Executing: " + stmt);
                driver.executeSQL(stmt, null, conn);
            } catch(SQLException e) {
                // SQLException
                log.error(e.toString(), e);
                if (ignoreErrors==false)
                    return error(DBErrors.SQLException, e);
                // continue
                log.debug("Ignoring error. Continuing with script...");
            }
        }
        log.debug("Script completed.");
        return success();
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
