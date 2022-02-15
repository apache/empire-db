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

import java.sql.Connection;

import org.apache.empire.db.context.DBRollbackHandler;
import org.apache.empire.dbms.DBMSHandler;

/**
 * DBContext 
 * A context is required for every database operation
 * @author rainer
 */
public interface DBContext
{
    DBMSHandler getDbms();
    
    Connection getConnection();
    
    <T extends DBUtils> T getUtils();

    DBCommand createCommand(); 
    
    int executeSQL(String sqlCmd, Object[] sqlParams);
    
    int executeInsert(DBCommand cmd);
    
    int executeInsertInto(DBTable table, DBCommand cmd);
    
    int executeUpdate(DBCommand cmd);

    int executeDelete(DBTable from, DBCommand cmd);
    
    void commit();

    void rollback();
    
    boolean isRollbackHandlingEnabled();
    
    void appendRollbackHandler(DBRollbackHandler handler);

    void removeRollbackHandler(DBObject object);
    
    void discard();
}