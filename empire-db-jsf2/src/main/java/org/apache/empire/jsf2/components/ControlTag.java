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
import javax.faces.component.UIComponentBase;
import javax.faces.component.UIInput;
import javax.faces.component.UINamingContainer;
import javax.faces.component.html.HtmlOutputLabel;
import javax.faces.component.visit.VisitCallback;
import javax.faces.component.visit.VisitContext;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.convert.ConverterException;

import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.data.Column;
import org.apache.empire.db.exceptions.FieldIllegalValueException;
import org.apache.empire.exceptions.EmpireException;
import org.apache.empire.jsf2.controls.InputControl;
import org.apache.empire.jsf2.utils.ControlRenderInfo;
import org.apache.empire.jsf2.utils.TagEncodingHelper;
import org.apache.empire.jsf2.utils.TagEncodingHelperFactory;
import org.apache.empire.jsf2.utils.TagStyleClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ControlTag extends UIInput implements NamingContainer
{
    /**
     * ControlSeparatorComponent
     */
    public static abstract class ControlSeparatorComponent extends javax.faces.component.UIComponentBase
    {
        private ControlTag control = null;
        
        protected String tagName = null;

        protected abstract String getTagName(ControlRenderInfo renderInfo);
        
        protected abstract void writeAttributes(ResponseWriter writer, TagEncodingHelper helper)
            throws IOException;

        @Override
        public String getFamily()
        {
            return UINamingContainer.COMPONENT_FAMILY;
        }

        protected final ControlTag getControl()
        {
            return control;
        }

        @Override
        public void encodeBegin(FacesContext context)
            throws IOException
        {
            super.encodeBegin(context);

            UIComponent parent = getParent();
            if (!(parent instanceof ControlTag))
                parent = parent.getParent();
            if (!(parent instanceof ControlTag))
            {   log.error("Invalid parent component for " + getClass().getName());
                return;
            }

            this.control = (ControlTag) parent;
            this.tagName = getTagName(control.renderInfo);

            // Start
            if (tagName!=null && tagName.length()>0) 
            {   // render tag
                ResponseWriter writer = context.getResponseWriter();
                writer.startElement(tagName, this);
                writeAttributes(writer, this.control.helper);
            }
        }
        
        @Override
        public void encodeEnd(FacesContext context)
            throws IOException
        {
            if (tagName!=null && tagName.length()>0) 
            {   // render tag
                ResponseWriter writer = context.getResponseWriter();
                writer.endElement(tagName);
            }
            // call default
            super.encodeEnd(context);
        }
        
        @Override
        public boolean getRendersChildren()
        {
            // return super.getRendersChildren();
            return control.helper.isVisible();
        }
    }

    public static class LabelSeparatorComponent extends ControlSeparatorComponent
    {
        @Override
        protected String getTagName(ControlRenderInfo renderInfo)
        {
            return renderInfo.LABEL_WRAPPER_TAG;
        }
        
        @Override
        protected void writeAttributes(ResponseWriter writer, TagEncodingHelper helper)
            throws IOException
        {
            // style Class
            String labelClass = helper.getTagAttributeStringEx("labelClass");
            helper.writeStyleClass(writer, TagStyleClass.CONTROL_LABEL.get(), labelClass);
        }

        @Override
        public void encodeChildren(FacesContext context)
            throws IOException
        {
            // encode label component
            getControl().encodeLabel(context, this);
            // now render
            super.encodeChildren(context);
        }
    }

    public static class InputSeparatorComponent extends ControlSeparatorComponent
    {
        @Override
        protected String getTagName(ControlRenderInfo renderInfo)
        {
            return renderInfo.INPUT_WRAPPER_TAG;
        }

        @Override
        protected void writeAttributes(ResponseWriter writer, TagEncodingHelper helper)
            throws IOException
        {
            // style Class
            String inputClass = helper.getTagAttributeStringEx("inputClass");
            helper.writeStyleClass(writer, TagStyleClass.CONTROL_INPUT.get(), inputClass);
            // colspan
            String colSpan = tagName.equalsIgnoreCase(InputControl.HTML_TAG_TD) ? helper.getTagAttributeStringEx("colspan") : null;            
            if (colSpan!=null)
                writer.writeAttribute("colspan", colSpan, null);
        }

        @Override
        public void encodeChildren(FacesContext context)
            throws IOException
        {
            // encode input components
            getControl().encodeInput(context, this);
            // don't call super.encodeChildren()!
        }
    }

    public static class ValueOutputComponent extends javax.faces.component.UIComponentBase
    {
        private final String tagName = "span";

        /*
        public ValueOutputComponent()
        {
            if (log.isTraceEnabled())
                log.trace("ValueOutputComponent created.");
        } 
        */

        @Override
        public String getFamily()
        {
            return UINamingContainer.COMPONENT_FAMILY;
        }

        @Override
        public void encodeBegin(FacesContext context)
            throws IOException
        {
            super.encodeBegin(context);

            UIComponent parent = getParent();
            if (!(parent instanceof ControlTag))
                parent = parent.getParent();
            if (!(parent instanceof ControlTag))
                parent = parent.getParent();
            if (!(parent instanceof ControlTag))
            {   log.error("Invalid parent component for " + getClass().getName());
                return;
            }

            ControlTag controlTag = (ControlTag) parent;
            InputControl control = controlTag.control;
            InputControl.ValueInfo valInfo = controlTag.inpInfo;

            TagEncodingHelper helper = controlTag.helper;
            if (control == null)
                control = helper.getInputControl(); // Oops, should not come here 
            if (valInfo == null)
                valInfo = helper.getValueInfo(context); // Oops, should not come here 

            String styleClass = helper.getTagStyleClass(TagStyleClass.INPUT_DIS.get());
            String tooltip = helper.getValueTooltip(helper.getTagAttributeValue("title"));

            // render components
            control.renderValue(this, this.tagName, styleClass, tooltip, valInfo, context);
        }
    }

    // Logger
    private static final Logger       log                  = LoggerFactory.getLogger(ControlTag.class);

    protected static final String     readOnlyState        = "readOnlyState";

    protected final TagEncodingHelper helper               = TagEncodingHelperFactory.create(this, TagStyleClass.INPUT.get()); // Must be "INPUT" not "CONTROL"!

    protected InputControl            control              = null;
    protected InputControl.InputInfo  inpInfo              = null;
    protected ControlRenderInfo       renderInfo           = null;
    protected boolean                 hasRequiredFlagSet   = false;
    protected boolean                 encodeLabel          = true;
    private boolean                   creatingComponents   = false;

    public ControlTag()
    {
        super();
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
        getStateHelper().put(ControlTag.readOnlyState, (this.inpInfo == null));
    }

    protected boolean initState(FacesContext context)
    {
        // Check read-Only
        Boolean ros = (Boolean) getStateHelper().get(ControlTag.readOnlyState);
        if (ros != null && ros.booleanValue())
            return false;
        // Must have children        
        if (getChildCount() == 0)
        {   log.warn("ControlTag '{}' has no children. encodeBegin may not have been called yet.", getClientId());
            return false;
        }
        // control = ;
        helper.prepareData();
        this.control = helper.getInputControl();
        this.inpInfo = helper.getInputInfo(context);
        return (this.control != null && this.inpInfo != null);
    }

    /**
     * remember original clientId
     * necessary only inside UIData
     */
    protected String treeClientId = null;
    
    @Override
    public boolean visitTree(VisitContext visitContext, VisitCallback callback) 
    {
        FacesContext context = visitContext.getFacesContext();
        treeClientId = this.getClientId(context);
        return super.visitTree(visitContext, callback);
    }

    @Override
    public String getClientId(FacesContext context)
    {
        // Check if dynamic components are being created
        if (this.treeClientId!=null && (this.creatingComponents || this.control!=null && this.control.isCreatingComponents()))
        {   // return the original tree client id
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
        
        // renderInfo
        if (this.renderInfo==null)
            this.renderInfo=helper.getControlRenderInfo();

        // init
        helper.encodeBegin();
        this.control = helper.getInputControl();
        boolean isCustomInput = isCustomInput();
        
        // encodeBegin
        if (renderInfo.CONTROL_TAG!=null)
        {   // control wrapper tag
            ResponseWriter writer = context.getResponseWriter();
            writer.startElement(renderInfo.CONTROL_TAG, this);
            // render id
            helper.writeComponentId(writer, renderInfo.RENDER_AUTO_ID);
            // style class
            String controlClass = helper.getTagAttributeStringEx("controlClass"); 
            String styleClass   = helper.getControlContextStyleClass(); 
            helper.writeStyleClass(writer, TagStyleClass.CONTROL.get(), controlClass, styleClass);
        }
        
        // LabelSeparatorComponent
        if (this.encodeLabel)
        {   // Create Label Separator Tag
            ControlSeparatorComponent labelSepTag = null;
            if (getChildCount() > 0)
                labelSepTag = (ControlSeparatorComponent) getChildren().get(0);
            if (labelSepTag == null)
            {   try {
                    creatingComponents = true;
                    labelSepTag = new LabelSeparatorComponent();
                    getChildren().add(labelSepTag);
                    helper.resetComponentId(labelSepTag);
                } finally {
                    creatingComponents = false;
                }
            }
            // encode
            labelSepTag.encodeAll(context);
        }
        
        // InputSeparatorComponent
        if (!isCustomInput)
        {   // Create Input Separator Tag
            ControlSeparatorComponent inputSepTag = null;
            if (getChildCount() > 1)
                inputSepTag = (ControlSeparatorComponent) getChildren().get(1);
            if (inputSepTag == null)
            {   try {
                    creatingComponents = true;
                    inputSepTag = new InputSeparatorComponent();
                    getChildren().add(inputSepTag);
                    helper.resetComponentId(inputSepTag);
                } finally {
                    creatingComponents = false;
                }
            }
            // encode
            inputSepTag.encodeAll(context);
        }
        // done
        saveState();
    }

    @Override
    public boolean getRendersChildren()
    {
        return true;
    }

    @Override
    public void encodeChildren(FacesContext context)
        throws IOException
    {
        if (isRendered() && isCustomInput())
        {   // Custom input
            ResponseWriter writer = null;
            String tagName  = renderInfo.INPUT_WRAPPER_TAG;
            if (tagName!=null && tagName.length()>0)
            {   // render tag
                writer = context.getResponseWriter();
                writer.startElement(tagName, this);
                // style Class
                String inpClass = helper.getTagAttributeStringEx("inputClass");
                helper.writeStyleClass(writer, TagStyleClass.CONTROL_INPUT.get(), inpClass);
                // write more
                String colSpan = tagName.equalsIgnoreCase(InputControl.HTML_TAG_TD) ? helper.getTagAttributeStringEx("colspan") : null;
                if (colSpan!=null)
                    writer.writeAttribute("colspan", colSpan, null);
            }
            // encode children
            super.encodeChildren(context);
            // end of element
            if (writer!=null)
            {   // render tag
                writer.endElement(tagName);
            }
        }
    }

    @Override
    public void encodeEnd(FacesContext context)
        throws IOException
    {
        // call base
        super.encodeEnd(context);
        // encodeEnd
        if (renderInfo.CONTROL_TAG!=null)
        {   // control wrapper tag
            ResponseWriter writer = context.getResponseWriter();
            writer.endElement(renderInfo.CONTROL_TAG);
        }
    }
    
    @Override
    public void setId(String id) 
    {   // complete
        id = helper.completeInputTagId(id); 
        // set
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
    public void setRequired(boolean required)
    {
        super.setRequired(required);
        // flag has been set
        this.hasRequiredFlagSet = true;
    }

    public boolean isCustomInput()
    {
        Object custom = getAttributes().get("custom");
        if (custom != null)
            return ObjectUtils.getBoolean(custom);
        return false;
    }
    
    /**
     * called from LabelSeparatorComponent
     * @param context the faces context
     * @param parent the LabelSeparatorComponent
     * @throws IOException from ResponseWriter
     */
    protected void encodeLabel(FacesContext context, UIComponentBase parent)
        throws IOException
    {
        UIComponent labelFacet = getFacet("label");
        if (labelFacet!=null)
        {   // label facet
            labelFacet.encodeAll(context);
        }
        else
        {   // render components
            try {
                creatingComponents = true;
                HtmlOutputLabel labelComponent = null;
                if (parent.getChildCount() > 0)
                {
                    labelComponent = (HtmlOutputLabel) parent.getChildren().get(0);
                    // update
                    helper.updateLabelComponent(context, labelComponent, null);
                }
                if (labelComponent == null)
                {
                    String forInput = isCustomInput() ? helper.getTagAttributeString("for") : "*";
                    // createLabelComponent 
                    labelComponent = helper.createLabelComponent(context, forInput, "eLabel", null, getColon());
                    parent.getChildren().add(0, labelComponent);
                    helper.resetComponentId(labelComponent);
                }
            } finally {
                creatingComponents = false;
            }
        }
    }

    /**
     * called from InputSeparatorComponent
     * @param context the faces context
     * @param parent the InputSeparatorComponent
     * @throws IOException from ResponseWriter
     */
    protected void encodeInput(FacesContext context, UIComponent parent)
        throws IOException
    {
        // render components
        try {
            creatingComponents = true;
            // check children
            int count = parent.getChildCount();
            UIComponent valueComp = (count>0 ? parent.getChildren().get(count-1) : null);
            boolean resetChildId = (count==0);
            // continue
            this.inpInfo = helper.getInputInfo(context);
            // set required
            if (this.hasRequiredFlagSet==false)
                super.setRequired(helper.isValueRequired());
	        // create Input Controls
            // boolean recordReadOnly = helper.isRecordReadOnly();
            if (count==0)
            {   // Create components
                control.createInput(parent, inpInfo, context);
                // create Value Component
                if (valueComp == null)
                {   // create ValueOutputComponent
                    valueComp = new ValueOutputComponent();
                    parent.getChildren().add(valueComp);
                }
            }
            else
            {   // Update
                control.updateInputState(parent, inpInfo, context, context.getCurrentPhaseId());
            }
            // set rendered
            boolean renderValue = helper.isRenderValueComponent();
            List<UIComponent> children = parent.getChildren();
            for (UIComponent child : children)
            {   // reset child id
                if (resetChildId && child.getId()!=null)
                    child.setId(child.getId());
                // set rendered
                boolean valueOutput = (child instanceof ValueOutputComponent);
                child.setRendered((valueOutput ? renderValue : !renderValue));
            }
            // wrapperTag
            String wrapperTag = helper.writeWrapperTag(context, false, renderValue); 
            // render
            control.renderInput(parent, inpInfo, context);
            // wrapperTagEnd
            if (wrapperTag!=null)
            {   // control wrapper tag
                ResponseWriter writer = context.getResponseWriter();
                writer.endElement(wrapperTag);
            }
        } finally {
            creatingComponents = false;
        }
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
        if (this.control == null || this.inpInfo == null || helper.isReadOnly())
            return null;
        // Get Input Tag
        if (getChildCount() <= 1)
            return null;
        // get Input Value
        ControlSeparatorComponent inputSepTag = (ControlSeparatorComponent) getChildren().get(1);
        return this.control.getInputValue(inputSepTag, this.inpInfo, true);
    }
    
    @Override
    protected Object getConvertedValue(FacesContext context, Object newSubmittedValue)
        throws ConverterException
    { // Check state
        if (this.control == null || this.inpInfo == null || helper.isReadOnly())
            return null;
        // Get Input Tag
        if (getChildCount() <= 1)
            return null;
        // get Input Value
        ControlSeparatorComponent inputSepTag = (ControlSeparatorComponent) getChildren().get(1);
        return this.control.getConvertedValue(inputSepTag, this.inpInfo, newSubmittedValue);
    }

    @Override
    public void processDecodes(FacesContext context) 
    {
        // check UI-Data
        if (helper.isInsideUIData() && getChildCount()>0)
        {   // update input state
            updateControlInputState(context);
            // render Input or Value component
            boolean readOnly = helper.isRecordReadOnly();
            setRenderInput(!readOnly);
        }
        // default
        super.processDecodes(context);
    }

    @Override
    public void processValidators(FacesContext context)
    {
        // check UI-Data
        if (helper.isInsideUIData() && getChildCount()>0)
        {   // update input state
            updateControlInputState(context);
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
            log.debug("Validating input for {}.", this.inpInfo.getColumn().getName());
        // Validate value
        try
        {   // Will internally call getSubmittedValue() and validateValue() 
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
    public void validateValue(FacesContext context, Object value)
    { // Check state
        if (this.inpInfo == null || !isValid())
            return;
        // Skip Null values on partial submit
        if (UIInput.isEmpty(value) && isPartialSubmit(context)) //  && helper.isValueRequired()
        { // Value is null
            log.debug("Skipping validation for {} due to Null value.", this.inpInfo.getColumn().getName());
            return;
        }
        // Validate value
        this.inpInfo.validate(value);
        setValid(true);
        // don't call base class!
        // super.validateValue(context, value);
    }

    @Override
    public void updateModel(FacesContext context)
    {
        if (initState(context) == false)
            return;
        // No Action
        if (!isValid() || !isLocalValueSet())
            return;
        // check required?
        Object value = getLocalValue();
        // check required
        if (UIInput.isEmpty(value) && isPartialSubmit(context) && !helper.isTempoaryNullable())
        { // Value is null, but required
            log.debug("Skipping model update for {} due to Null value.", this.inpInfo.getColumn().getName());
            return;
        }
        // super.updateModel(context);
        log.debug("Updating model input for {}.", this.inpInfo.getColumn().getName());
        this.inpInfo.setValue(value);
        setValue(null);
        setLocalValueSet(false);
        // Post update
        ControlSeparatorComponent inputSepTag = (ControlSeparatorComponent) getChildren().get(1);
        this.control.postUpdateModel(inputSepTag, this.inpInfo, context);
    }

    public InputControl getInputControl()
    {
        return this.control;
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
    
    protected void updateControlInputState(FacesContext context)
    {
        // get control
        helper.prepareData();
        if (control==null)
            control = helper.getInputControl();
        if (inpInfo==null)
            inpInfo = helper.getInputInfo(context);
        // update control
        ControlSeparatorComponent inputSepTag = (ControlSeparatorComponent) getChildren().get(1);
        control.updateInputState(inputSepTag, inpInfo, context, context.getCurrentPhaseId());
    }

    protected void setRenderInput(boolean renderInput)
    {
        if (getChildCount()>1 && (getChildren().get(1) instanceof InputSeparatorComponent))
        {   // Make sure all inputs are rendered
            boolean hasChanged = false;
            InputSeparatorComponent parent = (InputSeparatorComponent)getChildren().get(1);
            // set rendered of children
            for (UIComponent child : parent.getChildren())
            {   // set rendered 
                boolean rendered = (child instanceof ValueOutputComponent) ? renderInput : !renderInput;
                if (child.isRendered()!=rendered)
                {   child.setRendered(rendered);
                    hasChanged = true;
                }    
            }
            // give control chance to update
            if (hasChanged && log.isDebugEnabled())
                log.debug("Changing UIInput readOnly state for {} to {}", helper.getColumnName(), renderInput);
        }
        else
        {   // Must have at least two children, for label and input
            log.warn("Control-Tag does not have separate Label and Input components");
        }
    }
    
    protected boolean getColon()
    {
        Object colon = getAttributes().get("colon");
        if (colon!=null)
            return ObjectUtils.getBoolean(colon);
        return true;
    }

    protected boolean isPartialSubmit(FacesContext context)
    {
        // Check Required Flag
        if (this.hasRequiredFlagSet && !isRequired())
            return true;
        // partial  
        return helper.isPartialSubmit(context);
    }    
}
