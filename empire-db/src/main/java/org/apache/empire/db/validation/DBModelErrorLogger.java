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
package org.apache.empire.db.validation;

import org.apache.empire.data.DataType;
import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBIndex;
import org.apache.empire.db.DBObject;
import org.apache.empire.db.DBRelation;
import org.apache.empire.db.DBTable;
import org.apache.empire.db.DBView;
import org.apache.empire.db.DBView.DBViewColumn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implemtnation of the {@link DBModelErrorHandler} interface that logs all errors
 */
public class DBModelErrorLogger implements DBModelErrorHandler
{
    private static final Logger log = LoggerFactory.getLogger(DBModelErrorLogger.class);
    
    protected int errorCount;

    protected int warnCount;
    
    public int getErrorCount()
    {
        return errorCount;
    }

    public int getWarnCount()
    {
        return warnCount;
    }
    
    protected void logWarn(String msg, Object...params)
    {
        DBModelErrorLogger.log.warn(msg, params);
    }
    
    protected void logError(String msg, Object...params)
    {
        DBModelErrorLogger.log.error(msg, params);
    }

    /**
     * handle itemNotFound errors
     */
    @Override
    public void itemNotFound(DBObject dbo)
    {
        if (dbo instanceof DBTable)
        {
            DBTable table = (DBTable)dbo;
            logError("The table {} does not exist in the target database.", table.getName());
        }
        else if (dbo instanceof DBColumn)
        {
            DBColumn column = (DBColumn) dbo;
            logError("The column {} does not exist in the target database.", column.getFullName());
        }
        else if (dbo instanceof DBIndex)
        {
            DBIndex dbi = (DBIndex) dbo;
            logError("The primary key {} for table{} does not exist in the target database.", dbi.getName(), dbi.getTable().getName());
        }
        else if (dbo instanceof DBView)
        {
            DBView view = (DBView)dbo;
            logError("The view {} does not exist in the target database.", view.getName());
        }
        else if (dbo instanceof DBRelation)
        {
            DBRelation relation = (DBRelation)dbo;
            logError("The foreing key relation "+relation.getName()+" from table {} to table {} does not exist in the target database.", relation.getForeignKeyTable().getName(), relation.getReferencedTable().getName());
        }
        else
        {
            logError("The object {} does not exist in the target database.", dbo.toString());
        }
        // increase count
        errorCount++;
    }

    /**
     * handle objectTypeMismatch errors
     */
    @Override
    public void objectTypeMismatch(DBObject object, String name, Class<?> expectedType)
    {
        // log
        logError("The oboject \"{}\" type of {} does not match the expected type of {}.", name, object.getClass().getSimpleName(), expectedType.getSimpleName());
        // increase count
        errorCount++;
    }

    /**
     * handle columnTypeMismatch errors
     */
    @Override
    public void columnTypeMismatch(DBColumn col, DataType type)
    {
        if ((col instanceof DBViewColumn) && col.getDataType()==DataType.DECIMAL && type==DataType.INTEGER)
        {   // sepcial view column handling
            return;
        }
        // log
        logError("The column " + col.getFullName() + " type of {} does not match the database type of {}.", col.getDataType(), type);
        // increase count
        errorCount++;
    }

    /**
     * handle columnSizeMismatch errors
     */
    @Override
    public void columnSizeMismatch(DBColumn col, int size, int scale)
    {
        if (size>0 && size<col.getSize())
        {   // Database size is smaller: Error 
            logError("The column "+col.getFullName()+" size of {} does not match the database size of {}.", col.getSize(), size);
            // increase count
            errorCount++;
        }
        else if (col.getDataType()!=DataType.INTEGER)
        {   // Database size is bigger or unknown: Warning only
            logWarn("The column "+col.getFullName()+" size of {} does not match the database size of {}.", col.getSize(), size);
            // increase count
            warnCount++;
        }
    }

    /**
     * handle columnNullableMismatch errors
     */
    @Override
    public void columnNullableMismatch(DBColumn col, boolean nullable)
    {
        if (nullable)
        {
            logError("The column " + col.getFullName() + " must not be nullable");
        }
        else
        {
            logError("The column " + col.getFullName() + " must be nullable");
        }
        // increase count
        errorCount++;
    }

    /**
     * handle primaryKeyColumnMissing errors
     */
    @Override
    public void primaryKeyColumnMissing(DBIndex primaryKey, DBColumn column)
    {
        logError("The primary key of table {} misses the column {}", primaryKey.getTable().getName(), column.getName());
        // increase count
        errorCount++;
    }

    @Override
    public void primaryKeyMismatch(DBIndex primaryKey, DBColumn[] tableKey)
    {
        String defColumns;
        String tblColumns;
        StringBuilder b = new StringBuilder("[");
        DBColumn[] keyColumns = primaryKey.getColumns();
        for (int i=0; i<keyColumns.length; i++) {
            if (i>0) b.append("|");
            b.append(keyColumns[i].getName());
        }
        b.append("]");
        defColumns = b.toString();
        b.setLength(1);
        for (int i=0; i<tableKey.length; i++) {
            if (i>0) b.append("|");
            b.append(tableKey[i].getName());
        }
        b.append("]");
        tblColumns = b.toString();
        logError("The primary key of table {} {} does not match the key of the existing table {}.", primaryKey.getTable().getName(), defColumns, tblColumns);
        // increase count
        warnCount++;
    }
}
