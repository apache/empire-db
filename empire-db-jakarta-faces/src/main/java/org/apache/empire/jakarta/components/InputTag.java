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
package org.apache.empire.jakarta.components;

import java.io.IOException;
import java.util.List;

import org.apache.empire.data.Column;
import org.apache.empire.jakarta.controls.InputControl;
import org.apache.empire.jakarta.utils.TagEncodingHelper;
import org.apache.empire.jakarta.utils.TagEncodingHelperFactory;
import org.apache.empire.jakarta.utils.TagStyleClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.component.NamingContainer;
import jakarta.faces.component.UIComponent;
import jakarta.faces.component.UIInput;
import jakarta.faces.component.visit.VisitCallback;
import jakarta.faces.component.visit.VisitContext;
import jakarta.faces.context.FacesContext;
import jakarta.faces.context.ResponseWriter;
import jakarta.faces.convert.ConverterException;
import jakarta.faces.view.AttachedObjectHandler;

public class InputTag extends UIInput implements NamingContainer
{
    // Logger
    private static final Logger       log                    = LoggerFactory.getLogger(InputTag.class);

    // private static final String inpControlPropName = InputControl.class.getSimpleName();
    // private static final String inputInfoPropName = InputControl.InputInfo.class.getSimpleName();
    protected static final String     readOnlyState          = "readOnlyState";

    protected final TagEncodingHelper helper                 = TagEncodingHelperFactory.create(this, TagStyleClass.INPUT.get());

    protected InputControl            control                = null;
    protected InputControl.InputInfo  inpInfo                = null;
    protected boolean                 submittedValueDetected = false;
    protected Object                  submittedValue;
    protected boolean                 valueValidated         = false;

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
        return "jakarta.faces.NamingContainer";
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
        helper.prepareData();
        if (helper.isVisible() == false)
            return false; // not visible
        // Read only State
        Boolean ros = (Boolean) getStateHelper().get(readOnlyState);
        if (ros != null && ros.booleanValue())
            return false;
        // Must have children
        if (getChildCount() == 0)
        {   log.warn("InputTag '{}' has no children. encodeBegin may not have been called yet.", getClientId());
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
    public boolean getRendersChildren()
    {
        // render children ourselves using 
        //     encodeChildren(FacesContext context)
        return true;
    }

    @Override
    public void encodeBegin(FacesContext context)
        throws IOException
    {
        super.encodeBegin(context);

        // get Control (before checking visible)
        helper.encodeBegin();
        
        // create child components
        this.control = helper.getInputControl();
        this.inpInfo = helper.getInputInfo(context);
        // set required
        super.setRequired(helper.isValueRequired());
        // create input
        if (this.getChildCount()==0)
        {   // create input
            control.createInput(this, inpInfo, context);
            attachEvents(context);
        }
        else
        {   // update state
            control.updateInputState(this, inpInfo, context, context.getCurrentPhaseId());
        }
        
        // Set Render Input
        boolean controlVisible = helper.isVisible();
        boolean renderValue = helper.isRenderValueComponent();
        setRenderInput(!renderValue && controlVisible);
        // Render when visible
        if (controlVisible)
        {   // Render now
            String wrapperTag = helper.writeWrapperTag(context, true, renderValue);
            // render components
            if (renderValue)
            {   // render value
                String tagName = "span";
                String styleClass = helper.getTagStyleClass(TagStyleClass.INPUT_DIS.get());
                String tooltip = helper.getValueTooltip(helper.getTagAttributeValue("title"));
                control.renderValue(this, tagName, styleClass, tooltip, inpInfo, context);
            }
            else
            {   // render input
                control.renderInput(this, inpInfo, context);
            }
            // wrapperTagEnd
            if (wrapperTag!=null)
            {   // control wrapper tag
                ResponseWriter writer = context.getResponseWriter();
                writer.endElement(wrapperTag);
            }
        }
        saveState();
    }

    @Override
    public void encodeChildren(FacesContext context)
        throws IOException
    {
        // Ignore this
        // super.encodeChildren(context);
        log.debug("InputTag:encodeChildren is ignored");
    }

    @Override
    public void encodeEnd(FacesContext context)
        throws IOException
    {
        // call base
        super.encodeEnd(context);
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
    public void setId(String id)
    {   // complete
        id = helper.completeInputTagId(id); 
        // setId
        super.setId(id);
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
    {   // Already detected
        if (this.submittedValueDetected)
            return this.submittedValue; // already detected
        // Check state
        if (control == null || inpInfo == null || helper.isReadOnly())
            return null;
        // get Input Value
        this.submittedValue = control.getInputValue(this, inpInfo, true);
        this.submittedValueDetected = true;
        return this.submittedValue; 
    }
    
    @Override
    public void setSubmittedValue(Object submittedValue)
    {
        // super.setSubmittedValue(submittedValue);
        this.submittedValue = submittedValue;
        this.submittedValueDetected = true;
    }

    @Override
    protected Object getConvertedValue(FacesContext context, Object newSubmittedValue)
        throws ConverterException
    {   // Check state
        if (control == null || inpInfo == null || helper.isReadOnly())
            return null;
        // convert value
        try {
            // parse and convert value
            return this.control.getConvertedValue(this, this.inpInfo, newSubmittedValue);
        } catch (Exception e) {
            // Add error message
            FacesMessage msg = helper.getFieldValueErrorMessage(context, e, newSubmittedValue);
            throw new ConverterException(msg);
        }
    }

    @Override
    public void processDecodes(FacesContext context) 
    {
        // check UI-Data
        if (helper.isInsideUIData() && getChildCount()>0)
        {   // update input state
            updateControlInputState(context);
            // Set Rendered for children (changed 2024-04-05)
            // boolean readOnly = helper.isRecordReadOnly();
            boolean renderValue = helper.isRenderValueComponent();
            setRenderInput(!renderValue);
        }
        // default
        super.processDecodes(context);
    }

    @Override
    public void processValidators(FacesContext context)
    {
        resetSubmittedValue();
        // check UI-Data
        if (helper.isInsideUIData() && getChildCount()>0)
        {   // update input state
            updateControlInputState(context);
            // Only if value was submitted
            boolean hasValue = (getSubmittedValue()!=null);
            setRenderInput(hasValue);
        }
        // process all validators (including children)
        super.processValidators(context);
    }

    @Override
    public void validate(FacesContext context)
    {   // init state
        if (initState(context) == false)
            return;
        // get submitted value and validate
        if (log.isDebugEnabled())
            log.debug("Validating input for {}.", inpInfo.getColumn().getName());
        // reset again (important!)
        resetSubmittedValue();
        // Will internally call getSubmittedValue() and validateValue()
        this.valueValidated = false;
        super.validate(context);
        // post check
        if (!this.valueValidated && !helper.isReadOnly())
        {   // New since 2024-04-04
            // Validate Record value
            Object value = helper.getDataValue(true);
            log.warn("No Submitted value for {}. Validating Record value of \"{}\" instead.", helper.getColumnName(), value);
            if (value!=null)
                validateValue(context, value);
        }
    }
    
    @Override
    public void validateValue(FacesContext context, Object value)
    {   // Validate value
        try
        {   // Check state
            if (inpInfo == null)
                return;
            // validateValue
            if (helper.beginValidateValue(context, value))
                inpInfo.validate(value);
            setValid(true);
            // don't call base class!
            // super.validateValue(context, value);

        } catch (Exception e) {
            // Add error message
            helper.addFieldValueErrorMessage(context, e, value);
            setValid(false);

        } finally {
            this.valueValidated = true;
        }
    }
    
    @Override
    public void processUpdates(FacesContext context)
    {
        // Added for EMPIREDB-436 on 2024-09-27
        try
        {   
            // setCachedFacesContext(context);
            pushComponentToEL(context, this);
            if (!isRendered())
            {
                return;
            }
            try
            {
                updateModel(context);
            }
            catch (RuntimeException e)
            {
                context.renderResponse();
                throw e;
            }
            if (!isValid())
            {
                context.renderResponse();
            }
        }
        finally
        {
            // setCachedFacesContext(null);
            popComponentFromEL(context);
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
        // updateModel
        Object value = getLocalValue();
        if (helper.beginUpdateModel(context, value)) 
        {   // don't call base class!
            // super.updateModel(context);
            log.debug("Updating model input for {}.", this.inpInfo.getColumn().getName());
            this.inpInfo.setValue(value);
        }
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
    
    protected void resetSubmittedValue()
    {
        submittedValueDetected = false;
        submittedValue = null;
    }
    
    protected void updateControlInputState(FacesContext context)
    {
        // get control
        helper.prepareData();
        if (control==null)
            control = helper.getInputControl();
        if (inpInfo==null)
            inpInfo = helper.getInputInfo(context);
        // update control
        control.updateInputState(this, inpInfo, context, context.getCurrentPhaseId());
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
    
    protected void attachEvents(FacesContext context)
    {
        // Events available?
        @SuppressWarnings("unchecked")
        List<AttachedObjectHandler> result = (List<AttachedObjectHandler>) getAttributes().get("jakarta.faces.RetargetableHandlers");
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
        getAttributes().remove("jakarta.faces.RetargetableHandlers");
    }
}
