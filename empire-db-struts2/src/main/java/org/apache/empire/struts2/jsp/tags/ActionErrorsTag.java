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
package org.apache.empire.struts2.jsp.tags;

import java.util.Collection;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;

import org.apache.empire.commons.ErrorInfo;
import org.apache.empire.struts2.action.ActionErrorProvider;
import org.apache.empire.struts2.html.HtmlTagDictionary;
import org.apache.empire.struts2.html.HtmlWriter;
import org.apache.empire.struts2.html.HtmlWriter.HtmlTag;
import org.apache.struts2.components.Component;

import com.opensymphony.xwork2.util.ValueStack;


@SuppressWarnings("serial")
public class ActionErrorsTag extends EmpireTagSupport
{
    private String actionErrorClass;
    private String fieldErrorClass;
    
    @Override
    protected void resetParams()
    {
        actionErrorClass = null;
        fieldErrorClass = null;
        // Value
        super.resetParams();
    }

    @Override
    public Component getBean(ValueStack stack, HttpServletRequest req, HttpServletResponse res)
    {
        return null;
    }

    @Override
    public int doStartTag()
        throws JspException
    {       
        // Get the action
        Object action = getAction();
        if (action instanceof ActionErrorProvider)
        {   // Tabel cell tag
            HtmlWriter w = new HtmlWriter(pageContext.getOut());
            renderAllErrors(w, (ActionErrorProvider)action);
        }
        else
        {   // Error: Action does implement ActionErrorProvider 
            log.error("Cannot render errors. Action does implement ActionErrorProvider.");
        }
        // wrap.beginBody(getTextValue());
        // return usesBody ? EVAL_BODY_BUFFERED : EVAL_BODY_INCLUDE;
        return SKIP_BODY;
    }

    @Override
    public int doEndTag()
        throws JspException
    {   // done
        resetParams();
        return EVAL_PAGE;
    }
    
    // ------- render helpers -------
    
    @SuppressWarnings("null")
    private void renderAllErrors(HtmlWriter w, ActionErrorProvider provider)
    {
        // Get errors
        ErrorInfo lastActionError = provider.getLastActionError(true);
        Map<String, ErrorInfo> fieldErrors = provider.getItemErrors();
        
        boolean hasActionError = (lastActionError!=null && lastActionError.hasError());
        boolean hasFieldErrors = (fieldErrors!=null && fieldErrors.size()>0); 

        // Check wether we have an error to render
        if (hasActionError==false && hasFieldErrors==false)
        {   // No Errors, nothing to render
            return;
        }
        
        // Render error list
        HtmlTagDictionary dic = HtmlTagDictionary.getInstance();
        HtmlTag list = w.startTag(dic.ErrorListTag());
        addStandardAttributes(list, dic.ErrorListClass());
        list.beginBody();
    
        // Are there field errors to render?
        if (hasFieldErrors)
        {   // Render all field errors
            Collection<ErrorInfo> errors = fieldErrors.values();
            String fieldErrClass = str(fieldErrorClass, dic.ErrorItemEntryClass());
            for (ErrorInfo e : errors)
            {
                String msg = provider.getLocalizedErrorMessage(e);
                renderError(w, fieldErrClass, msg);
            }
        }
        
        // Render last action error
        if (hasActionError)
        {   // Render action error
            String actionErrClass = str(actionErrorClass, dic.ErrorActionEntryClass());
            String msg = provider.getLocalizedErrorMessage(lastActionError);
            renderError(w, actionErrClass, msg);
        }

        // done
        list.endTag();
    }

    private void renderError(HtmlWriter w, String cssClassName, String msg)
    {
        HtmlTagDictionary dic = HtmlTagDictionary.getInstance();
        HtmlTag tag = w.startTag(dic.ErrorEntryTag());
        // Check whether additional wrapper is desired
        String wrapTag = dic.ErrorEntryWrapperTag();
        if (wrapTag!=null && wrapTag.length()>0)
        {   tag.beginBody();
            // Item wrapper tag
            HtmlTag wrap = w.startTag(wrapTag);
            wrap.addAttribute("class", cssClassName);
            wrap.endTag(msg);
        }
        else
        {   // No additional error wrapper tag
            tag.addAttribute("class", cssClassName);
            tag.beginBody(msg);
        }
        // end
        tag.endTag();
    }
    
    // ------- Property Accessors -------

    public void setActionErrorClass(String actionErrorClass)
    {
        this.actionErrorClass = actionErrorClass;
    }

    public void setFieldErrorClass(String fieldErrorClass)
    {
        this.fieldErrorClass = fieldErrorClass;
    }
    
}
