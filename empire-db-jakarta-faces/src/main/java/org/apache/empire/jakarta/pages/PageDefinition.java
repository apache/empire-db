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
package org.apache.empire.jakarta.pages;


import org.apache.empire.commons.StringUtils;
import org.apache.empire.jakarta.app.FacesUtils;
import org.apache.empire.jakarta.utils.ParameterMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class PageDefinition // *Deprecated* implements Serializable
{
    // *Deprecated* private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(PageDefinitions.class);

    private static final String ACTION_PARAMETER_TYPE = "ACTION";
    private static final String ACTION_PARAMETER_NAME = "action";
    
    private final String path;
    private final String fileExtension;

    private final String pageBeanName;
    private final Class<? extends Page> pageBeanClass;
    private final PageDefinition parent;

    /**
     * Encodes a page action method into a MD5 code and puts it on the session's action list.
     * @param pageDef the page for which the action is provided
     * @param action the name of the action method
     * @return the encoded action
     */
    private static String encodeActionParam(PageDefinition pageDef, String action)
    {
        ParameterMap pm = FacesUtils.getParameterMap(FacesUtils.getContext());
        if (pm==null)
            return action;
        return pm.put(ACTION_PARAMETER_TYPE, StringUtils.concat(pageDef.getPageBeanName(), ":", action), action, true);
    }

    /**
     * Decodes a page action method from a MD5 code. 
     * @param param the MD5 code for a page action method
     * @return the action method name
     */
    public static String decodeActionParam(String param)
    {
        ParameterMap pm = FacesUtils.getParameterMap(FacesUtils.getContext());
        if (pm==null)
            return param;
        String action = StringUtils.toString(pm.get(ACTION_PARAMETER_TYPE, param));
        if (action==null)
            log.warn("no action available for param {}.", param);
        return action;
    }
    
    /**
     * Constructs a page definition
     * @param path  the path of the view associated with this page
     * @param pageBeanClass the page bean class associated with this page
     * @param parent the parent page (if any). May be null
     * @param pageBeanName the page bean name. If null this will be calculated from the path
     */
    public PageDefinition(String path, Class<? extends Page> pageBeanClass, PageDefinition parent, String pageBeanName)
    { 
        this.path = path;
        this.pageBeanClass = pageBeanClass;
        this.parent = parent;
        // extension
        int ext = path.lastIndexOf('.');
        fileExtension = (ext>0) ? path.substring(ext) : null;
        // beanName
        if (pageBeanName==null) 
            this.pageBeanName = getPageBeanNameFromPath(path, fileExtension);
        else
            this.pageBeanName = pageBeanName;
        // add this view
        PageDefinitions.registerPage(this);
    }

    /**
     * Constructs a page definition
     * @param path  the path of the view associated with this page
     * @param pageBeanClass the page bean class associated with this page
     * @param pageBeanName the page bean name. If null this will be calculated from the path
     */
    public PageDefinition(String path, Class<? extends Page> pageBeanClass, String pageBeanName)
    {
        this(path, pageBeanClass, null, pageBeanName);
    }

    /**
     * Constructs a page definition
     * @param path  the path of the view associated with this page
     * @param pageBeanClass the page bean class associated with this page
     * @param parent the parent page (if any). May be null
     */
    public PageDefinition(String path, Class<? extends Page> pageBeanClass, PageDefinition parent)
    {
        this(path, pageBeanClass, parent, null);
    }

    /**
     * Constructs a page definition
     * @param path  the path of the view associated with this page
     * @param pageBeanClass the page bean class associated with this page
     */
    public PageDefinition(String path, Class<? extends Page> pageBeanClass)
    {
        this(path, pageBeanClass, null, null);
    }
    
    protected String getPageBeanNameFromPath(String path, String extension)
    {
        // beanName 
        int lastSlash = path.lastIndexOf('/');
        String name = path.substring(lastSlash + 1);
        if (extension!=null)
            name = name.substring(0,(name.length()-extension.length()));
        return name;
    }
    
    public String getPath()
    {
        return path;
    }

    public String getFileExtension() 
    {
        return fileExtension;
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
        String uri = PageDefinitions.getInstance().getPageUri(this);
        return new PageOutcome(uri);
    }
    
    public PageOutcome getOutcome(String action)
    {
        PageOutcome outcome = getOutcome();
        if (StringUtils.isNotEmpty(action))
            outcome = outcome.addParam(ACTION_PARAMETER_NAME, encodeActionParam(this, action));
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
            outcome = outcome.addParam(ACTION_PARAMETER_NAME, encodeActionParam(this, action));
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
            outcome = outcome.addParam(ACTION_PARAMETER_NAME, encodeActionParam(this, action));
        return outcome;
    }

    @Override
    public String toString()
    {
        return getOutcome().toString();
    }
}
