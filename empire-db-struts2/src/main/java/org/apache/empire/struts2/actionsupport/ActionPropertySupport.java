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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.empire.commons.StringUtils;


public class ActionPropertySupport
{
    // Logger
    protected static Logger log = LoggerFactory.getLogger(ActionPropertySupport.class);

    private ActionBase action;
    private String propertyName;
    private boolean persist;
    
    public ActionPropertySupport(ActionBase action, String propertyName, boolean persist)
    {
        this.action = action;
        this.propertyName = propertyName;
        this.persist = persist;
    }
    
    // ------- ActionItem Property -------
    
    private String value;
    
    public String getName()
    {
        return propertyName;
    }

    public String getValue()
    {   // Get Item from request?
        if (value== null) // && (this instanceof NoParameters))
        {   value = action.getRequestParam(propertyName);
            if (value!=null)
                setValue(value);
        }
        // Check if Item is on Session
        if (value==null && persist)
            return StringUtils.toString(action.getActionObject(propertyName));
        // return Item
        return value;
    }

    public void setValue(String value)
    {   // Set Session Item
        if (value==null)
        {   log.error("Cannot set action item to null. Use clearitem() to remove the item from the session");
            return;
        }
        // Persist Item on session
        if (persist)
            action.putActionObject(propertyName, value);
        // Set item now
        this.value = value;
    }
    
    /**
     * Sets the property value from a request param.
     * The request must contain a property of that name. 
     * @return true if the param was supplied with the request or false otherwise
     */
    public boolean setFromRequest()
    {
        String param = action.getRequestParam(propertyName);
        if (param==null)
        {   // Param has not been supplied
            return false;
        }
        setValue(param);
        return true;
    }
    
    public void clear()
    {
        // Remove from session
        if (persist)
            action.removeActionObject(propertyName);
        // Clear value
        value=null;
    }
    
    public boolean hasValue(boolean lookOnSession)
    {
        if (lookOnSession==false)
            return (action.getRequestParam(propertyName)!=null);
        // Also look on Session
        return (getValue()!=null);
    }
}
