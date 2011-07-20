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

import org.apache.empire.commons.EmpireException;
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

    private String empireDBDriverClass = "";

    /**
     * Initialize the configuration.
     * 
     * @param filename the file to read
     * 
     * @return true on success
     */
    public void init(String filename)
    {
        // Read the properties file
        super.init(filename, false);
        // Init Logging
        initLogging();
        // Done
        readProperties(this, "properties");
        // Reader Provider Properties
        readProperties(this, "properties-" + databaseProvider);
    }

    /**
     * Init logging using Log4J's DOMConfigurator 
     * @return
     */
    private void initLogging()
    {
        // Get configuration root node
        Element rootNode = getRootNode();
        if (rootNode == null)
            throw new EmpireException(Errors.ObjectNotValid, getClass().getName());
        // Find log configuration node
        Element loggingNode = XMLUtil.findFirstChild(rootNode, loggingNodeName);
        if (loggingNode == null)
        {   // log configuration node not found
            log.error("Log configuration node {} has not been found. Logging has not been configured.", loggingNodeName);
            throw new EmpireException(Errors.ItemNotFound, loggingNodeName);
        }
        // Init Log4J
        DOMConfigurator.configure(loggingNode);
        // done
        log.info("Logging sucessfully configured from node {}.", loggingNodeName);
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

    public String getEmpireDBDriverClass()
    {
        return empireDBDriverClass;
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

    public void setEmpireDBDriverClass(String empireDBDriverClass)
    {
        this.empireDBDriverClass = empireDBDriverClass;
    }

}
