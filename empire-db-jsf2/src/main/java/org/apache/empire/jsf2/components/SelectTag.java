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
import java.util.Locale;
import java.util.Map;

import javax.el.ValueExpression;
import javax.faces.component.NamingContainer;
import javax.faces.component.UIInput;
import javax.faces.component.html.HtmlSelectOneMenu;
import javax.faces.component.visit.VisitCallback;
import javax.faces.component.visit.VisitContext;
import javax.faces.context.FacesContext;

import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.commons.Options;
import org.apache.empire.commons.StringUtils;
import org.apache.empire.data.Column;
import org.apache.empire.exceptions.InvalidPropertyException;
import org.apache.empire.exceptions.NotSupportedException;
import org.apache.empire.jsf2.app.FacesUtils;
import org.apache.empire.jsf2.app.TextResolver;
import org.apache.empire.jsf2.controls.InputAttachedObjectsHandler;
import org.apache.empire.jsf2.controls.InputControl;
import org.apache.empire.jsf2.controls.InputControl.InputInfo;
import org.apache.empire.jsf2.controls.InputControlManager;
import org.apache.empire.jsf2.controls.SelectInputControl;
import org.apache.empire.jsf2.utils.TagEncodingHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
                if (value.getClass().isEnum())
                    value = ((Enum<?>) value).name();
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
        public boolean isFieldReadOnly()
        {
            return false;
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
            return null;
        }

        @Override
        public Object getAttributeEx(String name)
        {
            Object value = SelectTag.this.getAttributes().get(name);
            if (value==null)
            {   // try value expression
                ValueExpression ve = SelectTag.this.getValueExpression(name);
                if (ve!=null)
                {   // It's a value expression
                    FacesContext ctx = FacesContext.getCurrentInstance();
                    value = ve.getValue(ctx.getELContext());
                }
            }
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
        return "javax.faces.NamingContainer";
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
            if (inputComponent instanceof HtmlSelectOneMenu)
            {
                this.control = (SelectInputControl) InputControlManager.getControl(SelectInputControl.NAME);
                // disabled
                boolean disabled = isDisabled();
                ((HtmlSelectOneMenu) inputComponent).setDisabled(disabled);
                // Options (sync)
                control.syncOptions((HtmlSelectOneMenu) inputComponent, textResolver, selectInputInfo);
                setInputValue((HtmlSelectOneMenu) inputComponent);
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
    public void updateModel(FacesContext context)
    {
        // check read only
        if (!isDisabled())
        {
            UIInput inputComponent = getInputComponent();

            Object value = (inputComponent==null ? "" : inputComponent.getValue());
            if (value == null)
                value = "";
            setValue(value);
        }
        super.updateModel(context);
    }

    @Override
    public void validate(FacesContext context)
    {
        // nothing submitted (AJAX part request, e.g. calendar component) or readonly (won't be set
        // in updateModel())?
        UIInput inputComponent = getInputComponent();
        if (inputComponent == null)
        {
            return;
        }
        // component itself already checked validity, was it successful?
        if (!inputComponent.isValid() || isDisabled())
        {
            return;
        }
        // nothing to do
        super.validate(context);
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
        HtmlSelectOneMenu input = control.createMenuComponent(this);
        // css style
        String userStyle = StringUtils.toString(getAttributes().get("styleClass"));
        String cssStyle = TagEncodingHelper.assembleStyleClassString("eSelect", null, null, userStyle);
        input.setStyleClass(cssStyle);
        // other attributes
        copyAttributes(input);
        input.setId(SELECT_COMPONENT_ID);
        // Options
        control.initOptions(input, textResolver, selectInputInfo);
        // disabled
        boolean disabled = isDisabled();
        input.setDisabled(disabled);
        control.addRemoveDisabledStyle(input, disabled);
        // input.setLabel(getLabelString());
        // input.setRequired(col.isRequired() && !col.isAutoGenerated());
        // input.setId(this.getId() + INPUT_SUFFIX);
        setInputValue(input);
        return input;
    }

    protected void setInputValue(HtmlSelectOneMenu input)
    {
        Object value = getValue();
        if (value != null)
        {
            if (value.getClass().isEnum())
                value = ((Enum<?>) value).name();
            else
                value = String.valueOf(value);
        }
        input.setValue(value);
    }

    protected void copyAttributes(HtmlSelectOneMenu input)
    {
        // set id
        String inputId = this.getId();
        if (StringUtils.isNotEmpty(inputId))
        { // remove trailing underscore (workaround since parent and child may not have the same name)
            if (inputId.endsWith("_"))
            {
                inputId = inputId.substring(0, inputId.length() - 1);
            }
            input.setId(inputId);
        }

        Map<String, Object> attr = getAttributes();
        Object value;
        if ((value = attr.get("style")) != null)
        {
            input.setStyle(StringUtils.toString(value));
        }
        if ((value = attr.get("tabindex")) != null)
        {
            input.setTabindex(StringUtils.toString(value));
        }
        if ((value = attr.get("onchange")) != null)
        {
            input.setOnchange(StringUtils.toString(value));
        }
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
}
