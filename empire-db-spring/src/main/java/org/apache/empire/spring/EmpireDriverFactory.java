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
package org.apache.empire.spring;

import java.sql.Connection;

import org.apache.empire.db.DBDatabaseDriver;
import org.apache.empire.db.derby.DBDatabaseDriverDerby;
import org.apache.empire.db.h2.DBDatabaseDriverH2;
import org.apache.empire.db.hsql.DBDatabaseDriverHSql;
import org.apache.empire.db.mysql.DBDatabaseDriverMySQL;
import org.apache.empire.db.oracle.DBDatabaseDriverOracle;
import org.apache.empire.db.postgresql.DBDatabaseDriverPostgreSQL;
import org.apache.empire.db.sqlserver.DBDatabaseDriverMSSQL;
import org.springframework.jdbc.core.support.JdbcDaoSupport;

/**
 * Creates the Empire-DB Drivers. Extends JdbcDaoSupport because it needs a Connection
 */
public class EmpireDriverFactory extends JdbcDaoSupport {

    // copy&paste from the SampleApp

    public DBDatabaseDriver createDriver(String driverclass, String schema) {
        if (isClass(DBDatabaseDriverMySQL.class, driverclass))
        {
            DBDatabaseDriverMySQL driver = new DBDatabaseDriverMySQL();
            // Set Driver specific properties (if any)
            driver.setDatabaseName(schema);
            return driver;
        }
        else if (isClass(DBDatabaseDriverOracle.class, driverclass))
        {
            DBDatabaseDriverOracle driver = new DBDatabaseDriverOracle();
            // Set Driver specific properties (if any)
            return driver;
        }
        else if (isClass(DBDatabaseDriverMSSQL.class, driverclass))
        {
            DBDatabaseDriverMSSQL driver = new DBDatabaseDriverMSSQL();
            // Set Driver specific properties (if any)
            driver.setDatabaseName(schema);
            return driver;
        }
        else if (isClass(DBDatabaseDriverHSql.class, driverclass))
        {
            DBDatabaseDriverHSql driver = new DBDatabaseDriverHSql();
            // Set Driver specific properties (if any)
            return driver;
        }
        else if (isClass(DBDatabaseDriverPostgreSQL.class, driverclass))
        {
            DBDatabaseDriverPostgreSQL driver = new DBDatabaseDriverPostgreSQL();
            // Set Driver specific properties (if any)
            driver.setDatabaseName(schema);
            // Create the reverse function that is needed by this sample
            Connection conn = getConnection();
            driver.createReverseFunction(conn);
            releaseConnection(conn);
            return driver;
        }
        else if (isClass(DBDatabaseDriverH2.class, driverclass))
        {
            DBDatabaseDriverH2 driver = new DBDatabaseDriverH2();
            // Set Driver specific properties (if any)
            driver.setDatabaseName(schema);
            return driver;
        }
        else if (isClass(DBDatabaseDriverDerby.class, driverclass))
        {
            DBDatabaseDriverDerby driver = new DBDatabaseDriverDerby();
            // Set Driver specific properties (if any)
            driver.setDatabaseName(schema);
            return driver;
        }
        else
        {   // Unknown Provider
            throw new RuntimeException("Unknown Database Driver " + driverclass);
        }
    }

    private boolean isClass(Class<? extends DBDatabaseDriver> clazz, String name) {
        return name.equals(clazz.getName());
    }
}
