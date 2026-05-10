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

import javax.faces.component.UIComponent;
import javax.faces.component.UIOutput;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.apache.empire.commons.StringUtils;
import org.apache.empire.jsf2.controls.InputControl;
import org.apache.empire.jsf2.utils.TagEncodingHelper;
import org.apache.empire.jsf2.utils.TagEncodingHelperFactory;
import org.apache.empire.jsf2.utils.TagEncodingHelperFactory.TagEncodingHolder;
import org.apache.empire.jsf2.utils.TagStyleClass;

public class MenuListTag extends UIOutput implements TagEncodingHolder
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
    }
    
    protected MenuListTag parentMenu = null;
    protected String currentId = null; 
    protected String currentClass = null; 
    protected String parentClass = null;
    protected String disabledClass = null; 
    protected String expandedClass = null;
    protected String itemWrapTag = null;
    protected String defaultItemClass = null; // e.g. "level{}"
    protected int level = 0;
    
    private transient Boolean cachedIsRendered;
    
    @Override
    public String getFamily()
    {
        return TagEncodingHelper.COMPONENT_FAMILY;
    }

    @Override
    public TagEncodingHelper getEncodingHelper()
    {
        return helper;
    }
    
    @Override
    public boolean isRendered()
    {
        // call base for default
        if (!super.isRendered())
            return false;
        // detect if not already detected
        if (cachedIsRendered== null)
            cachedIsRendered = isRenderList();
        // render
        return cachedIsRendered;
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
    
    public Object getAttribute(String attributeName)
    {
        return helper.getTagAttributeValue(attributeName);
    }
    
    public final MenuListTag getParentMenu()
    {
        if (parentMenu==null)
            parentMenu = findParentMenu();
        return parentMenu;
    }
    
    public String getCurrentItemId()
    {
        return currentId;
    }

    public String getCurrentItemClass()
    {
        return currentClass;
    }

    public String getParentItemClass()
    {
        return parentClass;
    }

    public String getItemDisabledClass()
    {
        return disabledClass;
    }

    public String getItemExpandedClass()
    {
        return expandedClass;
    }

    public String getItemWrapperTagName()
    {
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
     * helpers
     */
    
    /**
     * Returns whether or not the MenuList should be rendered
     * Override this to implement custom logic
     * @return true if the menu item should be rendered or false otherwise
     */
    protected boolean isRenderList()
    {
        /*
         * Example implementation for derived classes
         * 
        // check whether to render the menu list
        FacesContext fc = FacesContext.getCurrentInstance();
        UIViewRoot vr = (fc!=null ? fc.getViewRoot() : null);
        Map<String, Object> vm = (vr!=null ? vr.getViewMap(false) : null);
        Page page = (Page) (vm!=null ?  vm.get("page") : null);
        if (page!=null && !page.isRenderMenuList(this))
            return false;
         */    
        // yes, render
        return true;
    }
    
    protected void initMenuAttributes()
    {        
        currentId        = helper.getTagAttributeString(MenuProperty.currentId.name()); 
        currentClass     = helper.getTagAttributeString(MenuProperty.currentClass.name()); 
        parentClass      = helper.getTagAttributeString(MenuProperty.parentClass.name());
        disabledClass    = helper.getTagAttributeString(MenuProperty.disabledClass.name()); 
        expandedClass    = helper.getTagAttributeString(MenuProperty.expandedClass.name());
        itemWrapTag      = helper.getTagAttributeString(MenuProperty.itemWrapTag.name());
        defaultItemClass = helper.getTagAttributeString(MenuProperty.defaultItemClass.name());

        // find parent
        if (getParentMenu()==null)
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
                currentId = parentMenu.getCurrentItemId();
            if (currentClass==null)
                currentClass = parentMenu.getCurrentItemClass();  
            if (parentClass==null)
                parentClass = parentMenu.getParentItemClass();
            if (disabledClass==null)
                disabledClass = parentMenu.getItemDisabledClass();
            if (expandedClass==null)
                expandedClass = parentMenu.getItemExpandedClass();
            if (itemWrapTag==null)
                itemWrapTag = parentMenu.getItemWrapperTagName();
            if (defaultItemClass==null)
                defaultItemClass = parentMenu.defaultItemClass;
            // increase level
            level = parentMenu.level + 1;
        }
    }

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
