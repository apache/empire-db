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
    
    private static boolean renderPlaceholderDefault = false;

    private static boolean renderExtraWrapperStyles = false;
    
    public static ControlRenderInfo getDefault()
    {
        return DEFAULT_CONTROL_RENDER_INFO;
    }
    
    public static void setDefault(ControlRenderInfo renderInfo)
    {
        DEFAULT_CONTROL_RENDER_INFO = renderInfo;
    }

    public static boolean isRenderExtraWrapperStyles()
    {
        return renderExtraWrapperStyles;
    }

    public static void setRenderExtraWrapperStyles(boolean renderExtraWrapperStyles)
    {
        ControlRenderInfo.renderExtraWrapperStyles = renderExtraWrapperStyles;
    }

    public static boolean isRenderPlaceholderDefault()
    {
        return renderPlaceholderDefault;
    }

    public static void setRenderPlaceholderDefault(boolean renderPlaceholderDefault)
    {
        ControlRenderInfo.renderPlaceholderDefault = renderPlaceholderDefault;
    }

    public static boolean isRenderPlaceholder(UIComponent component)
    {
        return ObjectUtils.getBoolean(component.getAttributes().get(PLACEHOLDER_ATTRIBUTE), renderPlaceholderDefault);        
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
    
    public void writeLabelWrapperAttributes(ResponseWriter writer, TagEncodingHelper helper)
        throws IOException
    {
        // style Class
        String extraStyleClass = helper.getControlExtraLabelWrapperStyle();
        helper.writeStyleClass(writer, TagStyleClass.CONTROL_LABEL.get(), extraStyleClass);
    }

    public void writeInputWrapperAttributes(ResponseWriter writer, TagEncodingHelper helper)
        throws IOException
    {
        // wrapper Class
        String wrapperClass = helper.getTagAttributeStringEx("wrapperClass"); 
        if (wrapperClass!=null && wrapperClass.equals("-"))
            wrapperClass= null;
        // extra
        String extraStyleClass = helper.getControlExtraInputWrapperStyle();
        helper.writeStyleClass(writer, TagStyleClass.CONTROL_INPUT.get(), wrapperClass, extraStyleClass);
        // colspan for <td>
        String colSpan = InputControl.HTML_TAG_TD.equalsIgnoreCase(INPUT_WRAPPER_TAG) ? helper.getTagAttributeStringEx("colspan") : null;            
        if (colSpan!=null)
            writer.writeAttribute("colspan", colSpan, null);
    }
    
    @SuppressWarnings("unused")
    public void renderPlaceholder(FacesContext context, ControlTag controlTag)
        throws IOException
    {
        /* add code to render invisible controls */
    }
}
