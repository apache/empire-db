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

import jakarta.faces.component.NamingContainer;
import jakarta.faces.component.UIComponent;
import jakarta.faces.component.UIComponentBase;
import jakarta.faces.component.UIInput;
import jakarta.faces.component.UINamingContainer;
import jakarta.faces.component.html.HtmlOutputLabel;
import jakarta.faces.component.visit.VisitCallback;
import jakarta.faces.component.visit.VisitContext;
import jakarta.faces.context.FacesContext;
import jakarta.faces.context.ResponseWriter;
import jakarta.faces.convert.ConverterException;

import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.data.Column;
import org.apache.empire.db.exceptions.FieldIllegalValueException;
import org.apache.empire.exceptions.EmpireException;
import org.apache.empire.jakarta.controls.InputControl;
import org.apache.empire.jakarta.utils.ControlRenderInfo;
import org.apache.empire.jakarta.utils.TagEncodingHelper;
import org.apache.empire.jakarta.utils.TagEncodingHelperFactory;
import org.apache.empire.jakarta.utils.TagStyleClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ControlTag extends UIInput implements NamingContainer
{
    /**
     * ControlSeparatorComponent
     */
    public static abstract class ControlSeparatorComponent extends jakarta.faces.component.UIComponentBase
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

            // Get Control and TagName
            this.control = (ControlTag) parent;
            this.tagName = (this.control.controlVisible ? getTagName(control.renderInfo) : null);

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
            // String inputState =(helper.isRenderValueComponent() ? TagStyleClass.INPUT_DIS.get() : null); 
            helper.writeStyleClass(writer, TagStyleClass.CONTROL_INPUT.get(), inputClass); // , inputState
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

    public static class ValueOutputComponent extends jakarta.faces.component.UIComponentBase
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
            if (!controlTag.controlVisible)
                return; // Not visible!
            
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
    protected boolean                 encodeLabel          = true;
    private boolean                   creatingComponents   = false;
    protected boolean                 valueValidated       = false;
    protected boolean                 controlVisible       = true;

    public ControlTag()
    {
        super();
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
        // set
        super.setId(id);
    }

    @Override
    public void setParent(UIComponent parent)
    {
        super.setParent(parent);
        // check
        if (helper.hasComponentId())
            return;
        if (this.renderInfo==null) {
            /*
             * Attention: Only works if FormGrid is a direct parent of the Control.
             * Does not work, if other components are between the Control and the FormGrid.
             */
            this.renderInfo=helper.getControlRenderInfo();
            if (this.renderInfo!=null && this.renderInfo.AUTO_CONTROL_ID!=null) {
                // Auto set component Id
                setId(this.renderInfo.AUTO_CONTROL_ID.toString());
                log.warn("Auto-Setting compontent id for control to {}", this.getId());
            }
        }
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
    public boolean getRendersChildren()
    {
        // render children ourselves using 
        //     encodeChildren(FacesContext context)
        return true;
    }

    /*
     * Add label and input components when the view is loaded for the first time
     */
    @Override
    public void encodeBegin(FacesContext context)
        throws IOException
    {
        super.encodeBegin(context);

        // init
        helper.encodeBegin();

        // set InputControl
        if (this.control==null)
            this.control = helper.getInputControl();
        
        // renderInfo
        if (this.renderInfo==null) {
            this.renderInfo=helper.getControlRenderInfo();
            if (this.renderInfo==null)
                this.renderInfo=ControlRenderInfo.DEFAULT_CONTROL_RENDER_INFO;  // No FormGrid found. Use Default!
        }

        // Check visiblity
        this.controlVisible = helper.isVisible();

        // Render
        if (this.controlVisible && renderInfo.CONTROL_TAG!=null)
        {   // control wrapper tag
            ResponseWriter writer = context.getResponseWriter();
            writer.startElement(renderInfo.CONTROL_TAG, this);
            // render id
            helper.writeComponentId(writer, false);
            // style class
            String controlClass = helper.getTagAttributeStringEx("controlClass");
            String styleClass   = helper.getTagAttributeString(InputControl.CSS_STYLE_CLASS);
            String contextClass = helper.getContextStyleClass(); 
            helper.writeStyleClass(writer, TagStyleClass.CONTROL.get(), controlClass, styleClass, contextClass);
        }
        
        // Encode LabelSeparatorComponent
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
            labelSepTag.setRendered(this.controlVisible);
            if (this.controlVisible)
                labelSepTag.encodeAll(context);
        }
        
        // Encode InputSeparatorComponent
        boolean isCustomInput = isCustomInput();
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
            inputSepTag.setRendered(this.controlVisible);
            if (this.controlVisible)
                inputSepTag.encodeAll(context);
        }
        // done
        saveState();
    }

    @Override
    public void encodeChildren(FacesContext context)
        throws IOException
    {
        /*
         * Composite component renderer must render children for custom components
         *   <cc:implementation>
         *       <c:choose>
         *           <c:when test="#{cc.attrs.custom == true}">
         *               <cc:insertChildren/>
         *           </c:when>
         *       </c:choose>
         *   </cc:implementation>
         */
        if (this.controlVisible && isCustomInput())
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
        // encodeEnd
        if (this.controlVisible && renderInfo.CONTROL_TAG!=null)
        {   // control wrapper tag
            ResponseWriter writer = context.getResponseWriter();
            writer.endElement(renderInfo.CONTROL_TAG);
        }
        // call base
        super.encodeEnd(context);
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
        // check UI-Data
        if (helper.isInsideUIData() && getChildCount()>0)
        {   // update input state
            updateControlInputState(context);
            // No need to processValidators for children
            setRenderInput(false);
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
            if (this.inpInfo == null || !isValid())
                return;
            // validateValue
            if (helper.beginValidateValue(context, value))
                inpInfo.validate(value);
            setValid(true);
            // don't call base class!
            // super.validateValue(context, value);

        } catch (Exception e) {
            // Value is not valid
            if (!(e instanceof EmpireException))
                e = new FieldIllegalValueException(helper.getColumn(), "", e);
            // Add error message
            helper.addErrorMessage(context, e);
            setValid(false);

        } finally {
            this.valueValidated = true;
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
}
