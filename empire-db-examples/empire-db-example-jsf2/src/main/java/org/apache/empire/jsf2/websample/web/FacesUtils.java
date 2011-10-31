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
package org.apache.empire.jsf2.websample.web;

import java.sql.Connection;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.el.ELContext;
import javax.faces.FactoryFinder;
import javax.faces.application.Application;
import javax.faces.application.ApplicationFactory;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

import org.apache.empire.jsf2.websample.db.SampleDB;
import org.apache.empire.jsf2.websample.web.pages.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FacesUtils
{

    private static final Logger log = LoggerFactory.getLogger(FacesUtils.class);

    /**
     * gets the Managed Bean of a given bean name
     * 
     * @param context
     *            the faces context
     * @param name
     *            the name of the bean
     * @return the managed bean instance or null if name was not found
     */
    public static Object getManagedBean(final FacesContext context, final String name)
    {
        if (context == null)
        {
            throw new NullPointerException("context must not be null");
        }
        if (name == null)
        {
            throw new NullPointerException("name must not be null");
        }

        final ELContext elcontext = context.getELContext();
        final Application application = context.getApplication();

        return application.getELResolver().getValue(elcontext, null, name);
    }

    public static Object getManagedBean(final String name)
    {
        if (FacesContext.getCurrentInstance() == null)
        {
            return null;
        }
        return FacesUtils.getManagedBean(FacesContext.getCurrentInstance(), name);
    }

    public static Object getManagedBean(Class<?> clazz)
    {
        return FacesUtils.getManagedBean(getManagedBeanName(clazz));
    }

    public static String getManagedBeanName(Class<?> clazz)
    {
    	String className = clazz.getSimpleName();
    	className = className.substring(0,1).toLowerCase()+className.substring(1);
    	return className;
    }
    
    /**
     * returns the request param value for a given param
     * 
     * @param context
     *            the faces context
     * @param param
     *            the param name
     * @return the param value
     */
    public static String getRequestParam(final FacesContext context, final String param)
    {
        return context.getExternalContext().getRequestParameterMap().get(param);
    }

    /**
     * returns the request param value for a given param
     * 
     * @param param
     *            the param name
     * @return the param value
     */
    public static String getRequestParam(final String param)
    {
        return FacesUtils.getRequestParam(FacesContext.getCurrentInstance(), param);
    }

    /**
     * Add information message.
     * 
     * @param msg
     *            the information message
     */
    public static void addInfoMessage(String msg)
    {
        FacesUtils.addInfoMessage(null, msg);
    }

    /**
     * Add information message to a specific client.
     * 
     * @param clientId
     *            the client id
     * @param msg
     *            the information message
     */
    public static void addInfoMessage(String clientId, String msg)
    {
        FacesContext.getCurrentInstance().addMessage(clientId, new FacesMessage(FacesMessage.SEVERITY_INFO, msg, msg));
    }

    /**
     * Add error message.
     * 
     * @param msg
     *            the error message
     */
    public static void addErrorMessage(String msg)
    {
        FacesUtils.addErrorMessage(null, msg);
    }

    /**
     * Add error message to a specific client.
     * 
     * @param clientId
     *            the client id
     * @param msg
     *            the error message
     */
    public static void addErrorMessage(String clientId, String msg)
    {
        FacesContext.getCurrentInstance().addMessage(clientId, new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, msg));
    }

    public static Application getFacesApplication()
    {
        ApplicationFactory appFactory = (ApplicationFactory) FactoryFinder.getFactory(FactoryFinder.APPLICATION_FACTORY);
        return appFactory.getApplication();
    }

    // public static String getResource(String key)
    // {
    // String resource;
    // // TODO: remove debug try/catch
    // try
    // {
    // // TODO resource = ResourceBundle.getBundle("lang.messages",
    // FacesUtils.getUserBean().getLocale())
    // .getString(key);
    // }
    // catch (MissingResourceException e)
    // {
    // resource = "Key '" + key + "' missing.";
    // e.printStackTrace();
    // }
    // if (resource == null)
    // {
    // throw new RuntimeException("The resource '" + key + "' does not exist.");
    // }
    // return resource;
    // }

    public static SampleApplication getApplication()
    {
        return (SampleApplication) FacesContext.getCurrentInstance().getExternalContext().getApplicationMap().get("app");
    }

    public static SampleDB getDatabase()
    {
        return FacesUtils.getApplication().getDatabase();
    }

    public static HttpServletRequest getHttpRequest()
    {
        // Check
        /*
         * HttpServletRequest req =
         * (HttpServletRequest)FacesContext.getCurrentInstance().getExternalContext().getRequest();
         * FWSRequest x1 =
         * (FWSRequest)req.getAttribute(FWSRequest.REQUEST_ATTRIBUTE_NAME); FWSRequest x2 =
         * FWSRequest.get();
         * log.info("Comparing FWS-Requests: x1={} | x2={}", x1.getId(), x2.getId()); if
         * (x1.getId()!=x2.getId()) {
         * x1.checkDisposed(false); x2.checkDisposed(false); }
         */
        return SampleRequest.get().getHttpRequest();
    }

    public static Connection getConnection()
    {
        int reqId = SampleRequest.get() != null ? SampleRequest.get().getId() : 0;
        FacesUtils.log.info("REQUEST {}: obtaining connection.", reqId);
        return SampleRequest.get().getConnection();
    }

    static Connection getRequestConnection(ServletRequest request)
    {
        int requestId = (Integer) request.getAttribute("requestId");
        FacesUtils.log.debug("REQUEST {}: obtaining connection.", requestId);
        return (Connection) request.getAttribute("connection");
    }

    public static String getMessageForKey(String messageKey)
    {
		// if translation available, use it
		try {
			return ResourceBundle.getBundle("lang.messages").getString(messageKey);
		} catch (MissingResourceException mre) {
			log.warn("Couldn't find resource for '" + messageKey
					+ "', using key directly instead.");
		}
        return "!!!"+ messageKey;
    }

    public static void setPage(String page)
    {
        ((SampleSession) FacesUtils.getManagedBean("sampleSession")).setPage(page);
    }

    public static Page getPage()
    {
        return (Page) FacesUtils.getManagedBean(((SampleSession) FacesUtils.getManagedBean("sampleSession")).getPage());
    }

    public static SampleSession getSampleSession()
    {
        return (SampleSession) FacesUtils.getManagedBean("sampleSession");
    }
}
