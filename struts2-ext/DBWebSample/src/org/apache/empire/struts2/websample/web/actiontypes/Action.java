package org.apache.empire.struts2.websample.web.actiontypes;

import java.sql.Connection;
import java.util.Locale;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.empire.struts2.action.WebAction;
import org.apache.empire.struts2.websample.db.SampleDB;
import org.apache.empire.struts2.websample.web.SampleApplication;
import org.apache.empire.struts2.websample.web.SampleContext;
import org.apache.empire.struts2.websample.web.SampleRequest;
import org.apache.empire.struts2.websample.web.SampleSession;
import org.apache.empire.struts2.websample.web.SampleUser;


@SuppressWarnings("serial")
public abstract class Action extends WebAction
    implements SampleContext
{
    // Logger
    @SuppressWarnings("hiding")
    protected static Log log = LogFactory.getLog(Action.class);

    public Action()
    {
        // Constructor
    }

    // Login requried for this action
    @Override
    public boolean loginRequired()
    {
        return (getSession().getUser()==null);
    }
    
    @Override
    public Locale getLocale()
    {
        /*
        Locale loc = super.getLocale();
        if (loc.equals(Locale.GERMAN))
            return loc;
        */    
        return Locale.ENGLISH; 
    }
    
    // Request
    public static SampleRequest getRequest() 
    {
        return SampleRequest.getInstance();
    }
    
    // Application getters
    public SampleApplication getApplication()
    {
        return getRequest().getApplication();
    }
    
    public SampleSession getSession()
    {
        return getRequest().getSession();
    }

    // ------ Sample Context Implementation ------
    
    public SampleDB getDatabase()
    {
        return getApplication().getDatabase();
    }
    
    public SampleUser getUser()
    {
        return getSession().getUser();
    }
    
    @Override
    public Connection getConnection()
    {
        return getRequest().getConnection();
    }
    
}
