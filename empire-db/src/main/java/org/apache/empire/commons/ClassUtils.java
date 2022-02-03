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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.commons.beanutils.ConstructorUtils;
import org.apache.empire.exceptions.EmpireException;
import org.apache.empire.exceptions.InternalException;
import org.apache.empire.exceptions.InvalidArgumentException;
import org.apache.empire.exceptions.NotSupportedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ClassUtils
{
    // Logger
    private static final Logger log = LoggerFactory.getLogger(ClassUtils.class);
    
    /*
     * ClassUtils contains static methods only
     */
    private ClassUtils()
    { 
        /* No instances */
    }

    /**
     * Used to test Serialization
     * @param objToSerialize
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T> T testSerialization(Class<T> clazz, T objToSerialize)
    {
        try
        {   ByteArrayOutputStream baos = new ByteArrayOutputStream();
            // Write the object
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(objToSerialize);
            // Read the object
            ObjectInputStream oin = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));
            Object unserializedObject = oin.readObject();
            // return result
            return (T)unserializedObject;
        }
        catch (IOException e)
        {
            log.error("Serialization failed: "+e.getMessage(), e);
            throw new InternalException(e);
        }
        catch (ClassNotFoundException e)
        {
            log.error("Class not Found: "+e.getMessage(), e);
            throw new InternalException(e);
        }
    }
    
    /**
     * Retrieve a field value using reflection
     * @param clazz the class from which to obtain the field
     * @param object the object instance from which to obtain the field
     * @param property the property to obtain 
     * @param includePrivateFields flag whether or not to include private fields
     * @return the property value
     */
    public static synchronized Object getFieldValue(Class<?> clazz, Object object, String property, boolean includePrivateFields)
    {
        // check arguments
        if (clazz==null || (object!=null && !clazz.isInstance(object)))
            throw new InvalidArgumentException("clazz", clazz);
        if (StringUtils.isEmpty(property))
            throw new InvalidArgumentException("property", property);
        // begin
        boolean accessible = true; 
        Field field = null;
        try
        { // find and invoke
            field = (includePrivateFields ? clazz.getDeclaredField(property) : clazz.getField(property));
            accessible = field.isAccessible();
            if (includePrivateFields && accessible==false)
                field.setAccessible(true);
            // invoke
            return field.get(object);
        }
        catch (NoSuchFieldException e)
        {   // No such Method
            if (includePrivateFields)
            {   // try superclass
                clazz = clazz.getSuperclass();
                if (clazz!=null && !clazz.equals(java.lang.Object.class))
                    return getFieldValue(clazz, object, property, true);
            }
            // not found
            return null;
        }
        catch (IllegalAccessException e)
        {   // Invalid Method definition   
            throw new NotSupportedException(object, property, e);
        }
        finally {
            // restore accessible
            if (field!=null && accessible==false)
                field.setAccessible(false);
        }
    }

    /**
     * Retrieve a field value using reflection
     * The field accessor must be public
     * @param object the object instance from which to obtain the field
     * @param property the property to obtain 
     * @return the property value
     */
    public static Object getFieldValue(Object object, String property)
    {
        if (object==null)
            throw new InvalidArgumentException("object", object);
        // begin
        return getFieldValue(object.getClass(), object, property, false);
    }

    /**
     * Retrieve a field value using reflection 
     * @param object the object instance from which to obtain the field
     * @param property the property to obatin 
     * @return the property value
     */
    public static Object getPrivateFieldValue(Object object, String property)
    {
        if (object==null)
            throw new InvalidArgumentException("object", object);
        // begin
        return getFieldValue(object.getClass(), object, property, true);
    }

    /**
     * Retrieve a field value using reflection 
     * @param clazz the class of the object
     * @param object the object or null if static fields are to be changed
     * @param property the field name
     * @param value the field value
     */
    public static synchronized void setPrivateFieldValue(Class<?> clazz, Object object, String property, Object value)
    {
        try
        {
            Field field = clazz.getDeclaredField(property);
            field.setAccessible(true);
            // Object val = field.get(object);
            field.set(object, value);
            field.setAccessible(false);
        }
        catch (Exception e)
        {   // Access Error
            log.error("Unable to modify private field '"+property+"* on class '"+clazz.getName()+"'", e);
            throw new InternalException(e);
        }
    }

    /**
     * copied from org.apache.commons.beanutils.ConstructorUtils since it's private there
     */
    public static Constructor<?> findMatchingAccessibleConstructor(Class<?> clazz, Class<?>[] parameterTypes)
    {
        // search through all constructors 
        int paramSize = (parameterTypes!=null ? parameterTypes.length : 0);
        Constructor<?>[] ctors = clazz.getConstructors();
        for (int i = 0, size = ctors.length; i < size; i++)
        {   // compare parameters
            Class<?>[] ctorParams = ctors[i].getParameterTypes();
            int ctorParamSize = ctorParams.length;
            if (ctorParamSize == paramSize)
            {   // Param Size matches
                boolean match = true;
                for (int n = 0; n < ctorParamSize; n++)
                {
                    if (!ObjectUtils.isAssignmentCompatible(ctorParams[n], parameterTypes[n]))
                    {
                        match = false;
                        break;
                    }
                }
                if (match) {
                    // get accessible version of method
                    Constructor<?> ctor = ConstructorUtils.getAccessibleConstructor(ctors[i]);
                    if (ctor != null) {
                        try {
                            ctor.setAccessible(true);
                        } catch (SecurityException se) { /* ignore */ }
                        return ctor;
                    }
                }
            }
        }
        return null;
    }
    
    /**
     * Invoke a simple method (without parameters) on an object using reflection
     * @param clazz the class from which to obtain the field
     * @param object the object instance on which to invoke the method
     * @param methodName the name of the method to invoke 
     * @param includePrivateMethods flag whether or not to include private methods
     * @return the return value of the method
     */
    public static synchronized Object invokeSimpleMethod(Class<?> clazz, Object object, String methodName, boolean includePrivateMethods)
    {
        // check arguments
        if (object==null)
            throw new InvalidArgumentException("object", object);
        if (clazz==null || !clazz.isInstance(object))
            throw new InvalidArgumentException("clazz", clazz);
        if (StringUtils.isEmpty(methodName))
            throw new InvalidArgumentException("methodName", methodName);
        // begin
        boolean accessible = true; 
        Method method = null;
        try
        { // find and invoke
            method = (includePrivateMethods ? clazz.getDeclaredMethod(methodName) : clazz.getMethod(methodName));
            accessible = method.isAccessible();
            if (includePrivateMethods && accessible==false)
                method.setAccessible(true);
            // invoke
            return method.invoke(object);
        }
        catch (NoSuchMethodException e)
        {   // No such Method
            if (includePrivateMethods)
            {   // try superclass
                clazz = clazz.getSuperclass();
                if (clazz!=null && !clazz.equals(java.lang.Object.class))
                    return invokeSimpleMethod(clazz, object, methodName, true);
            }
            // not found
            return null;
        }
        catch (SecurityException e)
        {   // Invalid Method definition   
            throw new NotSupportedException(object, methodName, e);
        }
        catch (IllegalAccessException e)
        {   // Invalid Method definition   
            throw new NotSupportedException(object, methodName, e);
        }
        catch (IllegalArgumentException e)
        {   // Invalid Method definition   
            throw new NotSupportedException(object, methodName, e);
        }
        catch (InvocationTargetException e)
        {   // Error inside Method
            Throwable cause = e.getCause();
            if (cause instanceof EmpireException)
                throw (EmpireException)cause;
            // wrap    
            throw new InternalException(cause);
        }
        finally {
            // restore accessible
            if (method!=null && accessible==false)
                method.setAccessible(false);
        }
    }

    /**
     * Invoke a simple method (without parameters) on an object using reflection
     * @param object the object instance on which to invoke the method
     * @param methodName the name of the method to invoke 
     * @return the return value of the method
     */
    public static Object invokeSimpleMethod(Object object, String methodName)
    {
        if (object==null)
            throw new InvalidArgumentException("object", object);
        // begin
        return invokeSimpleMethod(object.getClass(), object, methodName, false);
    }

    /**
     * Invoke a simple method (without parameters) on an object using reflection
     * @param object the object instance on which to invoke the method
     * @param methodName the name of the method to invoke 
     * @return the return value of the method
     */
    public static Object invokeSimplePrivateMethod(Object object, String methodName)
    {
        if (object==null)
            throw new InvalidArgumentException("object", object);
        // begin
        return invokeSimpleMethod(object.getClass(), object, methodName, true);
    }

}
