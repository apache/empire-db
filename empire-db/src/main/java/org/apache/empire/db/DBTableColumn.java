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
package org.apache.empire.db;

import java.sql.Connection;

import org.apache.empire.commons.Attributes;
import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.commons.OptionEntry;
import org.apache.empire.commons.Options;
import org.apache.empire.data.Column;
import org.apache.empire.data.DataType;
import org.apache.empire.exceptions.InvalidArgumentException;
import org.apache.empire.exceptions.InvalidPropertyException;
import org.apache.empire.exceptions.NotSupportedException;
import org.apache.empire.exceptions.PropertyReadOnlyException;
import org.apache.empire.xml.XMLUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;


/**
 * This class represent one column of a table.
 * It contains all properties of this columns (e.g. the column width).
 * 
 *
 */
public class DBTableColumn extends DBColumn
{
    // *Deprecated* private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(DBTableColumn.class);
    
    // Additional Attributes
    public static final String DBCOLATTR_SINGLEBYTECHARS  = "singleByteChars";
    
    // Column Information
    protected DataType  type;
    protected double    size;
    protected boolean   required;
    protected boolean   autoGenerated;
    protected boolean   readOnly;
    protected Object    defaultValue;
    protected int 		decimalScale = 0;

    /**
     * Constructs a DBTableColumn object set the specified parameters to this object.
     * 
     * @param table the table object to add the column to, set to null if you don't want it added to a table
     * @param type the type of the column e.g. integer, text, date
     * @param name the column name
     * @param size the column width
     * @param required flag whether the column is required 
     * @param autoGenerated flag whether the column is auto generated 
     * @param defValue the object value
     */
    public DBTableColumn(DBTable table, DataType type, String name, double size, boolean required, boolean autoGenerated, Object defValue)
    {
        super(table, name);
        // check properties
        // set column properties
        this.type = type;
        this.required = required;
        this.autoGenerated = autoGenerated;
        this.readOnly = autoGenerated;
        this.defaultValue = defValue;
        // xml
        this.attributes = new Attributes();
        this.options = null;
        // size (after attributes!)
        setSize(size);
    }
    
    /**
     * Clone Constructor
     * @param newTable the cloned table
     * @param other the column to clone
     */
    protected DBTableColumn(DBTable newTable, DBTableColumn other)
    {
        super(newTable, other.name);
        // Copy
        this.type = other.type;
        this.size = other.size;
        this.required = other.required;
        this.autoGenerated = other.autoGenerated;
        this.readOnly = other.readOnly;
        this.defaultValue = other.defaultValue;
        this.attributes = new Attributes();
        this.attributes.addAll(other.attributes);
        this.options = other.options;
        if (newTable != null)
        {
            newTable.addColumn(this);
        }
    }
    
    /**
     * Returns the default column value.
     * For columns of type DBDataType.AUTOINC this is assumed to be the name of a sequence
     * 
     * @return the default column value
     */
    public Object getDefaultValue()
    {
        return defaultValue;
    }

    /**
     * Sets the default column value.
     * 
     * @param defValue the default column value
     */
    public void setDefaultValue(Object defValue)
    {
        this.defaultValue = defValue;
    }
    
    /**
     * Returns the default column value. 
     * Unlike getDefaultValue this function is used when creating or adding records.
     * If the column value is DBDataType AUTOIN this function will return a new sequence value for this record
     * 
     * @param conn a valid database connection
     * @return the default column value
     */
    public Object getRecordDefaultValue(Connection conn)
    {	// Check params   
        if (rowset==null)
            return defaultValue;
        // Detect default value
        if (isAutoGenerated())
        {   // If no connection is supplied defer till later
        	if (conn==null)
        		return null; // Create Later
            // Other auto-generated values
	        DBDatabase db = rowset.getDatabase();
            return db.getDbms().getColumnAutoValue(db, this, conn);
        }
        // Normal value
        return defaultValue;
    }

    /**
     * Returns the data type.
     * 
     * @return the data type
     */
    @Override
    public DataType getDataType()
    {
        return type;
    }

    /**
     * Gets the the column width.
     * 
     * @return the column width
     */
    @Override
    public double getSize()
    {
        return size;
    }

    /**
     * Changes the size of the table column<BR>
     * Use for dynamic data model changes only.<BR>
     * @param size the new column size
     */
    public void setSize(double size)
    {
        // Negative size?
        if (size<0)
        {   // For Text-Columns set attribute "SINGLEBYTECHARS"
            if (getDataType().isText())
            {
                setAttribute(DBCOLATTR_SINGLEBYTECHARS, Boolean.TRUE);
            }    
            else
                throw new InvalidArgumentException("size", size);
            // Remove sign
            size = Math.abs(size);
        }
        else if (attributes!=null && attributes.contains(DBCOLATTR_SINGLEBYTECHARS))
        {   // Remove single by chars attribute
            attributes.remove(DBCOLATTR_SINGLEBYTECHARS);
        }
        // set now
        this.size = size;
        // set scale
    	if (getDataType()==DataType.DECIMAL)
    	{	// set scale from size
		    int reqPrec = (int)size;
		    this.decimalScale = ((int)(size*10)-(reqPrec*10));
    	}
    }
    
    /**
     * Returns the scale of the Decimal or 0 if the DataType is not DataType.DECIMAL.
     * @return the decimal scale
     */
    public int getDecimalScale()
    {
	    return this.decimalScale;
    }
    
    /**
     * Sets the scale of a decimal. The DataType must be set to DataType.DECIMAL otherwise an exception is thrown.
     * @param scale the number of fraction digits 
     */
    public void setDecimalScale(int scale)
    {
    	if (getDataType()!=DataType.DECIMAL)
    		throw new NotSupportedException(this, "setDecimalScale");
    	// return scale
	    this.decimalScale = scale;
    }

    /**
     * Returns true if column is mandatory. Only for the graphic presentation.
     * 
     * @return true if column is mandatory 
     */
    @Override
    public boolean isRequired()
    {
        return this.required;
    }
    
    /**
     * Returns true if column is a numeric sequence or otherwise generated value
     * 
     * @return true if column is auto increment
     */
    @Override
    public boolean isAutoGenerated()
    {
        return this.autoGenerated;
    }
    
    /**
     * Returns true if column the column is a single byte text or character column or false otherwise
     * 
     * @return true if column is a single byte character based column
     */
    public boolean isSingleByteChars()
    {
        if (attributes==null || !attributes.contains(DBCOLATTR_SINGLEBYTECHARS))
            return false;
        // Check Attribute
        return ObjectUtils.getBoolean(attributes.get(DBCOLATTR_SINGLEBYTECHARS));
    }
    
    /**
     * sets whether this column is a single byte character or text column
     * @param singleByteChars flag whether single byte chars should be used
     */
    public void setSingleByteChars(boolean singleByteChars)
    {
        if (!getDataType().isText())
            throw new NotSupportedException(this, "setSingleByteChars");
        // set single byte
        setAttribute(DBCOLATTR_SINGLEBYTECHARS, singleByteChars);
    }

    /**
     * Changes the required property of the table column<BR>
     * Use for dynamic data model changes only.<BR>
     * @param required true if the column is required or false otherwise
     */
    public void setRequired(boolean required)
    {
        if (isAutoGenerated())
        {	// cannot change auto-generated columns
            throw new PropertyReadOnlyException("required"); 
        }
        else
        {	// Set DataMode
        	this.required = required;
        }
    }

    /**
     * Checks whether the column is read only.
     * 
     * @return true if the column is read only
     */
    @Override
    public boolean isReadOnly()
    {   
        return this.readOnly;
    }

    /**
     * Sets the read only attribute of the column.
     *
     * @param readOnly true if the column should be read only or false otherwise
     */
    public void setReadOnly(boolean readOnly)
    {
        this.readOnly = readOnly;
    }
    
    /**
     * sets the options from an enum class
     * @param enumType the enum type
     */
    public void setEnumOptions(Class<? extends Enum<?>> enumType)
    {
        // Enum special treatment
        log.debug("Adding enum options of type {} for column {}.", enumType.getName(), getName());            
        this.options = new Options(enumType);
        // set enumType
        setAttribute(Column.COLATTR_ENUMTYPE, enumType);
        // check length
        if (getDataType().isNumeric())
            return; // no check required
        int maxLength = (int)size;
        for (OptionEntry oe : options)
        {   // check length
            String val = oe.getValueString();
            if (val!=null && val.length()>maxLength)
                throw new InvalidPropertyException(enumType.getName(), val);
        }
    }

    /**
     * Checks whether the supplied value is valid for this column.
     * If the type of the value supplied does not match the columns
     * data type the value will be checked for compatibility. 
     * 
     * @param value the checked to check for validity
     * @return true if the value is valid or false otherwise.
     */
    @Override
    public Object validateValue(Object value)
    {
        return ((DBTable)rowset).validateValue(this, value);
    }

    /**
     * Creates a foreign key relation for this column.
     * 
     * @param target the referenced primary key column
     * @return the reference object
     */
    public DBRelation.DBReference referenceOn(DBTableColumn target)
    {
        return new DBRelation.DBReference(this, target);
    }

    /**
     * Sets field elements, default attributes and all options to
     * the specified Element object (XML tag).
     * 
     * @param parent the parent object
     * @param flags a long value
     * @return the work up Element object
     */
    @Override
    public Element addXml(Element parent, long flags)
    { // Add Field element
        Element elem = XMLUtil.addElement(parent, "column");
        elem.setAttribute("name", name);
        // set default attributes
        DBIndex primaryKey = ((DBTable) rowset).getPrimaryKey();
        if (primaryKey != null)
        {
            int keyIndex;
            if ((keyIndex = ((DBTable) rowset).getPrimaryKey().getColumnPos(this)) >= 0)
                elem.setAttribute("key", String.valueOf(keyIndex + 1));
        }
        if (size > 0)
        {
            elem.setAttribute("size", String.valueOf((int)size));
            if (getDataType()==DataType.DECIMAL)
                elem.setAttribute("decimals", String.valueOf((int)(size*10)%10));
        }
        if (isRequired())
            elem.setAttribute("mandatory", String.valueOf(Boolean.TRUE));
        // add All Attributes
        if (attributes!=null)
            attributes.addXml(elem, flags);
        // add All Options
        if (options!=null)
            options.addXml(elem, this.type);
        // done
        return elem;
    }
}