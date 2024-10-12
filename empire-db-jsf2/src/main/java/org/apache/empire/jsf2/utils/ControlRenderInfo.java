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
package org.apache.empire.jsf2.utils;

import java.io.IOException;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.commons.StringUtils;
import org.apache.empire.jsf2.components.ControlTag;
import org.apache.empire.jsf2.controls.InputControl;

/**
 * ControlRenderInfo
 */
public class ControlRenderInfo
{
    /*
     * Use setDefault(ControlRenderInfo renderInfo) in order to change the default
     */
    private static ControlRenderInfo DEFAULT_CONTROL_RENDER_INFO = new DefaultControlRenderInfo();
    
    public static final String PLACEHOLDER_ATTRIBUTE = "placeholder";

    public static ControlRenderInfo getDefault()
    {
        return DEFAULT_CONTROL_RENDER_INFO;
    }
    
    public static void setDefault(ControlRenderInfo renderInfo)
    {
        DEFAULT_CONTROL_RENDER_INFO = renderInfo;
    }
    
    public static boolean isRenderPlaceholder(UIComponent component)
    {
        return ObjectUtils.getBoolean(component.getAttributes().get(PLACEHOLDER_ATTRIBUTE), false);        
    }
    
    private static class DefaultControlRenderInfo extends ControlRenderInfo
    {
        public DefaultControlRenderInfo()
        {
            super(null, InputControl.HTML_TAG_TD, InputControl.HTML_TAG_TD, null);
        }
        
        @Override
        public void renderPlaceholder(FacesContext context, ControlTag controlTag)
            throws IOException
        {
            // check attribute "placeholder"
            if (isRenderPlaceholder(controlTag))
            {   // render placeholder for invisible controls 
                ResponseWriter writer = context.getResponseWriter();
                writer.startElement(InputControl.HTML_TAG_TD, controlTag);
                writer.writeAttribute(InputControl.HTML_ATTR_CLASS, TagStyleClass.CONTROL_PLACEHOLDER.get(), null);
                writer.writeAttribute("colspan", 2, null);
                writer.endElement(InputControl.HTML_TAG_TD);
            }
        }
    }
    
    public final String CONTROL_TAG;
    public final String LABEL_WRAPPER_TAG;
    public final String INPUT_WRAPPER_TAG;
    public final Character AUTO_CONTROL_ID;
    
    public ControlRenderInfo(String controlTag, String labelTag, String inputTag, Character autoControlId)
    {
        this.CONTROL_TAG = StringUtils.nullIfEmpty(controlTag);
        this.LABEL_WRAPPER_TAG = StringUtils.nullIfEmpty(labelTag);
        this.INPUT_WRAPPER_TAG = StringUtils.nullIfEmpty(inputTag);
        this.AUTO_CONTROL_ID = autoControlId;
    }
    
    @SuppressWarnings("unused")
    public void renderPlaceholder(FacesContext context, ControlTag controlTag)
        throws IOException
    {
        /* add code to render invisible controls */
    }
}
