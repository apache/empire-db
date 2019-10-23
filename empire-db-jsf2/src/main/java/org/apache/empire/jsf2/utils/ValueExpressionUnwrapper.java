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
package org.apache.empire.jsf2.utils;

import javax.el.ValueExpression;
import javax.el.VariableMapper;

import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.commons.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ValueExpressionUnwrapper
 * @author doebele
 * 
 * This class is used to unwrap nested ValueExpressions
 * This is useful for Faclet-Taglib-Tags, that forward attributes which may or may not be provided for the tag.
 * 
 * IMPORTANT: The expression in the Facelet-Tag must use a '$' character instead of a '#'
 *
 * Here is an example for such a tag
    <ui:composition
        xmlns="http://www.w3.org/1999/xhtml"
        xmlns:e="http://java.sun.com/jsf/composite/empire">
        <tr>
            <e:control
                column="#{column}" 
                record="#{record}"
                value="${value}"
            </e:control>         
        </tr> 
    </ui:composition>
 *   
 * In this example the "${value}" will be unwrapped from the TagEncodingHelper in order to dectect
 * whether or not an expression has been provided. 
 */
public class ValueExpressionUnwrapper
{
    // Logger
    private static final Logger log = LoggerFactory.getLogger(ValueExpressionUnwrapper.class);
    
    private static ValueExpressionUnwrapper instance = null;
    
    public static synchronized ValueExpressionUnwrapper getInstance()
    {
        if (instance==null)
            instance = new ValueExpressionUnwrapper(); 
        return instance;
    }

    public static synchronized void setInstance(ValueExpressionUnwrapper instance)
    {
        ValueExpressionUnwrapper.instance = instance;
    }
    
    protected ValueExpressionUnwrapper()
    {
        log.debug("Instance of {} created", this.getClass().getName());
    }
    
    public ValueExpression unwrap(ValueExpression ve)
    {   // now unwrap ValueExpressionImpl
        if (ve!=null && !ve.isLiteralText())
        {   // immediate evaluation?
            String expression = ve.getExpressionString();
            if (expression.startsWith("${"))
            {   // expected: ve = org.apache.el.ValueExpressionImpl
                if (ve.getClass().getName().equals("org.apache.el.ValueExpressionImpl"))
                {   // get the Node
                    Object node = ObjectUtils.invokeSimplePrivateMethod(ve, "getNode");
                    if (node!=null)
                    {   // We have a Node
                        // Now get the Image
                        String image = StringUtils.toString(ObjectUtils.invokeSimpleMethod(node, "getImage"));
                        if (StringUtils.isNotEmpty(image)) 
                        {   // We have an image
                            // Now find the varMapper
                            VariableMapper varMapper = (VariableMapper)ObjectUtils.getPrivateFieldValue(ve, "varMapper");
                            if (varMapper!=null)
                            {   // Resolve variable using mapper
                                ve = varMapper.resolveVariable(image);
                                log.debug("EL-Expression \"{}\" has been resolved to variable \"{}\"", expression, (ve!=null ? ve.getExpressionString() : null));
                            } else {
                                // Variable not provided!
                                log.debug("EL-Expression \"{}\" has been resolved to NULL.", expression);
                                ve = null;
                            }
                        } else {
                            // No image: complex expression of unsupported type
                            log.info("EL-Expression \"{}\" has unsupported Node type {}", expression, node.getClass().getName());
                        }
                    } else {
                        // Unexpected: No node available for ValueExpression 
                        log.warn("Unexpected: ValueExpressionImpl has no Node. Expression \"{}\" remains unchanged.");
                    }
                } else {
                    // Unexpected EL implementation: 
                    // Only "org.apache.el.ValueExpressionImpl" is supported! 
                    log.warn("Unexpected ValueExpression-Implementation: {}", ve.getClass().getName());
                    log.warn("ValueExpression unwrapping does not work!");
                }
            }
        }
        // done 
        return ve;
    }
}

