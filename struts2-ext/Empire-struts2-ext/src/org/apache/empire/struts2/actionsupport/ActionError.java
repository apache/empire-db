/*
 * ESTEAM Software GmbH, 21.07.2007
 */
package org.apache.empire.struts2.actionsupport;

import org.apache.empire.commons.ErrorInfo;
import org.apache.empire.commons.ErrorObject;
import org.apache.empire.commons.ErrorType;
import org.apache.empire.commons.Errors;

public class ActionError extends ErrorObject
{
    private ObjectErrorInfo errorInfo = null;

    @Override
    protected ObjectErrorInfo getErrorInfo(boolean create)
    {
        if (errorInfo==null && create)
            errorInfo = new ObjectErrorInfo();
        return errorInfo;
    }

    @Override
    protected void clearErrorInfo()
    {
        errorInfo = null;
    }

    /**
     * Private constructor to prevent inheritance.
     * Other classes should not be derived from Action Error
     * since this might be stored on the session.
     * Instead derive your error objects from ErrorObject
     */
    @SuppressWarnings("unused")
    private ActionError()
    {  
        // Default Constructor
    }

    /**
     * @see ErrorObject#error(ErrorInfo)
     */
    public ActionError(ErrorInfo other)
    {
        // copy other error
        error(other);
    }

    /**
     * @see ErrorObject#error(ErrorType, Object[])
     */
    public ActionError(ErrorType errType, Object[] params)
    {   // Set the Error
        error(errType, params);
    }

    /**
     * @see ErrorObject#error(ErrorType)
     */
    public ActionError(ErrorType errType)
    {
        error(errType);
    }

    /**
     * @see ErrorObject#error(ErrorType, Object)
     */
    public ActionError(ErrorType errType, Object param)
    {
        error(errType, param);
    }

    /**
     * @see ErrorObject#error(ErrorType, Object, Object)
     */
    public ActionError(ErrorType errType, Object param1, Object param2)
    {
        error(errType, param1, param2);
    }

    /**
     * @see ErrorObject#error(ErrorType, Throwable)
     */
    public ActionError(ErrorType errType, Exception exptn)
    {
        error(errType,  exptn);
    }

    /**
     * @see ErrorObject#error(Throwable)
     */
    public ActionError(Throwable exptn)
    {
        error(Errors.Exception, exptn);
    }
    
    /**
     * @return the error message for this Action Error
     */
    @Override
    public String toString()
    {
        return (hasError() ? getErrorMessage() : "");
    }
}
