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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;

import org.apache.empire.struts2.action.ActionErrorProvider;
import org.apache.empire.struts2.html.HtmlTagDictionary;
import org.apache.empire.struts2.html.HtmlWriter;
import org.apache.empire.struts2.html.HtmlWriter.HtmlTag;
import org.apache.struts2.components.Component;

import com.opensymphony.xwork2.util.ValueStack;


@SuppressWarnings("serial")
public class ActionMessageTag extends EmpireTagSupport
{
    @Override
    public Component getBean(ValueStack stack, HttpServletRequest req, HttpServletResponse res)
    {
        return null;
    }

    @Override
    public int doStartTag()
        throws JspException
    {       
        // Get the action
        Object action = getAction();
        if (action instanceof ActionErrorProvider)
        {   // Tabel cell tag
            HtmlWriter w = new HtmlWriter(pageContext.getOut());
            renderMessage(w, (ActionErrorProvider)action);
        }
        else
        {   // Error: Action does implement ActionErrorProvider 
            log.error("Cannot render message. Action does implement ActionErrorProvider.");
        }
        // wrap.beginBody(getTextValue());
        // return usesBody ? EVAL_BODY_BUFFERED : EVAL_BODY_INCLUDE;
        return SKIP_BODY;
    }

    @Override
    public int doEndTag()
        throws JspException
    {   // done
        resetParams();
        return EVAL_PAGE;
    }
    
    // ------- render helpers -------
    
    private void renderMessage(HtmlWriter w, ActionErrorProvider provider)
    {
        // Get errors
        String msg = provider.getLastActionMessage(true);
        if (msg==null || msg.length()==0)
            return; // Nothing to render

        // Render error list
        HtmlTagDictionary dic = HtmlTagDictionary.getInstance();
        HtmlTag tag = w.startTag(dic.MessageTag());
        addStandardAttributes(tag, dic.MessageClass());
        tag.endTag(msg);
    }
    
}
