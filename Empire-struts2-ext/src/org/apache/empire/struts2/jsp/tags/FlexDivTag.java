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

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;

import org.apache.empire.struts2.html.HtmlTagDictionary;
import org.apache.empire.struts2.html.HtmlWriter;
import org.apache.empire.struts2.html.HtmlTagDictionary.FlexDivRenderInfo;
import org.apache.empire.struts2.html.HtmlWriter.HtmlTag;
import org.apache.struts2.components.Component;

import com.opensymphony.xwork2.util.ValueStack;


@SuppressWarnings("serial")
public class FlexDivTag extends EmpireTagSupport
{
    // Type of tag
    protected String type;
    
    // Temporary
    FlexDivRenderInfo flexDivRenderInfo = null;
    
    @Override
    protected void resetParams()
    {
        type = null;
        // reset
        super.resetParams();
    }

    @Override
    public Component getBean(ValueStack stack, HttpServletRequest req, HttpServletResponse res)
    {
        log.fatal("No Bean available for flex-tag");
        return null;
    }

    @Override
    public int doStartTag()
        throws JspException
    {
        String userAgent = getUserAgent();
        // User-Agent
        HtmlTagDictionary dic = HtmlTagDictionary.getInstance();
        flexDivRenderInfo = dic.FlexDivTag(type, userAgent);
        if (flexDivRenderInfo!=null)
        {   // Render Flex Div
            HtmlWriter w = new HtmlWriter(pageContext.getOut());
            HtmlTag tag = w.startTag(flexDivRenderInfo.tag);
            tag.addAttribute("id", getId());
            tag.addAttribute("class", cssClass);
            tag.addAttributes(flexDivRenderInfo.attributes);
            tag.beginBody(flexDivRenderInfo.bodyBegin);
        }
        // return super.doStartTag();
        return EVAL_BODY_INCLUDE;
    }

    @Override
    public int doEndTag()
        throws JspException
    {
        // Render End Tag
        if (flexDivRenderInfo!=null)
        {   // End flexible Tag
            HtmlWriter w = new HtmlWriter(pageContext.getOut());
            HtmlTag tag = w.continueTag(flexDivRenderInfo.tag, true);
            tag.endTag(flexDivRenderInfo.bodyEnd);
            flexDivRenderInfo = null;
        }
        // return super.doEndTag();
        resetParams();
        return EVAL_PAGE;
    }
    
    private String getUserAgent()
    {
        ServletRequest req = pageContext.getRequest();
        if (req instanceof HttpServletRequest)
            return ((HttpServletRequest)req).getHeader("User-Agent");
        // Not detectable
        return "";
    }

    public void setType(String type)
    {
        this.type = type;
    }

}
