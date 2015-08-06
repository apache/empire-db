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
package org.apache.empire.db.eclipse;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.empire.db.eclipse.model.JdbcDriverClass;
import org.apache.empire.db.eclipse.service.ConfigFileService;
import org.apache.empire.db.eclipse.service.ConfigFileServiceImpl;
import org.apache.empire.db.eclipse.service.MessageService;
import org.apache.empire.db.eclipse.service.MessageServiceImpl;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The activator class controls the plug-in life cycle
 */
public class Plugin extends AbstractUIPlugin
{
    private static final Logger                log = LoggerFactory.getLogger(Plugin.class);
    // The shared instance
    private static Plugin                      plugin;

    private final MessageService               messageService;

    private final ConfigFileService            configFileService;

    private final Map<String, JdbcDriverClass> driverClasses;

    /**
     * The constructor
     */
    public Plugin()
    {
        this.messageService = new MessageServiceImpl(Locale.ENGLISH);
        // check config dir
        File checkConfigDir = new File(PluginConsts.CONFIG_DIR_PATH, PluginConsts.CONFIG_DIR);
        if (!checkConfigDir.exists())
        {
            checkConfigDir.mkdirs();
        }
        // check default config file
        checkConfigFile(PluginConsts.DEFAULT_CONFIG_FILE);
        // check database generation templates
        checkConfigFile(PluginConsts.TEMPLATE_BASE_RECORD);
        checkConfigFile(PluginConsts.TEMPLATE_BASE_TABLE);
        checkConfigFile(PluginConsts.TEMPLATE_BASE_VIEW);
        checkConfigFile(PluginConsts.TEMPLATE_DATABASE);
        checkConfigFile(PluginConsts.TEMPLATE_RECORD);
        checkConfigFile(PluginConsts.TEMPLATE_TABLE);
        checkConfigFile(PluginConsts.TEMPLATE_VIEW);

        this.configFileService = new ConfigFileServiceImpl(checkConfigDir);
        this.driverClasses = new HashMap<String, JdbcDriverClass>();
        this.driverClasses.put("Oracle", new JdbcDriverClass("oracle.jdbc.driver.OracleDriver", "jdbc:oracle:thin:@%s:%s:%s"));
        this.driverClasses.put("Microsoft SQL Server", new JdbcDriverClass("com.microsoft.sqlserver.jdbc.SQLServerDriver",
                                                                           "jdbc:sqlserver://%s:%s;databaseName=%s"));
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
     */
    @Override
    public void start(BundleContext context)
        throws Exception
    {
        Plugin.log.debug("Start empire-db-codegen plugin");
        super.start(context);
        Plugin.plugin = this;

    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
     */
    @Override
    public void stop(BundleContext context)
        throws Exception
    {
        Plugin.log.debug("Stop empire-db-codegen plugin");
        Plugin.plugin = null;
        super.stop(context);
    }

    /**
     * Returns the shared instance
     *
     * @return the shared instance
     */
    public static Plugin getInstance()
    {
        return Plugin.plugin;
    }

    public MessageService getMessageService()
    {
        return this.messageService;
    }

    public ConfigFileService getConfigFileService()
    {
        return this.configFileService;
    }

    public Map<String, JdbcDriverClass> getDriverClasses()
    {
        return this.driverClasses;
    }

    public String[] getDriverClassNames()
    {
        String[] driverClassNames = new String[this.driverClasses.size() + 1];
        driverClassNames[0] = " ";
        int i = 1;
        for (String key : this.driverClasses.keySet())
        {
            driverClassNames[i] = key;
            i++;
        }
        return driverClassNames;
    }

    public Connection getJDBCConnection(String connectionType, String jdbcServer, String jdbcPort, String jdbcDatabase, String jdbcUser,
                                        String jdbcPwd)
        throws SQLException
    {
        JdbcDriverClass jdbcDriverClass = this.driverClasses.get(connectionType);
        String jdbcClass = jdbcDriverClass.getJdbcClass();
        String jdbcURL = String.format(jdbcDriverClass.getJdbcUrl(), jdbcServer, jdbcPort, jdbcDatabase);
        Plugin.log.info("Connecting to Database'" + jdbcURL + "' / User=" + jdbcUser);
        Connection conn = null;
        try
        {
            Class.forName(jdbcClass).newInstance();
        }
        catch (Exception ex)
        {
            throw new SQLException("Could not load database driver: " + jdbcClass);
        }
        conn = DriverManager.getConnection(jdbcURL, jdbcUser, jdbcPwd);
        Plugin.log.info("Connected successfully");
        return conn;
    }

    private void checkConfigFile(String filename)
    {
        File configFile = new File(PluginConsts.CONFIG_DIR_PATH, filename);
        if (!configFile.exists())
        {
            URL inputUrl = Plugin.class.getClassLoader().getResource(filename);
            try
            {
                FileUtils.copyURLToFile(inputUrl, configFile);
            }
            catch (IOException e)
            {
                Plugin.log.error("Could not copy resoucres file in configuration directory! %s", e.getMessage());
            }
        }
    }
}
