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
package org.apache.empire.struts2.jsp.tags;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyTagSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.empire.struts2.html.HtmlTagDictionary;


@SuppressWarnings("serial")
public class FloatClearTag extends BodyTagSupport
{
    // Logger
    protected static Logger log = LoggerFactory.getLogger(FloatClearTag.class);
    
    @Override
    public int doStartTag()
        throws JspException
    {
        // super.doStartTag();
        // Write the float clear statement
        HtmlTagDictionary dic = HtmlTagDictionary.getInstance();
        String clear = dic.FloatClear();
        if (clear!=null)
        {   // Print the clear statement
            try {
                pageContext.getOut().print(clear);
            } catch (Exception e) {
                log.error("Unable to write to output stream.", e);
            }
        }
        // done, no body!
        return SKIP_BODY; 
    }

    @Override
    public int doEndTag()
        throws JspException
    {
        // return super.doEndTag();
        return EVAL_PAGE;
    }
}
