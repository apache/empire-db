/*
 * ESTEAM Software GmbH, 19.01.2022
 */
package org.apache.empire.db.context;

import java.sql.Connection;
import java.sql.SQLException;

import org.apache.empire.db.DBDatabaseDriver;
import org.apache.empire.db.exceptions.EmpireSQLException;

public class DBContextStatic extends DBContextBase
{
    private final DBDatabaseDriver driver;
    private final Connection conn;
    private final boolean closeOnDiscard;
    
    public DBContextStatic(DBDatabaseDriver driver, Connection conn, boolean closeOnDiscard)
    {
        this.driver = driver;
        this.conn = conn;
        this.closeOnDiscard = closeOnDiscard;
    }

    public DBContextStatic(DBDatabaseDriver driver, Connection conn)
    {
        this(driver, conn, false);
    }

    @Override
    public DBDatabaseDriver getDriver()
    {
        return driver;
    }

    @Override
    public Connection getConnection()
    {
        return conn;
    }
    
    @Override
    public void discard()
    {
        super.discard();
        // close
        if (closeOnDiscard)
            closeConnection();
    }
}
