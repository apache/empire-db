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

import org.apache.empire.commons.Errors;
import org.apache.empire.xml.XMLConfiguration;
import org.apache.empire.xml.XMLUtil;
import org.apache.log4j.xml.DOMConfigurator;
import org.slf4j.Logger;
import org.w3c.dom.Element;
/**
 * <PRE>
 * The SampleConfig class provides access to configuration settings.
 * The configuration will be read from a xml configuration file (usually config.xml) 
 * Thus the default values here will be overridden. 
 * </PRE>
 */
public class SampleConfig extends XMLConfiguration
{
	private static final Logger log = org.slf4j.LoggerFactory.getLogger(SampleConfig.class);
	
    // the logging configuration root node name
    private final String loggingNodeName = "log4j:configuration";

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
     * @return true on success
     */
    public boolean init(String filename)
    {
        // Read the properties file
        if (super.init(filename, false) == false)
            return false;
        // Init Logging
        initLogging();
        // Done
        if (readProperties(this, "properties")==false)
            return false;
        // Reader Provider Properties
        return readProperties(this, "properties-" + databaseProvider);
    }

    /**
     * Init logging using Log4J's DOMConfigurator 
     * @return
     */
    private boolean initLogging()
    {
        // Get configuration root node
        Element rootNode = getRootNode();
        if (rootNode == null)
            return error(Errors.ObjectNotValid, getClass().getName());
        // Find log configuration node
        Element loggingNode = XMLUtil.findFirstChild(rootNode, loggingNodeName);
        if (loggingNode == null)
        {   // log configuration node not found
            log.error("Log configuration node {} has not been found. Logging has not been configured.", loggingNodeName);
            return error(Errors.ItemNotFound, loggingNodeName);
        }
        // Init Log4J
        DOMConfigurator.configure(loggingNode);
        // done
        log.info("Logging sucessfully configured from node {}.", loggingNodeName);
        return success();
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
