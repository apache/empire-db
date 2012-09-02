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
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

import javax.el.ELContext;
import javax.faces.FactoryFinder;
import javax.faces.application.Application;
import javax.faces.application.ApplicationFactory;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.empire.db.DBDatabase;
import org.apache.empire.jsf2.pages.Page;
import org.apache.empire.jsf2.pages.PageDefinition;
import org.apache.empire.jsf2.pages.PageOutcome;
import org.apache.empire.jsf2.utils.ParameterMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FacesUtils
{

    private static final Logger log = LoggerFactory.getLogger(FacesUtils.class);

    /* App */

    public static FacesApplication getFacesApplication()
    {
        ApplicationFactory appFactory = (ApplicationFactory) FactoryFinder.getFactory(FactoryFinder.APPLICATION_FACTORY);
        Application app = appFactory.getApplication();
        return (FacesApplication) app;
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

    public static Map<String, Object> getRequestMap(final FacesContext fc)
    {
        return fc.getExternalContext().getRequestMap();
    }
    
    public static HttpServletRequest getHttpRequest(final FacesContext fc)
    {
        return (HttpServletRequest) fc.getExternalContext().getRequest();
    }

    public static Object getRequestAttribute(final FacesContext fc, final String key)
    {
        return getHttpRequest(fc).getAttribute(key);
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

    public static void redirectDirectly(final FacesContext fc, final String url)
    {
        try
        {   // log
            if (log.isDebugEnabled())
                log.debug("Redirecting directly to {}.", url);
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
    
    /* Connection */
    
    public Connection getConnection(final FacesContext fc, DBDatabase db) 
    {
        return getFacesApplication().getConnectionForRequest(fc, db);
    }
    
    public void releaseAllConnections(final FacesContext fc, boolean commit)
    {
        getFacesApplication().releaseAllConnections(fc, commit);
    }
    
    public void releaseAllConnections(final FacesContext fc, DBDatabase db, boolean commit)
    {
        getFacesApplication().releaseConnection(fc, db, commit);
    }
    
    /* Pages */

    public static Page getPage(final FacesContext fc)
    {
        Page page = (Page) getManagedBean(fc, "page");
        if (page==null)
            log.error("Page bean {} does not exist!");
        return page; 
    }

    /* Parameter-map */
    public static final String PARAMETER_MAP_ATTRIBUTE = ParameterMap.class.getSimpleName();
    
    public static ParameterMap getParameterMap(final FacesContext fc)
    {
        Map<String, Object> sm = fc.getExternalContext().getSessionMap();
        Object pm = sm.get(PARAMETER_MAP_ATTRIBUTE);
        if (pm==null)
        {   pm = new ParameterMap();
            sm.put(PARAMETER_MAP_ATTRIBUTE, pm);
        }
        return (ParameterMap)pm;
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

        final ELContext elcontext = fc.getELContext();
        final Application application = fc.getApplication();

        return application.getELResolver().getValue(elcontext, null, name);
    }
    
    public static <T> T getManagedBean(final FacesContext fc, final Class<T> cls)
    {
        String name = cls.getName();
        int i = name.lastIndexOf('.')+1;
        name  = name.substring(i, i+1).toLowerCase()+name.substring(i+1);
        @SuppressWarnings("unchecked")
        T bean = (T)getManagedBean(fc, name);
        if (bean==null)
            log.warn("Managed Bean {} ist not available.", name);
        return bean;
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
        return ((FacesApplication)fc.getApplication()).getTextResolver(fc);
    }
    
    public static String getMessage(final FacesContext fc, String key)
    {
        return getTextResolver(fc).resolveKey(key);
    }

    public static void addInfoMessage(FacesContext fc, String clientId, String msg)
    {
        fc.addMessage(clientId, new FacesMessage(FacesMessage.SEVERITY_INFO, msg, msg));
    }
    
    public static void addInfoMessage(FacesContext fc, String msg)
    {
        addInfoMessage(fc, null, msg);
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
    
}
