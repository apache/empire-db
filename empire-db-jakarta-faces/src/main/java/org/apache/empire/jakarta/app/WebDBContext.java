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
package org.apache.empire.jakarta.app;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.sql.Connection;

import jakarta.faces.context.FacesContext;

import org.apache.empire.commons.ClassUtils;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.context.DBContextBase;
import org.apache.empire.db.context.DBRollbackManager;
import org.apache.empire.dbms.DBMSHandler;
import org.apache.empire.exceptions.ItemNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the basic implementation of a DBContext for a web application
 * Important: The connection is obtained on HttpRequest scope and hot held by the context
 * The connection is automatically released via the FacesRequestPhaseListener
 * @author rainer
 */
public class WebDBContext<DB extends DBDatabase> extends DBContextBase implements Serializable
{
    private static final long serialVersionUID = 1L;

    private static final Logger    log = LoggerFactory.getLogger(WebDBContext.class);

    protected final transient WebApplication app;
    protected final transient DB             database;
    protected final transient DBMSHandler    dbms;
    
    private final boolean rollbackHandlingEnabled;

    /**
     * Custom serialization for transient fields.
     * @param strm the output stream
     * @throws IOException from strm.defaultWriteObject
     */ 
    private void writeObject(ObjectOutputStream strm) throws IOException 
    {   // Database
        strm.writeObject((database!=null ? database.getIdentifier() : ""));
        // write the rest
        strm.defaultWriteObject();
    }
    
    /**
     * Custom deserialization for transient fields.
     * @param strm the input stream
     * @throws IOException from strm.defaultReadObject
     * @throws ClassNotFoundException from strm.defaultReadObject
     */ 
    private void readObject(ObjectInputStream strm) 
        throws IOException, ClassNotFoundException
    {   // WebApplication
        WebApplication app = WebApplication.getInstance();
        ClassUtils.setPrivateFieldValue(WebDBContext.class, this, "app", app);
        // Database
        String dbid = String.valueOf(strm.readObject());
        DBDatabase database = DBDatabase.findByIdentifier(dbid);
        if (database==null)
            throw new ItemNotFoundException(dbid);
        ClassUtils.setPrivateFieldValue(WebDBContext.class, this, "database", database);
        ClassUtils.setPrivateFieldValue(WebDBContext.class, this, "dbms",     database.getDbms());
        // read the rest
        strm.defaultReadObject();
    }
    
    /**
     * Constructor
     * @param db the database
     * @param rollbackHandlingEnabled flag whether to enable rollback handling
     */
    public WebDBContext(DB db, boolean rollbackHandlingEnabled)
    {
        this.app  = WebApplication.getInstance();
        this.dbms = db.getDbms();
        this.database = db;
        // more
        this.rollbackHandlingEnabled = rollbackHandlingEnabled;
        // check dbms
        if (db.getDbms() == null)
            log.warn("Database {} has no dbms attached.", db.getClass().getSimpleName());
    }
    
    /**
     * Constructor
     * @param db the database
     */
    public WebDBContext(DB db)
    {
        this(db, true);
    }

    public DB getDatabase()
    {
        return database;
    }

    @Override
    public DBMSHandler getDbms()
    {
        return dbms;
    }
    
    @Override
    public boolean isPreparedStatementsEnabled()
    {
        return database.isPreparedStatementsEnabled();
    }

    @Override
    public boolean isRollbackHandlingEnabled()
    {
        return rollbackHandlingEnabled;
    }

    /**
     * Unclear weather this is any useful here
     */
    @Override
    public void discard()
    {
        /*
        FacesContext fc = FacesContext.getCurrentInstance();
        this.app.releaseConnection(fc, database);
        // discard
        super.discard();
        */
    }

    @Override
    protected Connection getConnection(boolean create)
    {
        return app.getConnectionForRequest(getFacesContext(), database, create);
    }

    @Override
    protected DBRollbackManager getRollbackManager(boolean create)
    {
        return app.getRollbackManagerForRequest(getFacesContext(), create);
    }
    
    public FacesContext getFacesContext()
    {
        return FacesContext.getCurrentInstance();
    }
    
}
