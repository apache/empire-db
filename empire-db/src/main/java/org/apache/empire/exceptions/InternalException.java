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

public class InternalException extends EmpireException
{
    private static final long serialVersionUID = 1L;

    // Logger
    private static final Logger log = LoggerFactory.getLogger(InternalException.class);
    
    public static final ErrorType errorType = new ErrorType("error.exception", "An Exception of type {0} occurred.\n-->Message is: {1}\n-->at Position: {2}");

    private static String[] paramsFromThrowable(final Throwable exptn)
    {
        // Exception
        String type  = exptn.getClass().getName();
        if (type.startsWith("java.lang."))
            type = type.substring("java.lang.".length());
        // The message
        String msg = exptn.getMessage();
        // Prepare stack trace
        StackTraceElement[] stack = exptn.getStackTrace();
        String pos = (stack!=null && stack.length>0) ? stack[0].toString() : "{unknown}";
        // Create Error
        return new String[] { type, msg, pos };
    }
    
    public InternalException(Throwable cause)
    {
        super(errorType, paramsFromThrowable(cause));
    }
    
    public InternalException(EmpireException other)
    {
        super(other.getErrorType(), other.getErrorParams());
        // should not happen
        log.warn("InternalException used to wrap an EmpireException! Type and params will be copied.");
    }

}
