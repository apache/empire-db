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
import javax.faces.component.html.HtmlOutputLabel;
import javax.faces.context.FacesContext;

import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.data.DataType;
import org.apache.empire.jsf2.utils.TagEncodingHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LabelTag extends UIOutput // implements NamingContainer
{
    // Logger
    private static final Logger log = LoggerFactory.getLogger(LabelTag.class);
    
    private final TagEncodingHelper helper = new TagEncodingHelper(this, "eLabel");

    public LabelTag()
    {
        log.trace("component LabelTag created");
    }

    @Override
    public String getFamily()
    {
        return "javax.faces.NamingContainer";
    }

    @Override
    public void encodeBegin(FacesContext context)
        throws IOException
    {
        // add label and input components when the view is loaded for the first time
        super.encodeBegin(context);
        
        // render components
        HtmlOutputLabel labelComponent = null;
        if (getChildCount() > 0)
        {
            labelComponent = (HtmlOutputLabel) getChildren().get(0);
        }
        if (labelComponent == null)
        {
            String forInput   = helper.getTagAttribute("for");
            String styleClass = helper.getTagStyleClass(DataType.UNKNOWN);
            String style      = helper.getTagAttribute("style");
            // createLabelComponent 
            labelComponent = helper.createLabelComponent(context, forInput, styleClass, style, getColon());
            this.getChildren().add(labelComponent);
        }

        // render components
        labelComponent.encodeAll(context);
    }
    
    protected boolean getColon()
    {
        Object colon = getAttributes().get("colon");
        if (colon!=null)
            return ObjectUtils.getBoolean(colon);
        // See if we have a record parent
        return true;
    }

    /*
    protected boolean isRequired(Column column)
    {
        Object required = getAttributes().get("required");
        if (required!=null)
            return ObjectUtils.getBoolean(required);
        // See if we have a record parent
        if (helper.isReadOnly())
            return false;
        // Required
        return column.isRequired();
    }
    */
    
}
