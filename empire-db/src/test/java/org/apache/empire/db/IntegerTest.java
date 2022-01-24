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
import org.apache.empire.db.DBDDLGenerator.DDLActionType;
import org.apache.empire.db.context.DBContextStatic;
import org.apache.empire.dbms.DBMSHandler;
import org.apache.empire.dbms.derby.DBMSHandlerDerby;
import org.apache.empire.dbms.h2.DBMSHandlerH2;
import org.apache.empire.dbms.hsql.DBMSHandlerHSql;
import org.apache.empire.dbms.mysql.DBMSHandlerMySQL;
import org.apache.empire.dbms.oracle.DBMSHandlerOracle;
import org.apache.empire.dbms.postgresql.DBMSHandlerPostgreSQL;
import org.apache.empire.dbms.sqlite.DBMSHandlerSQLite;
import org.apache.empire.dbms.sqlserver.DBMSHandlerMSSQL;
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

        DBMSHandler dbmsHandler = getDBMSHandler(config, connection);
        DBContext context = new DBContextStatic(dbmsHandler, connection); 

        database.open(context);
        createDatabase(database, context);

        DBRecord maxRec = new DBRecord(context, database.SAMPLE);
        maxRec.create();
        maxRec.setValue(database.SAMPLE.MY_INTEGER, Integer.MAX_VALUE);
        maxRec.setValue(database.SAMPLE.MY_LONG, Long.MAX_VALUE);
        maxRec.setValue(database.SAMPLE.MY_SHORT, Short.MAX_VALUE);
        maxRec.update();
        
        DBRecord minRec = new DBRecord(context, database.SAMPLE);
        minRec.create();
        minRec.setValue(database.SAMPLE.MY_INTEGER, Integer.MIN_VALUE);
        minRec.setValue(database.SAMPLE.MY_LONG, Long.MIN_VALUE);
        minRec.setValue(database.SAMPLE.MY_SHORT, Short.MIN_VALUE);
        minRec.update();
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

    private DBMSHandler getDBMSHandler(SampleConfig config, Connection conn) {
        if (config.databaseProvider.equalsIgnoreCase("sqlite")) {
            DBMSHandlerSQLite dbms = new DBMSHandlerSQLite();
            return dbms;
        }
        if (config.databaseProvider.equalsIgnoreCase("mysql")) {
            DBMSHandlerMySQL dbms = new DBMSHandlerMySQL();
            dbms.setDatabaseName(config.schemaName);
            return dbms;
        }
        if (config.databaseProvider.equalsIgnoreCase("oracle")) {
            DBMSHandlerOracle dbms = new DBMSHandlerOracle();
            return dbms;
        }
        if (config.databaseProvider.equalsIgnoreCase("sqlserver")) {
            DBMSHandlerMSSQL dbms = new DBMSHandlerMSSQL();
            dbms.setDatabaseName(config.schemaName);
            return dbms;
        }
        if (config.databaseProvider.equalsIgnoreCase("hsqldb")) {
            DBMSHandlerHSql dbms = new DBMSHandlerHSql();
            return dbms;
        }
        if (config.databaseProvider.equalsIgnoreCase("postgresql")) {
            DBMSHandlerPostgreSQL dbms = new DBMSHandlerPostgreSQL();
            dbms.setDatabaseName(config.schemaName);
            return dbms;
        }
        if (config.databaseProvider.equalsIgnoreCase("h2")) {
            DBMSHandlerH2 dbms = new DBMSHandlerH2();
            dbms.setDatabaseName(config.schemaName);
            return dbms;
        }
        if (config.databaseProvider.equalsIgnoreCase("derby")) {
            DBMSHandlerDerby dbms = new DBMSHandlerDerby();
            dbms.setDatabaseName(config.schemaName);
            return dbms;
        }

        throw new RuntimeException("Unknown Database Provider " + config.databaseProvider);
    }

    private void createDatabase(DBDatabase db, DBContext context) {
        // try to remove previously existing tables
        List<String> tables = getTableNames(db, context.getConnection());
        DBSQLScript script2 = new DBSQLScript(context);
        for(DBTable table:db.getTables()){
            if(tables.contains(table.getName())){
                db.getDbms().getDDLScript(DDLActionType.DROP, table, script2);
            }
        }
        script2.executeAll(false);
        // Commit
        context.commit();
        
        // create DDL for Database Definition
        DBSQLScript script = new DBSQLScript(context);
        db.getCreateDDLScript(script);
        
        // Show DDL Statement
        LOGGER.info(script.toString());
        // Execute Script
        script.executeAll(false);
        // Commit
        context.commit();
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

        // *Deprecated* private static final long serialVersionUID = 1L;
        public final SampleTable SAMPLE = new SampleTable(this);

        public class SampleTable extends DBTable {

            // *Deprecated* private static final long serialVersionUID = 1L;
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
