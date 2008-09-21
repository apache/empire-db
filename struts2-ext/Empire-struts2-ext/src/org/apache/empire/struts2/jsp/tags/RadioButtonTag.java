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

import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.struts2.html.HtmlWriter;
import org.apache.empire.struts2.html.HtmlWriter.HtmlTag;
import org.apache.struts2.components.Component;

import com.opensymphony.xwork2.util.ValueStack;


@SuppressWarnings("serial")
public class RadioButtonTag extends EmpireValueTagSupport
{
    // Checkbox Tag
    protected String name;
    protected Object checked;
    protected Object disabled;
    // More UI
    protected String tabindex;
    protected String onclick;
    protected String onchange;
    protected String onfocus;
    protected String onblur;
    
    @Override
    protected void resetParams()
    {
        name = null;
        checked = null;
        disabled = null;
        // AbstractUITag
        onclick = null;
        onchange = null;
        onfocus = null;
        onblur = null;
        tabindex = null;
        // Value
        super.resetParams();
    }

    @Override
    public Component getBean(ValueStack stack, HttpServletRequest req, HttpServletResponse res)
    {
        return null;
    }

    @Override
    public int doStartTag()
        throws JspException
    {
        // Tabel cell tag
        HtmlWriter w = new HtmlWriter(pageContext.getOut());
        HtmlTag rb = w.startTag("input");
        addStandardAttributes(rb, null);
        rb.addAttribute("type", "radio");
        rb.addAttribute("name",  getTagName(name));
        rb.addAttributeNoCheck("value", getStringValue(), true);
        rb.addAttribute("checked", getChecked());
        rb.addAttribute("disabled", getBoolean(disabled, false));
        rb.addAttribute("tabindex", this.tabindex);
        // Event Attributes
        rb.addAttribute("onclick",   this.onclick);
        rb.addAttribute("onchange",  this.onchange);
        rb.addAttribute("onfocus",   this.onfocus);
        rb.addAttribute("onblur",    this.onblur);
        rb.endTag();
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
    
    // ------- helpers -------
    
    public boolean getChecked()
    {
        if (checked!=null)
        {
            return getBoolean(checked, false);
        }
        if (hasDefaultValue())
        {
            Object value = getValue();
            Object defval = getDefaultValue();
            if ((value instanceof String) && ((String)value).length()==0)
                value = null;
            if ((defval instanceof String) && ((String)defval).length()==0)
                defval = null;
            return ObjectUtils.compareEqual(value, defval);
        }
        // Done
        return false;
    }

    // -------------------------------- Property accessors -----------------------------

    public void setName(String name)
    {
        this.name = name;
    }
    
    public void setChecked(Object checked)
    {
        this.checked = checked;
    }
    
    public void setCheck(Object checked)
    {
        this.checked = checked;
    }

    public void setDisabled(Object disabled)
    {
        this.disabled = disabled;
    }

    public void setTabindex(String tabindex)
    {
        this.tabindex = tabindex;
    }

    public void setOnclick(String onclick)
    {
        this.onclick = onclick;
    }

    public void setOnchange(String onchange)
    {
        this.onchange = onchange;
    }

    public void setOnfocus(String onfocus)
    {
        this.onfocus = onfocus;
    }

    public void setOnblur(String onblur)
    {
        this.onblur = onblur;
    }
    
}
