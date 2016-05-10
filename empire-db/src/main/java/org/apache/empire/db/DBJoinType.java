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
 * 
 * DBJoinType contains the possibilities to join two database tables.
 * 
 *
 */
public enum DBJoinType
{
    /**
     * SQL Left join
     */
    LEFT,   //   =-1,
    
    /**
     * SQL Inner join
     */
    INNER,  //   = 0,
    
    /**
     * SQL Right join
     */
    RIGHT,  //   = 1
    
    /**
     * SQL Cross join
     */
    CROSS;
    
    public static DBJoinType reversed(DBJoinType type)
    {
        switch(type)
        {
            case LEFT:  
                return RIGHT;
            case RIGHT: 
                return LEFT;
            default:    
                // no change
                return type; 
        }
    }
}
