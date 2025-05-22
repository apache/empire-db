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
package org.apache.empire.jakarta.controls;

import java.io.IOException;
import java.util.List;

import jakarta.faces.component.UIComponent;
import jakarta.faces.component.html.HtmlInputTextarea;
import jakarta.faces.context.FacesContext;
import jakarta.faces.event.PhaseId;

import org.apache.empire.commons.StringUtils;
import org.apache.empire.exceptions.InvalidArgumentException;
import org.apache.empire.exceptions.UnexpectedReturnValueException;

public class TextAreaInputControl extends InputControl
{
    // private static final Logger log = LoggerFactory.getLogger(TextAreaInputControl.class);
    
    public static final String NAME = "textarea"; 
    
    public static final String FORMAT_COLS = "cols:";
    public static final String FORMAT_COLS_ATTRIBUTE = "format:cols";

    public static final String FORMAT_ROWS = "rows:";
    public static final String FORMAT_ROWS_ATTRIBUTE = "format:rows";

    private Class<? extends jakarta.faces.component.html.HtmlInputTextarea> inputComponentClass;

    public TextAreaInputControl(String name, Class<? extends HtmlInputTextarea> inputComponentClass)
    {
        super(name);
        this.inputComponentClass = inputComponentClass;
    }

    public TextAreaInputControl()
    {
        this(NAME, jakarta.faces.component.html.HtmlInputTextarea.class);
    }

    @Override
    public void renderValue(UIComponent comp, String tagName, String styleClass, String tooltip, ValueInfo vi, FacesContext context)
            throws IOException
    {
        styleClass += " textarea";
        // cols
        int cols = getFormatInteger(vi, FORMAT_COLS, FORMAT_COLS_ATTRIBUTE);
        if (cols>0)
            styleClass += " cols-"+String.valueOf(cols);
        // rows
        int rows = getFormatInteger(vi, FORMAT_ROWS, FORMAT_ROWS_ATTRIBUTE);
        if (rows>0)
            styleClass += " rows-"+String.valueOf(rows);
        // render
        super.renderValue(comp, tagName, styleClass, tooltip, vi, context);
    }
    
    @Override
    protected void createInputComponents(UIComponent parent, InputInfo ii, FacesContext context, List<UIComponent> compList)
    {
        // check params
        if (!compList.isEmpty())
            throw new InvalidArgumentException("compList", compList);
        // create
        HtmlInputTextarea input = InputControlManager.createComponent(context, this.inputComponentClass);
        // once
        copyAttributes(parent, ii, input);
        // cols
        int cols = getFormatInteger(ii, FORMAT_COLS, FORMAT_COLS_ATTRIBUTE);
        if (cols>0) {
            input.setCols(cols);
            input.setStyleClass(input.getStyleClass()+" cols-"+String.valueOf(cols)); 
        }
        // rows
        int rows = getFormatInteger(ii, FORMAT_ROWS, FORMAT_ROWS_ATTRIBUTE);
        if (rows>0) {
            input.setRows(rows);
            input.setStyleClass(input.getStyleClass()+" rows-"+String.valueOf(rows)); 
        }
        // add
        compList.add(input);
        // update
        updateInputState(compList, ii, context, PhaseId.RENDER_RESPONSE);
    }
    
    @Override
    protected void updateInputState(List<UIComponent> compList, InputInfo ii, FacesContext context, PhaseId phaseId)
    {
        UIComponent comp = compList.get(0);
        if (!(comp instanceof HtmlInputTextarea))
        {
            throw new UnexpectedReturnValueException(comp.getClass().getName(), "compList.get(0)");
        }
        HtmlInputTextarea input = (HtmlInputTextarea)comp;
        // disabled
        DisabledType disabled = ii.getDisabled();
        input.setReadonly((disabled==DisabledType.READONLY));
        input.setDisabled((disabled==DisabledType.DISABLED));
        
        // Set Value
        if (phaseId==PhaseId.RENDER_RESPONSE)
        {   // style
            setInputStyleClass(input, ii);
            // set value
            setInputValue(input, ii);
        }    
    }

    @Override
    public String formatValue(Object value, ValueInfo vi, boolean escapeHtml)
    {
        // escape
        String strVal = super.formatValue(value, vi, escapeHtml);
        if (!escapeHtml)
            return strVal;
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
