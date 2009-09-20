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
package org.apache.empire.db.codegen;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.logging.Logger;

import org.apache.empire.commons.ErrorObject;

public class CodeGen
{
    public static Logger logger = Logger.getLogger(CodeGen.class.getName());

    private static CodeGenConfig config = new CodeGenConfig();

    /**
     * <PRE>
     * This is the entry point of the Empire-DB Sample Application
     * Please check the config.xml configuration file for Database and Connection settings.
     * </PRE>
     * @param args arguments
     */
    public static void main(String[] args)
    {
        Connection conn = null;
        try
        {
            // Init Configuration
            config.init((args.length > 0 ? args[0] : "config.xml" ));

            // Enable Exceptions
            ErrorObject.setExceptionsEnabled(true);

            // Get a JDBC Connection
            conn = getJDBCConnection();
            
            // Get Metadata
            DatabaseMetaData dmd = conn.getMetaData();
            
            // Process Metadata
            // ....
            ResultSet rs = dmd.getCatalogs();
            while (rs.next()) {
                System.out.println(rs.getString(1));
            }
            rs.close();
            
        } catch(Exception e) {
            // Error
            System.out.println(e.toString());
            e.printStackTrace();
        } finally {
            // done
            if (conn!=null)
                close(conn);
        }
    }
    
    /**
     * <PRE>
     * Opens and returns a JDBC-Connection.
     * JDBC url, user and password for the connection are obained from the SampleConfig bean
     * Please use the config.xml file to change connection params.
     * </PRE>
     */
    private static Connection getJDBCConnection()
    {
        // Establish a new database connection
        Connection conn = null;
        logger.info("Connecting to Database'" + config.getJdbcURL() + "' / User=" + config.getJdbcUser());
        try
        {
            // Connect to the databse
            Class.forName(config.getJdbcClass()).newInstance();
            conn = DriverManager.getConnection(config.getJdbcURL(), config.getJdbcUser(), config.getJdbcPwd());
            logger.info("Connected successfully");
            // set the AutoCommit to false this session. You must commit
            // explicitly now
            conn.setAutoCommit(true);
            logger.info("AutoCommit is " + conn.getAutoCommit());

        } catch (Exception e)
        {
            logger.severe("Failed to connect directly to '" + config.getJdbcURL() + "' / User=" + config.getJdbcUser());
            logger.severe(e.toString());
            throw new RuntimeException(e);
        }
        return conn;
    }
    
    /**
     * Closes a JDBC-Connection.
     */
    private static void close(Connection conn)
    {
        logger.info("Closing database connection");
        try {
            conn.close();
        } catch (Exception e) {
            logger.severe("Error closing connection");
            logger.severe(e.toString());
        }
    }
    
}
