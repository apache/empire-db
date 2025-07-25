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

import org.apache.empire.commons.StringUtils;
import org.apache.empire.data.Column;
import org.apache.empire.exceptions.InvalidArgumentException;
import org.apache.empire.jakarta.controls.TextInputControl;
import org.apache.empire.jakarta.utils.TagEncodingHelper;
import org.apache.empire.jakarta.utils.TagEncodingHelperFactory;
import org.apache.empire.jakarta.utils.TagEncodingHelperFactory.TagEncodingHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.faces.component.UIOutput;
import jakarta.faces.context.FacesContext;
import jakarta.faces.context.ResponseWriter;

public class UnitTag extends UIOutput implements TagEncodingHolder
{
    // Logger
    private static final Logger log = LoggerFactory.getLogger(UnitTag.class);
    
    public final static String SPAN_ELEM = "span";
    
    protected final TagEncodingHelper helper = TagEncodingHelperFactory.create(this, "eUnit");

    public UnitTag()
    {
        log.trace("component LabelTag created");
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
    public void encodeBegin(FacesContext context)
        throws IOException
    {
        // add label and input components when the view is loaded for the first time
        super.encodeBegin(context);

        // Create
        Column column = helper.getColumn();
        if (column==null)
            throw new InvalidArgumentException("column", column);
        
        // the unit
        String unitLabel = StringUtils.toString(column.getAttribute(TextInputControl.FORMAT_UNIT_ATTRIBUTE));
        
        // render components
        ResponseWriter writer = context.getResponseWriter();
        writer.startElement(SPAN_ELEM, this);
        helper.writeAttribute(writer, "class", "eUnit");
        if (StringUtils.isNotEmpty(unitLabel))
            writer.append(unitLabel);
        writer.endElement(SPAN_ELEM);
    }

}
