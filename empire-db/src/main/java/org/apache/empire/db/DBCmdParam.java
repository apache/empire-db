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

import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.data.DataType;

/**
 * This class defines a parameter for a prepared statement query.
 * Do not create instances of this class yourself, rather use DBCommand.addParam(...)
 * @author Rainer
 */
public class DBCmdParam extends DBExpr
{
    private final static long serialVersionUID = 1L;
    protected DBCommand cmd;
    protected DataType  type;
    protected Object    value;

    /**
     * Protected constructor used e.g. by DBCommand.addParam(...) 
     * @param cmd the command to which this DBCommand belongs to
     * @param type the parameter data type
     * @param value the initial value (can be changed any time by calling setValue(...))
     */
    protected DBCmdParam(DBCommand cmd, DataType type, Object value)
    {
        this.cmd = cmd;
        this.type = type;
        this.value = getCmdParamValue(value);
    }

    /**
     * Returns the internal parameter value for a given "real" value.  
     * Depending on the data type this might involve wrapping the real value with another class.
     * This is done e.g. for CLOBs and BLOBs 
     * @param value the "real" value
     * @return the (possibly wrapped) value
     */
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
            case BOOL:
            	return ObjectUtils.getBoolean(value);
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
    
    /**
     * Returns the data type of the command parameter
     * @return the data type
     */
    public DataType getDataType()
    {
        return type;
    }
    
    /**
     * Returns the current value of the parameter.
     * In some cases (as for CLOBs and BLOBs) this might return a Wrapper class for the underlying value.  
     * @return the current (possibly wrapped) value
     */
    public Object getValue()
    {
        return value;
    }
    
    /**
     * Sets the current value of the parameter
     * @param value the new value
     */
    public void setValue(Object value)
    {
        this.value = getCmdParamValue(value);
    }
}
