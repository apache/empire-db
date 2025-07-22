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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DBContextStatic extends DBContextBase
{    // Logger
    private static final Logger log = LoggerFactory.getLogger(DBContextStatic.class);

    private final DBMSHandler dbms;
    private final Connection conn;
    private final boolean closeOnDiscard;
    // features
    private boolean enableRollbackHandling = false;
    private boolean autoPrepareStmt = false;
    
    /**
     *  Global DBRollbackManager
     *  
     *  initialConnectionCapacity = 2
     *  initialObjectCapacity = 16
     */
    private static final DBRollbackManager staticRollbackManager = new DBRollbackManager(2, 16);

    /**
     * Creates a static DBContext with default options
     * @param dbmsHandler the database handler
     * @param conn a database connection
     */
    public DBContextStatic(DBMSHandler dbmsHandler, Connection conn)
    {
        this(dbmsHandler, conn, false);
    }
    
    /**
     * Creates a static DBContext with custom options
     * @param dbmsHandler the database handler
     * @param conn a database connection
     * @param closeOnDiscard flag whether to close the connection when calling discard()
     */
    public DBContextStatic(DBMSHandler dbmsHandler, Connection conn, boolean closeOnDiscard)
    {
        this.dbms = dbmsHandler;
        this.conn = conn;
        this.closeOnDiscard = closeOnDiscard;
    }

    @Override
    public DBMSHandler getDbms()
    {
        return dbms;
    }

    @Override
    public boolean isPreparedStatementsEnabled()
    {
        return autoPrepareStmt;
    }
    
    /**
     * Enables or disables the use of prepared statements for update and insert commands as well as for read operations on a DBRecord.
     * Note: For custom SQL commands parameters must be explicitly declared using cmd.addCmdParam();   
     * @param enabled flag whether to enable prepared statements
     * @return the context (this)
     */
    public DBContextStatic setPreparedStatementsEnabled(boolean enabled)
    {
        this.autoPrepareStmt = enabled;
        // log prepared statement 
        log.info("PreparedStatementsEnabled has been set to {}", autoPrepareStmt);
        return this;
    }

    @Override
    public boolean isRollbackHandlingEnabled()
    {
        return enableRollbackHandling;
    }

    /**
     * Enables or disables rollback handling
     * @param enabled flag whether to enable rollback handling
     * @return the context (this)
     */
    public DBContextStatic setRollbackHandlingEnabled(boolean enabled)
    {
        this.enableRollbackHandling = enabled;
        // log prepared statement 
        log.info("RollbackHandlingEnabled has been set to {}", enableRollbackHandling);
        return this;
    }
    
    @Override
    public void discard()
    {
        super.discard();
        // close
        if (closeOnDiscard) 
        {   // rollbackManager release
            if (enableRollbackHandling)
                staticRollbackManager.releaseConnection(conn, ReleaseAction.Discard);
            // Close the connection
            closeConnection();
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
