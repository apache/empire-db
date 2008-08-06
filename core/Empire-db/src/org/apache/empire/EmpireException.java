/*
 * ESTEAM Software GmbH, 26.12.2007
 */
package org.apache.empire;

import org.apache.empire.commons.ErrorObject;
import org.apache.empire.commons.ErrorType;

/**
 * This exception type is used for all empire errors.<br>
 * Exceptions will only be thrown if exceptions are enabled in the ErrorObject.<BR>
 * @see ErrorObject#setExceptionsEnabled(boolean)<BR>
 * <P>
 * @param errorObject the object that caused the error
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
     * The type of error that occurred<BR>
     * @see org.apache.empire.commons.Errors<BR>
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
