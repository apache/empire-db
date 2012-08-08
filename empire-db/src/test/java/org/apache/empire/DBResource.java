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
package org.apache.empire;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.empire.db.DBDatabaseDriver;
import org.apache.empire.db.DBTools;
import org.apache.empire.db.derby.DBDatabaseDriverDerby;
import org.apache.empire.db.h2.DBDatabaseDriverH2;
import org.apache.empire.db.hsql.DBDatabaseDriverHSql;
import org.apache.empire.db.postgresql.DBDatabaseDriverPostgreSQL;
import org.apache.empire.db.sqlite.DBDatabaseDriverSQLite;
import org.apache.empire.db.sqlserver.DBDatabaseDriverMSSQL;
import org.junit.rules.ExternalResource;

public class DBResource extends ExternalResource
{
    private final DB db;
    
    public Connection connection;
    
    public DBResource(final DB db)
    {
        this.db = db;
    }
    
    public DBDatabaseDriver newDriver(){
        try
        {
            return db.driver.newInstance();
        } 
        catch (InstantiationException e)
        {
            throw new RuntimeException(e);
        } 
        catch (IllegalAccessException e)
        {
            throw new RuntimeException(e);
        }
    }
    
    public Connection getConnection()
    {
        return connection;
    }
    
    @Override
    protected void before()
        throws Throwable
    {   
        Class.forName(db.jdbcClass);
        String user = db.username != null ? db.username:"sa";
        String password = db.password != null ? db.password:"";
        connection = DriverManager.getConnection(db.jdbcURL, user, password);
    }
    
    @Override
    protected void after()
    {
        if(db == DB.HSQL){
            try
            {
                Statement st = connection.createStatement();
                // properly shutdown hsqldb
                st.execute("SHUTDOWN");
            } catch (SQLException ex)
            {
                ex.printStackTrace();
            }
        }
        try
        {
            DBTools.close(connection);
        } catch (SQLException e)
        {
            e.printStackTrace();
        }
    }
    
    public enum DB{
        SQLITE(
                "org.sqlite.JDBC", 
                "jdbc:sqlite::memory:",
                DBDatabaseDriverSQLite.class),
        HSQL(
             "org.hsqldb.jdbcDriver", 
             "jdbc:hsqldb:mem:data/derby/test",
             DBDatabaseDriverHSql.class),
        DERBY(
              "org.apache.derby.jdbc.EmbeddedDriver", 
              "jdbc:derby:memory:data/derby/test;create=true",
              DBDatabaseDriverDerby.class),
        H2(
              "org.h2.Driver", 
              "jdbc:h2:mem:data/h2/test",
              DBDatabaseDriverH2.class),
        POSTGRESQL(
              "org.postgresql.Driver", 
              "jdbc:postgresql://localhost",
              DBDatabaseDriverPostgreSQL.class,
              "postgres",
              "postgres"),
        MSSQL(
              "com.microsoft.sqlserver.jdbc.SQLServerDriver", 
              "jdbc:sqlserver://localhost:1433",
              DBDatabaseDriverMSSQL.class),
        MSSQL_JTDS(
              // http://jtds.sourceforge.net/faq.html#driverImplementation
              "net.sourceforge.jtds.jdbc.Driver",
              "jdbc:jtds:sqlserver://localhost/databasename;instance=sqlexpress;domain=mydomain",
              DBDatabaseDriverMSSQL.class);

        private final String jdbcClass;
        private final String jdbcURL;
        private final String username;
        private final String password;
        private final Class<? extends DBDatabaseDriver> driver;
        
        private DB(final String jdbcClass, final String jdbcURL, final Class<? extends DBDatabaseDriver> driver)
        {
        	this(jdbcClass, jdbcURL, driver, null, null);
        }
        
        private DB(final String jdbcClass, final String jdbcURL, final Class<? extends DBDatabaseDriver> driver, final String username, final String password)
        {
            this.driver = driver;
            this.jdbcClass = jdbcClass;
            this.jdbcURL = jdbcURL;
            this.username = username;
            this.password = password;
        }
    }
}
