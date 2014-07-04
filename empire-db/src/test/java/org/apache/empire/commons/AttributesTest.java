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

import java.util.Random;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * @author francisdb
 *
 */
public class AttributesTest
{

	/**
	 * Test method for {@link org.apache.empire.commons.Attributes#get(java.lang.String)}.
	 */
	@Test
	public void testGetString()
	{
		Attributes attributes = new Attributes();
		Object val = attributes.get("unexisting");
		assertEquals(null, val);
		attributes.set("test", Integer.valueOf(123));
		val = attributes.get("test");
		assertEquals(Integer.valueOf(123), val);
	}

	/**
	 * Test method for {@link org.apache.empire.commons.Attributes#set(java.lang.String, java.lang.Object)}.
	 */
	@Test
	public void testSet()
	{
		Attributes attributes = new Attributes();
		attributes.set(null, null);
		assertEquals(null, attributes.get(null));
		attributes.set("", null);
		assertEquals(null, attributes.get(""));
		attributes.set("test", null);
		assertEquals(null, attributes.get("test"));
		attributes.set("test", Integer.valueOf(456));
		assertEquals(Integer.valueOf(456), attributes.get("test"));
	}

	/**
	 * Test method for {@link org.apache.empire.commons.Attributes#addXml(org.w3c.dom.Element, long)}.
	 * @throws ParserConfigurationException 
	 */
	@Test
	public void testAddXml() throws ParserConfigurationException
	{
		Attributes attributes = new Attributes();
		attributes.set("test", "testvalue");
		attributes.set("test2", "testvalue2");

		DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Document doc = builder.newDocument();
		Element root = doc.createElement("root");
        doc.appendChild(root);
        
		// value should be ignored
		Random random = new Random();
		attributes.addXml(root, random.nextInt());
		root.getAttribute("test").equals("testvalue");
		root.getAttribute("test2").equals("testvalue2");
	}

}
