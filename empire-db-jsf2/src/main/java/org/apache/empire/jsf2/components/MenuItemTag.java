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
import javax.faces.component.html.HtmlOutcomeTargetLink;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.commons.StringUtils;
import org.apache.empire.jsf2.controls.InputControl;
import org.apache.empire.jsf2.pages.PageDefinition;
import org.apache.empire.jsf2.utils.TagEncodingHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MenuItemTag extends LinkTag
{
    // Logger
    private static final Logger log = LoggerFactory.getLogger(MenuItemTag.class);
    
    protected MenuListTag parentMenu;
    protected String menuId;
    
    private transient Boolean cachedIsRendered;
    private transient Boolean cachedIsExpanded;

    public MenuItemTag()
    {
        super();
    }

    @Override
    public String getFamily()
    {
        return TagEncodingHelper.COMPONENT_FAMILY; 
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
    public boolean isRendered()
    {
        // call base for default
        if (!super.isRendered())
            return false;
        // detect if not already detected
        if (cachedIsRendered== null)
            cachedIsRendered = isRenderItem();
        // render
        return cachedIsRendered;
    }
    
    public final String getMenuItemId()
    {
        if (menuId==null)
            menuId = helper.getTagAttributeString("menuId");
        return menuId;
    }
    
    public final MenuListTag getParentMenu()
    {
        if (parentMenu==null)
            parentMenu = findParentMenu();
        return parentMenu;
    }
    
    public final Object getAttribute(String attributeName)
    {
        return helper.getTagAttributeValue(attributeName);
    }
    
    public final PageDefinition getPageDefinition()
    {
        // check page
        Object pageDefinition = helper.getTagAttributeValue("page");
        if (pageDefinition instanceof PageDefinition)
            return (PageDefinition)pageDefinition;
        else if (pageDefinition!=null)
            log.warn("MenuItem attribute \"page\" is not a PageDefinition but a {}", pageDefinition.getClass().getName());
        // not provided
        return null;
    }
    
    @Override
    public void encodeBegin(FacesContext context)
        throws IOException
    {
        // Detect Parent Menu
        if (parentMenu==null)
            getParentMenu();
        if (menuId==null)
            getMenuItemId();         
        
        // render components
        ResponseWriter writer = context.getResponseWriter();
        writer.startElement("li", this);
        
        //Compoent-ID
        helper.writeComponentId(writer);
        
        // Style Class
        helper.writeAttribute(writer, "class", getStyleClass());

        // wrap
        String wrap = (parentMenu!=null ? parentMenu.getItemWrapperTagName() : null);
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
        if (isItemExpanded())
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
        if (isItemExpanded())
        {
            super.encodeEnd(context);
        }
        // EndElement
        ResponseWriter writer = context.getResponseWriter();
        writer.endElement("li");
    }

    @Override
    protected String getLinkStyleClass()
    {
        Object linkStyle = getAttributes().get("linkStyle");
        return (linkStyle!=null ? linkStyle.toString() : null);
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
    
    public boolean isCurrent()
    {
        if (getMenuItemId()==null || getParentMenu()==null || parentMenu.getCurrentItemId()==null)
            return false;
        // All present
        return menuId.equals(parentMenu.getCurrentItemId());
    }
    
    public boolean isParent()
    {
        if (getMenuItemId()==null || getParentMenu()==null || parentMenu.getCurrentItemId()==null)
            return false;
        // All present
        String  currentId = parentMenu.getCurrentItemId();
        return (currentId.length()>menuId.length() && currentId.startsWith(menuId));
    }

    public boolean isItemDisabled()
    {
        Object value = helper.getTagAttributeValue("disabled");
        if (value!=null)
            return ObjectUtils.getBoolean(value);
        return false;
    }
    
    public final boolean isItemExpanded()
    {
        if (cachedIsExpanded==null)
            cachedIsExpanded=detectItemExpanded();
        return cachedIsExpanded;
    }
    
    /**
     * Returns whether or not the MenuItem should be rendered
     * Override this to implement custom logic
     * @return true if the menu item should be rendered or false otherwise
     */
    protected boolean isRenderItem()
    {
        /*
         * Example implementation for derived classes
         * 
        // check page
        Object pageDefinition = helper.getTagAttributeValue("page");
        if (pageDefinition instanceof PageDefinition)
        {   // check whether to render the item
            FacesContext fc = FacesContext.getCurrentInstance();
            UIViewRoot vr = (fc!=null ? fc.getViewRoot() : null);
            Map<String, Object> vm = (vr!=null ? vr.getViewMap(false) : null);
            Page page = (Page) (vm!=null ?  vm.get("page") : null);
            if (page!=null && !page.isRenderMenuItem(this))
                return false;
                
        }
         */
        // check currentOnly
        Object currentOnly = helper.getTagAttributeValue("currentOnly");
        if (currentOnly!=null && ObjectUtils.getBoolean(currentOnly))
        {   // Check current and parent
            return isCurrent() || isParent();
        }
        // yes, render
        return true;
    }

    protected boolean detectItemExpanded()
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
        if (getMenuItemId()==null || getParentMenu()==null || parentMenu.getCurrentItemId()==null)
            return auto;
        // All present
        String currentId = parentMenu.getCurrentItemId();
        return currentId.startsWith(menuId+".");
    }
    
    protected String getStyleClass()
    {
        String styleClass = helper.getTagAttributeString(InputControl.CSS_STYLE_CLASS);
        if (parentMenu!=null)
        {
            // Style Class
            if (StringUtils.isEmpty(styleClass))
                styleClass = parentMenu.getItemStyleClass();
            // Menu Class
            if (isCurrent())
                styleClass = appendStyleClass(styleClass, parentMenu.getCurrentItemClass());
            else if (isParent())
                styleClass = appendStyleClass(styleClass, parentMenu.getParentItemClass());
            // expanded
            if (isItemExpanded())
                styleClass = appendStyleClass(styleClass, parentMenu.getItemExpandedClass());            
            // Disabled / enabled
            if (isItemDisabled())
                styleClass = appendStyleClass(styleClass, parentMenu.getItemDisabledClass());
        }
        else
        {   // disabled
            if (isItemDisabled())
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
    
    /**
     * Returns the menu item label
     * Override this to e.g. provide the label from the PageDefinition
     * @return the menu item label
     */
    @Override
    protected Object getLinkValue(boolean hasColumn)
    {
        Object label = super.getLinkValue(hasColumn);
        /*
         * Example implementation for derived classes
         * 
        if (label==null && getFacet("label")==null)
        {   // Set title from PageDefinition
            PageDefinition pageDef = getPageDefinition();
            label = helper.getTextResolver(FacesContext.getCurrentInstance()).resolveKey("pageTitle."+pageDef.getPageBeanName());
        }
         */
        return label; 
    }
    
    /* 
     * Supports a "label" facet" e.g.
     * <e:mitem menuId="..." page="..."><f:facet name="label"><span class="icon"/><label>...</label></f:facet></e:mitem>
     */ 
    
    @Override
    protected void encodeLinkComponent(FacesContext context, HtmlOutcomeTargetLink linkComponent)
        throws IOException
    {
        UIComponent labelFacet = this.getFacet("label");
        if (labelFacet!=null)
        {   // custom rendering
            linkComponent.encodeBegin(context);
            labelFacet.encodeAll(context);
            linkComponent.encodeEnd(context);
        }
        else
        {   // default
            linkComponent.encodeAll(context);
        }
    }
    
}
