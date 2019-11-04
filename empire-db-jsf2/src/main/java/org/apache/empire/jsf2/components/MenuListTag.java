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
import org.apache.empire.jsf2.utils.TagEncodingHelperFactory;

public class MenuListTag extends UIOutput implements NamingContainer
{
    // Logger
    // private static final Logger log = LoggerFactory.getLogger(MenuListTag.class);
    
    protected final TagEncodingHelper helper = TagEncodingHelperFactory.create(this, "eMenuList");
    
    private enum Properties
    {
        currentId,
        currentClass,
        parentClass,
        disabledClass,
        expandedClass,
        itemWrapTag,
        defaultItemClass;
    }
    
    protected String currentId = null; 
    protected String currentClass = null; 
    protected String parentClass = null;
    protected String disabledClass = null; 
    protected String expandedClass = null;
    protected String itemWrapTag = null;
    protected String defaultItemClass = null; // e.g. "level{}"
    protected int level = 0;

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
        
        initMenuAttributes(context);

        // render components
        ResponseWriter writer = context.getResponseWriter();
        writer.startElement("ul", this);
        // writeAttribute(writer, map, "id");
        helper.writeAttribute(writer, "class", helper.getTagAttributeString("styleClass"));
        helper.writeAttribute(writer, "style", helper.getTagAttributeString("style"));
        // previousId
        /*
        if (prevMenuId!=null)
            helper.writeAttribute(writer, "previousId", prevMenuId);
        */    
    }

    @Override
    public boolean getRendersChildren()
    {
        boolean test = super.getRendersChildren();
        return test;
    }
    
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
    
    protected void initMenuAttributes(FacesContext context)
    {        
        currentId        = helper.getTagAttributeString(Properties.currentId.name()); 
        currentClass     = helper.getTagAttributeString(Properties.currentClass.name()); 
        disabledClass    = helper.getTagAttributeString(Properties.disabledClass.name()); 
        parentClass      = helper.getTagAttributeString(Properties.parentClass.name());
        expandedClass    = helper.getTagAttributeString(Properties.expandedClass.name());
        itemWrapTag      = helper.getTagAttributeString(Properties.itemWrapTag.name());
        defaultItemClass = helper.getTagAttributeString(Properties.defaultItemClass.name());

        // find parent
        MenuListTag parent = getParentMenu();
        if (parent==null)
            return;
        
        if (currentId==null)
            currentId = parent.getCurrentId();
        if (currentClass==null)
            currentClass = parent.getCurrentClass();  
        if (disabledClass==null)
            disabledClass = parent.getDisabledClass();
        if (parentClass==null)
            parentClass = parent.getParentClass();
        if (expandedClass==null)
            expandedClass = parent.getExpandedClass();
        if (itemWrapTag==null)
            itemWrapTag = parent.getItemWrapTag();
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
        if (currentId==null)
            currentId= StringUtils.toString(getStateHelper().get(Properties.currentId));
        return currentId;
    }

    public String getCurrentClass()
    {
        if (currentClass==null)
            currentClass= StringUtils.toString(getStateHelper().get(Properties.currentClass));
        return currentClass;
    }

    public String getDisabledClass()
    {
        if (disabledClass==null)
            disabledClass= StringUtils.toString(getStateHelper().get(Properties.disabledClass));
        return disabledClass;
    }

    public String getParentClass()
    {
        if (parentClass==null)
            parentClass= StringUtils.toString(getStateHelper().get(Properties.parentClass));
        return parentClass;
    }

    public String getExpandedClass()
    {
        if (expandedClass==null)
            expandedClass= StringUtils.toString(getStateHelper().get(Properties.expandedClass));
        return expandedClass;
    }

    public String getItemWrapTag()
    {
        if (itemWrapTag==null)
            itemWrapTag= StringUtils.toString(getStateHelper().get(Properties.itemWrapTag));
        return itemWrapTag;
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
        // save
        getStateHelper().put(Properties.currentId, currentId);
    }

    public void setCurrentClass(String currentClass)
    {
        this.currentClass = currentClass;
        // save
        getStateHelper().put(Properties.currentClass, currentClass);
    }

    public void setDisabledClass(String disabledClass)
    {
        this.disabledClass = disabledClass;
        // save
        getStateHelper().put(Properties.disabledClass, disabledClass);
    }

    public void setParentClass(String parentClass)
    {
        this.parentClass = parentClass;
        // save
        getStateHelper().put(Properties.parentClass, parentClass);
    }

    public void setExpandedClass(String expandedClass)
    {
        this.expandedClass = expandedClass;
        // save
        getStateHelper().put(Properties.expandedClass, expandedClass);
    }

    public void setItemWrapTag(String itemWrapTag)
    {
        this.itemWrapTag = itemWrapTag;
        // save
        getStateHelper().put(Properties.itemWrapTag, itemWrapTag);
    }

}
