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
package org.apache.empire.commons;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.w3c.dom.Element;

/**
 * This class holds a map of objects which are identified by a case insensitive key string.
 * 
 */
@SuppressWarnings("serial")
public class Attributes extends LinkedHashMap<String, Object>
{
    /**
     * @param key the attribute
     * @return the attribute value
     */
    public Object get(String key)
    {   // Check Key
        if (key==null || key.length()==0)
            return null;
        // Get
        return super.get(key.toLowerCase());
    }

    @Override
    public Object get(Object key)
    {   // Check Key
        return get((key!=null ? key.toString() : null));
    }

    @Override
    public Object put(String key, Object v)
    {
        if (key==null || key.length()==0)
            return null;
        return super.put(key.toLowerCase(), v);
    }

    /**
     * @param name the attribute
     * @param object the attribute value
     */
    public void set(String name, Object object)
    {
        this.put(name, object);
    }

    /**
     * @param element the XMLElement to which to append the options
     * @param flags options (currently unused)
     */
    public void addXml(Element element, long flags)
    {
        // add All Options
    	for(Map.Entry<String,Object> entry:entrySet())
    	{
    		element.setAttribute(entry.getKey(), String.valueOf(entry.getValue()));
    	}
    }
}
