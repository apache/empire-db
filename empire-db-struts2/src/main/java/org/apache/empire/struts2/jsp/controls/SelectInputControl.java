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

import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.commons.OptionEntry;
import org.apache.empire.commons.Options;
import org.apache.empire.struts2.html.HtmlWriter;
import org.apache.empire.struts2.html.HtmlWriter.HtmlTag;


public class SelectInputControl extends InputControl
{

    @Override
    public void renderInput(HtmlWriter writer, ControlInfo ci)
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

}
