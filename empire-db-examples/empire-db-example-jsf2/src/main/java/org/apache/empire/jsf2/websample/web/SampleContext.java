/*
 * ESTEAM Software GmbH, 19.01.2022
 */
package org.apache.empire.jsf2.websample.web;

import java.sql.Connection;

import javax.faces.context.FacesContext;

import org.apache.empire.db.DBDatabaseDriver;
import org.apache.empire.db.context.DBContextBase;
import org.apache.empire.jsf2.websample.db.SampleDB;

public class SampleContext extends DBContextBase
{
    private final SampleApplication app;
    
    private final SampleDB db;
    
    public SampleContext(SampleApplication app)
    {
        this.app = app;
        this.db = app.getDatabase(); 
    }
    
    public SampleDB getDatabase()
    {
        return db;
    }

    public SampleUser getUser()
    {
        return SampleUtils.getSampleUser();
    }
    
    @Override
    public DBDatabaseDriver getDriver()
    {
        return getDatabase().getDriver();
    }

    @Override
    public Connection getConnection()
    {
        FacesContext fc = FacesContext.getCurrentInstance();
        return app.getConnectionForRequest(fc, db);
    }
}
