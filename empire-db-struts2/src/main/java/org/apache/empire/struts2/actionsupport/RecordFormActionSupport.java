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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.data.Column;
import org.apache.empire.data.DataType;
import org.apache.empire.data.Record;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.exceptions.EmpireException;
import org.apache.empire.exceptions.ObjectNotValidException;
import org.apache.empire.struts2.action.ErrorInfo;
import org.apache.empire.struts2.jsp.controls.InputControl;
import org.apache.empire.struts2.jsp.controls.InputControlManager;
import org.apache.empire.struts2.web.FieldErrors;


public abstract class RecordFormActionSupport extends FormActionSupport
{
    protected SessionPersistence persistence;  // Session persistence

    protected RecordFormActionSupport(ActionBase action, SessionPersistence persistence)
    {
        this(action, persistence, action.getItemPropertyName());
    }
    
    protected RecordFormActionSupport(ActionBase action, SessionPersistence persistence, String propertyName)
    {
        super(action, propertyName);
        // Set Persistence
        this.persistence = persistence;
    }

    public SessionPersistence getPersistence()
    {
        return persistence;
    }

    public abstract Record getRecord();

    public String getRecordKeyString()
    {
        Record record = getRecord();
        if (record==null || record.isValid()==false)
        {   // Check whether we can get the record key from the request or the session
            String property = getRecordPropertyName();
            String key = getActionParam(property, persistence==SessionPersistence.Key);
            if (key!=null || (persistence!=SessionPersistence.Data))
                return key; // Return key or null
            // Get Record from the session
            record = getRecordFromSession();
            if (record==null)
                return null; // No Record Provided
            // We have a record
            // setRecord(record);
        }
        return action.getRecordKeyString(record);
    }
    
    public boolean isNewRecord()
    {
        Record record = getRecord();
        if (record==null || record.isValid()==false)
        {   // Check whether we can get the record key from the request or the session
            String property = getRecordPropertyName();
            String key = getActionParam(property, persistence==SessionPersistence.Key);
            if (key!=null)
                return action.getRecordNewFlagFromString(key); // Return key or null
            if (persistence!=SessionPersistence.Data)
                return false; // Unknown
            // Get Record from the session
            record = getRecordFromSession();
            if (record==null)
                return false; // No Record Provided
            // We have a record
            // setRecord(record);
        }
        return record.isNew();
    }
    
    /**
     * returns the name of a field as used in the form
     * @param column
     * @return the form name of the record field
     */
    public String getRequestFieldName(Column column)
    {
        String name = column.getName();
        if (propertyName!=null && propertyName.length()>0)
            return propertyName + "." + name; 
        return name;
    }
    
    // ------- persistence -------
    
    protected Record getRecordFromSession()
    {
        // Check if session persistence is enabled
        if (persistence!=SessionPersistence.Data)
            return null; 
        // GetRecord
        Object rec = action.getActionObject(getRecordPropertyName() + ".data");
        if (rec==null || !(rec instanceof Record))
        {   // Record has not been stored on session
            return null; 
        }
        // Done
        return (Record)rec;
    }
    
    protected void persistOnSession()
    {
        // Clear Item or data
        if (persistence==SessionPersistence.Key)
        {
            String key = action.getRecordKeyString(getRecord());
            action.putActionObject(getRecordPropertyName(), key);
        }
        else if (persistence==SessionPersistence.Data)
        {
            action.putActionObject(getRecordPropertyName() + ".data", getRecord());
        }
    }
    
    protected void removeFromSession()
    {
        // Clear Item or data
        if (persistence==SessionPersistence.Key)
        {
            action.removeActionObject(getRecordPropertyName());
        }
        else if (persistence==SessionPersistence.Data)
        {
            action.removeActionObject(getRecordPropertyName() + ".data");
        }
    }

    // --------------------------- public --------------------------------

    /**
     * Checks wether or not the record key is supplied
     * @param acceptSessionKey true if a key supplied on the session is acceptable or false otherwise 
     * @return true if the record has a bean associated with it or false otherwiese
     */
    public boolean hasActionKey(boolean acceptSessionKey)
    {
        String property = getRecordPropertyName();
        String param = getActionParam(property, (acceptSessionKey && persistence==SessionPersistence.Key)); 
        return (param!=null);
    }

    /**
     * Returns the record key.
     * The key may be supplied with the request or with the session.
     * @return the record key
     */
    public Object[] getActionParamKey()
    {
        // Get Record Key
        String property = getRecordPropertyName();
        String param = getActionParam(property, (persistence==SessionPersistence.Key)); 
        return action.getRecordKeyFromString(param);
    }
    
    /**
     * Returns a flag whether or not the current record is a new record.
     * @return true if the record is a new unsaved record.
     */
    public boolean getActionParamNewFlag()
    {
        // Get Record Key
        String property = getRecordPropertyName();
        return action.getRecordNewFlagFromString( getActionParam(property, false));
    }

    /**
     * loads the data from the form into the current record object
     * @return true if all fields supplied with the request have been successfully set on the record
     */
    @Override
    public boolean loadFormData()
    {
        try {
            Record record = getRecord();
            if (record.isValid()==false)
                throw new ObjectNotValidException(record);
            // Load Data
            return setUpdateFields(record);

        } catch(Exception e) {
            // Action failed
            action.setActionError(e);
            return false;
        }
    }
    
    // --------------------------- protected --------------------------------
 
    /**
     * overridable: sets the value of single field
     */
    protected boolean setRecordFieldValue(int i, Object value, boolean verify)
    {
        // Check Empty
        if (ObjectUtils.isEmpty(value))
            value = null;
        // Set Value
        try {
            getRecord().setValue(i, value);
            return true;
        } catch(EmpireException e) {
            log.info("setRecordFieldValue failed. Message is {}.", e.getMessage());
            return false;
        }
    }

    /**
     * adds all fields found in the HTTP-JSPRequest for this table to the record
     * 
     * @param record
     *            the Record object, contains all fields and the field
     *            properties
     * @return true if all values have been set successfully or otherwise false
     */
    protected boolean setUpdateFields(Record record)
    { // Set all field Values
        boolean valid = true;
        Locale locale = action.getLocale();
        String sysdate = DBDatabase.SYSDATE.toString();
        Column[] keyColumns = record.getKeyColumns();
        // Pull all field values
        int fieldCount = record.getFieldCount();
        for (int i = 0; i < fieldCount; i++)
        {
            Column col = record.getColumn(i);
            // Check wether it is a key column
            if (ObjectUtils.contains(keyColumns, col))
                continue;
            // Get the value from the input control
            Object value = null; 
            String field = getRequestFieldName(col);
            InputControl control = null;
            if (record.isFieldReadOnly(col)==false)
                control = InputControlManager.getControl(col.getControlType());
            // Get Value from Control first
            if (control!=null && (value=control.getFieldValue(field, action, locale, col))!=null)
            {   // Check for Error
                if (value instanceof InputControl.FieldValueError)
                {
                    InputControl.FieldValueError fieldError = (InputControl.FieldValueError)value;
                    // Error
                    String errorValue = fieldError.getValue();
                    addFieldError(field, col, fieldError, errorValue);
                    setRecordFieldValue(i, errorValue, false);
                    valid = false;
                    continue;
                }
                // Check Value
                if (value.equals(InputControl.NULL_VALUE) && col.isRequired())
                {   // Oops, columns is required
                    InputControl.FieldValueError fieldError = new InputControl.FieldValueError(FieldErrors.InputValueRequired, null, "");
                    addFieldError(field, col, fieldError, value);
                    valid = false;
                    continue;
                }
                // set field value now
                if (log.isInfoEnabled())
                    log.info("SetUpdateFields: setting field '" + col.getName() + "' to " + String.valueOf(value));
                if (!setRecordFieldValue(i, value, true))
                {   // Force to set field value
                    if (record instanceof ErrorInfo)
                        addFieldError(field, col, (ErrorInfo)record, value);
                    else
                        addFieldError(field, col, new ActionError(FieldErrors.InputInvalidValue), value);
                    // set Value
                    setRecordFieldValue(i, value, false);
                    valid = false;
                }
            }
            else if ((value=action.getRequestParam(field + "!"))!=null)
            {   // hidden value
                if (col.getDataType().isDate())
                {   // Special for Dates and timestamps
                    if (value.equals(sysdate)==false)
                    {   // Parse Date Time
                        String format = (col.getDataType()==DataType.DATE) ? "yyyy-MM-dd" : "yyyy-MM-dd HH:mm:ss.S";
                        SimpleDateFormat sdf = new SimpleDateFormat(format);
                        try {
                            value = sdf.parseObject(value.toString());
                        } catch(ParseException e) {
                            log.error("Failed to parse date for record", e);
                            continue;
                        }
                    }
                }
                // Set Value
                if (log.isInfoEnabled())
                    log.info("SetUpdateFields: directly setting field '" + col.getName() + "' to " + String.valueOf(value));
                // Has Value changed?
                if (ObjectUtils.compareEqual(record.getValue(i), value)==false)
                {   // Modify Value
                    setRecordFieldValue(i, value, false);
                }
            }
            else 
            {   // value not supplied
                continue;
            }
        }
        // Result
        if (valid == false)
        { // Fehler
            if (log.isInfoEnabled())
                log.info("SetUpdateFields: Failed to modify record! At least one field error!");
            return false;
        }
        return true;
    }
    
    /**
     * this method compares two primary key objects
     *
     * @param currentKey
     *            the current key object
     * @param updateKey
     *            the comparative value
     */
    protected final boolean compareKey(Object[] currentKey, Object[] updateKey)
    {   // Compare Keys
        if (currentKey==null || currentKey.length!=updateKey.length)
            return false;
        // Compare Key Values
        for (int i = 0; i < currentKey.length; i++)
        {   // Check String Values
            if (!ObjectUtils.compareEqual(currentKey[i], updateKey[i]))
                return false;
        }
        return true;
    }
    
}
