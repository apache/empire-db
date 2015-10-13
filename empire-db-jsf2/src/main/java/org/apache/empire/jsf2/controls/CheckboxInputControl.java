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

import java.io.IOException;
import java.util.List;

import javax.faces.component.UIComponent;
import javax.faces.component.html.HtmlSelectBooleanCheckbox;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.exceptions.InternalException;
import org.apache.empire.exceptions.UnexpectedReturnValueException;

public class CheckboxInputControl extends InputControl
{
    public static final String NAME = "checkbox";

    private Class<? extends javax.faces.component.html.HtmlSelectBooleanCheckbox> inputComponentClass;

    public CheckboxInputControl(Class<? extends HtmlSelectBooleanCheckbox> inputComponentClass)
    {
        super(NAME);
        this.inputComponentClass = inputComponentClass;
    }

    public CheckboxInputControl()
    {
        this(javax.faces.component.html.HtmlSelectBooleanCheckbox.class);
    }

	@Override
	public void renderValue(ValueInfo vi, ResponseWriter writer)
		throws IOException
	{
		boolean value = (Boolean) vi.getValue(true);
		writer.startElement("div", null);
		writer.writeAttribute("class", value ? "eTypeBoolTrue" : "eTypeBoolFalse", null);
		writer.append("&nbsp;");
		writer.endElement("div");
	}
	
    @Override
    protected void createInputComponents(UIComponent parent, InputInfo ii, FacesContext context, List<UIComponent> compList)
    {
        HtmlSelectBooleanCheckbox input;
        if (compList.size()==0)
        {   try {
                input = inputComponentClass.newInstance();
            } catch (InstantiationException e1) {
                throw new InternalException(e1);
            } catch (IllegalAccessException e2) {
                throw new InternalException(e2);
            }
            copyAttributes(parent, ii, input);
            // add
            compList.add(input);
        }
        else
        {   // check type
            UIComponent comp = compList.get(0);
            if (!(comp instanceof HtmlSelectBooleanCheckbox))
                throw new UnexpectedReturnValueException(comp.getClass().getName(), "compList.get");
            // cast
            input = (HtmlSelectBooleanCheckbox)comp;
        }

        // disabled
        boolean disabled = ii.isDisabled(); 
        input.setDisabled(disabled);

        // style
        addRemoveDisabledStyle(input, input.isDisabled());
        
        // Set Value
        setInputValue(input, ii);
    }
    
    @Override
    protected Object parseInputValue(String value, InputInfo ii)
    {
        return ObjectUtils.getBoolean(value);
    }
    
}
