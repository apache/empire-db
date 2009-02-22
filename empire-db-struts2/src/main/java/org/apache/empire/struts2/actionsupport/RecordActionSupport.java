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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.empire.commons.Errors;
import org.apache.empire.commons.StringUtils;
import org.apache.empire.data.Record;
import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBCommand;
import org.apache.empire.db.DBErrors;
import org.apache.empire.db.DBIndex;
import org.apache.empire.db.DBReader;
import org.apache.empire.db.DBRecord;
import org.apache.empire.db.DBRowSet;
import org.apache.empire.db.DBTable;
import org.apache.empire.db.expr.compare.DBCompareExpr;
import org.apache.empire.struts2.web.WebErrors;


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
    protected static Log log = LogFactory.getLog(RecordActionSupport.class);

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
        // initNew
        if (!record.create(rowset, action.getConnection()))
            return error(record);
        // Save Record Key Info
        persistOnSession();
        return success();
    }
    
    /**
     * sets all required foreign keys for this record.<BR/>
     * The foreign key values must be supplied with the request.
     * @return true if all required foreign keys have been successfully set, or false otherwise
     */
    public boolean initReferenceColumns()
    {
        // set Reference Values (if provided)
        this.clearError();
        Map<DBColumn, DBColumn> refs = rowset.getColumnReferences();
        if (refs!=null)
        {   // Get Parent Columns from Request (if provided)
            for (DBColumn column : refs.keySet())
            {   // Parent Column
                String name  = column.getName();
                String value = action.getRequestParam(name);
                if (value!=null)
                {
                    if (StringUtils.isValid(value))
                        record.setValue(column, value);
                }
                else if (column.isRequired())
                {   // Reference column not provided
                    log.warn("Value for reference column has not been provided!");
                    error(Errors.ItemNotFound, name);
                }
            }
        }
        return (hasError()==false);
    }
    
    /**
     * loads the record identified by the supplied key from the database<BR/>
     * @return true if the record has been successfully reloaded or false otherwise
     */
    public boolean loadRecord(Object[] recKey)
    {
        // Check Key
        if (recKey==null || recKey.length==0)
        {   // Invalid Record key
            return error(DBErrors.RecordInvalidKey, recKey);
        }
        // Record laden
        if (record.read(rowset, recKey, action.getConnection()) == false)
        {   // error
            return error(record);
        }
        // Save Record Key Info
        persistOnSession();
        return success();
    }
    
    /**
     * loads the record either from the supplied item key on the request or from the session.<BR/>
     * @return true if the record has been successfully reloaded or false otherwise
     */
    public boolean loadRecord()
    {   // Load 
        Object[] key = getActionParamKey();
        if (key==null && (persistence==SessionPersistence.Data))
        {   // reload session record
            return reloadRecord();
        }
        // Load Record
        return loadRecord(key);
    }
    
    /**
     * reloads the current record from the session.<BR/>
     * If persistence is set to Key then the key is obtained from the session and the record 
     * is reloaded from the database. 
     * @return true if the record has been successfully reloaded or false otherwise
     */
    public boolean reloadRecord()
    {   // Load 
        switch(persistence)
        {
            // Key persistence
            case Key:
            {   // Load from session key
                String stKey = StringUtils.toString(action.getActionObject(getRecordPropertyName()));
                Object[] key = action.getRecordKeyFromString(stKey);
                return loadRecord(key);
            }
            // Data persistence
            case Data:
            {   // get record object from session
                Record rec = getRecordFromSession();
                if (rec!=null && (rec instanceof DBRecord))
                {   // Check rowset
                    if (((DBRecord)rec).getRowSet()!=rowset)
                        return error(Errors.ObjectNotValid, "record");
                    // Record restored
                    this.record = (DBRecord)rec;
                    return success();
                }
                // Record not found
                return error(Errors.ItemNotFound, rowset.getName());
            }
            // Other
            default:
                return error(Errors.NotSupported, "reloadRecord[] " + String.valueOf(persistence));
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
        // Check Key
        if (recKey==null || recKey.length==0)
        {   // Invalid Record Key
            return error(DBErrors.RecordInvalidKey, recKey);
        }
        if (newRec)
        { 	// Record has not been saved yet!
            record.close();
            return success();
        }
        // Delete Record
        if (loadBeforeDelete)
        {   // load record and delete afterwards
            if (record.read(rowset, recKey, action.getConnection()) == false ||
                record.delete(action.getConnection()) == false)
            {   // error
                return error(record);
            }
        }
        else if (rowset.deleteRecord(recKey, action.getConnection()) == false)
        {   // rowset error
            return error(rowset);
        }
        // Success
        removeFromSession();
        return success();
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
        // Check Key
        if (recKey==null || recKey.length==0)
        {   // Invalid Record key
            return error(DBErrors.RecordInvalidKey, recKey);
        }
        // Prepare Update
        Connection conn = action.getConnection();
        if (!initUpdateRecord(recKey, insert, conn))
            return false;
        // Set Update Fields
        if (!setUpdateFields(record))
            return false;
        // Done
        persistOnSession();
        return success();
    }

    @Override
    public boolean loadFormData()
    {
        clearError();
        // Get Record Key
        Object[] recKey = getActionParamKey();
        boolean  insert = getActionParamNewFlag();
        return loadFormData(recKey, insert);
    }
    
    /**
     * Updates the record by calling onUpdateRecord and updates the currentKey
     * The update will not be commited, hence the caller must commit or rollback
     * the operation
     * 
     * @return true if the update was successful otherwise false
     */
    public boolean saveChanges()
    {
        // Record is not valid
        if (record.isValid()==false)
        {   
            log.error("Cannot save changes: record ist not valid");
            return error(Errors.ObjectNotValid, record.getClass().getName());
        }
        // Update Record
        if (updateRecord(action.getConnection())==false)
        {
            log.error("Error updating record." + getErrorMessage());
            return false;
        }
        // Save Record Key Info
        persistOnSession();
        return success();
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
                if (reader.getRecordData(cmd, action.getConnection()))
                {   // We have found a record
                    Object[] key = new Object[keyColumns.length];
                    for (int i=0; i<keyColumns.length; i++)
                    {   // Check if column has changed
                        key[i] = reader.getValue(i);
                    }
                    return key;
                }
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
    protected boolean updateRecord(Connection conn)
    {
        // Modified?
        if (!record.isModified())
            return success();
        // Missing defaults?
        record.fillMissingDefaults(null);
        // Update Record
        return (record.update(conn) ? success() : error(record));
    }

    // --------------------------- overrides --------------------------------

    @Override
    protected boolean setRecordFieldValue(int i, Object value, boolean verify)
    {
        if (verify)
        {   // Set Value with checking
            return record.setValue(i, value);
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
     * @retrun true if successfull otherwise false
     */
    private boolean initUpdateRecord(Object[] keyValues, boolean insert, Connection conn)
    { // Get the record key
        DBColumn[] keyColumns = rowset.getKeyColumns();
        if (keyColumns == null || keyColumns.length < 1)
            return error(DBErrors.NoPrimaryKey, rowset.getName());
        if (keyValues == null || keyValues.length != keyColumns.length)
            return error(DBErrors.RecordInvalidKey, keyValues, "keyValues");
        // Get Persistent record
        if (persistence==SessionPersistence.Data)
        {   // Get the record from the session
            Record rec = getRecordFromSession();
            if (rec==null || (rec instanceof DBRecord)==false)
            {   // Record restored
                return error(WebErrors.InvalidFormData);
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
                return error(WebErrors.InvalidFormData);
            }
            // We have a valid record
            return success();
        }
        // Insert
        if (insert)
        { // Initialize the Record
            if (!record.init(rowset, keyValues, insert))
                return error(record);
            // Add the record
            // rec.state = DBRecord.REC_NEW;
            log.debug("Record '" + rowset.getName() + "' prepared for Insert!");
        } else
        { // Read the record from the db
            if (!record.read(rowset, keyValues, conn))
                return error(record);
            // Record has been reloaded
            log.debug("Record '" + rowset.getName() + "' prepared for Update!");
        }
        // Done
        return success();
    }
    
}
