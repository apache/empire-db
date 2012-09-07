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

import javax.faces.component.UIComponent;
import javax.faces.component.UISelectItem;
import javax.faces.component.html.HtmlSelectOneMenu;
import javax.faces.context.FacesContext;

import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.commons.OptionEntry;
import org.apache.empire.commons.Options;
import org.apache.empire.data.Column;
import org.apache.empire.exceptions.InternalException;
import org.apache.empire.exceptions.UnexpectedReturnValueException;
import org.apache.empire.jsf2.app.TextResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SelectInputControl extends InputControl
{
    private static final Logger log = LoggerFactory.getLogger(SelectInputControl.class);

    public static final String COLATTR_ABBR_OPTIONS   = "ABBR_OPTIONS";     // Option list for abbreviations
    
    public static final String NAME = "select";

    private Class<? extends javax.faces.component.html.HtmlSelectOneMenu> inputComponentClass;

    public SelectInputControl(Class<? extends HtmlSelectOneMenu> inputComponentClass)
    {
        super(NAME);
        this.inputComponentClass = inputComponentClass;
    }

    public SelectInputControl()
    {
        this(javax.faces.component.html.HtmlSelectOneMenu.class);
    }
    
    /* for SelectTag (when no column is available) */
    public HtmlSelectOneMenu createMenuComponent(UIComponent parent)
    {
        try {
            return inputComponentClass.newInstance();
        } catch (InstantiationException e1) {
            throw new InternalException(e1);
        } catch (IllegalAccessException e2) {
            throw new InternalException(e2);
        }
    }
    
    @Override
    protected void createInputComponents(UIComponent parent, InputInfo ii, FacesContext context, List<UIComponent> compList)
    {
        HtmlSelectOneMenu input;
        if (compList.size()==0)
        {   try {
                input = inputComponentClass.newInstance();
            } catch (InstantiationException e1) {
                throw new InternalException(e1);
            } catch (IllegalAccessException e2) {
                throw new InternalException(e2);
            }
            // copy Attributes
            copyAttributes(parent, ii, input);
            // disabled
            boolean disabled = ii.isDisabled(); 
            input.setDisabled(disabled);
            // Options
            Options options = ii.getOptions();
            boolean hasEmpty =(!ii.isRequired() && !(disabled && ii.getColumn().isRequired()) && !options.contains(""));
            String nullText = (hasEmpty) ? getNullText(ii) : "";
            initOptions(input, ii.getTextResolver(), options, hasEmpty, nullText);
            // add
            compList.add(input);
        }
        else
        {   // check type
            UIComponent comp = compList.get(0);
            if (!(comp instanceof HtmlSelectOneMenu))
                throw new UnexpectedReturnValueException(comp.getClass().getName(), "compList.get");
            // cast
            input = (HtmlSelectOneMenu)comp;
            // disabled
            boolean disabled = ii.isDisabled(); 
            input.setDisabled(disabled);
            // Options (sync)
            Options options = ii.getOptions();
            boolean hasEmpty =(!ii.isRequired() && !(disabled && ii.getColumn().isRequired()) && !options.contains(""));
            String nullText = (hasEmpty) ? getNullText(ii) : "";
            syncOptions(input, ii.getTextResolver(), options, hasEmpty, nullText);
        }

        // style
        addRemoveDisabledStyle(input, input.isDisabled());
        
        // Set Value
        setInputValue(input, ii);
    }

    public void initOptions(HtmlSelectOneMenu input, TextResolver textResolver, Options options, boolean addEmpty, String nullText)
    {
        if (addEmpty)
        {   // Empty entry
            addSelectItem(input, textResolver, new OptionEntry("", nullText));
        }
        if (options!=null && options.size()>0)
        {   // Add options
            for (OptionEntry e : options)
            { // Option entries
                addSelectItem(input, textResolver, e);
            }
        }
    }
    
    public void syncOptions(HtmlSelectOneMenu input, TextResolver textResolver, Options options, boolean hasEmpty, String nullText)
    {
        // Compare child-items with options
        Iterator<OptionEntry> ioe = options.iterator();
        OptionEntry oe =(ioe.hasNext() ? ioe.next() : null);
        List<UIComponent> childList = input.getChildren();
        Iterator<UIComponent> ico = childList.iterator();
        int lastIndex = 0;
        while (ico.hasNext())
        {
            lastIndex++;
            UIComponent co = ico.next(); 
            if (!(co instanceof UISelectItem))
                continue;
            UISelectItem si = (UISelectItem)co;
            Object ov = si.getItemValue();
            if (ObjectUtils.isEmpty(ov) && hasEmpty)
                continue;
            if (oe==null)
            {   // remove obsolete items
                lastIndex--; 
                for (int index = childList.size()-1; index>=lastIndex; index--)
                    childList.remove(index);
                // done
                return;
            }    
            if (ObjectUtils.compareEqual(ov, oe.getValue()))
            {   // next
                oe =(ioe.hasNext() ? ioe.next() : null);
                continue;
            }    
            // Not equal - do a full reload
            input.getChildren().clear();
            if (hasEmpty)
                addSelectItem(input, textResolver, new OptionEntry("", nullText));
            for (OptionEntry e : options)
            {   // Option entries
                addSelectItem(input, textResolver, e);
            }
            // done
            return;
        }
        // Are there any items left?
        while(oe!=null)
        {   // add missing item
            addSelectItem(input, textResolver, oe);
            oe =(ioe.hasNext() ? ioe.next() : null);
        }
    }

    public void addSelectItem(UIComponent input, TextResolver textResolver, OptionEntry e)
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
    
    private String getNullText(InputInfo ii)
    {
        String nullText = getFormatString(ii, FORMAT_NULL, FORMAT_NULL_ATTRIBUTE);
        return (nullText!=null) ? ii.getText(nullText) : "";
    }

    @Override
    protected String formatValue(Object value, ValueInfo vi)
    {
        // Lookup and Print value
        if (vi.getOptions()==null)
        {
            log.warn("Select field {} has no Option list attached!", vi.getColumn().getName());
            return super.formatValue(value, vi);
        }
        // Check for Abbreviation
        if (hasFormatOption(vi, "short"))
        {
            Column column = vi.getColumn();
            if (column!=null)
            {   // Check for Abbreviation option list
                Object attrValue = column.getAttribute(COLATTR_ABBR_OPTIONS);
                if (attrValue instanceof Options)
                { // Check for Options
                    String text = ((Options)attrValue).get(value);
                    if (text != null)
                        return vi.getText(text);
                    // Error
                    log.error("The element '" + String.valueOf(value) + "' is not part of the supplied option list.");
                }
            }
        }
        return super.formatValue(value, vi);
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
