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

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.beanutils.ConvertUtilsBean;
import org.apache.commons.beanutils.PropertyUtilsBean;
import org.apache.empire.exceptions.BeanPropertyGetException;
import org.apache.empire.exceptions.BeanPropertySetException;
import org.apache.empire.exceptions.InvalidArgumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * BeanPropertyUtils provides methods for getting and setting bean property values 
 * It is a replacement for org.apache.commons.beanutils.BeanUtils
 * @author rainer
 */
public final class BeanPropertyUtils
{
    // Logger
    private static final Logger log = LoggerFactory.getLogger(ClassUtils.class);
    
    static private BeanPropertyUtilsImpl instance = new BeanPropertyUtilsImpl();

    /**
     * Returns the BeanUtils implementation
     * @return the new BeanUtils implementation
     */
    public static BeanPropertyUtilsImpl getImplementation()
    {
        return instance;
    }

    /**
     * Allows to override the implementation
     * @param utils the new BeanUtils implementation
     */
    public static void setImplementation(BeanPropertyUtilsImpl utils)
    {
        instance = utils;
    }

    /**
     * Checks if a bean has a particular property and a corresponding getter/setter method exists
     * @param bean the bean to check
     * @param property the property to check
     * @param writeAccess flag whether to check for the getter method (false) or the setter method (true)
     * @return 1 if the getter/setter exists, 0 if the getter/setter does not exist, and -1 if the property does not exist at all
     */
    public static int hasProperty(Object bean, String property, boolean writeAccess)
    {
        return instance.hasProperty(bean, property, writeAccess);
    }
    
    /**
     * Returns the property of a bean by calling the correponding getter function
     * @param bean the bean
     * @param property the property name
     * @return the property value
     */
    public static Object getProperty(Object bean, String property)
    {
        return instance.getProperty(bean, property);
    }

    /**
     * Set a single property value of a java bean object used by readProperties.
     * Return false if the property does not exist or if no setter method exists (read only) 
     * throws BeanPropertySetException if an error occurs in the setter method
     *
     * @param column the column expression
     * @param bean the bean
     * @param property the property
     * @param value the value
     * @return true if the property has been set or false if the property does not exist or if no setter method exists
     */
    public static boolean setProperty(Object bean, String property, Object value)
    {
        return instance.setProperty(bean, property, value);
    }

    /** 
     * Converts a bean property value to a specific Java type
     * @param value the value to convert
     * @param type the type 
     * @return the converted value
     */
    public static Object convertPropertyValue(Object value, Class<?> type)
    {
        return instance.convertPropertyValue(value, type);
    }
    
    /**
     * Provided the implementation for the BeanUtils methods
     * @author rainer
     */
    public static class BeanPropertyUtilsImpl
    {
        /**
         * @see org.apache.empire.commons.BeanPropertyUtils#hasProperty(Object bean, String property, boolean writeAccess)
         */
        public int hasProperty(Object bean, String property, boolean writeAccess)
        {
            // Check Params
            if (bean==null)
                throw new InvalidArgumentException("bean", bean);
            if (property==null)
                throw new InvalidArgumentException("property", property);
            try
            {   // Get Property Value
                PropertyUtilsBean propUtils = BeanUtilsBean.getInstance().getPropertyUtils();
                PropertyDescriptor pd;
                pd = propUtils.getPropertyDescriptor(bean, property);
                if (pd==null) {
                    return -1; // No such property
                }
                // check methods
                final Method method;
                if (writeAccess)
                {   // find setter
                    method = propUtils.getWriteMethod(pd);
                }
                else
                {   // find getter
                    method = propUtils.getReadMethod(pd);
                }
                return (method!=null ? 1 : 0);
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e)
            {   log.warn("Property access not available for {} on {}", property, bean.getClass().getName());
                return 0;
            }
        }
        
        /**
         * @see org.apache.empire.commons.BeanPropertyUtils#getProperty(Object bean, String property)
         */
        public Object getProperty(Object bean, String property)
        {
            // Check Params
            if (bean==null)
                throw new InvalidArgumentException("bean", bean);
            if (property==null)
                throw new InvalidArgumentException("property", property);
            try
            {   // Get Property Value
                PropertyUtilsBean pub = BeanUtilsBean.getInstance().getPropertyUtils();
                return pub.getSimpleProperty(bean, property);
    
            } catch (IllegalAccessException e)
            {   log.error(bean.getClass().getName() + ": unable to get property '" + property + "'");
                throw new BeanPropertyGetException(bean, property, e);
            } catch (InvocationTargetException e)
            {   log.error(bean.getClass().getName() + ": unable to get property '" + property + "'");
                throw new BeanPropertyGetException(bean, property, e);
            } catch (NoSuchMethodException e)
            {   log.warn(bean.getClass().getName() + ": no getter available for property '" + property + "'");
                throw new BeanPropertyGetException(bean, property, e);
            }
        }

        /**
         * @see org.apache.empire.commons.BeanPropertyUtils#setProperty(Object bean, String property, Object value)
         */
        public boolean setProperty(Object bean, String property, Object value)
        {
            try
            {
                if (bean==null)
                    throw new InvalidArgumentException("bean", bean);
                if (StringUtils.isEmpty(property))
                    throw new InvalidArgumentException("property", property);
                if (log.isTraceEnabled())
                    log.trace("{}: setting property '{}' to {}", bean.getClass().getName(), property, value);
                // Set Property Value
                /**
                 *  replacement for BeanUtils.setProperty(bean, property, value);
                 */
                PropertyUtilsBean propUtils = BeanUtilsBean.getInstance().getPropertyUtils();
                PropertyDescriptor pd = propUtils.getPropertyDescriptor(bean, property);
                if (pd==null) {
                    if (log.isDebugEnabled())
                        log.debug("{}:  property '{}' does not exist!", bean.getClass().getName(), property);
                    return false; // No such property
                }
                // get the write method
                // final Method method = propUtils.getWriteMethod(pd);
                final Method method = propUtils.getWriteMethod(bean.getClass(), pd);
                if (method == null) {
                    if (log.isDebugEnabled())
                        log.debug("{}:  property '{}' has no write method!", bean.getClass().getName(), property);
                    return false; // Read-only
                }
                // convert type
                Class<?> propType = pd.getPropertyType();
                value = convertPropertyValue(value, propType);
                // invoke setter
                // propUtils.setSimpleProperty(bean, property, convertedValue);
                if (propType.isArray())
                    method.invoke(bean, new Object[] { value });
                else
                    method.invoke(bean, value);
                return true;
              // IllegalAccessException
            } catch (IllegalAccessException e)
            {   log.error(bean.getClass().getName() + ": unable to set property '" + property + "'");
                throw new BeanPropertySetException(bean, property, e);
              // InvocationTargetException  
            } catch (InvocationTargetException e)
            {   log.error(bean.getClass().getName() + ": unable to set property '" + property + "'");
                throw new BeanPropertySetException(bean, property, e);
              // NoSuchMethodException   
            } catch (NoSuchMethodException e)
            {   log.error(bean.getClass().getName() + ": unable to set property '" + property + "'");
                throw new BeanPropertySetException(bean, property, e);
            } catch (NullPointerException e)
            {   log.error(bean.getClass().getName() + ": unable to set property '" + property + "'");
                throw new BeanPropertySetException(bean, property, e);
            }
        }
        
        /**
         * @see org.apache.empire.commons.BeanPropertyUtils#convertPropertyValue(Object value, Class<?> type)
         */
        public Object convertPropertyValue(Object value, Class<?> type)
        {        
            ConvertUtilsBean convertUtils = BeanUtilsBean.getInstance().getConvertUtils();
            // Convert the specified value to the required type
            if (type.isArray()) { 
                // Scalar value into array
                if (value == null) {
                    final String[] values = new String[0];
                    return convertUtils.convert(values, type);
                } else if (value instanceof String) {
                    return convertUtils.convert(value, type);
                } else if (value instanceof String[]) {
                    return convertUtils.convert((String[]) value, type);
                } else {
                    /* old implementation from BeanUtilsBean
                    final Converter converter = convertUtils.lookup(type);
                    return (converter != null) ? converter.convert(type, value) : value; 
                    */
                    // see org.apache.commons.beanutils.BeanUtilsBean2 
                    return convertUtils.convert(value, type);
                }
            } else {
                // Value into scalar
                if (value==null)
                    return null;
                else if (value instanceof String) {
                    return convertUtils.convert((String) value, type);
                } else if (value instanceof String[]) {
                    return convertUtils.convert(((String[]) value)[0], type);
                } else {
                    /* old implementation from BeanUtilsBean
                    final Converter converter = convertUtils.lookup(type);
                    return (converter != null) ? converter.convert(type, value) : value;
                    */
                    // see org.apache.commons.beanutils.BeanUtilsBean2 
                    return convertUtils.convert(value, type);
                }
            }
        }
    }
    
}
