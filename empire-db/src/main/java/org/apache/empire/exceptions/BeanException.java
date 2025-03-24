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

import org.apache.empire.commons.ErrorType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * BeanException
 * Base class for bean related exceptions
 * @author doebele
 */
public abstract class BeanException extends EmpireException
{
    private static final long serialVersionUID = 1L;
    // Logger
    private static final Logger log = LoggerFactory.getLogger(BeanException.class);
    
    /**
     * Returns the root cause of a ReflectiveOperationException such as InvocationTargetException
     * @param e the ReflectiveOperationException
     * @return the causing exception
     */
    protected static Exception getCause(ReflectiveOperationException e)
    {
        Throwable cause = e.getCause();
        /*
        if (cause instanceof EmpireException)
            return ((EmpireException)cause);
        */
        if (cause instanceof Exception)
            return (Exception)cause;
        // wrap    
        return e;
    }
    
    protected static String getTypeName(Object object)
    {
        if (object instanceof Class<?>)
            return ((Class<?>)object).getName();
        if (object!=null)
            return object.getClass().getName();
        // null
        return "{unknown}";
    }

    /**
     * Constructor
     * @param errType the error type
     * @param params the error params
     * @param cause the causing exception
     */
    protected BeanException(ErrorType errType, String[] params, Throwable cause)
    {
        super(errType, params, cause);
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
