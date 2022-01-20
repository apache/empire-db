package org.apache.empire.jsf2.app;

import java.sql.Connection;

import javax.faces.context.FacesContext;

import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.DBDatabaseDriver;
import org.apache.empire.db.context.DBContextBase;
import org.apache.empire.exceptions.NotSupportedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the basic implementation of a DBContext for a web application
 * Important: The connection is obtained on HttpRequest scope and hot held by the context
 * The connection is automatically released via the FacesRequestPhaseListener
 * @author rainer
 *
 * @param <DB>
 */
public class WebDBContext<DB extends DBDatabase> extends DBContextBase
{
    private static final Logger  log  = LoggerFactory.getLogger(WebDBContext.class);

    private final WebApplication app;
    private final DB             database;

    // Held for request only
    private Connection           conn;

    public WebDBContext(WebApplication app, DB db)
    {
        this.app = app;
        this.database = db;
        // check driver
        if (db.getDriver() == null)
            log.warn("Database {} has no driver attached.", db.getClass().getSimpleName());
    }

    public DB getDatabase()
    {
        return database;
    }

    @Override
    public DBDatabaseDriver getDriver()
    {
        return database.getDriver();
    }

    /**
     * gets a Connection for the current request
     * IMPORTANT: Do not hold the connection!
     */
    @Override
    public Connection getConnection()
    {
        if (conn==null)
        {   // get a new connection
            FacesContext fc = FacesContext.getCurrentInstance();
            conn = this.app.getConnectionForRequest(fc, this);
        }
        return conn;
    }

    @Override
    public void commit()
    {
        if (conn!=null)
            super.commit();
        else
            log.info("No Connection to commmit changes");
    }

    @Override
    public void rollback()
    {
        if (conn!=null)
            super.rollback();
        else
            log.info("No Connection to rollback changes");
    }
    
    public void releaseConnection(boolean commitPerformed)
    {
        this.conn = null;
        // commit or rollback?
        if (commitPerformed)
            discardAllHandlers();
        else
            rollbackAllHandlers();
    }

    /**
     * Unclear weather this is any useful
     * Do not call, as the connections are managed by the Application
     */
    @Override
    public void discard()
    {
        /*
        FacesContext fc = FacesContext.getCurrentInstance();
        this.app.releaseConnection(fc, database);
        // discard
        super.discard();
        */
        throw new NotSupportedException(this, "discard");
    }
}
