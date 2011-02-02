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
package org.apache.empire.xml;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.beanutils.BeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.empire.commons.ErrorObject;
import org.apache.empire.commons.Errors;
import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.commons.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;


/**
 * <PRE>
 * This class manages the configuration of a Java Bean by an xml configuration file.
 * It also supports configuration of Log4J.
 * </PRE>
 *
 */
public class XMLConfiguration extends ErrorObject
{
    // Logger (not final!)
    protected static Logger log = LoggerFactory.getLogger(XMLConfiguration.class);

    private Element    configRootNode  = null;

    /**
     * Initialize the configuration.
     * 
     * @param filename the file
     * @param fromResource will read from the classpath if true
     * @param initLogging set to true to set up logging
     * 
     * @return true on succes
     */
    public boolean init(String filename, boolean fromResource, boolean initLogging)
    {
        // Read the properties file
        if (readConfiguration(filename, fromResource) == false)
            return false;
        // Done
        return success();
    }

    /**
     * Reads the configuration file and parses the XML Configuration.
     */
    protected boolean readConfiguration(String fileName, boolean fromResource)
    {
        FileReader reader = null;
        InputStream inputStream = null;
        try
        {
            DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = null;
            if (fromResource)
            {   // Open Resource
                log.info("reading resource file: " + fileName);
                inputStream = getClass().getClassLoader().getResourceAsStream(fileName);
                // Parse File
                doc = docBuilder.parse(inputStream);
            }
            else
            {   // Open File
                log.info("reading configuration file: " + fileName);
                reader = new FileReader(fileName);
                // Parse File
                doc = docBuilder.parse(new InputSource(reader));
            }
            // Get Root Element
            configRootNode = doc.getDocumentElement();
            return success();
        } catch (FileNotFoundException e)
        {
            log.error("Configuration file not found! filename=" + fileName);
            return error(Errors.FileNotFound, fileName);
        } catch (IOException e)
        {
            log.error("Error reading configuration file " + fileName);
            return error(Errors.FileReadError, fileName);
        } catch (SAXException e)
        {
            log.error("Invalid XML in configuraion file " + fileName, e);
            return error(e);
        } catch (ParserConfigurationException e)
        {
            log.error("ParserConfigurationException ", e);
            return error(e);
        } finally
        { // Close reader
            try
            {
                if (reader != null)
                    reader.close();
                if (inputStream != null)
                    inputStream.close();
            } catch (Exception e)
            {
                // Nothing to do.
            }
        }
    }

    public boolean readProperties(Object bean, String propertiesNodeName)
    {
        // Check state
        if (configRootNode == null)
            return error(Errors.ObjectNotValid, getClass().getName());
        // Check arguments
        if (bean == null)
            return error(Errors.InvalidArg, null, "bean");
        if (StringUtils.isValid(propertiesNodeName) == false)
            return error(Errors.InvalidArg, null, "propertiesNodeName");
        // Get Configuraiton Node
        Element propertiesNode = XMLUtil.findFirstChild(configRootNode, propertiesNodeName);
        if (propertiesNode == null)
        { // Configuration
            log.error("Property-Node " + propertiesNodeName + " has not been found.");
            return error(Errors.ItemNotFound, propertiesNodeName);
        }
        // configure Connection
        log.info("reading bean properties from node: " + propertiesNodeName);
        NodeList nodeList = propertiesNode.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++)
        {
            Node item = nodeList.item(i);
            if (item.getNodeType() != Node.ELEMENT_NODE)
                continue;
            // Get the Text and set the Property
            setPropertyValue(bean, item);
        }
        // done
        return success();
    }
    
    protected void setPropertyValue(Object bean, Node item)
    {
        // Get the Text and set the Property
        String name = item.getNodeName();
        try
        {
            String value = XMLUtil.getElementText(item);
            BeanUtils.setProperty(bean, name, value);

            Object check = BeanUtils.getProperty(bean, name);
            if (ObjectUtils.compareEqual(value, check)==false)
            {
                log.error("Failed to set property '" + name + "'. Value is " + String.valueOf(check));
                return;
            }

            // success
            log.info("Configuration property '" + name + "' set to \"" + value + "\"");

        } catch (IllegalAccessException e)
        {
            log.error("Access to Property " + name + " denied.");
        } catch (InvocationTargetException e)
        {
            log.error("Unable to set Property " + name);
        } catch (NoSuchMethodException e)
        {
            log.error("Property '"  + name + "' not found in " + bean.getClass().getName());
        }
    }

}
