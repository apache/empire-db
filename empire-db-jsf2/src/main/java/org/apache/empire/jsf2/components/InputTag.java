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
package org.apache.empire.jsf2.components;

import java.io.IOException;
import java.util.Map;

import javax.faces.component.NamingContainer;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.apache.empire.data.Column;
import org.apache.empire.jsf2.controls.InputControl;
import org.apache.empire.jsf2.utils.TagEncodingHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InputTag extends UIInput implements NamingContainer
{
    // Logger
    private static final Logger log          = LoggerFactory.getLogger(InputTag.class);
    
    // private static final String inpControlPropName = InputControl.class.getSimpleName();
    // private static final String inputInfoPropName = InputControl.InputInfo.class.getSimpleName();
    private static final String readOnlyState  = "readOnlyState";

    private final TagEncodingHelper helper = new TagEncodingHelper(this, "eInput");

    private InputControl control = null;
    private InputControl.InputInfo inpInfo = null;

    public InputTag()
    {
        super();
    }

    @Override
    public String getFamily()
    {
        return "javax.faces.NamingContainer";
    }

    private void saveState()
    {
        // getStateHelper().put(inpControlPropName, control);
        // getStateHelper().put(inputInfoPropName, inpInfo);
        getStateHelper().put(readOnlyState, (inpInfo==null));
    }

    private boolean initState(FacesContext context)
    {
        Boolean ros = (Boolean)getStateHelper().get(readOnlyState);
        if (ros!=null && ros.booleanValue())
            return false;
        // control = ;
        control = helper.getInputControl();
        inpInfo  = helper.getInputInfo(context);
        return (control!=null && inpInfo!=null);
    }

    @Override
    public void encodeBegin(FacesContext context)
        throws IOException
    {
        // add label and input components when the view is loaded for the first time
        super.encodeBegin(context);
        
        // init
        helper.encodeBegin();
        control = helper.getInputControl();
        
        // render components
        if (helper.isRecordReadOnly())
        {
            InputControl.ValueInfo valInfo = helper.getValueInfo(context);
            // render value
            ResponseWriter writer = context.getResponseWriter();
            String tag = writeStartElement(valInfo, writer);
            control.renderValue(valInfo, writer);
            writer.endElement(tag);
        }
        else
        {
            inpInfo = helper.getInputInfo(context);
            // render input
            control.renderInput(this, inpInfo, context, true);
        }
        saveState();
    }

    @Override
    public Object getValue()
    {
        // check for record
        if (helper.getRecord()!=null)
            return helper.getDataValue(true);
        // default
        Object value = super.getValue(); 
        return value; 
    }

    @Override
    public Object getSubmittedValue()
    {   // Check state
        if (control==null || inpInfo==null)
            return null;
        // get Input Value
        return control.getInputValue(this, inpInfo, true);
    }

    @Override
    public void validateValue(FacesContext context, Object value)
    {   // Check state
        if (inpInfo==null)
            return;
        // Validate value
        inpInfo.validate(value);
        setValid(true);
        // call base class 
        super.validateValue(context, value);
    }    
    
    @Override
    public void validate(FacesContext context)
    {
        if (initState(context)==false)
            return;
        // check disabled
        if (inpInfo.isDisabled())
            return;
        // get submitted value and validate
        if (log.isDebugEnabled())
            log.debug("Validating input for {}.", inpInfo.getColumn().getName());
        
        // Validate value
        try {
            // Will internally call getSubmittedValue() and validateValue() 
            super.validate(context);
            
        } catch(Exception e) {
            // Value is not valid
            helper.addErrorMessage(context, e);
            setValid(false);
        }
    }

    @Override
    public void updateModel(FacesContext context)
    {
        if (initState(context)==false)
            return;
        // check disabled
        if (inpInfo.isDisabled())
            return;
        // No Action
        if (!isValid() || !isLocalValueSet())
            return; 
        // super.updateModel(context);
        log.debug("Updating model input for {}.", inpInfo.getColumn().getName());
        inpInfo.setValue(getLocalValue());
        setValue(null);
        setLocalValueSet(false);
    }
    
    public Column getInputColumn()
    {
        return helper.getColumn();
    }
    
    public boolean isInputReadOnly()
    {
        return helper.isRecordReadOnly();
    }
    
    public boolean isInputRequired()
    {
        return helper.isValueRequired();
    }

    /**
     * write value element
     * @param vi
     * @param writer
     * @return
     * @throws IOException
     */
    protected String writeStartElement(InputControl.ValueInfo vi, ResponseWriter writer)
        throws IOException
    {
        // tag and class name
        String tagName = "span";
        String className = helper.getTagStyleClass("eInpDis");
        // other attributes
        Map<String, Object> map = getAttributes();
        Object style = map.get("style");
        Object title = map.get("title");
        // Write tag
        writer.startElement(tagName, this);
        helper.writeAttribute(writer, "class", className);
        helper.writeAttribute(writer, "style", style);
        helper.writeAttribute(writer, "title", helper.getValueTooltip(title));
        return tagName;
    }
}
