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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.el.ValueExpression;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.component.UIViewRoot;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.commons.StringUtils;
import org.apache.empire.data.Column;
import org.apache.empire.exceptions.EmpireException;
import org.apache.empire.exceptions.InternalException;
import org.apache.empire.exceptions.ItemNotFoundException;
import org.apache.empire.jsf2.impl.FacesImplementation;
import org.apache.empire.jsf2.pages.Page;
import org.apache.empire.jsf2.pages.PageDefinition;
import org.apache.empire.jsf2.pages.PageOutcome;
import org.apache.empire.jsf2.utils.ParameterMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FacesUtils
{

    private static final Logger log = LoggerFactory.getLogger(FacesUtils.class);
    
    public static final String SKIP_INPUT_VALIDATION_PARAM = "empire.jsf.input.skipValidation";

    /* App */

    public static WebApplication getWebApplication()
    {
        return WebApplication.getInstance();
    }
    
    public static FacesImplementation getFacesImplementation()
    {
        return getWebApplication().getFacesImplementation();    
    }
    
    public static FacesContext getContext()
    {
        return FacesContext.getCurrentInstance();
    }

    /* Session */
    
    public static Map<String, Object> getSessionMap(final FacesContext fc)
    {
        return fc.getExternalContext().getSessionMap();
    }
    
    /* Request */
    
    public static HttpServletRequest getHttpRequest(final FacesContext fc)
    {
        return (HttpServletRequest) fc.getExternalContext().getRequest();
    }
    
    public static String getRequestUserAgent()
    {
        String agent = null;
        Object req = FacesContext.getCurrentInstance().getExternalContext().getRequest();
        if (req instanceof HttpServletRequest)
        {
            agent = ((HttpServletRequest)req).getHeader("user-agent");
        }
        return (agent!=null ? agent : "");
    }
    
    public static boolean isRequestUserAgentIE()
    {
        String userAgent = getRequestUserAgent();
        return (userAgent.indexOf("MSIE")>=0);
    }

    public static String getRequestContextPath()
    {
        return FacesContext.getCurrentInstance().getExternalContext().getRequestContextPath();
    }

    public static Object getRequestAttribute(final String key)
    {
        FacesContext fc = getContext();
        return getRequestAttribute(fc, key);
    }

    public static Object getRequestAttribute(final FacesContext fc, final String key)
    {
        return getHttpRequest(fc).getAttribute(key);
    }

    public static void setRequestAttribute(final String key, Object value)
    {
        FacesContext fc = getContext();
        setRequestAttribute(fc, key, value);
    }

    public static void setRequestAttribute(final FacesContext fc, final String key, Object value)
    {
        if (value!=null)
            getHttpRequest(fc).setAttribute(key, value);
        else
            getHttpRequest(fc).removeAttribute(key);
    }

    public static String getRequestParam(final FacesContext context, final String param)
    {
        return context.getExternalContext().getRequestParameterMap().get(param);
    }

    public static String getRequestParam(final String key)
    {
        FacesContext fc = getContext();
        return getRequestParam(fc, key);
    }

    public static boolean isSkipInputValidation(final FacesContext fc)
    {
        // Skip validate
        String val = FacesUtils.getRequestParam(fc, SKIP_INPUT_VALIDATION_PARAM);
        return (val!=null && ObjectUtils.getBoolean(val));
    }
    
    public static boolean isSkipInputValidation()
    {
        FacesContext fc = getContext();
        return isSkipInputValidation(fc); 
    }

    /* Navigation */
    
    public static void redirectDirectly(final FacesContext fc, final String url)
    {
        try
        {   // log
            if (log.isDebugEnabled())
                log.debug("Redirecting directly to {}.", url);
            if (fc.getResponseComplete())
                log.warn("Redirecting although response is already complete!");
            // redirectDirectly
            fc.getExternalContext().redirect(url);
            fc.responseComplete();
        }
        catch (IOException e)
        {
            log.error("Unable to redirect to " + url);
        }
    }

    public static void redirectDirectly(final FacesContext fc, final PageOutcome outcome)
    {
        String ctxPath = fc.getExternalContext().getRequestContextPath();
        String pageURI = ctxPath + outcome.toString();
        FacesUtils.redirectDirectly(fc, pageURI);
    }

    public static void redirectDirectly(final FacesContext fc, final PageDefinition page)
    {
        FacesUtils.redirectDirectly(fc, page.getOutcome());
    }
    
    /* Connection 
    
    public DBContext getDBContext(final FacesContext fc, DBDatabase db) 
    {
        return getWebApplication().getDBContextForRequest(fc, db);
    }
    
    public void releaseDBContext(final FacesContext fc, DBDatabase db, boolean commit)
    {
        getWebApplication().releaseDBContext(fc, db, commit);
    }
    
    public void releaseAllDBContexts(final FacesContext fc, boolean commit)
    {
        getWebApplication().releaseAllDBContexts(fc, commit);
    }
    */
    
    /* Pages */

    public static Page getPage(final FacesContext fc)
    {
        UIViewRoot vr = fc.getViewRoot();
        if (vr==null)
            throw new ItemNotFoundException("ViewRoot");
        // find page
        Page page = (Page)vr.getViewMap().get("page");
        if (page==null)
            throw new ItemNotFoundException("page");
        // ok
        return page; 
    }

    /* Parameter-map */
    public static final String PARAMETER_MAP_ATTRIBUTE = "PARAMETER_MAP";

    public static final String PARAMETER_MAP_CLASS_ATTRIBUTE = "PARAMETER_MAP_CLASS";

    public static void setParameterMapClass(final FacesContext fc, Class<? extends ParameterMap>clazz)
    {
        Map<String, Object> am = fc.getExternalContext().getApplicationMap();
        am.put(PARAMETER_MAP_CLASS_ATTRIBUTE, clazz);
    }
    
    public static ParameterMap getParameterMap(final FacesContext fc)
    {
        Map<String, Object> sm = fc.getExternalContext().getSessionMap();
        ParameterMap pm = (ParameterMap)sm.get(PARAMETER_MAP_ATTRIBUTE);
        if (pm==null)
        {   try
            {   // Create Paramter Map
                Map<String, Object> am = fc.getExternalContext().getApplicationMap();
                Object pmClass = am.get(PARAMETER_MAP_CLASS_ATTRIBUTE);
                if (pmClass instanceof Class<?>) {
                    // ParamterMapClass provided as Object
                    pm = (ParameterMap)((Class<?>)pmClass).newInstance(); 
                } 
                else if (pmClass instanceof String) {
                    // ParamterMapClass provided as String
                    pm = (ParameterMap)Class.forName((String)pmClass).newInstance(); 
                }
                else {
                    // not provided, use default
                    pm = new ParameterMap();
                }
            }
            catch (ClassNotFoundException e)
            {
                throw new InternalException(e);
            }
            catch (InstantiationException e)
            {
                throw new InternalException(e);
            }
            catch (IllegalAccessException e)
            {
                throw new InternalException(e);
            }
            // put on session
            sm.put(PARAMETER_MAP_ATTRIBUTE, pm);
        }
        return pm;
    }
    
    public static ParameterMap getParameterMap()
    {
        return getParameterMap(getContext());        
    }

    /* PageResource-map */
    public static final String PAGE_RESOURCE_MAP_ATTRIBUTE = "pageResources"; /** use el-expression: #{pageResources.xxx} **/
    
    @SuppressWarnings("unchecked")
    public static Map<String, Object> getPageResourceMap(final FacesContext fc)
    {
        Map<String, Object> sm = fc.getExternalContext().getSessionMap();
        Map<String, Object> rm = (Map<String, Object>)sm.get(PAGE_RESOURCE_MAP_ATTRIBUTE);
        if (rm==null)
        {   rm = new HashMap<String, Object>();
            sm.put(PAGE_RESOURCE_MAP_ATTRIBUTE, rm);
        }
        return rm;
    }
    
    public static Map<String, Object> getPageResourceMap()
    {
        return getPageResourceMap(getContext());
    }
    
    /* Managed Beans */
    
    public static Object getManagedBean(final FacesContext fc, final String name)
    {
        if (fc == null)
        {
            throw new NullPointerException("context must not be null");
        }
        if (name == null)
        {
            throw new NullPointerException("name must not be null");
        }
        /*
        final ELContext elcontext = fc.getELContext();
        final Application application = fc.getApplication();
        return application.getELResolver().getValue(elcontext, null, name);
        */
        return getWebApplication().getFacesImplementation().getManagedBean(name, fc);
    }
    
    public static <T> T getManagedBean(final FacesContext fc, final Class<T> cls, final String name)
    {
        @SuppressWarnings("unchecked")
        T bean = (T)getManagedBean(fc, name);
        if (bean==null)
        {   log.warn("Managed Bean {} ist not available.", name);
            throw new ItemNotFoundException(name);
        }    
        return bean;
    }
    
    public static <T> T getManagedBean(final FacesContext fc, final Class<T> cls)
    {
        String name = cls.getName();
        int i = name.lastIndexOf('.')+1;
        name  = name.substring(i, i+1).toLowerCase()+name.substring(i+1);
        return getManagedBean(fc, cls, name);
    }
    
    public static <T> T getManagedBean(final Class<T> cls)
    {
        FacesContext fc = getContext();
        return getManagedBean(fc, cls); 
    }

    /*
    @SuppressWarnings("unchecked") 
    public static <T> T findBean(String beanName) { 
        FacesContext context = FacesContext.getCurrentInstance(); 
        return (T) context.getApplication().evaluateExpressionGet(context, "#{" + beanName + "}", Object.class); 
    } 
    */
    
    /* file */
    
    public static String getRealPath(final FacesContext fc, String path)
    {
        return (((ServletContext)fc.getExternalContext().getContext())).getRealPath(path);
    }

    public static String getFilePath(final FacesContext fc, String path, String file)
    {
        String realPath = getRealPath(fc, path);
        return realPath.endsWith(File.separator) ? realPath+file : realPath + File.separator + file ;
    }
    
    /* Messages */
    
    public static TextResolver getTextResolver(final FacesContext fc)
    {
        return getWebApplication().getTextResolver(fc);
    }
    
    public static String getMessage(final FacesContext fc, String key)
    {
        return getTextResolver(fc).resolveKey(key);
    }
    
    public static String getMessage(String messageKey)
    {
        return getMessage(getContext(), messageKey);
    }

    public static void addInfoMessage(FacesContext fc, String clientId, String msg)
    {
        fc.addMessage(clientId, new FacesMessage(FacesMessage.SEVERITY_INFO, msg, msg));
    }
    
    public static void addInfoMessage(FacesContext fc, String msg)
    {
        addInfoMessage(fc, null, msg);
    }

    public static void addWarnMessage(FacesContext fc, String clientId, String msg)
    {
        fc.addMessage(clientId, new FacesMessage(FacesMessage.SEVERITY_WARN, msg, msg));
    }
    
    public static void addWarnMessage(FacesContext fc, String msg)
    {
        addWarnMessage(fc, null, msg);
    }

    public static void addErrorMessage(FacesContext fc, String clientId, String msg)
    {
        fc.addMessage(clientId, new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, msg));
    }

    public static void addErrorMessage(FacesContext fc, String msg)
    {
        addErrorMessage(fc, null, msg);
    }

    /**
     * indicates whether submitted values in InputControl should be cleared or preserved.
     * Default is true.   
     * @param fc the faces context
     * @return true if the submitted values should be cleared or false if they shold be preserved
    public static boolean isClearSubmittedValues(FacesContext fc)
    {
        Object validate = fc.getExternalContext().getRequestMap().get("CLEAR_SUBMITTED_VALUES");
        return (validate!=null ? ObjectUtils.getBoolean(validate) : false);
    }

    public static void setClearSubmittedValues(FacesContext fc, boolean validate)
    {
        fc.getExternalContext().getRequestMap().put("CLEAR_SUBMITTED_VALUES", validate);
    }
    */
    
    public static FacesMessage getFacesErrorMessage(FacesContext fc, Throwable t)
    {
        if (!(t instanceof EmpireException))
            t = new InternalException(t);
        // Get Message
        TextResolver tr = getWebApplication().getTextResolver(fc);
        String msg = tr.getExceptionMessage((EmpireException)t);
        // create Faces Message
        return new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, null);
    }
    
    public static void redirectFromError(Page page, Throwable t)
    {
        PageOutcome pageTarget = page.getPageDefinition().getOutcome();
        // throw new InternalException(e);
        FacesContext fc = FacesUtils.getContext();
        boolean committed = fc.getExternalContext().isResponseCommitted();
        if (committed)
        {   log.warn("Cannot redirect to {} from an already committed response! Error is {}.", pageTarget, t.getMessage());
            return;
        }
        // redirect to target page
        FacesMessage facesMsg = getFacesErrorMessage(fc, t);
        ExternalContext ec = fc.getExternalContext();
        ec.getSessionMap().put("PAGE_SESSION_MESSAGE", facesMsg);
        // redirect
        FacesUtils.redirectDirectly(fc, pageTarget);
    }
    
    
    /* Component search */
    public static UIInput findInputComponent(UIComponent parent, Column column)
    {
        // Get Children
        Iterator<UIComponent> children = parent.getFacetsAndChildren();
        while (children.hasNext())
        {   // Iterate through all children 
            UIComponent nextChild = children.next();
            log.info("Checking child {}", nextChild.getClass().getName());
            if (nextChild instanceof UIInput)
            {   // Check Column Attribute
                Object col = nextChild.getAttributes().get("column");
                if (col==null)
                {   // Check ID
                    ValueExpression ve = nextChild.getValueExpression("column");
                    if (ve!=null)
                        log.warn("TODO: evaluate Value Expression!");
                }
                else if (column.equals(col))
                {   // check form
                    return (UIInput)nextChild;
                }    
            }
            else
            {   // recurse children and facets    
                UIInput input = findInputComponent(nextChild, column); 
                if (input!=null)
                    return input;
            }
        }
        // Not found
        return null;
    }

    public static UIInput findInputComponent(FacesContext fc, String formId, Column column)
    {
        UIComponent parent = fc.getViewRoot();
        if (StringUtils.isNotEmpty(formId))
        {
            parent = WebApplication.findChildComponent(parent, formId);
            if (parent==null)
                return null; // not found
        }
        // find
        return findInputComponent(parent, column);
    }
    
}
