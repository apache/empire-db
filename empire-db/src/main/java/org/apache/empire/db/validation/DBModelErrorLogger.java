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
        if (dbo instanceof DBIndex)
        {
            DBIndex dbi = (DBIndex) dbo;
            DBModelErrorLogger.log.error("The primary key " + dbi.getName() + " for table " + dbi.getTable().getName()
                                         + " does not exist in the target database.");
        }
        else
        {
            DBModelErrorLogger.log.error("The object " + dbo.toString() + " does not exist in the target database.");
        }
    }

    /**
     * handle columnTypeMismatch errors
     */
    public void columnTypeMismatch(DBColumn col, DataType type)
    {
        DBModelErrorLogger.log.error("The column " + col.getFullName() + " type of " + col.getDataType().toString()
                                     + " does not match the database type of " + type.toString());
    }

    /**
     * handle columnSizeMismatch errors
     */
    public void columnSizeMismatch(DBColumn col, int size, int scale)
    {
        DBModelErrorLogger.log.error("The column " + col.getFullName() + " size of " + String.valueOf(col.getSize())
                                     + " does not match the size database size of " + String.valueOf(size));
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
