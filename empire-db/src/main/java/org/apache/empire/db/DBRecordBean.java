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

import org.apache.empire.commons.StringUtils;
import org.apache.empire.data.Column;
import org.apache.empire.db.DBRowSet.PartialMode;
import org.apache.empire.db.exceptions.NoPrimaryKeyException;
import org.apache.empire.db.expr.compare.DBCompareExpr;
import org.apache.empire.exceptions.InvalidArgumentException;
import org.apache.empire.exceptions.NotSupportedException;
import org.apache.empire.exceptions.ObjectNotValidException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class represents a record from a database table, view or query
 * 
 * Other than DBRecord it is not permanently attached to a context or rowset
 * 
 * Thus it has a Default constructor and is essentially a dynamic bean
 * 
 */
public class DBRecordBean extends DBRecordBase
{
    private static final long serialVersionUID = 1L;
    
    private static final Logger log  = LoggerFactory.getLogger(DBRecordBean.class);
    
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
    protected transient DBContext tempContext;
    protected transient DBRowSet  rowset;   /* will be injected by DBRowset */

    // options
    protected boolean enableRollbackHandling;

    /**
     * Constructs a new DBRecordBean.<BR>
     * @param enableRollbackHandling flag whether to enable rollback handing
     */
    public DBRecordBean(boolean enableRollbackHandling)
    {
        this.enableRollbackHandling = enableRollbackHandling;
    }

    /**
     * Constructs a new DBRecordBean.<BR>
     * @param context the DBContext for this record
     * @param rowset the corresponding RowSet(Table, View, Query, etc.)
     */
    public DBRecordBean()
    {
        this(false);
    }

    /**
     * Returns the current Context
     * @return
     */
    @SuppressWarnings("unchecked")
    @Override
    public DBContext getContext()
    {
        if (this.tempContext==null)
            throw new ObjectNotValidException(this);
        return tempContext;
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
    
    @Override
    public void close()
    {
        super.close();
        // clear
        this.rowset = null;
    }

    /**
     * Creates a new record
     */
    public DBRecordBean create(DBContext context, DBRowSet rowset, Object[] initalKey)
    {
        try {
            this.tempContext = context;
            rowset.createRecord(this, initalKey, true);
            return this;
        } finally {
            this.tempContext = null;
        }
    }

    /**
     * Creates a new record
     */
    public DBRecordBean create(DBContext context, DBRowSet rowset)
    {
        try {
            this.tempContext = context;
            rowset.createRecord(this, null, false);
            return this;
        } finally {
            this.tempContext = null;
        }
    }
    
    /**
     * Reads a record from the database
     * Hint: variable args param (Object...) caused problems with migration
     * @param key an array of the primary key values
     */
    public DBRecordBean read(DBContext context, DBRowSet rowset, Object[] key)
    {   // read
        try {
            this.tempContext = context;
            rowset.readRecord(this, key);
            return this;
        } finally {
            this.tempContext = null;
        }
    }

    /**
     * Reads a record from the database
     * @param id the record id value
     */
    public final DBRecordBean read(DBContext context, DBRowSet rowset, long id)
    {
        return read(context, rowset, new Object[] {id});
    }
    
    /**
     * Reads a record from the database
     * @param key an array of the primary key values
     */
    public DBRecordBean read(DBContext context, DBRowSet rowset, DBCompareExpr whereConstraints)
    {   // read
        try {
            this.tempContext = context;
            rowset.readRecord(this, whereConstraints);
            return this;
        } finally {
            this.tempContext = null;
        }
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
    public DBRecordBean read(DBContext context, DBRowSet rowset, Object[] key, PartialMode mode, DBColumn... columns)
    {   // read
        try {
            this.tempContext = context;
            rowset.readRecord(this, key, mode, columns);
            return this;
        } finally {
            this.tempContext = null;
        }
    }

    /**
     * Sets the value of a column in the record.
     * Same as getValue but provided in conjunction with set(...)

     * @param column a DBColumn object
     * @param value the value
     */
    public final Object get(Column column)
    {   
        return getValue(column);
    }

    /**
     * Sets the value of a column in the record.
     * Same as setValue but allows chaining as it returns itself

     * @param column a DBColumn object
     * @param value the value
     */
    public final DBRecordBean set(Column column, Object value)
    {   
        setValue(getFieldIndex(column), value);
        return this;
    }
    
    /**
     * Updates the record in the database
     * @param context the current context
     */
    public void update(DBContext context)
    {   // update
        if (isValid()==false)
            throw new ObjectNotValidException(this);
        if (!isModified())
            return; /* Not modified. Nothing to do! */
        try {
            this.tempContext = context;
            // check updatable
            checkUpdateable();
            // allow rollback
            if (this.enableRollbackHandling && context.isRollbackHandlingEnabled())
                context.appendRollbackHandler(createRollbackHandler());
            // update
            getRowSet().updateRecord(this);
        } finally {
            this.tempContext = null;
        }
    }

    /**
     * This helper function calls the DBRowset.deleteRecord method 
     * to delete the record.
     * 
     * WARING: There is no guarantee that it ist called
     * Implement delete logic in the table's deleteRecord method if possible
     * 
     * @see org.apache.empire.db.DBTable#deleteRecord(Object[], Connection)
     * @param context the current context
     */
    public void delete(DBContext context)
    {
        if (isValid()==false)
            throw new ObjectNotValidException(this);
        // check updatable
        checkUpdateable();
        // allow rollback
        if (this.enableRollbackHandling && context.isRollbackHandlingEnabled())
            context.appendRollbackHandler(createRollbackHandler());
        // Delete only if record is not new
        if (!isNew())
        {
            Object[] key = getKey();
            log.info("Deleting record {}", StringUtils.arrayToString(key, "|"));
            getRowSet().deleteRecord(key, context);
        }
        close();
    }
    
}
