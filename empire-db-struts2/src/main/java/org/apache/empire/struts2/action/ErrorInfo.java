/*
 * ESTEAM Software GmbH, 04.08.2011
 */
package org.apache.empire.struts2.action;

import org.apache.empire.commons.ErrorType;

public interface ErrorInfo
{
    /**
     * Returns the last error code.
     * 
     * @return the error code of ERR_NONE if no error has been set.
     */
    ErrorType getErrorType();

    /**
     * Returns the parameters used to build the error text.
     * 
     * @return the error message or null if no error has been set.
     */
    String[] getErrorParams();

    /**
     * Returns the message associated with the last error.
     *
     * @return the error message or an empty string if no error has been set.
     */
    String getErrorMessage();

}
