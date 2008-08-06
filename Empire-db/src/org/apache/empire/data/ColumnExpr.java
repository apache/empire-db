/*
 * ESTEAM Software GmbH, 17.09.2007
 */
package org.apache.empire.data;

import org.apache.empire.commons.Options;

/**
 * The column interface provides methods for accessing metadata that is relevant for managing
 * and displaying data available through the RecordData interface.
 * <P>
 * @author ESTEAM software <A TARGET="esteam" HREF="http://www.esteam.de">www.esteam.de </A>
 */
public interface ColumnExpr
{

    /**
     * Returns the column's data type.
     * @see DataType
     * @return the column's data type
     */
    DataType getDataType();

    /**
     * Returns the physical column name.
     * @return the physical column name
     */
    String getName();

    /**
     * Returns the column's display title.
     * @return the column's display title
     */
    String getTitle();

    /**
     * Returns the column's control type used for displaying and entering data.
     * @return the column's control type used for displaying and entering data
     */
    String getControlType();

    /**
     * Returns futher metadata attributes.
     * @param name the name of the attribute
     * @return futher metadata attributes
     */
    Object getAttribute(String name);

    /**
     * Returns an option set with possible column values and their
     * corresponding display text.
     * @return option set with possible column values and their corresponding display text
     */
    Options getOptions();

    /**
     * Returns the name of a Java bean property to which this column is mapped.
     * @return the name of a Java bean property to which this column is mapped
     */
    String getBeanPropertyName();

    /**
     * Returns the underlying source column (if any).
     * If an expression is based not based on a particutlar column this function returns null.
     * @return the column on which this expression is based or null if not applicable.
     */
    Column getSourceColumn();

}
