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
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

import org.apache.empire.commons.ClassUtils;
import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.commons.StringUtils;
import org.apache.empire.data.Column;
import org.apache.empire.db.DBRowSet.PartialMode;
import org.apache.empire.db.exceptions.NoPrimaryKeyException;
import org.apache.empire.db.expr.compare.DBCompareExpr;
import org.apache.empire.exceptions.InvalidArgumentException;
import org.apache.empire.exceptions.ItemNotFoundException;
import org.apache.empire.exceptions.NotSupportedException;
import org.apache.empire.exceptions.ObjectNotValidException;
import org.apache.empire.exceptions.UnspecifiedErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class represents a record from a database table, view or query
 * 
 * The class provides methods to create, read, update and delete records
 * 
 * If an Idendity-column (AUTOINC) is defined, the value will be set upon creation by the dbms to the next value
 * If a Timestamp-column is defined the value will be automatically set and concurrent changes of the record will be detected
 * 
 * If changes to the record are made, but a rollback on the connection is performed, the changes will be reverted (Rollback-Handling)
 * 
 * The record is Serializable either if the provided DBContext is serializable, or if the Context is provided on deserialization in a derived class.
 */
public class DBRecord extends DBRecordBase implements Serializable // really Serializable?
{
    private static final long serialVersionUID = 1L;
    
    private static final Logger log  = LoggerFactory.getLogger(DBRecord.class);
    
    /**
     * varArgs to Array
     * @param values
     * @return
     */
    public static Object[] key(Object... values)
    {
        if (values.length==0)
            throw new InvalidArgumentException("values", values);
        return values;
    }

    // Context and RowSet
    protected final transient DBContext context;  /* transient for serialization */
    protected final transient DBRowSet  rowset;   /* transient for serialization */

    // options
    private boolean enableRollbackHandling;
    
    // Parent-Record-Map for deferred identity setting 
    private Map<DBColumn, DBRecordBase> parentRecordMap;
    
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
     * Internal constructor for DBRecord
     * May be used by derived classes to provide special behaviour
     */
    protected DBRecord(DBContext context, DBRowSet rowset, boolean enableRollbackHandling)
    {   // init
        this.context = context;
        this.rowset = rowset;
        // options
        this.enableRollbackHandling = enableRollbackHandling;
        this.validateFieldValues = true;
    }

    /**
     * Constructs a new DBRecord.<BR>
     * @param context the DBContext for this record
     * @param rowset the corresponding RowSet(Table, View, Query, etc.)
     */
    public DBRecord(DBContext context, DBRowSet rowset)
    {
        this(checkParamNull("context", context),
             checkParamNull("rowset", rowset),
             context.isRollbackHandlingEnabled());
    }

    /**
     * Returns the current Context
     * @return
     */
    @Override
    @SuppressWarnings("unchecked")
    public DBContext getContext()
    {
        if (this.context==null)
            throw new ObjectNotValidException(this);
        return context;
    }

    /**
     * Returns the DBRowSet object.
     * 
     * @return the DBRowSet object
     */
    @Override
    public DBRowSet getRowSet()
    {
        if (this.rowset==null)
            throw new ObjectNotValidException(this);
        return this.rowset;
    }

    /**
     * Returns whether or not RollbackHandling is enabled for this record
     */
    @Override
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
        if (enabled && !getContext().isRollbackHandlingEnabled())
            throw new UnspecifiedErrorException("Rollback handling cannot be enabled for this record since it is not supported for this context!");
        // enable or disable
        this.enableRollbackHandling = enabled;
    }
    
    /**
     * Returns the record id for tables which have a single numeric primary key
     * This method is provided for convenience in addition to the the getKey() method
     * @return the record id or 0 if the key is null
     * @throws NoPrimaryKeyException if the table has no primary key
     * @throws NotSupportedException if the primary key is not a single column of if the column is not numeric
     */
    public long getId()
    {
        // Check Columns
        Column[] keyColumns = getKeyColumns();
        if (keyColumns == null || keyColumns.length==0)
            throw new NoPrimaryKeyException(getRowSet());
        // Check Columns
        if (keyColumns.length!=1 || !keyColumns[0].getDataType().isNumeric())
            throw new NotSupportedException(this, "getId");
        // the numeric id
        return getLong(keyColumns[0]);
    }

    /**
     * Creates a new record
     */
    public void create(Object[] initalKey)
    {
        getRowSet().createRecord(this, initalKey, true);
    }

    /**
     * Creates a new record
     */
    public void create()
    {
        getRowSet().createRecord(this, null, false);
    }
    
    /**
     * Reads a record from the database
     * Hint: variable args param (Object...) caused problems with migration
     * @param key an array of the primary key values
     */
    public void read(Object[] key)
    {   // read
        getRowSet().readRecord(this, key);
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
        getRowSet().readRecord(this, whereConstraints);
    }
    
    /**
     * Reads a record partially i.e. not with all but just some selected fields
     * There are two modes:
     *  1. PartialMode.INCLUDE reads only the fields provided with the column list
     *  2. PartialMode.EXCLUDE reads all but the fields provided with the column list
     * The primary key is always fetched implicitly
     * @param key the primary key values
     * @param mode flag whether to include only the given columns or whether to add all but the given columns
     * @param columns the columns to include or exclude (depending on mode)
     */
    public void read(Object[] key, PartialMode mode, DBColumn... columns)
    {
        getRowSet().readRecord(this, key, mode, columns);
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
        // check updatable
        checkUpdateable();
        // allow rollback
        if (isRollbackHandlingEnabled())
            getContext().appendRollbackHandler(createRollbackHandler());
        // set parent record identity
        if (ObjectUtils.isNotEmpty(parentRecordMap))
            assignParentIdentities();
        // update
        getRowSet().updateRecord(this);
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
        // check updatable
        checkUpdateable();
        // allow rollback
        if (isRollbackHandlingEnabled())
            getContext().appendRollbackHandler(createRollbackHandler());
        // Delete only if record is not new
        if (!isNew())
        {   // Delete existing record
            Object[] key = getKey();
            log.info("Deleting record {}", StringUtils.arrayToString(key, "|"));
            getRowSet().deleteRecord(key, getContext());
        }
        close();
    }
    
    /**
     * Overridden for special deferred parent record handling 
     */
    @Override
    public void setValue(int index, Object value)
    {
        if (value instanceof DBRecordBase)
        {   // Special case: Value contains parent record
            setParentRecord(getColumn(index), (DBRecordBase)value);
            return;
        }
        super.setValue(index, value);
    }
    
    /**
     * For DBMS with IDENTITY-columns defer setting the parent-id until the record is inserted
     * The parent record must have a one-column primary key
     * @param parentIdColumn the column for which to set the parent
     * @param record the parent record to be set for the column
     */
    public void setParentRecord(DBColumn parentIdColumn, DBRecordBase record)
    {
        if (!isValid())
            throw new ObjectNotValidException(this);
        // check column
        checkParamNull("parentIdColumn", parentIdColumn);
        // check updateable
        checkUpdateable();
        // remove
        if (record==null)
        {   // clear parent 
            if (parentRecordMap!=null)
                parentRecordMap.remove(parentIdColumn);
            setValue(parentIdColumn, null);
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
            setValue(parentIdColumn, keyValue);
        }
        parentRecordMap.clear();
    }
}
