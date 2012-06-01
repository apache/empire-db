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

import java.util.List;

import javax.faces.component.UIComponent;
import javax.faces.component.UISelectItem;
import javax.faces.component.html.HtmlSelectOneMenu;
import javax.faces.context.FacesContext;

import org.apache.empire.commons.OptionEntry;
import org.apache.empire.commons.Options;
import org.apache.empire.data.Column;
import org.apache.empire.exceptions.InternalException;
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
    
    @Override
    protected void createInputComponents(UIComponent parent, InputInfo ii, FacesContext context, List<UIComponent> compList)
    {
        HtmlSelectOneMenu input;
		try {
			input = inputComponentClass.newInstance();
		} catch (InstantiationException e1) {
			throw new InternalException(e1);
		} catch (IllegalAccessException e2) {
			throw new InternalException(e2);
		}
        copyAttributes(parent, ii, input);

        Options options = ii.getOptions();
        if (ii.isRequired()==false)
        {   // Empty entry
            options = new Options(options);
            addSelectItem(input, ii, new OptionEntry("", getNullText(ii)));
        }
        if (options!=null && options.size()>0)
        {   // Add options
            for (OptionEntry e : options)
            { // Option entries
                addSelectItem(input, ii, e);
            }
        }
        
        input.setDisabled(ii.isDisabled());
        setInputValue(input, ii);
        
        compList.add(input);
    }
    
    private String getNullText(InputInfo ii)
    {
        String nullText = getFormatString(ii, FORMAT_NULL, FORMAT_NULL_ATTRIBUTE);
        return (nullText!=null) ? ii.getText(nullText) : "";
    }

    private void addSelectItem(UIComponent input, InputInfo ii, OptionEntry e)
    {
        UISelectItem selectItem = new UISelectItem();
        // set value
        selectItem.setItemValue(e.getValueString());
        // set text
        String text = e.getText();
        text = ii.getText(text);
        selectItem.setItemLabel(text);
        // add item
        input.getChildren().add(selectItem);
    }

    @Override
    protected String formatValue(Object value, ValueInfo vi, boolean hasError)
    {
        // Lookup and Print value
        if (vi.getOptions()==null)
        {
            log.warn("Select field {} has no Option list attached!", vi.getColumn().getName());
            return super.formatValue(value, vi, hasError);
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
        return super.formatValue(value, vi, hasError);
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
