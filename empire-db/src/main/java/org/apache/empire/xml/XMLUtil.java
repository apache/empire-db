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

import java.util.Map;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This class provides a collection of static helper functions for common XML tasks.
 * The class cannot be instanciated since all.
 * methods provided are declared static.
 * <P>
 * 
 *
 */
public class XMLUtil
{
    protected static final Logger log = LoggerFactory.getLogger(XMLUtil.class);
    
    /**
     * DocumentBuilder that is aware of namespaces. This is necessary for parsing xsl files.
     */
    private static DocumentBuilder documentBuilder = null;

    /* Static initializer: */
    static
    {
        /* Instantiate a DocumentBuilderFactory. */
        DocumentBuilderFactory dFactory = DocumentBuilderFactory.newInstance();
        /* And setNamespaceAware, which is required when parsing xsl files. */
        dFactory.setNamespaceAware(true);
        try
        {
            /* Use the DocumentBuilderFactory to create a DocumentBuilder. */
            documentBuilder = dFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e)
        {
            log.error("XMLUtil::createDocument-->", e);
            e.printStackTrace();
        }
    }

    /**
     * Class contains only static functions! Constructor not available!
     */
    private XMLUtil()
    {
        /* No instances necessary */
    }

    /**
     * Returns a document newly created by the class's static DocumentBuilder.
     * 
     * @return An empty DOM document.
     */
    static public synchronized Document createDocument()
    {
        return documentBuilder.newDocument();
    }

    /**
     * Returns an initialzed, namespace aware DocumentBuilder.
     * 
     * @return The DocumentBuilder.
     */
    static public DocumentBuilder getDocumentBuilder()
    {
        return documentBuilder;
    }

    static public synchronized Element createDocument(String rootElemName)
    {
        // create document
        Document doc = createDocument();
        Element root = doc.createElement(rootElemName);
        doc.appendChild(root);
        return root;
    }

    static public synchronized Element createDocumentNS(String prefix, String rootElemName, Map<String, String> nsMap)
    {
        // create document
        Document doc = createDocument();
        // prefix
        Element root = null;
        if (prefix != null && prefix.length() > 0)
        { // Find matching URI
            String uri = nsMap.get(prefix);
            if (uri == null)
            {
                log.error("Namespace URI for namespace " + prefix + " not found!");
                return null;
            }
            // create Document
            root = doc.createElementNS(uri, prefix + ":" + rootElemName);
        } 
        else
        {
            root = doc.createElement(rootElemName);
        }
        doc.appendChild(root);
        // Add Namespace attributes
        addNamespaceURIs(doc, nsMap);
        return root;
    }

    static public boolean addNamespaceURIs(Document doc, Map<String, String> nsMap)
    {
        Element root = doc.getDocumentElement();
        if (root == null)
            return false;
        // Add Namespace attributes
        for(Entry<String, String> entry:nsMap.entrySet())
        {
            root.setAttribute("xmlns:" + entry.getKey(), entry.getValue());
        }
        return true;
    }

    static public String getNamespaceURI(Document doc, String prefix)
    {
        Element e = doc.getDocumentElement();
        return e.getAttributeNS("", prefix);
    }

    /**
     * Gets the first (direct) child Element.
     * 
     * @param parent the parent element below which to search the child
     * @return the first child element, or null otherwise
     */
    static public Element getFirstChild(Node parent)
    { // Child Element suchen
        if (parent == null)
            return null;
        Node node = parent.getFirstChild();
        while (node != null)
        { // Find all Element nodes
            if (node.getNodeType() == Node.ELEMENT_NODE)
                return (Element) node; // found
            node = node.getNextSibling();
        }
        return null; // not found!
    }

    /**
     * Finds the first (direct) child Element with a given tag name.
     * 
     * @param parent the parent element below which to search the child
     * @param tagName the (tag) name of the desired child element
     * @return the child element if an element of that name existed, or null otherwise
     */
    static public Element findFirstChild(Node parent, String tagName)
    { // Child Element suchen
        if (parent == null)
            return null;
        Node node = parent.getFirstChild();
        while (node != null)
        { // Find all Element nodes
            if (node.getNodeType() == Node.ELEMENT_NODE)
            { // check name
                Element elem = (Element) node;
                if (tagName.equalsIgnoreCase(elem.getTagName()))
                    return elem; // found
            }
            node = node.getNextSibling();
        }
        return null; // not found!
    }

    /**
     * Returns the next sibling Element for an element, optionally matching tag names.
     * 
     * @param child the element from which to search for a next sibling
     * @param sameName true to retrive the next sibling element of the same name, of false if any name is allowed
     * @return the next sibling element if one exists, or null otherwise
     */
    static public Element getNextSiblingElement(Element child, boolean sameName)
    { // Child Element suchen
        if (child == null)
            return null;
        String name = child.getTagName();
        Node node = child.getNextSibling();
        while (node != null)
        { // Find all Element nodes
            if (node.getNodeType() == Node.ELEMENT_NODE)
            { // check name
                Element elem = (Element) node;
                if (sameName && name.equalsIgnoreCase(elem.getTagName()))
                    return elem; // found
            }
            node = node.getNextSibling();
        }
        return null; // not found!
    }

    /**
     * Finds the first (direct) child element with a given tag name and attribute.
     * 
     * @param parent the parent element below which to search the child
     * @param tagName the (tag) name of the desired child element
     * @param attrName the name of the attribute which value must match the given value
     * @param value the attribute value to which elements are matched.
     * @return the child element if an element of that name existed, or null otherwise
     */
    static public Element findFirstChildWithAttrib(Node parent, String tagName, String attrName, Object value)
    {
        Element elem = findFirstChild(parent, tagName);
        if (attrName == null)
            return null;
        while (elem != null)
        { // Check Value
            String attrValue = elem.getAttribute(attrName);
            if (attrValue == null || attrValue.length() < 1)
            { // Attribute is null
                if (value == null)
                    break; // gefunden!
            } 
            else
            { // Attribute is not null
                if (attrValue.equals(value.toString()))
                    break; // gefunden!
            }
            // next
            elem = getNextSiblingElement(elem, true);
        }
        return elem;
    }

    /**
     * Finds the first element which name matchtes a given tag name
     * that is locacted anywhere below the given parent.
     * 
     * @param parent the parent element below which to search the child
     * @param tagName the (tag) name of the desired child element
     * @return the child element if an element of that name existed, or null otherwise
     */
    static public Element findFirstChildDeep(Element parent, String tagName)
    { // Child Element suchen
        if (parent == null)
            return null;
        NodeList nl = parent.getElementsByTagName(tagName);
        for (int i = 0; i < nl.getLength(); i++)
        {
            if (nl.item(i).getNodeType() == Node.ELEMENT_NODE)
                return (Element) nl.item(i);
        }
        return null;
    }

    /**
     * Returns the first element which name matchtes a given tag name.
     * 
     * @param doc the xml document in which to find an element of the given name
     * @param tagName the (tag) name of the desired child element
     * @return the child element if an element of that name existed, or null otherwise
     */
    static public Element findFirstChildDeep(Document doc, String tagName)
    { // Child Element suchen
        if (doc == null)
            return null;
        return findFirstChildDeep(doc.getDocumentElement(), tagName);
    }

    /**
     * Retrieves the text of a given element.
     * 
     * @param elem the Element for which the text value is requested
     * @return the text value of that element or null if the element has no text value
     */
    static public String getElementText(Node elem)
    {
        String value = null;
        Node node = (elem != null) ? elem.getFirstChild() : null;
        // Find Text
        while (node != null)
        { // Find all Text nodes
            if (node.getNodeType() == Node.TEXT_NODE)
            { // set or append
                if (value == null)
                    value = node.getNodeValue();
                else
                    value += node.getNodeValue();
            }
            node = node.getNextSibling();
        }
        return value;
    }

    /**
     * Sets the text value of an Element. if current text of the element is
     * replaced with the new text if text is null any
     * current text value is deleted.
     * 
     * @param elem the Element for which the text value should be set
     * @param text the new text value of the element
     * @return true if the text could be set or false otherwise
     */
    static public boolean setElementText(Node elem, Object text)
    {
        if (elem == null)
            return false; // Fehler
        // Find Text
        Node node = elem.getFirstChild();
        while (node != null)
        { // Find all Text nodes
            if (node.getNodeType() == Node.TEXT_NODE)
                break; // gefunden
            node = node.getNextSibling();
        }
        if (node != null)
        { // Set or remove text
            if (text != null)
                node.setNodeValue(text.toString());
            else
                elem.removeChild(node);
        } 
        else if (text != null)
        { // Add Text
            elem.appendChild(elem.getOwnerDocument().createTextNode(text.toString()));
        }
        return true;
    }

    /**
     * Adds a new child element to a parent.
     * 
     * @param parent the Element to which to append the child
     * @param name the (tag) name of the new child
     * @param value the text value of the new element. (can be null!)
     * @return the new child element
     */
    static public Element addElement(Node parent, String name, String value)
    {
        if (parent == null)
            return null; // Fehler
        // Name must not contain spaces
        if (name.indexOf(' ')>=0)
            name = name.replace(' ', '_');
        // Create Element
        Element child = parent.getOwnerDocument().createElement(name);
        if (value != null)
            setElementText(child, value);
        parent.appendChild(child);
        return child;
    }

    /**
     * Adds a child element to the parent.
     * 
     * @param parent
     * @param name
     * @return the newly created child element
     */
    static public Element addElement(Element parent, String name)
    {
        return addElement(parent, name, null);
    }

    /**
     * Adds a new child element to a parent with a namespace.
     * 
     * @param parent the Element to which to append the child
     * @param prefix the name of the namespace this element belongs to
     * @param name the (tag) name of the new child
     * @param value the text value of the new element. (can be null!)
     * @return the new child element
     */
    static public Element addElementNS(Node parent, String prefix, String name, String value)
    {
        if (parent == null)
            return null; // Fehler
        // URI
        if (prefix == null || prefix.length() == 0)
            return addElement(parent, name, value);
        // Find matching URI
        Document doc = parent.getOwnerDocument();
        String uri = getNamespaceURI(doc, prefix);
        if (uri == null)
        {
            log.error("Namespace URI for namespace " + prefix + " not found!");
            return null;
        }
        Element child = doc.createElementNS(uri, prefix + ":" + name);
        if (value != null)
            setElementText(child, value);
        parent.appendChild(child);
        return child;
    }

    static public Element addElementNS(Element parent, String prefix, String name)
    {
        return addElementNS(parent, prefix, name, null);
    }

    /**
     * Inserts a new child element to a parent.
     * 
     * @param parent the Element to which to append the child
     * @param name the (tag) name of the new child
     * @param value the text value of the new element. (can be null!)
     * @param pos the inserted element will be placed before this element
     * 
     * @return the new child element
     */
    static public Element insertElement(Node parent, String name, String value, Element pos)
    {
        if (parent == null)
            return null; // Fehler
        Element child = parent.getOwnerDocument().createElement(name);
        if (value != null)
            setElementText(child, value);
        // insert now
        parent.insertBefore(child, pos);
        return child;
    }

    static public Element insertElement(Node parent, String name, Element pos)
    {
        return insertElement(parent, name, null, pos);
    }

    /**
     * Inserts a new child element to a parent.
     * 
     * @param parent the Element to which to append the child
     * @param prefix 
     * @param name the (tag) name of the new child
     * @param value the text value of the new element. (can be null!)
     * @param pos pos the inserted element will be placed before this element
     * 
     * @return the new child element
     */
    static public Element insertElementNS(Node parent, String prefix, String name, String value, Element pos)
    {
        if (parent == null)
            return null; // Fehler
        // Has prefix?
        if (prefix == null || prefix.length() == 0)
            return insertElement(parent, name, value, pos);
        // Find matching URI
        Document doc = parent.getOwnerDocument();
        String uri = getNamespaceURI(doc, prefix);
        if (uri == null)
        {
            log.error("Namespace URI for namespace " + prefix + " not found!");
            return null;
        }
        Element child = doc.createElementNS(uri, prefix + ":" + name);
        if (value != null)
            setElementText(child, value);
        // insert now
        parent.insertBefore(child, pos);
        return child;
    }

    static public Element insertElementNS(Node parent, String prefix, String name, Element pos)
    {
        return insertElementNS(parent, prefix, name, null, pos);
    }

    /**
     * Returns the text value of a given child element.
     * 
     * @param parent the Element which contains the child
     * @param childName the (tag) name of the child
     * @return the text value of the child or null if no child exists or the child does not have a text value
     */
    static public String getChildText(Node parent, String childName)
    {
        Element elem = findFirstChild(parent, childName);
        if (elem == null)
            return null; // not Found!
        return getElementText(elem);
    }

    /**
     * Changes the tag name of an element.
     * 
     * @param elem Element which name should be changed
     * @param newName new tag name of the element
     * @return true if the name was changed successfully or false otherwise
     */
    static public boolean changeTagName(Element elem, String newName)
    {
        if (elem == null)
            return false; // not Found!
        Document doc = elem.getOwnerDocument();
        Element newElem = doc.createElement(newName);

        // Copy the attributes to the new element
        NamedNodeMap attrs = elem.getAttributes();
        for (int i = 0; i < attrs.getLength(); i++)
        {
            Attr attr2 = (Attr) doc.importNode(attrs.item(i), true);
            newElem.getAttributes().setNamedItem(attr2);
        }

        // Copy all Child Elements
        for (Node node = elem.getFirstChild(); node != null; node = node.getNextSibling())
            newElem.appendChild(node.cloneNode(true));
        // insert
        Node parent = elem.getParentNode();
        parent.replaceChild(newElem, elem);
        return true;
    }

}