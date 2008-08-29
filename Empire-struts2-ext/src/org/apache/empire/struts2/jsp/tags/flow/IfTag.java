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
package org.apache.empire.struts2.jsp.tags.flow;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;

import org.apache.empire.struts2.jsp.tags.EmpireTagSupport;
import org.apache.struts2.components.Component;

import com.opensymphony.xwork2.util.ValueStack;


@SuppressWarnings("serial")
public class IfTag extends EmpireTagSupport
{
    public static final String IF_RESULT_ANSWER = "esteam.if.result";
    
    /*
    private static class IfComponent extends org.apache.struts2.components.If
    {
        public IfComponent(ValueStack stack)
        {
            super(stack);
        }
    }
    */

    public Object test;
    
    @Override
    protected void resetParams()
    {
        test=null;
        // reset base params
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
        // Evaluate Expression if not already a boolean
        boolean result = this.getBoolean(test, true);
        test = result;
        // Tabel cell tag
        if (result==false)
            return SKIP_BODY; 
        // Condition is true, i.e. inlcude body
        return EVAL_BODY_INCLUDE;
    }

    @Override
    public int doEndTag()
        throws JspException
    {   // Put with End-Tag to allow Else being outside the if-tag
        putPageAttribute(IF_RESULT_ANSWER, test);
        test=null;
        return EVAL_PAGE;
    }

    // ------- Property accessors -------
    
    public void setTest(Object test)
    {
        this.test = test;
    }
}
