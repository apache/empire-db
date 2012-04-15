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
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.component.UISelectItem;
import javax.faces.component.html.HtmlSelectOneMenu;
import javax.faces.context.FacesContext;

import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.commons.OptionEntry;
import org.apache.empire.commons.Options;
import org.apache.empire.commons.StringUtils;
import org.apache.empire.jsf2.app.FacesUtils;
import org.apache.empire.jsf2.app.TextResolver;
import org.apache.empire.jsf2.utils.TagEncodingHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SelectTag extends UIInput implements NamingContainer
{
    // Logger
    private static final Logger log = LoggerFactory.getLogger(SelectTag.class);

    public SelectTag()
    {
        log.trace("component select created");
    }

    @Override
    public String getFamily()
    {
        return "javax.faces.NamingContainer";
    }

    @Override
    public void encodeBegin(FacesContext context)
        throws IOException
    {
        // add label and input components when the view is loaded for the first time
        UIInput inputComponent = null;
        if (getChildCount() > 0)
        {
            inputComponent = getInputComponent();
        }
        if (inputComponent == null)
        {
            inputComponent = createSelectOneMenu(FacesUtils.getTextResolver(context));
            this.getChildren().add(inputComponent);
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
            
            Object value = inputComponent == null ?  "" :  inputComponent.getValue();
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
            return new Options();
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

        HtmlSelectOneMenu input = new HtmlSelectOneMenu();
        // css style
        String userStyle = StringUtils.toString(getAttributes().get("styleClass"));
        String cssStyle  = TagEncodingHelper.getTagStyleClass("eSelect", null, null, userStyle);
        input.setStyleClass(cssStyle);
        // other attributes
        copyAttributes(input);
        // Options
        Options options = getOptionList();
        if (isAllowNull())
        { // Empty entry
            options = new Options(options);
            addSelectItem(input, textResolver, new OptionEntry("", getNullText()));
        }
        for (OptionEntry e : options)
        { // Option entries
            addSelectItem(input, textResolver, e);
        }
        // input.setReadonly(isReadOnly());
        if (isDisabled())
            input.setDisabled(true);
        // input.setLabel(getLabelString());
        // input.setRequired(col.isRequired() && !col.isAutoGenerated());
        // input.setId(this.getId() + INPUT_SUFFIX);
        input.setValue(getValue());
        return input;
    }

    private void copyAttributes(HtmlSelectOneMenu input)
    {
        Map<String, Object> attr = getAttributes();
        Object value;
        if ((value = attr.get("style")) != null)
            input.setStyle(StringUtils.toString(value));
        if ((value = attr.get("tabindex")) != null)
            input.setTabindex(StringUtils.toString(value));
        if ((value = attr.get("onchange")) != null)
            input.setOnchange(StringUtils.toString(value));
    }

    private void addSelectItem(UIComponent input, TextResolver textResolver, OptionEntry e)
    {
        UISelectItem selectItem = new UISelectItem();
        // set value
        selectItem.setItemValue(e.getValueString());
        // set text
        String text = e.getText();
        text = textResolver.resolveText(text);
        selectItem.setItemLabel(text);
        // add item
        input.getChildren().add(selectItem);
    }

}
