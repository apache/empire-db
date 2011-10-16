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
    private final String[]  errorParams;
    // private final String errorSourceClassname;
    
    private static String formatErrorMessage(final ErrorType errType, final String[] params)
    {
        // Check parameter count
        int paramCount = (params!=null) ? params.length : 0;
        if (paramCount!= errType.getNumParams())
        {   // Number of parameters does not match
            log.warn("Invalid Number of arguments supplied for error " + errType.getKey() 
                   + "\nArguments supplied= " + String.valueOf(paramCount) + "; Arguments expected= " + String.valueOf(errType.getNumParams()));
        }
        // Log Error
        String msg = MessageFormat.format(errType.getMessagePattern(), (Object[])params);
        return msg;
    }
    
    /**
     * Constructor for derived classes
     * @param errType
     * @param params
     * @param cause
     */
    protected EmpireException(final ErrorType errType, final String[] params, final Throwable cause)
    {
        super(formatErrorMessage(errType, params), cause);
        // save type and params for custom message formatting
        this.errorType = errType;
        this.errorParams = params;
        // done
        log();
    }
    /**
     * Constructor for derived classes
     * @param errType
     * @param params
     * @param cause
     */
    protected EmpireException(final ErrorType errType, final String[] params)
    {
        this(errType, params, null);
    }
    
    /**
     * log the error (info must be enabled)
     */
    protected void log()
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
    public String[] getErrorParams()
    {
        return errorParams;
    }

    /**
     * when serializing, convert all params to strings (unnesessary after change from object[] to string[]
     * @param out
     * @throws IOException
    private void writeObject(ObjectOutputStream out) throws IOException
    {
        // normalize Params
        if (errorParams!=null)
        {   // convert complex params from object[] to string[]
            for (int i=0; i<errorParams.length; i++)
            {   // convert to String
                Object o = errorParams[i]; 
                if (o==null || (o instanceof String))
                    continue;
                if (o.getClass().isPrimitive())
                    continue;
                // Convert to String
                errorParams[i] = StringUtils.toString(o);
            }
        }
        // Serialize
        out.defaultWriteObject(); 
    }
    */
}
