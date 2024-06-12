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
package org.apache.empire.jakarta.utils;

import java.util.Map;

import jakarta.faces.context.FacesContext;

import org.apache.empire.jakarta.pages.Page;

public class SessionMap
{
    @SuppressWarnings("unchecked")
    public static <T> T get(String objectName, Class<T> type)
    {
        Map<String, Object> map = FacesContext.getCurrentInstance().getExternalContext().getSessionMap();
        return (T) map.get(objectName);
    }

    public static <T> T get(Page page, String propertyName, Class<T> type)
    {
        String objectName = page.getPageName() + "." + propertyName + "." + type.getSimpleName();
        return get(objectName, type);
    }
    
    public static <T> void put(String objectName, Class<T> type, T object)
    {
        Map<String, Object> map = FacesContext.getCurrentInstance().getExternalContext().getSessionMap();
        if (object != null)
            map.put(objectName, object);
        else
            map.remove(objectName);
    }
    
    public static <T> void remove(Page page, String propertyName, Class<T> type, T object)
    {
        String objectName = page.getPageName() + "." + propertyName + "." + type.getSimpleName();
        put(objectName, type, object); 
    }
    
    public static <T> void remove(String objectName, Class<T> type)
    {
        put(objectName, type, null); 
    }
    
    public static <T> void remove(Page page, String propertyName, Class<T> type)
    {
        remove(page, propertyName, type, null); 
    }
}
