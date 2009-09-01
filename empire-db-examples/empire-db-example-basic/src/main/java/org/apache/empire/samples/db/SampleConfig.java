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
package org.apache.empire.samples.db;

import org.apache.empire.xml.XMLConfiguration;

/**
 * <PRE>
 * The SampleConfig class provides access to configuration settings.
 * The configruation will be read from a xml configuration file (usually config.xml) 
 * Thus the default values here will be overridden. 
 * </PRE>
 */
public class SampleConfig extends XMLConfiguration
{

    private String databaseProvider = "hsqldb";

    private String jdbcClass = "org.hsqldb.jdbcDriver";

    private String jdbcURL = "jdbc:hsqldb:file:hsqldb/sample;shutdown=true";

    private String jdbcUser = "sa";

    private String jdbcPwd = "";

    private String schemaName = "DBSAMPLE";

    /**
     * Initialize the configuration.
     * 
     * @param filename the file to read
     * 
     * @return true on succes
     */
    public boolean init(String filename)
    {
        // Read the properties file
        if (super.init(filename, false, true) == false)
            return false;
        // Done
        if (readProperties(this, "properties")==false)
            return false;
        // Reader Provider Properties
        return readProperties(this, "properties-" + databaseProvider);
    }

    public String getDatabaseProvider()
    {
        return databaseProvider;
    }

    public String getJdbcClass()
    {
        return jdbcClass;
    }

    public void setJdbcClass(String jdbcClass)
    {
        this.jdbcClass = jdbcClass;
    }

    public String getJdbcPwd()
    {
        return jdbcPwd;
    }

    public void setJdbcPwd(String jdbcPwd)
    {
        this.jdbcPwd = jdbcPwd;
    }

    public String getJdbcURL()
    {
        return jdbcURL;
    }

    public String getSchemaName()
    {
        return schemaName;
    }

    // ------- Setters -------

    public void setDatabaseProvider(String databaseProvider)
    {
        this.databaseProvider = databaseProvider;
    }

    public void setJdbcURL(String jdbcURL)
    {
        this.jdbcURL = jdbcURL;
    }

    public String getJdbcUser()
    {
        return jdbcUser;
    }

    public void setJdbcUser(String jdbcUser)
    {
        this.jdbcUser = jdbcUser;
    }

    public void setSchemaName(String schemaName)
    {
        this.schemaName = schemaName;
    }

}
