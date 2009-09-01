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

import java.io.Writer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.empire.commons.StringUtils;
import org.apache.empire.data.ColumnExpr;
import org.apache.struts2.components.Component;

import com.opensymphony.xwork2.util.ValueStack;


@SuppressWarnings("serial")
public class ParamTag extends EmpireValueTagSupport
{
    public static class ParamComponent extends Component
    {
        public String name;
        public String value;

        public ParamComponent(ValueStack stack)
        {
            super(stack);
        }
        @Override
        public boolean start(Writer writer)
        {   // no body support
            return false;
        }
        @Override
        public boolean end(Writer writer, String body)
        {
            try {
                Component component = findAncestor(Component.class);
                if (component!=null)
                {
                    component.addParameter(name, value);
                }
            } finally {
                popComponentStack();
            }
            return false;
        }
    }
    
    private Object name;
    
    /*
     * Clears all params since tag is reused
     */
    @Override
    protected void resetParams()
    {
        // Param Tag
        name = null;
        // Value
        super.resetParams();
    }
    
    @Override
    public Component getBean(ValueStack stack, HttpServletRequest req, HttpServletResponse res)
    {
        return new ParamComponent(stack);
    }

    @Override
    protected void populateParams()
    {        
        super.populateParams();
        
        ParamComponent comp = (ParamComponent)component;
        comp.name  = getTagName(name);
        comp.value = getStringValue();
    }
    
    public void setName(Object name)
    {
        this.name = name;
    }
    
    private String getTagName(Object name)
    {
        if (name instanceof ColumnExpr)
            return ((ColumnExpr)name).getName();
        if (name == null)
            setPropertyNameFromValue();
        // Call Base Class
        return super.getTagName(StringUtils.toString(name));
    }
    
}
