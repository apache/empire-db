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
import javax.faces.component.UINamingContainer;
import javax.faces.component.UIOutput;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.apache.empire.jsf2.controls.InputControl;
import org.apache.empire.jsf2.utils.TagEncodingHelper;
import org.apache.empire.jsf2.utils.TagEncodingHelperFactory;

public class TabPageTag extends UIOutput implements NamingContainer
{
    // Logger
    // private static final Logger log = LoggerFactory.getLogger(MenuTag.class);
    
    protected final TagEncodingHelper helper = TagEncodingHelperFactory.create(this, "eTabPage");

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
        
        // render components
        ResponseWriter writer = context.getResponseWriter();
        writer.startElement(InputControl.HTML_TAG_TR, this);
        writer.writeAttribute(InputControl.HTML_ATTR_ID, getClientId(), null);
        helper.writeAttribute(writer, InputControl.HTML_ATTR_CLASS, helper.getTagAttributeString("styleClass"));
        helper.writeAttribute(writer, InputControl.HTML_ATTR_STYLE, helper.getTagAttributeString("style"));
        // TabPage
        writer.startElement(InputControl.HTML_TAG_TD, this);
        writer.writeAttribute(InputControl.HTML_ATTR_CLASS, "eTabPage", null);
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
        // close
        ResponseWriter writer = context.getResponseWriter();
        writer.endElement(InputControl.HTML_TAG_TD);
        writer.endElement(InputControl.HTML_TAG_TR);
    }

    public String getTabLabel()
    {
        return helper.getTagAttributeString("title");
    }
}
