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

import java.text.MessageFormat;

import org.apache.empire.commons.ErrorType;

/**
 * InvalidOperationException
 * Indicates that an operation cannot be performed due to requirements not met.
 */
public class InvalidOperationException extends EmpireException
{
    private static final long serialVersionUID = 1L;
    
    public static final ErrorType errorType = new ErrorType("error.internal", "Internal Error: {0}");
    
    public InvalidOperationException(String errorMessage)
    {
        super(errorType, new String[] { errorMessage });
    }
    
    public InvalidOperationException(String errorMessage, Throwable cause)
    {
        super(errorType, new String[] { errorMessage }, cause);
    }
    
    public InvalidOperationException(String msgTemplate, Object... msgArgs)
    {
        super(errorType, new String[] { MessageFormat.format(msgTemplate, msgArgs) }, (msgArgs.length>0 && (msgArgs[msgArgs.length-1] instanceof Exception)) ? (Exception)msgArgs[msgArgs.length-1] : null );
    }

}
