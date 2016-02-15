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
import javax.faces.component.visit.VisitCallback;
import javax.faces.component.visit.VisitContext;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.convert.ConverterException;

import org.apache.empire.data.Column;
import org.apache.empire.db.exceptions.FieldIllegalValueException;
import org.apache.empire.exceptions.EmpireException;
import org.apache.empire.jsf2.controls.InputControl;
import org.apache.empire.jsf2.utils.TagEncodingHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InputTag extends UIInput implements NamingContainer
{
    // Logger
    private static final Logger       log                = LoggerFactory.getLogger(InputTag.class);

    // private static final String inpControlPropName = InputControl.class.getSimpleName();
    // private static final String inputInfoPropName = InputControl.InputInfo.class.getSimpleName();
    private static final String       readOnlyState      = "readOnlyState";

    protected final TagEncodingHelper helper             = new TagEncodingHelper(this, "eInput");

    protected InputControl            control            = null;
    protected InputControl.InputInfo  inpInfo            = null;
    protected boolean                 hasRequiredFlagSet = false;

    /*
    private static int itemIdSeq = 0;
    private final int itemId;
    */

    public InputTag()
    {
        super();
        // Debug stuff
        /*
        itemId = ++itemIdSeq;
        if (log.isDebugEnabled())
            log.debug("InputTag {} created", itemId);
        */
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
        getStateHelper().put(readOnlyState, (inpInfo == null));
    }

    private boolean initState(FacesContext context)
    {
        // Check visibility
        if (helper.isVisible() == false)
            return false; // not visible
        // Read only State
        Boolean ros = (Boolean) getStateHelper().get(readOnlyState);
        if (ros != null && ros.booleanValue())
            return false;
        // Must have children
        if (getChildCount() == 0)
        {   log.warn("InputTag has no children! Unable to init Input state for id={}", getClientId());
            log.warn("Problem might be related to Mojarra's state context saving for dynamic components (affects all versions > 2.1.6). See com.sun.faces.context.StateContext.java:AddRemoveListener");
            return false;
        }
        // Init Control and inputInfo;
        control = helper.getInputControl();
        inpInfo = helper.getInputInfo(context);
        return (control != null && inpInfo != null);
    }

    /**
     * remember original clientId
     * necessary only inside UIData
     */
    private String treeClientId = null;

    @Override
    public boolean visitTree(VisitContext visitContext, VisitCallback callback)
    {
        FacesContext context = visitContext.getFacesContext();
        treeClientId = getClientId(context);
        return super.visitTree(visitContext, callback);
    }

    @Override
    public String getClientId(FacesContext context)
    {
        // Check if dynamic components are being created
        if (this.treeClientId != null && control != null && control.isCreatingComponents())
        { // return the original tree client id
            return treeClientId;
        }
        // default behavior
        return super.getClientId(context);
    }

    @Override
    public void encodeBegin(FacesContext context)
        throws IOException
    {
        // add label and input components when the view is loaded for the first time
        super.encodeBegin(context);

        // Check visiblity
        if (helper.isVisible() == false)
        {   setRendered(false);
            return; // not visible
        }

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
            // set required
            if (hasRequiredFlagSet == false)
                super.setRequired(helper.isValueRequired());
            // render input
            control.renderInput(this, inpInfo, context, true);
        }
        saveState();
    }

    @Override
    public void setId(String id)
    {
        super.setId(id);
        // reset record
        helper.setRecord(null);
    }

    /*
    @Override
    public void processDecodes(FacesContext context) 
    {
        if (helper.isInsideUIData())
        {   // Check input controls
            if (getChildCount()>0)
            {   // Change readOnly and disabled too
                boolean readOnly = helper.isRecordReadOnly();
                log.info("Changing UIInput readOnly state for {} to {}", helper.getColumnName(), readOnly);
                if (control==null)
                    control = helper.getInputControl();
                if (inpInfo==null)
                    inpInfo = helper.getInputInfo(context);
                control.updateInputState(this, inpInfo, context);
            }
        }
        // default
        super.processDecodes(context);
    }
    */

    @Override
    public void setRequired(boolean required)
    {
        super.setRequired(required);
        // flag has been set
        hasRequiredFlagSet = true;
    }

    @Override
    public Object getValue()
    {
        // check for record
        if (helper.getRecord() != null)
            return helper.getDataValue(true);
        // default
        Object value = super.getValue();
        return value;
    }

    @Override
    public Object getSubmittedValue()
    { // Check state
        if (control == null || inpInfo == null || helper.isReadOnly())
            return null;
        // get Input Value
        return control.getInputValue(this, inpInfo, true);
    }

    @Override
    protected Object getConvertedValue(FacesContext context, Object newSubmittedValue)
        throws ConverterException
    {   // Check state
        if (control == null || inpInfo == null || helper.isReadOnly())
            return null;
        // parse and convert value
        return this.control.getConvertedValue(this, this.inpInfo, newSubmittedValue);
    }

    @Override
    public void validateValue(FacesContext context, Object value)
    { // Check state
        if (inpInfo == null)
            return;
        // Skip Null values if not required
        if (UIInput.isEmpty(value) && isPartialSubmit(context)) //  && helper.isValueRequired()
        { // Value is null
            log.debug("Skipping validation for {} due to Null value.", inpInfo.getColumn().getName());
            return;
        }
        // Validate value
        inpInfo.validate(value);
        setValid(true);
        // don't call base class!
        // super.validateValue(context, value);
    }

    @Override
    public void validate(FacesContext context)
    {
        if (initState(context) == false)
            return;
        // get submitted value and validate
        if (log.isDebugEnabled())
            log.debug("Validating input for {}.", inpInfo.getColumn().getName());

        // Validate value
        try
        { // Will internally call getSubmittedValue() and validateValue() 
            super.validate(context);

        } catch (Exception e) {
            // Value is not valid
            if (!(e instanceof EmpireException))
                e = new FieldIllegalValueException(helper.getColumn(), "", e);
            // Add error message
            helper.addErrorMessage(context, e);
            setValid(false);
        }
    }

    @Override
    public void updateModel(FacesContext context)
    {
        if (initState(context) == false)
            return;
        // No Action
        if (!isValid() || !isLocalValueSet())
            return;
        // check required
        Object value = getLocalValue();
        if (UIInput.isEmpty(value) && isPartialSubmit(context) && !helper.isTempoaryNullable())
        { // Value is null, but required
            log.debug("Skipping model update for {} due to Null value.", inpInfo.getColumn().getName());
            return;
        }
        // super.updateModel(context);
        log.debug("Updating model input for {}.", inpInfo.getColumn().getName());
        inpInfo.setValue(value);
        setValue(null);
        setLocalValueSet(false);
        // Post update
        control.postUpdateModel(this, inpInfo, context);
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

    private boolean isPartialSubmit(FacesContext context)
    {
        // Check Required Flag
        if (hasRequiredFlagSet && !isRequired())
            return true;
        // partial  
        return helper.isPartialSubmit(context);
    }

    /**
     * write value element
     * 
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
