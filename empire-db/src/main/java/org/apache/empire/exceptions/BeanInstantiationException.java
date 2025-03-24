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
package org.apache.empire.exceptions;

import java.lang.reflect.Constructor;

import org.apache.empire.commons.ErrorType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BeanInstantiationException extends EmpireException
{
    private static final long serialVersionUID = 1L;

    // Logger
    private static final Logger log = LoggerFactory.getLogger(BeanInstantiationException.class);
    
    public static final ErrorType errorType = new ErrorType("error.beanInstantiationFailed", "Unable create an instance of type {0}.");
    
    public BeanInstantiationException(Class<?> clazz, ReflectiveOperationException e)
    {
        super(errorType, new String[] { clazz.getName() }, getCause(e));
    }
    
    public BeanInstantiationException(Class<?> clazz, RuntimeException e)
    {
        super(errorType, new String[] { clazz.getName() }, e);
    }
    
    public BeanInstantiationException(Constructor<?> c, ReflectiveOperationException e)
    {
        this(c.getDeclaringClass(), e);
    }
    
    public BeanInstantiationException(Constructor<?> c, RuntimeException e)
    {
        this(c.getDeclaringClass(), e);
    }
    
    /**
     * log the error
     */
    @Override
    protected void log()
    {
       if ( log.isErrorEnabled() )
            log.error(getMessage());
       else
           super.log();
    }
    
    
}
