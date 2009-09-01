/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.empire.struts2.web;

import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.empire.struts2.action.Disposable;
import org.apache.struts2.dispatcher.Dispatcher;
import org.apache.struts2.dispatcher.mapper.ActionMapping;


public class EmpireStrutsDispatcher extends Dispatcher
{
    // Logger
    protected static Log log = LogFactory.getLog(EmpireStrutsDispatcher.class);

    private static final ThreadLocal<Object> currentRequest = new ThreadLocal<Object>();

    public final String APPLICATION_CLASS = "ApplicationClass";
    public final String SESSION_CLASS     = "SessionClass";
    public final String REQUEST_CLASS     = "RequestClass";

    private String sessionClassName = null;
    private String requestClassName = null;
    
    private boolean logRequestWarning = true;
    
    // Accessor for Current Request
    public static Object getCurrentRequest()
    {
        return currentRequest.get();
    }
    
    public EmpireStrutsDispatcher(ServletContext servletContext, Map<String, String> initParams)
    {
        super(servletContext, initParams);
        // Create Applicaton Object (if specified)
        String appClassName = initParams.get( APPLICATION_CLASS );
        if (appClassName!=null)
        {
            createApplicationObject( servletContext, appClassName);
        }   
        
        // remember Session Class Name
        sessionClassName = initParams.get( SESSION_CLASS );
        requestClassName = initParams.get( REQUEST_CLASS );

        // if no request class is specified use default
        if (requestClassName==null || requestClassName.length()==0)
            requestClassName = DefaultWebRequest.class.getName();
    }
    
    @Override
    public void serviceAction(HttpServletRequest request, HttpServletResponse response, ServletContext context,
            ActionMapping mapping) throws ServletException
    {
        try {
            // Create Applicaton Class
            if (sessionClassName!=null)
            {
                Object applObj = context.getAttribute( WebApplication.APPLICATION_NAME );
                createSessionObject(request, applObj);
            }
            // Create Applicaton Class
            boolean continueProcessing = true; 
            if (requestClassName!=null)
            {
                Object sessObj = request.getSession().getAttribute( WebSession.SESSION_NAME );
                continueProcessing = createRequestObject(request, response, sessObj);
            }
            // Call Default
            if (continueProcessing)
            {
                log.debug("calling Struts for request processing");
                super.serviceAction(request, response, context, mapping);
            }
          
        } catch (Exception e) {
            
            log.error("Failed to process request due to Exception.", e);

        } finally {
            // cleanup
            log.debug("cleanung up request");
            // Dispose action first
            int exitCode = disposeAction(request);
            // Call Exit on Request
            Object reqObj = currentRequest.get();
            if (reqObj!=null)
            {
                if (reqObj instanceof WebRequest)
                {
                    ((WebRequest)reqObj).exit(exitCode);
                }
                // Clear Thread local
                currentRequest.set( null );
            }
        }
    }

    /*
    @Override
    public HttpServletRequest wrapRequest(HttpServletRequest request, ServletContext servletContext)
        throws IOException
    {
        HttpServletRequest wrappedRequest = super.wrapRequest(request, servletContext);
        if (wrappedRequest instanceof MultiPartRequestWrapper)
        {
            boolean hasError = ((MultiPartRequestWrapper)wrappedRequest).hasErrors();
            if (hasError)
            {   // Multipart-Error!   
                log.error("Multipart-Request has errors. Use normal Wrapper");
                return new StrutsRequestWrapper(request);
            }
        }
        return wrappedRequest;
    }
    */

    private int disposeAction(HttpServletRequest request)
    {
        int exitCode = 0;
        // Is the action still the?
        Object action = request.getAttribute("action");
        if (action!=null)
        {
            if (action instanceof Disposable)
            {   // Call dispose and return exitCode
                exitCode = ((Disposable)action).dispose();
            }
            else
            {   // Disposible interface not implemented
                log.warn("Cannot dispose action. Action does not implement the Disposible interface)");
            }
            // Cleanup the "action" Attribute on the HttpServletRequest
            request.setAttribute("action", null);
        }
        else
        {
            log.warn("Cannot dispose action. Action is not supplied on the request. (Missing ActionBasicsInterceptor?)");
        }
        // Exit Code
        return exitCode;
    }
    
    private Object createObject(String className)
        throws ClassNotFoundException, IllegalAccessException, InstantiationException
    {
        // Create Instance for Object  
        Class c = Class.forName(className);
        return c.newInstance();
    }

    protected void createApplicationObject(ServletContext servletContext, String appClassName)
    {
        try {
            // Create Applicaton Class  
            if (servletContext.getAttribute( WebApplication.APPLICATION_NAME )==null)
            {
                Object app = createObject( appClassName );
                if (app instanceof WebApplication)
                {
                    ((WebApplication)app).init( servletContext );
                }
                else
                    log.warn("Application object does not implement IWebApplication!");
                // Store it
                servletContext.setAttribute( WebApplication.APPLICATION_NAME, app);
            }    
        } catch(Exception e) {
            log.error("Failed to create web application object of type " + appClassName + " --> " + e.getMessage());
        }
    }
    
    protected void createSessionObject(HttpServletRequest request, Object applObj)
    {
        try {
            // createSessionObject
            HttpSession httpSession = request.getSession(false);
            if (httpSession==null ||  httpSession.getAttribute( WebSession.SESSION_NAME)==null)
            {
                Object session = createObject(sessionClassName);
                httpSession = request.getSession(true);
                // Call init() if Session Object implements IWebSession 
                if (session instanceof WebSession)
                {
                    ((WebSession)session).init( httpSession, applObj );
                }
                else
                    log.warn("Session object does not implement IWebSession!");
                // Save Session
                httpSession.setAttribute( WebSession.SESSION_NAME , session);
            }
        } catch(Exception e) {
            // Error  
            log.error("Failed to create session [" + sessionClassName + "] msg= " + e.getMessage());
        }    
    }

    protected boolean createRequestObject(HttpServletRequest request, HttpServletResponse response, Object sessObj)
    {
        try {
            // createSessionObject
            Object reqObj = request.getAttribute( WebRequest.REQUEST_NAME );
            if (reqObj==null)
            {
                // Create Request
                reqObj = createObject( requestClassName );
                // Store Request in Thread Local variable
                currentRequest.set( reqObj );
                // Call init() if Request Object implements IWebRequest 
                if (reqObj instanceof WebRequest)
                {
                    return ((WebRequest)reqObj).init( request, response, sessObj );
                }
                else if (logRequestWarning)
                {
                    log.warn("Request object does not implement IWebRequest!");
                    logRequestWarning = false;
                }
            }
            // continue processing
            return true;

        } catch(Exception e) {
            // Error  
            log.error("Failed to create request [" + requestClassName + "] msg= " + e.getMessage());
            requestClassName = null; // don't try again
            // continue processing
            return true;
        }    
    }
    
}
