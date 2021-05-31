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

import java.util.Iterator;
import java.util.List;

import javax.el.ValueExpression;
import javax.faces.component.UIComponent;
import javax.faces.component.UISelectItem;
import javax.faces.component.UISelectOne;
import javax.faces.component.html.HtmlSelectOneListbox;
import javax.faces.component.html.HtmlSelectOneMenu;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseId;

import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.commons.OptionEntry;
import org.apache.empire.commons.Options;
import org.apache.empire.commons.StringUtils;
import org.apache.empire.data.Column;
import org.apache.empire.exceptions.InvalidArgumentException;
import org.apache.empire.exceptions.UnexpectedReturnValueException;
import org.apache.empire.jsf2.app.TextResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SelectInputControl extends InputControl
{
    private static final Logger log                   = LoggerFactory.getLogger(SelectInputControl.class);

    public static final String  COLATTR_ABBR_OPTIONS  = "ABBR_OPTIONS";                                   // Option list for abbreviations

    public static final String  VALUE_EXPRESSION_FLAG = "VALUE_EXPRESSION_FLAG";

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
        // setValueExpressionFlag
        Object value = ii.getValue(false);
        input.getAttributes().put(SelectInputControl.VALUE_EXPRESSION_FLAG, (value instanceof ValueExpression));
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
    	addRemoveStyle(input, InputControl.STYLECLASS_REQUIRED, ii.isRequired());
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
        if (options==null)
        {   // clear or not?
            if (ii.getValue(false)!=null)
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
            // skip inactive
            while (oe!=null && !oe.isActive())
            {   // check for current
                if (ObjectUtils.compareEqual(oe.getValue(), currentValue))
                    break;
                // next oe
                oe = (ioe.hasNext() ? ioe.next() : null);
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
                si.setItemLabel(oe.getText());
                oe = (ioe.hasNext() ? ioe.next() : null);
                continue;
            }
            // Not equal - do a full reload
            input.getChildren().clear();
            if (hasEmpty) {
                // add empty entry
                addSelectItem(input, textResolver, new OptionEntry("", getNullText(ii)));
            }
            for (OptionEntry opt : options)
            {   // Option entries
                if (opt.isActive() || ObjectUtils.compareEqual(opt.getValue(), currentValue))
                {   // add active or current item
                    addSelectItem(input, textResolver, opt);
                }
            }
            // done
            return;
        }
        // check empty entry
        if (hasEmpty && !emptyPresent)
        {   // add missing empty entry
            addSelectItem(input, textResolver, new OptionEntry("", getNullText(ii)), 0);
        }
        // Are there any items left?
        while (oe != null)
        {   // add missing item
            if (oe.isActive() || ObjectUtils.compareEqual(oe.getValue(), currentValue))
            {   // add item
                addSelectItem(input, textResolver, oe);
            }
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
        {   // Use formatted value
            value = formatInputValue(e.getValue());
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
                    String text = ((Options) attrValue).get(value);
                    if (StringUtils.isNotEmpty(text))
                        return vi.getText(text);
                    // Error
                    if (value!=null)
                        SelectInputControl.log.error("The element '" + String.valueOf(value) + "' is not part of the supplied option list.");
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
        return formatInputValue(value);
    }

    protected Object formatInputValue(Object value)
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
