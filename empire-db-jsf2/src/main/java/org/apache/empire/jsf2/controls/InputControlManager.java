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
package org.apache.empire.jsf2.controls;

import java.util.HashMap;

public final class InputControlManager
{
    private static Class<? extends javax.faces.component.html.HtmlOutputLabel> labelComponentClass = javax.faces.component.html.HtmlOutputLabel.class;
    
    public static Class<? extends javax.faces.component.html.HtmlOutputLabel> getLabelComponentClass()
    {
        return labelComponentClass;
    }

    public static void setLabelComponentClass(Class<? extends javax.faces.component.html.HtmlOutputLabel> labelComponentClass)
    {
        InputControlManager.labelComponentClass = labelComponentClass;
    }

    static HashMap<String, InputControl> controlMap = null;
    
    static {
        
        controlMap = new HashMap<String, InputControl>();
        
        registerControl(new TextInputControl());
        registerControl(new SelectInputControl());
        registerControl(new TextAreaInputControl());
        registerControl(new CheckboxInputControl());
        /*
        registerControl("phone",    new PhoneInputControl());
        registerControl("radio",    new RadioInputControl());
        registerControl("email",    new EMailInputControl());
        registerControl("hlink",    new HLinkInputControl());
        registerControl("password", new PasswordInputControl());
        */
    }
    
    private InputControlManager()
    {
        // Default Constructor
    }
    
    public static void registerControl(InputControl control)
    {
        controlMap.put(control.getName(), control);
    }
    
    public static InputControl getControl(String name)
    {
        return controlMap.get(name);
    }
    
}
