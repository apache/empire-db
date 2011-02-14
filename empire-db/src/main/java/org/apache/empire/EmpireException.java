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

import org.apache.empire.commons.ErrorInfo;
import org.apache.empire.commons.ErrorObject;
import org.apache.empire.commons.ErrorType;

import java.io.Serializable;

/**
 * This exception type is used for all empire errors.<br>
 * Exceptions will only be thrown if exceptions are enabled in the ErrorObject.
 * @see ErrorObject#setExceptionsEnabled(boolean)
 */
public final class EmpireException extends RuntimeException
{
    private static final long serialVersionUID = 1L;
    
    private final ErrorType errorType;
    private final ErrorInfo errorObject;
    private final String errorObjectClassname;
    
    /**
     * creates an empire exception from an error object.
     * @param errorObject
     */
    public EmpireException(final ErrorInfo errorObject)
    {
        super(errorObject.getErrorMessage());
        // init
        this.errorType = errorObject.getErrorType();
        this.errorObject = new ErrorInfoImpl(errorObject);
        this.errorObjectClassname = errorObject.getClass().getName();
    }
    
    @Override
    public String toString()
    {   // Return Object class name and error message
        return errorObjectClassname + ": " + getMessage();
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
}
