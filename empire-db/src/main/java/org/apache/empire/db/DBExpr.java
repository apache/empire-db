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


/**
 * This abstract class is the base class for all database expression classes (e.g. DBAliasExpr or DBCalsExpr)
 * <P>
 * 
 *
 */
public abstract class DBExpr extends DBObject
{
    // *Deprecated* private static final long serialVersionUID = 1L;
  
    // SQL Context Flags
    public static final long CTX_DEFAULT       = 7;  // Default: FullyQualified + Value
    public static final long CTX_ALL           = 15; // All Flags set (except exclusions)

    public static final long CTX_NAME          = 1;  // Unqualified Name
    public static final long CTX_FULLNAME      = 2;  // Fully Qualified Name
    public static final long CTX_VALUE         = 4;  // Value Only
    public static final long CTX_ALIAS         = 8;  // Rename expression
    public static final long CTX_NOPARENTHESES = 16; // No Parentheses
    
    /**
     * Used to build the SQL command. SQL for this expression must be appended to StringBuilder.
     * 
     * @param sql the string buffer used to build the sql command
     * @param context context flag for this SQL-Command (see CTX_??? constants).
     */
    public abstract void addSQL(DBSQLBuilder sql, long context);

    /**
     * Internal function to obtain all DBColumnExpr-objects used by this expression. 
     * 
     * @param list list to which all used column expressions must be added
     */
    public abstract void addReferencedColumns(Set<DBColumn> list);
}