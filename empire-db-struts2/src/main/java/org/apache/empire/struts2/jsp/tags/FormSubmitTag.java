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

import org.apache.empire.struts2.html.HtmlTagDictionary;
import org.apache.empire.struts2.html.HtmlWriter;
import org.apache.empire.struts2.html.HtmlWriter.HtmlTag;
import org.apache.struts2.components.Component;

import com.opensymphony.xwork2.util.ValueStack;


@SuppressWarnings("serial")
public class FormSubmitTag extends EmpireTagSupport
{
    // Properties
    protected String text; 
    protected String action;
    protected Object redirect;
    protected Object disabled;
    protected String tabindex;
    protected String onclick;
    protected Object embed;
    protected String name;
    
    @Override
    protected void resetParams()
    {
        text = null; 
        name = null;
        action = null;
        disabled = null;
        tabindex = null;
        onclick = null;
        redirect = null;
        embed = null;
        // reset
        super.resetParams();
    }
    
    @Override
    public Component getBean(ValueStack stack, HttpServletRequest req, HttpServletResponse res)
    {
        // Create Anchor Component
        log.error("No Bean avaliable for Submit tag.");
        return null; 
    }

    @Override
    protected void populateParams()
    {
        log.error("Illegar mehtod call.");
    }

    @Override
    public int doStartTag() throws JspException
    {
        HtmlTagDictionary dic = HtmlTagDictionary.getInstance();
        HtmlWriter w = new HtmlWriter(pageContext.getOut());
        // Wrap button like an input control
        if (getBoolean(embed, false))
        {
            // the wrapper (only if renderLabel && renderControl are both true)
            HtmlTag wrapper = w.startTag( dic.InputWrapperTag());
            wrapper.addAttribute("class", dic.InputWrapperClass());
            wrapper.beginBody(true);
            
            HtmlTag wrapCtrl = w.startTag( dic.SubmitControlTag());
            wrapCtrl.addAttribute("class", dic.SubmitControlClass());
            wrapCtrl.addAttributes(dic.SubmitControlAttributes());
            wrapCtrl.beginBody();
        }
        // Button
        renderButtonStart(w);
        // return usesBody ? EVAL_BODY_BUFFERED : EVAL_BODY_INCLUDE;
        return EVAL_BODY_INCLUDE;
    }
    
    @Override
    public int doEndTag() throws JspException
    {
        HtmlTagDictionary dic = HtmlTagDictionary.getInstance();
        HtmlWriter w = new HtmlWriter(pageContext.getOut());
        // End Button
        renderButtonEnd(w);
        // Has Wrappers
        if (getBoolean(embed, false))
        {   // End Control Wrapper 
            HtmlTag wrapCtrl = w.continueTag( dic.SubmitControlTag(), true);
            wrapCtrl.endTag();
            // End Wrapper 
            HtmlTag wrapper = w.continueTag( dic.InputWrapperTag(), true);
            wrapper.endTag();
        }    
        // done
        resetParams();
        // Done
        return EVAL_PAGE;
    }
    
    private void renderButtonStart(HtmlWriter w)
    {
        // Tabel cell tag
        HtmlTag button = w.startTag("button");
        button.addAttribute("id",   getId());
        button.addAttribute("type", "submit");
        button.addAttribute("name", getButtonName());
        // General Attributes
        button.addAttribute("onclick", onclick);
        button.addAttribute("disabled", getBoolean(disabled, false));
        button.addAttribute("tabindex", getString (tabindex, null));
        // Commmon
        button.addAttribute("class", getButtonClass());
        button.addAttribute("style", cssStyle);
        // Button Text
        button.beginBody(getString(text));
    }

    private void renderButtonEnd(HtmlWriter w)
    {
        // Write End Tag
        HtmlTag menu = w.continueTag ("button", true);
        menu.endTag();
    }
    
    private String getButtonName()
    {
        if (name!=null)
        {
            if (action!=null)
            {
                log.warn("Name is set on submit button. Action property is ignored! " + action);
            }
            return name;
        }
        // Method given?
        if (action==null)
        {
            return null;
        }
		
        // Set Name from method
        /*
         * UPGRADE-struts 2.1.6
         * CHANGE: changed "redirect-action" to "redirectAction"
         * Reason: The types are now written in "camelCase"
         */
        String call = (getBoolean(redirect, false) ? "redirectAction:" : "action:");
        return call + checkAction(action);
    }
    
    private String getButtonClass()
    {
        // get Default Class
        if (cssClass!=null)
            return cssClass;
        // Get Class from Dictionary
        HtmlTagDictionary dic = HtmlTagDictionary.getInstance();
        return dic.SubmitClass();
    }
    
    // ------- Property setters -------

    public void setAction(String action)
    {
        this.action = action;
    }

    public void setDisabled(Object disabled)
    {
        this.disabled = disabled;
    }

    public void setOnclick(String onclick)
    {
        this.onclick = onclick;
    }

    public void setTabindex(String tabindex)
    {
        this.tabindex = tabindex;
    }

    public void setText(String text)
    {
        this.text = text;
    }

    public void setRedirect(Object redirect)
    {
        this.redirect = redirect;
    }

    public void setEmbed(Object embed)
    {
        this.embed = embed;
    }

    public void setName(String name)
    {
        this.name = name;
    }
}
