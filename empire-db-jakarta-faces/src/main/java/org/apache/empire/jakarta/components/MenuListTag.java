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
package org.apache.empire.jakarta.components;

import java.io.IOException;

import org.apache.empire.commons.StringUtils;
import org.apache.empire.jakarta.controls.InputControl;
import org.apache.empire.jakarta.utils.TagEncodingHelper;
import org.apache.empire.jakarta.utils.TagEncodingHelperFactory;
import org.apache.empire.jakarta.utils.TagStyleClass;

import jakarta.faces.component.UIComponent;
import jakarta.faces.component.UINamingContainer;
import jakarta.faces.component.UIOutput;
import jakarta.faces.context.FacesContext;
import jakarta.faces.context.ResponseWriter;

public class MenuListTag extends UIOutput // implements NamingContainer
{
    // Logger
    // private static final Logger log = LoggerFactory.getLogger(MenuListTag.class);
    
    protected final TagEncodingHelper helper = TagEncodingHelperFactory.create(this, TagStyleClass.MENU.get());
    
    private enum MenuProperty
    {
        currentId,
        currentClass,
        parentClass,
        disabledClass,
        expandedClass,
        itemWrapTag,
        defaultItemClass;
        // autoItemId
    }
    
    protected String currentId = null; 
    protected String currentClass = null; 
    protected String parentClass = null;
    protected String disabledClass = null; 
    protected String expandedClass = null;
    protected String itemWrapTag = null;
    protected String defaultItemClass = null; // e.g. "level{}"
    // protected Boolean autoItemId = null;
    protected int level = 0;
    
    private MenuListTag parentMenu = null; 

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
        
        //Compoent-ID
        helper.writeComponentId(writer);
        
        // Style class and style
        helper.writeAttribute(writer, "class", getMenuStyleClass());
        helper.writeAttribute(writer, "style", helper.getTagAttributeString("style"));
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
        currentId        = helper.getTagAttributeString(MenuProperty.currentId.name()); 
        currentClass     = helper.getTagAttributeString(MenuProperty.currentClass.name()); 
        parentClass      = helper.getTagAttributeString(MenuProperty.parentClass.name());
        disabledClass    = helper.getTagAttributeString(MenuProperty.disabledClass.name()); 
        expandedClass    = helper.getTagAttributeString(MenuProperty.expandedClass.name());
        itemWrapTag      = helper.getTagAttributeString(MenuProperty.itemWrapTag.name());
        defaultItemClass = helper.getTagAttributeString(MenuProperty.defaultItemClass.name());

        // find parent
        if (parentMenu==null)
            parentMenu = findParentMenu();
        if (parentMenu==null)
        {   // the root menu
            if (currentClass==null)
                currentClass = "current";  
            if (parentClass==null)
                parentClass = "parent";
            if (disabledClass==null)
                disabledClass = "parent";
            if (expandedClass==null)
                expandedClass = "expanded";
            // level
            level = 0;
        }
        else
        {   // copy from parent
            if (currentId==null)
                currentId = parentMenu.getCurrentId();
            if (currentClass==null)
                currentClass = parentMenu.getCurrentClass();  
            if (parentClass==null)
                parentClass = parentMenu.getParentClass();
            if (disabledClass==null)
                disabledClass = parentMenu.getDisabledClass();
            if (expandedClass==null)
                expandedClass = parentMenu.getExpandedClass();
            if (itemWrapTag==null)
                itemWrapTag = parentMenu.getItemWrapTag();
            if (defaultItemClass==null)
                defaultItemClass = parentMenu.defaultItemClass;
            // increase level
            level = parentMenu.level + 1;
        }
    }
    
    public String getCurrentId()
    {
        if (currentId==null)
            currentId= StringUtils.toString(getStateHelper().get(MenuProperty.currentId));
        return currentId;
    }

    public String getCurrentClass()
    {
        if (currentClass==null)
            currentClass= StringUtils.toString(getStateHelper().get(MenuProperty.currentClass));
        return currentClass;
    }

    public String getDisabledClass()
    {
        if (disabledClass==null)
            disabledClass= StringUtils.toString(getStateHelper().get(MenuProperty.disabledClass));
        return disabledClass;
    }

    public String getParentClass()
    {
        if (parentClass==null)
            parentClass= StringUtils.toString(getStateHelper().get(MenuProperty.parentClass));
        return parentClass;
    }

    public String getExpandedClass()
    {
        if (expandedClass==null)
            expandedClass= StringUtils.toString(getStateHelper().get(MenuProperty.expandedClass));
        return expandedClass;
    }

    public String getItemWrapTag()
    {
        if (itemWrapTag==null)
            itemWrapTag= StringUtils.toString(getStateHelper().get(MenuProperty.itemWrapTag));
        return itemWrapTag;
    }
    
    public int getLevel()
    {
        return level;
    }
    
    public String getMenuStyleClass()
    {
        String styleClass = helper.getTagAttributeString(InputControl.CSS_STYLE_CLASS);
        if (styleClass!=null && styleClass.indexOf("{}")>0)
        {   // add level to style class
            styleClass = StringUtils.replace(styleClass, "{}", String.valueOf(level));
        }
        return styleClass;
    }

    public String getItemStyleClass()
    {
        if (defaultItemClass!=null && defaultItemClass.indexOf("{}")>0)
            return StringUtils.replace(defaultItemClass, "{}", String.valueOf(level));
        // return default
        return defaultItemClass;
    }
    
    /*
    public Boolean isAutoItemId()
    {
        if (this.autoItemId == null) {
            this.autoItemId = (Boolean)getStateHelper().get(Properties.autoItemId);
            if (this.autoItemId==null) {
                if (parentMenu==null)
                    parentMenu = findParentMenu();
                if (parentMenu!=null)
                    return parentMenu.isAutoItemId();
            }
        }
        return this.autoItemId;
    }
    */

    /* setters */
    
    public void setCurrentId(String currentId)
    {
        this.currentId = currentId;
        // save
        getStateHelper().put(MenuProperty.currentId, currentId);
    }

    public void setCurrentClass(String currentClass)
    {
        this.currentClass = currentClass;
        // save
        getStateHelper().put(MenuProperty.currentClass, currentClass);
    }

    public void setDisabledClass(String disabledClass)
    {
        this.disabledClass = disabledClass;
        // save
        getStateHelper().put(MenuProperty.disabledClass, disabledClass);
    }

    public void setParentClass(String parentClass)
    {
        this.parentClass = parentClass;
        // save
        getStateHelper().put(MenuProperty.parentClass, parentClass);
    }

    public void setExpandedClass(String expandedClass)
    {
        this.expandedClass = expandedClass;
        // save
        getStateHelper().put(MenuProperty.expandedClass, expandedClass);
    }

    public void setItemWrapTag(String itemWrapTag)
    {
        this.itemWrapTag = itemWrapTag;
        // save
        getStateHelper().put(MenuProperty.itemWrapTag, itemWrapTag);
    }

    /*
    public void setAutoItemId(Boolean autoItemId)
    {
        this.autoItemId = autoItemId;
        // save
        getStateHelper().put(Properties.autoItemId, this.autoItemId);
    }
    */

    /*
     * helpers
     */

    protected MenuListTag findParentMenu()
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
    
}
