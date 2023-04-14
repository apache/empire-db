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

import org.apache.empire.exceptions.InvalidArgumentException;

/**
 * Base class for all objects that directly or indirectly belong to a database including the database object itself.
 * Examples are: tables, views, columns, indexes, relations etc.
 * Not included are: DBMSHanlders, helper classes
 */
public abstract class DBObject // *Deprecated* implements Serializable
{
    // *Deprecated* private static final long serialVersionUID = 1L;

    /**
     * Internally used for parameter checking
     * @param <T> the object type
     * @param name the name of the param to check
     * @param param the param vlaue to check
     * @return the param value
     */
    public static <T extends Object> T checkParamNull(String name, T param)
    {
        if (param==null)
            throw new InvalidArgumentException(name, param);
        return param;
    }
    
    /**
     * Returns the database object to which this object belongs to.
     * For the database object itself this function will return the this pointer.
     * 
     * @param <T> the object type
     * @return the database object
     */
    public abstract <T extends DBDatabase> T getDatabase();
}