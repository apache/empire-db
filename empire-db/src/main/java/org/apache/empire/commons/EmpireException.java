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

import java.text.MessageFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This exception type is used for all empire errors.<br>
 * Exceptions will only be thrown if exceptions are enabled in the ErrorObject.
 * @see ErrorObject#setExceptionsEnabled(boolean)
 */
public class EmpireException extends RuntimeException
{
    // Logger
    private static final Logger log = LoggerFactory.getLogger(EmpireException.class);
    
    private static final long serialVersionUID = 1L;
    
    private final ErrorType errorType;
    private final Object[]  errorParams;
    // private final String errorSourceClassname;
    
    private static String formatErrorMessage(final ErrorType errType, final Object[] params)
    {
        // check error code
        if (errType == Errors.None)
        {   // Must supply a valid error code
            log.error("error function called with invalid error Code.");
            return formatErrorMessage(Errors.Internal, new Object[] { "Invalid Error Code" });
        }
        // Check parameter count
        int paramCount = (params!=null) ? params.length : 0;
        if (paramCount!= errType.getNumParams())
        {   // Number of parameters does not match
            log.warn("Invalid Number of arguments supplied for error " + errType.getKey() 
                   + "\nArguments supplied= " + String.valueOf(paramCount) + "; Arguments expected= " + String.valueOf(errType.getNumParams()));
        }
        // Log Error
        String msg = MessageFormat.format(errType.getMessagePattern(), params);
        return msg;
    }
    
    private static Object[] paramsFromThrowable(final Throwable exptn)
    {
        // Exception
        String type  = exptn.getClass().getName();
        if (type.startsWith("java.lang."))
            type = type.substring("java.lang.".length());
        // The message
        String msg   = exptn.getMessage();
        // Prepare stack trace
        StackTraceElement[] stack = exptn.getStackTrace();
        String pos = (stack!=null) ? stack[0].toString() : "{unknown}";
        // Create Error
        return new Object[] { type, msg, pos };
    }
    
    private static Object[] normalizeParams(final Object[] params)
    {
        if (params!=null && params.length>0)
        {   // convert complex params form object[] to string[]
            for (int i=0; i<params.length; i++)
            {   // convert to String
                Object o = params[i]; 
                if (o==null || (o instanceof String))
                    continue;
                if (o.getClass().isPrimitive())
                    continue;
                // Convert to String
                if (o instanceof RuntimeException)
                    params[i] = ((RuntimeException)o).getMessage();
                else 
                    params[i] = String.valueOf(o.toString());
            }
        }
        return params;
    }

    /**
     * Constructor for derived classes
     * @param errType
     * @param params
     * @param cause
     */
    public EmpireException(final ErrorType errType, final Object[] params, final Throwable cause)
    {
        super(formatErrorMessage(errType, params), cause);
        // save type and params for custom message formatting
        this.errorType = errType;
        this.errorParams = params;
        // done
        log();
    }

    /**
     * Constructor
     * @param errType
     * @param params
     */
    public EmpireException(final ErrorType errType, final Object... params)
    {
        super(formatErrorMessage(errType, params));
        // save type and params for custom message formatting
        this.errorType = errType;
        this.errorParams = normalizeParams(params);
        // done
        log();
    }
    
    /**
     * Constructor
     * @param cause
     */
    public EmpireException(final Throwable cause)
    {
        super(formatErrorMessage(Errors.Exception, paramsFromThrowable(cause)), cause);
        // save type and params for custom message formatting
        this.errorType   = Errors.Exception;
        this.errorParams = paramsFromThrowable(cause); 
        // done
        log();
    }

    /**
     * Constructor
     * @param other
     */
    public EmpireException(final EmpireException other)
    {
        super(other.getMessage(), other);
        // save type and params for custom message formatting
        this.errorType   = other.getErrorType();
        this.errorParams = other.getErrorParams(); 
        // done
        log();
    }
    
    /**
     * log the error (info must be enabled)
     */
    private void log()
    {
        if (log.isInfoEnabled())
            log.info("An Error occured. Message is: {}", this.getMessage());
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
     * The message parameters for the message.
     * @see org.apache.empire.commons.Errors
     * @return the type of error
     */
    public Object[] getErrorParams()
    {
        return errorParams;
    }
}
