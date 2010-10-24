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

import org.apache.commons.lang.xwork.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.empire.struts2.html.HtmlWriter;
import org.apache.empire.struts2.html.HtmlWriter.HtmlTag;
import org.apache.struts2.components.Form;

import com.opensymphony.xwork2.util.ValueStack;


public class FormComponent extends Form
{
    // Logger
    protected static Log log = LogFactory.getLog(ControlComponent.class);
    
    // Internal use only
    private HtmlTag formTag = null;
    private boolean readOnly = false;

    /*
    <form id="login" name="login" onsubmit="return true;" action="/dbsample/login!doLogin.action;jsessionid=5A7C79EFBDCEB97C83918726B7D7EC3D" method="post">
    <table class="wwFormTable">
    */
    
    public FormComponent(ValueStack stack, HttpServletRequest request, HttpServletResponse response)
    {
        super(stack, request, response);
    }

    @Override
    public boolean start(Writer writer)
    {
        evaluateParams(); // We need to call this!
        try {
            
            HtmlWriter htmlWriter = new HtmlWriter(writer);

            // render form Tag?
            if (readOnly==false)
            {
                urlRenderer.renderFormUrl(this);
                formTag = htmlWriter.startTag("form");
                formTag.addAttribute("id",       getId());
                formTag.addAttribute("name",     getName());
                formTag.addAttribute("onsubmit", this.onsubmit);
                formTag.addAttribute("action",   getURL(action));
                formTag.addAttribute("target",   this.target);
                formTag.addAttribute("method",   this.method);
                formTag.addAttribute("enctype",  this.enctype);
                formTag.beginBody(true);
            }
            
        } catch (Exception e) {
            log.error("error when rendering", e);
        }
        return true;
    }
    
    @Override
    public boolean end(Writer writer, String body)
    {
        // super.end(writer, body);
        // evaluateParams();
        try {
            
            if (formTag!=null)
                formTag.endTag(true);
            
            return false; // do not evaluate body again!
            
        } catch (Exception e) {
            log.error("error when rendering", e);
            return false; // do not evaluate body again!
        }
        finally {
            popComponentStack();
        }
    }

    @Override
    public String getId()
    {
        if (StringUtils.isEmpty(id)==false)
        	return id;
        // Check for id Attribute	
    	Object p = getParameters().get("id");
        return (p!=null) ? p.toString() : null;
    }

    public String getName()
    {
        if (StringUtils.isEmpty(name)==false)
        	return name;
        // Check for id Attribute	
    	Object p = getParameters().get("name");
        return (p!=null) ? p.toString() : null;
    }

    private String getURL(String action)
    {
        Object url = getParameters().get("action");
        return String.valueOf(url);
    }

    public void setReadOnly(boolean readOnly)
    {
        this.readOnly = readOnly;
    }
    
}
