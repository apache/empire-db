/*
 * ESTEAM Software GmbH, 19.01.2022
 */
package org.apache.empire.db.context;

import java.sql.Connection;

import org.apache.empire.db.DBDatabaseDriver;
import org.apache.empire.db.context.DBRollbackManager.ReleaseAction;

public class DBContextStatic extends DBContextBase
{
    private final DBDatabaseDriver driver;
    private final Connection conn;
    private final boolean enableRollbackHandling;
    private final boolean closeOnDiscard;
    
    /**
     *  Global DBRollbackManager
     *  
     *  initialConnectionCapacity = 2
     *  initialObjectCapacity = 16
     */
    private static final DBRollbackManager staticRollbackManager = new DBRollbackManager(2, 16);

    /**
     * Creates a static DBContext with default options
     * @param driver
     * @param conn
     */
    public DBContextStatic(DBDatabaseDriver driver, Connection conn)
    {
        this(driver, conn, true, false);
    }
    
    /**
     * Creates a static DBContext with custom options
     * @param driver
     * @param conn
     * @param enableRollbackHandling
     * @param closeOnDiscard
     */
    public DBContextStatic(DBDatabaseDriver driver, Connection conn, boolean enableRollbackHandling, boolean closeOnDiscard)
    {
        this.driver = driver;
        this.conn = conn;
        this.enableRollbackHandling = enableRollbackHandling;
        this.closeOnDiscard = closeOnDiscard;
    }

    @Override
    public DBDatabaseDriver getDriver()
    {
        return driver;
    }

    @Override
    public boolean isRollbackHandlingEnabled()
    {
        return enableRollbackHandling;
    }
    
    @Override
    public void discard()
    {
        super.discard();
        // close
        if (closeOnDiscard) 
        {   // Close the connection
            closeConnection();
            // rollbackManager release
            if (enableRollbackHandling)
                staticRollbackManager.releaseConnection(conn, ReleaseAction.Discard);
        }
    }

    @Override
    protected Connection getConnection(boolean create)
    {
        return conn;
    }

    @Override
    protected DBRollbackManager getRollbackManager(boolean create)
    {
        return staticRollbackManager;
    }
}
