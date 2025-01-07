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

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.apache.empire.commons.StringUtils;
import org.apache.empire.exceptions.InternalException;
import org.apache.empire.exceptions.InvalidArgumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class InputControlManager
{
    private static final Logger log = LoggerFactory.getLogger(InputControlManager.class);

    private static Class<? extends javax.faces.component.html.HtmlOutputLabel> labelComponentClass = javax.faces.component.html.HtmlOutputLabel.class;
    
    private static boolean showLabelRequiredMark = false;

    public static Class<? extends javax.faces.component.html.HtmlOutputLabel> getLabelComponentClass()
    {
        return labelComponentClass;
    }

    public static void setLabelComponentClass(Class<? extends javax.faces.component.html.HtmlOutputLabel> labelComponentClass)
    {
        InputControlManager.labelComponentClass = labelComponentClass;
    }
    
    public static boolean isShowLabelRequiredMark()
    {
        return showLabelRequiredMark;
    }
    
    public static void setShowLabelRequiredMark(boolean showLabelRequiredMark)
    {
        InputControlManager.showLabelRequiredMark = showLabelRequiredMark;
    }

    static HashMap<String, InputControl> controlMap = null;

    static
    {

        controlMap = new HashMap<String, InputControl>();

        registerControl(new TextInputControl());
        registerControl(new SelectInputControl());
        registerControl(new TextAreaInputControl());
        registerControl(new CheckboxInputControl());
        registerControl(new RadioInputControl());
        /*
        registerControl(new PhoneInputControl());
        registerControl(new EMailInputControl());
        registerControl(new HLinkInputControl());
        registerControl(new PasswordInputControl());
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

    static InputAttachedObjectsHandler attachedObjectsHandler = new InputAttachedObjectsHandler();
    
    public static InputAttachedObjectsHandler getAttachedObjectsHandler()
    {
        return attachedObjectsHandler;
    }

    public static void setAttachedObjectsHandler(InputAttachedObjectsHandler attachedObjectsHandler)
    {
        InputControlManager.attachedObjectsHandler = attachedObjectsHandler;
    }

    static boolean inputValueExpressionEnabled = true;
    
    public static boolean isInputValueExpressionEnabled()
    {
        return inputValueExpressionEnabled;
    }

    public static void setInputValueExpressionEnabled(boolean valueExpressionEnabled)
    {
        inputValueExpressionEnabled = valueExpressionEnabled;
    }

    private static Map<Class<? extends UIComponent>, String> componentTypeMap = new HashMap<Class<? extends UIComponent>, String>();

    @SuppressWarnings("unchecked")
    public static <T extends UIComponent> T createComponent(FacesContext context, Class<T> clazz)
    {
        // Get component type from class
        String type = componentTypeMap.get(clazz);
        if (type == null)
        { // Detect type
            try
            { // Detect component type
                Field field = clazz.getDeclaredField("COMPONENT_TYPE");
                if (field != null)
                    type = StringUtils.toString(field.get(null), ""); // Empty string is default
                else
                    type = ""; // Empty string is default
                // show
                log.debug("Component-Type for class {} is {}", clazz.getName(), type);
            }
            catch (SecurityException e)
            {
                throw new InternalException(e);
            }
            catch (NoSuchFieldException e)
            {   // No COMPONENT_TYPE field
                log.debug("No Component-Type available for class {}!", clazz.getName());
                type = ""; // Empty string is default
            }
            catch (IllegalArgumentException e)
            {
                throw new InternalException(e);
            }
            catch (IllegalAccessException e)
            {
                throw new InternalException(e);
            }
            // put in map
            componentTypeMap.put(clazz, type);
        }
        // Now, create the instance
        if (StringUtils.isEmpty(type))
        {
            try
            { // create instance directly
                return clazz.newInstance();
            }
            catch (InstantiationException e)
            {
                throw new InternalException(e);
            }
            catch (IllegalAccessException e)
            {
                throw new InternalException(e);
            }
        }
        // otherwise ask the application
        return (T) context.getApplication().createComponent(type);
    }
    
    public static void registerComponentType(Class<? extends UIComponent> componentClass, String componentType)
    {
        if (componentClass==null)
            throw new InvalidArgumentException("componentClass", componentClass);
        if (StringUtils.isNotEmpty(componentType))
        {   // add
            log.debug("registering Component type {} for {}", componentType, componentClass.getName());
            componentTypeMap.put(componentClass, componentType);
        }
        else
        {   // remove
            log.debug("unregisterin Component type {} for {}", componentType, componentClass.getName());
            componentTypeMap.remove(componentClass);
        }
    }

}
