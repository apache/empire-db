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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implemtnation of the {@link DBModelErrorHandler} interface that logs all errors
 */
public class DBModelErrorLogger implements DBModelErrorHandler
{
    private static final Logger log = LoggerFactory.getLogger(DBModelErrorLogger.class);

    /**
     * handle itemNotFound errors
     */
    public void itemNotFound(DBObject dbo)
    {
        if (dbo instanceof DBTable)
        {
            DBTable table = (DBTable)dbo;
            DBModelErrorLogger.log.error("The table {} does not exist in the target database.", table.getName());
        }
        else if (dbo instanceof DBColumn)
        {
            DBColumn column = (DBColumn) dbo;
            DBModelErrorLogger.log.error("The column {} does not exist in the target database.", column.getFullName());
        }
        else if (dbo instanceof DBIndex)
        {
            DBIndex dbi = (DBIndex) dbo;
            DBModelErrorLogger.log.error("The primary key {} for table{} does not exist in the target database.", dbi.getName(), dbi.getTable().getName());
        }
        else if (dbo instanceof DBView)
        {
            DBView view = (DBView)dbo;
            DBModelErrorLogger.log.error("The view {} does not exist in the target database.", view.getName());
        }
        else if (dbo instanceof DBRelation)
        {
            DBRelation relation = (DBRelation)dbo;
            DBModelErrorLogger.log.error("The foreing key relation "+relation.getName()+" from table {} to table {} does not exist in the target database.", relation.getForeignKeyTable().getName(), relation.getReferencedTable().getName());
        }
        else
        {
            DBModelErrorLogger.log.error("The object {} does not exist in the target database.", dbo.toString());
        }
    }

    /**
     * handle columnTypeMismatch errors
     */
    public void columnTypeMismatch(DBColumn col, DataType type)
    {
        DBModelErrorLogger.log.error("The column " + col.getFullName() + " type of {} does not match the database type of {}.", col.getDataType(), type);
    }

    /**
     * handle columnSizeMismatch errors
     */
    public void columnSizeMismatch(DBColumn col, int size, int scale)
    {
        if (size>0 && size<col.getSize())
        {   // Database size is smaller: Error 
            DBModelErrorLogger.log.error("The column "+col.getFullName()+" size of {} does not match the database size of {}.", col.getSize(), size);
        }
        else
        {   // Database size is bigger or unknown: Warning only
            DBModelErrorLogger.log.warn("The column "+col.getFullName()+" size of {} does not match the database size of {}.", col.getSize(), size);
        }
    }

    /**
     * handle columnNullableMismatch errors
     */
    public void columnNullableMismatch(DBColumn col, boolean nullable)
    {
        if (nullable)
        {
            DBModelErrorLogger.log.error("The column " + col.getFullName() + " must not be nullable");
        }
        else
        {
            DBModelErrorLogger.log.error("The column " + col.getFullName() + " must be nullable");
        }
    }

    /**
     * handle primaryKeyColumnMissing errors
     */
    public void primaryKeyColumnMissing(DBIndex primaryKey, DBColumn column)
    {
        DBModelErrorLogger.log.error("The primary key " + primaryKey.getName() + " of table " + primaryKey.getTable().getName()
                                     + " misses the column " + column.getName());
    }

}
