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

import java.util.Set;

import org.apache.empire.data.DataType;

public class DBCmdParam extends DBExpr
{
    private final static long serialVersionUID = 1L;
    protected DBCommand cmd;
    protected DataType  type;
    protected Object    value;
    
    protected DBCmdParam(DBCommand cmd, DataType type, Object value)
    {
        this.cmd = cmd;
        this.type = type;
        this.value = getCmdParamValue(value);
    }
    
    protected Object getCmdParamValue(Object value)
    {        
        switch (type)
        {
            case BLOB:
                if (value == null)
                    return null;
                if (value instanceof DBBlobData)
                    return value;
                if (value instanceof byte[])
                    return new DBBlobData((byte[])value);
                // create a blob data
                return new DBBlobData(value.toString());
            case CLOB:
                if (value == null)
                    return null;
                if (value instanceof DBClobData)
                    return value;
                // create a clob data
                return new DBClobData(value.toString());
            default:
                return value;
        }
    }
    
    @Override
    public void addSQL(StringBuilder buf, long context)
    {
        buf.append("?"); //$NON-NLS-1$
        // Move to current usage position
        cmd.notifyParamUsage(this);
    }
    
    /**
     * @see org.apache.empire.db.DBExpr#addReferencedColumns(Set)
     */
    @Override
    public void addReferencedColumns(Set<DBColumn> list)
    {
        // Nothing to add
    }
    
    @Override
    public DBDatabase getDatabase()
    {
        return cmd.getDatabase();
    }
    
    public DataType getDataType()
    {
        return type;
    }
    
    public Object getValue()
    {
        return value;
    }
    
    public void setValue(Object value)
    {
        this.value = getCmdParamValue(value);
    }
}
