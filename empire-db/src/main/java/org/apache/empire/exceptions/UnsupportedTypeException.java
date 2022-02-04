package org.apache.empire.exceptions;

import org.apache.empire.commons.ErrorType;

public class UnsupportedTypeException extends EmpireException
{
    private static final long serialVersionUID = 1L;
    
    public static final ErrorType errorType = new ErrorType("error.typeNotSupported", "The type '{0}' is not supported.");
    
    public UnsupportedTypeException(Class<?> clazz)
    {
       super(errorType, new String[] { clazz.getSimpleName() } );
    }
}
