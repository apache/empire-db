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

import org.apache.empire.commons.StringUtils;
import org.apache.empire.jsf2.app.FacesUtils;
import org.apache.empire.jsf2.utils.ParameterMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class PageDefinition
{
    private static final Logger log = LoggerFactory.getLogger(PageDefinitions.class);

    private static final String ACTION_PARAMETER_TYPE = "ACTION";
    
    private String path;
    private String pageBeanName;
    private Class<? extends Page> pageBeanClass;
    private PageDefinition parent = null;

    /*
    private static Hashtable<String, String> actionCodeMap = new Hashtable<String, String>();
    
    private static Hashtable<String, String> actionMap = new Hashtable<String, String>();

    private static String encodeActionParam(String action)
    {
        String param = ParameterMap.encodeString(action);
        actionMap.put(param, action);
        return param;
    }
    
    public static String decodeActionParam(String param)
    {
        String action = actionMap.get(param);
        if (action==null)
            log.warn("no action available for param {}.", param);
        return action;
    }
    */

    private static String encodeActionParam(String action)
    {
        ParameterMap pm = FacesUtils.getParameterMap(FacesUtils.getContext());
        if (pm==null)
            return action;
        return pm.put(ACTION_PARAMETER_TYPE, action, true);
    }
    
    public static String decodeActionParam(String param)
    {
        ParameterMap pm = FacesUtils.getParameterMap(FacesUtils.getContext());
        if (pm==null)
            return param;
        String action = StringUtils.valueOf(pm.get(ACTION_PARAMETER_TYPE, param));
        if (action==null)
            log.warn("no action available for param {}.", param);
        return action;
    }
    
    public PageDefinition(String path, Class<? extends Page> pageBeanClass)
    { 
        this.path = path;
        this.pageBeanClass = pageBeanClass;
        // beanName 
        int lastSlash = path.lastIndexOf("/");
        String name = path.substring(lastSlash + 1);
        this.pageBeanName = name.replace(".xhtml", "Page");
        // add this view
        PageDefinitions.registerPage(this);
    }
    
    public PageDefinition(String path, Class<? extends Page> pageClass, PageDefinition parent)
    {
        this(path, pageClass);
        this.parent = parent;
    }
    
    public String getPath()
    {
        return path;
    }
    
    public String getPageBeanName()
    {
        return pageBeanName;
    }

    public Class<? extends Page> getPageBeanClass()
    {
        return pageBeanClass;
    }

    public PageDefinition getParent()
    {
        return parent;
    }

    /* Outcome generator */
    
    public PageOutcome getOutcome()
    {
        return new PageOutcome(path.replace(".xhtml", ".iface"));
    }
    
    public PageOutcome getOutcome(String action)
    {
        PageOutcome outcome = getOutcome();
        if (StringUtils.isNotEmpty(action))
            outcome = outcome.addParam("action", encodeActionParam(action));
        return outcome;
    }
    
    public PageOutcome getRedirect()
    {
        PageOutcome outcome = getOutcome();
        outcome = outcome.addParamWithValue("faces-redirect=true");
        return outcome;
    }
    
    public PageOutcome getRedirect(String action)
    {   
        PageOutcome outcome = getRedirect();
        if (StringUtils.isNotEmpty(action))
            outcome = outcome.addParam("action", encodeActionParam(action));
        return outcome;
    }
    
    public PageOutcome getRedirectWithViewParams()
    {
        PageOutcome outcome = getRedirect();
        outcome = outcome.addParamWithValue("includeViewParams=true");
        return outcome;
    }
    
    public PageOutcome getRedirectWithViewParams(String action)
    {
        PageOutcome outcome = getRedirectWithViewParams();
        if (StringUtils.isNotEmpty(action))
            outcome = outcome.addParam("action", encodeActionParam(action));
        return outcome;
    }

    @Override
    public String toString()
    {
        return getOutcome().toString();
    }
}
