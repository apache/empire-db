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

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.util.Collection;
import java.util.List;

import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.beanutils.PropertyUtilsBean;
import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.commons.Options;
import org.apache.empire.data.Column;
import org.apache.empire.data.ColumnExpr;
import org.apache.empire.data.Record;
import org.apache.empire.db.exceptions.FieldIsReadOnlyException;
import org.apache.empire.exceptions.BeanPropertyGetException;
import org.apache.empire.exceptions.InvalidArgumentException;
import org.apache.empire.exceptions.ObjectNotValidException;
import org.apache.empire.xml.XMLUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;


/**
 * 
 * This class handles one record from a database table. 
 *
 */
public class DBRecord extends DBRecordData implements Record, Cloneable
{
    private final static long serialVersionUID = 1L;
  
    /* Record state enum */
    public enum State
    {
    	Invalid,
    /*	Empty,  not used! */
    	Valid,
    	Modified,
    	New;

    	/* Accessors */
    	boolean isLess(State other)
    	{
    		return this.ordinal()<other.ordinal();
    	}
    	boolean isEqualOrMore(State other)
    	{
    		return this.ordinal()>=other.ordinal();
    	}
    }

    protected static final Logger log    = LoggerFactory.getLogger(DBRecord.class);

    // This is the record data
    private State           state;
    private DBRowSet        rowset;
    private Object[]        fields;
    private boolean[]       modified;
    // Special Rowset Data (usually null)
    private Object          rowsetData;

    /**
     * Create a new DBRecord object.<BR>
     * The record is not attached to a RowSet and the record's state is initially set to REC_INVALID.
     * 
     * Please derive your own Objects from this class.   
     */
    public DBRecord()
    {
        state = State.Invalid;
        rowset = null;
        fields = null;
        modified = null;
        rowsetData = null;
    }
    
    /**
     * This method is used internally by the RowSet to initialize the record's properties
     * @param rowset the rowset to which to attach this record
     * @param state the state of the record 
     * @param rowSetData any further RowSet specific data
     */
    protected void init(DBRowSet rowset, State state, Object rowSetData)
    {
        // Init
        if (this.rowset != rowset)
        {   // Set Rowset
            this.rowset = rowset;
            if (rowset!=null)
                fields = new Object[rowset.getColumns().size()];
            else
                fields = null;
            onRowSetChanged();
        }
        else if (fields!=null)
        {   // clear fields
            for (int i=0; i<fields.length; i++)
                fields[i]=null;
        }
        // Set State
        changeState(state, rowSetData);
    }
    
    /**
     * This function provides direct access to the record fields.<BR>
     * This method is used internally be the RowSet to fill the data.<BR>
     * @return an array of field values
     */
    protected Object[] getFields()
    {
        return fields;
    }
    
    /**
     * This method is used internally be the RowSet to change the record's state<BR>
     * @param state
     * @param rowSetData
     */
    protected void changeState(State state, Object rowSetData)
    {
        this.state = state;
        this.rowsetData = rowSetData;
        this.modified = null;
    }

    /**
     * Closes the record by releasing all resources and resetting the record's state to invalid.
     */
    @Override
    public void close()
    {
        init(null, State.Invalid, null);
    }
    
    /** {@inheritDoc} */
    @Override
    public DBRecord clone()
    {
        try 
        {
            DBRecord rec = (DBRecord)super.clone();
            rec.rowset = rowset;
            rec.state = state;
            if (rec.fields == fields && fields!=null)
                rec.fields = fields.clone();
            if (rec.modified == modified && modified!=null)
                rec.modified = modified.clone();
            rec.rowsetData = rowsetData;
            return rec;
            
        } catch (CloneNotSupportedException e)
        {
            log.error("Unable to clone record.", e);
            return null;
        }
    }

    /**
     * Returns the current DBDatabase object.
     * 
     * @return the current DBDatabase object
     */
    @Override
    public DBDatabase getDatabase()
    {
        return (rowset != null) ? rowset.db : null;
    }

    /**
     * Returns the DBRowSet object.
     * 
     * @return the DBRowSet object
     */
    public DBRowSet getRowSet()
    {
        return rowset;
    }

    /**
     * Returns the DBRowSet object.
     * 
     * @return the DBRowSet object
     */
    public Object getRowSetData()
    {
        return rowsetData;
    }

    /**
     * Returns the record state.
     * 
     * @return the record state
     */
    public State getState()
    {
        return state;
    }

    /**
     * Returns true if the record is valid.
     * 
     * @return true if the record is valid
     */
    public boolean isValid()
    {
        return (state != State.Invalid);
    }

    /**
     * Returns true if the record is valid.
     * 
     * @return true if the record is valid
     */
    public boolean isReadOnly()
    {
        if (!isValid())
            return true;
        DBRowSet rowset = getRowSet();
        return (rowset==null || !rowset.isUpdateable());
    }

    /**
     * Returns true if the record is modified.
     * 
     * @return true if the record is modified
     */
    public boolean isModified()
    {
        return (state.isEqualOrMore(State.Modified));
    }

    /**
     * Returns true if this record is a new record.
     * 
     * @return true if this record is a new record
     */
    public boolean isNew()
    {
        return (state == State.New);
    }

    /**
     * Returns the number of the columns.
     * 
     * @return the number of the columns
     */
    @Override
    public int getFieldCount()
    {
        return (fields != null) ? fields.length : 0;
    }

    /**
     * Returns the index value by a specified DBColumnExpr object.
     * 
     * @return the index value
     */
    @Override
    public int getFieldIndex(ColumnExpr column)
    {
        DBColumnExpr expr = (DBColumnExpr)column;
        return (rowset != null) ? rowset.getColumnIndex(expr.getUpdateColumn()) : -1;
    }

    /**
     * Returns the index value by a specified column name.
     * 
     * @return the index value
     */
    @Override
    public int getFieldIndex(String column)
    {
        if (rowset != null)
        {
            List<DBColumn> columns = rowset.getColumns();
            for (int i = 0; i < columns.size(); i++)
            {
                DBColumn col = columns.get(i);
                if (col.getName().equalsIgnoreCase(column))
                    return i;
            }
        }
        // not found
        return -1;
    }

    /**
     * Returns the DBColumn for the field at the given index.
     * 
     * @param index the field index 
     * 
     * @return the index value
     */
    public DBColumn getDBColumn(int index)
    {
        return (rowset!=null ? rowset.getColumn(index) : null);
    }

    /**
     * Implements the Record Interface getColumn method.<BR>
     * Internally calls getDBColumn()
     * @return the Column at the specified index 
     */
    public final Column getColumn(int index)
    {
        return getDBColumn(index);
    }
    
    /**
     * Returns a DBColumnExpr object by a specified index value.
     * @return the index value
     */
    @Override
    public final ColumnExpr getColumnExpr(int index)
    {
        return getDBColumn(index);
    }
    
    /**
     * Returns true if the field was modified.
     * 
     * @param index the field index
     *  
     * @return true if the field was modified
     */
    public boolean wasModified(int index)
    {
        if (!isValid())
            throw new ObjectNotValidException(this);
        if (index < 0 || index >= fields.length)
            throw new InvalidArgumentException("index", index);
        // Check modified
        if (modified == null)
            return false;
        return modified[index];
    }

    /**
     * Returns true if the field was modified.
     * 
     * @return true if the field was modified
     */
    public final boolean wasModified(Column column)
    {
        return wasModified(getFieldIndex(column));
    }

    /**
     * Sets the modified state of a column.<BR>
	 * This will force the field to be updated in the database, if set to TRUE.
	 * 
     * @param column the column
     * @param isModified modified or not
     */
    public void setModified(DBColumn column, boolean isModified)
    {	// Check valid
        if (state == State.Invalid)
            throw new ObjectNotValidException(this);
        // Check modified
        if (modified == null)
        { 	// Init array
            modified = new boolean[fields.length];
            for (int j = 0; j < fields.length; j++)
                modified[j] = false;
        }
        int index = getFieldIndex(column);
        if (index >= 0)
            modified[index] = isModified;
        // Set State to modified, if not already at least modified and isModified is set to true
        if (state.isLess(State.Modified) && isModified)
            state = State.Modified;
        // Reset state to unmodified, if currently modified and not modified anymore after the change
        if (state == State.Modified && !isModified)
        {
        	boolean recordNotModified = true;
            for (int j = 0; j < fields.length; j++)
            {
                if (modified[j] == true)
                {
                	recordNotModified = false;
                }
            }
            if (recordNotModified)
            {
            	state = State.Valid;
            }
        }
    }
    
    /**
     * returns an array of key columns which uniquely identify the record.
     * @return the array of key columns if any
     */
    public Column[] getKeyColumns()
    {
        return rowset.getKeyColumns();
    }

    /**
     * Returns the array of primary key columns.
     * @return the array of primary key columns
     */
    public Object[] getKeyValues()
    {
        return ((rowset != null) ? rowset.getRecordKey(this) : null);
    }

    /**
     * Returns the value for the given column or null if either the index is out of range or the value is not valid (see {@link DBRecord#isValueValid(int)})
     * @return the index value
     */
    @Override
    public Object getValue(int index)
    {   // Check state
        if (fields == null)
            throw new ObjectNotValidException(this);
        // Check index
        if (index < 0 || index>= fields.length)
            throw new InvalidArgumentException("index", index);
        // Special check for NO_VALUE 
        if (fields[index] == ObjectUtils.NO_VALUE)
            return null;
        // Return field value
        return fields[index];
    }
    
    /**
     * Returns whether a field value is provided i.e. the value is not DBRowSet.NO_VALUE<BR>
     * This function is only useful in cases where records are partially loaded.<BR>
     * 
     * @param index the filed index
     *  
     * @return true if a valid value is supplied for the given field or false if value is {@link ObjectUtils#NO_VALUE}  
     */
    public boolean isValueValid(int index)
    {   // Check state
        if (fields == null)
            throw new ObjectNotValidException(this);
        // Check index
        if (index < 0 || index>= fields.length)
        {   // Index out of range
            throw new InvalidArgumentException("index", index);
        }
        // Special check for NO_VALUE
        return (fields[index] != ObjectUtils.NO_VALUE);
    }

    /**
     * Gets the possbile Options for a field in the context of the current record.
     * 
     * @param column the database field column
     *  
     * @return the field options 
     */
    public Options getFieldOptions(DBColumn column)
    {
        // DBColumn col = ((colexpr instanceof DBColumn) ? ((DBColumn) colexpr) : colexpr.getUpdateColumn());
        return column.getOptions();
    }

    /**
     * Gets the possbile Options for a field in the context of the current record.<BR>
     * Same as getFieldOptions(DBColumn)
     * @return the Option
     */
    public final Options getFieldOptions(Column column)
    {
        return getFieldOptions((DBColumn)column); 
    }

    /**
     * Modifies a column value bypassing all checks made by setValue.
     * Use this to explicitly set invalid values i.e. for temporary storage.
     * 
     * @param i index of the column
     * @param value the column value
     */
    public void modifyValue(int i, Object value)
    {	// Check valid
        if (state == State.Invalid)
            throw new ObjectNotValidException(this);
        // Init original values
        if (modified == null)
        { // Save all original values
            modified = new boolean[fields.length];
            for (int j = 0; j < fields.length; j++)
                modified[j] = false;
        }
        // Set Modified
        if (fields[i]!=ObjectUtils.NO_VALUE || value!=null)
            modified[i] = true;
        // Set Value
        fields[i] = value;
        // Set State
        if (state.isLess(State.Modified))
            state = State.Modified;
        // field changed
        onFieldChanged(i);
    }

    /**
     * Sets the value of the column in the record.
     * The functions checks if the column and the value are valid and whether the
     * value has changed.
     * 
     * @param index the index of the column
     * @param value the value
     */
    public void setValue(int index, Object value)
    {
        if (state == State.Invalid)
            throw new ObjectNotValidException(this);
        if (index < 0 || index >= fields.length)
            throw new InvalidArgumentException("index", index);
        // Strings special
        if ((value instanceof String) && ((String)value).length()==0)
            value = null;
        // Has Value changed?
        if (ObjectUtils.compareEqual(fields[index], value))
            return; // no change
        // Field has changed
        DBColumn column = rowset.getColumn(index);
        if (column.isAutoGenerated())
        {   // Read Only column may be set
            throw new FieldIsReadOnlyException(column);
        }
        if (state!=State.New && rowset.isKeyColumn(column))
        {   // Key columns cannot be changed
            throw new FieldIsReadOnlyException(column);
        }
        // Is Value valid
        column.validate(value);
        // Init original values
        modifyValue(index, value);
    }

    /**
     * Sets the value of the column in the record.
     * The functions checks if the column and the value are valid and whether the
     * value has changed.
     * 
     * @param column a DBColumn object
     * @param value the value
     */
    public final void setValue(Column column, Object value)
    {
        if (rowset == null)
            throw new ObjectNotValidException(this);
        // Get Column Index
        setValue(rowset.getColumnIndex(column), value);
    }
    
    /**
     * returns whether a field is visible to the client or not
     * <P>
     * May be overridden to implement context specific logic.
     * @param column the column which to check for visibility
     * @return true if the column is visible or false if not 
     */
    public boolean isFieldVisible(Column column)
    {
        if (rowset==null)
            return false;
    	// Check value
        int index = rowset.getColumnIndex(column);
        return (index>=0 && isValueValid(index));
    }
    
    /**
     * returns whether a field is read only or not
     * 
     * @param column the database column 
     * 
     * @return true if the field is read only
     */
    public boolean isFieldReadOnly(Column column)
    {
        if (rowset==null)
            throw new ObjectNotValidException(this);
    	if (rowset.getColumnIndex(column)<0)
            throw new InvalidArgumentException("column", column);
        // Ask RowSet
        return (rowset.isColumnReadOnly((DBColumn)column));
    }
    
    /**
     * returns whether a field is required or not
     * 
     * @param column the database column 
     * 
     * @return true if the field is required
     */
    public boolean isFieldRequired(Column column)
    {
        if (rowset==null)
            throw new ObjectNotValidException(this);
    	if (rowset.getColumnIndex(column)<0)
            throw new InvalidArgumentException("column", column);
        // from column definition
        return (column.isRequired());
    }

    /**
     * Initializes this record object by attaching it to a rowset,
     * setting its primary key values and setting the record state.<BR>
     * This function is useful for updating a record without prior reading.
     * <P>
     * @param table the rowset
     * @param keyValues a Object array, the primary key(s)
     * @param insert if true change the state of this object to REC_NEW
     */
     
    public void init(DBRowSet table, Object[] keyValues, boolean insert)
    { // Init with keys
    	State initialState = (insert ? State.New : State.Valid);  
        table.initRecord(this, initialState, keyValues);
    }
    
    /**
     * Creates a new record for the given table.<BR>
     * All record fields will be filled with their default values.
     * The record's state is set to NEW
     * <P>
     * If a connection is supplied sequence generated values will be obtained<BR>
     * Otherwise the sequence will be generated later. 
     * <P>
     * @param table the table for which to create a record
     * @param conn a valid JDBC connection
     */
    public void create(DBRowSet table, Connection conn)
    {
        table.createRecord(this, conn);
    }
    
    /**
     * Creates a new record for the given table.<BR>
     * All record fields will be filled with their default values.<BR>
     * The record's state is set to NEW
     * <P>
     * @param table the table for which to create a record
     */
    public void create(DBRowSet table)
    {
        create(table, null);
    }

    /**
     * Loads a record from the database identified by it's primary key. 
     * After successful reading the record will be valid and all values will be accessible.
     * @see org.apache.empire.db.DBTable#readRecord(DBRecord, Object[], Connection)
     * 
     * @param table the rowset from which to read the record
     * @param keys an array of the primary key values
     * @param conn a valid connection to the database.
     * @return true if the record was successfully loaded or false if the record was not found or an error occurred.
     */
    public void read(DBRowSet table, Object[] keys, Connection conn)
    {
        table.readRecord(this, keys, conn);
    }

    /**
     * Loads a record from the database identified by it's primary key. 
     * After successful reading the record will be valid and all values will be accessible.
     * @see org.apache.empire.db.DBTable#readRecord(DBRecord, Object[], Connection)
     * 
     * @param table the rowset from which to read the record
     * @param id the primary key of the record to load.
     * @param conn a valid connection to the database.
     * @return true if the record was successfully loaded or false if the record was not found or an error occurred.
     */
    public final void read(DBRowSet table, Object id, Connection conn)
    {
        if (id instanceof Collection<?>)
        {   // If it's a collection then convert it to an array
            read(table, ((Collection<?>)id).toArray(), conn);
        }
        // Simple One-Column key
        read(table, new Object[] { id }, conn);
    }

    /**
     * Updates the record and saves all changes in the database.
     * 
     * @see org.apache.empire.db.DBTable#updateRecord(DBRecord, Connection)
     * @param conn a valid connection to the database.
     */
    public void update(Connection conn)
    {
        if (rowset == null)
            throw new ObjectNotValidException(this);
        // update
        rowset.updateRecord(this, conn);
    }

    /**
     * This helper function calls the DBRowset.deleteRecord method 
     * to delete the record.
     * 
     * WARING: There is no guarantee that it ist called
     * Implement delete logic in the table's deleteRecord method if possible
     * 
     * @see org.apache.empire.db.DBTable#deleteRecord(Object[], Connection)
     * @param conn a valid connection to the database.
     */
    public void delete(Connection conn)
    {
        if (isValid()==false)
            throw new ObjectNotValidException(this);
        // Delete only if record is not new
        if (!isNew())
        {
            Object[] keys = rowset.getRecordKey(this);
            rowset.deleteRecord(keys, conn);
        }
        close();
    }

    /**
     * This function set the field descriptions to the the XML tag.
     * 
     * @return the number of column descriptions added to the element
     */
    @Override
    public int addColumnDesc(Element parent)
    {
        if (rowset == null)
            throw new ObjectNotValidException(this);
        // Add Field Description
        int count = 0;
        List<DBColumn> columns = rowset.getColumns();
        for (int i = 0; i < columns.size(); i++)
        { // Add Field
            DBColumn column = columns.get(i);
            if (isFieldVisible(column)==false)
                continue;
            column.addXml(parent, 0);
            count++;
        }
        return count;
    }

    /**
     * Add the values of this record to the specified XML Element object.
     * 
     * @param parent the XML Element object
     * @return the number of row values added to the element
     */
    @Override
    public int addRowValues(Element parent)
    {
        if (rowset == null)
            throw new ObjectNotValidException(this);
        // set row key
        DBColumn[] keyColumns = rowset.getKeyColumns();
        if (keyColumns != null && keyColumns.length > 0)
        { // key exits
            if (keyColumns.length > 1)
            { // multi-Column-id
                StringBuilder buf = new StringBuilder();
                for (int i = 0; i < keyColumns.length; i++)
                { // add
                    if (i > 0)
                        buf.append("/");
                    buf.append(getString(keyColumns[i]));
                }
                parent.setAttribute("id", buf.toString());
            } 
            else
                parent.setAttribute("id", getString(keyColumns[0]));
        }
        // row attributes
        if (isNew())
            parent.setAttribute("new", "1");
        // Add all children
        int count = 0;
        List<DBColumn> columns = rowset.getColumns();
        for (int i = 0; i < fields.length; i++)
        { // Read all
            DBColumn column = columns.get(i);
            if (isFieldVisible(column)==false)
                continue;
            // Add Field Value
            String name = column.getName();
            if (fields[i] != null)
                XMLUtil.addElement(parent, name, getString(i));
            else
                XMLUtil.addElement(parent, name).setAttribute("null", "yes"); // Null-Value
            // increase count
            count++;
        }
        return count;
    }
    
    /**
     * returns the DBXmlDictionary that should used to generate XMLDocuments<BR>
     * @return the DBXmlDictionary
     */
    protected DBXmlDictionary getXmlDictionary()
    {
        return DBXmlDictionary.getInstance();
    }

    /**
     * Returns a XML document with the field description an values of this record.
     * 
     * @return the new XML Document object
     */
    @Override
    public Document getXmlDocument()
    {
        if (rowset == null)
            throw new ObjectNotValidException(this);
        // Create Document
        DBXmlDictionary xmlDic = getXmlDictionary();
        Element root = XMLUtil.createDocument(xmlDic.getRowSetElementName());
        if (rowset.getName() != null)
            root.setAttribute("name", rowset.getName());
        // Add Field Description
        if (addColumnDesc(root)>0)
        {   // Add row Values
            addRowValues(XMLUtil.addElement(root, xmlDic.getRowElementName()));
        }
        // return Document
        return root.getOwnerDocument();
    }

    /**
     * Set the record default value for the fields with 
     * the value {@link ObjectUtils#NO_VALUE}
     * 
     * @param conn the sql connection
     *  
     * @return the number of fields set to default
     */
    public int fillMissingDefaults(Connection conn)
    {
        int count = 0;
        for (int i = 0; i < fields.length; i++)
        {
            if (fields[i] == ObjectUtils.NO_VALUE)
            {
                DBTableColumn col = (DBTableColumn) rowset.getColumn(i);
                Object value = col.getRecordDefaultValue(conn);
                if (value==null)
                    continue;
                // Modify value
                modifyValue(i, value);
                count++;
            }
        }
        return count;
    }
    

    /**
     * set a record value from a particular bean property.
     * <P>
     * For a property called FOO this is equivalent of calling<BR>
     *     setValue(column, bean.getFOO())
     * <P>
     * @param bean the Java Bean from which to read the value from
     * @param property the name of the property
     * @param column the column for which to set the record value
     * @return true if the value has been successfully set or false if not    
     */
    protected void setBeanValue(Object bean, String property, Column column)
    {
        try
        {   /*
            if (log.isTraceEnabled())
                log.trace(bean.getClass().getName() + ": getting property '" + property + "' for column " + column.getName());
            */
            
            // Get Property Value
            PropertyUtilsBean pub = BeanUtilsBean.getInstance().getPropertyUtils();
            Object value = pub.getSimpleProperty(bean, property);

            // Now, set the record value
            setValue( column, value ); 

        } catch (IllegalAccessException e)
        {   log.error(bean.getClass().getName() + ": unable to get property '" + property + "'");
            throw new BeanPropertyGetException(bean, property, e);
        } catch (InvocationTargetException e)
        {   log.error(bean.getClass().getName() + ": unable to get property '" + property + "'");
            throw new BeanPropertyGetException(bean, property, e);
        } catch (NoSuchMethodException e)
        {   log.warn(bean.getClass().getName() + ": no getter available for property '" + property + "'");
            throw new BeanPropertyGetException(bean, property, e);
        }
    }
    
    /**
     * Sets record values from the supplied java bean.
     * 
     * @return true if at least one value has been set successfully 
     */
    public int setBeanValues(Object bean, Collection<Column> ignoreList)
    {
        // Add all Columns
        int count = 0;
        for (int i = 0; i < getFieldCount(); i++)
        { // Check Property
            DBColumn column = getDBColumn(i);
            if (column.isReadOnly())
                continue;
            if (ignoreList != null && ignoreList.contains(column))
                continue; // ignore this property
            // Get Property Name
            String property = column.getBeanPropertyName();
            setBeanValue(bean, property, column);
            count++;
        }
        return count;
    }

    /**
     * Sets record values from the suppied java bean.
     * @return true if at least one value has been set sucessfully
     */
    public final int setBeanValues(Object bean)
    {
        return setBeanValues(bean, null);
    }
    
    /**
     * Override this to do extra handling when the rowset for this record changes
     */
    protected void onRowSetChanged()
    {
        if (log.isTraceEnabled() && rowset!=null)
            log.trace("Record has been attached to rowset " + rowset.getName());
    }
    
    /**
     * Override this to do extra handling when the record changes
     */
    protected void onRecordChanged()
    {
        if (log.isTraceEnabled() && isValid())
            log.trace("Record has been changed");
    }
    
    /**
     * Override this to get notified when a field value changes
     */
    protected void onFieldChanged(int i)
    {
        if (log.isDebugEnabled())
            log.debug("Record field " + rowset.getColumn(i).getName() + " changed to " + String.valueOf(fields[i]));
    }
    
}
