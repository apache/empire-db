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
package org.apache.empire.db;

import org.apache.empire.data.Record;

/**
 * DBGeneratedValue
 * Abstract base class for Auto generated values
 * @author doebele
 */
public abstract class DBGeneratedValue extends DBExpr
{
    protected final DBDatabase db;
    
    public DBGeneratedValue(DBDatabase db)
    {
        this.db = db;
    }

    /**
     * Returns true if the value was modified and thus needs to be be updated in the record
     * @param record the record for which to check
     * @return true if the value was modified and needs to be be updated
     */
    public abstract boolean isModified(Record record);

    /**
     * Evaluates a record and returns the generated value
     * @param record
     * @return
     */
    public abstract Object eval(Record record);

    /**
     * @return the current DBDatabase object
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T extends DBDatabase> T getDatabase()
    {
        return (T)this.db;
    }
    
}
