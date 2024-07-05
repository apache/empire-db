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
 * This exception type is used for all empire errors.
 */
public class EmpireException extends RuntimeException
{
    private static final long serialVersionUID = 1L;
    
    // Logger
    private static final Logger log = LoggerFactory.getLogger(EmpireException.class);

    /**
     * ExceptionMessageFormatter
     * returns a message text for an Exception from a given pattern and parameters 
     */
    public static class ExceptionMessageFormatter
    {
        protected String missingArgument = "?";
        
        public String format(final ErrorType errType, String pattern, String[] params)
        {
            // format error message
            try {
                // the pattern
                if (pattern==null)
                    pattern=errType.getMessagePattern();
                // Check parameter count
                int patParamCount = errType.getNumParams();
                int extraParamIndex = -1;
                // wildcard
                if (pattern.contains("{*}")) 
                {   pattern = pattern.replace("{*}", "{"+String.valueOf(patParamCount)+"}");
                    extraParamIndex = patParamCount;
                    patParamCount++;
                }
                // check params
                int paramCount = (params!=null) ? params.length : 0;            
                if (paramCount < patParamCount)
                {   // Missing arguments
                    log.warn("Invalid Number of arguments supplied for error {}: Arguments expected={} / Arguments supplied={}.", errType.getKey(), patParamCount, paramCount);
                }
                // more params than expected
                else if (paramCount>patParamCount && extraParamIndex<0)
                {   // Too many arguments
                    log.info("Additional arguments supplied for error {}: Arguments expected={} / Arguments supplied={}.", errType.getKey(), patParamCount, paramCount);
                }
                // Build format list
                Object[] messageArgs = new String[patParamCount];
                for (int i=0; i<messageArgs.length; i++)
                {
                    if (i==extraParamIndex) {
                        // summary of remaining params
                        StringBuilder b = new StringBuilder();
                        for (int j=extraParamIndex;j<paramCount;j++)
                        {   if (b.length()>0)
                                b.append(", ");
                            b.append(formatParam(errType, i, params[j]));
                        }
                        messageArgs[i] = b.toString();
                    }
                    else if (i<paramCount)
                        messageArgs[i] = formatParam(errType, i, params[i]);
                    else
                        messageArgs[i] = missingArgument;
                }
                // format now
                String msg = MessageFormat.format(pattern, messageArgs);
                return msg;
            } catch(Exception e) {
                log.error("Unable to format error message: "+pattern, e);
                return pattern;
            }
        }
        
        protected Object formatParam(final ErrorType errType, int index, String param)
        {
            return param;
        }
    }
    
    private static ExceptionMessageFormatter messageFormatter = new ExceptionMessageFormatter();
    
    public static ExceptionMessageFormatter getMessageFormatter()
    {
        return messageFormatter;
    }

    public static void setMessageFormatter(ExceptionMessageFormatter messageFormatter)
    {
        if (messageFormatter==null)
            throw new InvalidArgumentException("messageFormatter", messageFormatter);
        // set formatter
        EmpireException.messageFormatter = messageFormatter;
    }

    public static String formatErrorMessage(final ErrorType errType, String pattern, final String[] params)
    {
        return messageFormatter.format(errType, pattern, params);
    }
    
    private final ErrorType errorType;
    private final String[]  errorParams;
    // private final String errorSourceClassname;
    
    
    /**
     * Constructor for derived classes
     * @param errType
     * @param params
     * @param cause
     */
    protected EmpireException(final ErrorType errType, final String[] params, final Throwable cause)
    {
        super(messageFormatter.format(errType, null, params), cause);
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
     * @see org.apache.empire.commons.ErrorType
     * @return the type of error
     */
    public ErrorType getErrorType()
    {
        return errorType;
    }

    /**
     * The message parameters for the message.
     * @return the parameter array
     */
    public String[] getErrorParams()
    {
        return errorParams;
    }

    /**
     * when serializing, convert all params to strings 
     * Obsolete after change from object[] to string[]
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
