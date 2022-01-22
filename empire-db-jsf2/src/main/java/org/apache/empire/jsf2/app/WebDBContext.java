package org.apache.empire.jsf2.app;

import java.sql.Connection;

import javax.faces.context.FacesContext;

import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.DBDatabaseDriver;
import org.apache.empire.db.context.DBContextBase;
import org.apache.empire.db.context.DBRollbackManager;
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
    private static final Logger    log = LoggerFactory.getLogger(WebDBContext.class);

    private final WebApplication   app;
    private final DBDatabaseDriver driver;
    private final DB               database;

    public WebDBContext(WebApplication app, DB db)
    {
        this.app = app;
        this.driver = db.getDriver();
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
        return driver;
    }

    @Override
    public boolean isRollbackHandlingEnabled()
    {
        return true;
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

    @Override
    protected Connection getConnection(boolean create)
    {
        FacesContext fc = FacesContext.getCurrentInstance();
        return this.app.getConnectionForRequest(fc, database, create);
    }

    @Override
    protected DBRollbackManager getRollbackManager(boolean create)
    {
        FacesContext fc = FacesContext.getCurrentInstance();
        return this.app.getRollbackManagerForRequest(fc, create);
    }
    
}
