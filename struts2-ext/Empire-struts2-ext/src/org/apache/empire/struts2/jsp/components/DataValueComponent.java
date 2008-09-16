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
package org.apache.empire.struts2.jsp.components;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.empire.struts2.html.HtmlWriter;
import org.apache.empire.struts2.html.HtmlWriter.HtmlTag;
import org.apache.empire.struts2.jsp.controls.InputControl;

import com.opensymphony.xwork2.util.ValueStack;


public class DataValueComponent extends ControlComponent
{
    // Logger
    @SuppressWarnings("hiding")
    protected static Log log = LogFactory.getLog(DataValueComponent.class);

    // the wrapper Tag
    private String htmlTag;
    
    // Link
    private String action;
    private Object alt; // for the <a title="xxx" tag (alternative text)
    private String anchorClass;
    
    public DataValueComponent(InputControl control, ValueStack stack, HttpServletRequest req, HttpServletResponse res)
    {
        super(control, stack, req, res);
    }
    
    public String getURL(String actionName)
    {
        // JAVASCRIPT ?
        if (actionName.startsWith("javascript:"))
            return actionName;
        
        String namespace = null;
        String method = null;
        String scheme = null;
        boolean includeContext = true;
        boolean encodeResult = true;
        boolean forceAddSchemeHostAndPort = false;
        boolean escapeAmp = true;        
        return this.determineActionURL(actionName, namespace, method, request, response, parameters, scheme, 
                                       includeContext, encodeResult, forceAddSchemeHostAndPort, escapeAmp);
    }
    
    @SuppressWarnings("unchecked")
    public void setParam(String key, String value)
    {
        if (value==null)
        {   // Action
            log.error("Action parameter '" + key + "' cannot be set to null!");
            return;
        }
        parameters.put(key, value);
    }
    
    // <td class="???"><a href="action!method" class=""/>value</a></td>
    
    @Override
    protected void render(HtmlWriter hw, String body, InputControl control)
    {
        HtmlTag td = hw.startTag(htmlTag);
        td.addAttribute("class", this.cssClass);
        td.addAttribute("style", this.cssStyle);
        td.beginBody();
        // Add Link?
        HtmlTag anchor = null;
        if (action!=null)
        {
            String url = getURL(action);
            // print href
            anchor = hw.startTag("a");
            anchor.addAttribute("href", url);
            anchor.addAttribute("title", alt);
            anchor.addAttribute("class", anchorClass);
            anchor.addAttribute("onclick", onclick);
            anchor.beginBody();
        }
        // Body prepend
        if (usesBody() && "append".equalsIgnoreCase(bodyUsage)==false)
            hw.print(body);
        // Render Data
        control.renderText(hw, this);
        // Body append
        if (usesBody() && "append".equalsIgnoreCase(bodyUsage))
            hw.print(body);
        // close anchor
        if (anchor!=null)
            anchor.endTag();
        // close td
        td.endTag();
    }

    public void setHtmlTag(String htmlTag)
    {
        this.htmlTag = htmlTag;
    }

    public void setAction(String action)
    {
        this.action = action;
    }

    public void setAnchorClass(String anchorClass)
    {
        this.anchorClass = anchorClass;
    }
    
    public void setAlt(Object alt)
    {
        this.alt = alt;
    }
}
