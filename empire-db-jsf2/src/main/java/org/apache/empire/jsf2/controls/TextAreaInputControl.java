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
import org.apache.empire.exceptions.InternalException;
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

    public TextAreaInputControl(Class<? extends HtmlInputTextarea> inputComponentClass)
    {
        super(NAME);
        this.inputComponentClass = inputComponentClass;
    }

    public TextAreaInputControl()
    {
        this(javax.faces.component.html.HtmlInputTextarea.class);
    }
    
    @Override
    protected void createInputComponents(UIComponent parent, InputInfo ii, FacesContext context, List<UIComponent> compList)
    {
        HtmlInputTextarea input;
        if (compList.size()==0)
        {   try {
                input = inputComponentClass.newInstance();
            } catch (InstantiationException e1) {
                throw new InternalException(e1);
            } catch (IllegalAccessException e2) {
                throw new InternalException(e2);
            }
            // once
            copyAttributes(parent, ii, input);
            // disabled
            Object dis = ii.getAttribute("disabled");
            if (dis!=null)
                input.setDisabled(ObjectUtils.getBoolean(dis));
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
        
        // Set Value
        input.setReadonly(ii.isDisabled());
        setInputValue(input, ii);
        
    }

    /*
    private int getTextareaCols(InputInfo ii)
    {
        ii.getColumn().getAttribute("");
    }
    */
    
}
