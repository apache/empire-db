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
package org.apache.empire.jsf2.app;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import javax.faces.application.Application;
import javax.faces.application.FacesMessage;
import javax.faces.application.FacesMessage.Severity;
import javax.faces.component.NamingContainer;
import javax.faces.component.UIComponent;
import javax.faces.component.UIViewRoot;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.ExceptionQueuedEvent;
import javax.faces.event.ExceptionQueuedEventContext;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;

import org.apache.empire.commons.StringUtils;
import org.apache.empire.data.DataType;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.context.DBRollbackManager;
import org.apache.empire.db.context.DBRollbackManager.ReleaseAction;
import org.apache.empire.exceptions.EmpireException;
import org.apache.empire.exceptions.InternalException;
import org.apache.empire.exceptions.InvalidArgumentException;
import org.apache.empire.exceptions.InvalidOperationException;
import org.apache.empire.exceptions.NotSupportedException;
import org.apache.empire.exceptions.UnexpectedReturnValueException;
import org.apache.empire.jsf2.controls.TextAreaInputControl;
import org.apache.empire.jsf2.controls.TextInputControl;
import org.apache.empire.jsf2.impl.FacesImplementation;
import org.apache.empire.jsf2.impl.ResourceTextResolver;
import org.apache.empire.jsf2.pages.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class WebApplication
{
    private static final Logger log                   = LoggerFactory.getLogger(WebApplication.class);
    
    private static final String REQUEST_CONNECTION_MAP = "requestConnectionMap";

    private static final String CONN_ROLLBACK_MANAGER = "connRollbackManager";
    
    public static String        APPLICATION_BEAN_NAME = "webApplication";

    protected TextResolver[]    textResolvers         = null;

    private String              webRoot               = null;
    
    private FacesImplementation facesImpl			  = null;
    
    private static WebApplication appInstance         = null;
    
    /**
     * Returns the one and only instance of the WebApplication (Singleton)
     * @param <T> the application type
     * @return the WebApplication instance
     */
    @SuppressWarnings("unchecked")
    public static <T extends WebApplication> T getInstance()
    {
        if (appInstance==null)
            log.warn("No WebApplication instance available. Please add a PostConstructApplicationEvent using WebAppStartupListener in your faces-config.xml to create the WebApplication object.");
        // return instance
        return (T)appInstance;
    }

    protected abstract void init(ServletContext servletContext);
    
    protected abstract DataSource getAppDataSource(DBDatabase db);

    protected WebApplication()
    {   // subscribe
        log.info("WebApplication {} created", getClass().getName());
        // Must be a singleton
        if (appInstance!=null) {
            throw new RuntimeException("An attempt was made to create second instance of WebApplication. WebApplication must be a Singleton!");
        }
        // set Instance
        appInstance = this;
    }

    /**
     * Init the Application
     * @param facesImpl the faces implementation
     * @param startupContext the startup context
     */
    public final void init(FacesImplementation facesImpl, FacesContext startupContext)
    {
        // Only call once!
        if (this.facesImpl!=null || this.webRoot!=null) 
        {   // already initialized
            log.warn("WARNING: WebApplication has already been initialized! Continuing without init...");
            return;
        }
        // set imppl
        this.facesImpl = facesImpl;
        // webRoot
        ServletContext servletContext = (ServletContext) startupContext.getExternalContext().getContext();
        webRoot = servletContext.getContextPath();
        servletContext.setAttribute("webRoot", webRoot);
        servletContext.setAttribute("app", this);
        // Init
        init(servletContext);
        // post init
        if (this.textResolvers==null)
        {   // text resolvers
            log.info("*** initTextResolvers() ***");
            initTextResolvers(startupContext.getApplication());
        }
        // Log info
        log.info("*** WebApplication initialization complete ***");
        log.info("JSF-Implementation is '{}'", facesImpl.getClass().getName());
        log.info("WebRoot is '{}'", webRoot);
    }

    public void destroy()
    {
    	// Override if needed
    }
    
    /* Context handling */
    
    /**
     * handle request cleanup
     * @param ctx the faces context
     */
    public void onRequestComplete(final FacesContext ctx)
    {
        releaseAllConnections(ctx);
    }

    /**
     * handle view not found
     * @param fc the faces context
     * @param req the request
     */
    public void onViewNotFound(final FacesContext fc, final HttpServletRequest req)
    {   // View not Found Error
        log.warn("No view found for request to '{}'. Use FacesUtils.redirectDirectly() to redirect to valid view.", req.getRequestURI());
        redirectDirectly(fc, StringUtils.EMPTY);
    }

    /**
     * handle view change
     * @param fc the faces context
     * @param viewId the view id
     */
    public void onChangeView(final FacesContext fc, String viewId)
    {   // allow custom view change logic

        // clear page resources
        Map<String, Object> sm = FacesUtils.getSessionMap(fc);
        if (sm!=null)
            sm.remove(FacesUtils.PAGE_RESOURCE_MAP_ATTRIBUTE);
    }

    /**
     * adds a Javascript call to the request 
     * @param fc the faces context
     * @param function the javascript command
     */
    public void addJavascriptCall(final FacesContext fc, String function)
    {
        throw new NotSupportedException(this, "addJavascriptCall");
    }

    /**
     * return the interface for Implementation specific features 
     * that are specific for Mojarra or MyFaces
     * @return the faces implementation
     */
    public FacesImplementation getFacesImplementation() 
    {
		return facesImpl;
	}
    
    /**
     * returns the web context path as returned from ServletContext.getContextPath()
     * @return the web root
     */
    public String getWebRoot()
    {
        return webRoot;
    }

    /**
     * returns the active locale for a given FacesContext
     * @param ctx the faces context
     * @return the context locale
     */
    public Locale getContextLocale(final FacesContext ctx)
    {
        UIViewRoot root;
        // Start out with the default locale
        Locale locale;
        Locale defaultLocale = Locale.getDefault();
        locale = defaultLocale;
        // See if this FacesContext has a ViewRoot
        if (null != (root = ctx.getViewRoot()))
        {
            // If so, ask it for its Locale
            if (null == (locale = root.getLocale()))
            {
                // If the ViewRoot has no Locale, fall back to the default.
                locale = defaultLocale;
            }
        }
        return locale;
    }

    public TextResolver getTextResolver(Locale locale)
    {
        // No text Resolvers provided
        if (textResolvers == null || textResolvers.length == 0)
        {
            throw new NotSupportedException(this, "getTextResolver");
        }
        // Lookup resolver for locale
        for (int i = 0; i < textResolvers.length; i++)
            if (locale.equals(textResolvers[i].getLocale()))
                return textResolvers[i];
        // locale not found: return default
        return textResolvers[0];
    }

    public TextResolver getTextResolver(FacesContext ctx)
    {
        return getTextResolver(getContextLocale(ctx));
    }

    /**
     * Returns a FacesMessage 
     * @param ctx the FacesContext
     * @param severity the message severity
     * @param msg the message or message key
     * @param params the message params
     * @return the FacesMessage or null to ignore
     */
    public FacesMessage getFacesMessage(FacesContext ctx, Severity severity, String msg, Object... params)
    {
        TextResolver resolver = getTextResolver(getContextLocale(ctx));
        msg = resolver.resolveText(msg);
        if (params.length>0)
        {   // translate params
            for (int i=0; i<params.length; i++)
                if (params[i] instanceof String)
                    params[i] = resolver.resolveText((String)params[i]);
                else if ((params[i] instanceof Integer) || (params[i] instanceof Long))
                    params[i] = String.valueOf(params[i]); // avoid group separator
            // format
            msg = MessageFormat.format(msg, params);
        }
        return new FacesMessage(severity, msg, null);
    }
    
    /**
     * Returns a FacesMessage for an Exception 
     * @param ctx the FacesContext
     * @param errorContext the error context (optional)
     * @param t the exception
     * @return the FacesMessage or null to ignore
     */
    public FacesMessage getFacesErrorMessage(FacesContext ctx, String errorContext, Throwable t)
    {
        // Wrap exception if necessary
        EmpireException e = (t instanceof EmpireException) ? ((EmpireException)t) : new InternalException(t); 
        // Get Message
        TextResolver tr = getTextResolver(ctx);
        String msg = tr.getExceptionMessage(e);
        String msgDetail = extractErrorMessageDetail(errorContext, t, 3);
        // create Faces Message
        return new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, msgDetail);
    }
    
    protected String extractErrorMessageDetail(String errorContext, Throwable e, int stackTraceElements)
    {
        StringBuilder b = new StringBuilder();
        if (errorContext!=null)
        {   // Append context String
            b.append(errorContext);
            b.append(": ");
        }
        b.append(e.toString());
        b.append("\r\nat:");
        StackTraceElement[] stack = e.getStackTrace();
        int len = (stack.length>stackTraceElements) ? stackTraceElements : stack.length; 
        for (int i=0; i<len; i++)
        {
            b.append(stack[i].toString());
            b.append("\r\n");
        }
        return b.toString();
    }

    /**
     * checks if the current context contains an error
     * @param fc the FacesContext
     * @return true if the context has an error set or false otherwise
     */
    public boolean hasError(final FacesContext fc)
    {
        Iterator<FacesMessage> msgIterator = fc.getMessages();
        if (msgIterator != null)
        { // Check Messages
            while (msgIterator.hasNext())
            { // Check Severity
                Severity fms = msgIterator.next().getSeverity();
                if (fms == FacesMessage.SEVERITY_ERROR || fms == FacesMessage.SEVERITY_FATAL)
                    return true;
            }
        }
        return false;
    }
    
    /**
     * Redirects to another page or url
     * @param fc the FacesContext
     * @param url the target url
     */
    public void redirectDirectly(final FacesContext fc, String url)
    {
        try
        {   // check params
            if (fc==null)
                throw new InvalidArgumentException("fc", fc);
            if (fc.getResponseComplete())
                throw new InvalidOperationException("Unable to redirect. Response is already complete!");
            if (url==null)
                url = StringUtils.EMPTY; // redirect to root
            // Prepend Context-Path
            ExternalContext ec = fc.getExternalContext(); 
            String ctxPath = ec.getRequestContextPath();
            if (url.indexOf("://")>0)
            {   // Should not contain the context-path
                if (url.startsWith("http") && url.indexOf(ctxPath)>0)
                    log.warn("Redirect url \"{}\" contains protokoll and context-path. Please remove.", url);
                else
                    log.info("Redirecting to absolute url {}", url);
            }
            else if (!url.startsWith(ctxPath))
            {   // assemble url
                String sep = (url.length()>0 && url.charAt(0)!='/' ? "/" : null);
                url = StringUtils.concat(ctxPath, sep, url);
                // relative
                log.debug("Redirecting to relative url {}", url);
            }
            else
                log.warn("Redirect url \"{}\" already contains context-path. Please remove.", url);
            // redirectDirectly
            ec.redirect(url);
            fc.responseComplete();
        }
        catch (IOException e)
        {   // What can we do now?
            log.error("Failed to redirect to {}", url, e);
        }
    }
    
    /**
     * Handles an exeption, that could not be handled on the page level
     * The application should redirect to the error page.
     * @param context the faces context
     * @param page the page from which the exception originated
     * @param the Exception
     */
    public void handleException(FacesContext context, Page source, Throwable e)
    {
        // log source
        String origin = (source!=null ? source.getPageDefinition().getPageBeanName() : "[Unknown]");
        log.error("Fatal error of type {} from \"{}\": {}: {}", e.getClass().getName(), origin, e.getMessage());

        // For page errors, give the ExceptionHandler a chance to handle
        if (source!=null)
        {
            // Queue event 
            ExceptionQueuedEventContext event = new ExceptionQueuedEventContext(context, e, null, context.getCurrentPhaseId());
            event.getAttributes().put (ExceptionQueuedEventContext.IN_BEFORE_PHASE_KEY, Boolean.TRUE);
            context.getApplication().publishEvent (context, ExceptionQueuedEvent.class, event);
            
            // Handle Exception
            context.getExceptionHandler().handle(); 
            if (context.getResponseComplete())
                return;
        }
            
        // If all has failed, redirect to ContextPath (root)
        redirectDirectly(context, StringUtils.EMPTY);
    }
    
    /**
     * returns true if a form input element has been partially submitted
     * @param fc the Faces Context
     * @return the componentId or null if no partial submit was been performed
     */
    public boolean isPartialSubmit(final FacesContext fc)
    {
        // Override for your JSF component Framework. e.g. for IceFaces
        // Map<String,String> parameterMap = fc.getExternalContext().getRequestParameterMap();
        // return ObjectUtils.getBoolean(parameterMap.get("ice.submit.partial"));
        return false;
    }

    /**
     * returns the componentId for which a partial submit has been performed.
     * @param fc the Faces Context
     * @return the componentId or null if no partial submit was been performed
     */
    public String getPartialSubmitComponentId(final FacesContext fc)
    {
        Map<String,String> parameterMap = fc.getExternalContext().getRequestParameterMap();
        return parameterMap.get("javax.faces.source");  // or javax.faces.partial.execute ?
    }

    /**
     * finds the component with the given id that is located in the same NamingContainer as a given component 
     * @param fc the FacesContext
     * @param componentId the component id
     * @param nearComponent a component within the same naming container from which to start the search (optional)
     * @return the component or null if no component was found
     */
    public UIComponent findComponent(FacesContext fc, String componentId, UIComponent nearComponent)
    {
        if (StringUtils.isEmpty(componentId))
            throw new InvalidArgumentException("componentId", componentId);
        // Begin search near given component (if any)
        UIComponent component = null;
        if (nearComponent != null)
        {   // Search below the nearest naming container  
            component = nearComponent.findComponent(componentId);
            if (component == null)
            {   // Recurse upwards
                UIComponent nextParent = nearComponent;
                while (true)
                {
                    nextParent = nextParent.getParent();
                    // search NamingContainers only
                    while (nextParent != null && !(nextParent instanceof NamingContainer))
                    {
                        nextParent = nextParent.getParent();
                    }
                    if (nextParent == null)
                    {
                        break;
                    }
                    else
                    {
                        component = nextParent.findComponent(componentId);
                    }
                    if (component != null)
                    {
                        break;
                    }
                }
            }
        }
        // Not found. Search the entire tree 
        if (component == null)
            component = findChildComponent(fc.getViewRoot(), componentId);
        // done
        return component;
    }

    /**
     * finds a child component with the given id that is located below the given parent component 
     * @param parent the parent
     * @param componentId the component id
     * @return the component or null if no component was found
     */
    public static UIComponent findChildComponent(UIComponent parent, String componentId)
    {
        UIComponent component = null;
        if (parent.getChildCount() == 0)
            return null;
        Iterator<UIComponent> children = parent.getChildren().iterator();
        while (children.hasNext())
        {
            UIComponent nextChild = children.next();
            if (nextChild instanceof NamingContainer)
            {
                component = nextChild.findComponent(componentId);
            }
            if (component == null)
            {
                component = findChildComponent(nextChild, componentId);
            }
            if (component != null)
            {
                break;
            }
        }
        return component;
    }

    /**
     * returns the default input control type for a given data Type
     * @see org.apache.empire.jsf2.controls.InputControlManager
     * @param dataType
     * @return an Input Cnotrol type
     */
    public String getDefaultControlType(DataType dataType)
    {
        switch (dataType)
        {
            case CLOB:
                return TextAreaInputControl.NAME;
            default:
                return TextInputControl.NAME;
        }
    }

    /* Message handling */

    protected void initTextResolvers(Application app)
    {
        int count = 0;
        Iterator<Locale> locales = app.getSupportedLocales();
        for (count = 0; locales.hasNext(); count++)
        {
            locales.next();
        }

        // get message bundles
        String messageBundle = app.getMessageBundle();
        textResolvers = new TextResolver[count];
        locales = app.getSupportedLocales();
        for (int i = 0; locales.hasNext(); i++)
        {
            Locale locale = locales.next();
            textResolvers[i] = new ResourceTextResolver(ResourceBundle.getBundle(messageBundle, locale));
            log.info("added TextResolver for {} bundle='{}'", locale.getLanguage(), messageBundle);
        }
    }

    /**
     * Obtains a JDBC-Connection from the connection pool
     * @param db the database for which to obtain a connection
     * @return the connection
     */
    protected Connection getConnection(DBDatabase db)
    {
        // Get From Pool
        try
        { // Obtain a connection
            Connection conn = getAppDataSource(db).getConnection();
            conn.setAutoCommit(false);
            log.trace("Connection {} obtained from pool", conn.hashCode());
            return conn;
        }
        catch (SQLException e)
        {
            log.error("Failed to get connection from pool.", e);
            throw new InternalException(e);
        }
    }

    /**
     * Releases a JDBC-Connection from the connection pool
     * @param conn the connection to release
     * @param commit flag whether to commit changes
     * @param dbrm the rollback manager
     */
    protected void releaseConnection(Connection conn, boolean commit, DBRollbackManager dbrm)
    {
        try
        {   // check
            if (conn == null)
                return;
            // release connection
            log.trace("releasing Connection {}", conn.hashCode());
            // Commit or rollback connection depending on the exit code
            if (commit)
            {   // success: commit all changes
                if (dbrm!=null)
                    dbrm.releaseConnection(conn, ReleaseAction.Discard);  // before commit
                conn.commit();
                log.debug("REQUEST commited.");
            }
            else
            {   // failure: rollback all changes
                conn.rollback();
                if (dbrm!=null)
                    dbrm.releaseConnection(conn, ReleaseAction.Rollback); // after rollback
                log.debug("REQUEST rolled back.");
            }
            // Release Connection
            conn.close();
            // done
            if (log.isDebugEnabled())
                log.debug("REQUEST returned connection to pool.");
        }
        catch (SQLException e)
        {
            log.error("Error releasing connection", e);
            e.printStackTrace();
        }
    }

    /**
     * Obtains a DBRollbackManager for the current request
     * @param fc the FacesContext
     * @param create if true a DBRollbackManager will be created if not already present
     * @return the rollback manager
     */
    public DBRollbackManager getRollbackManagerForRequest(FacesContext fc, boolean create)
    {
        DBRollbackManager dbrm = (DBRollbackManager)FacesUtils.getRequestAttribute(fc, CONN_ROLLBACK_MANAGER);
        if (dbrm==null && create)
        {   dbrm = new DBRollbackManager(1, 8);
            FacesUtils.setRequestAttribute(fc, CONN_ROLLBACK_MANAGER, dbrm);
        }
        return dbrm;
    }
    
    /**
     * Obtains a JDBC-Connection for the current request
     * @param fc the FacesContext
     * @param db the DBDatabase for which to obtain a connection
     * @param create if true a Connection will be created if not already present
     * @return the connection
     */
    public Connection getConnectionForRequest(FacesContext fc, DBDatabase db, boolean create)
    {
        if (fc == null)
            throw new InvalidArgumentException("FacesContext", fc);
        if (db == null)
            throw new InvalidArgumentException("DBDatabase", db);
        // Get the ConnectionContextInfo map
        @SuppressWarnings("unchecked")
        Map<DBDatabase, Connection> connMap = (Map<DBDatabase, Connection>) FacesUtils.getRequestAttribute(fc, REQUEST_CONNECTION_MAP);
        if (connMap== null && create)
        {   connMap = new HashMap<DBDatabase, Connection>(1);
            FacesUtils.setRequestAttribute(fc, REQUEST_CONNECTION_MAP, connMap);
        }
        else if (connMap==null)
        {   // Nothing to do
            return null; 
        }
        Connection conn = connMap.get(db); 
        if (conn==null && create)
        {   // Get Pooled Connection
            conn = getConnection(db);
            if (conn== null)
                throw new UnexpectedReturnValueException(this, "getConnection"); 
            // Add to map
            connMap.put(db, conn);
        }
        // done
        return conn;
    }

    /**
     * Releases all connections attached to the current request
     * @param fc the FacesContext
     * @param commit when true changes are committed otherwise they are rolled back
     */
    public void releaseAllConnections(final FacesContext fc, boolean commit)
    {
        @SuppressWarnings("unchecked")
        Map<DBDatabase, Connection> connMap = (Map<DBDatabase, Connection>) FacesUtils.getRequestAttribute(fc, REQUEST_CONNECTION_MAP);
        if (connMap == null)
            return; // Nothing to do
        // Walk the connection map
        DBRollbackManager dbrm = getRollbackManagerForRequest(fc, false);
        for (Connection conn : connMap.values())
        {
            releaseConnection(conn, commit, dbrm);
        }
        // remove from request map
        FacesUtils.setRequestAttribute(fc, REQUEST_CONNECTION_MAP, null);
    }

    /**
     * Releases all connections attached to the current request
     * If an error is detected in the faces message list, a rollback will automatically be performed insteamd of a commmit
     * @param fc the FacesContext
     */
    public void releaseAllConnections(final FacesContext fc)
    {
        releaseAllConnections(fc, !hasError(fc));
    }

    /**
     * Releases the connection associated with a database from the request
     * @param fc the FacesContext
     * @param db the DBDatabase for which to release the connection
     * @param commit when true changes are committed otherwise they are rolled back
     */
    public void releaseConnection(final FacesContext fc, DBDatabase db, boolean commit)
    {
        @SuppressWarnings("unchecked")
        Map<DBDatabase, Connection> connMap = (Map<DBDatabase, Connection>) FacesUtils.getRequestAttribute(fc, REQUEST_CONNECTION_MAP);
        if (connMap == null || !connMap.containsKey(db))
            return; // Nothing to do;
        // Get RollbackManager   
        DBRollbackManager dbrm = getRollbackManagerForRequest(fc, false);
        // Release Connection   
        Connection conn = connMap.get(db);
        releaseConnection(conn, commit, dbrm);
        // Remove from map
        connMap.remove(db);
    }

    /**
     * Releases the connection associated with a database from the request
     * If an error is detected in the faces message list, a rollback will automatically be performed insteamd of a commmit
     * @param fc the FacesContext
     * @param db the DBDatabase for which to release the connection
     */
    public void releaseConnection(final FacesContext fc, DBDatabase db)
    {
        releaseConnection(fc, db, !hasError(fc));
    }
    
}
