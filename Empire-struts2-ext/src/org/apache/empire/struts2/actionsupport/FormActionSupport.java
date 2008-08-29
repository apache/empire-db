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
package org.apache.empire.struts2.actionsupport;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.empire.commons.ErrorInfo;
import org.apache.empire.commons.ErrorObject;
import org.apache.empire.commons.StringUtils;
import org.apache.empire.data.Column;


public abstract class FormActionSupport extends ErrorObject
{
    protected static Log log = LogFactory.getLog(FormActionSupport.class);

    protected ActionBase action;
    
    protected String propertyName;
    
    private boolean enableFieldErrors = true;
    
    /**
     * Creates a new FormActionSupport object.
     * @param action the action this object belongs to
     * @param propertyName the name of the properties
     */
    protected FormActionSupport(ActionBase action, String propertyName)
    {
        this.action = action;
        this.propertyName = propertyName;
        // Check Param
        if (propertyName==null || propertyName.length()==0)
        {   // Must use key persistence
            log.warn("No property name has been specified for FormActionSupport! Using default 'item'.");
            propertyName="item";
        }
    }

    /**
     * Returns true if field error handling is enabled or false otherwise.
     * @return true if field error handling is enabled or false otherwise.
     */
    public final boolean isEnableFieldErrors()
    {
        return enableFieldErrors;
    }

    /**
     * Enables or disables field error messages. 
     * @param enableFieldErrors true to enable field errors or false otherwise
     */
    public final void setEnableFieldErrors(boolean enableFieldErrors)
    {
        this.enableFieldErrors = enableFieldErrors;
    }
    
    /**
     * Returns the property name of this form support object
     * @return the property name
     */
    public String getRecordPropertyName()
    {
        if (propertyName!=null)
            return propertyName;
        // Get Default Name from Action
        return action.getItemPropertyName();
    }
    
    /**
     * Returns the value of a paramether supplied with the request or the session 
     * @param name the name of the parameter
     * @param persist true if the request value should be persisted on the session or false otherwise.
     * @return the value of the parameter or null if not supplied.
     */
    protected String getActionParam(String name, boolean persist)
    {
        // Find Item on Request
        String item = action.getRequestParam(name);
        if (persist)
        {   // Check if item is supplied
            if (item==null)
                return StringUtils.toString(action.getActionObject(name));
            // Set Session Item
            action.putActionObject(name, item);
        }
        return item;
    }
    
    /**
     * load the form data into an object.
     * @return true if loading the form data was successful or false otherwise.
     */
    public abstract boolean loadFormData();
    
    // --------------------------- protected --------------------------------
    
    /**
     * overridable: sets a field error message on the action
     */
    protected void addFieldError(String name, Column column, ErrorInfo error, Object value)
    {
        if (enableFieldErrors)
            action.addFieldError(name, column, error);
    }

}
