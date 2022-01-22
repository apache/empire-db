/*
 * ESTEAM Software GmbH, 19.01.2022
 */
package org.apache.empire.db.context;

import java.sql.Connection;
import java.sql.SQLException;

import org.apache.empire.db.DBContext;
import org.apache.empire.db.DBObject;
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
            DBRollbackManager dbrm = (isEnableRollbackHandling() ? getRollbackManager(false) : null);
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
            DBRollbackManager dbrm = (isEnableRollbackHandling() ? getRollbackManager(false) : null);
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
        if (!isEnableRollbackHandling())
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
        if (!isEnableRollbackHandling())
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
