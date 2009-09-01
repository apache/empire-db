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
package org.apache.empire;

import org.apache.empire.commons.ErrorObject;
import org.apache.empire.commons.ErrorType;

/**
 * This exception type is used for all empire errors.<br>
 * Exceptions will only be thrown if exceptions are enabled in the ErrorObject.
 * @see ErrorObject#setExceptionsEnabled(boolean)
 */
public final class EmpireException extends RuntimeException
{
    private static final long serialVersionUID = 1L;
    
    private final ErrorType errorType;
    private final ErrorObject errorObject; 
    
    /**
     * creates an empire exception from an error object.
     * @param errorObject
     */
    public EmpireException(final ErrorObject errorObject)
    {
        super(errorObject.getErrorMessage());
        // init
        this.errorType = errorObject.getErrorType();
        this.errorObject = errorObject;
    }
    
    @Override
    public String toString()
    {   // Return Object class name and error message
        return errorObject.getClass().getName() + ": " + getMessage();
    }

    /**
     * The type of error that occurred
     * @see org.apache.empire.commons.Errors
     * @return the type of error
     */
    public ErrorType getErrorType()
    {
        return errorType;
    }

    /**
     * @return the object that caused the error
     */
    public ErrorObject getErrorObject()
    {
        return errorObject;
    }
}
