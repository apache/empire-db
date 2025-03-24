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

public class NotSupportedException extends EmpireException
{
    private static final long serialVersionUID = 1L;
    
    public static final ErrorType errorType = new ErrorType("error.notSupported", "The operation {0} is not supported for type {1}.");
    
    private static String typeFromObject(Object object) 
    {
        if (object instanceof String)
            return (String)object;
        if (object instanceof Class<?>)
            return ((Class<?>)object).getName();
        if (object!=null)
            return object.getClass().getName();
        // null
        return "-";
    }
    
    public NotSupportedException(Object object, String operationName, ReflectiveOperationException e)
    {
        super(errorType, new String[] { operationName, typeFromObject(object) }, getCause(e));
    }
    
    public NotSupportedException(Object object, String operationName, RuntimeException e)
    {
        super(errorType, new String[] { operationName, typeFromObject(object) }, e);
    }
    
    public NotSupportedException(Object object, String operationName)
    {
        super(errorType, new String[] { operationName, typeFromObject(object) });
    }

}
