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

public interface DBModelErrorHandler
{

    /**
     * This method is called when an object (e. g. table or column) is missing in
     * the database.
     * 
     * @param dbo
     *            The missing object
     */
    void itemNotFound(DBObject dbo);

    /**
     * This method is called when a column in a primary key of the Empire-db definition
     * is missing in the database
     * 
     * @param primaryKey
     *            The primary key that misses the column
     * @param column
     *            The missing column
     */
    void primaryKeyColumnMissing(DBIndex primaryKey, DBColumn column);

    /**
     * This method is called when the type of a column in the Empire-db
     * definition does not match the database.
     * 
     * @param col
     *            The affected column
     * @param type
     */
    void columnTypeMismatch(DBColumn col, DataType type);

    /**
     * This method is called when the size of a column in the Empire-db
     * definition does not match the database.
     * 
     * @param col
     *            The affected column
     * @param size
     *            Size in the database
     * @param scale
     *            Decimal scale in the database (only for decimal types, 0 otherwise)
     */
    void columnSizeMismatch(DBColumn col, int size, int scale);

    /**
     * This method is called when a NOT NULL constraints of a column in
     * the Empire-db definition does not match the database.
     * 
     * @param col
     *            The affected column
     * @param nullable
     *            true if the column is required in the database
     */
    void columnNullableMismatch(DBColumn col, boolean nullable);
}
