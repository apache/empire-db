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
package org.apache.empire.jsf2.websample.web;

import org.apache.empire.xml.XMLConfiguration;
import org.apache.empire.xml.XMLUtil;
import org.apache.log4j.xml.DOMConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

public class SampleConfig extends XMLConfiguration
{
    // Logger
    private static final Logger log              = LoggerFactory.getLogger(SampleConfig.class);
    private String              loggingNodeName  = "log4j:configuration";

    private String              jndiContext      = "";

    private String              databaseProvider = "oracle";

    private String              jdbcClass        = "oracle.jdbc.driver.OracleDriver";

    private String              jdbcURL          = "jdbc:oracle:thin:@192.168.0.2:1521:ora10";

    private String              dataSource       = "java:comp/env/jdbc/sampleDataSource";

    private String              jdbcUser         = "DBSAMPLE";

    private String              jdbcPwd          = "DBSAMPLE";

    private String              schemaName       = "DBSAMPLE";

    /**
     * Initialize the configuration
     * 
     * @param filename the file to read
     * 
     * @return true on success
     */
    public boolean init(String filename)
    {
        try
        {   // init
            super.init(filename, false);
            // Init Logging
            if (initLogging() == false)
                return false;
            // Read config
            log.info("*** init Configuration ***");
            log.info("Config file is '{}'", filename);
            // Read the properties
            readProperties(this, "properties");
            readProperties(this, "properties-" + databaseProvider);
        } catch (Exception e)
        {
            log.error(e.getLocalizedMessage());
            return false;
        }
        return true;
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

    /**
     * Init logging using Log4J's DOMConfigurator
     * 
     * @return
     */
    private boolean initLogging()
    {
        // Get configuration root node
        Element rootNode = getRootNode();
        if (rootNode == null)
        {
            return false;
        }
        // Find log configuration node
        Element loggingNode = XMLUtil.findFirstChild(rootNode, loggingNodeName);
        if (loggingNode == null)
        { // log configuration node not found
            SampleConfig.log.error("Log configuration node {} has not been found. Logging has not been configured.", loggingNodeName);
            return false;
        }
        // Init Log4J
        DOMConfigurator.configure(loggingNode);
        // done
        SampleConfig.log.info("Logging sucessfully configured from node {}.", loggingNodeName);
        return true;
    }

    public String getDataSourceName()
    {
        return dataSource;
    }

    public void setDataSourceName(String dataSource)
    {
        this.dataSource = dataSource;
    }


    public String getJndiContextFactoryName()
    {
        return jndiContext;
    }

    public void setJndiContextFactoryName(String jndiContext)
    {
        this.jndiContext = jndiContext;
    }

    public String getAccessDeniedPage()
    {
        return null;
    }

}
