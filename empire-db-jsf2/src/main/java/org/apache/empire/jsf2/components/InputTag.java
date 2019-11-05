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
import java.util.List;

import javax.faces.component.NamingContainer;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.component.visit.VisitCallback;
import javax.faces.component.visit.VisitContext;
import javax.faces.context.FacesContext;
import javax.faces.convert.ConverterException;
import javax.faces.view.AttachedObjectHandler;

import org.apache.empire.data.Column;
import org.apache.empire.db.DBRecord;
import org.apache.empire.db.exceptions.FieldIllegalValueException;
import org.apache.empire.exceptions.EmpireException;
import org.apache.empire.exceptions.InvalidArgumentException;
import org.apache.empire.jsf2.controls.InputControl;
import org.apache.empire.jsf2.utils.TagEncodingHelper;
import org.apache.empire.jsf2.utils.TagEncodingHelperFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InputTag extends UIInput implements NamingContainer
{
    // Logger
    private static final Logger       log                = LoggerFactory.getLogger(InputTag.class);

    // private static final String inpControlPropName = InputControl.class.getSimpleName();
    // private static final String inputInfoPropName = InputControl.InputInfo.class.getSimpleName();
    protected static final String     readOnlyState      = "readOnlyState";

    protected final TagEncodingHelper helper             = TagEncodingHelperFactory.create(this, "eInput");

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

    protected void saveState()
    {
        // getStateHelper().put(inpControlPropName, control);
        // getStateHelper().put(inputInfoPropName, inpInfo);
        getStateHelper().put(readOnlyState, (inpInfo == null));
    }

    protected boolean initState(FacesContext context)
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
        helper.prepareData();
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

        // get Control (before checking visible)
        helper.encodeBegin();
        
        // Check visibility
        if (helper.isVisible() == false)
        {   // not visible
            setRendered(false);
            // Check column
            Column column = helper.getColumn();
            if (column==null)
                throw new InvalidArgumentException("column", null);
            // Check record
            Object record = helper.getRecord();
            if (record!=null && (record instanceof DBRecord) && ((DBRecord)record).isValid())
            {   // Check if column exists
                if (((DBRecord)record).getFieldIndex(column)<0)
                    throw new InvalidArgumentException("column", column.getName());
                // not visible
                log.info("Column {} is not visible for record of {} and will not be rendered!", column.getName(), ((DBRecord)record).getRowSet().getName());
            }    
            else
            {   // Record not valid
                log.warn("Invalid Record provided for column {}. Input will not be rendered!", column.getName());
            }    
            return; // not visible
        }

        // render
        this.control = helper.getInputControl();
        this.inpInfo = helper.getInputInfo(context);
        // set required
        if (hasRequiredFlagSet == false)
            super.setRequired(helper.isValueRequired());
        // create input
        if (this.getChildCount()==0)
        {   // create input
            control.createInput(this, inpInfo, context);
            attachEvents(context);
        }
        else
        {   // update state
            control.updateInputState(this, inpInfo, context, true);
        }
        
        // set readonly
        boolean renderValue = helper.isRenderValueComponent();
        setRenderInput(!renderValue);

        // render components
        if (renderValue)
        {   // render value
            String tagName = "span";
            String styleClass = helper.getTagStyleClass("eInpDis");
            String tooltip = helper.getValueTooltip(helper.getTagAttributeValue("title"));
            control.renderValue(this, tagName, styleClass, tooltip, inpInfo, context);
        }
        else
        {   // render input
            control.renderInput(this, inpInfo, context);
        }
        saveState();
    }

    @Override
    public void setId(String id)
    {   // complete
        id = helper.completeInputTagId(id); 
        // setId
        super.setId(id);
    }

    @Override
    public String getId()
    {
        String compId = super.getId();
        // Mojarra-Patch since Id might have been set to "null"
        if ("null".equals(compId))
            compId =  helper.completeInputTagId(null);
        // done
        return compId;
    }

    @Override
    public void processDecodes(FacesContext context) 
    {
        if (helper.isInsideUIData())
        {   // Check input controls
            if (getChildCount()>0)
            {   // Set readOnly and disabled for each row
                boolean readOnly = helper.isRecordReadOnly();
                setRenderInput(!readOnly);
                // get control
                helper.prepareData();
                if (control==null)
                    control = helper.getInputControl();
                if (inpInfo==null)
                    inpInfo = helper.getInputInfo(context);
                // update control
                control.updateInputState(this, inpInfo, context, false);
            }
        }
        // default
        super.processDecodes(context);
    }

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

    protected void setRenderInput(boolean renderInput)
    {
        for (UIComponent child : getChildren())
        {
            if (child.isRendered()!=renderInput)
            {
                if (log.isDebugEnabled())
                    log.debug("Changing UIInput rendered state for {} to {}", helper.getColumnName(), renderInput);
                child.setRendered(renderInput);
            }    
        }
    }
    
    protected boolean isPartialSubmit(FacesContext context)
    {
        // Check Required Flag
        if (hasRequiredFlagSet && !isRequired())
            return true;
        // partial  
        return helper.isPartialSubmit(context);
    }
    
    protected void attachEvents(FacesContext context)
    {
        // Events available?
        @SuppressWarnings("unchecked")
        List<AttachedObjectHandler> result = (List<AttachedObjectHandler>) getAttributes().get("javax.faces.RetargetableHandlers");
        if (result == null)
        {
            return;
        }
        UIInput inputComponent = null;
        for (UIComponent c : getChildren())
        {
            if (c instanceof UIInput)
            {   // found
                inputComponent = (UIInput)c;
                break;
            }
        }
        if (inputComponent == null)
            return;
        // Attach Events
        for (AttachedObjectHandler aoh : result)
        {
            aoh.applyAttachedObject(context, inputComponent);
        }
        // remove
        result.clear();
        getAttributes().remove("javax.faces.RetargetableHandlers");
    }
}
