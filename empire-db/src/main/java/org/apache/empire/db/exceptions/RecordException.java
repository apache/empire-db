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
package org.apache.empire.db.exceptions;

import org.apache.empire.commons.ErrorType;
import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.commons.StringUtils;
import org.apache.empire.data.EntityType;
import org.apache.empire.data.Record;
import org.apache.empire.db.DBRowSet;
import org.apache.empire.exceptions.EmpireException;

public abstract class RecordException extends EmpireException
{
    private static final long serialVersionUID = 1L;
    
    private static final Object[] NO_KEY = new Object[0];
    
    protected static Object[] getKey(Record rec)
    {
        try {
            if (!rec.isValid() || ObjectUtils.isEmpty(rec.getKeyColumns()))
                return NO_KEY;
            return rec.getKey();
        } catch(Exception e) {
            return NO_KEY;
        }
    }
    
    protected static EntityType getEntityType(Record record)
    {
        try {
            return record.getEntityType();
        } catch(Exception e) {
            return null;
        }
    }
    
    protected static String keyToString(Object[] key)
    {
        return (key==null ? "["+StringUtils.arrayToString(key, "|")+"]" : "[]");
    }
    
    protected static String entityName(EntityType entity)
    {
        return (entity!=null ? entity.getEntityName() : "{Null}"); 
    }
    
    protected static String rowsetName(DBRowSet rowset)
    {
        return (rowset!=null ? StringUtils.coalesce(rowset.getName(), rowset.getAlias()) : "{Null}");        
    }
    
    private transient final EntityType entityType;
    private transient final Object[] key;
    
    public RecordException(EntityType entityType, Object[] key, final ErrorType errType, final String[] params, final Throwable cause)
    {
        super(errType, params, cause);
        // save type and params for custom message formatting
        this.entityType = entityType;
        this.key = key;
    }

    public RecordException(EntityType entity, Object[] key, final ErrorType errType, final String[] params)
    {
        this(entity, key, errType, params, null);
    }
    
    public RecordException(final Record record, final ErrorType errType, final String[] params)
    {
        this(getEntityType(record), getKey(record), errType, params, null);
    }

    public EntityType getEntityType()
    {
        return entityType;
    }

    public Object[] getKey()
    {
        return key;
    }
}
