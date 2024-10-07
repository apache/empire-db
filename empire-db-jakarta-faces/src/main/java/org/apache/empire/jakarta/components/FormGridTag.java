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

import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.commons.StringUtils;
import org.apache.empire.jakarta.controls.InputControl;
import org.apache.empire.jakarta.utils.ControlRenderInfo;
import org.apache.empire.jakarta.utils.TagEncodingHelper;
import org.apache.empire.jakarta.utils.TagEncodingHelperFactory;
import org.apache.empire.jakarta.utils.TagStyleClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.faces.component.UIComponent;
import jakarta.faces.component.UINamingContainer;
import jakarta.faces.component.UIOutput;
import jakarta.faces.context.FacesContext;
import jakarta.faces.context.ResponseWriter;

public class FormGridTag extends UIOutput // implements NamingContainer
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
        GRID  (InputControl.HTML_TAG_DIV, InputControl.HTML_TAG_DIV, InputControl.HTML_TAG_DIV, InputControl.HTML_TAG_DIV),
        FLAT  (InputControl.HTML_TAG_DIV, InputControl.HTML_TAG_DIV, null, null);
        
        public final String GRID_TAG;
        public final String CONTROL_TAG;
        public final String LABEL_TAG;
        public final String INPUT_TAG;
        
        private FormGridMode(String gridTag, String controlTag, String labelTag, String inputTag)
        {
            this.GRID_TAG = gridTag;
            this.CONTROL_TAG = controlTag;
            this.LABEL_TAG = labelTag;
            this.INPUT_TAG = inputTag;
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

    /**
     * FromGridRenderInfo
     * Rendering the form Grid
     */
    protected static class FromGridRenderInfo extends ControlRenderInfo
    {
        private final UIComponent placeholderFacet;
        private final boolean renderPlaceholder;
        
        public FromGridRenderInfo(FormGridMode mode, UIComponent placeholderFacet, boolean renderPlaceholder, Character autoControlId)
        {
            super(mode.CONTROL_TAG, mode.LABEL_TAG, mode.INPUT_TAG, autoControlId);
            // set
            this.placeholderFacet = placeholderFacet;
            this.renderPlaceholder = renderPlaceholder; 
        }
        
        @Override
        public void renderPlaceholder(FacesContext context, ControlTag controlTag)
            throws IOException
        {
            if (placeholderFacet!=null)
            {   // label facet
                placeholderFacet.encodeAll(context);
            }
            else if (renderPlaceholder)
            {   // render placeholder   
                ResponseWriter writer = context.getResponseWriter();
                String placeholderTag = (CONTROL_TAG!=null ? CONTROL_TAG : INPUT_WRAPPER_TAG);
                writer.startElement(placeholderTag, controlTag);
                writer.writeAttribute(InputControl.HTML_ATTR_CLASS, TagStyleClass.CONTROL_PLACEHOLDER.get(), null);
                if (InputControl.HTML_TAG_TD.equalsIgnoreCase(placeholderTag))
                    writer.writeAttribute("colspan", 2, null);
                writer.endElement(placeholderTag);
            }
        }
    }
    
    protected final TagEncodingHelper helper = TagEncodingHelperFactory.create(this, TagStyleClass.FORM_GRID.get());
    
    protected ControlRenderInfo controlRenderInfo = null;
    
    private FormGridMode mode;

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
        this.mode = FormGridMode.detect(helper.getTagAttributeString("mode", FormGridMode.GRID.name())); 
        
        // render components
        ResponseWriter writer = context.getResponseWriter();
        writer.startElement(mode.GRID_TAG, this);
        // id
        helper.writeComponentId(writer);
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
        writer.endElement(mode.GRID_TAG);
    }
    
    public ControlRenderInfo getControlRenderInfo()
    {
        // already set?
        if (controlRenderInfo!=null)
            return controlRenderInfo;
        // check mode
        if (this.mode==null)
            this.mode = FormGridMode.detect(helper.getTagAttributeString("mode", FormGridMode.GRID.name())); 
        // autoControlId
        Character autoControlId = null;
        Object autoId = helper.getTagAttributeString("autoControlId");
        if (autoId!=null)
        {   // check
            String id = autoId.toString();
            if ("true".equalsIgnoreCase(id))
                autoControlId = TagEncodingHelper.PH_COLUMN_SMART;
            else if (id.length()==1 && StringUtils.indexOfAny(id, TagEncodingHelper.ALLOWED_COLUMN_PH)>=0)
                autoControlId = id.charAt(0);
            else if (!"false".equalsIgnoreCase(id))
                log.warn("FormGridTag: Invalid value \"{}\" for attribute \"autoControlId\". Allowed values are *|@|&", id);
        }
        // create control info
        UIComponent placeholderFacet = getFacet("placeholder");
        boolean renderPlaceholder = (placeholderFacet==null ? ObjectUtils.getBoolean(getAttributes().get("placeholder"), false) : false);
        this.controlRenderInfo = new FromGridRenderInfo(mode, placeholderFacet, renderPlaceholder, autoControlId);
        return controlRenderInfo;
    }
}