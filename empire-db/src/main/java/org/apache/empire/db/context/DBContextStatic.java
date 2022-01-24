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
package org.apache.empire.db.context;

import java.sql.Connection;

import org.apache.empire.db.context.DBRollbackManager.ReleaseAction;
import org.apache.empire.dbms.DBMSHandler;

public class DBContextStatic extends DBContextBase
{
    private final DBMSHandler dbms;
    private final Connection conn;
    private final boolean enableRollbackHandling;
    private final boolean closeOnDiscard;
    
    /**
     *  Global DBRollbackManager
     *  
     *  initialConnectionCapacity = 2
     *  initialObjectCapacity = 16
     */
    private static final DBRollbackManager staticRollbackManager = new DBRollbackManager(2, 16);

    /**
     * Creates a static DBContext with default options
     * @param dbmsHandler
     * @param conn
     */
    public DBContextStatic(DBMSHandler dbmsHandler, Connection conn)
    {
        this(dbmsHandler, conn, (conn!=null), false);
    }
    
    /**
     * Creates a static DBContext with custom options
     * @param dbmsHandler
     * @param conn
     * @param enableRollbackHandling
     * @param closeOnDiscard
     */
    public DBContextStatic(DBMSHandler dbmsHandler, Connection conn, boolean enableRollbackHandling, boolean closeOnDiscard)
    {
        this.dbms = dbmsHandler;
        this.conn = conn;
        this.enableRollbackHandling = enableRollbackHandling;
        this.closeOnDiscard = closeOnDiscard;
    }

    @Override
    public DBMSHandler getDbms()
    {
        return dbms;
    }

    @Override
    public boolean isRollbackHandlingEnabled()
    {
        return enableRollbackHandling;
    }
    
    @Override
    public void discard()
    {
        super.discard();
        // close
        if (closeOnDiscard) 
        {   // Close the connection
            closeConnection();
            // rollbackManager release
            if (enableRollbackHandling)
                staticRollbackManager.releaseConnection(conn, ReleaseAction.Discard);
        }
    }

    @Override
    protected Connection getConnection(boolean create)
    {
        return conn;
    }

    @Override
    protected DBRollbackManager getRollbackManager(boolean create)
    {
        return staticRollbackManager;
    }
}
