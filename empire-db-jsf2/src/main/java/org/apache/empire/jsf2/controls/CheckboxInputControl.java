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
import javax.faces.event.PhaseId;

import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.exceptions.InvalidArgumentException;
import org.apache.empire.exceptions.UnexpectedReturnValueException;

public class CheckboxInputControl extends InputControl
{
    public static final String NAME = "checkbox";

    private final Class<? extends javax.faces.component.html.HtmlSelectBooleanCheckbox> inputComponentClass;

    public CheckboxInputControl(String name, Class<? extends HtmlSelectBooleanCheckbox> inputComponentClass)
    {
        super(name);
        this.inputComponentClass = inputComponentClass;
    }

    public CheckboxInputControl()
    {
        this(NAME, javax.faces.component.html.HtmlSelectBooleanCheckbox.class);
    }

    @Override
    public void renderValue(ValueInfo vi, ResponseWriter writer)
        throws IOException
    {
    	Boolean value = (Boolean) vi.getValue(true);
        writer.startElement(HTML_TAG_DIV, null);
        if (value == null)
        {
        	// value not set
        	writer.writeAttribute(HTML_ATTR_CLASS, "eTypeBoolNull", null);
        } else if (value) 
        {
        	// value is true
        	writer.writeAttribute(HTML_ATTR_CLASS, "eTypeBoolTrue", null);
        } else
        {
        	// value is false
        	writer.writeAttribute(HTML_ATTR_CLASS, "eTypeBoolFalse", null);
        }
        writer.append(HTML_EXPR_NBSP);
        writer.endElement(HTML_TAG_DIV);
    }

    @Override
    protected void createInputComponents(UIComponent parent, InputInfo ii, FacesContext context, List<UIComponent> compList)
    {
        if (!compList.isEmpty())
            throw new InvalidArgumentException("compList", compList);
        // create
        HtmlSelectBooleanCheckbox input = InputControlManager.createComponent(context, this.inputComponentClass);
        // copy attributes
        copyAttributes(parent, ii, input);
        // add
        compList.add(input);
        // set style and value
        updateInputState(compList, ii, context, context.getCurrentPhaseId());
    }

    @Override
    protected void updateInputState(List<UIComponent> compList, InputInfo ii, FacesContext context, PhaseId phaseId)
    {
        UIComponent comp = compList.get(0);
        if (!(comp instanceof HtmlSelectBooleanCheckbox))
        {
            throw new UnexpectedReturnValueException(comp.getClass().getName(), "compList.get(0)");
        }
        HtmlSelectBooleanCheckbox input = (HtmlSelectBooleanCheckbox) comp;
        // disabled
        boolean disabled = ii.isDisabled();
        input.setDisabled(disabled);
        // check phase
        if (phaseId==PhaseId.RENDER_RESPONSE)
        {   // style
            addRemoveDisabledStyle(input, input.isDisabled());
            // set value
            setInputValue(input, ii);
        }
    }

    @Override
    protected Object parseInputValue(String value, InputInfo ii)
    {
        return ObjectUtils.getBoolean(value);
    }

}
