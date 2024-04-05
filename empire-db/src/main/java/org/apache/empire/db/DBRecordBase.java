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

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.beanutils.PropertyUtilsBean;
import org.apache.empire.commons.ClassUtils;
import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.commons.Options;
import org.apache.empire.commons.StringUtils;
import org.apache.empire.data.Column;
import org.apache.empire.data.ColumnExpr;
import org.apache.empire.data.EntityType;
import org.apache.empire.data.Record;
import org.apache.empire.db.context.DBRollbackHandler;
import org.apache.empire.db.exceptions.FieldReadOnlyException;
import org.apache.empire.db.exceptions.FieldValueNotFetchedException;
import org.apache.empire.db.exceptions.NoPrimaryKeyException;
import org.apache.empire.db.exceptions.RecordReadOnlyException;
import org.apache.empire.exceptions.BeanPropertyGetException;
import org.apache.empire.exceptions.InvalidArgumentException;
import org.apache.empire.exceptions.NotSupportedException;
import org.apache.empire.exceptions.ObjectNotValidException;
import org.apache.empire.xml.XMLUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * This abstract class provides write access to the fields of a record
 * 
 * The class provides methods that are useful for frontend-form development like
 *   - providing information about the allowed values for a field (field options)
 *   - providing information about whether or not a field is visible to the user    
 *   - providing information about whether or not a field is required (mandantory)    
 *   - providing information about whether or not a field is read-only    
 *   - providing information about whether a particular field value is valid    
 *   - providing information about whether a field was modified since it was read from the database
 *   - providing information about whether the record was modified
 * 
 * Also, field value changes, can be handled using the onFieldChanged event.
 */
public abstract class DBRecordBase extends DBRecordData implements Record, Cloneable, Serializable
{
    private static final long serialVersionUID = 1L;
    
    private static final Logger log  = LoggerFactory.getLogger(DBRecordBase.class);
    
    /**
     * DBRecordRollbackHandler
     * @author doebele
     */
    public static class DBRecordRollbackHandler implements DBRollbackHandler
    {
        // Logger
        private static final Logger log = LoggerFactory.getLogger(DBRecordRollbackHandler.class);
        
        public final DBRecordBase   record;
        
        private final State     state;  /* the original state */
        private Object[]        fields;
        private boolean[]       modified;
        private Object          rowsetData;
    
        public DBRecordRollbackHandler(DBRecordBase record)
        {
            this.record = record;
            // check
            if (record.state==State.Invalid)
                throw new ObjectNotValidException(record);
            // save state
            this.state = record.state;            
            this.modified   = copy(record.modified);
            this.fields     = copy(record.fields);
            this.rowsetData = copy(record.rowsetData);
        }

        @Override
        public DBObject getObject()
        {
            return record;
        }

        @Override
        public String getObjectInfo()
        {
            String info = "Record "+record.getRowSet().getName();
            if (record.getKeyColumns()==null)
                return info;
            return info+":"+StringUtils.arrayToString(record.getKey(), "|");
        }

        @Override
        public void combine(DBRollbackHandler successor)
        {
            if (record!=successor.getObject())
                throw new InvalidArgumentException("successor", successor);
            // combine now
            DBRecordRollbackHandler s = (DBRecordRollbackHandler)successor;
            log.info("combining rollback state for record {}/{}", record.getRowSet().getName(), StringUtils.arrayToString(record.getKey(), "|"));
            if (s.modified==null)
            {
                return; // not modified!
            }
            // Make sure we have a modified array 
            if (modified==null)
                modified = new boolean[fields.length];
            // special case Timestamp
            DBRowSet rowset = record.getRowSet();
            DBColumn tsColumn = record.getRowSet().getTimestampColumn();
            // copy
            for (int i=0; i<fields.length; i++)
            {   // ignore timestamp and key columns
                DBColumn column = record.getColumn(i);
                if (column==tsColumn || rowset.isKeyColumn(column))
                    continue;
                // copy modified fields
                if (s.modified[i]==false)
                    continue;
                // field was modified
                fields[i] = s.fields[i];
                if (modified!=null)
                    modified[i] = true;
            }
        }

        @Override
        public void rollback(Connection conn)
        {
            // rollback
            record.state = this.state;
            record.fields = this.fields;
            record.modified = this.modified;
            record.rowsetData = rowsetData;
            // done
            if (log.isInfoEnabled())
                log.info("Rollback for {} performed.", getObjectInfo());
        }

        @Override
        public void discard(Connection conn)
        {
            /* nothing */
        }
        
        private boolean[] copy(boolean[] other)
        {
            if (other==null)
                return null;
            boolean[] copy = new boolean[other.length];
            for (int i=0; i<copy.length; i++)
            {
                copy[i] = other[i]; 
            }
            return copy;
        }
        
        private Object[] copy(Object[] other)
        {
            if (other==null)
                return null;
            Object[] copy = new Object[other.length];
            for (int i=0; i<copy.length; i++)
            {
                copy[i] = other[i]; 
            }
            return copy;
        }
        
        protected Object copy(Object other)
        {   
            if (other==null)
                return null;
            if (other instanceof Object[])
                return copy((Object[])other);
            return ClassUtils.copy(other);
        }
    }
  
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
    
    // This is the record data
    private State           state;
    private Object[]        fields;
    private boolean[]       modified;
    Object                  rowsetData; // Special Rowset Data (usually null)
    protected boolean       validateFieldValues;
    protected boolean       allowReadOnlyUpdate;
    
    // Parent-Record-Map for deferred identity setting 
    private Map<DBColumn, DBRecordBase> parentRecordMap;
    
    /**
     * Internal constructor for DBRecord
     * May be used by derived classes to provide special behaviour
     */
    protected DBRecordBase()
    {
        // init
        this.state = State.Invalid;
        this.fields = null;
        this.modified = null;
        this.rowsetData = null;
        this.validateFieldValues = true;
        this.allowReadOnlyUpdate = false;
        this.parentRecordMap = null;
    }

    /**
     * helper to check if the object is valid
     * @throws ObjectNotValidException if the object is not valid
     */
    protected void checkValid()
    {
        if (!isValid())
            throw new ObjectNotValidException(this);
    }

    /**
     * helper to check if the object is valid
     * @throws ObjectNotValidException if the object is not valid
     */
    protected void checkValid(int fieldIndex)
    {
        if (!isValid())
            throw new ObjectNotValidException(this);
        // Check index
        if (fieldIndex < 0 || fieldIndex>= fields.length)
            throw new InvalidArgumentException("index", fieldIndex);
    }
    
    /**
     * Closes the record by releasing all resources and resetting the record's state to invalid.
     */
    public void close()
    {
        // clear fields
        fields = null;
        modified = null;
        rowsetData = null;
        // change state
        if (state!=State.Invalid)
            changeState(State.Invalid);
        // done
        onRecordChanged();
    }
    
    /** {@inheritDoc} */
    @Override
    public DBRecordBase clone()
    {
        try 
        {
            DBRecordBase rec = (DBRecordBase)super.clone();
            rec.state = this.state;
            if (rec.fields == fields && fields!=null)
                rec.fields = fields.clone();
            if (rec.modified == modified && modified!=null)
                rec.modified = modified.clone();
            rec.rowsetData = ClassUtils.copy(this.rowsetData);
            return rec;
            
        } catch (CloneNotSupportedException e)
        {
            log.error("Unable to clone record.", e);
            return null;
        }
    }

    /**
     * Returns the DBRowSet object.
     * @return the DBRowSet object
     */
    public abstract DBRowSet getRowSet();
    
    /**
     * Returns whether or not RollbackHandling is enabled for this record
     */
    public abstract boolean isRollbackHandlingEnabled(); 

    /**
     * returns true if this record is a new record.
     * @return true if this record is a new record
     */
    @Override
    public EntityType getEntityType()
    {
        return getRowSet();
    }

    /**
     * Returns the current DBDatabase object.
     * 
     * @return the current DBDatabase object
     */
    @SuppressWarnings("unchecked")
    @Override
	public DBDatabase getDatabase()
    {
        return getRowSet().getDatabase();
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
    @Override
    public boolean isValid()
    {
        return (state != State.Invalid);
    }

    /**
     * Returns true if the record is valid.
     * 
     * @return true if the record is valid
     */
    @Override
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
    @Override
    public boolean isModified()
    {
        return (state.isEqualOrMore(State.Modified));
    }

    /**
     * Returns true if this record is a new record.
     * 
     * @return true if this record is a new record
     */
    @Override
    public boolean isNew()
    {
        return (state == State.New);
    }

    /**
     * Returns true if this record is a existing record (valid but not new).
     * This may be used from expression language instead of the not allowed property "new" 
     * 
     * @return true if this record is a existing record (valid but not new).
     */
    public boolean isExists()
    {
        return (state == State.Valid || state == State.Modified);
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
        return getRowSet().getColumnIndex(column);
    }

    /**
     * Returns the index value by a specified column name.
     * 
     * @return the index value
     */
    @Override
    public int getFieldIndex(String column)
    {
        List<DBColumn> columns = getRowSet().getColumns();
        for (int i = 0; i < columns.size(); i++)
        {
            DBColumn col = columns.get(i);
            if (col.getName().equalsIgnoreCase(column))
                return i;
        }
        // not found
        return -1;
    }

    /**
     * Implements the Record Interface getColumn method.<BR>
     * Internally calls getDBColumn()
     * @return the Column at the specified index 
     */
    @Override
    public DBColumn getColumn(int index)
    {
        return getRowSet().getColumn(index);
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
        checkValid(index);
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
    @Override
    public final boolean wasModified(Column column)
    {
        return wasModified(getFieldIndex(column));
    }

    /**
     * Returns true if any of the given fields was modified.
     * 
     * @return true if any of the given fields were modified or false otherwise
     */
    public final boolean wasAnyModified(Column... columns)
    {
        for (Column c : columns)
        {
            if (wasModified(getFieldIndex(c)))
                return true;
        }
        return false;
    }

    /**
     * Returns whether or not values are checked for validity when calling setValue().
     * If set to true validateValue() is called to check validity
     * @return true if the validity of values is checked or false otherwise
     */
    public boolean isValidateFieldValues() 
    {
        return validateFieldValues;
    }

    /**
     * Set whether or not values are checked for validity when calling setValue().
     * If set to true validateValue() is called to check validity, otherwise not.
     * @param validateFieldValues flag whether to check validity
     */
    public void setValidateFieldValues(boolean validateFieldValues) 
    {
        this.validateFieldValues = validateFieldValues;
    }

	/**
     * returns an array of key columns which uniquely identify the record.
     * @return the array of key columns if any
     */
    @Override
    public Column[] getKeyColumns()
    {
        return getRowSet().getKeyColumns();
    }

    /**
     * Returns a array of key columns by a specified DBRecord object.
     * @return a array of key columns
     */
    @Override
    public Object[] getKey()
    {
        // Check Columns
        Column[] keyColumns = getKeyColumns();
        if (keyColumns == null || keyColumns.length==0)
            throw new NoPrimaryKeyException(getRowSet());
        // create the key
        Object[] key = new Object[keyColumns.length];
        for (int i = 0; i < keyColumns.length; i++)
        {
            key[i] = get(keyColumns[i]);
            if (key[i] == null)
            { // Primary Key not set
                log.warn("DBRecord.getKey() failed: " + getRowSet().getName() + " primary key value is null!");
            }
        }
        return key;
    }

    /**
     * Returns the value for the given column or null if either the index is out of range or the value is not valid (see {@link DBRecordBase#isValueValid(int)})
     * @return the index value
     */
    @Override
    public Object getValue(int index)
    {   // Check state
        checkValid(index);
        // Special check for NO_VALUE 
        if (fields[index] == ObjectUtils.NO_VALUE)
            throw new FieldValueNotFetchedException(getColumn(index));
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
        checkValid(index);
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
        // DBColumn col = ((colexpr instanceof DBColumn) ? ((DBColumn) colexpr) : colexpr.getSourceColumn());
        return column.getOptions();
    }

    /**
     * Gets the possbile Options for a field in the context of the current record.<BR>
     * Same as getFieldOptions(DBColumn)
     * @return the Option
     */
    @Override
    public final Options getFieldOptions(Column column)
    {
        return getFieldOptions((DBColumn)column); 
    }
    
    /**
     * validates all modified values of a record
     */
    public void validateAllValues()
    {
        checkValid();
        // Modified
        if (modified == null)
            return; // nothing to do
        // check for field
        for (int index=0; index<fields.length; index++)
        {   // Modified or No value?
            if (modified[index]==false || fields[index]==ObjectUtils.NO_VALUE)
                continue;
            // Auto-generated ?
            DBColumn column = getColumn(index);
            if (column.isAutoGenerated())
                continue;
            // validate this one
            fields[index] = validateValue(column, fields[index]);
        }
    }

    /**
     * Sets the value of a column in the record.
     * The functions checks if the column and the value are valid and whether the
     * value has changed.
     * 
     * @param index the index of the column
     * @param value the value
     */
    @Override
    public void setValue(int index, Object value)
    {
        checkValid(index);
        // check updatable
        checkUpdateable();
        // Special case ParentRecord
        if (value instanceof DBRecordBase)
        {   // Special case: Value contains parent record
            setParentRecord(getColumn(index), (DBRecordBase)value);
            return;
        }
        // Strings special
        if ((value instanceof String) && ((String)value).length()==0)
            value = null;
        // Is value valid
        Object current = fields[index]; 
        if (current==ObjectUtils.NO_VALUE)
            throw new FieldValueNotFetchedException(getColumn(index));
        // convert
        DBColumn column = getColumn(index);
        // must convert enums
        if (value instanceof Enum<?>)
        {   // convert enum
            Enum<?> enumVal = ((Enum<?>)value);
            boolean numeric = column.getDataType().isNumeric();
            value = ObjectUtils.getEnumValue(enumVal, numeric);
        }
        // Has Value changed?
        if (ObjectUtils.compareEqual(current, value))
        {   // value has not changed!
            return; 
        }
        // Check whether we can change this field
        if (!allowFieldChange(column))
        {   // Read Only column may be set
            throw new FieldReadOnlyException(column);
        }
        // Is Value valid?
        if (isValidateFieldValues())
        {   // validate
            Object validated = validateValue(column, value);
            if (value != validated)
            {   // Value has been converted, check again
                if (ObjectUtils.compareEqual(current, validated))
                    return; 
                // user converted value
                value = validated;
            }
        }
        // Init original values
        modifyValue(index, value, true);
    }

    /**
     * Deprecated Renamed to set(...)   
     */
    @Deprecated
    public DBRecordBase setValue(Column column, Object value)
    {
        return set(column, value);
    }

    /**
     * Sets the value of a column in the record.
     * The functions checks if the column and the value are valid and whether the
     * value has changed.
     * 
     * @param column a DBColumn object
     * @param value the value
     */
    @Override
    public DBRecordBase set(Column column, Object value)
    {   
        setValue(getFieldIndex(column), value);
        return this;
    }

    /**
     * Validates a value before it is set in the record.
     * By default, this method simply calls column.validate()
     * @param column the column that needs to be changed
     * @param value the new value
     */
    @Override
    public Object validateValue(Column column, Object value)
    {
    	return column.validateValue(value);
    }
    
    /**
     * returns whether a field is visible to the client or not
     * <P>
     * May be overridden to implement context specific logic.
     * @param column the column which to check for visibility
     * @return true if the column is visible or false if not 
     */
    @Override
    public boolean isFieldVisible(Column column)
    {
    	// Check value
        int index = getRowSet().getColumnIndex(column);
        if (index<0)
        {   // Column not found
            log.warn("Column {} does not exist for record of {}", column.getName(), getRowSet().getName());
        }
        return (index>=0 && isValueValid(index));
    }
    
    /**
     * returns whether a field is read only or not
     * 
     * @param column the database column 
     * 
     * @return true if the field is read only
     */
    @Override
    public boolean isFieldReadOnly(Column column)
    {
        DBRowSet rowset = getRowSet();
    	if (getFieldIndex(column)<0)
            throw new InvalidArgumentException("column", column);
    	// Check key column 
        if (isValid() && !isNew() && rowset.isKeyColumn((DBColumn)column))
        	return true;
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
    @Override
    public boolean isFieldRequired(Column column)
    {
    	if (getRowSet().getColumnIndex(column)<0)
            throw new InvalidArgumentException("column", column);
        // from column definition
        return (column.isRequired());
    }
    
    /**
     * Sets record values from the supplied java bean.
     * 
     * @return true if at least one value has been set successfully 
     */
    @Override
    public int setRecordValues(Object bean, Collection<Column> ignoreList)
    {
        // Add all Columns
        int count = 0;
        for (int i = 0; i < getFieldCount(); i++)
        { // Check Property
            DBColumn column = getColumn(i);
            if (column.isReadOnly())
                continue;
            if (ignoreList != null && ignoreList.contains(column))
                continue; // ignore this property
            // Get Property Name
            String property = column.getBeanPropertyName();
            setRecordValue(column, bean, property);
            count++;
        }
        return count;
    }

    /**
     * Sets record values from the suppied java bean.
     * @return true if at least one value has been set successfully
     */
    @Override
    public final int setRecordValues(Object bean)
    {
        return setRecordValues(bean, null);
    }
    
    /**
     * For DBMS with IDENTITY-columns defer setting the parent-id until the record is inserted
     * The parent record must have a one-column primary key
     * @param parentIdColumn the column for which to set the parent
     * @param record the parent record to be set for the column
     */
    public void setParentRecord(DBColumn parentIdColumn, DBRecordBase record)
    {
        checkValid();
        // check column
        checkParamNull("parentIdColumn", parentIdColumn);
        // check updateable
        checkUpdateable();
        // remove
        if (record==null)
        {   // clear parent 
            if (parentRecordMap!=null)
                parentRecordMap.remove(parentIdColumn);
            set(parentIdColumn, null);
            return;
        }
        // set key or record
        Object[] key = record.getKey();
        if (key.length!=1)
            throw new NotSupportedException(this, "setParentRecord");
        if (key[0]==null)
        {   // preserve until later
            if (parentRecordMap==null)
                parentRecordMap = new HashMap<DBColumn, DBRecordBase>(1);
            // add record to map
            log.info("Deffering setting of {} until the record is saved!", parentIdColumn.getName());
            parentRecordMap.put(parentIdColumn, record);
        }
        else
        {   // set directly
            int index = getFieldIndex(parentIdColumn);
            Object id = getValue(index);
            if (!ObjectUtils.compareEqual(id, key[0]))
            {   // set parent-id
                modifyValue(index, key[0], true);
            }
        }
    }

    /**
     * Compares the record to another one
     * @param other the record to compare this record with
     * @return true if it is the same record (but maybe a different instance)
     */
    public boolean isSame(DBRecordBase other)
    {   // check valid
        if (!isValid() || !other.isValid())
            return false;
        // compare table
        if (!getRowSet().isSame(other.getRowSet()))
            return false;
        // compare key
        Object[] key1 = getKey();
        Object[] key2 = other.getKey();
        return ObjectUtils.compareEqual(key1, key2);
    }

    /**
     * This function set the field descriptions to the the XML tag.
     * 
     * @return the number of column descriptions added to the element
     */
    @Override
    public int addXmlMeta(Element parent)
    {
        checkValid();
        // Add Field Description
        int count = 0;
        List<DBColumn> columns = getRowSet().getColumns();
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
    public int addXmlData(Element parent)
    {
        checkValid();
        // set row key
        Column[] keyColumns = getKeyColumns();
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
        List<DBColumn> columns = getRowSet().getColumns();
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
     * Returns a XML document with the field description an values of this record.
     * 
     * @return the new XML Document object
     */
    @Override
    public Document getXmlDocument()
    {
        checkValid();
        // Create Document
        DBXmlDictionary xmlDic = getXmlDictionary();
        Element root = XMLUtil.createDocument(xmlDic.getRowSetElementName());
        DBRowSet rowset = getRowSet();
        if (rowset.getName() != null)
            root.setAttribute("name", rowset.getName());
        // Add Field Description
        if (addXmlMeta(root)>0)
        {   // Add row Values
            addXmlData(XMLUtil.addElement(root, xmlDic.getRowElementName()));
        }
        // return Document
        return root.getOwnerDocument();
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
     * changes the state of the record
     * @param newState
     */
    protected void changeState(State newState)
    {
        this.state = newState;
    }
    
    /**
     * Factory function to create  createRollbackHandler();
     * @return the DBRollbackHandler
     */
    protected DBRollbackHandler createRollbackHandler()
    {
        return new DBRecordRollbackHandler(this);
    }
    
    /**
     * This method is used internally by the RowSet to initialize the record's properties
     * @param newRecord flag whether the record is new (non-existing) in the database
     */
    protected void initData(boolean newRecord)
    {
        // Init rowset
        DBRowSet rowset = getRowSet();
        int colCount = rowset.getColumns().size();
        if (fields==null || fields.length!=colCount)
            fields = new Object[colCount];
        else
        {   // clear fields
            for (int i=0; i<fields.length; i++)
                if (fields[i]!=ObjectUtils.NO_VALUE)
                    fields[i]=null;
        }
        // Set State
        this.modified = null;
        this.rowsetData = null;
        changeState((rowset==null ? State.Invalid : (newRecord ? State.New : State.Valid)));
    }
    
    /**
     * This method is used internally to indicate that the record update has completed<BR>
     * This will set change the record's state to Valid
     */
    protected void updateComplete()
    {
        // Change state
        this.modified = null;
        changeState(State.Valid);
    }
    
    /**
     * Checks whether the record is updateable  
     * If its read-only a RecordReadOnlyException is thrown 
     * @throws RecordReadOnlyException
     */
    protected void checkUpdateable()
    {
        if (this.isReadOnly())
        {   // Check allowReadOnlyUpdate
            if (!allowReadOnlyUpdate)
                throw new RecordReadOnlyException(this);
        }
    }
    
    /**
     * Checks whether or not this field can be changed at all.
     * Note: This is not equivalent to isFieldReadOnly() 
     * @param column the column that needs to be changed
     * @return true if it is possible to change this field for this record context
     */
    protected boolean allowFieldChange(DBColumn column)
    {
        // Check auto generated
        if (column.isAutoGenerated() && (!isNew() || !isNull(column)))
            return false;
        // Check key Column
        if (!isNew() && getRowSet().isKeyColumn(column))
            return false;
        // done
        return true;
    }

    /**
     * Modifies a column value bypassing all checks made by setValue.
     * Use this to explicitly set invalid values i.e. for temporary storage.
     * 
     * @param index index of the column
     * @param value the column value
     */
    protected void modifyValue(int index, Object value, boolean fireChangeEvent)
    {   // Check valid
        checkValid(index);
        // modified state array
        if (modified == null)
        {   modified = new boolean[fields.length];
            for (int j = 0; j < fields.length; j++)
                modified[j] = false;
        }
        // set value and modified
        fields[index] = value;
        modified[index] = true;
        // set record state
        if (state.isLess(State.Modified))
            changeState(State.Modified);
        // field changed event
        if (fireChangeEvent)
            onFieldChanged(index);
    }
    
    /**
     * Override this to do extra handling when the record changes
     */
    protected void onRecordChanged()
    {
        if (log.isTraceEnabled() && isValid())
            log.trace("Record has been changed");
        // Remove rollback (but not when close() is called!)
        if (fields!=null && isRollbackHandlingEnabled())
            getContext().removeRollbackHandler(this);
    }
    
    /**
     * Override this to get notified when a field value changes
     */
    protected void onFieldChanged(int i)
    {
        if (log.isDebugEnabled())
            log.debug("Record field " + getColumn(i).getName() + " changed to " + String.valueOf(fields[i]));
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
     */
    protected void setRecordValue(Column column, Object bean, String property)
    {
        if (StringUtils.isEmpty(property))
            property = column.getBeanPropertyName();
        try
        {   // Get Property Value
            PropertyUtilsBean pub = BeanUtilsBean.getInstance().getPropertyUtils();
            Object value = pub.getSimpleProperty(bean, property);
            // Now, set the record value
            set( column, value ); 
            // done
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
     * For DBMS with IDENTITY-columns the deferred parent-keys are set by this functions
     * The parent records must have been previously set using setParentRecord
     */
    protected void assignParentIdentities()
    {
        // Check map
        if (parentRecordMap==null)
            return;
        // Apply map
        for (Map.Entry<DBColumn, DBRecordBase> e : parentRecordMap.entrySet())
        {
            DBColumn parentIdColumn = e.getKey();
            Object keyValue = e.getValue().getKey()[0];
            if (keyValue==null)
                throw new ObjectNotValidException(e.getValue());
            // Set key
            log.info("Deffered setting of {} to {}!", parentIdColumn.getName(), keyValue);
            set(parentIdColumn, keyValue);
        }
        parentRecordMap.clear();
    }
    
    /**
     * helper function to check if a given field index corresponds to one of the given columns
     * @param index the field index
     * @param column one or more columns to check
     * @return true if the index is for one of the columns or false otherwise
     */
    protected boolean isColumn(int index, DBColumn... column)
    {
        if (index < 0 || index >= fields.length)
            throw new InvalidArgumentException("index", index);
        if (column==null)
            throw new InvalidArgumentException("column", column);
        Column col = getColumn(index);
        for (int i=0; i<column.length; i++)
        {   // compare
            if (col==column[i])
                return true;
        }
        // not found
        return false; 
    }
    
    /**
     * returns the DBXmlDictionary that should used to generate XMLDocuments<BR>
     * @return the DBXmlDictionary
     */
    protected DBXmlDictionary getXmlDictionary()
    {
        return DBXmlDictionary.getInstance();
    }
    
}
