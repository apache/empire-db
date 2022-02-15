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

import org.apache.empire.db.DBTable;

public class TTable<DB extends TDatabase<DB>> extends DBTable
{
    public final DB DB;
    
    public TTable(String name, DB db, String alias)
    { 
        super(name, db, alias);
        // set type
        this.DB = db;
    }

    public TTable(String name, DB db)
    { 
        super(name, db);
        // set type
        this.DB = db;
    }
    
    /**
     * finally we know the database type
     */
    @Override
    public final DB getDatabase()
    {
        return this.DB;
    }

}
