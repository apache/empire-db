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
import org.apache.empire.db.DBRollbackHandler;
import org.apache.empire.db.exceptions.EmpireSQLException;
import org.apache.empire.exceptions.InvalidArgumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class DBContextBase implements DBContext
{
    // Logger
    private static final Logger log = LoggerFactory.getLogger(DBContextBase.class);
    
    private Map<DBObject, DBRollbackHandler> rollbackHandler;
    
    @Override
    public synchronized void commit()
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
            if (rollbackHandler!=null)
                for (DBRollbackHandler handler : rollbackHandler.values())
                    handler.discard();
            // Done
            return;
        } catch (SQLException sqle) { 
            // Commit failed!
            throw new EmpireSQLException(getDriver(), sqle);
        } finally {
            rollbackHandler=null;
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
    public synchronized void rollback()
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
            if (rollbackHandler!=null)
                for (DBRollbackHandler handler : rollbackHandler.values())
                    handler.rollback();
            // Done
            return;
        } catch (SQLException sqle) { 
            // Commit failed!
            throw new EmpireSQLException(getDriver(), sqle);
        } finally {
            rollbackHandler=null;
        }
    }
    
    @Override
    public synchronized void addRollbackHandler(DBRollbackHandler handler)
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
    public synchronized void removeRollbackHandler(DBObject object)
    {
        if (rollbackHandler!=null && rollbackHandler.containsKey(object))
            log.info("test");
        
        if (object==null)
            rollbackHandler=null;   // remove all
        else if (rollbackHandler!=null && rollbackHandler.remove(object)!=null)
            log.info("Rollback handler for object {} was removed", object.getClass().getSimpleName());
    }

    @Override
    public void discard()
    {
        /* don't close connection! */
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
