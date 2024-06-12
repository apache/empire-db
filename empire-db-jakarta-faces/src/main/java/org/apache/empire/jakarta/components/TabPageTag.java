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

import jakarta.faces.component.NamingContainer;
import jakarta.faces.component.UIComponent;
import jakarta.faces.component.UINamingContainer;
import jakarta.faces.component.UIOutput;
import jakarta.faces.context.FacesContext;
import jakarta.faces.context.ResponseWriter;

import org.apache.empire.jakarta.components.TabViewTag.TabViewMode;
import org.apache.empire.jakarta.controls.InputControl;
import org.apache.empire.jakarta.utils.TagEncodingHelper;
import org.apache.empire.jakarta.utils.TagEncodingHelperFactory;
import org.apache.empire.jakarta.utils.TagStyleClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TabPageTag extends UIOutput implements NamingContainer
{
    // Logger
    private static final Logger log = LoggerFactory.getLogger(TabPageTag.class);
    
    protected final TagEncodingHelper helper = TagEncodingHelperFactory.create(this, TagStyleClass.TAB_PAGE.get());

    private TabViewMode mode;
    
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
