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

import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.application.FacesMessage.Severity;
import javax.faces.application.NavigationHandler;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import org.apache.empire.commons.StringUtils;
import org.apache.empire.db.DBRowSet;
import org.apache.empire.exceptions.EmpireException;
import org.apache.empire.exceptions.InternalException;
import org.apache.empire.exceptions.ItemNotFoundException;
import org.apache.empire.exceptions.InvalidOperationException;
import org.apache.empire.jsf2.app.FacesUtils;
import org.apache.empire.jsf2.app.TextResolver;
import org.apache.empire.jsf2.app.WebApplication;
import org.apache.empire.jsf2.utils.ParameterMap;
import org.apache.empire.jsf2.utils.ParameterObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Page // *Deprecated* implements Serializable
{
    // *Deprecated* private static final long serialVersionUID = 1L;

    public static final String  SESSION_MESSAGE  = "PAGE_SESSION_MESSAGE";

    private static final Logger log              = LoggerFactory.getLogger(Page.class);

    private String              action           = null;
    private boolean             initialized      = false;
    private PageDefinition      pageDefinition   = null;
    private List<PageElement>   pageElements     = null;

    protected Page()
    {
        if (log.isDebugEnabled())
        {   String name = this.getClass().getSimpleName();
            Page.log.debug("PageBean {} created.", name);
        }
    }

    /*
     * Removed with EMPIREDB-421:
     * 
     * public abstract T getDBContext(DBDatabase db);
     */
    
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
        if (this.action==null)
            return null;

        // Generate key
        ParameterMap pm = FacesUtils.getParameterMap(FacesUtils.getContext());
        String actionParam = (pm!=null ? pm.encodeString(action) : action);
        return actionParam;
    }

    public void setAction(String actionParam)
    {
        if (!initialized)
            Page.log.debug("Setting PageBean action {} for bean {}.", action, getPageName());
        else
            Page.log.trace("Re-setting PageBeanAction {} for bean {}.", action, getPageName());
        
        // actionParam
        if (StringUtils.isEmpty(actionParam))
            return;

        // Set action from param
        this.action = PageDefinition.decodeActionParam(actionParam);
        if (this.action==null)
            throw new ItemNotFoundException(actionParam); 
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
            catch (Exception e)
            {
                logAndHandleActionException("doRefresh", e);
            }
            return; // already Initialized
        }

        // Check access
        try
        {
            checkPageAccess();
            // redirected?
            if (context.getResponseComplete())
                return;
        }
        catch (Exception e)
        {
            logAndHandleActionException("checkAccess", e);
            // redirected?
            if (context.getResponseComplete())
                return;
            // Oops, not redirected yet?
            if (getParentPage()!=null)
                navigateTo(getParentOutcome(true)); 
            // Done
            return;
        }

        // Initialize
        this.initialized = true;

        // String value of "null"?
        if (this.action!=null && "null".equals(this.action))
        {   log.warn("Invalid action name 'null' for {}", getClass().getName());
            this.action = null;
        }    
        
        // Execute Action
        if (this.action != null)
        {   try
            {   // Process action
                log.info("Processing action {} on {}.", String.valueOf(action), getPageName());
                Method method = getClass().getMethod(action);
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
                this.action = null; // Page.INVALID_ACTION;
            }
        }
        else
        {   // call default Action
            try
            {
                Page.log.debug("Initializing PageBean {}. Calling doInit()", getPageName());
                doInit();
                restoreSessionMessage();
            }
            catch (Exception e)
            {
                logAndHandleActionException("doInit", e);
            }
        }
    }
    
    public boolean isHasMessages()
    {
        Iterator<FacesMessage> fmi = FacesContext.getCurrentInstance().getMessages();
        return fmi.hasNext();
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
        {   // Not handled. Throw again
            if (e instanceof EmpireException)
                throw ((EmpireException)e);
            else
                throw new InternalException(e);
        }    
    }
    
    protected void setSessionMessage(FacesMessage facesMsg)
    {
        // Set Session Message
        ExternalContext ec = FacesContext.getCurrentInstance().getExternalContext();
        ec.getSessionMap().put(SESSION_MESSAGE, facesMsg);
    }
    
    protected void setSessionError(Throwable e)
    {
        // Set Session Message
        String msg = extractErrorMessage(e);
        String detail = extractErrorMessageDetail(action, e, 1);
        if (log.isDebugEnabled())
            log.debug(msg + "\r\n" + detail, e);
        FacesMessage facesMsg = new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, detail);
        setSessionMessage(facesMsg);
    }

    protected boolean handleActionError(String action, Throwable e)
    {
        // Set Faces Message
        String msg = extractErrorMessage(e);
        String detail = extractErrorMessageDetail(action, e, 1);
        log.error(msg + "\r\n" + detail);
        FacesMessage facesMsg = new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, detail);
        setSessionMessage(facesMsg);
        // Return to parent page
        PageDefinition parentPage = getParentPage();
        if (parentPage == null)
        {   FacesContext.getCurrentInstance().addMessage(getPageName(), facesMsg);
            return false;
        }
        // redirect
        navigateTo(parentPage.getRedirect());
        return true;
    }

    protected void addFacesMessage(Severity severity, String msg, Object... params)
    {
        TextResolver resolver = getTextResolver();
        msg = resolver.resolveText(msg);
        if (params.length>0)
        {   // translate params
            for (int i=0; i<params.length; i++)
                if (params[i] instanceof String)
                    params[i] = resolver.resolveText((String)params[i]);
            // format
            msg = MessageFormat.format(msg, params);
        }
        FacesMessage facesMsg = new FacesMessage(severity, msg, msg);
        FacesContext.getCurrentInstance().addMessage(getPageName(), facesMsg);
    }

    public final void addInfoMessage(String msg, Object... params)
    {
        addFacesMessage(FacesMessage.SEVERITY_INFO, msg, params);
    }

    public final void addWarnMessage(String msg, Object... params)
    {
        addFacesMessage(FacesMessage.SEVERITY_WARN, msg, params);
    }

    public final void addErrorMessage(String msg, Object... params)
    {
        addFacesMessage(FacesMessage.SEVERITY_ERROR, msg, params);
    }

    public void setErrorMessage(Throwable e)
    {
        String msg = extractErrorMessage(e);
        String detail = extractErrorMessageDetail(action, e, 1);
        if (log.isDebugEnabled())
            log.debug(msg + "\r\n" + detail, e);
        FacesMessage facesMsg = new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, detail);
        FacesContext.getCurrentInstance().addMessage(getPageName(), facesMsg);
    }

    protected String extractErrorMessage(Throwable e)
    {   // Wrap Exception
        if (!(e instanceof EmpireException))
            e = new InternalException(e);
        // get message 
        return getTextResolver().getExceptionMessage((Exception)e);
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

    /**
     * navigates to the desired page. Depending on the page outcome provided this is either a forward or a redirect.
     * @param outcome the destination page to navigate to
     */
    protected void navigateTo(PageOutcome outcome)
    {
        if (log.isDebugEnabled())
            log.debug("Redirecting from page {} to page {}.", getPageName(), outcome.toString());
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
            throw new InvalidOperationException("No Parent Page defined for " + getPageName());
        if (redirect)
            return parentPage.getRedirect(action);
        else
            return parentPage.getOutcome(action);
    }

    protected PageOutcome getParentOutcome(boolean redirect)
    {
        return getParentOutcome(null, redirect);
    }

    public <T extends ParameterObject> T getObjectFromParam(Class<T> paramType, String idParam)
    {
        FacesContext fc = FacesUtils.getContext();
        ParameterMap paramMap = FacesUtils.getParameterMap(fc);
        return paramMap.get(paramType, idParam);
    }

    public Object[] getKeyFromParam(DBRowSet rowset, String idParam)
    {
        FacesContext fc = FacesUtils.getContext();
        return FacesUtils.getParameterMap(fc).getKey(rowset, idParam);
    }
    
    public Object[] getKeyFromParam(PageDefinition page, DBRowSet rowset, String idParam)
    {
        FacesContext fc = FacesUtils.getContext();
        return FacesUtils.getParameterMap(fc).getKey(page, rowset, idParam);
    }
    
    public String getIdParamForKey(DBRowSet rowset, Object[] key)
    {
        FacesContext fc = FacesUtils.getContext();
        return FacesUtils.getParameterMap(fc).put(rowset, key);
    }

    public String getIdParamForKey(PageDefinition page, DBRowSet rowset, Object[] key)
    {
        FacesContext fc = FacesUtils.getContext();
        return FacesUtils.getParameterMap(fc).put(page, rowset, key);
    }

    /**
     * Adds a Javascript call to the current request
     * @param function the javascript function to be called
     */
    public void addJavascriptCall(String function)
    {
        // Add Call
        FacesContext fc = FacesUtils.getContext();
        WebApplication app = FacesUtils.getWebApplication();
        app.addJavascriptCall(fc, function);
    }

    /**
     * Adds a Javascript call to the current request
     * @param function the function call template 
     * @param args the call arguments to be replaced in the function template
     */
    public void addJavascriptCall(String function, Object... args)
    {
        // template and arguments?
        for (int i=0; i<args.length; i++)
        {
            String placeholder = "{"+String.valueOf(i)+"}";
            function = StringUtils.replace(function, placeholder, String.valueOf(args[i]));
        }
        // Add Call
        this.addJavascriptCall(function);
    }
    
    /* Page Resources */
    
    /**
     * Adds an object required for resource handling, to the page resource map.
     * <pre>
     * Since resource requests are not attached to a view, they cannot access page properties via expression language like this
     *    #{page.someProperty}
     * Instead, the page should add properties that are required to the "pageResources"-map. This map is held on the session, and cleared when the page changes.
     * In order to access such page resources via expression language use
     *    #{pageResources.someProperty}
     * </pre>   
     * @param name the name of the resource
     * @param resource the resource
     */
    public void addPageResource(String name, Object resource)
    {
        Map<String, Object> prm = FacesUtils.getPageResourceMap(FacesUtils.getContext());
        prm.put(name, this);
    }
    
    /**
     * Returns the page resource object previously added by addPageResource(...)
     * @param name the name of the resource
     * @return resource the resource
     */
    public Object getPageResource(String name)
    {
        Map<String, Object> prm = FacesUtils.getPageResourceMap(FacesUtils.getContext());
        return prm.get(name);
    }
    
    /* Default Init Method */
    public void doInit()
    {
        if (pageElements != null)
        {   // Init Page Elements
            for (PageElement pe : pageElements)
                doInitElement(pe);
        }
    }

    public void doRefresh()
    {
        if (pageElements != null)
        {   // Refresh Page Elements
            for (PageElement pe : pageElements)
                doRefreshElement(pe);
        }
    }
    
    /**
     * called by doInit() to initialize a particular page element
     * @param pe the page element to initialize
     */
    protected void doInitElement(PageElement pe)
    {
        pe.onInitPage();
    }
    
    /**
     * called by doRefresh() to refresh a particular page element
     * @param pe the page element to refresh
     */
    protected void doRefreshElement(PageElement pe)
    {
        pe.onRefreshPage();
    }
    
    /* Helpers */
    
    protected final TextResolver getTextResolver()
    {
        FacesContext fc = FacesUtils.getContext();
        return FacesUtils.getTextResolver(fc);
    }
}
