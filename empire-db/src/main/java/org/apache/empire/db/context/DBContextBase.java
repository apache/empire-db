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
package org.apache.empire.db.context;

import java.sql.Connection;
import java.sql.SQLException;

import org.apache.empire.db.DBCommand;
import org.apache.empire.db.DBContext;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.DBObject;
import org.apache.empire.db.DBTable;
import org.apache.empire.db.DBUtils;
import org.apache.empire.db.context.DBRollbackManager.ReleaseAction;
import org.apache.empire.db.exceptions.EmpireSQLException;
import org.apache.empire.exceptions.InvalidArgumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DBContextBase
 * Basic implementation of the DBContext interface which can be used as a base class for own implmentations
 * @author rainer
 */
public abstract class DBContextBase implements DBContext
{
    // Logger
    private static final Logger log = LoggerFactory.getLogger(DBContextBase.class);
    
    private DBUtils utils = null;
    
    private boolean noRollbackManagerWarnOnce = true;
    
    /**
     * Factory function for Utils creation 
     * @return the utils implementation
     */
    protected DBUtils createUtils()
    {
        return new DBUtils(this);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends DBUtils> T getUtils()
    {
        if (utils==null)
            utils = createUtils();
        return ((T)utils);
    }

    protected abstract Connection getConnection(boolean required);

    protected abstract DBRollbackManager getRollbackManager(boolean required);

    @Override
    public Connection getConnection()
    {
        return getConnection(true);
    }
    
    /**
     * Creates a new Command object for the given database
     * 
     * @return the command object.
     */
    @Override
    public final DBCommand createCommand(DBDatabase db)
    {
        return getDriver().createCommand(db);
    }

    /**
     * Executes an SQLStatment
     * @param sqlCmd the SQL-Command
     * @param sqlParams a list of objects to replace sql parameters
     */
    @Override
    public final int executeSQL(String sqlCmd, Object[] sqlParams)
    {
        if (utils==null) getUtils(); 
        return utils.executeSQL(sqlCmd, sqlParams, null); 
    }

    /**
     * Executes an Insert statement from a command object
     * @param cmd the command object containing the insert command
     * @return the number of records that have been inserted with the supplied statement
     */
    @Override
    public final int executeInsert(DBCommand cmd)
    {
        if (utils==null) getUtils(); 
        return utils.executeSQL(cmd.getInsert(), cmd.getParamValues(), null); 
    }

    /**
     * Executes an InsertInfo statement from a command object
     * @param table the table into which to insert the selected data
     * @param cmd the command object containing the selection command 
     * @return the number of records that have been inserted with the supplied statement
     */
    @Override
    public final int executeInsertInto(DBTable table, DBCommand cmd)
    {
        if (utils==null) getUtils(); 
        return utils.executeSQL(cmd.getInsertInto(table), cmd.getParamValues(), null); 
    }

    /**
     * Executes an Update statement from a command object
     * @param cmd the command object containing the update command
     * @return the number of records that have been updated with the supplied statement
     */
    @Override
    public final int executeUpdate(DBCommand cmd)
    {
        if (utils==null) getUtils(); 
        return utils.executeSQL(cmd.getUpdate(), cmd.getParamValues(), null); 
    }

    /**
     * Executes a Delete statement from a command object
     * @param from the database table from which to delete records
     * @param cmd the command object containing the delete constraints
     * @return the number of records that have been deleted with the supplied statement
     */
    @Override
    public final int executeDelete(DBTable from, DBCommand cmd)
    {
        if (utils==null) getUtils(); 
        return utils.executeSQL(cmd.getDelete(from), cmd.getParamValues(), null); 
    }
    
    @Override
    public void commit()
    {
        try
        {   // Check argument
            Connection conn = getConnection(false);
            if (conn==null)
            {   log.info("No Connection to commmit changes");
                return; // Nothing to do
            }
            // Commit
            if (conn.getAutoCommit()==false)
                conn.commit();
            // discard rollbacks
            DBRollbackManager dbrm = (isRollbackHandlingEnabled() ? getRollbackManager(false) : null);
            if (dbrm!=null)
                dbrm.releaseConnection(conn, ReleaseAction.Discard);
            // Done
            return;
        } catch (SQLException sqle) { 
            // Commit failed!
            throw new EmpireSQLException(getDriver(), sqle);
        }
    }

    /**
     * Discards all changes made since the previous commit/rollback
     * and releases any database locks currently held by this
     * Connection.
     * <P>
     * @param conn a valid database connection
     */
    @Override
    public void rollback()
    {
        try
        {   // Check argument
            Connection conn = getConnection(false);
            if (conn==null)
            {   log.info("No Connection to rollback changes");
                return; // Nothing to do
            }
            // rollback
            log.info("Database rollback issued!");
            conn.rollback();
            // perform Rollback
            DBRollbackManager dbrm = (isRollbackHandlingEnabled() ? getRollbackManager(false) : null);
            if (dbrm!=null)
                dbrm.releaseConnection(conn, ReleaseAction.Rollback);
            // Done
            return;
        } catch (SQLException sqle) { 
            // Commit failed!
            throw new EmpireSQLException(getDriver(), sqle);
        }
    }
    
    @Override
    public void appendRollbackHandler(DBRollbackHandler handler)
    {
        if (handler==null || handler.getObject()==null)
            throw new InvalidArgumentException("handler", handler);
        // Check enabled
        if (!isRollbackHandlingEnabled())
        {   log.warn("*** Rollback handling is disabled for this context. AppendRollbackHandler must not be called! ***");
            return;
        }
        // Add handler
        DBRollbackManager dbrm = getRollbackManager(true);
        if (dbrm!=null)
            dbrm.appendHandler(getConnection(true), handler);
        else if (noRollbackManagerWarnOnce)
        {   log.warn("*** No DBRollbackManager provided! Rollbacks will be disabled. ***");
            noRollbackManagerWarnOnce = false;
        }
    }
    
    @Override
    public void removeRollbackHandler(DBObject object)
    {
        if (object==null)
            throw new InvalidArgumentException("object", object);
        // Check enabled
        if (!isRollbackHandlingEnabled())
        {   log.warn("*** Rollback handling is disabled for this context. RemoveRollbackHandler should not be called! ***");
            return;
        }
        // Remove handler
        DBRollbackManager dbrm = getRollbackManager(false);
        if (dbrm!=null)
            dbrm.removeHandler(getConnection(false), object);
    }

    /**
     * Discard connection releated ressources
     * WARING: No gurarantee it will be called
     */
    @Override
    public void discard()
    {
        /* don't close connection! */
    }
    
    /**
     * helper to close a connection on discard
     */
    protected void closeConnection()
    {   try
        {   // close connection
            Connection conn = getConnection();
            conn.close();
        } catch (SQLException sqle) { 
            // Commit failed!
            throw new EmpireSQLException(getDriver(), sqle);
        }
    }
    
}
