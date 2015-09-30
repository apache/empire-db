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
package org.apache.empire.db.eclipse.service;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.empire.commons.StringUtils;
import org.apache.empire.db.eclipse.CodeGenConfig;
import org.apache.empire.db.eclipse.Plugin;
import org.apache.empire.db.eclipse.PluginConsts;
import org.apache.empire.db.eclipse.model.ConfigFile;
import org.apache.empire.db.eclipse.model.JdbcDriverClass;
import org.apache.empire.xml.XMLWriter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class ConfigFileServiceImpl implements ConfigFileService
{
    private Map<String, ConfigFile> configurationgs;

    private final File              directory;

    public ConfigFileServiceImpl(File configDir)
    {
        this.directory = configDir;
    }

    public boolean saveConfig(ConfigFile config)
    {
        ConfigFile configFile = this.configurationgs.get(config.getUuid());
        if (configFile == null)
        {
            configFile = config;
            configFile.setFilename(config.getCodeGenConfig().getConfigTitle() + "_" + Calendar.getInstance().getTimeInMillis()
                                   + ".xml");
            configFile.setUuid(UUID.randomUUID().toString());
            this.configurationgs.put(configFile.getUuid(), configFile);
        }
        else
        {
            configFile.setCodeGenConfig(config.getCodeGenConfig());
        }
        try
        {
            File file = new File(this.directory, configFile.getFilename());
            if (!file.exists())
            {
                file.createNewFile();
            }
            // build url string
            CodeGenConfig pluginConfig = configFile.getCodeGenConfig();
            if (!StringUtils.isEmpty(pluginConfig.getJdbcType()))
            {
                JdbcDriverClass driver = Plugin.getInstance().getDriverClasses().get(pluginConfig.getJdbcType());
                pluginConfig.setJdbcURL(String.format(driver.getJdbcUrl(), pluginConfig.getJdbcServer(), pluginConfig.getJdbcPort(),
                                                      pluginConfig.getJdbcSID()));
                pluginConfig.setJdbcClass(driver.getJdbcClass());
            }
            else
            {
                pluginConfig.setJdbcURL("");
            }
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document document = db.newDocument();
            Element configElement = document.createElement("config");
            document.appendChild(configElement);
            // Marshal the Object to a Document
            JAXBContext jc = JAXBContext.newInstance(CodeGenConfig.class);
            Marshaller marshaller = jc.createMarshaller();
            marshaller.marshal(pluginConfig, configElement);
            document.renameNode(configElement.getFirstChild(), configElement.getFirstChild().getNamespaceURI(), "properties");
            configElement.appendChild(createLogginNode(document));
            XMLWriter writer = new XMLWriter(new FileOutputStream(file));
            writer.print(document);
        }
        catch (Exception e)
        {
            return false;
        }
        return true;
    }

    private Element createLogginNode(Document document)
    {
        Element log4j = document.createElement("log4j:configuration");
        log4j.setAttribute("xmlns:log4j", "http://jakarta.apache.org/log4j/");
        Element appender = document.createElement("appender");
        appender.setAttribute("name", "default");
        appender.setAttribute("class", "org.apache.log4j.ConsoleAppender");
        Element layout = document.createElement("layout");
        layout.setAttribute("class", "org.apache.log4j.PatternLayout");
        Element param = document.createElement("param");
        param.setAttribute("name", "ConversionPattern");
        param.setAttribute("value", "%-5p [%d{yyyy/MM/dd HH:mm}]: %m        at %l %n");
        layout.appendChild(param);
        appender.appendChild(layout);
        log4j.appendChild(appender);

        Element logger = document.createElement("logger");
        logger.setAttribute("name", "org.apache.empire.commons");
        logger.setAttribute("additivity", "false");
        Element level = document.createElement("level");
        level.setAttribute("value", "warn");
        Element loggerAppender = document.createElement("appender-ref");
        loggerAppender.setAttribute("ref", "default");
        logger.appendChild(level);
        logger.appendChild(loggerAppender);
        log4j.appendChild(logger);

        Element root = document.createElement("root");
        Element priority = document.createElement("priority");
        priority.setAttribute("value", "info");
        Element rootAppender = document.createElement("appender-ref");
        rootAppender.setAttribute("ref", "default");
        root.appendChild(priority);
        root.appendChild(rootAppender);
        log4j.appendChild(root);

        return log4j;
    }

    public ConfigFile getConfig(String configTitle)
    {
        for (ConfigFile configFile : this.configurationgs.values())
        {
            if (configFile.getCodeGenConfig().getConfigTitle().equals(configTitle))
            {
                return configFile;
            }
        }
        return null;
    }

    public boolean deleteConfig(String uuid)
    {
        ConfigFile configFile = this.configurationgs.get(uuid);
        if (configFile == null)
        {
            return false;
        }
        File file = new File(this.directory, configFile.getFilename());
        boolean deleted = file.delete();
        if (deleted)
        {
            this.configurationgs.remove(configFile);
        }
        return deleted;
    }

    public void refreshConfigList()
    {
        File[] files = this.directory.listFiles();
        this.configurationgs = new HashMap<String, ConfigFile>();
        if (files != null)
        {
            for (File file : files)
            {
                if (file.isFile())
                {
                    CodeGenConfig config = new CodeGenConfig();
                    try
                    {
                        config.init(file.getAbsolutePath());
                    }
                    catch (Exception e)
                    {
                        continue;
                    }
                    String uuid = UUID.randomUUID().toString();
                    this.configurationgs.put(uuid, new ConfigFile(file.getName(), uuid, config));
                }
            }
        }
    }

    public String[] getConfigTitles()
    {
        List<String> configTitles = new ArrayList<String>();
        for (ConfigFile configFile : this.configurationgs.values())
        {
            configTitles.add(configFile.getCodeGenConfig().getConfigTitle());
        }
        Collections.sort(configTitles);
        return configTitles.toArray(new String[configTitles.size()]);
    }

    public ConfigFile getDefaultConfig()
    {
        CodeGenConfig defaultConfig = new CodeGenConfig();
        File file = new File(PluginConsts.CONFIG_DIR_PATH, PluginConsts.DEFAULT_CONFIG_FILE);
        defaultConfig.init(file.getAbsolutePath());
        return new ConfigFile(defaultConfig);
    }

    public String getConfigFilePath(String uuid)
    {
        ConfigFile config = this.configurationgs.get(uuid);
        if (config == null)
        {
            return null;
        }
        return new File(new File(PluginConsts.CONFIG_DIR_PATH, PluginConsts.CONFIG_DIR), config.getFilename()).getAbsolutePath();
    }
}
