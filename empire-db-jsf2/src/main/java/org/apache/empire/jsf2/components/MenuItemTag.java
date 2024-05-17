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
import org.apache.empire.jsf2.utils.TagEncodingHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MenuItemTag extends LinkTag
{
    // Logger
    private static final Logger log = LoggerFactory.getLogger(MenuItemTag.class);
    
    protected MenuListTag parentMenu;
    protected String menuId;

    public MenuItemTag()
    {
        super();
    }

    @Override
    public String getFamily()
    {
        return UINamingContainer.COMPONENT_FAMILY; 
    }
    
    /* 
     * Auto-generate Menu item component id
     * Works, but is too inflexible
     * 
    @Override
    public void setParent(UIComponent parent)
    {
        super.setParent(parent);
        // check
        if (parent instanceof UIPanel)
            parent = parent.getParent();
        if (parent instanceof MenuListTag) {
            parentMenu = ((MenuListTag)parent);
            if (!helper.hasComponentId() && Boolean.TRUE==parentMenu.isAutoItemId()) {
                // setAutoComponentId
                menuId = helper.getTagAttributeString("menuId");
                String compId = TagEncodingHelper.buildComponentId(menuId);
                if (compId!=null) {
                    super.setId(compId);
                    log.debug("Auto-Setting compontent id for menu-item \"{}\" to {}", menuId, compId);
                }
            }
        }
    }
    */
    
    @Override
    public void setId(String id)
    {
        if (id.endsWith("@")) {
            // Generate MenuItem component id from menuId
            menuId = helper.getTagAttributeString("menuId");
            if (StringUtils.isNotEmpty(menuId)) {
                int idx = id.indexOf('@');
                String ident = (idx>0) ? id.substring(0,idx)+menuId : menuId;
                id = TagEncodingHelper.buildComponentId(ident);
            }
        }
        super.setId(id);
    }
    
    @Override
    public void encodeBegin(FacesContext context)
        throws IOException
    {
        // Detect Parent Menu
        parentMenu = findParentMenu();
        if (menuId==null)
            menuId = helper.getTagAttributeString("menuId");         
        
        if(!isRendered())
            return;
        
        // render components
        ResponseWriter writer = context.getResponseWriter();
        writer.startElement("li", this);
        
        //Compoent-ID
        helper.writeComponentId(writer);
        
        // Style Class
        helper.writeAttribute(writer, "class", getStyleClass());

        // wrap
        String wrap = (parentMenu!=null ? parentMenu.getItemWrapTag() : null);
        if (StringUtils.isNotEmpty(wrap))
        {   // Wrap-Element
            writer.startElement(wrap, this);
            // writer.writeAttribute("class", "item", null);
        }
        
        // begin
        super.encodeBegin(context);
        
        // End Wrapper
        if (StringUtils.isNotEmpty(wrap))
            writer.endElement(wrap);
        
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
            super.forceEncodeChildren(context);
        }
    }

    @Override
    public void encodeEnd(FacesContext context)
        throws IOException
    {
        if(!isRendered())
            return;
        // call base
        if (isExpanded())
        {
            super.encodeEnd(context);
        }
        // EndElement
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
    
    protected boolean isCurrent()
    {
        if (menuId==null || parentMenu==null || parentMenu.getCurrentId()==null)
            return false;
        // All present
        return menuId.equals(parentMenu.getCurrentId());
    }
    
    protected boolean isParent()
    {
        if (menuId==null || parentMenu==null || parentMenu.getCurrentId()==null)
            return false;
        // All present
        String  currentId = parentMenu.getCurrentId();
        return (currentId.length()>menuId.length() && currentId.startsWith(menuId));
    }

    protected boolean isDisabled()
    {
        Object value = helper.getTagAttributeValue("disabled");
        if (value!=null)
            return ObjectUtils.getBoolean(value);
        return false;
    }

    protected boolean isExpanded()
    {
        Object value = helper.getTagAttributeValue("expanded");
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
        Object value = helper.getTagAttributeValue("currentOnly");
        boolean currentOnly = false;
        if (value!=null)
            currentOnly = ObjectUtils.getBoolean(value);
        
        // Check parent
        if (currentOnly && menuId!=null && parentMenu!=null && parentMenu.getCurrentId()!=null)
        {    
            return isCurrent() || isParent();
        }
        
        return super.isRendered();
    }
    
    protected String getStyleClass()
    {
        String styleClass = helper.getTagAttributeString("styleClass");
        if (parentMenu!=null)
        {
            // Style Class
            if (StringUtils.isEmpty(styleClass))
                styleClass = parentMenu.getItemStyleClass();
            // Menu Class
            if (isCurrent())
                styleClass = appendStyleClass(styleClass, parentMenu.getCurrentClass());
            else if (isParent())
                styleClass = appendStyleClass(styleClass, parentMenu.getParentClass());
            // expanded
            if (isExpanded())
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
    
    protected String appendStyleClass(String styleClass, String newClass)
    {
        if (StringUtils.isEmpty(newClass))
            return styleClass;
        return (styleClass==null) ? newClass : styleClass+" "+newClass;
    }
    
    @Override
    protected boolean isEncodeLinkChildren(Object linkValue)
    {
        return false;
    }
}
