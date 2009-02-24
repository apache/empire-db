/**
 * 
 */
package org.apache.empire.commons;

import static org.junit.Assert.*;

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
		attributes.put("test", Integer.valueOf(123));
		val = attributes.get("test");
		assertEquals(Integer.valueOf(123), val);
	}

	/**
	 * Test method for {@link org.apache.empire.commons.Attributes#get(java.lang.Object)}.
	 */
	@Test
	public void testGetObject()
	{
		Attributes attributes = new Attributes();
		Object val = attributes.get(new Object());
		assertEquals(null, val);
		attributes.put("123", Integer.valueOf(456));
		val = attributes.get(Integer.valueOf(123));
		assertEquals(Integer.valueOf(456), val);
	}

	/**
	 * Test method for {@link org.apache.empire.commons.Attributes#put(java.lang.String, java.lang.Object)}.
	 */
	@Test
	public void testPutStringObject()
	{
		Attributes attributes = new Attributes();
		attributes.put(null, null);
		assertEquals(null, attributes.get(null));
		attributes.put("", null);
		assertEquals(null, attributes.get(""));
		attributes.put("test", null);
		assertEquals(null, attributes.get("test"));
		attributes.put("test", Integer.valueOf(456));
		assertEquals(Integer.valueOf(456), attributes.get("test"));
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
