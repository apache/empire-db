/*
 * ESTEAM Software GmbH, 11.09.2007
 */
package org.apache.empire.data;

/**
 * The column interface provides methods for accessing metadata that is only relevant for updateing records.
 * <P>
 * This interface inherits from ColumnExpr which provides futher metadata.
 * <P>
 * @author ESTEAM software <A TARGET="esteam" HREF="http://www.esteam.de">www.esteam.de </A>
 */
public interface Column extends ColumnExpr
{

    /**
     * Returns the maximum size a value for this column is allowed to have.
     * <P>
     * For the data type DECIMAL the size defines the scale and precision of the value.
     * <P>
     * @return Returns the maximum size a value for this column is allowed to have.
     */
    double getSize();

    /**
     * Returns whether or not the value for this column must be
     * supplied (i.e. it is mandatory) or not.
     * <P>
     * @return Returns true if the value for this column must be supplied
     */
    boolean isRequired();

    /**
     * Returns true if the values for this column are generally
     * read only (like i.e. for sequence generated values).
     * <P>
     * @return Returns true if the values for this column are generally read-only
     */
    boolean isReadOnly();

}
