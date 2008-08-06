package org.apache.empire.struts2.websample.web;

import java.util.HashMap;

import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.empire.struts2.web.WebSession;


public class SampleSession implements WebSession
{
    // Logger
    protected static Log log = LogFactory.getLog(SampleSession.class);

    // Non-Static
    private SampleApplication application = null;
    public SampleUser user = null;
    public HashMap<String, Object> objectMap = new HashMap<String, Object>();
    
    // Init Session
    public void init(HttpSession session, Object application)
    {
        this.application = (SampleApplication) application;
        if (this.application==null)
        {
            throw new RuntimeException("Fatal: Application object is null!");
        }
        log.info("Session created ");
    }

    // Get Application
    public SampleApplication getApplication()
    {
        return application;
    }

    public SampleUser getUser()
    {
        return user;
    }

    public void setUser(SampleUser user)
    {
        this.user = user;
    }
    
    public Object getObject(String name)
    {
        return objectMap.get(name);
    }
    
    public final Object getObject(Class objclass)
    {
        return getObject(objclass.getName());
    }
    
    public Object setObject(String name, Object obj)
    {
        return objectMap.put(name, obj);
    }
    
    public final Object setObject(Class objclass, Object obj)
    {
        return setObject(objclass.getName(), obj);
    }
    
}
