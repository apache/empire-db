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

import javax.portlet.PortletConfig;
import javax.portlet.PortletContext;
import javax.portlet.PortletException;
import javax.portlet.PortletRequest;
import javax.portlet.PortletResponse;
import javax.portlet.PortletSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.empire.struts2.action.Disposable;
import org.apache.empire.struts2.web.portlet.PortletContextWrapper;
import org.apache.empire.struts2.web.portlet.PortletRequestWrapper;
import org.apache.empire.struts2.web.portlet.PortletResponseWrapper;
import org.apache.empire.struts2.web.portlet.PortletSessionWrapper;
import org.apache.struts2.portlet.dispatcher.Jsr168Dispatcher;

public class EmpirePortletDispatcher extends Jsr168Dispatcher {
    // Logger
    protected static Logger log = LoggerFactory.getLogger(EmpireStrutsDispatcher.class);

    public final String RENDER_DIRECT_ACTION = "struts.portlet.renderDirectAction";
    
    public final String APPLICATION_CLASS = "ApplicationClass";
    public final String SESSION_CLASS     = "SessionClass";
    public final String REQUEST_CLASS     = "RequestClass";

    private String sessionClassName = null;
    private String requestClassName = null;
    
    private boolean logRequestWarning = true;

    /**
     * Initialize the portlet with the init parameters from <tt>portlet.xml</tt>
     */
    @Override
	public void init(PortletConfig cfg) throws PortletException {
        super.init(cfg);
        
        // Create Applicaton Object (if specified)
        String appClassName = cfg.getInitParameter( APPLICATION_CLASS );
        if (appClassName!=null)
        {
            createApplicationObject( cfg.getPortletContext(), appClassName);
        }   
        
        // remember Session Class Name
        sessionClassName = cfg.getInitParameter( SESSION_CLASS );
        requestClassName = cfg.getInitParameter( REQUEST_CLASS );

        // if no request class is specified use default
        if (requestClassName==null || requestClassName.length()==0)
            requestClassName = DefaultWebRequest.class.getName();
    }
    
    @Override
    public void serviceAction(PortletRequest request, PortletResponse response, Map<String, Object> requestMap, Map<String, String[]> parameterMap,
            Map<String, Object> sessionMap, Map<String, Object> applicationMap, String portletNamespace,
            Integer phase) throws PortletException
    {
        try {
            // Create Applicaton Class
            if (sessionClassName!=null)
            {
                Object applObj = applicationMap.get( WebApplication.APPLICATION_NAME );
                createSessionObject(request, applObj);
            }
            // Create Applicaton Class
            boolean continueProcessing = true; 
            if (requestClassName!=null)
            {
                Object sessObj = sessionMap.get( WebSession.SESSION_NAME );
                continueProcessing = createRequestObject(request, response, sessObj);
            }
            // Call Default
            if (continueProcessing)
            {
                if (log.isDebugEnabled())
	            	log.debug("starting portlet service request");
                
                super.serviceAction(request, response, requestMap, parameterMap, sessionMap, applicationMap, portletNamespace, phase);
            }
          
        } catch (Exception e) {
            
            log.error("Failed to process request due to Exception.", e);

        } finally {
            // cleanup
            if (log.isDebugEnabled())
	            log.debug("cleanung up request");
            // Dispose action first
            int exitCode = disposeAction(request);
            // Call Exit on Request
            EmpireThreadManager.exit(exitCode);
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

    private int disposeAction(PortletRequest request)
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
            request.removeAttribute("action");
        }
        else
        {
            log.warn("Cannot dispose action. Action is not supplied on the request. (Missing ActionBasicsInterceptor?)");
        }
        // Exit Code
        return exitCode;
    }
    
    @SuppressWarnings("rawtypes")
	private Object createObject(String className)
        throws ClassNotFoundException, IllegalAccessException, InstantiationException
    {
        // Create Instance for Object  
        Class c = Class.forName(className);
        return c.newInstance();
    }

    protected void createApplicationObject(PortletContext portletContext, String appClassName)
    {
        try {
            // Create Applicaton Class  
            if (portletContext.getAttribute( WebApplication.APPLICATION_NAME )==null)
            {
                Object app = createObject( appClassName );
                if (app instanceof WebApplication)
                {
                    ((WebApplication)app).init( new PortletContextWrapper(portletContext) );
                }
                else
                    log.warn("Application object does not implement IWebApplication!");
                // Store it
                portletContext.setAttribute( WebApplication.APPLICATION_NAME, app);
            }    
        } catch(Exception e) {
            log.error("Failed to create web application object of type " + appClassName + " --> " + e.getMessage());
        }
    }
    
    protected void createSessionObject(PortletRequest request, Object applObj)
    {
        try {
            // createSessionObject
            PortletSession portletSession = request.getPortletSession(false);
            if (portletSession==null ||  portletSession.getAttribute( WebSession.SESSION_NAME)==null)
            {
                Object session = createObject(sessionClassName);
                portletSession = request.getPortletSession(true);
                // Call init() if Session Object implements IWebSession 
                if (session instanceof WebSession)
                {
                    ((WebSession)session).init( new PortletSessionWrapper(portletSession), applObj );
                }
                else
                    log.warn("Session object does not implement IWebSession!");
                // Save Session
                portletSession.setAttribute( WebSession.SESSION_NAME , session);
            }
        } catch(Exception e) {
            // Error  
            log.error("Failed to create session [" + sessionClassName + "] msg= " + e.getMessage());
        }    
    }

    protected boolean createRequestObject(PortletRequest request, PortletResponse response, Object sessObj)
    {
        try {
            // createSessionObject
            Object reqObj = request.getAttribute( WebRequest.REQUEST_NAME );
            if (reqObj==null)
            {
                // Create Request
                reqObj = createObject( requestClassName );
                // Store Request in Thread Local variable
                EmpireThreadManager.setCurrentRequest( reqObj );
                // Call init() if Request Object implements IWebRequest 
                if (reqObj instanceof WebRequest)
                {
                	RequestContext req = new PortletRequestWrapper(request);
                	ResponseContext res = new PortletResponseWrapper(response);
                    return ((WebRequest)reqObj).init( req, res, sessObj );
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
