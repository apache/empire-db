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

import java.io.Writer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.empire.struts2.html.HtmlTagDictionary;
import org.apache.empire.struts2.html.HtmlWriter;
import org.apache.empire.struts2.html.HtmlWriter.HtmlTag;
import org.apache.struts2.components.Anchor;

import com.opensymphony.xwork2.util.ValueStack;


public class AnchorComponent extends Anchor
{
    // Logger
    protected static Log log = LogFactory.getLog(AnchorComponent.class);
    
    private String  action;
    private String  text;
    /*
     * EMPIREDB-45 
     * CHANGE: attribute "target" added
     * Reason: the Attribute no longer exists in org.apache.struts2.components.Anchor 
     */
    private String  target;
    private boolean disabled = false;
    private String  disabledTag = null;

    public AnchorComponent(ValueStack stack, HttpServletRequest req, HttpServletResponse res)
    {
        super(stack, req, res);
    }
    
    public String getUrl(String actionName)
    {
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
    
    @Override
    public boolean usesBody()
    {
        // super.usesBody();
        return true; 
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean start(Writer writer)
    {
        // return super.start(writer);
        // evaluateParams(); // We need to call this!
        return true;
    }

    @Override
    public boolean end(Writer writer, String body)
    {
        // return super.end(writer, body);
        try {

            // Check writer
            if (writer==null)
                return false;
           
            // HtmlTagDictionary dic = HtmlTagDictionary.getInstance();  
            HtmlWriter htmlWriter = new HtmlWriter(writer);

            // The Anchors
            if (disabled==false)
            {
                String url = getUrl(action);

                HtmlTag a = htmlWriter.startTag("a");
                a.addAttribute("id",       this.getId());
                a.addAttribute("href",     url);
				a.addAttribute("target",   this.target);
                a.addAttribute("class",    this.cssClass);
                a.addAttribute("style",    this.cssStyle);
                a.addAttribute("onclick",  this.onclick);
                a.beginBody(text);
                a.endTag(body);
            }
            else
            {   
                // disabledTag = null
                HtmlTagDictionary dic = HtmlTagDictionary.getInstance();
                if (disabledTag == null)
                    disabledTag = dic.AnchorDisabledTag();
                if (cssClass ==null)
                    cssClass = dic.AnchorDisabledClass(); 
                // The value
                HtmlTag s = htmlWriter.startTag(disabledTag);
                s.addAttribute("class",    this.cssClass);
                s.addAttribute("style",    this.cssStyle);
                s.beginBody(text);
                s.endTag(body);
            }
            return false;
            
        } catch (Exception e) {
            log.error("error when rendering", e);
            return false;
        }
        finally {
            popComponentStack();
        }
    }
    
    public void setAction(String action)
    {
        this.action = action;
    }

    public void setText(String text)
    {
        this.text = text;
    }

    public void setDisabled(boolean disabled)
    {
        this.disabled = disabled;
    }

    public void setDisabledTag(String disabledTag)
    {
        this.disabledTag = disabledTag;
    }

    public String getTarget()
    {
        return target;
    }

    public void setTarget(String target)
    {
        this.target = target;
    }
}
