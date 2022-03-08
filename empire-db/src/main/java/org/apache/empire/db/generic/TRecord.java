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

public class TRecord<CTX extends DBContext, T extends DBRowSet> extends DBRecord
{
    private static final long serialVersionUID = 1L;
    
    public final T T;   // provide access to RowSet via T
    
    public final CTX CTX; // provide access to Context via CTX

    /**
     * Internal constructor for TRecord
     * May be used by derived classes to provide special behaviour
     */
    protected TRecord(CTX context, T rowset, boolean enableRollbackHandling)
    {   
        super(context, rowset, enableRollbackHandling);
        // set the rowset for quick access
        this.T = rowset;
        this.CTX = context;
    }

    /**
     * Constructs a new TRecord.<BR>
     * @param context the DBContext for this record
     * @param rowset the corresponding RowSet(Table, View, Query, etc.)
     */
    public TRecord(CTX context, T rowset)
    {
        super(context, rowset);
        // set the rowset for quick access
        this.T = rowset;
        this.CTX = context;
    }
    
    /**
     * finally we know the Context class
     */
    @Override
    public CTX getContext()
    {
        return this.CTX;
    }
    
    /**
     * finally we know the RowSet class
     */
    @Override
    public T getRowSet()
    {
        return this.T;
    }
}
