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

import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.jsf2.controls.InputControl;
import org.apache.empire.jsf2.utils.ControlRenderInfo;
import org.apache.empire.jsf2.utils.TagEncodingHelper;
import org.apache.empire.jsf2.utils.TagEncodingHelperFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FormGridTag extends UIOutput implements NamingContainer
{
    // Logger
    private static final Logger log = LoggerFactory.getLogger(FormGridTag.class);
    
    /*
     * FormGridMode
     */
    private enum FormGridMode 
    {
        LEGACY(InputControl.HTML_TAG_DIV, null, InputControl.HTML_TAG_TD, InputControl.HTML_TAG_TD),
        TABLE (InputControl.HTML_TAG_TABLE, InputControl.HTML_TAG_TR, InputControl.HTML_TAG_TD, InputControl.HTML_TAG_TD),
        GRID  (InputControl.HTML_TAG_DIV, InputControl.HTML_TAG_DIV, InputControl.HTML_TAG_DIV, InputControl.HTML_TAG_DIV);
        
        public final String GRID_TAG;
        public final String DEFAULT_CONTROL_TAG;
        public final String DEFAULT_LABEL_TAG;
        public final String DEFAULT_INPUT_TAG;
        
        private FormGridMode(String gridTag, String controlTag, String labelTag, String inputTag)
        {
            this.GRID_TAG = gridTag;
            this.DEFAULT_CONTROL_TAG = controlTag;
            this.DEFAULT_LABEL_TAG = labelTag;
            this.DEFAULT_INPUT_TAG = inputTag;
        }
        
        public static FormGridMode detect(String mode)
        {
            if (mode==null || mode.length()==0)
                return LEGACY;
            // find
            FormGridMode[] values = values();
            for (int i=0; i<values.length; i++)
            {
                if (values[i].name().equalsIgnoreCase(mode))
                    return values[i]; 
            }
            // not found
            log.warn("FormGridMode \"{}\" not found. Using default!", mode);
            return LEGACY;
        }
        
        @Override
        public String toString()
        {
            return name();
        }
    }
    
    protected final TagEncodingHelper helper = TagEncodingHelperFactory.create(this, "eFormGrid");
    
    protected ControlRenderInfo controlRenderInfo = null;
    
    private String tagName;

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
        
        // tagName
        FormGridMode mode = FormGridMode.detect(helper.getTagAttributeString("mode")); 
        this.tagName = helper.getTagAttributeString("tag", mode.GRID_TAG);
        
        // render components
        ResponseWriter writer = context.getResponseWriter();
        writer.startElement(tagName, this);
        // id
        helper.writeComponentId(writer, getControlRenderInfo().RENDER_AUTO_ID);
        // style class
        helper.writeStyleClass(writer);
        helper.writeAttribute(writer, InputControl.HTML_ATTR_STYLE, helper.getTagAttributeString("style"));
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
        writer.endElement(this.tagName);
    }
    
    public ControlRenderInfo getControlRenderInfo()
    {
        // already set?
        if (controlRenderInfo!=null)
            return controlRenderInfo;
        // check mode
        FormGridMode mode = FormGridMode.detect(helper.getTagAttributeString("mode")); 
        // override?
        String controlTag = helper.getTagAttributeString("controlTag", mode.DEFAULT_CONTROL_TAG);
        String labelTag   = helper.getTagAttributeString("labelTag",   mode.DEFAULT_LABEL_TAG);
        String inputTag   = helper.getTagAttributeString("inputTag",   mode.DEFAULT_INPUT_TAG);
        boolean renderAutoId = ObjectUtils.getBoolean(helper.getTagAttributeString("renderAutoId"));
        // done
        this.controlRenderInfo = new ControlRenderInfo(controlTag, labelTag, inputTag, renderAutoId);
        return controlRenderInfo;
    }
}
