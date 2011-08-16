/*
 * ESTEAM Software GmbH, 16.08.2011
 */
package org.apache.empire.struts2.exceptions;

import org.apache.empire.commons.ErrorType;

public class InvalidFormDataException extends WebException
{
    /**
     * Comment for <code>serialVersionUID</code>
     */
    private static final long serialVersionUID = 1L;
    
    public static final ErrorType errorType = new ErrorType("error.web.recordsDontMatch", "The form submitted is invalid for the current context.");
    
    public InvalidFormDataException()
    {
        super(errorType, null );
    }

}
