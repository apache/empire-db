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

import javax.faces.component.UIOutput;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.apache.empire.commons.StringUtils;
import org.apache.empire.jsf2.controls.InputControl;
import org.apache.empire.jsf2.utils.StyleClass;
import org.apache.empire.jsf2.utils.TagEncodingHelper;
import org.apache.empire.jsf2.utils.TagEncodingHelperFactory;
import org.apache.empire.jsf2.utils.TagStyleClass;
import org.apache.empire.jsf2.utils.TagEncodingHelperFactory.TagEncodingHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ValueTag extends UIOutput implements TagEncodingHolder
{

    // Logger
    private static final Logger log = LoggerFactory.getLogger(ValueTag.class);
    
    protected final TagEncodingHelper helper = TagEncodingHelperFactory.create(this, TagStyleClass.VALUE.get());

    public ValueTag()
    {
        log.trace("component value created");
    }

    @Override
    public String getFamily()
    {
        return TagEncodingHelper.COMPONENT_FAMILY;
    }

    @Override
    public TagEncodingHelper getEncodingHelper()
    {
        return helper;
    }

    @Override
    public String getId()
    {
        String compId = super.getId();
        // Mojarra-Patch since Id might have been set to "null"
        if ("null".equals(compId))
            compId =  helper.completeInputTagId(null);
        // done
        return compId;
    }

    @Override
    public void setId(String id)
    {   // complete
        id = helper.completeInputTagId(id); 
        // setId
        super.setId(id);
    }

    @Override
    public void encodeBegin(FacesContext context)
        throws IOException
    {
        // add label and input components when the view is loaded for the first time
        super.encodeBegin(context);
        
        helper.encodeBegin();

        // render components
        InputControl control = helper.getInputControl();
        InputControl.ValueInfo vi = helper.getValueInfo(context);
        
        // wrapperTag
        String wrapperTag = helper.writeWrapperTag(context, true, true); 
        // render value
        renderControlValue(control, vi, context);
        // wrapperTagEnd
        if (wrapperTag!=null)
        {   // control wrapper tag
            ResponseWriter writer = context.getResponseWriter();
            writer.endElement(wrapperTag);
        }
    }

    protected void renderControlValue(InputControl control, InputControl.ValueInfo vi, FacesContext context)
        throws IOException
    {
        // Map<String, Object> map = getAttributes();
        String tagName = helper.getTagAttributeString("tag");
        String tooltip = helper.getValueTooltip(helper.getTagAttributeValue("title"));
        String cssStyle = helper.getTagAttributeStringEx(InputControl.CSS_STYLE_CLASS, true); // only check if present!

        // Check whether tag is required
        StyleClass styleClass = null;
        if (StringUtils.isNotEmpty(tagName) || StringUtils.isNotEmpty(cssStyle) || StringUtils.isNotEmpty(tooltip))
        {   // tagname
            if (StringUtils.isEmpty(tagName))
                tagName = InputControl.HTML_TAG_SPAN;
            // get style
            styleClass = helper.getTagStyleClass();
        }
        // render now
        control.renderValue(this, tagName, styleClass, tooltip, vi, context);
    }

}
