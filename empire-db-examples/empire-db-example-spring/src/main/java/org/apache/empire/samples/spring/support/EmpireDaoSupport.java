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
package org.apache.empire.samples.spring.support;

import java.sql.Connection;

import org.apache.empire.db.DBCommand;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.DBDatabaseDriver;
import org.apache.empire.db.DBReader;
import org.apache.empire.db.DBSQLScript;
import org.apache.empire.db.DBTable;
import org.apache.empire.exceptions.EmpireException;
import org.springframework.jdbc.core.support.JdbcDaoSupport;

/**
 * Extent JdbcDaoSupport from spring to provide a helper class to implement DAO objects using the Empire-DB framework.
 *
 */
public class EmpireDaoSupport extends JdbcDaoSupport {

    protected DBDatabase db;
    protected DBDatabaseDriver driver;

    public EmpireDaoSupport() {
    }

    public void setDatabase(DBDatabase db) {
        this.db = db;
    }

    public void setDatabaseDriver(DBDatabaseDriver dbdriver) {
        this.driver = dbdriver;
    }

    @Override
    protected void checkDaoConfig() {
        super.checkDaoConfig();
        if (db == null)
            throw new IllegalArgumentException("'database' must be given!");

        if (driver == null)
            throw new IllegalArgumentException("'databaseDriver' must be given!");
    }

    public DBDatabase getDatabase() {
        try {
            if (!db.isOpen())
                db.open(driver, getConnection());
            return db;
        } catch (EmpireException e) {
            throw translateEmpireException(e);
        }
    }

    public boolean databaseExists() {
        Connection conn = getConnection();
        try {
            DBDatabase db = getDatabase();
            if (db.getTables() == null || db.getTables().isEmpty()) {
                throw new AssertionError("There are no tables in this database!");
            }
            DBCommand cmd = db.createCommand();
            if (cmd == null) {
                throw new AssertionError("The DBCommand object is null.");
            }
            DBTable t = db.getTables().get(0);
            cmd.select(t.count());
            return (db.querySingleInt(cmd.getSelect(), -1, conn) >= 0);
        } catch (Exception e) {
            return false;
        }
    }

    public void createDatabase() {
        Connection conn = getConnection();

        // create DLL for Database Definition
        DBSQLScript script = new DBSQLScript();
        db.getCreateDDLScript(driver, script);
        // Show DLL Statement
        System.out.println(script.toString());
        // Execute Script
        script.run(driver, conn, false);
    }

    public void initializeDatabase() {
        try {
            if (!databaseExists()) {
                createDatabase();
            }
        } catch (EmpireException e) {
            throw translateEmpireException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends DBDatabase> T getDB() {
        if (getDatabase() == null) {
            throw new IllegalStateException("The database is null?!");
        }
        return (T) getDatabase();
    }

    public DBDatabaseDriver getDatabasedriver() {
        return driver;
    }

    public RuntimeException translateEmpireException(EmpireException e) {
        return new EmpireDBException(e);
    }

    protected DBReader openReader(DBCommand cmd, Connection conn) {
        DBReader r = new DBReader();
        r.open(cmd, conn);
        return r;
    }

}
