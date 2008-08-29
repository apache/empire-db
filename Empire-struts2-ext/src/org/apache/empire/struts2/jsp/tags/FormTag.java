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
import org.apache.empire.struts2.jsp.components.FormComponent;
import org.apache.struts2.components.Component;

import com.opensymphony.xwork2.util.ValueStack;


@SuppressWarnings("serial")
public class FormTag extends FormPartTag // FormTag
{
    // FormTag
    protected String action;
    protected String name;
    protected String onsubmit;
    protected Object readOnly;
    protected String target;
    protected String enctype;
    protected String method;
    
    /*
     * InputControlTag Constructor
     */
    public FormTag()
    {
        this.method = "post";
    }

    /*
     * Clears all params since tag is reused
     */
    @Override
    protected void resetParams()
    {
        // FormTag
        action = null;
        name = null;
        onsubmit = null;
        readOnly = null;
        target = null;
        enctype = null;
        // method = null; // Don't reset this
        // reset
        super.resetParams();
    }
    
    @Override
    public boolean useBean()
    {
        return true;
    }
    
    @Override
    public Component getBean(ValueStack stack, HttpServletRequest req, HttpServletResponse res)
    {
        return new FormComponent(stack, req, res);
    }
    
    @Override
    protected void populateParams()
    {
        action = checkAction(action);

        if (onsubmit==null)
            onsubmit= HtmlTagDictionary.getInstance().FormDefaultOnSubmitScript();
        
        super.populateParams();

        // Form Component
        FormComponent comp = (FormComponent)component;
        comp.setAction(action);
        comp.setName(name);
        comp.setOnsubmit(onsubmit);
        comp.setTarget(target);
        comp.setEnctype(enctype);
        comp.setMethod(method);
        comp.setReadOnly(getBoolean(readOnly, false));
    }
    
    @Override
    public int doStartTag() throws JspException
    {
        int result = super.doStartTag();
        // Set default Property name
        if (getBoolean(readOnly, false))
            putPageAttribute(READONLY_ATTRIBUTE, true);
        // do Start
        return result;
    }

    @Override
    public int doEndTag() throws JspException
    {
        // Remove Read Only
        if (getBoolean(readOnly, false))
            removePageAttribute(READONLY_ATTRIBUTE, null);
        // done
        return super.doEndTag();
    }
    
    @Override
    protected boolean renderReadOnlyFields()
    {
        if (getBoolean(readOnly, false)==true)
            return false;
        // Default is to render the hidden fields
        return getBoolean(hiddenFields, true);        
    }

    @Override
    protected boolean renderWrapperTag()
    {
        HtmlTagDictionary dic = HtmlTagDictionary.getInstance();  
        return getBoolean(wrap, dic.FormDefaultRenderWrapper());
    }
    
    // ------- Setters -------

    public void setAction(String action)
    {
        this.action = action;
    }

    public void setEnctype(String enctype)
    {
        this.enctype = enctype;
    }

    public void setMethod(String method)
    {
        this.method = method;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public void setOnsubmit(String onsubmit)
    {
        this.onsubmit = onsubmit;
    }

    public void setTarget(String target)
    {
        this.target = target;
    }

    public void setReadOnly(Object readOnly)
    {
        this.readOnly = readOnly;
    }

}
