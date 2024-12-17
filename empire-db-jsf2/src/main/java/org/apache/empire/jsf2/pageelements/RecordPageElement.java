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
package org.apache.empire.jsf2.pageelements;

import org.apache.empire.db.DBRecord;
import org.apache.empire.db.DBRowSet;
import org.apache.empire.exceptions.EmpireException;
import org.apache.empire.exceptions.InternalException;
import org.apache.empire.exceptions.InvalidArgumentException;
import org.apache.empire.exceptions.ObjectNotValidException;
import org.apache.empire.jsf2.pages.Page;
import org.apache.empire.jsf2.pages.PageDefinition;
import org.apache.empire.jsf2.pages.PageElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RecordPageElement<T extends DBRecord> extends PageElement<Page>
{
    // *Deprecated* private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(RecordPageElement.class);

    protected final DBRowSet rowset;

    protected T record;
    
    /**
     * Creates a record page element for the given Table or View
     * @param page the page element
     * @param rowset Table or View
     * @param record the record object for this page element
     * @param propertyName the property name which is used to get and retrieve session information
     */
    public RecordPageElement(Page page, DBRowSet rowset, T record, String propertyName)
    {
        super(page, propertyName);
        // Set Rowset and Record
        this.rowset = rowset;
        this.record = record;
        // log
        log.debug("RecordPageSupport for {} created.", rowset.getName());
    }

    /**
     * Creates a record page element for the given Table or View
     * @param page the page element
     * @param rowset Table or View
     * @param record the record object for this page element
     */
    public RecordPageElement(Page page, DBRowSet rowset, T record)
    {
        this(page, rowset, record, getDefaultPropertyName(rowset));
    }
    
    public T getRecord()
    {
        return record;
    }
    
    public DBRowSet getRowSet()
    {
        return record.getRowSet();
    }
    
    public String getRecordIdParam()
    {
        if (!record.isValid())
            return null;
        // idParam
        Object[] key = record.getKey();
        return getPage().getIdParamForKey(rowset, key);
    }

    @Override
    protected void onInitPage()
    {
        // Chance to init the page
        if (record.isValid()==false)
            reloadRecord();
    }

    /**
     * return true if the record can be restored from the session.
     * @param newRecord flag to detect session data for a new reaord otherwise for an existing record
     * @return true if information to restore the record is available on the session
     */
    public boolean canReloadRecord(boolean newRecord)
    {
        if (newRecord)
            return (getSessionObject(DBRecord.class)!=null);
        else
            return (getSessionObject(Object[].class)!=null); 
    }
    
    /**
     * loads the record identified by the supplied key from the database<BR>
     */
    public void reloadRecord()
    {
        Object[] recKey = getSessionObject(Object[].class);
        // Check Key
        if (recKey==null || recKey.length==0)
        {   // Invalid Record key
            @SuppressWarnings("unchecked")
            T rec = (T)getSessionObject(DBRecord.class);
            if (rec!=null)
            {   // A new record
                record = rec;
                return;
            }    
            // Not Valid
            throw new ObjectNotValidException(record);
        }
        // Record laden
        record.read(recKey);
    }
    
    /**
     * loads the record identified by the supplied key from the database
     * @param recKey the record key
     */
    public void loadRecord(Object[] recKey)
    {
        // Check Key
        if (recKey==null || recKey.length==0)
        {   // Invalid Record key
            throw new InvalidArgumentException("recKey", recKey);
        }
        // Put key on Session
        this.removeSessionObject(DBRecord.class);
        this.setSessionObject(Object[].class, recKey);
        // Record laden
        record.read(recKey);
    }

    /**
     * loads an existing record
     * @param idParam the id param
     */
    public void loadRecord(String idParam)
    {
        Object[] key = getPage().getKeyFromParam(rowset, idParam);
        loadRecord(key);
    }
    
    /**
     * loads an existing record the the specified page
     * 
     * @param page the page
     * @param idParam the id param
     */
    public void loadRecord(PageDefinition page, String idParam) {
        Object[] key = getPage().getKeyFromParam(page, rowset, idParam);
        loadRecord(key);
    }
    
    /**
     * creates a new record
     */
    public void createRecord()
    {
        record.create();
        // Put key on Session
        this.removeSessionObject(Object[].class);
        this.setSessionObject(DBRecord.class, record);
    }

    /**
     * updates or inserts the record in the database
     * @return true if the record was modified and successfully udpated
     */
    public boolean updateRecord()
    {
        // Record laden
        try {
            // Check Key
            if (record.isValid()==false)
            {   // Invalid Record key
                throw new ObjectNotValidException(record); 
            }
            // Check Modified
            if (record.isModified()==false)
            {   // Not Modified
                return true; 
            }
            record.update();
            // Put key on Session
            this.removeSessionObject(DBRecord.class);
            this.setSessionObject(Object[].class, record.getKey());
            return true; 
            // OK
        } catch(Exception e) {
            // Wrap exception
            if (!(e instanceof EmpireException))
                e = new InternalException(e);
            // Set error Message
            getPage().setErrorMessage(e);
            return false; 
        }
    }
    
    /**
     * deletes a record
     */
    public void deleteRecord()
    {
        // check valid
        if (!record.isValid())
            throw new ObjectNotValidException(record);
        // delete
        record.delete();
        // Put key on Session
        this.removeSessionObject(Object[].class);
        this.removeSessionObject(DBRecord.class);
    }
    
    /**
     * closes a record
     */
    public void closeRecord()
    {
        record.close();
        // Put key on Session
        this.removeSessionObject(Object[].class);
        this.removeSessionObject(DBRecord.class);
    }

}
