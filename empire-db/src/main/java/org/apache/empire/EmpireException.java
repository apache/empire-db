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

import java.io.Serializable;
import java.text.MessageFormat;

import org.apache.empire.commons.ErrorInfo;
import org.apache.empire.commons.ErrorType;
import org.apache.empire.commons.Errors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This exception type is used for all empire errors.<br>
 * Exceptions will only be thrown if exceptions are enabled in the ErrorObject.
 * @see ErrorObject#setExceptionsEnabled(boolean)
 */
public final class EmpireException extends RuntimeException
{
    private static final long serialVersionUID = 1L;
    
    private static final Logger log = LoggerFactory.getLogger(EmpireException.class);
    
    private final ErrorType errorType;
    private final ErrorInfo errorObject;
    
    /**
     * creates an empire exception from an error object.
     * @param errorObject
     */
    @Deprecated
    public EmpireException(final ErrorInfo errorObject)
    {
        super(errorObject.getErrorMessage());
        // init
        this.errorType = errorObject.getErrorType();
        this.errorObject = new ErrorInfoImpl(errorObject);
    }
    
    public EmpireException(final ErrorType errType, final Object... params) {
    	super(getMessage(errType, params));
		this.errorType = errType;
		this.errorObject = null;
	}
    
    public EmpireException(final Throwable cause, final ErrorType errType, final Object... params) {
    	super(getMessage(errType, params), cause);
		this.errorType = errType;
		this.errorObject = null;
	}
    
    public EmpireException(final Throwable cause) {
    	super(getMessage(cause), cause);
		this.errorType = Errors.Exception;
		this.errorObject = null;
	}
    
    
    
//    @Override
//    public String toString()
//    {   // Return Object class name and error message
//        return errorObjectClassname + ": " + getMessage();
//    }

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
    public ErrorInfo getErrorObject()
    {
        return errorObject;
    }

    /**
     * A serializable version of {@link ErrorInfo}.
     */
    private static class ErrorInfoImpl implements ErrorInfo, Serializable {
        private static final long serialVersionUID = 1L;
        
        private final boolean hasError;
        private final ErrorType errorType;
        private final Serializable[] errorParams;
        private final String errorSource;
        private final String errorMessage;

        /**
         * Copy ctor.
         * @param errorInfo
         */
        ErrorInfoImpl(ErrorInfo errorInfo) {
            this.hasError = errorInfo.hasError();
            this.errorType = errorInfo.getErrorType();
            Object[] params = errorInfo.getErrorParams();
            if (params != null) {
                this.errorParams = new Serializable[params.length];
                for (int i=0; i<params.length; i++) {
                  Object p = params[i];
                  if (p == null) {
                      this.errorParams[i] = null;
                  } else if (p instanceof Serializable) {
                      Serializable serializable = (Serializable) p;
                      this.errorParams[i] = serializable;
                  } else {
                      this.errorParams[i] = p.toString();
                  }
                }
            } else {
                this.errorParams = null;
            }
            this.errorSource = errorInfo.getErrorSource();
            this.errorMessage = errorInfo.getErrorMessage();
        }

        public boolean hasError() {
            return hasError;
        }

        public ErrorType getErrorType() {
            return errorType;
        }

        public Object[] getErrorParams() {
            return errorParams;
        }

        public String getErrorSource() {
            return errorSource;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
    
    private static void validate(final ErrorType errType, final Object[] params){
    	// check error code
        if (errType == Errors.None)
        {	// Must supply a valid error code
            log.error("error function called with invalid error Code.");
        }
        // Check parameter count
        int paramCount = (params!=null) ? params.length : 0;
        if (paramCount!= errType.getNumParams())
        {   // Number of parameters does not match
            log.warn("Invalid Number of arguments supplied for error " + errType.getKey() 
                   + "\nArguments supplied= " + String.valueOf(paramCount) + "; Arguments expected= " + String.valueOf(errType.getNumParams()));
        }
        // Make sure no real objects are stored in the params section
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
//                if (o instanceof ErrorInfo)
//                    params[i] = getMessage((ErrorInfo)o);
//                else 
                    params[i] = String.valueOf(o.toString());
            }
        }

    }
    
    /**
     * Returns the message associated with an error.
     * 
     * @param error the error information for which to obtain an error message
     * @return the error message or an empty string if no error has been set.
     */
    private static String getMessage(final ErrorType type, final Object[] params)
    {
    	
    	validate(type, params);
    	
        // Check params
        if (type==null || type==Errors.None)
            return ""; // No Error
        if (params==null)
            return type.getMessagePattern();
        // Get Error Message
        String msgPattern = type.getMessagePattern();
        String msg = MessageFormat.format(msgPattern, params);
        return msg;
    }
    
    private static String getMessage(Throwable cause){
    	// Exception
        String type  = cause.getClass().getName();
        if (type.startsWith("java.lang."))
            type = type.substring("java.lang.".length());
        // The message
        String msg   = cause.getMessage();
        // Prepare stack trace
        StackTraceElement[] stack = cause.getStackTrace();
        String pos = (stack!=null) ? stack[0].toString() : cause.getClass().getName();
        return (getMessage(Errors.Exception, new Object[] { type, msg, pos }));
    }
}
