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
package org.apache.empire.struts2.jsp.controls;

import java.util.HashMap;

public final class InputControlManager
{
    static HashMap<String, InputControl> controlMap = null;
    
    static {
        
        controlMap = new HashMap<String, InputControl>();
        
        registerControl("text",     new TextInputControl());
        registerControl("select",   new SelectInputControl());
        registerControl("checkbox", new CheckboxInputControl());
        registerControl("phone",    new PhoneInputControl());
        registerControl("radio",    new RadioInputControl());
        registerControl("textarea", new TextAreaInputControl());
        registerControl("email",    new EMailInputControl());
        registerControl("hlink",    new HLinkInputControl());
        registerControl("password", new PasswordInputControl());
    }
    
    private InputControlManager()
    {
        // Default Constructor
    }
    
    public static void registerControl(String name, InputControl control)
    {
        controlMap.put(name, control);
    }
    
    public static InputControl getControl(String name)
    {
        return controlMap.get(name);
    }
}
