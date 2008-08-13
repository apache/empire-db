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
package org.apache.empire.struts2.jsp.controls;

import java.util.Locale;

import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.commons.OptionEntry;
import org.apache.empire.commons.Options;
import org.apache.empire.data.Column;
import org.apache.empire.struts2.action.RequestParamProvider;
import org.apache.empire.struts2.html.HtmlWriter;
import org.apache.empire.struts2.html.HtmlWriter.HtmlTag;


public class RadioInputControl extends InputControl
{
    private static String RBCHECK_POSTFIX = "__RBCHECK";

    @Override
    public Object getFieldValue(String name, RequestParamProvider request, Locale locale, Column column)
    {
        Object val = super.getFieldValue(name, request, locale, column);
        if (val==NO_VALUE)
        {   // Is Hidden Value present   
            if (request.getRequestParam(name + RBCHECK_POSTFIX)!=null)
                return NULL_VALUE;
        }
        return val;
    }
    
    @Override
    public void renderInput(HtmlWriter writer, ControlInfo ci)
    {
        HtmlTag radioGroupWrapper = writer.startTag("table");
        radioGroupWrapper.addAttribute("cellspacing", "0");
        radioGroupWrapper.addAttribute("cellpadding", "0");
        radioGroupWrapper.addAttribute("id",    ci.getId());
        radioGroupWrapper.addAttribute("class", ci.getCssClass());
        radioGroupWrapper.addAttribute("style", ci.getCssStyle());
        radioGroupWrapper.beginBody();
        
        // Get options
        Options options = ci.getOptions();
        if (options!=null)
        {   // Doing a label top, radio bottom table layout
            if (hasFormatOption(ci, "labelAbove"))
            {
                renderLabelAboveControl(writer, ci, options);
            }
            else
            {   // Label next to control (to the left or to the right)
                renderLabelNextToControl(writer, ci, options, hasFormatOption(ci, "labelLeft"));
            }
            // Add hidden field for NULL_VALUE checking
            if (options.contains(ci.getValue())==false && ci.getDisabled()==false)
            {   // Additionally add a hidden field
                // to detect unchecked state
                HtmlTag hidden = writer.startTag("input");
                hidden.addAttribute("type",  "hidden");
                hidden.addAttribute("name",  ci.getName() + RBCHECK_POSTFIX);
                hidden.addAttribute("value", ci.getValue());
                hidden.endTag();
            }
        }
        else
        {   // Error
            log.error("No option list supplied for control type 'radio'");
        }
        // End Tag
        radioGroupWrapper.endTag();       
    }
    
    // render helpers
    private void renderInput(HtmlWriter writer, ControlInfo ci, OptionEntry entry, int pairs)
    {
        boolean disabled = ci.getDisabled();
        // Get the current value
        Object current = ci.getValue();
        Object value = entry.getValue();
        // Check if the current value is the option being rendered
        HtmlTag input = writer.startTag("input");
        input.addAttribute("type", "radio");        
        input.addAttribute("id", ci.getId() + pairs);
        input.addAttribute("value", value);
        input.addAttribute("name", ci.getName());
        // Set Checked
        if(ObjectUtils.compareEqual(current, value))
        {
            input.addAttribute("checked");
        }
        if (disabled)
        {
            input.addAttribute("disabled");
        }
        // Event Attributes
        input.addAttribute("onclick",   ci.getOnclick());
        input.addAttribute("onchange",  ci.getOnchange());
        input.addAttribute("onfocus",   ci.getOnfocus());
        input.addAttribute("onblur",    ci.getOnblur());
        input.endTag();        
    }

    private void renderLabel(HtmlWriter writer, ControlInfo ci, OptionEntry entry, int pairs)
    {
        HtmlTag label = writer.startTag("label");
        label.addAttribute("for", ci.getId() + pairs);
        label.beginBody(ci.getTranslation(entry.getText()));
        label.endTag();
    }
    
    private void renderLabelAboveControl(HtmlWriter writer, ControlInfo ci, Options options)
    {
        int pairs = 1;
        HtmlTag row = writer.startTag("tr");
        // do label row (th)
        row.beginBody();
        for (OptionEntry entry : options)
        {
            HtmlTag td = writer.startTag("th");
            td.beginBody();
            renderLabel(writer, ci, entry, pairs);
            td.endTag();
            pairs ++;
        }
        row.endTag();
        
        // do radio input row (td)
        pairs = 1;
        row = writer.startTag("tr");
        row.beginBody();
        for (OptionEntry entry : options)
        {
            HtmlTag td = writer.startTag("td");
            td.beginBody();
            renderInput(writer, ci, entry, pairs);
            td.endTag();
            pairs ++;
        }
        row.endTag();
    }
    
    private void renderLabelNextToControl(HtmlWriter writer, ControlInfo ci, Options options, boolean labelFirst)
    {
        int pairs = 1;        
        // Render out the radio options in a single row table
        HtmlTag row = writer.startTag("tr");
        row.beginBody();
        for (OptionEntry entry : options)
        {   
            HtmlTag radioOptionWrapper = writer.startTag("td");
            radioOptionWrapper.beginBody();
            // Decide render order: (label, input) or (input, label)
            if( labelFirst )
            {   // render td's with label buttons first then radio
                renderLabel(writer, ci, entry, pairs);
                renderInput(writer, ci, entry, pairs);                
            }
            else
            {   // render td's with radio buttons first then label
                renderInput(writer, ci, entry, pairs);                
                renderLabel(writer, ci, entry, pairs);
            }
            pairs ++;                
            radioOptionWrapper.endTag();
        }
        row.endTag();
    }
}
