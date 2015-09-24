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
package org.apache.empire.jsf2.components;

import java.io.IOException;

import javax.faces.component.NamingContainer;
import javax.faces.component.UIComponent;
import javax.faces.component.UINamingContainer;
import javax.faces.component.UIOutput;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.apache.empire.commons.StringUtils;
import org.apache.empire.jsf2.utils.TagEncodingHelper;

public class MenuListTag extends UIOutput implements NamingContainer
{
    // Logger
    // private static final Logger log = LoggerFactory.getLogger(MenuTag.class);
    
    protected final TagEncodingHelper helper = new TagEncodingHelper(this, "eMenuList");
    
    private String currentId = null; 
    private String currentClass = null; 
    // private String enabledClass = null; 
    private String disabledClass = null; 
    private String expandedClass = null;
    private String defaultItemClass = null; // e.g. "level{}"
    private int level = 0;

    @Override
    public String getFamily()
    {
        return UINamingContainer.COMPONENT_FAMILY; 
    }
        
    @Override
    public void encodeBegin(FacesContext context)
        throws IOException
    {
        // call base
        super.encodeBegin(context);
        
        initMenuAttributes();

        // render components
        ResponseWriter writer = context.getResponseWriter();
        writer.startElement("ul", this);
        // writeAttribute(writer, map, "id");
        helper.writeAttribute(writer, "class", helper.getTagAttributeString("styleClass"));
        helper.writeAttribute(writer, "style", helper.getTagAttributeString("style"));
    }

    /*
    @Override
    public boolean getRendersChildren()
    {
        boolean test = super.getRendersChildren();
        return test;
    }
    */
    
    @Override
    public void encodeChildren(FacesContext context)
        throws IOException
    {
        super.encodeChildren(context);
    }

    @Override
    public void encodeEnd(FacesContext context)
        throws IOException
    {
        // call base
        super.encodeEnd(context);
        // close
        ResponseWriter writer = context.getResponseWriter();
        writer.endElement("ul");
    }
    
    private void initMenuAttributes()
    {        
        currentId        = helper.getTagAttributeString("currentId"); 
        currentClass     = helper.getTagAttributeString("currentClass"); 
        // enabledClass  = StringUtils.toString(map.get("enabledClass")); 
        disabledClass    = helper.getTagAttributeString("disabledClass"); 
        expandedClass    = helper.getTagAttributeString("expandedClass");
        defaultItemClass = helper.getTagAttributeString("defaultItemClass");
        
        MenuListTag parent = getParentMenu();
        if (parent==null)
            return;
        
        if (currentId==null)
            currentId = parent.getCurrentId();  
        if (currentClass==null)
            currentClass = parent.getCurrentClass();  
        // if (enabledClass==null)
        //     enabledClass = parent.getEnabledClass();
        if (disabledClass==null)
            disabledClass = parent.getDisabledClass();
        if (expandedClass==null)
            expandedClass = parent.getExpandedClass();
        if (defaultItemClass==null)
            defaultItemClass = parent.defaultItemClass;
        
        // Copy parent Info
        level = parent.level + 1;
    }

    protected MenuListTag getParentMenu()
    {
        // walk upwards the parent component tree and return the first record component found (if
        // any)
        UIComponent parent = this;
        while ((parent = parent.getParent()) != null)
        {
            if (parent instanceof MenuListTag)
            {
                return (MenuListTag) parent;
            }
        }
        return null;
    }
    
    public String getCurrentId()
    {
        return currentId;
    }

    public String getCurrentClass()
    {
        return currentClass;
    }

    /*
    public String getEnabledClass()
    {
        return enabledClass;
    }
    */

    public String getDisabledClass()
    {
        return disabledClass;
    }

    public String getExpandedClass()
    {
        return expandedClass;
    }
    
    public int getLevel()
    {
        return level;
    }

    public String getItemStyleClass()
    {
        if (defaultItemClass!=null && defaultItemClass.indexOf("{}")>=0)
            return StringUtils.replace(defaultItemClass, "{}", String.valueOf(level));
        // return default
        return defaultItemClass;
    }

    /* setters */
    
    public void setCurrentId(String currentId)
    {
        this.currentId = currentId;
    }

    public void setCurrentClass(String currentClass)
    {
        this.currentClass = currentClass;
    }

    /*
    public void setEnabledClass(String enabledClass)
    {
        this.enabledClass = enabledClass;
    }
    */

    public void setDisabledClass(String disabledClass)
    {
        this.disabledClass = disabledClass;
    }

    public void setExpandedClass(String expandedClass)
    {
        this.expandedClass = expandedClass;
    }

}
