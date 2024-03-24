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
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.commons.StringUtils;
import org.apache.empire.exceptions.EmpireException;
import org.apache.empire.exceptions.FileParseException;
import org.apache.empire.exceptions.FileReadException;
import org.apache.empire.exceptions.InternalException;
import org.apache.empire.exceptions.InvalidArgumentException;
import org.apache.empire.exceptions.ItemNotFoundException;
import org.apache.empire.exceptions.ObjectNotValidException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
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

    /*
     * Variable prefix and suffix 
     */
    protected final String VAR_BEGIN;
    protected final char   VAR_END;
    protected final char   VAR_DEFAULT;

    /**
     * Standard Constructor without variable support
     */
    public XMLConfiguration()
    {
        this(null); // No variable support for legacy purposes
    }
    
    /**
     * Constructor allowing to specify a variable prefix (e.g. $ or #)
     * @param varIndicator the variable prefix
     */
    public XMLConfiguration(String varIndicator)
    {
        this.VAR_BEGIN = (varIndicator!=null ? (varIndicator.endsWith("{") ? varIndicator : varIndicator+"{") : null); 
        this.VAR_END   = '}';
        this.VAR_DEFAULT = '|';
    }

    /**
     * Initialize the configuration.
     * 
     * @param filename the file
     * @param fromResource will read from the classpath if true
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
            throw new FileReadException(fileName, e);
        } catch (IOException e)
        {
            log.error("Error reading configuration file {}", fileName, e);
            throw new FileReadException(fileName, e);
        } catch (SAXException e)
        {
            log.error("Invalid XML in configuration file {}", fileName, e);
            throw new FileParseException(fileName, e);
        } catch (ParserConfigurationException e)
        {
            log.error("ParserConfigurationException: {}", e.getMessage(), e);
            throw new InternalException(e);
        } finally
        { 
        	close(reader);
        	close(inputStream);
        }
    }

    /**
     * reads all properties from a given properties node and applies them to the given bean
     * @param bean the bean to which to apply the configuration
     * @param propertiesNodeNames the name of the properties node below the root element
     */
    public void readProperties(Object bean, String... propertiesNodeNames)
    {
        // Check state
        if (configRootNode == null)
            throw new ObjectNotValidException(this);
        // Check arguments
        if (bean == null)
            throw new InvalidArgumentException("bean", bean);
        
        Element propertiesNode = configRootNode;  
        for(String nodeName : propertiesNodeNames)
        {
            if (StringUtils.isEmpty(nodeName))
                throw new InvalidArgumentException("propertiesNodeNames", null);
            // Get configuration node
            propertiesNode = XMLUtil.findFirstChild(propertiesNode, nodeName);
            if (propertiesNode == null)
            { // Configuration
                log.warn("Property-Node {} has not been found.", nodeName);
                throw new ItemNotFoundException(nodeName);
            }
        }
        // read the properties
        readProperties(bean, propertiesNode);
    }

    /**
     * reads all properties from a given properties node and applies them to the given bean
     * @param bean the bean to which to apply the configuration
     * @param propertiesNode the properties node
     */
    public void readProperties(Object bean, Element propertiesNode)
    {
        // Check arguments
        if (propertiesNode == null)
            throw new InvalidArgumentException("propertiesNode", propertiesNode);
        if (bean == null)
            throw new InvalidArgumentException("bean", bean);
        // apply configuration
        log.info("reading bean properties from node: {}", propertiesNode.getNodeName());
        NodeList nodeList = propertiesNode.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++)
        {
            Node item = nodeList.item(i);
            if (item.getNodeType() != Node.ELEMENT_NODE)
                continue;
            // Get the Text and set the Property
            if (item instanceof Element)
                setPropertyValue(bean, (Element)item);
        }
    }
    
    /*
    protected boolean isProperty(Node item)
    {
        NamedNodeMap map = item.getAttributes();
        if (map==null)
            return false; // not an element?
        Node propAttr = map.getNamedItem("property");
        if (propAttr==null)
            return true; // assume yes
        String value = propAttr.getNodeValue();
        if (value==null)
            return true; // assume yes
        return ObjectUtils.getBoolean(value);
    }
    */
    
    /**
     * Sets the property value of an XML Element 
     * @param bean the java bean containing the property 
     * @param item the configuration node
     */
    @SuppressWarnings("unchecked")
    protected void setPropertyValue(Object bean, Element item)
    {
        String name = item.getNodeName();
        String prop = name;
        try
        {
            Class<?> valueType = PropertyUtils.getPropertyType(bean, name);
            // Has Attributes?
            if (item.hasAttributes() && Map.class.isAssignableFrom(valueType))
            {   // It's a map type
                Object value = PropertyUtils.getNestedProperty(bean, name);
                Map<String,Object> map;
                if (value instanceof Map)
                {   // Use existing
                    map = (Map<String,Object>)value;
                }
                else
                {   // Create and Set
                    map = new HashMap<String, Object>();
                    PropertyUtils.setProperty(bean, name, map);
                }
                // copy to map
                NamedNodeMap nm = item.getAttributes();
                for (int i = 0; i<nm.getLength(); i++)
                {
                    Node attrib = nm.item(i);
                    prop = attrib.getNodeName();
                    String propVal = attrib.getNodeValue();
                    map.put(prop, resolveValue(valueType, propVal));
                    log.info("Configuration Map property '{}' has been set to \"{}\"", prop, map.get(prop));
                }
                return; 
            }
            // default
            Object newValue = resolveValue(valueType, XMLUtil.getElementText(item));
            setPropertyValue(bean, name, newValue);
        } catch (IllegalAccessException e) {
            log.error("Config error: Access to '{}' in {} denied.", prop, bean.getClass().getName(), e);
        } catch (InvocationTargetException e) {
            log.error("Config error: Unable to set value for '{}' in {}", prop, bean.getClass().getName(), e);
        } catch (NoSuchMethodException e) {
            log.error("Config error: Property '{}' in {} not found. Property is ignored.", prop, bean.getClass().getName(), e);
        } catch (EmpireException e) {
            log.error("Config error: Invalid Value for '{}' in {}", prop, bean.getClass().getName(), e);
        }
    }
    
    /**
     * Sets a property value
     * @param bean the name of the object
     * @param name the name of the property
     * @param value the value
     */
    protected void setPropertyValue(Object bean, String name, Object value)
        throws IllegalAccessException, InvocationTargetException, NoSuchMethodException
    {
        // This method provides type conversion
        BeanUtils.setProperty(bean, name, value);

        // Check
        // Object newValue = BeanUtils.getProperty(bean, name);
        Object newValue = PropertyUtils.getProperty(bean, name);
        if (ObjectUtils.compareEqual(newValue, value))
        {   // Success
            log.info("Configuration property '{}' has been set to \"{}\"", name, value);
        }
        else
        {   // Property value does not match
            log.error("Config error: Failed to set '{}' to \"{}\" in {}. Value is \"{}\" instead.", name, value, bean.getClass().getName(), value);
        }
    }
    
    /**
     * Resolves a property value by replacing variables and type conversion
     * @param valueType the destination type
     * @param value the config value
     * @return the property value
     */
    protected Object resolveValue(Class<?> valueType, String value)
    {
        if (value==null)
            return null;
        int beg = (StringUtils.isNotEmpty(VAR_BEGIN) ? value.indexOf(VAR_BEGIN) : -1);
        if (beg<0)
            return value;
        // resolve variable
        int end = findVarEnd(value, beg+2);
        if (end<0)
            throw new InvalidArgumentException("value", value);
        // resolve
        String defaultValue = null; 
        String var = value.substring(beg+2, end);
        int def = var.indexOf(VAR_DEFAULT);
        if (def>0)
        {   // Default value provided
            defaultValue = (String)resolveValue(String.class, var.substring(def+1));
            var = var.substring(0, def);
        }
        String varVal = resolveVariable(var, defaultValue);
        if (varVal==null)
            throw new ItemNotFoundException(var);
        StringBuilder b = new StringBuilder(value.length()+varVal.length()-(end-beg)-1);
        b.append(value.substring(0, beg));
        b.append(varVal);
        b.append(value.substring(end+1));
        return resolveValue(valueType, b.toString());
    }

    /**
     * Returns the value for a given variable
     * If a variable value cannot be resolved the function should return the default value
     * @param var the variable name
     * @param defaultValue the default value or null
     * @return the variable value
     */
    protected String resolveVariable(String var, String defaultValue)
    {
        log.debug("Config: Resolving variable {}", var);
        String value = System.getProperty(var);
        return (value!=null ? value : defaultValue);
    }
    
    /* Implementation of toString */
    
    private static String EOL = "\r\n";
    private static String EOC = "----------------------------------------\r\n";  // end of class
    
    @Override
    public String toString()
    {
        StringBuilder b = new StringBuilder(EOC);
        Class<?> clazz = getClass();
        while (clazz!=null && clazz!=XMLConfiguration.class)
        {
            appendConfigProperties(clazz, this, b, true);
            clazz = clazz.getSuperclass();
        }
        return b.toString();
    }
    
    protected void appendConfigProperties(Class<?> clazz, Object object, StringBuilder b, boolean appendClassInfo)
    {
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            // ignore static fields
            int modifiers = field.getModifiers();
            if (Modifier.isStatic(modifiers) || Modifier.isFinal(modifiers))
                continue;
            // make accessible
            try
            {
                Object value = PropertyUtils.getProperty(object, field.getName());
                if (value!=null && !(value instanceof Map<?,?>))
                {
                    Class<?> vc = value.getClass();
                    boolean simple = (vc.isPrimitive() || vc.isEnum() 
                         || vc == String.class  || vc == Character.class || vc == Byte.class 
                         || vc == Integer.class || vc == Long.class  || vc == Short.class 
                         || vc == Double.class  || vc == Float.class || vc == Boolean.class
                         || vc == Class.class);
                    // Nested?
                    if (!simple)
                    {   // Nested Class
                        b.append(EOC);
                        b.append(field.getName());
                        b.append("[");
                        b.append(clazz.getName());
                        b.append("]=");
                        b.append(EOL);
                        appendConfigProperties(value.getClass(), value, b, false);
                        return;
                    }
                }
                // class info 
                if (appendClassInfo)
                {   appendClassInfo = false;
                    b.append("[Properties of ");
                    b.append(clazz.getName());
                    b.append("]");
                    b.append(EOL);
                }
                // property value
                b.append(field.getName());
                b.append("=");
                b.append(String.valueOf(value));
                b.append(EOL);
            }
            catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e)
            {
                log.warn("Field {} is ignored due to Exception {}", field.getName(), e.toString());
            }
        }
        if (appendClassInfo==false)
            b.append(EOC);
    }
    
    /* helpers */
    
    private int findVarEnd(String value, int from)
    {
        final char NEST_CHAR = VAR_BEGIN.charAt(VAR_BEGIN.length()-1);
        for (int nestCount = 0;from<value.length();from++)
        {
            char c = value.charAt(from);
            if (c==VAR_END)
            {   // Nested Variable
                if (nestCount==0)
                    return from;
                nestCount--;
            }
            if (c==NEST_CHAR)
                nestCount++;
        }
        return -1; // not found
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
				log.error("Failed to close stream: "+e.getMessage(), e);
			}
		}
	}

}
