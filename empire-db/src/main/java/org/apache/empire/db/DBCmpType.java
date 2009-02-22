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

/**
 * This enum allocates the available compare types.
 *
 *
 */
public enum DBCmpType
{
    NONE, //         = -1,
    EQUAL, //        =  0,
    NOTEQUAL, //     =  1,
    LESSTHAN, //     =  2,
    MOREOREQUAL, //  =  3,
    GREATERTHAN, //  =  4,
    LESSOREQUAL, //  =  5,
    LIKE, //         =  6,
    NOTLIKE, //      =  7,
    NULL, //         =  8,
    NOTNULL, //      =  9,
    BETWEEN, //      = 10,
    NOTBETWEEN, //   = 11,
    IN, //           = 12,
    NOTIN; //        = 13,
    
    public static DBCmpType getNullType(DBCmpType type)
    {
        switch(type)
        {
            case NOTEQUAL:
            case MOREOREQUAL:
            case LESSOREQUAL:
            case NOTLIKE:
            case NOTNULL:
            case NOTBETWEEN:
            case NOTIN:
            // Compare with NOT null
                 return DBCmpType.NOTNULL;
            // Compare with null
            default: return DBCmpType.NULL;
        }
    }
}
