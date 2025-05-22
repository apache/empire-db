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
import java.util.Locale;
import java.util.Map;

import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.commons.OptionEntry;
import org.apache.empire.commons.Options;
import org.apache.empire.commons.StringUtils;
import org.apache.empire.data.Column;
import org.apache.empire.exceptions.InvalidPropertyException;
import org.apache.empire.exceptions.NotSupportedException;
import org.apache.empire.jakarta.app.FacesUtils;
import org.apache.empire.jakarta.app.TextResolver;
import org.apache.empire.jakarta.controls.InputAttachedObjectsHandler;
import org.apache.empire.jakarta.controls.InputControl;
import org.apache.empire.jakarta.controls.InputControl.DisabledType;
import org.apache.empire.jakarta.controls.InputControl.InputInfo;
import org.apache.empire.jakarta.controls.InputControlManager;
import org.apache.empire.jakarta.controls.SelectInputControl;
import org.apache.empire.jakarta.utils.TagEncodingHelper;
import org.apache.empire.jakarta.utils.TagStyleClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.faces.component.NamingContainer;
import jakarta.faces.component.UIInput;
import jakarta.faces.component.UISelectOne;
import jakarta.faces.component.html.HtmlSelectOneListbox;
import jakarta.faces.component.html.HtmlSelectOneMenu;
import jakarta.faces.component.visit.VisitCallback;
import jakarta.faces.component.visit.VisitContext;
import jakarta.faces.context.FacesContext;

public class SelectTag extends UIInput implements NamingContainer
{
    // Logger
    private static final Logger log = LoggerFactory.getLogger(SelectTag.class);
    
    public static final String SELECT_COMPONENT_ID = "select";

    protected SelectInputControl control = null;
    
    private class SelectInputInfo implements InputInfo
    {
        @Override
        public Column getColumn()
        {
            return null;
        }

        @Override
        public Options getOptions()
        {
            return SelectTag.this.getOptionList();
        }

        @Override
        public Object getValue(boolean evalExpression)
        {
            Object value = SelectTag.this.getValue();
            if (value != null)
            {
                if (value instanceof Enum<?>)
                    value = ((Enum<?>)value).name();
                else
                    value = String.valueOf(value);
            }
            return value;
        }

        @Override
        public String getFormat()
        {
            String nullText = SelectTag.this.getNullText();
            if (StringUtils.isEmpty(nullText))
                return null;
            // return format for null text
            return InputControl.FORMAT_NULL+nullText;
        }

        @Override
        public Locale getLocale()
        {
            return null;
        }

        @Override
        public String getText(String key)
        {
            return null;
        }

        @Override
        public TextResolver getTextResolver()
        {
            return  FacesUtils.getTextResolver(FacesContext.getCurrentInstance());
        }

        @Override
        public String getStyleClass(String addlStyle)
        {
            return null;
        }

        @Override
        public boolean isInsideUIData()
        {
            return false;
        }

        @Override
        public void setValue(Object value)
        {
            throw new NotSupportedException(SelectTag.this, "setValue");
        }

        @Override
        public void validate(Object value)
        {
        }

        @Override
        public boolean isRequired()
        {
            return !(SelectTag.this.isAllowNull());
        }

        @Override
        public boolean isModified()
        {
            Object modified = SelectTag.this.getAttributes().get("modified");
            return (modified!=null ? ObjectUtils.getBoolean(modified) : false);
        }

        @Override
        public boolean isDisabled()
        {
            return SelectTag.this.isDisabled();
        }

        @Override
        public DisabledType getDisabled()
        {
            return (SelectTag.this.isDisabled() ? DisabledType.DISABLED : DisabledType.NO);
        }

        @Override
        public String getInputId()
        {
            return "select";
        }

        @Override
        public boolean hasError()
        {
            return false;
        }

        @Override
        public Object getAttribute(String name)
        {
            Object value = SelectTag.this.getAttributes().get(name);
            return value;
        }

        @Override
        public Object getAttributeEx(String name)
        {
            Object value = getAttribute(name);
            return value;
        }
    }
    
    private SelectInputInfo selectInputInfo = new SelectInputInfo();
    
    public SelectTag()
    {
        log.trace("component select created");
    }

    @Override
    public String getFamily()
    {
        return "jakarta.faces.NamingContainer";
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
        if (this.treeClientId!=null && control!=null && control.isCreatingComponents())
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
        UIInput inputComponent = null;
        TextResolver textResolver = FacesUtils.getTextResolver(context);
        if (getChildCount() > 0)
        {
            inputComponent = getInputComponent();
            if (inputComponent instanceof UISelectOne)
            {
                this.control = (SelectInputControl) InputControlManager.getControl(SelectInputControl.NAME);
                UISelectOne selectComp = (UISelectOne) inputComponent;
                // disabled
                selectComp.setRequired(isRequired()); // !isAllowNull()
                setInputDisabled(selectComp, isDisabled());
                // Options (sync)
                control.syncOptions(selectComp, textResolver, selectInputInfo);
                // style and value
                setInputStyleClass(selectComp);
                setInputValue(selectComp);
            }
            else
            { // Something's wrong here?
                log.warn("WARN: Unexpected child node for {}! Child item type is {}.", getClass().getName(), inputComponent.getClass().getName());
                inputComponent = null;
            }
        }
        if (inputComponent == null)
        {
            inputComponent = createSelectOneMenu(textResolver);
            this.getChildren().add(0, inputComponent);
            // attach objects
            addAttachedObjects(context, inputComponent);
        }
        else
        {   // update attached objects
            updateAttachedObjects(context, inputComponent);
        }
        // render components
        inputComponent.encodeAll(context);
        // default
        super.encodeBegin(context);
    }

    @Override
    public void validate(FacesContext context)
    {
        // nothing submitted (AJAX part request, e.g. calendar component) or readonly (won't be set
        // in updateModel())?
        UIInput inputComponent = getInputComponent();
        if (inputComponent == null || isDisabled())
        {
            return;
        }
        // component itself already checked validity, was it successful?
        if (!inputComponent.isValid())
        {
            return;
        }
        // nothing to do
        super.validate(context);
    }

    @Override
    public void updateModel(FacesContext context)
    {
        // check read only
        if (!isDisabled())
        {
            UIInput inputComponent = getInputComponent();

            Object value = (inputComponent==null ? "" : inputComponent.getValue());
            if (value != null)
            {   // get the real value
                Options options = this.getOptionList();
                OptionEntry entry = options.getEntry(value);
                if (entry!=null)
                    value = entry.getValue();
            }
            else value = "";

            setValue(value);
        }
        super.updateModel(context);
    }

    protected UIInput getInputComponent()
    {
        if (getChildren().size() == 0)
        {
            return null;
        }

        return (UIInput) getChildren().get(0);
    }

    protected Options getOptionList()
    {
        Object options = getAttributes().get("options");
        if (!(options instanceof Options))
        {
            return new Options();
        }
        return ((Options) options);
    }

    protected boolean isAllowNull()
    {
        Object allowNull = getAttributes().get("allowNull");
        return ObjectUtils.getBoolean(allowNull);
    }

    protected String getNullText()
    {
        Object nullText = getAttributes().get("nullText");
        return StringUtils.toString(nullText, "");
    }

    protected String getInputControl()
    {
        Object inputControl = getAttributes().get("inputControl");
        return StringUtils.toString(inputControl, SelectInputControl.NAME);
    }

    protected boolean isDisabled()
    {
        Object disabled = getAttributes().get("disabled");
        return ObjectUtils.getBoolean(disabled);
    }

    protected UIInput createSelectOneMenu(TextResolver textResolver)
    {
        // find inputControl by name
        InputControl inputControl = InputControlManager.getControl(getInputControl());
        if (inputControl==null || !(inputControl instanceof SelectInputControl))
            throw new InvalidPropertyException("inputControl", getInputControl());
        // create component
        this.control = (SelectInputControl)inputControl; 
        Object size = getAttributes().get("size");
        UISelectOne input = control.createSelectComponent(this, FacesContext.getCurrentInstance(), size);
        // other attributes
        copyAttributes(input);
        // Options
        control.initOptions(input, textResolver, selectInputInfo);
        // disabled
        setInputDisabled(input, isDisabled());
        // set style class
        setInputStyleClass(input);
        // input.setLabel(getLabelString());
        input.setRequired(isRequired()); // !isAllowNull()
        // input.setId(this.getId() + INPUT_SUFFIX);
        setInputValue(input);
        return input;
    }

    protected final void setInputStyleClass(UISelectOne input)
    {
        // css style
        String userStyle = StringUtils.toString(getAttributes().get(InputControl.CSS_STYLE_CLASS));
        // additional style classes
        if (isDisabled())
        {   // disabled
            userStyle = TagStyleClass.INPUT_DIS.addTo(userStyle);
        }
        else if (input.isRequired() || !isAllowNull())
        {   // required
            userStyle = TagStyleClass.INPUT_REQ.addTo(userStyle);
        }
        // String cssStyle = TagEncodingHelper.assembleStyleClassString(TagStyleClass.SELECT.get(), null, null, userStyle);
        String cssStyle = TagStyleClass.SELECT.append(userStyle);
        input.getAttributes().put(InputControl.CSS_STYLE_CLASS, cssStyle);
    }

    protected void setInputValue(UISelectOne input)
    {
        Object value = getValue();
        if (value != null)
        {
            if (value instanceof Enum<?>)
                value = ((Enum<?>)value).name();
            else
                value = String.valueOf(value);
        }
        input.setValue(value);
    }

    protected void copyAttributes(UISelectOne input)
    {
        // set id
        if (TagEncodingHelper.hasComponentId(this))
        {   // remove trailing underscore (workaround since parent and child may not have the same name)
            String inputId = this.getId();
            if (inputId.endsWith("_"))
            {
                inputId = inputId.substring(0, inputId.length() - 1);
            }
            input.setId(inputId);
        }
        else
        {   // always set to CompoentID
            input.setId(SELECT_COMPONENT_ID);
        }
        // the map
        Map<String, Object> tagMap = getAttributes();
        Map<String, Object> inputMap = input.getAttributes();
        // other
        copyAttribute(inputMap, tagMap, "style");
        copyAttribute(inputMap, tagMap, "label");
        copyAttribute(inputMap, tagMap, "tabindex");
        copyAttribute(inputMap, tagMap, "onchange");
    }
    
    protected void addAttachedObjects(FacesContext context, UIInput inputComponent)
    {
        InputAttachedObjectsHandler aoh = InputControlManager.getAttachedObjectsHandler();
        if (aoh!=null)
            aoh.addAttachedObjects(this, context, null, inputComponent);
    }
    
    protected void updateAttachedObjects(FacesContext context, UIInput inputComponent)
    {
        InputAttachedObjectsHandler aoh = InputControlManager.getAttachedObjectsHandler();
        if (aoh!=null)
            aoh.updateAttachedObjects(this, context, null, inputComponent);
    }
    
    protected void setInputDisabled(UISelectOne input, boolean disabled)
    {
        if (input instanceof HtmlSelectOneMenu)
            ((HtmlSelectOneMenu)input).setDisabled(disabled);
        else if (input instanceof HtmlSelectOneListbox)
            ((HtmlSelectOneListbox)input).setDisabled(disabled);
        else
            log.warn("Unable to set disabled attribute!");
    }
    
    protected void copyAttribute(Map<String, Object> inputMap, Map<String, Object> tagMap, String name)
    {
        Object value = tagMap.get(name);
        if (value==null) // Empty String must be allowed!
            return;
        // set
        inputMap.put(name, String.valueOf(value));
    }
    
}
