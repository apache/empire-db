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
package org.apache.empire.jakarta.controls;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import jakarta.el.ValueExpression;
import jakarta.faces.component.UIComponent;
import jakarta.faces.component.UIInput;
import jakarta.faces.component.UISelectItem;
import jakarta.faces.component.html.HtmlSelectOneRadio;
import jakarta.faces.context.FacesContext;
import jakarta.faces.context.ResponseWriter;
import jakarta.faces.event.PhaseId;

import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.commons.OptionEntry;
import org.apache.empire.commons.Options;
import org.apache.empire.data.Column;
import org.apache.empire.exceptions.InvalidArgumentException;
import org.apache.empire.exceptions.UnexpectedReturnValueException;
import org.apache.empire.jakarta.app.TextResolver;
import org.apache.empire.jakarta.utils.TagStyleClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RadioInputControl extends InputControl
{
    private static final Logger                      log                   = LoggerFactory.getLogger(RadioInputControl.class);

    public static final String                       COLATTR_ABBR_OPTIONS  = "ABBR_OPTIONS";                                   // Option list for abbreviations

    public static final String                       VALUE_EXPRESSION_FLAG = "VALUE_EXPRESSION_FLAG";

    public static final String                       NAME                  = "radio";

    private final Class<? extends HtmlSelectOneRadio> inputComponentClass;

    public RadioInputControl(String name, Class<? extends HtmlSelectOneRadio> inputComponentClass)
    {
        super(name);
        this.inputComponentClass = inputComponentClass;
    }

    public RadioInputControl()
    {
        this(RadioInputControl.NAME, HtmlSelectOneRadio.class);
    }

    /* Value */
    @Override
    public void renderValue(Object value, ValueInfo vi, ResponseWriter writer)
        throws IOException
    {
        String style = vi.getStyleClass(TagStyleClass.RADIO.append(TagStyleClass.INPUT_DIS.get()));
        writer.startElement(HTML_TAG_DIV, null);
        writer.writeAttribute(HTML_ATTR_CLASS, style, null);
        writer.startElement(HTML_TAG_TABLE, null);
        writer.writeAttribute(HTML_ATTR_CLASS, style, null);
        writer.startElement(HTML_TAG_TR, null);
        Options o = vi.getOptions();
        for (OptionEntry e : o)
        {
            writer.startElement(HTML_TAG_TD, null);
            // input
            writer.startElement(HTML_TAG_INPUT, null);
            writer.writeAttribute(HTML_ATTR_TYPE, "radio", null);
            writer.writeAttribute(HTML_ATTR_DISABLED, "disabled", null);
            if (ObjectUtils.compareEqual(e.getValue(), value))
                writer.writeAttribute(HTML_ATTR_CHECKED, "checked", null);
            writer.endElement(HTML_TAG_INPUT);
            // label
            writer.startElement(HTML_TAG_LABEL, null);
            writer.writeAttribute(HTML_ATTR_CLASS, TagStyleClass.RADIO.get(), null);
            String text = e.getText();
            text = vi.getTextResolver().resolveText(text);
            writer.writeText(text, null);
            writer.endElement(HTML_TAG_LABEL);
            // end
            writer.endElement(HTML_TAG_TD);
        }
        writer.endElement(HTML_TAG_TR);
        writer.endElement(HTML_TAG_TABLE);
        writer.endElement(HTML_TAG_DIV);
    }
    
    @Override
    protected void copyAttributes(UIComponent parent, InputInfo ii, UIInput input, String additonalStyle)
    {
        // copy
        super.copyAttributes(parent, ii, input, TagStyleClass.RADIO.append(additonalStyle));
    }

    @Override
    protected void createInputComponents(UIComponent parent, InputInfo ii, FacesContext context, List<UIComponent> compList)
    {
        if (!compList.isEmpty())
            throw new InvalidArgumentException("compList", compList);
        // create
        HtmlSelectOneRadio input = InputControlManager.createComponent(context, this.inputComponentClass);
        // setValueExpressionFlag
        Object value = ii.getValue(false);
        input.getAttributes().put(RadioInputControl.VALUE_EXPRESSION_FLAG, (value instanceof ValueExpression));
        // copy Attributes
        copyAttributes(parent, ii, input);
        // disabled
        boolean disabled = ii.isDisabled();
        input.setDisabled(disabled);
        // Options
        Options options = ii.getOptions();
        boolean addEmpty = getEmptyEntryRequired(ii, disabled) && !options.containsNull();
        String nullText = (addEmpty) ? getNullText(ii) : "";
        initOptions(input, ii.getTextResolver(), options, addEmpty, nullText);
        // add
        compList.add(input);
        // style
        addRemoveDisabledStyle(input, disabled);
        addRemoveInvalidStyle(input, ii.hasError());
        // Set Value
        setInputValue(input, ii);
    }
    
    @Override
    protected void updateInputState(List<UIComponent> compList, InputInfo ii, FacesContext context, PhaseId phaseId)
    {
        UIComponent comp = compList.get(0);
        if (!(comp instanceof HtmlSelectOneRadio))
        {
            throw new UnexpectedReturnValueException(comp.getClass().getName(), "parent.getChildren()");
        }
        HtmlSelectOneRadio input = (HtmlSelectOneRadio)comp;
        // disabled
        boolean disabled = ii.isDisabled();
        input.setDisabled(disabled);
        // check phase
        if (phaseId!=PhaseId.APPLY_REQUEST_VALUES)
        {   // Options (sync)
            Options options = ii.getOptions();
            boolean addEmpty = getEmptyEntryRequired(ii, disabled) && !options.containsNull();
            String nullText = (addEmpty) ? getNullText(ii) : "";
            syncOptions(input, ii.getTextResolver(), options, addEmpty, nullText);
        }
        if (phaseId==PhaseId.RENDER_RESPONSE)
        {   // style
            addRemoveDisabledStyle(input, disabled);
            addRemoveInvalidStyle(input, ii.hasError());
            // set value
            setInputValue(input, ii);
        }
    }

    protected boolean getEmptyEntryRequired(InputInfo ii, boolean disabled)
    {
        if (!ii.isRequired() && !(disabled && ii.getColumn().isRequired()))
        {
            return true;
        }
        // Check Value
        return (ii.getValue(true) == null);
    }

    public void initOptions(HtmlSelectOneRadio input, TextResolver textResolver, Options options, boolean addEmpty, String nullText)
    {
        if (addEmpty)
        { // Empty entry
            addSelectItem(input, textResolver, new OptionEntry(null, nullText));
        }
        if (options != null && options.size() > 0)
        { // Add options
            for (OptionEntry e : options)
            { // Option entries
                addSelectItem(input, textResolver, e);
            }
        }
    }
    
    public void syncOptions(HtmlSelectOneRadio input, TextResolver textResolver, Options options, boolean hasEmpty, String nullText)
    {
        // Compare child-items with options
        Iterator<OptionEntry> ioe = options.iterator();
        OptionEntry oe = (ioe.hasNext() ? ioe.next() : null);
        List<UIComponent> childList = input.getChildren();
        Iterator<UIComponent> ico = childList.iterator();
        int lastIndex = 0;
        while (ico.hasNext())
        {
            lastIndex++;
            UIComponent co = ico.next();
            if (!(co instanceof UISelectItem))
            {
                continue;
            }
            UISelectItem si = (UISelectItem) co;
            Object ov = si.getItemValue();
            if (ObjectUtils.isEmpty(ov) && hasEmpty)
            {
                continue;
            }
            if (oe == null)
            { // remove obsolete items
                lastIndex--;
                for (int index = childList.size() - 1; index >= lastIndex; index--)
                {
                    childList.remove(index);
                }
                // done
                return;
            }
            if (ObjectUtils.compareEqual(ov, oe.getValue()))
            { // next
                oe = (ioe.hasNext() ? ioe.next() : null);
                continue;
            }
            // Not equal - do a full reload
            input.getChildren().clear();
            if (hasEmpty)
            {
                addSelectItem(input, textResolver, new OptionEntry("", nullText));
            }
            for (OptionEntry e : options)
            { // Option entries
                addSelectItem(input, textResolver, e);
            }
            // done
            return;
        }
        // Are there any items left?
        while (oe != null)
        { // add missing item
            addSelectItem(input, textResolver, oe);
            oe = (ioe.hasNext() ? ioe.next() : null);
        }
    }

    public void addSelectItem(UIComponent input, TextResolver textResolver, OptionEntry e)
    {
        UISelectItem selectItem = new UISelectItem();
        // set value
        Object value;
        Object valueExpressionFlag = input.getAttributes().get(RadioInputControl.VALUE_EXPRESSION_FLAG);
        if (ObjectUtils.getBoolean(valueExpressionFlag))
        { // Use value as is
            value = e.getValue();
        }
        else
        { // Convert to String
            value = e.getValueString();
        }
        selectItem.setItemValue(value);
        // set text
        String text = e.getText();
        text = textResolver.resolveText(text);
        selectItem.setItemLabel(text);
        // add item
        input.getChildren().add(selectItem);
    }

    protected String getNullText(InputInfo ii)
    {
        String nullText = getFormatString(ii, InputControl.FORMAT_NULL, InputControl.FORMAT_NULL_ATTRIBUTE);
        return (nullText != null) ? ii.getText(nullText) : "";
    }

    @Override
    protected String formatValue(Object value, ValueInfo vi)
    {
        // Lookup and Print value
        if (vi.getOptions() == null)
        {
            RadioInputControl.log.warn("Select field {} has no Option list attached!", vi.getColumn().getName());
            return super.formatValue(value, vi);
        }
        // Check for Abbreviation
        if (hasFormatOption(vi, "short"))
        {
            Column column = vi.getColumn();
            if (column != null)
            { // Check for Abbreviation option list
                Object attrValue = column.getAttribute(RadioInputControl.COLATTR_ABBR_OPTIONS);
                if (attrValue instanceof Options)
                { // Check for Options
                    String text = getOptionText(((Options) attrValue), value, vi);
                    if (text!=null)
                        return text;
                }
            }
        }
        return super.formatValue(value, vi);
    }

    @Override
    protected Object formatInputValue(Object value, InputInfo ii)
    {
        // the enum Value
        if (value instanceof Enum<?>)
            return ((Enum<?>) value).name();
        // the value
        return value;
    }

    @Override
    protected Object parseInputValue(String value, InputInfo ii)
    {
        Class<Enum<?>> enumType = ii.getColumn().getEnumType();
        if (enumType!=null)
        {   // convert to enum
            return ObjectUtils.getEnumByName(enumType, value);
        }
        return value;
    }

}
