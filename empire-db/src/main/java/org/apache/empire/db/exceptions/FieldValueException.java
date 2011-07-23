/*
 * ESTEAM Software GmbH, 23.07.2011
 */
package org.apache.empire.db.exceptions;

import org.apache.empire.commons.ErrorType;
import org.apache.empire.db.DBColumn;
import org.apache.empire.exceptions.EmpireException;

public abstract class FieldValueException extends EmpireException
{
    /**
     * Comment for <code>serialVersionUID</code>
     */
    private static final long serialVersionUID = 1L;
    
    private transient final DBColumn column;

    protected FieldValueException(final DBColumn column, final ErrorType errType, final Object[] params, final Throwable cause)
    {
        super(errType, params, cause);
        // save type and params for custom message formatting
        this.column = column;
    }
    
    public DBColumn getColumn()
    {
        return column;
    }

}
