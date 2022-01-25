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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.util.Collection;
import java.util.List;

import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.beanutils.PropertyUtilsBean;
import org.apache.empire.commons.ClassUtils;
import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.commons.Options;
import org.apache.empire.commons.StringUtils;
import org.apache.empire.data.Column;
import org.apache.empire.data.ColumnExpr;
import org.apache.empire.data.Record;
import org.apache.empire.db.context.DBContextAware;
import org.apache.empire.db.context.DBRollbackHandler;
import org.apache.empire.db.exceptions.FieldIsReadOnlyException;
import org.apache.empire.db.exceptions.FieldValueNotFetchedException;
import org.apache.empire.db.expr.compare.DBCompareExpr;
import org.apache.empire.exceptions.BeanPropertyGetException;
import org.apache.empire.exceptions.InvalidArgumentException;
import org.apache.empire.exceptions.ItemNotFoundException;
import org.apache.empire.exceptions.NotSupportedException;
import org.apache.empire.exceptions.ObjectNotValidException;
import org.apache.empire.exceptions.UnspecifiedErrorException;
import org.apache.empire.xml.XMLUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * This class represents a record from a database table, view or query
 * 
 * The class provides methods to create, read, update and delete records
 * The class provides methods to obtain as well as to modify its fields
 * 
 * If an Idendity-column (AUTOINC) is defined, the value will be set upon creation by the dbms to the next value
 * If a Timestamp-column is defined the value will be automatically set and concurrent changes of the record will be detected
 * 
 * If changes to the record are made, but a rollback on the connection is performed, the changes will be reverted (Rollback-Handling)
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
 * 
 * The record is Serializable either if the provided DBContext is serializable, or if the Context is provided on deserialization in a derived class.
 */
public class DBRecord extends DBRecordData implements DBContextAware, Record, Cloneable, Serializable
{
    private static final long serialVersionUID = 1L;
    
    private static final Logger log  = LoggerFactory.getLogger(DBRecord.class);
    
    /**
     * DBRecordRollbackHandler
     * @author doebele
     */
    public static class DBRecordRollbackHandler implements DBRollbackHandler
    {
        // Logger
        private static final Logger log = LoggerFactory.getLogger(DBRecordRollbackHandler.class);
        
        public final DBRecord   record;
        
        private final State     state;  /* the original state */
        private Object[]        fields;
        private boolean[]       modified;
        private Object          rowsetData;
        
        public DBRecordRollbackHandler(DBRecord record)
        {
            this.record = record;
            // check
            if (record.state==State.Invalid)
                throw new ObjectNotValidException(record);
            // save state
            this.state = record.state;            
            this.modified = copy(record.modified);
            this.fields   = copy(record.fields);
            this.rowsetData = record.rowsetData;
        }

        @Override
        public DBObject getObject()
        {
            return record;
        }

        @Override
        public String getObjectInfo()
        {
            return "Record "+record.getRowSet().getName()+":"+StringUtils.arrayToString(record.getKey(), "|");
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
            // copy
            for (int i=0; i<fields.length; i++)
            {
                if (fields[i]!= s.fields[i])
                    fields[i] = s.fields[i]; 
                // not modified
                if (modified==null)
                    continue;
                if (modified[i]==false && s.modified[i])
                    modified[i] = s.modified[i]; 
            }
            // check modified
            if (modified==null && s.modified!=null)
                modified = copy(s.modified);
        }

        @Override
        public void rollback()
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
        public void discard()
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

    /**
     * varArgs to Array
     * @param parts
     * @return
     */
    public static Object[] key(Object... parts)
    {
        return parts;
    }

    // Context and RowSet
    protected final transient DBContext context;  /* transient for serialization */
    protected final transient DBRowSet  rowset;   /* transient for serialization */
    
    // This is the record data
    private State           state;
    private Object[]        fields;
    private boolean[]       modified;
    private Object          rowsetData; // Special Rowset Data (usually null)

    // options
    protected boolean       enableRollbackHandling;
    protected boolean       validateFieldValues;
    
    
    /**
     * Custom serialization for transient rowset.
     * 
     */
    private void writeObject(ObjectOutputStream strm) throws IOException 
    {   // Context
        writeContext(strm);
        // RowSet
        writeRowSet(strm);
        // write object
        strm.defaultWriteObject();
    }
    
    protected void writeContext(ObjectOutputStream strm) throws IOException
    {
        strm.writeObject(context);
    }
    
    protected void writeRowSet(ObjectOutputStream strm) throws IOException
    {
        String dbid = rowset.getDatabase().getIdentifier(); 
        String rsid = rowset.getName(); 
        strm.writeObject(dbid);
        strm.writeObject(rsid);
    }
    
    /**
     * Custom deserialization for transient rowset.
     */
    private void readObject(ObjectInputStream strm) throws IOException, ClassNotFoundException 
    {   // Context
        DBContext ctx = readContext(strm);
        ClassUtils.setPrivateFieldValue(DBRecord.class, this, "context", ctx);
        // set final field
        DBRowSet rowset = readRowSet(strm);
        ClassUtils.setPrivateFieldValue(DBRecord.class, this, "rowset", rowset);
        // read the rest
        strm.defaultReadObject();
    }
    
    protected DBContext readContext(ObjectInputStream strm)  throws IOException, ClassNotFoundException
    {
        return (DBContext)strm.readObject();
    }
    
    protected DBRowSet readRowSet(ObjectInputStream strm)  throws IOException, ClassNotFoundException
    {   // Rowset
        String dbid = String.valueOf(strm.readObject());
        String rsid = String.valueOf(strm.readObject());
        // find database
        DBDatabase dbo = DBDatabase.findByIdentifier(dbid);
        if (dbo==null)
            throw new ItemNotFoundException(dbid);
        // find rowset
        DBRowSet rso = dbo.getRowSet(rsid);
        if (rso==null)
            throw new ItemNotFoundException(dbid);
        // done
        return rso;
    }
    
    /**
     * Constructs a new DBRecord.<BR>
     * @param context the DBContext for this record
     * @param rowset the corresponding RowSet(Table, View, Query, etc.)
     */
    public DBRecord(DBContext context, DBRowSet rowset)
    {
        // Check params
        if (context==null || rowset==null)
            throw new InvalidArgumentException("context|rowset", context);
        // init
        this.context = context;
        this.rowset = rowset;
        this.state = State.Invalid;
        this.fields = null;
        this.modified = null;
        this.rowsetData = null;
        // options                         
        enableRollbackHandling = context.isRollbackHandlingEnabled();
        validateFieldValues = true;
    }

    /**
     * Closes the record by releasing all resources and resetting the record's state to invalid.
     */
    @Override
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
    public DBRecord clone()
    {
        try 
        {
            DBRecord rec = (DBRecord)super.clone();
            if (rec.rowset!= this.rowset)
                throw new NotSupportedException(this, "clone");
            rec.state = this.state;
            if (rec.fields == fields && fields!=null)
                rec.fields = fields.clone();
            if (rec.modified == modified && modified!=null)
                rec.modified = modified.clone();
            rec.rowsetData = this.rowsetData;
            rec.validateFieldValues = this.validateFieldValues;
            return rec;
            
        } catch (CloneNotSupportedException e)
        {
            log.error("Unable to clone record.", e);
            return null;
        }
    }

    /**
     * Returns the current Context
     * @return
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T extends DBContext> T  getContext()
    {
        return ((T)context);
    }

    /**
     * Returns the current DBDatabase object.
     * 
     * @return the current DBDatabase object
     */
    @Override
    public final <T extends DBDatabase> T getDatabase()
    {
        return rowset.getDatabase();
    }

    /**
     * Returns the DBRowSet object.
     * 
     * @return the DBRowSet object
     */
    @SuppressWarnings("unchecked")
    public <T extends DBRowSet> T getRowSet()
    {
        return (T)this.rowset;
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
    @Override
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
     * returns an array of key columns which uniquely identify the record.
     * @return the array of key columns if any
     */
    @Override
    public Column[] getKeyColumns()
    {
        return rowset.getKeyColumns();
    }

    /**
     * Returns the array of primary key columns.
     * @return the array of primary key columns
     */
    @Override
    public Object[] getKey()
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
        if (!this.isValid())
            throw new ObjectNotValidException(this);
        // Modified
        if (modified == null)
            return; // nothing to do
        // check for field
        for (int index=0; index<fields.length; index++)
        {   // Modified or No value?
            if (modified[index]==false || fields[index]==ObjectUtils.NO_VALUE)
                continue;
            // Auto-generated ?
            DBColumn column = rowset.getColumn(index);
            if (column.isAutoGenerated())
                continue;
            // validate this one
            fields[index] = validateValue(column, fields[index]);
        }
    }

    /**
     * Sets the value of the column in the record.
     * The functions checks if the column and the value are valid and whether the
     * value has changed.
     * 
     * @param index the index of the column
     * @param value the value
     */
    @Override
    public void setValue(int index, Object value)
    {
        if (state == State.Invalid)
            throw new ObjectNotValidException(this);
        if (index < 0 || index >= fields.length)
            throw new InvalidArgumentException("index", index);
        // Strings special
        if ((value instanceof String) && ((String)value).length()==0)
            value = null;
        // Is value valid
        Object current = fields[index]; 
        if (current==ObjectUtils.NO_VALUE)
            throw new FieldValueNotFetchedException(getColumn(index));
        // convert
        DBColumn column = rowset.getColumn(index);
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
            throw new FieldIsReadOnlyException(column);
        }
        // Is Value valid?
        if (this.validateFieldValues)
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
     * Sets the value of the column in the record.
     * The functions checks if the column and the value are valid and whether the
     * value has changed.
     * 
     * @param column a DBColumn object
     * @param value the value
     */
    @Override
    public final void setValue(Column column, Object value)
    {
        if (!isValid())
            throw new ObjectNotValidException(this);
        // Get Column Index
        setValue(getFieldIndex(column), value);
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
     * Returns whether or not RollbackHandling is enabled for this record
     */
    public boolean isRollbackHandlingEnabled() 
    {
		return this.enableRollbackHandling;
	}

    /**
     * Set whether or not RollbackHandling will be performed for this record
     * Since Rollback handling requires additional resources it should only be used if necessary
     * Especially for bulk operations it should be disabled
     * @param enabled flag whether to enable or disable RollbackHandling 
     */
	public void setRollbackHandlingEnabled(boolean enabled) 
	{
	    // check
	    if (enabled && !context.isRollbackHandlingEnabled())
	        throw new UnspecifiedErrorException("Rollback handling cannot be enabled for this record since it is not supported for this context!");
	    // enable now
		this.enableRollbackHandling = enabled;
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
     * returns whether a field is visible to the client or not
     * <P>
     * May be overridden to implement context specific logic.
     * @param column the column which to check for visibility
     * @return true if the column is visible or false if not 
     */
    @Override
    public boolean isFieldVisible(Column column)
    {
        if (rowset==null)
            return false;
    	// Check value
        int index = rowset.getColumnIndex(column);
        if (index<0)
        {   // Column not found
            log.warn("Column {} does not exist for record of {}", column.getName(), rowset.getName());
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
        if (rowset==null)
            throw new ObjectNotValidException(this);
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
        if (rowset==null)
            throw new ObjectNotValidException(this);
    	if (rowset.getColumnIndex(column)<0)
            throw new InvalidArgumentException("column", column);
        // from column definition
        return (column.isRequired());
    }

    /**
     * Creates a new record
     */
    public void create(Object[] initalKey)
    {
        rowset.createRecord(this, initalKey, true);
    }

    /**
     * Creates a new record
     */
    public void create()
    {
        rowset.createRecord(this, null, false);
    }
    
    /**
     * Reads a record from the database
     * Hint: variable args param (Object...) caused problems with migration
     * @param key an array of the primary key values
     */
    public void read(Object[] key)
    {   // read
        rowset.readRecord(this, key);
    }

    /**
     * Reads a record from the database
     * @param id the record id value
     */
    public final void read(long id)
    {
        read(new Object[] {id});
    }
    
    /**
     * Reads a record from the database
     * @param key an array of the primary key values
     */
    public void read(DBCompareExpr whereConstraints)
    {
        rowset.readRecord(this, whereConstraints);
    }

    /**
     * Updates the record and saves all changes in the database.
     */
    public void update()
    {
        if (!isValid())
            throw new ObjectNotValidException(this);
        if (!isModified())
            return; /* Not modified. Nothing to do! */
        // allow rollback
        if (enableRollbackHandling)
            context.appendRollbackHandler(createRollbackHandler());
        // update
        rowset.updateRecord(this);
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
    public void delete()
    {
        if (isValid()==false)
            throw new ObjectNotValidException(this);
        // allow rollback
        if (enableRollbackHandling)
            context.appendRollbackHandler(createRollbackHandler());
        // Delete only if record is not new
        if (!isNew())
        {
            Object[] keys = rowset.getRecordKey(this);
            rowset.deleteRecord(keys, context);
        }
        close();
    }

    /**
     * This function set the field descriptions to the the XML tag.
     * 
     * @return the number of column descriptions added to the element
     */
    @Override
    public int getXmlMeta(Element parent)
    {
        if (!isValid())
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
    public int getXmlData(Element parent)
    {
        if (!isValid())
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
     * Returns a XML document with the field description an values of this record.
     * 
     * @return the new XML Document object
     */
    @Override
    public Document getXmlDocument()
    {
        if (!isValid())
            throw new ObjectNotValidException(this);
        // Create Document
        DBXmlDictionary xmlDic = getXmlDictionary();
        Element root = XMLUtil.createDocument(xmlDic.getRowSetElementName());
        if (rowset.getName() != null)
            root.setAttribute("name", rowset.getName());
        // Add Field Description
        if (getXmlMeta(root)>0)
        {   // Add row Values
            getXmlData(XMLUtil.addElement(root, xmlDic.getRowElementName()));
        }
        // return Document
        return root.getOwnerDocument();
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
            DBColumn column = getDBColumn(i);
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
     * @return true if at least one value has been set sucessfully
     */
    @Override
    public final int setRecordValues(Object bean)
    {
        return setRecordValues(bean, null);
    }

    /**
     * Compares the record to another one
     * @param otherObject
     * @return true if it is the same record (but maybe a different instance)
     */
    public boolean isSame(DBRecord other)
    {   // check valid
        if (!isValid() || !other.isValid())
            return false;
        // compare table
        if (!rowset.isSame(other.getRowSet()))
            return false;
        // compare key
        Object[] key1 = getKey();
        Object[] key2 = other.getKey();
        return ObjectUtils.compareEqual(key1, key2);
    }

    /**
     * Returns the DBRowSet object.
     * 
     * @return the DBRowSet object
     */
    protected Object getRowSetData()
    {
        return rowsetData;
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
     * returns the DBXmlDictionary that should used to generate XMLDocuments<BR>
     * @return the DBXmlDictionary
     */
    protected DBXmlDictionary getXmlDictionary()
    {
        return DBXmlDictionary.getInstance();
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
     * @param rowset the rowset to which to attach this record
     * @param rowsetData any further RowSet specific data
     * @param newRecord
     */
    protected void initData(Object rowsetData, boolean newRecord)
    {
        // Init rowset
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
        this.rowsetData = rowsetData;
        this.modified = null;
        changeState((rowset==null ? State.Invalid : (newRecord ? State.New : State.Valid)));
    }
    
    /**
     * This method is used internally to indicate that the record update has completed<BR>
     * This will set change the record's state to Valid
     * @param rowsetData additional data held by the rowset for this record (optional)
     */
    protected void updateComplete(Object rowsetData)
    {
        // change rowsetData
        if (rowsetData!=ObjectUtils.NO_VALUE)
            this.rowsetData = rowsetData;
        // Change state
        this.modified = null;
        changeState(State.Valid);
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
        if (!isNew() && rowset.isKeyColumn(column))
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
        if (state == State.Invalid)
            throw new ObjectNotValidException(this);
        if (index < 0 || index >= fields.length)
            throw new InvalidArgumentException("index", index);
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

    /*
     * Sets the modified state of a column.<BR>
     * This will force the field to be updated in the database, if set to TRUE.
     * 
     * @param column the column
     * @param isModified modified or not
     * 
    protected void setModified(DBColumn column, boolean isModified)
    {   // Check valid
        if (state == State.Invalid)
            throw new ObjectNotValidException(this);
        // Check modified
        if (modified == null)
        {   // Init array
            modified = new boolean[fields.length];
            for (int j = 0; j < fields.length; j++)
                modified[j] = false;
        }
        int index = getFieldIndex(column);
        if (index >= 0)
            modified[index] = isModified;
        // Set State to modified, if not already at least modified and isModified is set to true
        if (state.isLess(State.Modified) && isModified)
            changeState(State.Modified);
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
                changeState(State.Valid);
            }
        }
    }
    */

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
            setValue( column, value ); 
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
     * Override this to do extra handling when the record changes
     */
    protected void onRecordChanged()
    {
        if (log.isTraceEnabled() && isValid())
            log.trace("Record has been changed");
        // Remove rollback (but not when close() is called!)
        if (enableRollbackHandling && fields!=null)
            context.removeRollbackHandler(this);
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
