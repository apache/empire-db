/*
 * ESTEAM Software GmbH, 16.08.2011
 */
package org.apache.empire.struts2.exceptions;

import org.apache.empire.commons.ErrorType;
import org.apache.empire.exceptions.EmpireException;

public abstract class WebException extends EmpireException
{
    /**
     * Comment for <code>serialVersionUID</code>
     */
    private static final long serialVersionUID = 1L;

    /**
     * Comment for <code>serialVersionUID</code>
     */
    /**
     * Constructor for derived classes
     * @param errType
     * @param params
     * @param cause
     */
    protected WebException(final ErrorType errType, final Object[] params)
    {
        super(errType, params, null);
    }
    
}
