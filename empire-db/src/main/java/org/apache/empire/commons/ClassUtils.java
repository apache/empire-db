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
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;

import org.apache.commons.beanutils.ConstructorUtils;
import org.apache.empire.exceptions.EmpireException;
import org.apache.empire.exceptions.InternalException;
import org.apache.empire.exceptions.InvalidArgumentException;
import org.apache.empire.exceptions.InvalidOperationException;
import org.apache.empire.exceptions.NotImplementedException;
import org.apache.empire.exceptions.NotSupportedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ClassUtils
{
    // Logger
    private static final Logger log = LoggerFactory.getLogger(ClassUtils.class);

    public static final Object[] EMPTY_ARGS = new Object[0];
    
    /*
     * ClassUtils contains static methods only
     */
    private ClassUtils()
    { 
        /* No instances */
    }

    /**
     * Used to test Serialization
     * @param <T> the class type
     * @param clazz class to serialize
     * @param objToSerialize objedt to serialize
     * @return the unserializedObject
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
     * Checks if a class is an immutable class such as a wrapper class of a primitive type
     * String and Class are also considered to be a immutable classes
     * @param clazz the class to check
     * @return true if the class is immutable
     */
    public static boolean isImmutableClass(Class<?> clazz) 
    {
        if (clazz.isPrimitive() || clazz.isEnum())
            return true;
        // Check Standard types
        return (clazz == String.class  || clazz == Character.class || clazz == Byte.class 
             || clazz == Integer.class || clazz == Long.class  || clazz == Short.class 
             || clazz == Double.class  || clazz == Float.class || clazz == Boolean.class
             || clazz == Class.class);
    }
    
    /**
     * Namespace for Copy flags
     * @author rainer
     */
    public static class Copy
    {
        public static final int RET_SELF        = 0x00; /* return self if copy fails */
        public static final int RET_NULL        = 0x01; /* return null if copy fails */

        public static final int RECURSE_FLAT    = 0x02; /* only for default constructor cloning */
        public static final int RECURSE_DEEP    = 0x04; /* only for default constructor cloning */

        public static final int SKIP_CLONE      = 0x10;
        public static final int SKIP_SERIAL     = 0x20; /* try serialize and deserialize */
        public static final int SKIP_INST       = 0x40; /* try copy constructor or default constructor */
        
        public static boolean has(int flags, int flag)
        {
            return ((flags & flag)!=0);
        }
    }

    /**
     * Makes a copy of an object if possible or returns the object itself if copy is not supported 
     * @param <T> the class type
     * @param obj the object to copy
     * @return either a copy of the object or the object itself if copy is not supported
     */
    public static <T> T copy(T obj)
    {
        return copy(obj, Copy.RET_SELF | Copy.RECURSE_FLAT | Copy.SKIP_SERIAL); /* Serial is too hot */
    }
    
    /**
     * Makes a copy of an object if possible or returns null or self (depending on flags) 
     * @param <T> the class type
     * @param obj the object to copy
     * @param flags options for the copy
     * @return either a copy of the object or null the object itself
     */
    @SuppressWarnings("unchecked")
    public static <T> T copy(T obj, int flags)
    {
        if (obj==null)
            return null;
        // the class
        Class<T> clazz = (Class<T>)obj.getClass(); 
        if (isImmutableClass(clazz)) 
        {   // no need to copy
            return obj; 
        }
        // class check
        if (clazz.isInterface() || clazz.isAnnotation()) {
            log.warn("Unable to copy Interface or Annotation {}", clazz.getName());
            return (Copy.has(flags, Copy.RET_NULL) ? null : obj); // not supported
        }
        // try serialize
        if ((obj instanceof Serializable) && !Copy.has(flags, Copy.SKIP_SERIAL))
        {   try
            {   ByteArrayOutputStream baos = new ByteArrayOutputStream();
                // Write the object
                ObjectOutputStream oos = new ObjectOutputStream(baos);
                oos.writeObject(obj);
                // Read the object
                ObjectInputStream oin = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));
                Object copy = oin.readObject();
                // return result
                return (T)copy;
            } catch (IOException | ClassNotFoundException e) {
                log.error("Copy through Serialization failed for : "+clazz.getName(), e);
            }
        }
        // array copy
        if (clazz.isArray())
        {   // check primitive array like int[] or boolean[]
            if (!(obj instanceof Object[]))
            {   if (Copy.has(flags, Copy.RECURSE_DEEP))
                    log.warn("Copy.RECURSE_DEEP not supported for primitive arrays of type {}", clazz.getSimpleName());
                return (T)invokeSimpleMethod(java.lang.Object.class, obj, "clone", true);
            }
            // Object-array: cast and clone
            T[] cpy = ((T[])obj).clone();
            if (Copy.has(flags, Copy.RECURSE_DEEP))
            {   // copy array items
                for (int i=0; i<cpy.length; i++)
                    cpy[i] = copy(cpy[i], (flags & ~(Copy.RET_NULL)));
            }
            return (T)cpy;
        }
        // try clone
        if ((obj instanceof Cloneable) && !Copy.has(flags, Copy.SKIP_CLONE))
        {   try {
                return (T)invokeSimpleMethod(java.lang.Object.class, obj, "clone", true);
            } catch (Exception e) {
                log.error("Copy through Cloning failed for : "+clazz.getName(), e);
            }
        }
        // try copy through instantiation
        Constructor<T> ctor = (Copy.has(flags, Copy.SKIP_INST) ? null : findMatchingConstructor(clazz, 0, clazz));
        if (ctor!=null)
        {   try
            {   if (ctor.getParameterCount()==1)
                {   // try copy constructor
                    return ctor.newInstance(obj);
                }
                else
                {   // Try default constructor and copy fields
                    T copy = ctor.newInstance();
                    // copy fields of this class and all superclasses
                    for (Class<?> cl = clazz; (cl!=null && cl!=Object.class); cl=cl.getSuperclass())  
                    {
                        Field[] fields = cl.getDeclaredFields();
                        for (Field field : fields) {
                            // ignore static fields
                            if (Modifier.isStatic(field.getModifiers()))
                                continue;
                            // make accessible
                            boolean accessible = field.isAccessible();
                            if (!accessible)
                                field.setAccessible(true);
                            // copy
                            Object value = field.get(obj);
                            if (Copy.has(flags, Copy.RECURSE_FLAT | Copy.RECURSE_DEEP))
                                value = copy(value, (flags & ~(Copy.RET_NULL | Copy.RECURSE_FLAT)));
                            field.set(copy, value);
                            // restore
                            if (!accessible)
                                field.setAccessible(false);
                        }
                    }
                    return copy;
                }
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                log.error("Copy through Instantiation failed for : "+clazz.getName(), e);
            }
        }
        // not supported
        return (Copy.has(flags, Copy.RET_NULL) ? null : obj);
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
     * Modifies a private field value using reflection 
     * @param clazz the class of the object
     * @param object the object or null if static fields are to be changed
     * @param property the field name
     * @param value the field value
     */
    public static void setPrivateFieldValue(Class<?> clazz, Object object, String property, Object value)
    {
        Field field = null;
        boolean accessible = true; 
        try
        {   // Find field
            field = clazz.getDeclaredField(property);
            accessible = field.isAccessible();
            if (accessible==false)
                field.setAccessible(true);
            // Set value 
            field.set(object, value);
        }
        catch (NoSuchFieldException e)
        {   // try superclass
            Class<?> superClass = clazz.getSuperclass();
            if (superClass!=null && !superClass.equals(java.lang.Object.class)) {
                setPrivateFieldValue(superClass, object, property, value);
                return;
            }
            // not found
            log.error("Field \""+property+"\" not found on class "+clazz.getName());
            throw new InternalException(e);
        }
        catch (SecurityException | IllegalArgumentException | IllegalAccessException e)
        {   // Access Error
            log.error("Failed to modify private field \""+property+"\" on class "+clazz.getName(), e);
            throw new InternalException(e);
        } finally {
            // restore accessible
            if (field!=null && accessible==false)
                field.setAccessible(false);
        }
    }

    /**
     * Modifies a private field value using reflection 
     * @param object the object or null if static fields are to be changed
     * @param property the field name
     * @param value the field value
     */
    public static void setPrivateFieldValue(Object object, String property, Object value)
    {
        setPrivateFieldValue(object.getClass(), object, property, value);
    }
    
    /**
     * Creates a new Object instance
     * @param typeClass the class of the object to instantiate
     * @return the instance
     */
    public static <T> T newInstance(Class<T> typeClass)
    {
        try
        {   // Find default constructor and create instance
            Constructor<T> constructor = typeClass.getDeclaredConstructor();
            return constructor.newInstance();
        }
        catch (NoSuchMethodException e)
        {
            throw new InvalidOperationException(typeClass.getName()+" has no default constructor.");
        }
        catch (InstantiationException | IllegalAccessException | IllegalArgumentException | SecurityException | InvocationTargetException e)
        {
            throw new InternalException(e);
        }
    }
    
    /**
     * Creates a new Object instance with a single parameter constructor
     * @param typeClass the type of the object to instantiate
     * @param paramClass the type of the constructor parameter
     * @param typeClass the value of the constructor parameter 
     * @return the instance
     */
    public static <T> T newInstance(Class<T> typeClass, Class<?> paramClass, Object paramValue)
    {
        Constructor<T> constructor = findMatchingConstructor(typeClass, 1, paramClass);
        return newInstance(constructor, paramValue);
    }

    /**
     * Creates a new Object instance
     * @param typeConstructor the constructor of the object to instantiate
     * @return the instance
     */
    public static <T> T newInstance(Constructor<T> typeConstructor, Object... params)
    {
        try
        {   // Check number of arguments
            if (typeConstructor.getParameterCount()!=params.length)
                throw new InvalidArgumentException("typeConstructor|params", new Object[] { typeConstructor.getParameterCount(), params.length });
            // create
            return typeConstructor.newInstance(params);
        }
        catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e)
        {
            throw new InternalException(e);
        }
    }

    /**
     * copied from org.apache.commons.beanutils.ConstructorUtils since it's private there
     * @param <T> the class type
     * @param clazz the class of the object
     * @param minParams minimum number of params
     * @param parameterTypes the param types
     * @return the constructor or null
     */
    @SuppressWarnings("unchecked")
    public static <T> Constructor<T> findMatchingConstructor(Class<T> clazz, int minParams, Class<?>... parameterTypes)
    {
        // Minimum matching params
        int paramSize = (parameterTypes!=null ? parameterTypes.length : 0);
        if (minParams<0 || minParams>paramSize)
            minParams = paramSize;
     
        // see if we can find the method directly (faster)
        /*
        try {
            final Constructor<T> ctor = (parameterTypes!=null ? clazz.getConstructor(parameterTypes) : clazz.getConstructor());
            try {
                // XXX Default access superclass workaround
                ctor.setAccessible(true);
            } catch (final SecurityException se) {
                // SWALLOW, if workaround fails don't fret.
            }
            return ctor;
        } catch (final NoSuchMethodException e) {
            // ignore  
        }
        */
        
        // get all public constructors 
        Constructor<?>[] ctors = clazz.getConstructors();
        Constructor<T> foundCtor = null;
        
        // Search through all valid lengths
        for (int pLen = paramSize; pLen >= minParams; pLen--)
        {   
            // find longest valid constructor
            for (int i = 0, size = ctors.length; i < size; i++)
            {
                // Check parameter counts
                if (ctors[i].getParameterCount()!=pLen)
                    continue;
                
                // Param count matches
                boolean match = true;
                if (pLen>0) {
                    // Compare parameter types
                    Class<?>[] ctorParams = ctors[i].getParameterTypes();
                    for (int n = 0; n < pLen; n++)
                    {
                        if (!ObjectUtils.isAssignmentCompatible(ctorParams[n], parameterTypes[n]))
                        {
                            /* --------------------
                             * Allow down-casting ?
                             * --------------------
                            if (parameterTypes[n].isAssignableFrom(ctorParams[n]))
                            {   // down-casting possible
                                log.warn("down-casting may be possible.");
                                continue;
                            }
                            */
                            // No match
                            match = false;
                            break;
                        }
                    }
                }
                if (match) {
                    // get accessible version of method
                    Constructor<?> ctor = ConstructorUtils.getAccessibleConstructor(ctors[i]);
                    if (ctor != null) {
                        try {
                            // XXX Default access superclass workaround
                            ctor.setAccessible(true);
                        } catch (SecurityException se) {
                            // ignore 
                        }
                        Constructor<T> typedCtor = (Constructor<T>) ctor;
                        return typedCtor;
                    }
                    else
                    {   // set as found constructor
                        foundCtor = (Constructor<T>) ctors[i];
                    }
                }
            }
        }
        return foundCtor;
    }
    
    /**
     * Invoke a method on an object using reflection
     * @param clazz the class from which to obtain the field
     * @param object the object instance on which to invoke the method
     * @param methodName the name of the method to invoke
     * @param paramTypes the list of parameter types 
     * @param paramValues the list of parameter values 
     * @param makeAccessible flag whether to make private methods accessible
     * @return the return value of the method
     */
    public static Object invokeMethod(Class<?> clazz, Object object, String methodName, Class<?>[] paramTypes, Object[] paramValues, boolean makeAccessible)
    {
        // check arguments
        if (clazz==null || !clazz.isInstance(object))
            throw new InvalidArgumentException("clazz", clazz);
        if (StringUtils.isEmpty(methodName))
            throw new InvalidArgumentException("methodName", methodName);
        // method parameters
        boolean hasMethodParams = (paramTypes!=null && paramTypes.length>0); 
        if (hasMethodParams && (paramValues==null || paramValues.length!=paramTypes.length))
            throw new InvalidArgumentException("paramValues", paramValues);
        // begin
        boolean accessible = true; 
        Method method = null;
        try
        {   // find method and invoke
            if (hasMethodParams)
                method = (makeAccessible ? clazz.getDeclaredMethod(methodName, paramTypes) : clazz.getMethod(methodName, paramTypes));
            else
                method = (makeAccessible ? clazz.getDeclaredMethod(methodName) : clazz.getMethod(methodName));
            // set Accessible
            accessible = method.isAccessible();
            if (makeAccessible && accessible==false)
                method.setAccessible(true);
            // invoke
            if (hasMethodParams)
                return method.invoke(object, paramValues);
            else
                return method.invoke(object);
        }
        catch (NoSuchMethodException e)
        {   // No such Method
            if (makeAccessible)
            {   // try superclass
                clazz = clazz.getSuperclass();
                if (clazz!=null && !clazz.equals(java.lang.Object.class))
                    return invokeMethod(clazz, object, methodName, paramTypes, paramValues, true);
            }
            // not found
            if (hasMethodParams)
                methodName += StringUtils.concat("({", String.valueOf(paramTypes.length), " params})");
            throw new NotImplementedException(object, methodName);
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
     * Invokes a method with one parameter on an object using reflection
     * @param object the object instance on which to invoke the method
     * @param methodName the name of the method to invoke 
     * @param paramType the method parameter type
     * @param paramValue the method parameter value
     * @return the return value of the method
     */
    public static Object invokeMethod(Object object, String methodName, Class<?> paramType, Object paramValue)
    {
        try
        {   // find and invoke method
            Class<?> clazz = object.getClass();
            return invokeMethod(clazz, object, methodName, new Class<?>[] { paramType }, new Object[] { paramValue }, false);
        }
        catch (NotImplementedException e)
        {   // second chance: try superclass of parameter
            paramType = paramType.getSuperclass();
            if (paramType!=null && paramType!=java.lang.Object.class)
            {   // try superclass
                return invokeMethod(object, methodName, paramType, paramValue);
            }
            // not found
            throw e;
        }
    }

    /**
     * Invoke a simple method (without parameters) on an object using reflection
     * @param clazz
     * @param object the object instance on which to invoke the method
     * @param methodName the name of the method to invoke 
     * @return the return value of the method
     */
    public static Object invokeSimpleMethod(Class<?> clazz, Object object, String methodName, boolean makeAccessible)
    {
        return invokeMethod(clazz, object, methodName, null, null, makeAccessible);
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

    /**
     * Invoke a simple method (without parameters) on an object
     * @param object the object on which to invoke the method
     * @param method the method
     * @return the return value
     */
    public static Object invokeSimpleMethod(Object object, Method method)
    {
        try
        {
            return method.invoke(object, EMPTY_ARGS);
        }
        catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e)
        {
            throw new NotSupportedException(object, method.getName(), e); 
        } 
    }
    
    /**
     * Returns the JAR name that contains the implementation of a specific class
     * @param clazz the class 
     * @return the JAR File Name
     */
    public static String getImplemenationJarName(Class<?> clazz)
    {
        String implJar;
        try
        {   // detect
            implJar = clazz.getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI()
                    .getPath();
            // return JAR name
            return implJar.substring(implJar.lastIndexOf('/')+1);
        }
        catch (URISyntaxException e)
        {
            log.error("getImplemenationJarName failed.", e);
            return "[URISyntaxException]";
        }
        catch (NullPointerException e)
        {
            log.error("getImplemenationJarName failed.", e);
            return "[NullPointerException]";
        }
    }

    /**
     * Returns the JAR name that contains the implementation of a specific class
     * @param className the class Name of the class 
     * @return the JAR File Name or "[ClassNotFoundException]" if the class is not available
     */
    public static String getImplemenationJarName(String className)
    {
        try
        {   // Find the Class for the given name
            Class<?> clazz = Class.forName(className);
            return getImplemenationJarName(clazz);
        }
        catch (ClassNotFoundException e)
        {
            log.info("Class \"{}\" not found", className);
            return "[ClassNotFoundException]";
        }
    }
    
}
