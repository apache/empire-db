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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.commons.OptionEntry;
import org.apache.empire.commons.Options;
import org.apache.empire.commons.Options.OptionGroupResolver;
import org.apache.empire.data.Column;
import org.apache.empire.exceptions.InvalidArgumentException;
import org.apache.empire.exceptions.UnexpectedReturnValueException;
import org.apache.empire.jakarta.app.TextResolver;
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
import jakarta.faces.model.SelectItemGroup;

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
        setDisabled(input, ii);
        // Options
        initOptions(input, ii.getTextResolver(), ii);
        // add
        compList.add(input);
        // style
        setInputStyleClass(input, ii);
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
        // disabled
        setDisabled(input, ii);
        // check phase
        if (phaseId!=PhaseId.APPLY_REQUEST_VALUES)
        {   // Options (sync)
            syncOptions(input, ii.getTextResolver(), ii);
        }
        if (phaseId==PhaseId.RENDER_RESPONSE)
        {   // style
            setInputStyleClass(input, ii);
            // set value
            setInputValue(input, ii);
        }
    }
    
    protected void setDisabled(UISelectOne input, InputInfo ii)
    {
        boolean disabled = ii.isDisabled();
        if (input instanceof HtmlSelectOneMenu)
            ((HtmlSelectOneMenu)input).setDisabled(disabled);
        else if (input instanceof HtmlSelectOneListbox)
            ((HtmlSelectOneListbox)input).setDisabled(disabled);
        else
            log.warn("Unable to set disabled attribute!");
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

    /**
     * SelectGroup
     * helper class for building SelectItemGroups
     */
    protected static class SelectGroup
    {
        private final SelectItemGroup  selectItemGroup;
        private final List<SelectItem> groupItemList;
        public SelectGroup(SelectItemGroup  selectItemGroup)
        {
            this.selectItemGroup = selectItemGroup;
            this.groupItemList = new ArrayList<SelectItem>();
        }
        public List<SelectItem> getItemList()
        {
            return groupItemList;
        }
        public void peg()
        {
            SelectItem[] items = ObjectUtils.listToArray(SelectItem[].class, groupItemList);
            selectItemGroup.setSelectItems(items);
        }
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
        // list and type
        Class<?> exprType = (Class<?>)input.getAttributes().get(SelectInputControl.VALUE_EXPRESSION_TYPE);
        List<SelectItem> selectItemList = getSelectItemList(input);
        // current 
        Object currentValue = ii.getValue(true);
        if (isEmptyEntryRequired(input, options, ii, currentValue))
        {   // Empty entry
            addSelectItem(selectItemList, textResolver, new OptionEntry(null, getNullText(ii)), exprType);
        }
        if (options != null && options.size() > 0)
        {   // Option grouping?
            OptionGroupResolver optionGroupResolver = options.getOptionGroupResolver();
            Map<Object, SelectGroup> groupMap = (optionGroupResolver!=null ? new HashMap<Object, SelectGroup>() : null);
            // Add options
            for (OptionEntry oe : options)
            {   // Option entries
                if (oe.isActive() || ObjectUtils.compareEqual(oe.getValue(), currentValue))
                {   // add active or current item
                    List<SelectItem> list = selectItemList; 
                    if (optionGroupResolver!=null)
                    {   // get the option group
                        Object group = optionGroupResolver.getGroup(oe);
                        if (group!=null)
                        {   // We have a group
                            SelectGroup selectGroup = groupMap.get(group);
                            if (selectGroup==null)
                            {   // Create a new group
                                String groupLabel = (group!=null ? textResolver.resolveText(group.toString()) : null); 
                                SelectItemGroup selectItemGroup = new SelectItemGroup(groupLabel);
                                selectItemList.add(selectItemGroup);
                                // add group to map
                                selectGroup = new SelectGroup(selectItemGroup);
                                groupMap.put(group, selectGroup);
                            }
                            list = selectGroup.getItemList();
                        }
                    }
                    addSelectItem(list, textResolver, oe, exprType);
                }
                else if (log.isDebugEnabled())
                {   // not active, ignore this one
                    log.debug("Select item {} is not active.", oe.getValue());
                }
            }
            // complete groups
            if (groupMap!=null)
            {   // Peg all SelectItemGroups
                for (SelectGroup group : groupMap.values())
                    group.peg();
                groupMap.clear();
            }
        }
    }
    
    public void syncOptions(UISelectOne input, TextResolver textResolver, InputInfo ii)
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
        
        // check grouping
        OptionGroupResolver optionGroupResolver = options.getOptionGroupResolver();
        if (optionGroupResolver!=null)
        {   // not (yet) supported
            log.debug("SyncOptions is not supported for grouped SelectItems for column {}", ii.getColumn().getName());
            return;
        }
        
        // list and type
        Class<?> exprType = (Class<?>)input.getAttributes().get(SelectInputControl.VALUE_EXPRESSION_TYPE);
        List<SelectItem> selectItemList = getSelectItemList(input);

        // prepare
        Object currentValue = ii.getValue(true);
        boolean hasEmpty = isEmptyEntryRequired(input, options, ii, currentValue);
        // boolean isInsideUIData = ii.isInsideUIData();
        // Compare child-items with options
        Iterator<OptionEntry> ioe = options.iterator();
        OptionEntry oe = (ioe.hasNext() ? ioe.next() : null);
        
        // sync
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
            selectItemList.clear();
            if (hasEmpty)
            {   // add empty entry
                addSelectItem(selectItemList, textResolver, new OptionEntry("", getNullText(ii)), exprType);
            }
            for (OptionEntry opt : options)
            { // Option entries
                if (opt.isActive() || ObjectUtils.compareEqual(opt.getValue(), currentValue))
                { // add active or current item
                    addSelectItem(selectItemList, textResolver, opt, exprType);
                }
            }
            // done
            return;
        }
        // check empty entry
        if (hasEmpty && !emptyPresent)
        { // add missing empty entry
            addSelectItem(selectItemList, textResolver, new OptionEntry("", getNullText(ii)), exprType, 0);
        }
        // Are there any items left?
        while (oe != null)
        { // add missing item
            if (oe.isActive() || ObjectUtils.compareEqual(oe.getValue(), currentValue))
            { // add item
                addSelectItem(selectItemList, textResolver, oe, exprType);
            }
            oe = (ioe.hasNext() ? ioe.next() : null);
        }
    }
    
    protected List<SelectItem> getSelectItemList(UISelectOne input)
    {
        List<UIComponent> children = input.getChildren();
        // UISelectItems
        if (children.isEmpty())
            children.add(new UISelectItems());
        else if (children.size()>1 && !(children.get(1) instanceof UIParameter))
            log.warn("Unexpected number of child items ({}) for SelectInputControl", children.size());
        UISelectItems items = (UISelectItems) children.get(0);
        // List<SelectItem>
        @SuppressWarnings("unchecked")
        List<SelectItem> selectItemList = (List<SelectItem>) items.getValue();
        if (selectItemList==null)
        {   selectItemList = new ArrayList<SelectItem>();
            items.setValue(selectItemList);
        }
        return selectItemList;
    }
    
    public void addSelectItem(List<SelectItem> list, TextResolver textResolver, OptionEntry oe, Class<?> exprType, int pos)
    {
        // set value
        Object value;
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

    public void addSelectItem(List<SelectItem> list, TextResolver textResolver, OptionEntry e, Class<?> exprType)
    {
        addSelectItem(list, textResolver, e, exprType, -1);
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
            {   // Convert ordinal to enum
                value = ObjectUtils.getEnum(enumType, value);
            } 
        }
        // the value
        return formatInputValue(value, Object.class);
    }

    protected Object formatInputValue(Object value, Class<?> targetClass)
    {
        // the enum Value
        if ((value instanceof Enum<?>) && !targetClass.isEnum())
            return ObjectUtils.getString(value);
        // the value
        return value;
    }

    @Override
    protected Object parseInputValue(String value, InputInfo ii)
    {
        Class<Enum<?>> enumType = ii.getColumn().getEnumType();
        if (enumType!=null)
        {   // convert to enum
            return ObjectUtils.getEnum(enumType, value);
        }
        return value;
    }

}
