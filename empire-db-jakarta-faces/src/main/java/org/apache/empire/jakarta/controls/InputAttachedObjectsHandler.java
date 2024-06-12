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
package org.apache.empire.jakarta.controls;

import java.util.List;

import jakarta.el.ValueExpression;
import jakarta.faces.component.UIComponent;
import jakarta.faces.component.UIComponentBase;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.AttachedObjectHandler;

import org.apache.empire.commons.StringUtils;
import org.apache.empire.data.Column;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InputAttachedObjectsHandler
{
    private static final Logger log = LoggerFactory.getLogger(InputAttachedObjectsHandler.class);
    
    /**
     * Allows to add objects such as events, validators, etc to the dynamically created input components 
     * @param parent the CompositeComponent parent
     * @param context the faces context
     * @param column the column for which to attach the objects (optional, i.e. may be null) 
     * @param inputComponent the input component created by the InputControl implementation
     */
    public void addAttachedObjects(UIComponent parent, FacesContext context, Column column, UIComponentBase inputComponent)
    {
        // Move RetargetableHandlers 
        @SuppressWarnings("unchecked")
        List<AttachedObjectHandler> result = (List<AttachedObjectHandler>) parent.getAttributes().get("jakarta.faces.RetargetableHandlers");
        if (result == null)
        {
            return;
        }
        // Attach Events
        for (AttachedObjectHandler aoh : result)
        {
            log.info("applying RetargetableHandlers to component {}", inputComponent.getId());
            aoh.applyAttachedObject(context, inputComponent);
        }
        // remove
        result.clear();
        parent.getAttributes().remove("jakarta.faces.RetargetableHandlers");
    }
    
    /**
     * updates objects such as events, validators, etc on dynamically created input components 
     * @param parent the CompositeComponent parent
     * @param context the faces context
     * @param column the column for which to attach the objects (optional, i.e. may be null) 
     * @param inputComponent the input component created by the InputControl implementation
     */
    public void updateAttachedObjects(UIComponent parent, FacesContext context, Column column, UIComponentBase inputComponent)
    {
        // Normally nothing to do
    }

    /**
     * helper to get a tag attribute value
     * @param component the CompositeComponent parent 
     * @param name the name of the attribute
     * @return the value of the attribute
     */
    protected Object getTagAttributeValue(UIComponent component, String name)
    {
        Object value = component.getAttributes().get(name);
        if (value==null)
        {   // try value expression
            ValueExpression ve = component.getValueExpression(name);
            if (ve!=null)
            {   // It's a value expression
                FacesContext ctx = FacesContext.getCurrentInstance();
                value = ve.getValue(ctx.getELContext());
            }
        }
        return value;
    }
    
    protected final String getTagAttributeValueString(UIComponent component, String name)
    {
        return StringUtils.toString(getTagAttributeValue(component, name));
    }
    
}
