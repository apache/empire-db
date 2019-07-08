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
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.empire.data.DataType;
import org.apache.empire.db.derby.DBDatabaseDriverDerby;
import org.apache.empire.db.h2.DBDatabaseDriverH2;
import org.apache.empire.db.hsql.DBDatabaseDriverHSql;
import org.apache.empire.db.mysql.DBDatabaseDriverMySQL;
import org.apache.empire.db.oracle.DBDatabaseDriverOracle;
import org.apache.empire.db.postgresql.DBDatabaseDriverPostgreSQL;
import org.apache.empire.db.sqlite.DBDatabaseDriverSQLite;
import org.apache.empire.db.sqlserver.DBDatabaseDriverMSSQL;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Support of long integer fields
 * https://issues.apache.org/jira/browse/EMPIREDB-100
 */
public class IntegerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(IntegerTest.class);
    
    
    @Test
    public void testSQlitedb() {
        SampleConfig config = new SampleConfig();
        config.databaseProvider = "sqlite";
        config.jdbcClass = "org.sqlite.JDBC";
        config.jdbcURL = "jdbc:sqlite::memory:";
        testLongInteger(config);
    }
    
    @Test
    public void testHsqldb() {
        SampleConfig config = new SampleConfig();
        config.databaseProvider = "hsqldb";
        config.jdbcClass = "org.hsqldb.jdbcDriver";
        config.jdbcURL = "jdbc:hsqldb:mem:data/derby/test";
        testLongInteger(config);
    }
    
    @Test
    public void testDerby() {
        SampleConfig config = new SampleConfig();
        config.databaseProvider = "derby";
        config.jdbcClass = "org.apache.derby.jdbc.EmbeddedDriver";
        config.jdbcURL = "jdbc:derby:memory:data/derby/test;create=true";
        testLongInteger(config);
    }
    
    @Test
    public void testH2() {
        SampleConfig config = new SampleConfig();
        config.databaseProvider = "h2";
        config.jdbcClass = "org.h2.Driver";
        config.jdbcURL = "jdbc:h2:mem:data/h2/test";
        testLongInteger(config);
    }
    
    @Test
    @Ignore("This can only run when we have a local mydsl server set up")
    public void testMySql()
    {
        SampleConfig config = new SampleConfig();
        config.databaseProvider = "mysql";
        config.jdbcClass = "com.mysql.jdbc.Driver";
        config.jdbcURL = "jdbc:mysql://localhost:3306/test";
        config.jdbcUser = "test";
        config.jdbcPwd = "test";
        config.schemaName = "test";
        testLongInteger(config);
    }

    private void testLongInteger(SampleConfig config) {
        SampleDB database = new SampleDB();

        Connection connection = getJDBCConnection(config);

        DBDatabaseDriver driver = getDatabaseDriver(config, connection);

        database.open(driver, connection);

        createDatabase(database, driver, connection);

        DBRecord maxRec = new DBRecord();
        maxRec.create(database.SAMPLE);
        maxRec.setValue(database.SAMPLE.MY_INTEGER, Integer.MAX_VALUE);
        maxRec.setValue(database.SAMPLE.MY_LONG, Long.MAX_VALUE);
        maxRec.setValue(database.SAMPLE.MY_SHORT, Short.MAX_VALUE);
        maxRec.update(connection);
        
        DBRecord minRec = new DBRecord();
        minRec.create(database.SAMPLE);
        minRec.setValue(database.SAMPLE.MY_INTEGER, Integer.MIN_VALUE);
        minRec.setValue(database.SAMPLE.MY_LONG, Long.MIN_VALUE);
        minRec.setValue(database.SAMPLE.MY_SHORT, Short.MIN_VALUE);
        minRec.update(connection);
    }

    private static Connection getJDBCConnection(SampleConfig config) {
        Connection conn = null;
        LOGGER.info("Connecting to Database'" + config.jdbcURL + "'");

        try{
            // Connect to the database
            Class.forName(config.jdbcClass).newInstance();
            conn = DriverManager.getConnection(config.jdbcURL, config.jdbcUser, config.jdbcPwd);
            LOGGER.info("Connected successfully");
            conn.setAutoCommit(false);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        } 
            
        return conn;
    }

    private DBDatabaseDriver getDatabaseDriver(SampleConfig config, Connection conn) {
        if (config.databaseProvider.equalsIgnoreCase("sqlite")) {
            DBDatabaseDriverSQLite driver = new DBDatabaseDriverSQLite();
            return driver;
        }
        if (config.databaseProvider.equalsIgnoreCase("mysql")) {
            DBDatabaseDriverMySQL driver = new DBDatabaseDriverMySQL();
            driver.setDatabaseName(config.schemaName);
            return driver;
        }
        if (config.databaseProvider.equalsIgnoreCase("oracle")) {
            DBDatabaseDriverOracle driver = new DBDatabaseDriverOracle();
            return driver;
        }
        if (config.databaseProvider.equalsIgnoreCase("sqlserver")) {
            DBDatabaseDriverMSSQL driver = new DBDatabaseDriverMSSQL();
            driver.setDatabaseName(config.schemaName);
            return driver;
        }
        if (config.databaseProvider.equalsIgnoreCase("hsqldb")) {
            DBDatabaseDriverHSql driver = new DBDatabaseDriverHSql();
            return driver;
        }
        if (config.databaseProvider.equalsIgnoreCase("postgresql")) {
            DBDatabaseDriverPostgreSQL driver = new DBDatabaseDriverPostgreSQL();
            driver.setDatabaseName(config.schemaName);
            return driver;
        }
        if (config.databaseProvider.equalsIgnoreCase("h2")) {
            DBDatabaseDriverH2 driver = new DBDatabaseDriverH2();
            driver.setDatabaseName(config.schemaName);
            return driver;
        }
        if (config.databaseProvider.equalsIgnoreCase("derby")) {
            DBDatabaseDriverDerby driver = new DBDatabaseDriverDerby();
            driver.setDatabaseName(config.schemaName);
            return driver;
        }

        throw new RuntimeException("Unknown Database Provider " + config.databaseProvider);
    }

    private void createDatabase(DBDatabase db, DBDatabaseDriver driver, Connection conn) {
        // try to remove previously existing tables
        List<String> tables = getTableNames(db, conn);
        DBSQLScript script2 = new DBSQLScript();
        for(DBTable table:db.getTables()){
            if(tables.contains(table.getName())){
                db.getDriver().getDDLScript(DBCmdType.DROP, table, script2);
            }
        }
        script2.executeAll(driver, conn, false);
        // Commit
        db.commit(conn);
        
        // create DDL for Database Definition
        DBSQLScript script = new DBSQLScript();
        db.getCreateDDLScript(driver, script);
        
        // Show DDL Statement
        LOGGER.info(script.toString());
        // Execute Script
        script.executeAll(driver, conn, false);
        // Commit
        db.commit(conn);
    }

    private List<String> getTableNames(DBDatabase db, Connection conn)
    {
        List<String> tables = new ArrayList<String>();
        ResultSet result = null;
        try
        {
            DatabaseMetaData metaData = conn.getMetaData();
            result = metaData.getTables(db.getSchema(), null, null, null);
            while(result.next()) {
                tables.add(result.getString("TABLE_NAME"));
            }
        } 
        catch (SQLException e)
        {
            e.printStackTrace();
        }finally{
            if (result != null){
                try
                {
                    result.close();
                } 
                catch (SQLException e)
                {
                    e.printStackTrace();
                }
            }
        }
        return tables;
    }

    public static class SampleDB extends DBDatabase {

        private static final long serialVersionUID = 1L;
        public final SampleTable SAMPLE = new SampleTable(this);

        public class SampleTable extends DBTable {

            private static final long serialVersionUID = 1L;
            public final DBTableColumn MY_INTEGER;
            public final DBTableColumn MY_LONG;
            public final DBTableColumn MY_SHORT;

            public SampleTable(DBDatabase db) {
                super("SAMPLE", db);
                MY_INTEGER = addColumn("MY_INTEGER", DataType.INTEGER, 0, true);
                MY_LONG    = addColumn("MY_LONG",    DataType.INTEGER, 8, true);
                MY_SHORT   = addColumn("MY_SHORT",   DataType.INTEGER, 2, true);
            }
        }
    }

    public static class SampleConfig {

        public String databaseProvider;
        public String jdbcClass;
        public String jdbcURL;
        public String jdbcUser;
        public String jdbcPwd;
        public String schemaName;
    }
}
