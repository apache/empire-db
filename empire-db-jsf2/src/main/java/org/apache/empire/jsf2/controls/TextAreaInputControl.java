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
package org.apache.empire.jsf2.controls;

import java.util.List;

import javax.faces.component.UIComponent;
import javax.faces.component.html.HtmlInputTextarea;
import javax.faces.context.FacesContext;

import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.commons.StringUtils;
import org.apache.empire.exceptions.UnexpectedReturnValueException;

public class TextAreaInputControl extends InputControl
{
    // private static final Logger log = LoggerFactory.getLogger(TextAreaInputControl.class);
    
    public static final String NAME = "textarea"; 
    
    public static final String FORMAT_COLS = "cols:";
    public static final String FORMAT_COLS_ATTRIBUTE = "format:cols";

    public static final String FORMAT_ROWS = "rows:";
    public static final String FORMAT_ROWS_ATTRIBUTE = "format:rows";
    
    private Class<? extends javax.faces.component.html.HtmlInputTextarea> inputComponentClass;

    public TextAreaInputControl(String name, Class<? extends HtmlInputTextarea> inputComponentClass)
    {
        super(name);
        this.inputComponentClass = inputComponentClass;
    }

    public TextAreaInputControl()
    {
        this(NAME, javax.faces.component.html.HtmlInputTextarea.class);
    }
    
    @Override
    protected void createInputComponents(UIComponent parent, InputInfo ii, FacesContext context, List<UIComponent> compList)
    {
        HtmlInputTextarea input;
        if (compList.size()==0)
        {   // create component
            input = InputControlManager.createComponent(context, this.inputComponentClass);
            // once
            copyAttributes(parent, ii, input);
            // cols
            int cols = getFormatInteger(ii, FORMAT_COLS, FORMAT_COLS_ATTRIBUTE);
            if (cols>0)
                input.setCols(cols);
            // rows
            int rows = getFormatInteger(ii, FORMAT_ROWS, FORMAT_ROWS_ATTRIBUTE);
            if (rows>0)
                input.setRows(rows);
            // add
            compList.add(input);
        }
        else
        {   // check type
            UIComponent comp = compList.get(0);
            if (!(comp instanceof HtmlInputTextarea))
                throw new UnexpectedReturnValueException(comp.getClass().getName(), "compList.get");
            // cast
            input = (HtmlInputTextarea)comp;
        }

        // disabled
        Object dis = ii.getAttributeEx("disabled");
        if (dis!=null)
            input.setDisabled(ObjectUtils.getBoolean(dis));
        // field-readOnly
        if (ObjectUtils.getBoolean(dis)==false)
            input.setReadonly(ii.isFieldReadOnly());
        // style
        addRemoveDisabledStyle(input, (input.isDisabled() || input.isReadonly()));
        addRemoveInvalidStyle(input, ii.hasError());
        
        // Set Value
        setInputValue(input, ii);
        
    }
    
    @Override
    protected void updateInputState(List<UIComponent> compList, InputInfo ii, FacesContext context)
    {
        UIComponent comp = compList.get(0);
        if (!(comp instanceof HtmlInputTextarea))
        {
            throw new UnexpectedReturnValueException(comp.getClass().getName(), "compList.get(0)");
        }
        HtmlInputTextarea input = (HtmlInputTextarea)comp;
        // disabled
        Object dis = ii.getAttributeEx("disabled");
        if (dis!=null)
            input.setDisabled(ObjectUtils.getBoolean(dis));
        // field-readOnly
        if (ObjectUtils.getBoolean(dis)==false)
            input.setReadonly(ii.isFieldReadOnly());
    }

    @Override
    protected String formatValue(Object value, ValueInfo vi)
    {
        String strVal = super.formatValue(value, vi);
        // replace CR/LF by <BR/>
        if (strVal.indexOf("\r\n")>0)
        {   // replace CR with <BR/>
            strVal = StringUtils.replace(strVal, "\r\n", "<BR/>\n");
        }
        else if (strVal.indexOf('\n')>0)
        {   // replace CR with <BR/>
            strVal = StringUtils.replace(strVal, "\n", "<BR/>\n");
        }
        return strVal; 
    }

    @Override
    protected Object parseInputValue(String value, InputInfo ii)
    {
        // Trim
        if (hasFormatOption(ii, "notrim")==false)
            value = value.trim();
        // Done 
        return value; 
    }
    
}
