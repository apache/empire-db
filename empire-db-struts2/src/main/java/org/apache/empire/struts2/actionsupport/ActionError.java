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
package org.apache.empire.struts2.actionsupport;

import java.text.MessageFormat;

import org.apache.empire.commons.ErrorType;
import org.apache.empire.exceptions.EmpireException;
import org.apache.empire.exceptions.InternalException;
import org.apache.empire.struts2.action.ErrorInfo;

public class ActionError implements ErrorInfo
{
    private ErrorType errType;  // Type of error
    private String[]  errParams;  // Error message params
    private String    errMsg;

    protected void clear()
    {
        errType = null;
    }
    
    public boolean hasError()
    {
        return (errType!=null);
    }

    public ErrorType getErrorType()
    {
        return errType;
    }
    public String[] getErrorParams()
    {
        return errParams;
    }
    public String getErrorMessage()
    {
        return errMsg;
    }
    
    /**
     * Private constructor to prevent inheritance.
     * Other classes should not be derived from Action Error
     * since this might be stored on the session.
     * Instead derive your error objects from ErrorObject
     */
    @SuppressWarnings("unused")
    private ActionError()
    {  
        // Default Constructor
    }

    public ActionError(ErrorInfo other)
    {
        // copy other error
        errType   = other.getErrorType();
        errParams = other.getErrorParams();
        errMsg    = other.getErrorMessage();
    }

    public ActionError(EmpireException e)
    {
        // copy other error
        errType   = e.getErrorType();
        errParams = e.getErrorParams();
        errMsg    = e.getMessage();
    }

    public ActionError(Throwable e)
    {
        // copy other error
        this(new InternalException(e));
    }

    public ActionError(ErrorType errType, String[] params)
    {   // Set the Error
        this.errType = errType;
        this.errParams = params;
        this.errMsg = MessageFormat.format(errType.getMessagePattern(), (Object[])params);
    }

    public ActionError(ErrorType errType, String param)
    {
        this(errType, new String[] { param });
    }

    public ActionError(ErrorType errType)
    {
        this(errType, (String[])null);
    }

    /**
     * @see ErrorObject#error(ErrorType, Object, Object)
     */
    /*
    public ActionError(ErrorType errType, Object param1, Object param2)
    {
        error(errType, param1, param2);
    }
    */

    /**
     * @see ErrorObject#error(ErrorType, Throwable)
     */
    /*
    public ActionError(ErrorType errType, Exception exptn)
    {
        error(errType,  exptn);
    }
    */

    /**
     * @see ErrorObject#error(Throwable)
     */
    /*
    public ActionError(Throwable exptn)
    {
        error(Errors.Exception, exptn);
    }
    */
    
    /**
     * @return the error message for this Action Error
     */
    @Override
    public String toString()
    {
        return (hasError() ? getErrorMessage() : "");
    }
}
