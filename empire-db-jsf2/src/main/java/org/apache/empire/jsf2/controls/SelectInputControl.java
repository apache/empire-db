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
package org.apache.empire.jsf2.controls;

import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.List;

import javax.el.ValueExpression;
import javax.faces.component.UIComponent;
import javax.faces.component.UISelectItem;
import javax.faces.component.html.HtmlSelectOneMenu;
import javax.faces.context.FacesContext;

import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.commons.OptionEntry;
import org.apache.empire.commons.Options;
import org.apache.empire.data.Column;
import org.apache.empire.exceptions.InternalException;
import org.apache.empire.exceptions.InvalidArgumentException;
import org.apache.empire.exceptions.ItemNotFoundException;
import org.apache.empire.exceptions.UnexpectedReturnValueException;
import org.apache.empire.jsf2.app.TextResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SelectInputControl extends InputControl
{
    private static final Logger log                   = LoggerFactory.getLogger(SelectInputControl.class);

    public static final String  COLATTR_ABBR_OPTIONS  = "ABBR_OPTIONS";                                   // Option list for abbreviations

    public static final String  VALUE_EXPRESSION_FLAG = "VALUE_EXPRESSION_FLAG";

    public static final String  NAME                  = "select";

    private final Class<? extends HtmlSelectOneMenu> inputComponentClass;
    
    public SelectInputControl(String name, Class<? extends HtmlSelectOneMenu> inputComponentClass)
    {
        super(name);
        this.inputComponentClass = inputComponentClass;
    }

    public SelectInputControl()
    {
        this(SelectInputControl.NAME, HtmlSelectOneMenu.class);
    }

    /* for SelectTag (when no column is available) */ 
    public HtmlSelectOneMenu createMenuComponent(UIComponent parent)
    {
        return InputControlManager.createComponent(FacesContext.getCurrentInstance(), this.inputComponentClass);
    }

    @Override
    protected void createInputComponents(UIComponent parent, InputInfo ii, FacesContext context, List<UIComponent> compList)
    {
        // check params
        if (!compList.isEmpty())
            throw new InvalidArgumentException("compList", compList);
        // create
        HtmlSelectOneMenu input = InputControlManager.createComponent(context, this.inputComponentClass);
        // setValueExpressionFlag
        Object value = ii.getValue(false);
        input.getAttributes().put(SelectInputControl.VALUE_EXPRESSION_FLAG, (value instanceof ValueExpression));
        // copy Attributes
        copyAttributes(parent, ii, input);
        // disabled
        boolean disabled = ii.isDisabled();
        input.setDisabled(disabled);
        // Options
        Options options = getOptions(ii);
        boolean addEmpty = getEmptyEntryRequired(ii, disabled) && !options.contains("");
        String nullText = (addEmpty) ? getNullText(ii) : "";
        initOptions(input, ii.getTextResolver(), options, addEmpty, nullText);
        // add
        compList.add(input);
        // style
        addRemoveDisabledStyle(input, input.isDisabled());
        addRemoveInvalidStyle(input, ii.hasError());
        // Set Value
        setInputValue(input, ii);
    }
    
    @Override
    protected void updateInputState(List<UIComponent> compList, InputInfo ii, FacesContext context, boolean setValue)
    {
        UIComponent comp = compList.get(0);
        if (!(comp instanceof HtmlSelectOneMenu))
        {
            throw new UnexpectedReturnValueException(comp.getClass().getName(), "parent.getChildren()");
        }
        HtmlSelectOneMenu input = (HtmlSelectOneMenu)comp;
        // disabled
        boolean disabled = ii.isDisabled();
        input.setDisabled(disabled);
        // Options (sync)
        Options options = ii.getOptions();
        boolean addEmpty = getEmptyEntryRequired(ii, disabled) && !options.contains("");
        String nullText = (addEmpty) ? getNullText(ii) : "";
        syncOptions(input, ii.getTextResolver(), options, addEmpty, nullText, ii.isInsideUIData());
        // set value
        if (setValue)
        {   // style
            addRemoveDisabledStyle(input, input.isDisabled());
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

    public void initOptions(HtmlSelectOneMenu input, TextResolver textResolver, Options options, boolean addEmpty, String nullText)
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
    
    public void syncOptions(HtmlSelectOneMenu input, TextResolver textResolver, Options options, boolean hasEmpty, String nullText, boolean isInsideUIData)
    {
        // Compare child-items with options
        Iterator<OptionEntry> ioe = options.iterator();
        OptionEntry oe = (ioe.hasNext() ? ioe.next() : null);
        List<UIComponent> childList = input.getChildren();
        Iterator<UIComponent> ico = childList.iterator();
        int lastIndex = 0;
        boolean emptyPresent = false;
        while (ico.hasNext())
        {
            lastIndex++;
            UIComponent co = ico.next();
            if (!(co instanceof UISelectItem))
                continue;
            UISelectItem si = (UISelectItem) co;
            Object ov = si.getItemValue();
            if (ObjectUtils.isEmpty(ov) && hasEmpty)
            {   emptyPresent = true;
                continue;
            }
            if (oe == null)
            {   // remove obsolete items
                lastIndex--;
                for (int index = childList.size() - 1; index >= lastIndex; index--)
                    childList.remove(index);
                // done
                return;
            }
            if (ObjectUtils.compareEqual(ov, oe.getValue()))
            {   // next
                oe = (ioe.hasNext() ? ioe.next() : null);
                continue;
            }
            // Not equal - do a full reload
            input.getChildren().clear();
            if (hasEmpty)
                addSelectItem(input, textResolver, new OptionEntry("", nullText));
            for (OptionEntry e : options)
            { // Option entries
                addSelectItem(input, textResolver, e);
            }
            // done
            return;
        }
        // check empty entry
        if (hasEmpty && !emptyPresent)
        {   // add missing empty entry
            addSelectItem(input, textResolver, new OptionEntry("", nullText), 0);
        }
        // Are there any items left?
        while (oe != null)
        { // add missing item
            addSelectItem(input, textResolver, oe);
            oe = (ioe.hasNext() ? ioe.next() : null);
        }
    }

    public void addSelectItem(UIComponent input, TextResolver textResolver, OptionEntry e, int pos)
    {
        UISelectItem selectItem = new UISelectItem();
        // set value
        Object value;
        Object valueExpressionFlag = input.getAttributes().get(SelectInputControl.VALUE_EXPRESSION_FLAG);
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
        if (pos>=0)
            input.getChildren().add(pos, selectItem);
        else
            input.getChildren().add(selectItem);
    }

    public void addSelectItem(UIComponent input, TextResolver textResolver, OptionEntry e)
    {
        addSelectItem(input, textResolver, e, -1);
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
            SelectInputControl.log.warn("Select field {} has no Option list attached!", vi.getColumn().getName());
            return super.formatValue(value, vi);
        }
        // Check for Abbreviation
        if (hasFormatOption(vi, "short"))
        {
            Column column = vi.getColumn();
            if (column != null)
            {   // Check for Abbreviation option list
                Object attrValue = column.getAttribute(SelectInputControl.COLATTR_ABBR_OPTIONS);
                if (attrValue instanceof Options)
                {   // Check for Options
                    String text = ((Options) attrValue).get(value);
                    if (text != null)
                        return vi.getText(text);
                    // Error
                    SelectInputControl.log.error("The element '" + String.valueOf(value) + "' is not part of the supplied option list.");
                }
            }
        }
        return super.formatValue(value, vi);
    }

    @Override
    protected Object formatInputValue(Object value, InputInfo ii)
    {
        // the enum Value
        if (value != null && value.getClass().isEnum())
        {
            return ((Enum<?>) value).name();
        }
        // the value
        return value;
    }

    @Override
    protected Object parseInputValue(String value, InputInfo ii)
    {
        Object enumType = ii.getColumn().getAttribute(Column.COLATTR_ENUMTYPE);
        if (enumType != null)
        {
            try
            { // get enum
                Class<?> enumClass = (Class<?>) enumType;
                Field field = enumClass.getDeclaredField(value);
                return field.get(null);
            }
            catch (NoSuchFieldException e)
            {
                throw new ItemNotFoundException(value);
            }
            catch (SecurityException e)
            {
                throw new InternalException(e);
            }
            catch (IllegalArgumentException e)
            {
                throw new InternalException(e);
            }
            catch (IllegalAccessException e)
            {
                throw new InternalException(e);
            }
        }
        return value;
    }

    /**
     * gets the options in a safe way (not null)
     * @param ii
     * @return the options for this column
     */
    protected Options getOptions(InputInfo ii)
    {
        Options options = ii.getOptions();
        if (options==null)
        {
            log.warn("No options given for column {}", ii.getColumn().getName());
            options = new Options();
        }
        return options;
    }
    
    /*
    @Override
    public void renderInput(ResponseWriter writer, ControlInfo ci)
    {
        boolean disabled = ci.getDisabled();
    
        HtmlTag input = writer.startTag("select");
        input.addAttribute("id",    ci.getId());
        input.addAttribute("class", ci.getCssClass());
        input.addAttribute("style", ci.getCssStyle());
        if (disabled)
        {
            input.addAttribute("disabled");
        } else
        {
            input.addAttribute("name", ci.getName());
        }
        // Event Attributes
        input.addAttribute("onclick",   ci.getOnclick());
        input.addAttribute("onchange",  ci.getOnchange());
        input.addAttribute("onfocus",   ci.getOnfocus());
        input.addAttribute("onblur",    ci.getOnblur());
        input.beginBody(true);
        // Render List of Options
        Options options = ci.getOptions();
        if (options!=null)
        {   // Render option list
            Object current = ci.getValue();
            if (hasFormatOption(ci, "allownull") && options.contains(null)==false)
            {   // add an empty entry
                addEmtpyEntry(writer, ObjectUtils.isEmpty(current));
            }
            for (OptionEntry entry : options)
            {
                Object value = entry.getValue();
                boolean isCurrent = ObjectUtils.compareEqual(current, value);
                if (isCurrent == false && disabled)
                    continue; // 
                // Add Option entry
                HtmlTag option = writer.startTag("option");
                option.addAttributeNoCheck("value", value, true);
                option.addAttribute("selected", isCurrent);
                option.beginBody(ci.getTranslation(entry.getText()));
                option.endTag(true);
            }
        }
        else
        {   // No Option list available
            log.error("No options available for select input control.");
        }
        // done
        input.endTag();
    }
    
    private void addEmtpyEntry(HtmlWriter writer, boolean isCurrent)
    {
        // Add Option entry
        HtmlTag option = writer.startTag("option");
        option.addAttributeNoCheck("value", "", false);
        option.addAttribute("selected", isCurrent);
        option.beginBody("");
        option.endTag(true);
    }
    */

}
