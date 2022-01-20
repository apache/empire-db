/*
 * ESTEAM Software GmbH, 19.01.2022
 */
package org.apache.empire.db.context;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.empire.db.DBContext;
import org.apache.empire.db.DBObject;
import org.apache.empire.db.DBUtils;
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
    
    private Map<DBObject, DBRollbackHandler> rollbackHandler;
    
    private DBUtils utils = null;

    @SuppressWarnings("unchecked")
    @Override
    public <T extends DBUtils> T getUtils()
    {
        if (utils==null)
            utils = createUtils();
        return ((T)utils);
    }
    
    @Override
    public void commit()
    {
        try
        {   // Check argument
            Connection conn = getConnection();
            if (conn==null)
                throw new InvalidArgumentException("conn", conn);
            // Commit
            if (conn.getAutoCommit()==false)
                conn.commit();
            // discard rollbacks
            discardAllHandlers();
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
            Connection conn = getConnection();
            if (conn==null)
                throw new InvalidArgumentException("conn", conn);
            // rollback
            log.info("Database rollback issued!");
            conn.rollback();
            // rollback
            rollbackAllHandlers();
            // Done
            return;
        } catch (SQLException sqle) { 
            // Commit failed!
            throw new EmpireSQLException(getDriver(), sqle);
        }
    }
    
    @Override
    public void addRollbackHandler(DBRollbackHandler handler)
    {
        if (rollbackHandler==null)
            rollbackHandler = new LinkedHashMap<DBObject, DBRollbackHandler>();
        // check
        DBObject object = handler.getObject();
        if (rollbackHandler.containsKey(object))
            rollbackHandler.get(object).combine(handler);
        else
            rollbackHandler.put(object, handler);
    }
    
    @Override
    public void removeRollbackHandler(DBObject object)
    {
        if (object==null)
            rollbackHandler=null;   // remove all
        else if (rollbackHandler!=null && rollbackHandler.remove(object)!=null)
            log.info("Rollback handler for object {} was removed", object.getClass().getSimpleName());
    }

    /**
     * Discard connection releated ressources
     * WARING: No gurarantee it will be called
     */
    @Override
    public void discard()
    {
        /* don't close connection! */
        discardAllHandlers();
    }
    
    /**
     * Discards all rollback handlers
     */
    protected void discardAllHandlers()
    {   // rollback
        if (rollbackHandler==null)
            return;
        for (DBRollbackHandler handler : rollbackHandler.values())
            handler.discard();
        rollbackHandler=null;
    }
    
    /**
     * Performs rollback on all rollback handlers
     */
    protected void rollbackAllHandlers()
    {   // rollback
        if (rollbackHandler==null)
            return;
        for (DBRollbackHandler handler : rollbackHandler.values())
            handler.rollback();
        rollbackHandler=null;
    }

    /**
     * Factory function for Utils creation 
     * @return the utils implementation
     */
    protected DBUtils createUtils()
    {
        return new DBUtils(this);
    }
    
    /**
     * helper to close a connection on discard
     */
    protected void closeConnection()
    {
        try
        {   // close connection
            Connection conn = getConnection();
            conn.close();
        } catch (SQLException sqle) { 
            // Commit failed!
            throw new EmpireSQLException(getDriver(), sqle);
        }
    }
    
}
