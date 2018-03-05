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

import org.apache.empire.data.DataType;

import java.sql.Connection;

public class MockDriver extends DBDatabaseDriver{
    private final static long serialVersionUID = 1L;
  
    class MockCommand extends DBCommand{
        private final static long serialVersionUID = 1L;
        protected MockCommand(DBDatabase db)
        {
            super(db);
        }
        
    }

    @Override
    public DBCommand createCommand(DBDatabase db)
    {
        return new MockCommand(db);
    }

    @Override
    public String getConvertPhrase(DataType destType, DataType srcType, Object format)
    {
        return null;
    }

    @Override
    public Object getNextSequenceValue(DBDatabase db, String SeqName, int minValue, Connection conn)
    {
        return null;
    }

    @Override
    public DBColumnExpr getNextSequenceValueExpr(DBTableColumn column)
    {
        return null;
    }

    @Override
    public String getSQLPhrase(int phrase)
    {
        return null;
    }

    @Override
    public boolean isSupported(DBDriverFeature type)
    {
        return false;
    }
    
}