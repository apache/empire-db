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
import java.util.Map;

import javax.faces.component.UIOutput;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.apache.empire.commons.StringUtils;
import org.apache.empire.data.Column;
import org.apache.empire.exceptions.InvalidArgumentException;
import org.apache.empire.jsf2.utils.TagEncodingHelper;
import org.apache.empire.jsf2.utils.TagEncodingHelperFactory;
import org.apache.empire.jsf2.utils.TagStyleClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TitleTag extends UIOutput // implements NamingContainer
{
    // Logger
    private static final Logger log = LoggerFactory.getLogger(TitleTag.class);
    
    protected final TagEncodingHelper helper = TagEncodingHelperFactory.create(this, TagStyleClass.TITLE.get());

    public TitleTag()
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

        // Create
        Column column = helper.getColumn();
        if (column==null)
            throw new InvalidArgumentException("column", column);
        
        // Tooltip title
        String title = helper.getLabelTooltip(column);
        
        // render components
        ResponseWriter writer = context.getResponseWriter();
        String tag = writeStartElement(title, writer);
        renderTitle(column, writer);
        if (tag != null)
            writer.endElement(tag);
    }

    /* Helpers */
    protected void renderTitle(Column column, ResponseWriter writer) throws IOException
    {
        String title=StringUtils.toString(getValue());
        // Check for short form    
        if (helper.hasFormat("short"))
        {
            title = StringUtils.toString(column.getAttribute(TagEncodingHelper.COLATTR_ABBR_TITLE));
            if (title==null)
                log.warn("No Abbreviation available for column {}. Using normal title.", column.getName());
        }
        // Use normal title
        if (title==null)
            title=column.getTitle();
        // render now
        title = helper.getDisplayText(title);
        writer.append((StringUtils.isEmpty(title) ? "&nbsp;" : title));
    }

    protected String writeStartElement(String title, ResponseWriter writer)
        throws IOException
    {
        Map<String, Object> map = getAttributes();
        String tag = StringUtils.toString(map.get("tag"));
        // Check
        if (tag == null && title == null && !map.containsKey("styleClass"))
            return null;
        // Write tag
        if (StringUtils.isEmpty(tag))
            tag="span";
        writer.startElement(tag, this);
        helper.writeAttribute(writer, "class", helper.getTagStyleClass());
        helper.writeAttribute(writer, "style", map.get("style"));
        helper.writeAttribute(writer, "title", title);
        return tag;
    }

}
