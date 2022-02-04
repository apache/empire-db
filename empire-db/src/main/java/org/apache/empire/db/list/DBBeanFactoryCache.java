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
package org.apache.empire.db.list;

import java.util.HashMap;
import java.util.Map;

import org.apache.empire.db.exceptions.UnknownBeanTypeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DBBeanFactoryCache
 * @author rainer
 */
public final class DBBeanFactoryCache
{
    protected static final Logger log = LoggerFactory.getLogger(DBBeanFactoryCache.class);
    
    private static Map<Class<?>, DBBeanListFactory<?>> beanFactoryMap; 
    
    static {
        // set default
        beanFactoryMap = new HashMap<Class<?>, DBBeanListFactory<?>>(); /* Concurrent ? */
    }

    /**
     * Allows to provide a different implementation of the bean factory map 
     * @param map the map to use
     */
    public static void setMapImplementation(Map<Class<?>, DBBeanListFactory<?>> map)
    {
        beanFactoryMap = map;
    }
   
    /**
     * Returns the DBRowSet instance assigned to a particular Java bean type
     * @param beanType the Java bean type
     * @return return the DBRowSet assigned to this type 
     */
    public static synchronized <T> DBBeanListFactory<T> getFactoryForType(Class<T> beanType)
    {
        @SuppressWarnings("unchecked")
        DBBeanListFactory<T> factory = (DBBeanListFactory<T>)beanFactoryMap.get(beanType); 
        return factory;
    }

    /**
     * sets the DBRowSet instance assigned to a particular Java bean type
     * @param beanType the Java bean type
     */
    public static synchronized <T> void setFactoryForType(Class<?> beanType, DBBeanListFactory<T> factory)
    {
        if (factory!=null)
        {   // Check previous
            DBBeanListFactory<?> prev = beanFactoryMap.get(beanType);
            if (prev!=null && prev!=factory)
                log.warn("The Java bean type '{}' has already been assigned to a different BeanListFactory!", beanType.getName());
            // Assign now
            beanFactoryMap.put(beanType, factory);
        }
        else
            beanFactoryMap.remove(beanType);
    }
}
