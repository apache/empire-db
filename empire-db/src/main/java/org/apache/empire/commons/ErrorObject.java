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
import java.util.WeakHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.empire.EmpireException;


/**
 * This class holds and provides information about the last error that occured on an object.
 * <P> 
 * In order to use this class you must derive your class from Error Object.<BR>
 * If a method fails then you must set the error by calling one of the error(...) functions.<BR>
 * The method usually indicates failure by returning false.<BR>
 * You may then retrieve error information by calling one of the function defined by the {@link ErrorInfo} interface.<BR>
 * All classes of the empire framework are derived from this class.<BR>
 * <P> 
 * This class is thread save so each thread may have it's own error information for a particular object. 
 * <P> 
 */
public abstract class ErrorObject implements ErrorInfo
{
    // Logger
    private static final Logger log = LoggerFactory.getLogger(ErrorObject.class);
    // Exceptions flag
    private static boolean exceptionsEnabled = false;

    /**
     * Returns wether or not Exceptions will be thrown on failure.<BR>
     * If disabled (default) the functions' return values will indicate success or failure<BR> 
     * @return true if Exceptions are enable or false otherwise
     */
    public static boolean isExceptionsEnabled()
    {
        return exceptionsEnabled;
    }

    /**
     * Use this to enable or disable Exceptions.<BR>
     * If exceptionsEnabled is false no exceptions will be thrown.<BR>
     * Instead the functions' return values will indicate success or failure<BR> 
     * @param enableExceptions true to enable exceptions or false to disable
     */
    public static void setExceptionsEnabled(boolean enableExceptions)
    {
        ErrorObject.exceptionsEnabled = enableExceptions;
    }

    /**
     * This class is internally used by the ErrorObject 
     * to provide thread-safe access to the error information.
     */
    protected static class ObjectErrorInfo
    {
        /**
         * Type of error
         */
        public ErrorType errType    = null; 
        /**
         * Error message params
         */
        public Object[]  errParams  = null; 
        /**
         * Source object's class name 
         */
        public String    errSource  = null;
        /**
         *  public constructor allows
         *  derived classes to instantiate 
         */
        public ObjectErrorInfo() 
        { 
            /* allow derived classes to instantiate */ 
        }
    }

    // The HashMap of error objects for the local thread
    private static final ThreadLocal<WeakHashMap<ErrorObject, ObjectErrorInfo>>
          errorMap = new ThreadLocal<WeakHashMap<ErrorObject, ObjectErrorInfo>>();

    
    /**
     * Returns the message associated with an error.
     * 
     * @param error the error information for which to obtain an error message
     * @return the error message or an empty string if no error has been set.
     */
    public static String getMessage(ErrorInfo error)
    {
        if (error==null)
            return "";
        // Check params
        ErrorType type = error.getErrorType();
        if (type==null || type==Errors.None)
            return ""; // No Error
        if (error.getErrorParams()==null)
            return type.getMessagePattern();
        // Get Error Message
        String msgPattern = type.getMessagePattern();
        String msg = MessageFormat.format(msgPattern, error.getErrorParams());
        return msg;
    }
    
    /**
     * Constructs an ErrorObject object.
     */
    public ErrorObject()
    {
        // Default Constructor
    }

    /**
     * Constructs a ErrorObject object
     * copying the Error information of another object.
     * 
     * @param other the source error information
     */
    public ErrorObject(ErrorInfo other)
    {
        error(other);
    }
    
    /**
     * getErrorInfo
     */
    protected ObjectErrorInfo getErrorInfo(boolean create)
    {
        WeakHashMap<ErrorObject, ObjectErrorInfo> map = errorMap.get();
        if (map==null)
        {   map = new WeakHashMap<ErrorObject, ObjectErrorInfo>();
            errorMap.set(map);
        }
        ObjectErrorInfo info = errorMap.get().get(this);
        if (info==null && create)
        {   info = new ObjectErrorInfo();
            map.put(this, info);
        }
        return info;
    }
    
    /**
     * Clears the ErrorInfo.
     */
    protected void clearErrorInfo()
    {
        WeakHashMap<ErrorObject, ObjectErrorInfo> map = errorMap.get();
        if (map!=null)
            map.remove(this);
    }

    /**
     * Returns wether or not an error is set.
     * 
     * @return true if an error has been set, or otherwise false
     */
    public final boolean hasError()
    {
        ObjectErrorInfo info = getErrorInfo(false);
        return (info!=null && info.errType!=Errors.None);
    }

    /**
     * Returns the last error code.
     * 
     * @return the error code of ERR_NONE if no error has been set.
     */
    public final ErrorType getErrorType()
    {
        ObjectErrorInfo info = getErrorInfo(false);
        return ((info!=null) ? info.errType : Errors.None);
    }

    /**
     * Returns the parameters used to build the error text.
     * 
     * @return the error message or null if no error has been set.
     */
    public final String getErrorSource()
    {
        ObjectErrorInfo info = getErrorInfo(false);
        return ((info!=null) ? info.errSource : null);
    }

    /**
     * Returns the parameters used to build the error text.
     * 
     * @return the error message or null if no error has been set.
     */
    public final Object[] getErrorParams()
    {
        ObjectErrorInfo info = getErrorInfo(false);
        return ((info!=null) ? info.errParams : null);
    }
    
    /**
     * Clears the error for this object.
     */
    public final void clearError()
    {
        internalSetError(Errors.None, null, null);
    }
    
    /**
     * Returns the message associated with the last error.
     * 
     * @return the error message or an empty string if no error has been set.
     */
    public final String getErrorMessage()
    {
        return getMessage(this);
    }
    
    /**
     * Sets the last error and the corresponding error message on this object.
     * 
     * @param errType the type of error to set.
     * @param params array of parameters for the error message if any.
     * @param source object from which this error originated from.
     * @return always false except if the errType is of Type Errors.None
     */
    protected boolean internalSetError(ErrorType errType, Object[] params, ErrorInfo source)
    { 	// setError
        if (errType == Errors.None )
        {   // No Error
            clearErrorInfo();
            return true;
        }    
        // set error info
        ObjectErrorInfo info = getErrorInfo(true);
        info.errType   = errType;
        info.errParams = params;
        info.errSource =(source!=null) ? source.getClass().getName() : getClass().getName();
        // Use Exceptions
        if (exceptionsEnabled)
            throw new EmpireException(this);
        // return Error Status
        return false;
    }
    
    /** returns true */
    protected final boolean success()
    {
        return internalSetError(Errors.None, null, null);
    }

    /**
     * Sets an error from an error code an an array of parameters
     * 
     * @param errType the type of error to set.
     * @param params array of parameters for the error message if any.
     * @return always false except if the errType is of Type Errors.None
     */
    protected final boolean error(ErrorType errType, Object[] params)
    {
        // check error code
        if (errType == Errors.None)
        {	// Must supply a valid error code
            log.error("error function called with invalid error Code.");
            return true; 
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
                if (o instanceof ErrorInfo)
                    params[i] = getMessage((ErrorInfo)o);
                else 
                    params[i] = String.valueOf(o.toString());
            }
        }
        // Log Error
        try
        {
            log.info("Error '" +  MessageFormat.format(errType.getMessagePattern(), params) + "' has been set for object " + getClass().getName());
        } catch (EmpireException e)
        {
            log.error("Unable to log error message.", e);
        }
        // Set the Error
        return internalSetError(errType, params, null);
    }

    /**
     * Sets the error.
     * 
     * @return always false except if the errType is of Type Errors.None
     */
    protected final boolean error(ErrorType errType)
    {
        return error(errType, (Object[])null);
    }

    /**
     * Sets the specified error and and the message.
     * 
     * @param errType the type of error to set.
     * @param param parameter for the error message.
     * @return always false except if the errType is of Type Errors.None
     */
    protected final boolean error(ErrorType errType, Object param)
    {
        return error(errType, new Object[] { param });
    }

    /**
     * Sets the specified error and two messages.
     * 
     * @param errType the type of error to set.
     * @param param1 parameter for the error message.
     * @param param2 parameter for the error message.
     * @return always false except if the errType is of Type Errors.None
     */
    protected final boolean error(ErrorType errType, Object param1, Object param2)
    {
        return error(errType, new Object[] { param1, param2 });
    }

    /**
     * Sets the specified error and the corresponding error message on this object.
     * 
     * @param errType the type of error to set.
     * @param exptn Exception from witch the error message is copied.
     * @return always false except if the errType is of Type Errors.None
     */
    protected final boolean error(ErrorType errType, Throwable exptn)
    {
        if (exptn==null)
        {   log.warn("Cannot set exception error with param of null!");
            return true; // No Error
        }
        // Exception
        String type  = exptn.getClass().getName();
        if (type.startsWith("java.lang."))
            type = type.substring("java.lang.".length());
        // The message
        String msg   = exptn.getMessage();
        // Prepare stack trace
        StackTraceElement[] stack = exptn.getStackTrace();
        String pos = (stack!=null) ? stack[0].toString() : getClass().getName();
        // Create Error
        return error(errType, new Object[] { type, msg, pos });
    }

    /**
     * Sets the last error and the error message: ERR_EXCEPTION.
     * 
     * @param exptn Exception from witch the error message is copied.
     * @return always false except if the errType is of Type Errors.None
     */
    protected final boolean error(Throwable exptn)
    {
        return error(Errors.Exception, exptn);
    }

    /**
     * Copies the error set on another object to this object.
     * 
     * @param other the object from which to copy the error.
     * @return always false except if the errType is of Type Errors.None
     */
    protected final boolean error(ErrorInfo other)
    {   // copy other error
        return internalSetError(other.getErrorType(),
                                other.getErrorParams(), other);
    }
}