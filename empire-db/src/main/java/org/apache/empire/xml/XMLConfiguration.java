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

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.empire.commons.EmpireException;
import org.apache.empire.commons.Errors;
import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.commons.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class XMLConfiguration
{
    private static final Logger log = LoggerFactory.getLogger(XMLConfiguration.class);

    private Element configRootNode = null;

    /**
     * Initialize the configuration.
     * 
     * @param filename the file
     * @param fromResource will read from the classpath if true
     * 
     * @return true on success
     */
    public void init(String filename, boolean fromResource)
    {
        // Read the properties file
        readConfiguration(filename, fromResource);
    }
    
    /**
     * returns the configuration root element or null if init() has not been called.
     * @return the configuration root element
     */
    public Element getRootNode()
    {
        return configRootNode;
    }

    /**
     * Reads the configuration file and parses the XML Configuration.
     */
    protected void readConfiguration(String fileName, boolean fromResource)
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
        } catch (FileNotFoundException e)
        {
            log.error("Configuration file {} not found!", fileName, e);
            throw new EmpireException(Errors.FileNotFound, fileName);
        } catch (IOException e)
        {
            log.error("Error reading configuration file {}", fileName, e);
            throw new EmpireException(Errors.FileReadError, fileName);
        } catch (SAXException e)
        {
            log.error("Invalid XML in configuration file {}", fileName, e);
            throw new EmpireException(e);
        } catch (ParserConfigurationException e)
        {
            log.error("ParserConfigurationException: {}", e.getMessage(), e);
            throw new EmpireException(e);
        } finally
        { 
        	close(reader);
        	close(inputStream);
        }
    }

    /**
     * reads all properties from a given properties node and applies them to the given bean
     * @param bean the bean to which to apply the configuration
     * @param propertiesNodeName the name of the properties node below the root element
     * @return true of successful or false otherwise
     */
    public void readProperties(Object bean, String... propertiesNodeNames)
    {
        // Check state
        if (configRootNode == null)
            throw new EmpireException(Errors.ObjectNotValid, getClass().getName());
        // Check arguments
        if (bean == null)
            throw new EmpireException(Errors.InvalidArg, null, "bean");
        
        Element propertiesNode = configRootNode;  
        for(String nodeName : propertiesNodeNames)
        {
            if (StringUtils.isEmpty(nodeName))
                throw new EmpireException(Errors.InvalidArg, null, "propertiesNodeNames");
            // Get configuration node
            propertiesNode = XMLUtil.findFirstChild(propertiesNode, nodeName);
            if (propertiesNode == null)
            { // Configuration
                log.error("Property-Node {} has not been found.", nodeName);
                throw new EmpireException(Errors.ItemNotFound, nodeName);
            }
        }
        // read the properties
        readProperties(bean, propertiesNode);
    }

    /**
     * reads all properties from a given properties node and applies them to the given bean
     * @param bean the bean to which to apply the configuration
     * @param propertiesNode the properties node
     * @return true of successful or false otherwise
     */
    public void readProperties(Object bean, Element propertiesNode)
    {
        // Check arguments
        if (propertiesNode == null)
            throw new EmpireException(Errors.InvalidArg, null, "propertiesNode");
        if (bean == null)
            throw new EmpireException(Errors.InvalidArg, null, "bean");
        // apply configuration
        log.info("reading bean properties from node: {}", propertiesNode.getNodeName());
        NodeList nodeList = propertiesNode.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++)
        {
            Node item = nodeList.item(i);
            if (item.getNodeType() != Node.ELEMENT_NODE)
                continue;
            // Get the Text and set the Property
            setPropertyValue(bean, item);
        }
    }
    
    protected void setPropertyValue(Object bean, Node item)
    {
        // Get the Text and set the Property
        String name = item.getNodeName();
        try
        {
            String newValue = XMLUtil.getElementText(item);
            BeanUtils.setProperty(bean, name, newValue);

            Object value = BeanUtils.getProperty(bean, name);
            if (ObjectUtils.compareEqual(newValue, value))
            {
            	log.info("Configuration property '{}' set to \"{}\"", name, newValue);
            }
            else
            {
            	log.error("Failed to set property '{}'. Value is \"{}\"", name, value);
            }

        } catch (IllegalAccessException e)
        {
            log.error("Access to Property {} denied.", name);
        } catch (InvocationTargetException e)
        {
            log.error("Unable to set Property {}", name);
        } catch (NoSuchMethodException e)
        {
            log.error("Property '{}' not found in {}", name, bean.getClass().getName());
        }
    }
    
	private void close(final Closeable closeable) {
		if (closeable != null)
		{
			try
			{
				closeable.close();
			}
			catch(IOException e)
			{
				log.debug(e.getMessage());
			}
		}
	}

}
