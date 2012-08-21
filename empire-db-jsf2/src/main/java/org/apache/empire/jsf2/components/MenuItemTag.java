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
import javax.faces.component.UINamingContainer;
import javax.faces.component.html.HtmlOutcomeTargetLink;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.commons.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MenuItemTag extends LinkTag
{
    // Logger
    private static final Logger log = LoggerFactory.getLogger(MenuItemTag.class);
    
    private MenuListTag parentMenu = null;
    private String menuId;

    private static int itemIdSeq = 0;
    private final int itemId;
    
    public MenuItemTag()
    {
        super();
        // Debug stuff
        itemId = ++itemIdSeq;
        if (log.isDebugEnabled())
            log.debug("MenuId {} created", itemId);
    }

    @Override
    public String getFamily()
    {
        return UINamingContainer.COMPONENT_FAMILY; 
    }

    @Override
    public void encodeBegin(FacesContext context)
        throws IOException
    {
        // Detect Parent Menu
        parentMenu = getParentMenu();
        menuId = StringUtils.toString(getAttributes().get("menuId"));
        if(!isRendered())
            return;
        
        // render components
        ResponseWriter writer = context.getResponseWriter();
        writer.startElement("li", this);
        writer.writeAttribute("id", getClientId(context), null);
        writer.writeAttribute("class", getStyleClass(), null);
        // writer.writeAttribute("item", String.valueOf(itemId), null);

        // begin
        super.encodeBegin(context);
    }
    
    @Override
    public boolean getRendersChildren()
    {
        return true;
    }
    
    @Override
    public void encodeChildren(FacesContext context)
        throws IOException
    {
        if (isExpanded())
        {
            UIComponent c = getChildren().get(0);
            if (c instanceof HtmlOutcomeTargetLink)
            {   if (c.isRendered())
                {   log.warn("WARN: Unexpected rendering of output link. Rendering is ignored.");
                    c.setRendered(false);
                }
            }
            else
                log.warn("WARN: Unexpected child element as first child of MenuItemTag!");
            // encode children
            super.encodeChildren(context);
        }
    }

    @Override
    public void encodeEnd(FacesContext context)
        throws IOException
    {
        if(!isRendered())
            return;
        // call base
        super.encodeEnd(context);
        // end of list item
        ResponseWriter writer = context.getResponseWriter();
        writer.endElement("li");
    }
    
    /*
    private void printChildTree(UIComponent comp, int level)
    {
        List<UIComponent> cl = comp.getChildren();
        for (UIComponent c : cl)
        {
            boolean isRendered = c.isRendered();
            log.info("-{}- rendering {} "+String.valueOf(isRendered), level, c.getClass().getSimpleName());
            printChildTree(c, level+1);
        }
    }
    */

    @Override
    protected String getLinkStyleClass()
    {
        return null;
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
    
    private boolean isCurrent()
    {
        if (menuId==null || parentMenu==null || parentMenu.getCurrentId()==null)
            return false;
        // All present
        return menuId.equals(parentMenu.getCurrentId());
    }

    private boolean isDisabled()
    {
        Object value = getAttributes().get("disabled");
        if (value!=null)
            return ObjectUtils.getBoolean(value);
        return false;
    }

    private boolean isExpanded()
    {
        Object value = getAttributes().get("expanded");
        boolean auto = false;
        if (value!=null)
        {   // is current?
            auto = "auto".equals(value);
            if (auto==false)
                return ObjectUtils.getBoolean(value);
            // check current
            if (isCurrent())
                return true;
        }
        // Check parent
        if (menuId==null || parentMenu==null || parentMenu.getCurrentId()==null)
            return auto;
        // All present
        String currentId = parentMenu.getCurrentId();
        return currentId.startsWith(menuId+".");
    }
    
    @Override
    public boolean isRendered()
    {
        Object value = getAttributes().get("currentOnly");
        boolean currentOnly = false;
        if(value!=null)
            currentOnly = ObjectUtils.getBoolean(value);
        
        // Check parent
        if (currentOnly && menuId!=null && parentMenu!=null && parentMenu.getCurrentId()!=null)
        {    
            return isCurrent();
        }
        
        return super.isRendered();
    }
    
    private String getStyleClass()
    {
        String styleClass = StringUtils.toString(getAttributes().get("styleClass"));
        if (parentMenu!=null)
        {
            // Style Class
            if (StringUtils.isEmpty(styleClass))
                styleClass = parentMenu.getItemStyleClass();
            // Menu Class
            if (isCurrent())
                styleClass = appendStyleClass(styleClass, parentMenu.getCurrentClass());
            else if (isExpanded())
                styleClass = appendStyleClass(styleClass, parentMenu.getExpandedClass());
            // Disabled / enabled
            if (isDisabled())
                styleClass = appendStyleClass(styleClass, parentMenu.getDisabledClass());
        }
        else
        {   // disabled
            if (isDisabled())
                styleClass = appendStyleClass(styleClass, "disabled");
        }
        // both supplied
        return styleClass;
    }
    
    private String appendStyleClass(String styleClass, String newClass)
    {
        if (StringUtils.isEmpty(newClass))
            return styleClass;
        return (styleClass==null) ? newClass : styleClass+" "+newClass;
    }
    
}
