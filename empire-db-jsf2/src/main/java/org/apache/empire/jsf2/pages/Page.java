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
package org.apache.empire.jsf2.pages;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.application.NavigationHandler;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.DBRowSet;
import org.apache.empire.jsf2.app.FacesApplication;
import org.apache.empire.jsf2.app.FacesUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Page implements Serializable
{
    private static final long   serialVersionUID = 1L;

    private static final String SESSION_MESSAGE  = "PAGE_SESSION_MESSAGE";

    private static final String INVALID_ACTION   = "XXXXXXXXXXXX";

    private static final Logger log              = LoggerFactory.getLogger(Page.class);

    private String              action           = null;
    private boolean             initialized      = false;
    private PageDefinition      pageDefinition   = null;
    private List<PageElement>   pageElements     = null;

    protected Page()
    {
        String name = this.getClass().getSimpleName();
        Page.log.info("PageBean {} created.", name);
    }

    public String getPageName()
    {
        return (pageDefinition != null ? pageDefinition.getPageBeanName() : "{" + getClass().getSimpleName() + "}");
    }

    public String getName()
    {
        String className = pageDefinition.getPageBeanClass().getName();
        int lastDot = className.lastIndexOf(".");
        String name = className.substring(lastDot + 1);
        return name;
    }

    public boolean isInitialized()
    {
        return initialized;
    }

    public String getAction()
    {
        return this.action;
    }

    public void setAction(String action)
    {
        if (!initialized)
            Page.log.info("Setting PageBean action {} for bean {}.", action, getPageName());
        else
            Page.log.trace("Re-setting PageBeanAction {} for bean {}.", action, getPageName());
        this.action = action;
    }

    public PageDefinition getPageDefinition()
    {
        return pageDefinition;
    }

    public void setPageDefinition(PageDefinition pageDefinition)
    {
        this.pageDefinition = pageDefinition;
    }

    public PageDefinition getParentPage()
    {
        return pageDefinition.getParent();
    }

    public void preRenderPage(FacesContext context)
    {
        if (this.initialized)
        {
            // PageBean.log.error("PageBean {} is already initialized.", name());
            try
            {
                Page.log.debug("PageBean {} is already initialized. Calling doRefresh().", getPageName());
                doRefresh();
            }
            catch (Throwable e)
            {
                logAndHandleActionException("doRefresh", e);
            }
            return; // already Initialized
        }

        // Check access
        try
        {
            checkPageAccess();
        }
        catch (Throwable e)
        {
            logAndHandleActionException("checkAccess", e);
            // redirected?
            if (context.getResponseComplete())
                return;
            // Oops, not redirected yet?
            redirectTo(getParentOutcome(true));
            return;
        }

        // Initialize
        this.initialized = true;

        // Execute Action
        if (this.action != null)
        {
            if (this.action.equals(Page.INVALID_ACTION))
            {
                Page.log.error("Action probably executed twice. Ignoring action.");
                return;
            }
            try
            {
                Page.log.debug("Executing action {} on {}.", String.valueOf(this.action), getPageName());
                Method method = getClass().getMethod(this.action);
                Object result = method.invoke(this);
                if (result != null)
                {
                    String outcome = result.toString();
                    // Retrieve the NavigationHandler instance..
                    NavigationHandler navHandler = context.getApplication().getNavigationHandler();
                    // Invoke nav handling..
                    navHandler.handleNavigation(context, action, outcome);
                    // Trigger a switch to Render Response if needed
                    context.renderResponse();
                    return;
                }
                restoreSessionMessage();
            }
            catch (NoSuchMethodException nsme)
            {
                logAndHandleActionException(action, nsme);
            }
            catch (Exception e)
            {
                logAndHandleActionException(action, e.getCause());
            }
            finally
            {
                // Clear action
                this.action = Page.INVALID_ACTION;
            }
        }
        else
        { // call default Action
            try
            {
                Page.log.debug("Initializing PageBean {}. Calling doInit()", getPageName());
                doInit();
                restoreSessionMessage();
            }
            catch (Throwable e)
            {
                logAndHandleActionException("doInit", e);
            }
        }
    }
    
    protected void checkPageAccess()
    {
        /* Throw exception if User has no Access */
    }

    private void restoreSessionMessage()
    {
        // Restore Session Error Message
        ExternalContext ec = FacesContext.getCurrentInstance().getExternalContext();
        Map<String, Object> map = ec.getSessionMap();
        if (map.containsKey(SESSION_MESSAGE))
        {
            FacesMessage errorMsg = (FacesMessage) map.get(SESSION_MESSAGE);
            FacesContext.getCurrentInstance().addMessage(getPageName(), errorMsg);
            map.remove(SESSION_MESSAGE);
        }
    }

    private void logAndHandleActionException(String action, Throwable e)
    {
        String msg = "Failed to perform action " + action + " on " + getPageName();
        // Message
        Page.log.error(msg, e);
        if (!handleActionError(action, e))
            throw new RuntimeException(msg, e);
    }

    protected void setSessionMessage(FacesMessage facesMsg)
    {
        // Set Session Message
        ExternalContext ec = FacesContext.getCurrentInstance().getExternalContext();
        ec.getSessionMap().put(SESSION_MESSAGE, facesMsg);
    }

    protected boolean handleActionError(String action, Throwable e)
    {
        // Set Faces Message
        String msg = e.getLocalizedMessage();
        String detail = extractErrorMessageDetail(action, e, 1);
        log.error(msg + "\r\n" + detail);
        FacesMessage facesMsg = new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, detail);
        setSessionMessage(facesMsg);
        // Return to parent page
        PageDefinition parentPage = getParentPage();
        if (parentPage == null)
            return false;
        // redirect
        redirectTo(parentPage.getRedirect());
        return true;
    }
    
    protected String extractErrorMessageDetail(String action, Throwable e, int stackTraceElements)
    {
        StringBuilder b = new StringBuilder();
        b.append("Error performing action '");
        b.append(action);
        b.append("' on page ");
        b.append(getPageName());
        b.append(": ");
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

    protected void redirectTo(PageOutcome outcome)
    {
        log.error("Redirecting from page {} to page {}.", getPageName(), outcome.toString());
        // Return to Parent
        FacesContext context = FacesContext.getCurrentInstance();
        // Retrieve the NavigationHandler instance..
        NavigationHandler navHandler = context.getApplication().getNavigationHandler();
        // Invoke nav handling..
        navHandler.handleNavigation(context, action, outcome.toString());
        // Trigger a switch to Render Response if needed
        context.renderResponse();
    }

    /**
     * adds a page element to this page
     * DO NOT CALL yourself, this method is called from the PageElement constructor!
     * 
     * @param element
     */
    protected void registerPageElement(PageElement element)
    {
        if (pageElements == null)
            pageElements = new ArrayList<PageElement>(1);
        // register now
        if (pageElements.contains(element) == false)
            pageElements.add(element);
        else
            log.warn("PageElement {} was registered twice!", element.getPropertyName());
    }

    /**
     * Helper methods for parent outcome
     * 
     * @param action
     * @param redirect
     * @return the parent outcome string
     */
    protected PageOutcome getParentOutcome(String action, boolean redirect)
    {
        PageDefinition parentPage = getParentPage();
        if (parentPage == null)
            throw new RuntimeException("No Parent Page defined for " + getPageName());
        if (redirect)
            return parentPage.getRedirect(action);
        else
            return parentPage.getOutcome(action);
    }

    protected PageOutcome getParentOutcome(boolean redirect)
    {
        return getParentOutcome(null, redirect);
    }

    /**
     * return a connection for a particular database
     * @param db the database for which to obtain a connection
     * @return the connection for the given database
     */
    public Connection getConnection(DBDatabase db)
    {       
        FacesApplication app = FacesUtils.getFacesApplication();
        return app.getConnectionForRequest(FacesUtils.getContext(), db);
    }

    public Object[] getKeyFromParam(DBRowSet rowset, String idParam)
    {
        FacesContext fc = FacesUtils.getContext();
        return FacesUtils.getParameterMap(fc).get(rowset, idParam);
    }
    
    public String getIdParamForKey(DBRowSet rowset, Object[] key)
    {
        FacesContext fc = FacesUtils.getContext();
        return FacesUtils.getParameterMap(fc).put(rowset, key);
    }

    public void addJavascriptCall(String function)
    {
        if (!function.endsWith(";"))
        { // Add a semicolon (important!)
            function += ";";
        }
        // Add Call
        FacesContext fc = FacesUtils.getContext();
        FacesApplication app = FacesUtils.getFacesApplication();
        app.addJavascriptCall(fc, function);
    }
    
    /* Default Init Method */
    public void doInit()
    {
        if (pageElements != null)
        { // Init Page Elements
            for (PageElement pe : pageElements)
                pe.onInitPage();
        }
    }

    public void doRefresh()
    {
        if (pageElements != null)
        { // Init Page Elements
            for (PageElement pe : pageElements)
                pe.onRefreshPage();
        }
    }
}
