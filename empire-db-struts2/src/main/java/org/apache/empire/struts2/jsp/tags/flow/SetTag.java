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

import org.apache.empire.struts2.jsp.tags.EmpireTagSupport;
import org.apache.struts2.components.Component;

import com.opensymphony.xwork2.util.ValueStack;


@SuppressWarnings("serial")
public class SetTag extends EmpireTagSupport  // org.apache.struts2.views.jsp.SetTag
{   
    private static class SetComponent extends org.apache.struts2.components.Set
    {
        public Object object;
        public SetComponent(ValueStack stack)
        {
            super(stack);
        }
        @Override
        protected Object findValue(String expr)
        {
            return object;
        }
        public void setValue(Object object)
        {
            this.object = object;
        }
    }

    // Properties
    protected String name;
    protected String scope;
    protected Object value;

    @Override
    protected void resetParams()
    {
        name = null;
        scope = null;
        value = null;
        // reset base params
        super.resetParams();
    }
    
    @Override
    protected void populateParams()
    {
        Object object = getObject(value, null, Object.class);
        // Use Struts Component
        SetComponent c = (SetComponent)component;
        c.setName(name);
        c.setScope(scope);
        c.setValue(object);
    }

    @Override
    public Component getBean(ValueStack stack, HttpServletRequest req, HttpServletResponse res)
    {
        return new SetComponent(stack);
    }

    public void setName(String name)
    {
        this.name = name;
    }
    public void setScope(String scope)
    {
        this.scope = scope;
    }
    public void setValue(Object value)
    {
        this.value = value;
    }
}
