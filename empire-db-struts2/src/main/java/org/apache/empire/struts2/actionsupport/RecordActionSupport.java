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
package org.apache.empire.struts2.actionsupport;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.empire.commons.StringUtils;
import org.apache.empire.data.Record;
import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBCommand;
import org.apache.empire.db.DBIndex;
import org.apache.empire.db.DBReader;
import org.apache.empire.db.DBRecord;
import org.apache.empire.db.DBRowSet;
import org.apache.empire.db.DBTable;
import org.apache.empire.db.exceptions.InvalidKeyException;
import org.apache.empire.db.exceptions.NoPrimaryKeyException;
import org.apache.empire.db.exceptions.QueryNoResultException;
import org.apache.empire.db.expr.compare.DBCompareExpr;
import org.apache.empire.exceptions.EmpireException;
import org.apache.empire.exceptions.ItemNotFoundException;
import org.apache.empire.exceptions.NotSupportedException;
import org.apache.empire.exceptions.ObjectNotValidException;
import org.apache.empire.struts2.exceptions.InvalidFormDataException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * RecordActionSupport
 * <p>
 * This class provides functions for form data processing for a given Table or View (DBRowSet).<br>   
 * The record object provided with the constructor will be used to obtain further context specific metadata
 * such as field options (getFieldOptions) and field accessibility (isFieldReadOnly).<br>
 * The record object should initially be invalid and not attached to any Table or View (DBRowSet).
 * </p>
 * @author Rainer
 */
public class RecordActionSupport extends RecordFormActionSupport
{
    /*
     * Choices for record persistence
     * 
     * None = nothing is stored on session
     * Key = the record key is stored on the session. The record is reloaded if necessary
     * Data = the whole record is stored on the session.  
     */
    @Deprecated
    public static enum SessionPersistance
    {
        None,
        Key,
        Data;
        
        static SessionPersistence convert(SessionPersistance sp)
        {
            switch(sp)
            {
                case Key: return SessionPersistence.Key;
                case Data: return SessionPersistence.Data;
                default: return SessionPersistence.None; 
            }
        }
    }
    
    
    @SuppressWarnings("hiding")
    protected static Logger log = LoggerFactory.getLogger(RecordActionSupport.class);

    protected DBRowSet rowset;

    protected DBRecord record;
    
    private boolean loadBeforeDelete = false;
    
    public RecordActionSupport(ActionBase action, DBRowSet rowset, DBRecord record, SessionPersistence persistence, String propertyName)
    {
        super(action, persistence, propertyName);
        // Set Rowset and Record
        this.rowset = rowset;
        this.record = record;
    }

    public RecordActionSupport(ActionBase action, DBRowSet rowset, DBRecord record, SessionPersistence persistence)
    {
        this(action, rowset, record, persistence, action.getItemPropertyName());
    }

    @Deprecated
    public RecordActionSupport(ActionBase action, DBRowSet rowset, DBRecord record, SessionPersistance persistence, String propertyName)
    {
        this(action, rowset, record, SessionPersistance.convert(persistence), propertyName);
    }

    @Deprecated
    public RecordActionSupport(ActionBase action, DBRowSet rowset, DBRecord record, SessionPersistance persistence)
    {
        this(action, rowset, record, SessionPersistance.convert(persistence), action.getItemPropertyName());
    }
    
    @Override
    public DBRecord getRecord()
    {
        return record;
    }

    public DBRowSet getRowset()
    {
        return rowset;
    }

    public boolean isLoadBeforeDelete()
    {
        return loadBeforeDelete;
    }

    public void setLoadBeforeDelete(boolean loadBeforeDelete)
    {
        this.loadBeforeDelete = loadBeforeDelete;
    }
    
    @Deprecated
    public SessionPersistance getPersistance()
    {
        switch(getPersistence())
        {
            case Key: return SessionPersistance.Key;
            case Data: return SessionPersistance.Data;
            default: return SessionPersistance.None; 
        }
    }
    
    // ------- Methods -------
    
    /**
     * creates a new record.<BR/>
     * Depending on the persistence setting the record key or the record data will be stored on the session.
     * @return true if the record was successfully created or false otherwise
     */
    public boolean createRecord()
    {
        try {
            // initNew
            record.create(rowset, action.getConnection());
            // Save Record Key Info
            persistOnSession();
            return true;
            
        } catch(Exception e) {
            // Action failed
            action.setActionError(e);
            return false;
        }
    }
    
    /**
     * sets all required foreign keys for this record.<BR/>
     * The foreign key values must be supplied with the request.
     * @return true if all required foreign keys have been successfully set, or false otherwise
     */
    public void initReferenceColumns()
    {
        // set Reference Values (if provided)
        Map<DBColumn, DBColumn> refs = rowset.getColumnReferences();
        if (refs!=null)
        {   // Get Parent Columns from Request (if provided)
            for (DBColumn column : refs.keySet())
            {   // Parent Column
                String name  = column.getName();
                String value = action.getRequestParam(name);
                if (value!=null)
                {
                    if (StringUtils.isNotEmpty(value))
                        record.setValue(column, value);
                }
                else if (column.isRequired())
                {   // Reference column not provided
                    log.warn("Value for reference column has not been provided!");
                    throw new ItemNotFoundException(name);
                }
            }
        }
    }
    
    /**
     * loads the record identified by the supplied key from the database<BR/>
     * @return true if the record has been successfully reloaded or false otherwise
     */
    public void loadRecord(Object[] recKey)
    {
        // Check Key
        if (recKey==null || recKey.length==0)
        {   // Invalid Record key
            throw new InvalidKeyException(rowset, recKey);
        }
        // Record laden
        record.read(rowset, recKey, action.getConnection());
        // Save Record Key Info
        persistOnSession();
    }
    
    /**
     * loads the record either from the supplied item key on the request or from the session.<BR/>
     * @return true if the record has been successfully reloaded or false otherwise
     */
    public boolean loadRecord()
    {   // Load 
        try {
            Object[] key = getActionParamKey();
            if (key==null && (persistence==SessionPersistence.Data))
            {   // reload session record
                reloadRecord();
                return true;
            }
            // Load Record
            loadRecord(key);
            return true;

        } catch(Exception e) {
            // Action failed
            action.setActionError(e);
            return false;
        }
    }
    
    /**
     * reloads the current record from the session.<BR/>
     * If persistence is set to Key then the key is obtained from the session and the record 
     * is reloaded from the database. 
     * @return true if the record has been successfully reloaded or false otherwise
     */
    public void reloadRecord()
    {   // Load 
        switch(persistence)
        {
            // Key persistence
            case Key:
            {   // Load from session key
                String stKey = StringUtils.toString(action.getActionObject(getRecordPropertyName()));
                Object[] key = action.getRecordKeyFromString(stKey);
                loadRecord(key);
            }
            // Data persistence
            case Data:
            {   // get record object from session
                Record rec = getRecordFromSession();
                if (rec!=null && (rec instanceof DBRecord))
                {   // Check rowset
                    if (((DBRecord)rec).getRowSet()!=rowset)
                        throw new ObjectNotValidException(rec);
                    // Record restored
                    this.record = (DBRecord)rec;
                }
                // Record not found
                throw new ItemNotFoundException(rowset.getName());
            }
            // Other
            default:
                throw new NotSupportedException(this, "reloadRecord[] " + String.valueOf(persistence));
        }
    }
    
    /**
     * deletes the record identified by the supplied key from the database.
     * @param recKey the record key
     * @param newRec flag indicating whether it is a new unsaved record.
     * @return true if the record has been successfully deleted
     */
    public boolean deleteRecord(Object[] recKey, boolean newRec)
    {
        try {
            // Check Key
            if (recKey==null || recKey.length==0)
            {   // Invalid Record Key
                throw new InvalidKeyException(rowset, recKey);
            }
            if (newRec)
            { 	// Record has not been saved yet!
                record.close();
                return true;
            }
            // Delete Record
            if (loadBeforeDelete)
            {   // load record and delete afterwards
                record.read(rowset, recKey, action.getConnection());
                record.delete(action.getConnection());
            }
            else
            {   // rowset error
                rowset.deleteRecord(recKey, action.getConnection());
            }
            // Success
            removeFromSession();
            return true;

        } catch(Exception e) {
            // Action failed
            action.setActionError(e);
            return false;
        }
    }

    /**
     * deletes the current record database.
     * @return true if the record has been successfully deleted
     */
    public final boolean deleteRecord()
    {
        // Get Record Key
        Object[] recKey = getActionParamKey();
        boolean  newRec = getActionParamNewFlag();
        return deleteRecord(recKey, newRec);
    }
    
    /**
     * This function load all form date from the request into the record
     * for each record column the following steps are taken
     * 1. Detects the control type of the column
     * 2. Let's the corresponding InputControl read, parse and validate the value from the request
     * 3. If a field error occurred the error is stored on the action using action.setFieldError(...)
     * 4. Stores either the parsed or - in case of an error - the request value in the record.  
     * 
     * This procedure does not stop if a field error occurs.
     * Use Action.hasActionError() or Action.getFieldErrors()
     * to determine whether a field error has occurred.
     * 
     * @return true if the record could be loaded and the form data has been filled in
     */
    public boolean loadFormData(Object[] recKey, boolean insert)
    {
        try {
            // Check Key
            if (recKey==null || recKey.length==0)
            {   // Invalid Record key
                throw new InvalidKeyException(rowset, recKey);
            }
            // Prepare Update
            Connection conn = action.getConnection();
            initUpdateRecord(recKey, insert, conn);
            // Set Update Fields
            setUpdateFields(record);
            // Done
            persistOnSession();
            return true;

        } catch(Exception e) {
            // Action failed
            action.setActionError(e);
            return false;
        }
    }

    @Override
    public boolean loadFormData()
    {
        // Get Record Key
        Object[] recKey = getActionParamKey();
        boolean  insert = getActionParamNewFlag();
        return loadFormData(recKey, insert);
    }
    
    /**
     * Updates the record by calling onUpdateRecord and updates the currentKey
     * The update will not be committed, hence the caller must commit or rollback
     * the operation
     * 
     * @return true if the update was successful otherwise false
     */
    public boolean saveChanges()
    {
        try {
            // Record is not valid
            if (record.isValid()==false)
            {   
                log.error("Cannot save changes: record ist not valid");
                throw new ObjectNotValidException(record);
            }
            // Update Record
            updateRecord(action.getConnection());
            // Save Record Key Info
            persistOnSession();
            return true;

        } catch(Exception e) {
            // Action failed
            action.setActionError(e);
            return false;
        }
    }
    
    /**
     * Closes the record and releases any allocated session objects
     */
    public void closeRecord()
    {
        record.close();
        removeFromSession();
    }
    
    public DBRecord detachRecord()
    {
        DBRecord rec = record;
        record = null;
        return rec;
    }

    // --------------------------- useful helpers --------------------------------
    
    public final List<DBIndex> findChangedIndexes()
    {   // Must be a table to do this
        if ((rowset instanceof DBTable)==false)
            return null;
        // Check Record
        if (record.isValid()==false || record.isModified()==false)
            return null;
        // See which Index has changed
        DBTable table = (DBTable)rowset;
        List<DBIndex> avail = table.getIndexes();
        if (avail==null)
            return null;
        List<DBIndex> changed = null;
        for (DBIndex idx : avail)
        {   // Iterate through all indexes
            DBColumn[] idxColumns = idx.getColumns();
            for (int i=0; i<idxColumns.length; i++)
            {   // Check if column has changed
                if (record.wasModified(idxColumns[i]))
                {   // Yes, column has changed
                    if (changed == null)
                        changed = new ArrayList<DBIndex>();
                    changed.add(idx);
                    break;
                }
            }
        }
        return changed;
    }
    
    public final Object[] findAnyConflictRecord()
    {
        // Get list of changed indexes
        List<DBIndex> changed = findChangedIndexes();
        if (changed==null)
            return null; // No Conflicts
        // Iterate through all changed indexes
        DBColumn[] keyColumns = rowset.getKeyColumns();
        for (DBIndex idx : changed)
        {
            // Select all key columns
            DBCommand cmd = rowset.getDatabase().createCommand();
            cmd.select(keyColumns);
            // add constraints
            boolean allNull = true;
            DBColumn[] idxColumns = idx.getColumns();
            for (int i=0; i<idxColumns.length; i++)
            {   // Check if column has changed
                Object value = record.getValue(idxColumns[i]);
                cmd.where(idxColumns[i].is(value));
                if (value!=null)
                    allNull = false;
            }
            // Check whether all constraints are null
            if (allNull)
                continue; 
            // Exclude current record
            if (record.isNew()==false)
            {   // add restriction
                if (keyColumns.length>1)
                {   // Multiple key columns
                    Object value = record.getValue(keyColumns[0]);
                    DBCompareExpr notExpr = keyColumns[0].is(value);
                    for (int i=1; i<keyColumns.length; i++)
                    {   // Check if column has changed
                        cmd.where(keyColumns[i].is(value));
                    }
                    cmd.where(notExpr.not());
                }
                else
                {   // Single key column
                    Object value = record.getValue(keyColumns[0]);
                    cmd.where(keyColumns[0].isNot(value));
                }
            }
            // Query now
            DBReader reader = new DBReader();
            try { 
                reader.getRecordData(cmd, action.getConnection());
                // We have found a record
                Object[] key = new Object[keyColumns.length];
                for (int i=0; i<keyColumns.length; i++)
                {   // Check if column has changed
                    key[i] = reader.getValue(i);
                }
                return key;
            } catch(QueryNoResultException e) {
                // ignore
            } finally {
                reader.close();
            }
        }
        // No, no conflicts
        return null;
    }
    
    // --------------------------- protected --------------------------------

    /**
     * overridable: onUpdateRecord
     */
    protected void updateRecord(Connection conn)
    {
        // Modified?
        if (!record.isModified())
            return;
        // Missing defaults?
        record.fillMissingDefaults(null);
        // Update Record
        record.update(conn);
    }

    // --------------------------- overrides --------------------------------

    @Override
    protected boolean setRecordFieldValue(int i, Object value, boolean verify)
    {
        if (verify)
        {   // Set Value with checking
            try {
                record.setValue(i, value);
                return true;
            } catch(EmpireException e) {
                log.info("setRecordFieldValue failed. Message is {}.", e.getMessage());
                return false;
            }
        }
        else
        {   // No Checking
            record.modifyValue(i, value);
            return true;
        }
    }
    
    // --------------------------- private --------------------------------
    
    /**
     * this function prepared (initialize) a record to insert or update them to
     * the database
     * 
     * @param rowset
     *            the current DBRowSet object
     * @param request
     *            the current request object
     * @param rec
     *            the DBRecord object, consist all fields and the field
     *            properties
     * @param recKey
     *            the primary key(s)
     * @param insert
     *            true if insert sql statement
     */
    private void initUpdateRecord(Object[] keyValues, boolean insert, Connection conn)
    { // Get the record key
        DBColumn[] keyColumns = rowset.getKeyColumns();
        if (keyColumns == null || keyColumns.length < 1)
            throw new NoPrimaryKeyException(rowset);
        if (keyValues == null || keyValues.length != keyColumns.length)
            throw new InvalidKeyException(rowset, keyValues);
        // Get Persistent record
        if (persistence==SessionPersistence.Data)
        {   // Get the record from the session
            Record rec = getRecordFromSession();
            if (rec==null || (rec instanceof DBRecord)==false)
            {   // Record restored
                throw new InvalidFormDataException();
            }
            // Record not found
            record = (DBRecord)rec;
        }
        // Check Record State
        if (record.isValid())
        {   // Is this the record we require?
            Object[] currentKey = record.getKeyValues();
            if (compareKey(currentKey, keyValues)==false)
            {   // Keys don't match
                throw new InvalidFormDataException();
            }
            // We have a valid record
            return;
        }
        // Insert
        if (insert)
        { // Initialize the Record
            record.init(rowset, keyValues, insert);
            // Add the record
            // rec.state = DBRecord.REC_NEW;
            log.debug("Record '" + rowset.getName() + "' prepared for Insert!");
        } else
        { // Read the record from the db
            record.read(rowset, keyValues, conn);
            // Record has been reloaded
            log.debug("Record '" + rowset.getName() + "' prepared for Update!");
        }
    }
    
}
