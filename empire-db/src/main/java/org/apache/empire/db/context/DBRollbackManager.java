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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.empire.db.DBObject;
import org.apache.empire.exceptions.InvalidArgumentException;
import org.apache.empire.exceptions.ObjectNotValidException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DBRollbackManager
{    // Logger
    private static final Logger log = LoggerFactory.getLogger(DBRollbackManager.class);
    
    /**
     * Connection release action 
     * @author rainer
     */
    public enum ReleaseAction
    {
        Discard,
        Rollback;
    }

    private final Map<Integer, Map<DBObject, DBRollbackHandler>> connectionMap;
    private final int initialObjectCapacity;
    
    /**
     * DBRollbackManager constructor
     * @param initialConnectionCapacity initial capacity 
     * @param initialObjectCapacity initial capacity
     */
    public DBRollbackManager(int initialConnectionCapacity, int initialObjectCapacity)
    {
        this.connectionMap = new HashMap<Integer, Map<DBObject, DBRollbackHandler>>(initialConnectionCapacity);
        this.initialObjectCapacity = initialObjectCapacity;
    }

    /**
     * Add a rollback handler for a particular Connection
     * @param conn the database connection
     * @param handler the handler to append
     */
    public synchronized void appendHandler(Connection conn, DBRollbackHandler handler)
    {
        if (conn==null)
        {   // Oops, no connection
            throw new InvalidArgumentException("conn", conn);
        }
        Map<DBObject, DBRollbackHandler> handlerMap = connectionMap.get(conn.hashCode());
        if (handlerMap==null)
        {   handlerMap = new LinkedHashMap<DBObject, DBRollbackHandler>(this.initialObjectCapacity);
            connectionMap.put(conn.hashCode(), handlerMap);
        }
        // check
        DBObject object = handler.getObject();
        if (object==null)
            throw new ObjectNotValidException(handler);
        // Append or combine
        if (handlerMap.containsKey(object))
            handlerMap.get(object).combine(handler);
        else
            handlerMap.put(object, handler);
        // log
        if (log.isDebugEnabled())
            log.debug("Rollback handler for {} was added.", handler.getObjectInfo());
    }
    
    /**
     * Remove the rollback handler for a particular Connection and Object
     * @param conn the database connection
     * @param object the object for which to remove the handler
     */
    public synchronized void removeHandler(Connection conn, DBObject object)
    {
        if (conn==null)
        {   // Oops, no connection
            return;
        }
        if (object==null)
        {   // Discard all
            releaseConnection(conn, ReleaseAction.Discard);
            return; 
        }
        Map<DBObject, DBRollbackHandler> handlerMap = connectionMap.get(conn.hashCode());
        if (handlerMap==null)
            return; // Nothing to do
        // Remover handler
        DBRollbackHandler handler = handlerMap.remove(object); 
        if (handler==null)
            return; // No handler
        // discard
        if (log.isDebugEnabled())
            log.debug("Rollback handler for {} was removed.", handler.getObjectInfo());
        handler.discard(conn);
    }
    
    /**
     * releaseConnection from handler and perform 
     * @param conn the database connection
     * @param action the release action
     */
    public synchronized void releaseConnection(Connection conn, ReleaseAction action)
    {
        if (conn==null)
        {   // Oops, no connection
            return;
        }
        Map<DBObject, DBRollbackHandler> handlerMap = connectionMap.get(conn.hashCode());
        if (handlerMap==null)
            return; // Nothing to do
        // rollback
        log.info("DBRollbackManager performes {} for {} objects.", action, handlerMap.size());
        for (DBRollbackHandler handler : handlerMap.values())
            if (action==ReleaseAction.Rollback)
                handler.rollback(conn);
            else
                handler.discard(conn);
        // cleanup
        connectionMap.remove(conn.hashCode());        
    }
}
