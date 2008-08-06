package org.apache.empire.struts2.websample.web;

import java.sql.Connection;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.empire.struts2.web.EmpireStrutsDispatcher;
import org.apache.empire.struts2.web.WebRequest;


public class SampleRequest implements WebRequest
{
    // Logger
    protected static Log log = LogFactory.getLog(SampleRequest.class);

    private HttpServletRequest  httpRequest;
    private HttpServletResponse httpResponse;
    private SampleSession       session;
    private Connection          connection;  // Connection for this request
    
    public static SampleRequest getInstance()
    {
        return (SampleRequest)EmpireStrutsDispatcher.getCurrentRequest();        
    }
    
    public boolean init(HttpServletRequest request, HttpServletResponse response, Object session)
    {
        this.httpRequest = request;
        this.httpResponse = response;
        // Set Internal objects
        this.session = (SampleSession)session;
        if (this.session==null)
        {   // Error
            log.fatal("Internal Error: Session object is null");
            return false;
        }
        // continue processing
        return true;
    }

    public void exit(int exitCode)
    {
        // Cleanup
        if (connection!=null)
        {   // Commit or rollback connection depending on the exit code
            if (exitCode>=0)
            {   // 0 or positive exitCode indicates success
                log.debug("Request ended sucessfully. Committing database changes.");
                getApplication().getDatabase().commit(connection);
            }
            else 
            {   // negative exitCode indicates an error
                log.warn("Request ended with errors. Database changes will be rolled back.");
                getApplication().getDatabase().rollback(connection);
            }
            // Release Connection
            getApplication().releaseConnection(connection);
            connection= null;
        }
        // Release objects
        this.httpRequest = null;
        this.httpResponse = null;
    }

    // Get Session
    public SampleSession getSession()
    {
        return session;
    }

    // Get Application
    public SampleApplication getApplication()
    {
        return session.getApplication();
    }

    // Get Connection
    public Connection getConnection()
    {
        // Get a Connection for the Connection Pool 
        if (connection==null)
            connection= getApplication().getPooledConnection();
        // return connection
        return connection;
    }
    
    public HttpServletRequest getHttpRequest()
    {
        return httpRequest;
    }

    public HttpServletResponse getHttpResponse()
    {
        return httpResponse;
    }
    
}
