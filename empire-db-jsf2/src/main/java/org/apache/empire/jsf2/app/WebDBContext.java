package org.apache.empire.jsf2.app;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.sql.Connection;

import javax.faces.context.FacesContext;

import org.apache.empire.commons.ClassUtils;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.context.DBContextBase;
import org.apache.empire.db.context.DBRollbackManager;
import org.apache.empire.dbms.DBMSHandler;
import org.apache.empire.exceptions.ItemNotFoundException;
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
public class WebDBContext<DB extends DBDatabase> extends DBContextBase implements Serializable
{
    private static final long serialVersionUID = 1L;

    private static final Logger    log = LoggerFactory.getLogger(WebDBContext.class);

    protected final transient WebApplication app;
    protected final transient DB             database;
    protected final transient DBMSHandler    dbms;

    /**
     * Custom serialization for transient fields.
     */ 
    private void writeObject(ObjectOutputStream strm) throws IOException 
    {   // Database
        strm.writeObject((database!=null ? database.getIdentifier() : ""));
        // write the rest
        strm.defaultWriteObject();
    }
    
    /**
     * Custom deserialization for transient fields.
     */ 
    private void readObject(ObjectInputStream strm) 
        throws IOException, ClassNotFoundException
    {   // WebApplication
        WebApplication app = WebApplication.getInstance();
        ClassUtils.setPrivateFieldValue(WebDBContext.class, this, "app", app);
        // Database
        String dbid = String.valueOf(strm.readObject());
        DBDatabase database = DBDatabase.findByIdentifier(dbid);
        if (database==null)
            throw new ItemNotFoundException(dbid);
        ClassUtils.setPrivateFieldValue(WebDBContext.class, this, "database", database);
        ClassUtils.setPrivateFieldValue(WebDBContext.class, this, "dbms",     database.getDbms());
        // read the rest
        strm.defaultReadObject();
    }
    
    public WebDBContext(DB db)
    {
        this.app    = WebApplication.getInstance();
        this.dbms = db.getDbms();
        this.database = db;
        // check dbms
        if (db.getDbms() == null)
            log.warn("Database {} has no dbms attached.", db.getClass().getSimpleName());
    }

    public DB getDatabase()
    {
        return database;
    }

    @Override
    public DBMSHandler getDbms()
    {
        return dbms;
    }
    
    @Override
    public boolean isPreparedStatementsEnabled()
    {
        return database.isPreparedStatementsEnabled();
    }

    @Override
    public boolean isRollbackHandlingEnabled()
    {
        return true;
    }

    /**
     * Unclear weather this is any useful here
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
    }

    @Override
    protected Connection getConnection(boolean create)
    {
        return app.getConnectionForRequest(getFacesContext(), database, create);
    }

    @Override
    protected DBRollbackManager getRollbackManager(boolean create)
    {
        return app.getRollbackManagerForRequest(getFacesContext(), create);
    }
    
    public FacesContext getFacesContext()
    {
        return FacesContext.getCurrentInstance();
    }
    
}
