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
package org.apache.empire.vue.sample.db.records;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.empire.commons.Options;
import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBRecord;
import org.apache.empire.db.exceptions.FieldValueException;
import org.apache.empire.rest.app.RecordInitException;
import org.apache.empire.rest.app.SampleServiceApp;
import org.apache.empire.rest.app.TextResolver;
import org.apache.empire.rest.json.JsoColumnMeta;
import org.apache.empire.rest.json.JsoRecordData;
import org.apache.empire.vue.sample.db.RecordContext;
import org.apache.empire.vue.sample.db.SampleTable;


public abstract class SampleRecord<T extends SampleTable> extends DBRecord {

	private static final long serialVersionUID = 1L;
	
	protected final T T; // The table
	protected final RecordContext recordContext;

	public SampleRecord(T table, RecordContext recordContext) {
		super(recordContext, table);
		this.T = table;
		this.recordContext = recordContext;
	}

	/**
	 * Returns the table this record is based upon.
	 * @return The table this record is based upon.
	 */
	public T getTable() {
		return T;
	}

    public RecordContext getRecordContext()
    {
        return recordContext;
    }
    
    public JsoColumnMeta[] getMeta()
    {
        List<DBColumn> columns = T.getColumns();
        JsoColumnMeta[] meta = new JsoColumnMeta[columns.size()]; 
        TextResolver txtres = SampleServiceApp.instance().getTextResolver(Locale.ENGLISH);
        boolean readOnly = isReadOnly();
        for (int i=0; i<meta.length; i++)
        {
            DBColumn col = columns.get(i);
            if (this.isFieldVisible(col)==false)
                continue;
            // get Meta
            Options  opt = this.getFieldOptions(col);
            boolean disabled = this.isFieldReadOnly(col);
            boolean required = this.isFieldRequired(col);
            meta[i] = new JsoColumnMeta(col, txtres, opt, readOnly, disabled, required);
        }
        return meta;
    }
    
    public void init(JsoRecordData data, boolean newRecord)
    {
        // build the key
        DBColumn[] kc = T.getKeyColumns();
        Object[] key = new Object[kc.length];
        for (int i=0; i<kc.length; i++)
        {   // set key values
            key[i] = data.getValue(kc[i]);
        }
        // load original record
        if (newRecord)
        {   // init a new record
            super.init(T, key, true);
        }
        else
        {   // read the current record
            super.read(T, key, recordContext.getConnection());
        }
        // set all fields
        List<FieldValueException> exptns = new ArrayList<FieldValueException>(0);
        for (DBColumn c : T.getColumns())
        {   // skip all key columns
            if (T.isKeyColumn(c))
                continue; // already set
            if (c==T.getTimestampColumn())
                continue;
            // get Value 
            String prop = c.getBeanPropertyName();
            if (!data.containsKey(prop))
            {   // not provided
                continue; 
            }
            Object value = data.getValue(prop, c.getDataType());
            // set Value
            try {
                // set the value
                this.setValue(c, value);
            } catch(FieldValueException e) {
                // add exception to list
                exptns.add(e);
            }
        }
        // Exceptions?
        if (!exptns.isEmpty())
        {   // Init failed with exceptions
            throw new RecordInitException(exptns);
        }
    }
}