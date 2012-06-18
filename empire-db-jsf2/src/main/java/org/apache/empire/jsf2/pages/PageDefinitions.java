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

import java.util.LinkedHashMap;

import org.apache.empire.commons.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.faces.mgbean.BeanManager;
import com.sun.faces.mgbean.ManagedBeanInfo;


public abstract class PageDefinitions
{
    private static final Logger log = LoggerFactory.getLogger(PageDefinitions.class);

    private static LinkedHashMap<String,PageDefinition> pageMap = new LinkedHashMap<String,PageDefinition>();
    
    private static PageDefinitions instance = null;

	public static PageDefinitions getInstance()
    {
        return instance;
    }
    
    protected String pageUriExtension = null; // ".jsf";  
	
    protected PageDefinitions()
    {
        if (instance!=null) 
        {
            throw new RuntimeException("PageDefintions alreday defined. Only one Instance allowed!");
        }
        // init
        instance = this;
        log.info("PageDefintions class created");
    }
    
    public String getPageUriExtension() 
    {
		return pageUriExtension;
	}
    
    /**
     * Register page beans with the BeanManager
     * @param bm
     */
    public void registerPageBeans(BeanManager bm)
    {
        for (PageDefinition v : pageMap.values())
        {
            String beanName  = v.getPageBeanName();
            String beanClass = v.getPageBeanClass().getName();
            // check
            if (bm.getRegisteredBeans().containsKey(beanName))
            {
                throw new RuntimeException("Bean of name "+beanName+" already defined!");
            }
            log.info("Registering managed bean '{}' of class '{}' for page '{}'.", new Object[] { beanName, beanClass, v.getPath() });
            /*
            ManagedBeanInfo(String name,
                            String className,
                            String beanScope,
                            ManagedBeanInfo.MapEntry mapEntry,
                            ManagedBeanInfo.ListEntry listEntry,
                            List<ManagedBeanInfo.ManagedProperty> managedProperties,
                            Map<String,String> descriptions) {
            */
            ManagedBeanInfo mbi = new ManagedBeanInfo(beanName, beanClass, "view", null, null, null, null);
            bm.register(mbi);
        }
    }
    
    /**
     * returns true if the view exists in the page map or false otherwise
     * @param viewId
     * @return true if the view exists in the page map or false otherwise
     */
    public boolean containsView(String viewId)
    {
        return pageMap.containsKey(viewId);        
    }

    /**
     * returns the default (welcome) page definition.
     * By default this is the first page in the PageMap
     * @return the default (welcome) page definition. 
     */
    public PageDefinition getDefaultPage()
    {
        // first page is default
        return pageMap.values().iterator().next();
    }
    
    /**
     * returns a page definition for a given viewId
     * @param viewId
     * @return the page definition
     */
    public PageDefinition getPageFromViewId(String viewId)
    {
        // Empty-String == default page
        if (StringUtils.isEmpty(viewId))
            return getDefaultPage();
        // remove extension
        int ext = viewId.lastIndexOf('.');
        if (ext>0)
        	viewId = viewId.substring(0,ext);
        // find in map
        PageDefinition view = pageMap.get(viewId);
        if (view==null)
            log.error("No page defintion for viewId {}", viewId);
        return view;
    }
    
    /**
     * register a page. Called internally from PageDefinition constructor!
     * Do not call yourself.    
     * @param pageDef
     */
    protected static void registerPage(PageDefinition pageDef)
    {
        String name = pageDef.getPath();
        int ext = name.lastIndexOf('.');
        if (ext>0)
        	name = name.substring(0,ext);
        // Check Name
        if (pageMap.containsKey(name))
        {
            throw new RuntimeException("Page of name "+name+" already defined!");
        }
        log.info("Registering view '{}'.", name);
        // Register Name
        pageMap.put(name, pageDef);
    }
}
