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
import java.io.Writer;

import javax.faces.component.UIComponent;
import javax.faces.context.ResponseWriter;

import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.commons.StringUtils;
import org.apache.empire.exceptions.NotImplementedException;

public class StringResponseWriter extends ResponseWriter
{
    private StringBuilder buf = new StringBuilder();

    @Override
    public String getContentType()
    {
        return "text/html";
    }

    @Override
    public String getCharacterEncoding()
    {
        return "UTF-8";
    }

    @Override
    public void flush()
        throws IOException
    {
    }

    @Override
    public void startDocument()
        throws IOException
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void endDocument()
        throws IOException
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void startElement(String name, UIComponent component)
        throws IOException
    {
        buf.append("<");
        buf.append(name);
        buf.append(">");
    }

    @Override
    public void endElement(String name)
        throws IOException
    {
        buf.append("</");
        buf.append(name);
        buf.append(">");
    }

    @Override
    public void writeAttribute(String name, Object value, String property)
        throws IOException
    {
        if (StringUtils.isEmpty(name) || ObjectUtils.isEmpty(value))
            return;
        buf.append(name);
        buf.append("=");
        buf.append(String.valueOf(value));
    }

    @Override
    public void writeURIAttribute(String name, Object value, String property)
        throws IOException
    {
        writeAttribute(name, value, property);
    }

    @Override
    public void writeComment(Object comment)
        throws IOException
    {
        /* Nothing */
    }

    @Override
    public void writeText(Object text, String property)
        throws IOException
    {
        buf.append(text);
    }

    @Override
    public void writeText(char[] text, int off, int len)
        throws IOException
    {
        buf.append(text, off, len);
    }

    @Override
    public ResponseWriter cloneWithWriter(Writer writer)
    {
        throw new NotImplementedException(getClass(), "cloneWithWriter");
    }

    @Override
    public void write(char[] cbuf, int off, int len)
        throws IOException
    {
        buf.append(cbuf, off, len);
    }

    @Override
    public void close()
        throws IOException
    {
    }

    @Override
    public String toString()
    {
        return buf.toString();
    }
    
}
