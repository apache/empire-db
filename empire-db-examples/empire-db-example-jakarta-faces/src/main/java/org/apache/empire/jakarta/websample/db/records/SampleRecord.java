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
package org.apache.empire.jakarta.websample.db.records;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import jakarta.faces.context.FacesContext;

import org.apache.empire.commons.ClassUtils;
import org.apache.empire.db.DBContext;
import org.apache.empire.db.DBRecord;
import org.apache.empire.jakarta.app.FacesUtils;
import org.apache.empire.jakarta.websample.db.SampleTable;
import org.apache.empire.jakarta.websample.web.pages.SamplePage;

public abstract class SampleRecord<T extends SampleTable> extends DBRecord
{
    private static final long   serialVersionUID = 1L;

    /*
     * Store the table for convenience
     */
    protected final transient T T;  /* transient for serialization */

    /**
     * Custom deserialization for transient T.
     */
    private void readObject(ObjectInputStream strm)
        throws IOException, ClassNotFoundException
    {   // Restore T
        @SuppressWarnings("unchecked")
        T table = (T)super.getRowSet();
        ClassUtils.setPrivateFieldValue(SampleRecord.class, this, "T", table);
        // read the rest
        strm.defaultReadObject();
    }

    @Override
    protected void writeContext(ObjectOutputStream strm) throws IOException
    {
        // Nothing
    }
    
    @Override
    protected DBContext readContext(ObjectInputStream strm)  throws IOException, ClassNotFoundException
    {
        SamplePage page = FacesUtils.getPage(FacesContext.getCurrentInstance());
        return page.getSampleContext();
    }

    /**
     * Constructor for SampleRecord
     * @param context
     * @param table
     */
    protected SampleRecord(DBContext context, T table)
    {
        super(context, table);
        this.T = table;
    }

    /**
     * Returns the table this record is based upon.
     * @return The table this record is based upon.
     */
    public T getTable()
    {
        return T;
    }

}
