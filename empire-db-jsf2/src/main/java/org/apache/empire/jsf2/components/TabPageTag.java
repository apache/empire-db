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
import javax.faces.component.UIPanel;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.apache.empire.commons.StringUtils;
import org.apache.empire.jsf2.components.TabViewTag.TabViewMode;
import org.apache.empire.jsf2.controls.InputControl;
import org.apache.empire.jsf2.utils.TagEncodingHelper;
import org.apache.empire.jsf2.utils.TagEncodingHelperFactory;
import org.apache.empire.jsf2.utils.TagStyleClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TabPageTag extends UIOutput implements NamingContainer
{
    // Logger
    private static final Logger log = LoggerFactory.getLogger(TabPageTag.class);
    
    public static final String  LABEL_FACET_NAME = "label";
    
    protected final TagEncodingHelper helper = TagEncodingHelperFactory.create(this, TagStyleClass.TAB_PAGE.get());

    private TabViewMode mode;
    
    @Override
    public String getFamily()
    {
        return UINamingContainer.COMPONENT_FAMILY; 
    }
    
    @Override
    public void setParent(UIComponent parent)
    {
        super.setParent(parent);
        // TabViewTag
        UIComponent tabView = parent;
        if (parent instanceof UIPanel)
            tabView = parent.getParent();
        if ((tabView instanceof TabViewTag) && !helper.hasComponentId())
        {   // We're inside a tabView
            String tabViewId = tabView.getId();
            if (tabViewId.startsWith(TagEncodingHelper.FACES_ID_PREFIX))
                tabViewId="tab";
            // Set tabId
            String tabId = StringUtils.concat(tabViewId, "_", String.valueOf(parent.getChildCount()-1));
            super.setId(tabId);
        }
    }
        
    @Override
    public void encodeBegin(FacesContext context)
        throws IOException
    {
        // call base
        super.encodeBegin(context);
        
        // TabViewMode 
        this.mode = detectTabViewMode();
        
        // render components
        ResponseWriter writer = context.getResponseWriter();
        if (mode.PAGE_WRAP_TAG!=null)
        {   // render page wrap tag
            writer.startElement(mode.PAGE_WRAP_TAG, this);
            writer.writeAttribute(InputControl.HTML_ATTR_ID, getClientId(), null);
        }
        // TabPage
        writer.startElement(mode.PAGE_TAG, this);
        if (mode.PAGE_WRAP_TAG==null)
        {   // no wrapper tag
            writer.writeAttribute(InputControl.HTML_ATTR_ID, getClientId(), null);
        }
        writer.writeAttribute(InputControl.HTML_ATTR_CLASS, this.helper.getSimpleStyleClass(), null);
    }

    @Override
    public boolean getRendersChildren()
    {
        return super.getRendersChildren();
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
        // close tags
        ResponseWriter writer = context.getResponseWriter();
        writer.endElement(mode.PAGE_TAG);
        // wrapper tag?
        if (mode.PAGE_WRAP_TAG!=null)
            writer.endElement(mode.PAGE_WRAP_TAG);
    }

    public String getTabLabel()
    {
        /* 
         * for backwards compatibilty use "title" as label when label is null and no facet is defined
         */
        String label = helper.getTagAttributeString("label");
        if (label==null && getFacet(LABEL_FACET_NAME)==null) {
            label = helper.getTagAttributeString("title");
            if (label!=null)
                log.info("TabPage \"{}\": attribute \"title\" was provided instead of \"label\". This is deprecated and may be removed in the future!", label);
        }
        // the label 
        return label;
    }

    public String getTabTitle()
    {
        String label = helper.getTagAttributeString("label");
        if (label==null && getFacet(LABEL_FACET_NAME)==null)
            return null;
        // the title
        return helper.getTagAttributeString("title");
    }
    
    protected TabViewMode detectTabViewMode()
    {
        // walk upwards the parent component tree and return the first record component found (if any)
        UIComponent parent = this;
        while ((parent = parent.getParent()) != null)
        {
            if (parent instanceof TabViewTag)
            {   // found
                return ((TabViewTag) parent).getViewMode();
            }
        }
        log.warn("TabViewTag not found! Unable to detect TabViewMode.");
        return TabViewMode.GRID;
    }
    
}
