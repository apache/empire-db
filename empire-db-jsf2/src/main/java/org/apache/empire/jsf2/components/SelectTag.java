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
import java.util.Map;

import javax.faces.component.NamingContainer;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.component.html.HtmlSelectOneMenu;
import javax.faces.component.visit.VisitCallback;
import javax.faces.component.visit.VisitContext;
import javax.faces.context.FacesContext;
import javax.faces.view.AttachedObjectHandler;

import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.commons.Options;
import org.apache.empire.commons.StringUtils;
import org.apache.empire.jsf2.app.FacesUtils;
import org.apache.empire.jsf2.app.TextResolver;
import org.apache.empire.jsf2.controls.InputControlManager;
import org.apache.empire.jsf2.controls.SelectInputControl;
import org.apache.empire.jsf2.utils.TagEncodingHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SelectTag extends UIInput implements NamingContainer
{
    // Logger
    private static final Logger log = LoggerFactory.getLogger(SelectTag.class);

    private SelectInputControl control = null;
    
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
                Options options = getOptionList();
                boolean hasEmpty = isAllowNull() && !options.contains("");
                control.syncOptions((HtmlSelectOneMenu) inputComponent, textResolver, options, hasEmpty, getNullText(), false);
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
            // attach events
            attachEvents(context, inputComponent);
        }
        // render components
        inputComponent.encodeAll(context);
        // default
        super.encodeBegin(context);
    }

    @Override
    public void decode(FacesContext context)
    {
        for (UIComponent c : getChildren())
        {
            c.decode(context);
        }
        super.decode(context);
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

    private UIInput getInputComponent()
    {
        if (getChildren().size() == 0)
        {
            return null;
        }

        return (UIInput) getChildren().get(0);
    }

    private Options getOptionList()
    {
        Object options = getAttributes().get("options");
        if (!(options instanceof Options))
        {
            return new Options();
        }
        return ((Options) options);
    }

    private boolean isAllowNull()
    {
        Object allowNull = getAttributes().get("allowNull");
        return ObjectUtils.getBoolean(allowNull);
    }

    private String getNullText()
    {
        Object nullText = getAttributes().get("nullText");
        return StringUtils.toString(nullText, "");
    }

    private boolean isDisabled()
    {
        Object disabled = getAttributes().get("disabled");
        return ObjectUtils.getBoolean(disabled);
    }

    private UIInput createSelectOneMenu(TextResolver textResolver)
    {
        this.control = (SelectInputControl) InputControlManager.getControl(SelectInputControl.NAME);
        HtmlSelectOneMenu input = control.createMenuComponent(this);
        // css style
        String userStyle = StringUtils.toString(getAttributes().get("styleClass"));
        String cssStyle = TagEncodingHelper.getTagStyleClass("eSelect", null, null, userStyle);
        input.setStyleClass(cssStyle);
        // other attributes
        copyAttributes(input);
        // Options
        Options options = getOptionList();
        boolean addEmpty = isAllowNull() && !options.contains("");
        control.initOptions(input, textResolver, options, addEmpty, getNullText());
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

    private void setInputValue(HtmlSelectOneMenu input)
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

    private void copyAttributes(HtmlSelectOneMenu input)
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

    protected void attachEvents(FacesContext context, UIInput inputComponent)
    {
        // Events available?
        @SuppressWarnings("unchecked")
        List<AttachedObjectHandler> result = (List<AttachedObjectHandler>) getAttributes().get("javax.faces.RetargetableHandlers");
        if (result == null)
        {
            return;
        }
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
