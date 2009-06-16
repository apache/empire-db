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
package org.apache.empire.commons;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.empire.commons.Options.InsertPos;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * @author francisdb
 *
 */
public class OptionsTest
{

    /**
     * Test method for {@link org.apache.empire.commons.Options#size()}.
     */
    @Test
    public void testSize()
    {
        Options options = new Options();
        assertEquals(0, options.size());
        options.add(new Object(), "text", true);
        assertEquals(1, options.size());
        options.add(new Object(), "text", true);
        assertEquals(2, options.size());
    }

    /**
     * Test method for {@link org.apache.empire.commons.Options#isEmpty()}.
     */
    @Test
    public void testIsEmpty()
    {
        Options options = new Options();
        assertTrue(options.isEmpty());
        options.add(new Object(), "text", true);
        assertFalse(options.isEmpty());
    }

    /**
     * Test method for {@link org.apache.empire.commons.Options#clear()}.
     */
    @Test
    public void testClear()
    {
        Options options = new Options();
        options.add(new Object(), "text", true);
        options.clear();
        assertTrue(options.isEmpty());
    }

    /**
     * Test method for {@link org.apache.empire.commons.Options#Options()}.
     */
    @Test
    public void testOptions()
    {
        Options options = new Options();
        assertTrue(options.isEmpty());
    }

    /**
     * Test method for {@link org.apache.empire.commons.Options#Options(org.apache.empire.commons.Options)}.
     */
    @Test
    public void testOptionsOptions()
    {
        Options options = new Options();
        options.add(new Object(), "text", true);
        Options options2 = new Options(options);
        assertEquals(1, options2.size());
    }

    /**
     * Test method for {@link org.apache.empire.commons.Options#Options(org.apache.empire.commons.OptionEntry[])}.
     */
    @Test
    public void testOptionsOptionEntryArray()
    {
        Options options = new Options(new OptionEntry[]{});
        assertTrue(options.isEmpty());
        Options options2 = new Options(new OptionEntry[]{new OptionEntry(new Object(),"text")});
        assertEquals(1, options2.size());
    }

    /**
     * Test method for {@link org.apache.empire.commons.Options#getIndex(java.lang.Object)}.
     */
    @Test
    public void testGetIndex()
    {
        Options options = new Options();
        assertEquals(-1, options.getIndex(new Object()));
        options.add(Integer.valueOf(123), "text", true);
        options.add(Integer.valueOf(456), "text", true);
        assertEquals(0, options.getIndex(Integer.valueOf(123)));
        assertEquals(1, options.getIndex(Integer.valueOf(456)));
    }

    /**
     * Test method for {@link org.apache.empire.commons.Options#createOptionEntry(java.lang.Object, java.lang.String)}.
     */
    @Test
    public void testCreateOptionEntry()
    {
        Options options = new Options();
        OptionEntry entry = options.createOptionEntry(Integer.valueOf(123), "test");
        assertEquals("test", entry.getText());
        assertEquals(Integer.valueOf(123), entry.getValue());
    }

    /**
     * Test method for {@link org.apache.empire.commons.Options#getEntry(java.lang.Object)}.
     */
    @Test
    public void testGetEntry()
    {
        Options options = new Options();
        options.add(Integer.valueOf("123"), "text", true);
        OptionEntry entry = options.getEntry(Integer.valueOf("123"));
        assertNotNull(entry);
        assertEquals("text", entry.getText());
        assertEquals(Integer.valueOf(123), entry.getValue());
    }

    /**
     * Test method for {@link org.apache.empire.commons.Options#get(java.lang.Object)}.
     */
    @Test
    public void testGet()
    {
        Options options = new Options();
        options.add(Integer.valueOf("123"), "text", true);
        String text = options.get(Integer.valueOf("123"));
        assertNotNull(text);
        assertEquals("text", text);
    }

    /**
     * Test method for {@link org.apache.empire.commons.Options#getValueAt(int)}.
     */
    @Test
    public void testGetValueAt()
    {
        Options options = new Options();
        assertEquals(null, options.getValueAt(10));
        options.add(Integer.valueOf(123), "text", true);
        options.add(Integer.valueOf(456), "text2", true);
        assertEquals(Integer.valueOf(456), options.getValueAt(1));
    }

    /**
     * Test method for {@link org.apache.empire.commons.Options#getTextAt(int)}.
     */
    @Test
    public void testGetTextAt()
    {
        Options options = new Options();
        assertEquals("", options.getTextAt(10));
        options.add(Integer.valueOf(123), "text", true);
        options.add(Integer.valueOf(456), "text2", true);
        assertEquals("text2", options.getTextAt(1));
    }

    /**
     * Test method for {@link org.apache.empire.commons.Options#getValues()}.
     */
    @Test
    public void testGetValues()
    {
        Options options = new Options();
        assertEquals(0, options.getValues().size());
        options.add(Integer.valueOf(123), "text", true);
        options.add(Integer.valueOf(456), "text2", true);
        Set<Object> values = options.getValues();
        assertTrue(values.contains(Integer.valueOf(123)));
        assertTrue(values.contains(Integer.valueOf(456)));
    }

    /**
     * Test method for {@link org.apache.empire.commons.Options#set(java.lang.Object, java.lang.String, org.apache.empire.commons.Options.InsertPos)}.
     */
    @Test
    public void testSetObjectStringInsertPos()
    {
        Options options = new Options();
        options.add(Integer.valueOf(123), "text", true);
        options.set(Integer.valueOf(456), "text2", InsertPos.Top);
        options.set(Integer.valueOf(789), "text3", InsertPos.Bottom);
        assertEquals(Integer.valueOf(456), options.getValueAt(0));
        assertEquals(Integer.valueOf(789), options.getValueAt(2));
    }

    /**
     * Test method for {@link org.apache.empire.commons.Options#set(java.lang.Object, java.lang.String)}.
     */
    @Test
    public void testSetObjectString()
    {
        Options options = new Options();
        options.add(Integer.valueOf(1), "text", true);
        options.set(Integer.valueOf(2), "text2");
        assertEquals(Integer.valueOf(2), options.getValueAt(1));
    }

    /**
     * Test method for {@link org.apache.empire.commons.Options#add(java.lang.Object, java.lang.String, boolean)}.
     */
    @Test
    public void testAddObjectStringBoolean()
    {
        Options options = new Options();
        options.add(Integer.valueOf(1), "text", true);
        assertEquals(Integer.valueOf(1), options.getValueAt(0));
        options.add(Integer.valueOf(1), "text", true);
        assertEquals(Integer.valueOf(1), options.getValueAt(1));
        
        options = new Options();
        options.add(Integer.valueOf(1), "text", false);
        assertEquals(Integer.valueOf(1), options.getValueAt(0));
        options.add(Integer.valueOf(1), "text2", false);
        assertEquals(null, options.getValueAt(1));
        assertEquals("text2", options.getTextAt(0));
    }

    /**
     * Test method for {@link org.apache.empire.commons.Options#add(org.apache.empire.commons.OptionEntry)}.
     */
    @Test
    public void testAddOptionEntry()
    {
        Options options = new Options();
        options.add(new OptionEntry(Integer.valueOf(1),"test1"));
        options.add(new OptionEntry(Integer.valueOf(1),"test1-2"));
        options.add(new OptionEntry(Integer.valueOf(3),"test3"));
        assertEquals("test1-2", options.getTextAt(0));
        assertEquals("test3", options.getTextAt(1));
    }

    /**
     * Test method for {@link org.apache.empire.commons.Options#contains(java.lang.Object)}.
     */
    @Test
    public void testContainsObject()
    {
        Options options = new Options();
        options.add(Integer.valueOf(1), "txt", false);
        options.add(Integer.valueOf(2), "txt", false);
        assertTrue(options.contains(Integer.valueOf(1)));
        assertTrue(options.contains(Integer.valueOf(2)));
    }

    /**
     * Test method for {@link org.apache.empire.commons.Options#iterator()}.
     */
    @Test
    public void testIterator()
    {
        Options options = new Options();
        options.add(Integer.valueOf(1), "txt", false);
        options.add(Integer.valueOf(2), "txt2", false);
        Iterator<OptionEntry> it = options.iterator();
        it.next();
        assertEquals("txt2", it.next().getText());
        it.remove();
        assertEquals(1, options.size());
    }

    /**
     * Test method for {@link org.apache.empire.commons.Options#remove(java.lang.Object)}.
     */
    @Test
    public void testRemoveObject()
    {
        Options options = new Options();
        options.add(Integer.valueOf(1), "txt", false);
        options.add(Integer.valueOf(2), "txt2", false);
        options.remove(Integer.valueOf(1));
        assertFalse(options.contains(Integer.valueOf(1)));
        assertTrue(options.contains(Integer.valueOf(2)));
    }

    /**
     * Test method for {@link org.apache.empire.commons.Options#toArray()}.
     */
    @Test
    public void testToArray()
    {
        Options options = new Options();
        options.add(Integer.valueOf(1), "txt", false);
        options.add(Integer.valueOf(2), "txt2", false);
        assertEquals(2, options.toArray().length);
    }

    /**
     * Test method for {@link org.apache.empire.commons.Options#addXml(org.w3c.dom.Element, long)}.
     * @throws ParserConfigurationException 
     */
    @Test
    public void testAddXml() throws ParserConfigurationException
    {
        Options options = new Options();
        options.add(Integer.valueOf(1), "txt", false);
        options.add(Integer.valueOf(2), "txt2", false);

        
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = builder.newDocument();
        Element root = doc.createElement("root");
        doc.appendChild(root);

        // TODO get rid of flags param??
        options.addXml(root, 0);
        Node node = root.getFirstChild();
        assertNotNull("no child was added", node);
        assertEquals("option", node.getNodeName());
        assertEquals("1", node.getAttributes().getNamedItem("value").getNodeValue());
        assertEquals("txt", node.getTextContent());
        
        node = node.getNextSibling();
        assertNotNull("no child was added", node);
        assertEquals("option", node.getNodeName());
        assertEquals("2", node.getAttributes().getNamedItem("value").getNodeValue());
        assertEquals("txt2", node.getTextContent());
    }

}
