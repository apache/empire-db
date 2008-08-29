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

import org.apache.empire.struts2.jsp.components.AnchorComponent;
import org.apache.struts2.components.Component;

import com.opensymphony.xwork2.util.ValueStack;


@SuppressWarnings("serial")
public class LinkTag extends EmpireTagSupport // AbstractRemoteCallUITag
{
    // LinkTag
    protected String action;
    protected Object item;
    protected String text;
    protected Object disabled;
    protected String target;
    protected String param;
    protected String onclick;
    protected Object visible;

    public LinkTag()
    {
        // Default constructor
    }

    /*
     * Clears all params since tag is reused
     */
    @Override
    protected void resetParams()
    {
        // LinkTag
        action = null;
        item = null;
        text = null;
        disabled = null;
        target = null;
        param = null;
        onclick = null;
        visible = null;
        // call base
        super.resetParams();
    }

    @Override
    public Component getBean(ValueStack stack, HttpServletRequest req, HttpServletResponse res)
    {
        return new AnchorComponent(stack, req, res);
    }

    @Override
    protected void populateParams()
    {
        // Checks action param and warns if method is not supplied
        action = checkAction(action);

        /*
        if (onclick == null)
            onclick = HtmlTagDictionary.getInstance().AnchorDefaultOnClickScript();
        */    

        super.populateParams();

        AnchorComponent anchor = (AnchorComponent) component;
        // Set item param
        if (item != null)
        {
            anchor.addParameter(str(param, getActionItemPropertyName()), getString(item));
        }
        // get Href
        anchor.setAction(action);
        anchor.setText(getString(text));
        anchor.setDisabled(getBoolean(disabled, false));
        anchor.setTargets(target);
        anchor.setOnclick(onclick);
    }

    @Override
    public int doStartTag()
        throws JspException
    {
        // check visibility
        if (getBoolean(visible, true)==false)
        {   // not visible
            return SKIP_BODY;
        }
        // Render Link
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
        // End tag
        return super.doEndTag();
    }

    // -------------------------------- Property accessors -----------------------------

    public void setAction(String action)
    {
        this.action = action;
    }

    public void setItem(Object item)
    {
        this.item = item;
    }

    public void setText(String text)
    {
        this.text = text;
    }

    public void setDisabled(Object disabled)
    {
        this.disabled = disabled;
    }

    public void setTarget(String target)
    {
        this.target = target;
    }

    public void setParam(String param)
    {
        this.param = param;
    }

    public void setOnclick(String onclick)
    {
        this.onclick = onclick;
    }

    public void setVisible(Object visible)
    {
        this.visible = visible;
    }

}
