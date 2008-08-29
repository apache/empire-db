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

import org.apache.empire.commons.Options;
import org.apache.empire.data.ColumnExpr;
import org.apache.empire.struts2.jsp.components.DataValueComponent;
import org.apache.empire.struts2.jsp.controls.InputControl;
import org.apache.empire.struts2.jsp.controls.InputControlManager;
import org.apache.struts2.components.Component;

import com.opensymphony.xwork2.util.ValueStack;


@SuppressWarnings("serial")
public class DataValueTag extends EmpireValueTagSupport
{
    // Control Component
    protected Options      lookup;
    protected String       controlType;
    protected String       body;  // body Usage
    protected String       format;
    protected Object       nullValue;
    // Wrapper tag
    protected String       tag;
    // Link attributes
    protected String       action;
    protected Object       alt;
    protected Object       item; 
    protected String       param; 
    protected String       anchorClass;
    protected String       onclick;
    protected String       ondblclick;
    
    /*
     * Clears all params since tag is reused
     */
    @Override
    protected void resetParams()
    {
        lookup = null;
        controlType = null;
        body = null;
        format = null;
        nullValue = null;
        // Wrapper tag
        tag = null;
        // Link attributes
        action = null;
        alt = null;
        item = null; 
        param = null;
        anchorClass = null;
        onclick = null;
        ondblclick = null;
        // Value
        super.resetParams();
    }

    @Override
    public Component getBean(ValueStack stack, HttpServletRequest req,
            HttpServletResponse res)
    {
        // Detect control type and readOnly state
        if (controlType==null)
            controlType = getControlType();
        // Create
        InputControl control = InputControlManager.getControl(controlType);
        if (control==null)
            control= InputControlManager.getControl("text"); 
        // Create component
        return new DataValueComponent(control, stack, req, res);
    }
    
    @Override
    protected void populateParams()
    {
        // Checks action param and warns if method is not supplied
        action = checkAction(action);
        
        super.populateParams();

        // Component
        DataValueComponent comp = (DataValueComponent)component;

        // Control Component
        comp.setOptions( getLookupOptions() );
        comp.setColumn (getColumn());
        comp.setRecordValue(getValue());
        comp.setNullValue(getObject(nullValue, null));
        comp.setBodyUsage(body);
        comp.setFormat(format);
        
        // Data Value Component
        comp.setHtmlTag(tag);
        if (action!=null)
        {
            comp.setAction(action);
            comp.setAlt(getItemValue(alt));
            comp.setAnchorClass(anchorClass);
            comp.setParam(str(param, getActionItemPropertyName()), getItemValue(item));
        }
        
        // Common UI
        comp.setOnclick(onclick);
        comp.setOndblclick(ondblclick);
        comp.setCssClass(cssClass);
        comp.setCssStyle(cssStyle);
        
    }

    private Options getLookupOptions()
    {
        if (lookup != null)
            return lookup;
        // Get List from Column
        ColumnExpr expr = getColumnExpr();
        if (expr!=null)
            return expr.getOptions();
        /*
        ColumnExpr expr = getColumnExpr();
        if (expr instanceof DBAliasExpr)
            expr = ((DBAliasExpr)expr).getExpr();
        // Check whether column expression is a Column
        if (expr instanceof Column)
            return ((Column)expr).getOptions();
        */
        return null;
    }
    
    // -------------------------------- Property accessors -----------------------------

    public void setLookup(Options lookup)
    {
        this.lookup = lookup;
    }

    public void setAction(String action)
    {
        this.action = action;
    }

    public void setItem(Object item)
    {
        this.item = item;
    }

    public void setAlt(Object alt)
    {
        this.alt = alt;
    }

    public void setAnchorClass(String anchorClass)
    {
        this.anchorClass = anchorClass;
    }

    public void setBody(String body)
    {
        this.body = body;
    }

    public void setTag(String tag)
    {
        this.tag = tag;
    }

    public void setFormat(String format)
    {
        this.format = format;
    }

    public void setControlType(String controlType)
    {
        this.controlType = controlType;
    }

    public void setNullValue(Object nullValue)
    {
        this.nullValue = nullValue;
    }

    public void setParam(String param)
    {
        this.param = param;
    }

    public void setOnclick(String onclick)
    {
        this.onclick = onclick;
    }

    public void setOndblclick(String ondblclick)
    {
        this.ondblclick = ondblclick;
    }
    
}
