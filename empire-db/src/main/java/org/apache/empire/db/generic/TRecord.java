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
package org.apache.empire.db.generic;

import org.apache.empire.db.DBContext;
import org.apache.empire.db.DBRecord;
import org.apache.empire.db.DBRowSet;

public class TRecord<RS extends DBRowSet> extends DBRecord
{
    private static final long serialVersionUID = 1L;
    
    public final RS RS;

    /**
     * Internal constructor for DBRecord
     * May be used by derived classes to provide special behaviour
     */
    protected TRecord(DBContext context, RS rowset, boolean enableRollbackHandling)
    {   
        super(context, rowset, enableRollbackHandling);
        // set the rowset for quick access
        this.RS = rowset;
    }

    /**
     * Constructs a new DBRecord.<BR>
     * @param context the DBContext for this record
     * @param rowset the corresponding RowSet(Table, View, Query, etc.)
     */
    public TRecord(DBContext context, RS rowset)
    {
        super(context, rowset);
        // set the rowset for quick access
        this.RS = rowset;
    }
    
    /**
     * finally we know the rowset
     */
    @Override
    public RS getRowSet()
    {
        return this.RS;
    }

}
