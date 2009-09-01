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

import org.apache.empire.struts2.html.HtmlTagDictionary;
import org.apache.empire.struts2.html.HtmlWriter;
import org.apache.empire.struts2.html.HtmlWriter.HtmlTag;


@SuppressWarnings("serial")
public class ButtonTag extends LinkTag
{
    /*
     * InputControlTag Constructor
     */
    /*
    public ButtonTag()
    {
    }

    @Override
    protected void resetParams()
    {
        // Reset Params
        super.resetParams();
    }
    */
    
    @Override
    public int doStartTag()
        throws JspException
    {
        // check visibility
        if (getBoolean(visible, true)==false)
        {   // not visible
            return SKIP_BODY;
        }
        // Start Tag
        HtmlTagDictionary dic = HtmlTagDictionary.getInstance();
        HtmlWriter w = new HtmlWriter(pageContext.getOut());
        HtmlTag button = w.startTag ( dic.ButtonTag() );
        addStandardAttributes(button, dic.ButtonClass());
        button.beginBody();
        // Start Value
        this.id=null;
        this.cssClass=null;
        this.cssStyle=null;
        // OnClick
        if (onclick==null && action!=null)
        {
            if (action.startsWith("!"))
                onclick = dic.ButtonSameActionDefaultOnClickScript();
            else
                onclick = dic.ButtonOtherActionDefaultOnClickScript();
        }
        // Render Value
        return super.doStartTag();
    }
    
    @Override
    public int doEndTag()
        throws JspException
    {
        // check visibility
        if (getBoolean(visible, true)==false)
        {   // Not visible
            if (autoResetParams)
                resetParams();
            return EVAL_PAGE;
        }    
        // End Tag
        int result = super.doEndTag();
        // Write End Tag
        HtmlTagDictionary dic = HtmlTagDictionary.getInstance();
        HtmlWriter w = new HtmlWriter(pageContext.getOut());
        HtmlTag td = w.continueTag(dic.ButtonTag(), true);
        td.endTag();
        // done
        return result;
    }

    // ------- Property accessors -------

}
