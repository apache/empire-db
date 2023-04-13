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

public class ObjectNotValidException extends EmpireException
{
    private static final long serialVersionUID = 1L;
    
    public static final ErrorType errorType = new ErrorType("error.objectNotValid", "The object {0} is invalid: {1}");

    public ObjectNotValidException(Class<?> clazz, String message)
    {
        super(errorType, new String[] { (clazz!=null) ? clazz.getName() : "{unknown}", message } );
    }
    
    public ObjectNotValidException(Object obj, String message)
    {
        this((obj!=null) ? obj.getClass() : null, message);
    }
    
    public ObjectNotValidException(Object obj)
    {
        this((obj!=null) ? obj.getClass() : null, "Not initialized");
    }

}
