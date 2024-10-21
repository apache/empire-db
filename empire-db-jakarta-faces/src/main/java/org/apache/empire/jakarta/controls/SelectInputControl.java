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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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

import jakarta.el.ValueExpression;
import jakarta.faces.component.UIComponent;
import jakarta.faces.component.UIParameter;
import jakarta.faces.component.UISelectItems;
import jakarta.faces.component.UISelectOne;
import jakarta.faces.component.html.HtmlSelectOneListbox;
import jakarta.faces.component.html.HtmlSelectOneMenu;
import jakarta.faces.context.FacesContext;
import jakarta.faces.event.PhaseId;
import jakarta.faces.model.SelectItem;

public class SelectInputControl extends InputControl
{
    private static final Logger log                   = LoggerFactory.getLogger(SelectInputControl.class);

    public static final String  COLATTR_ABBR_OPTIONS  = "ABBR_OPTIONS";                                   // Option list for abbreviations

    public static final String  VALUE_EXPRESSION_TYPE = "VALUE_EXPRESSION_TYPE";

    public static final String  FORMAT_SIZE           = "size:";

    public static final String  FORMAT_SIZE_ATTR      = "format:size";
    
    public static final String  NAME                  = "select";

    private final Class<? extends HtmlSelectOneMenu> menuComponentClass;

    private final Class<? extends HtmlSelectOneListbox> listComponentClass;
    
    public SelectInputControl(String name, Class<? extends HtmlSelectOneMenu> menuComponentClass, Class<? extends HtmlSelectOneListbox> listComponentClass)
    {
        super(name);
        this.menuComponentClass = menuComponentClass;
        this.listComponentClass = listComponentClass;
    }

    public SelectInputControl()
    {
        this(SelectInputControl.NAME, HtmlSelectOneMenu.class, HtmlSelectOneListbox.class);
    }

    /* for SelectTag (when no column is available) */ 
    public UISelectOne createSelectComponent(UIComponent parent, FacesContext context, Object formatSize)
    {
        Class<? extends UISelectOne> selectOneClass;
        int listSize = ObjectUtils.getInteger(formatSize, 1);
        if (listSize==-1 || listSize>1)
            selectOneClass = this.listComponentClass;
        else
            selectOneClass = this.menuComponentClass;
        // create now
        UISelectOne selectOne = InputControlManager.createComponent(context, selectOneClass);
        // set list size
        if ((selectOne instanceof HtmlSelectOneListbox) && listSize>1)
            ((HtmlSelectOneListbox)selectOne).setSize(listSize);
        // done
        return selectOne;
    }

    @Override
    protected void createInputComponents(UIComponent parent, InputInfo ii, FacesContext context, List<UIComponent> compList)
    {
        // check params
        if (!compList.isEmpty())
            throw new InvalidArgumentException("compList", compList);
        // create list or menu
        Object formatSize = getFormatOption(ii, FORMAT_SIZE, FORMAT_SIZE_ATTR);
        UISelectOne input = createSelectComponent(parent, context, formatSize);
        // set ValueExpressionType
        Object value = ii.getValue(false);
        if (value instanceof ValueExpression)
        {   // Set target class
            Class<?> exprType = ((ValueExpression)value).getType(context.getELContext());
            input.getAttributes().put(SelectInputControl.VALUE_EXPRESSION_TYPE, exprType);
        }
        // copy Attributes
        copyAttributes(parent, ii, input);
        // disabled
        boolean disabled = setDisabled(input, ii);
        // Options
        initOptions(input, ii.getTextResolver(), ii);
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
        if (!(comp instanceof UISelectOne))
        {
            throw new UnexpectedReturnValueException(comp.getClass().getName(), "parent.getChildren()");
        }
        UISelectOne input = (UISelectOne)comp;
        // required
        addRemoveStyle(input, TagStyleClass.INPUT_REQ, ii.isRequired());
        // disabled
        boolean disabled = setDisabled(input, ii);
        // check phase
        if (phaseId!=PhaseId.APPLY_REQUEST_VALUES)
        {   // Options (sync)
            syncOptions(input, ii.getTextResolver(), ii);
        }
        if (phaseId==PhaseId.RENDER_RESPONSE)
        {   // style
            addRemoveDisabledStyle(input, disabled);
            addRemoveInvalidStyle(input, ii.hasError());
            // set value
            setInputValue(input, ii);
        }
    }
    
    protected boolean setDisabled(UISelectOne input, InputInfo ii)
    {
        boolean disabled = ii.isDisabled();
        if (input instanceof HtmlSelectOneMenu)
            ((HtmlSelectOneMenu)input).setDisabled(disabled);
        else if (input instanceof HtmlSelectOneListbox)
            ((HtmlSelectOneListbox)input).setDisabled(disabled);
        else
            log.warn("Unable to set disabled attribute!");
        return disabled;
    }

    protected boolean isEmptyEntryRequired(UISelectOne input, Options options, InputInfo ii, Object currentValue)
    {
        if (input instanceof HtmlSelectOneListbox)
            return false; // not for listbox
        if (options!=null && options.containsNull())
        {   // already has an empty option
            return false;
        }
        // check required
        if (ii.isRequired()==false)
        {   // not required!   
            /* 
             * Old code: Why check this?
             * 
            Column column = ii.getColumn(); 
            if (column==null || !(ii.isDisabled() && !column.isRequired()))
            {   // add empty entry
                return true;
            }
            */
            return true;
        }
        // Check Value
        return ObjectUtils.isEmpty(currentValue);
    }

    public void initOptions(UISelectOne input, TextResolver textResolver, InputInfo ii)
    {
        // get the options
        Options options = ii.getOptions();
        if (options==null)
        {   // invalid options
            if (ii.getColumn()!=null)
                log.warn("No options given for column {}", ii.getColumn().getName());
            else
                log.warn("No options given for select tag {}", input.getClientId());
            options = new Options();
        }
        // current 
        Object currentValue = ii.getValue(true);
        if (isEmptyEntryRequired(input, options, ii, currentValue))
        {   // Empty entry
            addSelectItem(input, textResolver, new OptionEntry(null, getNullText(ii)));
        }
        if (options != null && options.size() > 0)
        {   // Add options
            for (OptionEntry oe : options)
            {   // Option entries
                if (oe.isActive() || ObjectUtils.compareEqual(oe.getValue(), currentValue))
                {   // add active or current item   
                    addSelectItem(input, textResolver, oe);
                }
                else if (log.isDebugEnabled())
                {   // not active, ignore this one
                    log.debug("Select item {} is not active.", oe.getValue());
                }
            }
        }
    }
    
    public void syncOptions(UISelectOne input, TextResolver textResolver, InputInfo ii)
    {
        // get the options
        Options options = ii.getOptions();
        if (options == null)
        { // clear or not?
            if (ii.getValue(false) != null)
                log.warn("No options have been set for column {}", ii.getColumn().getName());
            else
                input.getChildren().clear();
            return;
        }
        Object currentValue = ii.getValue(true);
        boolean hasEmpty = isEmptyEntryRequired(input, options, ii, currentValue);
        // boolean isInsideUIData = ii.isInsideUIData();
        // Compare child-items with options
        Iterator<OptionEntry> ioe = options.iterator();
        OptionEntry oe = (ioe.hasNext() ? ioe.next() : null);

        // get UISelectItems
        List<UIComponent> childList = input.getChildren();
        if (childList.isEmpty())
            childList.add(new UISelectItems());
        else if (childList.size()>1 && !(childList.get(1) instanceof UIParameter))
            log.warn("Unexpected number of child items ({}) for SelectInputControl of column {}", childList.size(), ii.getColumn().getName());
        UISelectItems items = (UISelectItems) childList.get(0);
        // get SelectItem list
        @SuppressWarnings("unchecked")
        List<SelectItem> selectItemList = (List<SelectItem>) items.getValue();
        if (selectItemList==null)
        {   selectItemList = new ArrayList<SelectItem>();
            items.setValue(selectItemList);
        }
        Iterator<SelectItem> ico = selectItemList.iterator();
        int lastIndex = 0;
        boolean emptyPresent = false;
        while (ico.hasNext())
        {
            lastIndex++;
            SelectItem si = ico.next();
            Object ov = si.getValue();
            // check empty
            if (ObjectUtils.isEmpty(ov) && hasEmpty)
            {   emptyPresent = true;
                continue;
            }
            // skip inactive
            while (oe != null && !oe.isActive())
            { // check for current
                if (ObjectUtils.compareEqual(oe.getValue(), currentValue))
                    break;
                // next oe
                oe = (ioe.hasNext() ? ioe.next() : null);
            }
            if (oe == null)
            { // remove obsolete items
                lastIndex--;
                for (int index = selectItemList.size() - 1; index >= lastIndex; index--)
                    selectItemList.remove(index);
                // done
                return;
            }
            if (ObjectUtils.compareEqual(ov, oe.getValue()))
            {   // update label and continue
                setItemLabel(si, textResolver, oe);
                oe = (ioe.hasNext() ? ioe.next() : null);
                continue;
            }
            // Not equal - do a full reload
            input.getChildren().clear();
            if (hasEmpty)
            {   // add empty entry
                addSelectItem(input, textResolver, new OptionEntry("", getNullText(ii)));
            }
            for (OptionEntry opt : options)
            { // Option entries
                if (opt.isActive() || ObjectUtils.compareEqual(opt.getValue(), currentValue))
                { // add active or current item
                    addSelectItem(input, textResolver, opt);
                }
            }
            // done
            return;
        }
        // check empty entry
        if (hasEmpty && !emptyPresent)
        { // add missing empty entry
            addSelectItem(input, textResolver, new OptionEntry("", getNullText(ii)), 0);
        }
        // Are there any items left?
        while (oe != null)
        { // add missing item
            if (oe.isActive() || ObjectUtils.compareEqual(oe.getValue(), currentValue))
            { // add item
                addSelectItem(input, textResolver, oe);
            }
            oe = (ioe.hasNext() ? ioe.next() : null);
        }
    }
    
    @SuppressWarnings("unchecked")
    public void addSelectItem(UIComponent input, TextResolver textResolver, OptionEntry oe, int pos)
    {
        List<UIComponent> children = input.getChildren();
        // UISelectItems
        UISelectItems items;
        List<SelectItem> list;
        if (children.isEmpty())
        {   // create and add UISelectItems
            items = new UISelectItems();
            children.add(items);
            list = new ArrayList<SelectItem>();
            items.setValue(list);
        }
        else
        {   // use existing UISelectItems
            items = ((UISelectItems) children.get(0));
            list = ((List<SelectItem>) items.getValue());
        }
        // set value
        Object value;
        Class<?> exprType = (Class<?>)input.getAttributes().get(SelectInputControl.VALUE_EXPRESSION_TYPE);
        if (exprType!=null)
        { // Use formatted value
            value = formatInputValue(oe.getValue(), exprType);
        }
        else
        { // Convert to String
            value = oe.getValueString();
        }
        // create and add item
        SelectItem selectItem = new SelectItem();
        selectItem.setValue(value);
        // set text
        setItemLabel(selectItem, textResolver, oe);
        // add item
        if (pos>=0)
            list.add(pos, selectItem);
        else
            list.add(selectItem);
    }

    public void addSelectItem(UIComponent input, TextResolver textResolver, OptionEntry e)
    {
        addSelectItem(input, textResolver, e, -1);
    }
    
    protected void setItemLabel(SelectItem si, TextResolver textResolver, OptionEntry oe)
    {
        String text = oe.getText();
        // only update if text is not a message key
        if (si.getLabel()!=null && text!=null && text.startsWith(TextResolver.MSG_KEY_INDICATOR))
            return;
        // set label
        text = textResolver.resolveText(text);
        si.setLabel(text);
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
            if (!hasFormatOption(vi, "nolookup"))
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
        if ((value instanceof Number)) 
        {   // Check whether it's an Enum
            Class<Enum<?>> enumType = ii.getColumn().getEnumType();
            if (enumType!=null) 
            {   // Convert ordinal to name
                Enum<?> enumVal = ObjectUtils.getEnum(enumType, value);
                value = enumVal.name();
            } 
        }
        // the value
        return formatInputValue(value, Object.class);
    }

    protected Object formatInputValue(Object value, Class<?> targetClass)
    {
        // the enum Value
        if ((value instanceof Enum<?>) && !targetClass.isEnum())
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
