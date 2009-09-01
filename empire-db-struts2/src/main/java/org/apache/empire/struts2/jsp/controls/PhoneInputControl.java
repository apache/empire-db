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

import org.apache.empire.data.Column;
import org.apache.empire.struts2.action.RequestParamProvider;
import org.apache.empire.struts2.html.HtmlWriter;
import org.apache.empire.struts2.html.HtmlWriter.HtmlTag;


public class PhoneInputControl extends InputControl
{
    // public static String TYPE_NAME = "phone";
    /*
    <input type="text" name="employee.lastName" size="40" value="" id="employeeDetail_employee_lastName"/
    */
    
    private static final String LKZ = "__LKZ"; // Landeskennzahl
    private static final String OKZ = "__OKZ"; // Ortskennzahl
    private static final String TKZ = "__TKZ"; // Teilenehmerkennzahl
    
    @Override
    public Object getFieldValue(String name, RequestParamProvider request, Locale locale, Column column)
    {
        String lkz = request.getRequestParam(name + LKZ);
        String okz = request.getRequestParam(name + OKZ);
        String tnk = request.getRequestParam(name + TKZ);
        if (lkz==null && okz==null && tnk==null)
            return NO_VALUE;
        // Get Phone number
        return getPhone(lkz, okz, tnk);
    }
    
    @Override
    public void renderInput(HtmlWriter writer, ControlInfo ci)
    {
        String phone = formatValue(ci);
        String lkz = GetLKZ(phone); 
        String okz = GetOKZ(phone); 
        String tnr = GetTNR(phone); 
        
        renderPart(writer, ci, LKZ, lkz, 4, false);
        writer.print(" - ");
        renderPart(writer, ci, OKZ, okz, 8, false);
        writer.print(" - ");
        renderPart(writer, ci, TKZ, tnr, 20, true);
    }

    // Phone specific helper functions
    private void renderPart(HtmlWriter writer, ControlInfo ci, String namePostFix, String part, int size, boolean setIdForLabel)
    {
        HtmlTag input = writer.startTag("input");
        input.addAttribute("type", "text");
        input.addAttribute("id",   (setIdForLabel) ? ci.getId() : null);
        input.addAttribute("class", ci.getCssClass());
        input.addAttribute("style", ci.getCssStyle());
        input.addAttribute("size",  size);
        if (ci.getDisabled()==false)
        {   
            input.addAttribute("name",  ci.getName() + namePostFix);
            input.addAttribute("maxLength", size);
        }
        else
        {
            input.addAttribute("disabled");
        }
        input.addAttribute("value", part);
        // Event Attributes
        input.addAttribute("onclick",   ci.getOnclick());
        input.addAttribute("onchange",  ci.getOnchange());
        input.addAttribute("onfocus",   ci.getOnfocus());
        input.addAttribute("onblur",    ci.getOnblur());
        input.endTag();
    }
    
    private String GetLKZ(String phone)
    {
        if (phone == null)
        	return"";
    	int idx = phone.indexOf('-');
        if (idx < 0)
            return ""; 
        int end = phone.indexOf('-', idx+1);
        if (end<0)
            return "";
        // done
        return (idx>0) ? phone.substring( 0, idx) : "";
    }
    private String GetOKZ(String phone)
    {
    	if (phone == null)
        	return"";
    	int idx = phone.indexOf('-');
        if (idx < 0)
            return ""; 
        int end = phone.indexOf('-', idx+1);
        if (end<0)
            return phone.substring( 0, idx);
        // done
        return phone.substring(idx+1, end);
    }
    private String GetTNR(String phone) 
    {
    	if (phone == null)
        	return"";
    	int idx = phone.indexOf('-');
        if (idx < 0)
            return phone; 
        int end = phone.indexOf('-', idx+1);
        if (end<0)
            return phone.substring( idx+1 );
        // done
        return phone.substring(end+1);
    }

    /**
     * return the complete telefon number (with international area code and area
     * code) as one String
     * 
     * @param lkz
     *            the international area code
     * @param okz
     *            the area code
     * @param tkn
     *            the telephone number
     */
    private String getPhone(String lkz, String okz, String tkn)
    {
        StringBuffer buf = new StringBuffer();
        if (lkz != null && lkz.length() > 0)
        {
            // Landeskennzahl
            lkz = lkz.trim();
            // replace dashes
            if (lkz.indexOf('-') >= 0)
                lkz = lkz.replace('-', '$');
            // replace leading '00' with '+'
            if (lkz.startsWith("00"))
            {
                buf.append("+");
                buf.append(lkz.substring(2));
            } else
            { // append leading '+' if misssing
                if (lkz.startsWith("+") == false)
                    buf.append("+");
                // append lkz
                buf.append(lkz);
            }
        }
        // DMB 20030616 always append the - even if there is no lkz
        buf.append("-"); 

        // Area code
        if (okz != null && okz.length() > 0)
        {
            okz = okz.trim();
            // replace all '-' with '$'
            if (okz.indexOf('-') >= 0)
                okz = okz.replace('-', '$');
            // append okz
            buf.append(okz);
        }
        // DMB 20030616 always append the - even if there is no okz
        buf.append("-"); 

        // Telephone number
        if (tkn != null && tkn.length() > 0)
        {
            tkn = tkn.trim();
            buf.append(tkn);
        }

        // check phone
        String phone = buf.toString();
        if (phone.equals("--"))
            return ""; // Empty Phone Number
        // done
        return phone;
    }
    
}
