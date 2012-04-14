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

import java.sql.Connection;

import org.apache.empire.db.DBRecord;
import org.apache.empire.db.DBRowSet;
import org.apache.empire.exceptions.InvalidArgumentException;
import org.apache.empire.exceptions.ObjectNotValidException;
import org.apache.empire.jsf2.pages.Page;
import org.apache.empire.jsf2.pages.PageElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RecordPageElement<T extends DBRecord> extends PageElement
{
    private static final long   serialVersionUID = 1L;

    private static final Logger log              = LoggerFactory.getLogger(RecordPageElement.class);

    protected DBRowSet rowset;

    protected T record;
    
    public RecordPageElement(Page page, DBRowSet rowset, T record, String propertyName)
    {
        super(page, propertyName);
        // Set Rowset and Record
        this.rowset = rowset;
        this.record = record;
        
        log.debug("RecordPageSupport for {} created.", rowset.getName());
    }
    
    public T getRecord()
    {
        return record;
    }
    
    public DBRowSet getRowSet()
    {
        return record.getRowSet();
    }

    @Override
    protected void onInitPage()
    {
        // Chance to init the page
        if (record.isValid()==false)
            reloadRecord();
    }
    
    /**
     * loads the record identified by the supplied key from the database<BR/>
     * @return true if the record has been successfully reloaded or false otherwise
     */
    public void reloadRecord()
    {
        Object[] recKey = getSessionObject(Object[].class);
        // Check Key
        if (recKey==null || recKey.length==0)
        {   // Invalid Record key
            throw new ObjectNotValidException(this);
        }
        // Record laden
        Connection conn = getPage().getConnection(rowset.getDatabase()); 
        record.read(rowset, recKey, conn);
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
            throw new InvalidArgumentException("recKey", recKey);
        }
        // Put key on Session
        this.setSessionObject(Object[].class, recKey);
        // Record laden
        Connection conn = getPage().getConnection(rowset.getDatabase()); 
        record.read(rowset, recKey, conn);
    }

    /**
     * creates a new record
     */
    public void createRecord()
    {
        record.create(rowset);
    }

    /**
     * loads an existing record
     * @param idParam
     */
    public void loadRecord(String idParam)
    {
        Object[] key = getPage().getKeyFromParam(rowset, idParam);

        loadRecord(key);
    }
    
    public String getRecordIdParam()
    {
        if (!record.isValid())
            return null;
        // idParam
        Object[] key = record.getKeyValues();
        return getPage().getIdParamForKey(rowset, key);
    }

}
