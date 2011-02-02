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
package org.apache.empire.struts2.html;

import java.io.IOException;
import java.io.Writer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.empire.commons.StringUtils;
import org.apache.commons.lang.xwork.StringEscapeUtils;


public class HtmlWriter
{
    // Logger
    protected static Logger log = LoggerFactory.getLogger(HtmlWriter.class);
    
    public static class HtmlTag
    {
        private HtmlWriter w;
        private String name;
        private boolean hasBody;
        
        protected HtmlTag(HtmlWriter writer, String name, boolean hasBody)
        {
            this.w = writer;
            this.name = name;
            this.hasBody = hasBody;
        }
        
        protected void startTag()
        {
            // write start
            if (name!=null)
            {
                w.print("<");
                w.print(name);
            }
        }
        
        public boolean isValid()
        {
            return (w!=null && name!=null);
        }

        public void addAttributes(String attributes)
        {
            if (name==null || attributes==null)
                return; // Ignore Attributes with null values
            w.print(" ");
            w.print(attributes);
        }
        
        public void addAttributeNoCheck(String attrib, Object value, boolean escapeHtml)
        {
            if (name==null)
                return;
            // Get String value
            String strValue = StringUtils.valueOf(value);
            if (escapeHtml)
                strValue = StringEscapeUtils.escapeHtml(strValue);
            // Add Attribute
            w.print(" ");
            w.print(attrib);
            w.print("=\"");
            w.print(strValue);
            w.print("\"");
        }
        
        public void addAttribute(String attrib, Object value, boolean escapeHtml)
        {
            if (name==null || value==null)
                return; // Ignore Attributes with null values
            String strValue = value.toString();
            if (strValue==null || strValue.length()==0)
                return; // Ingore Emtpy Strings
            if (escapeHtml)
                strValue = StringEscapeUtils.escapeHtml(strValue);
            w.print(" ");
            w.print(attrib);
            w.print("=\"");
            w.print(strValue);
            w.print("\"");
        }

        public void addAttribute(String attrib, Object value)
        {
            addAttribute(attrib, value, false);
        }
        
        public void addAttribute(String attrib)
        {
            addAttribute(attrib, attrib, false);
        }

        public void addAttribute(String attrib, boolean present)
        {
            if (present)
                addAttribute(attrib, attrib, false);
        }

        public void beginBody(String body, boolean newLine)
        {
            if (name!=null)
                w.print(">", newLine);
            if (body!=null)
                w.print(body);
            hasBody = true;
        }
        
        public void beginBody(String body)
        {
            beginBody(body, false);
        }
        
        public void beginBody(boolean newLine)
        {
            beginBody(null, newLine);
        }
        
        public void beginBody()
        {
            beginBody(null, false);
        }
        
        public void endTag(String body, boolean newLine)
        {
            // Write body
            if (body!=null && body.length()>0)
            {
                if (hasBody==false)
                    beginBody(body, false);
                else w.print(body);
            }
            // End of Tag
            if (name!=null)
            {
                // Check Body
                if (hasBody)
                {
                    w.print("</");
                    w.print(name);
                    w.print(">", newLine);
                }
                else
                {   
                    w.print(" />", newLine);
                }
            }
            // done
            w = null;
        }

        public void endTag(String body)
        {
            endTag(body, false);
        }

        public void endTag(boolean newLine)
        {
            endTag(null, newLine);
        }

        public void endTag()
        {
            endTag(null, false);
        }
    }   
    
    private Writer writer;
    
    public HtmlWriter(Writer writer)
    {
        this.writer = writer;
    }

    private void print(String text, boolean endOfLine)
    {
        try {
            // Check Param
            if (text==null)
            {
                log.warn("cannot print text value of null!");
                return; // nothing do do
            }
            // Print now
            writer.write(text);
            if (endOfLine)
                writer.write("\n");
          } catch(IOException e) {
              log.error(e.getMessage(), e);
          }
    }

    public void print(String text)
    {
        print(text, false);
    }

    public void println(String text)
    {
        print(text, true);
    }

    public void println()
    {
        print("", true);
    }
    
    public HtmlTag startTag(String name)
    {
        HtmlTag tag = new HtmlTag(this, name, false);
        tag.startTag();
        return tag;
    }
    
    public HtmlTag continueTag(String name, boolean hasBody)
    {
        return new HtmlTag(this, name, hasBody);
    }
    
}
