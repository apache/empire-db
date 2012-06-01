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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import javax.faces.application.FacesMessage;
import javax.faces.application.FacesMessage.Severity;
import javax.faces.component.NamingContainer;
import javax.faces.component.UIComponent;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;
import javax.sql.DataSource;

import org.apache.empire.commons.StringUtils;
import org.apache.empire.data.DataType;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.exceptions.InternalException;
import org.apache.empire.exceptions.InvalidArgumentException;
import org.apache.empire.exceptions.NotSupportedException;
import org.apache.empire.jsf2.controls.TextAreaInputControl;
import org.apache.empire.jsf2.controls.TextInputControl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.faces.application.ApplicationImpl;

public abstract class FacesApplication extends ApplicationImpl
{
    private static final Logger log                   = LoggerFactory.getLogger(FacesApplication.class);

    private static final String CONNECTION_ATTRIBUTE  = "dbConnections";

    public static String        APPLICATION_ATTRIBUTE = "app";

    protected TextResolver[]    textResolvers         = null;

    private String              webRoot               = null;

    protected FacesApplication(AppStartupListener startupListener)
    { // subscribe
        subscribeToEvent(javax.faces.event.PostConstructApplicationEvent.class, startupListener);
    }

    protected FacesApplication()
    { // subscribe
        this(new AppStartupListener());
    }

    protected abstract DataSource getAppDataSource(DBDatabase db);

    protected abstract void init(ServletContext fc);

    protected void initComplete(ServletContext servletContext)
    {
        // Get Web Root
        webRoot = servletContext.getContextPath();

        // Check Text resolvers
        if (textResolvers == null)
            initTextResolvers();

        // done
        log.info("FacesApplication initialization complete");
    }

    /* Context handling */

    public void onChangeView(final FacesContext fc, String viewId)
    {
        // allow custom view change logic
    }

    public void addJavascriptCall(final FacesContext fc, String function)
    {
        throw new NotSupportedException(this, "addJavascriptCall");
    }

    /**
     * returns the web context path as returned from ServletContext.getContextPath()
     */
    public String getWebRoot()
    {
        return webRoot;
    }

    /**
     * returns the active locale for a given FacesContext
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

    /**
     * checks if the current context contains an error
     * 
     * @param fc
     *            the FacesContext
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
     * finds a component from with a given id from a given start component
     * 
     * @param fc
     *            the FacesContext
     * @param componentId
     * @param nearComponent
     *            a component within the same naming container
     * @return
     */
    public UIComponent findComponent(FacesContext fc, String componentId, UIComponent nearComponent)
    {
        if (StringUtils.isEmpty(componentId))
            throw new InvalidArgumentException("forComponentId", componentId);
        // Search for compoent
        UIComponent forComponent = null;
        if (nearComponent != null)
        {
            // Look for the 'for' component in the nearest parental naming container 
            // of the UIComponent (there's actually a bit more to this search - see
            // the docs for the findComponent method
            forComponent = nearComponent.findComponent(componentId);
            // Since the nearest naming container may be nested, search the 
            // next-to-nearest parental naming container in a recursive fashion, 
            // until we get to the view root
            if (forComponent == null)
            {
                UIComponent nextParent = nearComponent;
                while (true)
                {
                    nextParent = nextParent.getParent();
                    // avoid extra searching by going up to the next NamingContainer
                    // (see the docs for findComponent for an information that will
                    // justify this approach)
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
                        forComponent = nextParent.findComponent(componentId);
                    }
                    if (forComponent != null)
                    {
                        break;
                    }
                }
            }
        }
        // There is one other situation to cover: if the 'for' component 
        // is not situated inside a NamingContainer then the algorithm above
        // will not have found it. We need, in this case, to search for the 
        // component from the view root downwards
        if (forComponent == null)
        {
            forComponent = searchDownwardsForChildComponentWithId(fc.getViewRoot(), componentId);
        }
        return forComponent;
    }

    private static UIComponent searchDownwardsForChildComponentWithId(UIComponent parent, String searchChildId)
    {
        UIComponent foundChild = null;
        if (parent.getChildCount() == 0)
            return foundChild;
        Iterator<UIComponent> children = parent.getChildren().iterator();
        while (children.hasNext())
        {
            UIComponent nextChild = children.next();
            if (nextChild instanceof NamingContainer)
            {
                foundChild = nextChild.findComponent(searchChildId);
            }
            if (foundChild == null)
            {
                searchDownwardsForChildComponentWithId(nextChild, searchChildId);
            }
            if (foundChild != null)
            {
                break;
            }
        }
        return foundChild;
    }

    /**
     * returns the default control type for a given data Type
     * 
     * @param dataType
     * @return
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

    protected void initTextResolvers()
    {
        int count = 0;
        Iterator<Locale> locales = getSupportedLocales();
        for (count = 0; locales.hasNext(); count++)
        {
            locales.next();
        }

        // get message bundles
        String messageBundle = this.getMessageBundle();
        textResolvers = new TextResolver[count];
        locales = getSupportedLocales();
        for (int i = 0; locales.hasNext(); i++)
        {
            Locale locale = locales.next();
            textResolvers[i] = new TextResolver(ResourceBundle.getBundle(messageBundle, locale));
        }
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
     * @see javax.faces.application.Application#getResourceBundle(javax.faces.context.FacesContext,
     *      String)
     */
    @Override
    public ResourceBundle getResourceBundle(FacesContext fc, String var)
    {
        if (var.equals("msg"))
        {
            TextResolver resolver = getTextResolver(fc);
            return resolver.getResourceBundle();
        }
        return null;
    }

    /**
     * returns a connection from the connection pool
     * 
     * @return
     */
    protected Connection getConnection(DBDatabase db)
    {
        // Get From Pool
        try
        { // Obtain a connection
            Connection conn = getAppDataSource(db).getConnection();
            conn.setAutoCommit(false);
            return conn;
        }
        catch (SQLException e)
        {
            log.error("Failed to get connection from pool.", e);
            throw new InternalException(e);
        }
    }

    /**
     * releases a connection from the connection pool
     */
    protected void releaseConnection(DBDatabase db, Connection conn, boolean commit)
    {
        try
        { // release connection
            if (conn == null)
            {
                return;
            }
            // Commit or rollback connection depending on the exit code
            if (commit)
            { // success: commit all changes
                db.commit(conn);
                log.debug("REQUEST {}: commited.");
            }
            else
            { // failure: rollback all changes
                db.rollback(conn);
                log.debug("REQUEST {}: rolled back.");
            }
            // Release Connection
            conn.close();
            // done
            if (log.isDebugEnabled())
                log.debug("REQUEST {}: returned connection to pool.");
        }
        catch (SQLException e)
        {
            log.error("Error releasing connection", e);
            e.printStackTrace();
        }
    }

    /**
     * Returns a connection for the current Request
     * Method should only be called by BeforeRestoreViewListener once per request!
     */
    public Connection getConnectionForRequest(FacesContext fc, DBDatabase db)
    {
        if (fc == null)
            throw new InvalidArgumentException("FacesContext", fc);
        if (db == null)
            throw new InvalidArgumentException("DBDatabase", db);
        // Get Conneciton map
        @SuppressWarnings("unchecked")
        Map<DBDatabase, Connection> connMap = (Map<DBDatabase, Connection>) FacesUtils.getRequestAttribute(fc, CONNECTION_ATTRIBUTE);
        if (connMap != null && connMap.containsKey(db))
            return connMap.get(db);
        // Pooled Connection
        Connection conn = getConnection(db);
        if (conn == null)
            return null;
        // Add to map
        if (connMap == null)
        {
            connMap = new HashMap<DBDatabase, Connection>();
            FacesUtils.setRequestAttribute(fc, CONNECTION_ATTRIBUTE, connMap);
        }
        connMap.put(db, conn);
        return conn;
    }

    /**
     * Releases the current request connection
     * 
     * @param fc
     * @param commit
     */
    public void releaseAllConnections(final FacesContext fc, boolean commit)
    {
        @SuppressWarnings("unchecked")
        Map<DBDatabase, Connection> connMap = (Map<DBDatabase, Connection>) FacesUtils.getRequestAttribute(fc, CONNECTION_ATTRIBUTE);
        if (connMap != null)
        { // Walk the connection map
            for (Map.Entry<DBDatabase, Connection> e : connMap.entrySet())
            {
                releaseConnection(e.getKey(), e.getValue(), commit);
            }
            // remove from request map
            FacesUtils.setRequestAttribute(fc, CONNECTION_ATTRIBUTE, null);
        }
    }

    public void releaseAllConnections(final FacesContext fc)
    {
        releaseAllConnections(fc, !hasError(fc));
    }

    public void releaseConnection(final FacesContext fc, DBDatabase db, boolean commit)
    {
        @SuppressWarnings("unchecked")
        Map<DBDatabase, Connection> connMap = (Map<DBDatabase, Connection>) FacesUtils.getRequestAttribute(fc, CONNECTION_ATTRIBUTE);
        if (connMap != null && connMap.containsKey(db))
        { // Walk the connection map
            releaseConnection(db, connMap.get(db), commit);
            connMap.remove(db);
            if (connMap.size() == 0)
                FacesUtils.setRequestAttribute(fc, CONNECTION_ATTRIBUTE, null);
        }
    }

    public void releaseConnection(final FacesContext fc, DBDatabase db)
    {
        releaseConnection(fc, db, !hasError(fc));
    }

}
