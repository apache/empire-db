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

import java.util.List;

import org.apache.empire.commons.Errors;
import org.apache.empire.data.Column;
import org.apache.empire.data.Record;
import org.apache.empire.data.bean.BeanClass;
import org.apache.empire.data.bean.BeanRecordProxy;
import org.apache.empire.struts2.web.WebErrors;


/**
 * BeanActionSupport
 * <p>
 * This class provides functions for form data processing through ordinary JavaBean or Data Transfer Objects (DTO).   
 * Metadata for the Beans should be provided using the BeanClass and BeanProperty classes.<br>
 * It is recommended (but not necessary) to create a subclass of the BeanRecordProxy<T> class in order to provide
 * further context specific metadata.
 * </p>
 * @author Rainer
 */
public class BeanActionSupport<T> extends RecordFormActionSupport
{
    private BeanRecordProxy<T> record;

    // ------- Constructors using BeanRecordProxy -------
    
    public BeanActionSupport(ActionBase action, BeanRecordProxy<T> record, SessionPersistence persistence, String propertyName)
    {
        super(action, persistence, propertyName);
        // Set Class
        this.record = record;
    }

    public BeanActionSupport(ActionBase action, BeanRecordProxy<T> record, SessionPersistence persistence)
    {
        this(action, record, persistence, action.getItemPropertyName());
    }
    
    // ------- Constructors using BeanClass -------
    
    public BeanActionSupport(ActionBase action, BeanClass beanClass, SessionPersistence persistence, String propertyName)
    {
        this(action, new BeanRecordProxy<T>(beanClass), persistence, propertyName);
    }
    
    public BeanActionSupport(ActionBase action, BeanClass beanClass, SessionPersistence persistence)
    {
        this(action, new BeanRecordProxy<T>(beanClass), persistence, action.getItemPropertyName());
    }

    // ------- Constructors using Column definitions -------
    
    public BeanActionSupport(ActionBase action, List<Column> updateColumns, Column[] keyColumns, SessionPersistence persistence, String propertyName)
    {
        this(action, new BeanRecordProxy<T>(updateColumns, keyColumns), persistence, propertyName);
    }

    public BeanActionSupport(ActionBase action, List<Column> updateColumns, Column[] keyColumns, SessionPersistence persistence)
    {
        this(action, updateColumns, keyColumns, persistence, action.getItemPropertyName());
    }

    public BeanActionSupport(ActionBase action, List<Column> updateColumns, Column keyColumn, SessionPersistence persistence)
    {
        this(action, updateColumns, new Column[] { keyColumn }, persistence, action.getItemPropertyName());
    }
    
    /**
     * returns the Record interface implementation for the bean
     */
    @Override
    @SuppressWarnings("unchecked")
    public Record getRecord()
    {
        if (record.isValid()==false && getPersistence()==SessionPersistence.Data)
            record = (BeanRecordProxy<T>)getRecordFromSession();
        return record;
    }
    
    /**
     * Checks wether or not the record has a bean object attached 
     * @return true if the record has a bean associated with it or false otherwiese
     */
    public boolean isValid()
    {
        return getRecord().isValid();
    }
    
    /**
     * Gets the bean data.
     * @return the bean data object
     */
    public T getData()
    {
        getRecord();
        return record.getBean();
    }
    
    /**
     * Sets the bean data.
     * This can be a new or an existing object.
     * @param data the bean data object
     */
    public void setData(T data)
    {
        record.setBean(data);
        if (record.isValid())
            persistOnSession();
        else
            removeFromSession();
    }
    
    /**
     * Returns the current key values of the bean attached to the record proxy. 
     */
    public Object[] getRecordKeyValues()
    {
        if (isValid()==false)
            return null;
        return record.getKeyValues();        
    }
    
    
    /**
     * Checks wether the key supplied with the request is identical to the key of the current record.
     */
    public boolean checkKey()
    {
        if (isValid()==false)
            return false;
        // The key
        Object[] updKey = getActionParamKey();
        Object[] curKey = getRecordKeyValues();
        return this.compareKey(curKey, updKey);
    }
    
    /**
     * Refreshes the record key stored on the session.
     * This is required for new records when the key values have been set after saving.
     * This function is only required if SessionPersistence.Key has been selected. 
     */
    public void updateSessionKey()
    {
        if (record.isValid())
            persistOnSession();
        else
            removeFromSession();
    }
    
    /**
     * Initializes the key columns of the current record from the action parameters. 
     * @return true if the key columns were set successfully of false otherwise
     */
    public boolean initKeyColumns()
    {
        // Action parameters
        Object[] keyValues = getActionParamKey();
        if (isValid()==false || keyValues==null)
            return error(WebErrors.InvalidFormData);
        // Check Record
        if (record==null || !record.isValid())
            return error(Errors.ObjectNotValid, "record");
        // Check Key Length
        Column[] keyColumns = record.getKeyColumns();
        if (keyValues.length!=keyColumns.length)
            return error(WebErrors.InvalidFormData);
        // Copy values
        for (int i=0; i<keyColumns.length; i++)
        {   
            if (!record.setValue(keyColumns[i], keyValues[i]))
                return error(record);
        }
        // done
        return success();
    }
    
}
