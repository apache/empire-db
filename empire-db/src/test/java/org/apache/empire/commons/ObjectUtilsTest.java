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

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;


import org.junit.Ignore;
import org.junit.Test;

/**
 * @author francisdb
 * 
 */
public class ObjectUtilsTest
{

	/**
	 * Test method for
	 * {@link org.apache.empire.commons.ObjectUtils#isEmpty(java.lang.Object)}.
	 */
	@Test
	public void testIsEmpty()
	{
		assertTrue(ObjectUtils.isEmpty(""));
		assertTrue(ObjectUtils.isEmpty(null));
		assertFalse(ObjectUtils.isEmpty(" "));
		assertFalse(ObjectUtils.isEmpty(new Object()));
	}

	@Test
    public void testIsZero()
    {
        assertTrue(ObjectUtils.isZero(BigDecimal.ZERO));
        assertTrue(ObjectUtils.isZero(0f));
        assertTrue(ObjectUtils.isZero(Float.valueOf("0")));
        assertTrue(ObjectUtils.isZero(0d));
        assertTrue(ObjectUtils.isZero(0l));
        assertTrue(ObjectUtils.isZero(0));
		assertTrue(ObjectUtils.isZero(null));
		assertFalse(ObjectUtils.isZero(0.1d));
        assertFalse(ObjectUtils.isZero(444l));
        assertFalse(ObjectUtils.isZero(-0.01f));
    }

	/**
	 * Test method for
	 * {@link org.apache.empire.commons.ObjectUtils#compareEqual(java.lang.Object, java.lang.Object)}
	 * .
	 */
	@Test
	public void testCompareEqual()
	{
		assertTrue(ObjectUtils.compareEqual((Object)null, null));
		
		Object object = new Object();
		assertTrue(ObjectUtils.compareEqual(object, object));
		assertTrue(ObjectUtils.compareEqual("", ""));
		assertTrue(ObjectUtils.compareEqual("abc", String.valueOf("abc")));
		assertTrue(ObjectUtils.compareEqual("abc", new String("abc")));

		assertTrue(ObjectUtils.compareEqual("", null));
		assertTrue(ObjectUtils.compareEqual(null, ""));
		assertFalse(ObjectUtils.compareEqual("", " "));
		
		assertTrue(ObjectUtils.compareEqual(Long.valueOf(100), Integer.valueOf(100)));
		assertTrue(ObjectUtils.compareEqual(Float.valueOf(100), Integer.valueOf(100)));
		assertTrue(ObjectUtils.compareEqual(Float.valueOf(100.0123f), Double.valueOf(100.0123f)));

		assertFalse(ObjectUtils.compareEqual(Float.valueOf(100.0123f), Long.valueOf(100)));
		
		Date date = new Date();
		Date dateEq = new Date(date.getTime());
		Date dateDiff = new Date(123);
		assertTrue(ObjectUtils.compareEqual(date, dateEq));
		
		assertFalse(ObjectUtils.compareEqual(date, dateDiff));
		
		// now test toString() equals
		Object o1 = new Object(){
			@Override
			public String toString()
			{
				return "JUnit";
			}
		};
		Object oEq = new Object(){
			@Override
			public String toString()
			{
				return "JUnit";
			}
		};
		Object oDiff = new Object(){
			@Override
			public String toString()
			{
				return "JUnitDiff";
			}
		};
		
		assertTrue(ObjectUtils.compareEqual(o1, oEq));
		assertFalse(ObjectUtils.compareEqual(o1, oDiff));
	}

	/**
	 * Test method for
	 * {@link org.apache.empire.commons.ObjectUtils#coalesce(java.lang.Object, java.lang.Object)}
	 * .
	 */
	@Test
	public void testCoalesce()
	{
		assertNull(ObjectUtils.coalesce(null, null));
		assertEquals("preferred", ObjectUtils.coalesce("preferred", null));
		assertEquals("preferred", ObjectUtils.coalesce("preferred", "alternative"));
		assertEquals("alternative", ObjectUtils.coalesce(null, "alternative"));
	}

	/**
	 * Test method for
	 * {@link org.apache.empire.commons.ObjectUtils#getInteger(java.lang.Object, int)}
	 * .
	 */
	@Test
	public void testGetIntegerObjectInt()
	{
		assertEquals(Integer.MAX_VALUE, ObjectUtils.getInteger(null, Integer.MAX_VALUE));
		assertEquals(Integer.MIN_VALUE, ObjectUtils.getInteger("", Integer.MIN_VALUE));
		assertEquals(123, ObjectUtils.getInteger("JUnit", 123));
		
		assertEquals(456, ObjectUtils.getInteger(456, 123));
		assertEquals(456, ObjectUtils.getInteger("456", 123));
		assertEquals(456, ObjectUtils.getInteger(Long.valueOf(456), 123));
		assertEquals(456, ObjectUtils.getInteger(Integer.valueOf(456), 123));
		assertEquals(456, ObjectUtils.getInteger(Float.valueOf(456), 123));
		assertEquals(456, ObjectUtils.getInteger(Double.valueOf(456), 123));
		
		// TODO check if this is the wanted result
		assertEquals(456, ObjectUtils.getInteger(Double.valueOf(456.456), 123));
	}

	/**
	 * Test method for
	 * {@link org.apache.empire.commons.ObjectUtils#getInteger(java.lang.Object)}
	 * .
	 */
	@Test
	public void testGetIntegerObject()
	{
		assertEquals(0, ObjectUtils.getInteger(null));
		assertEquals(0, ObjectUtils.getInteger(""));
		assertEquals(0, ObjectUtils.getInteger("JUnit"));
		
		assertEquals(456, ObjectUtils.getInteger("456"));
		assertEquals(456, ObjectUtils.getInteger(Long.valueOf(456)));
		assertEquals(456, ObjectUtils.getInteger(Integer.valueOf(456)));
		assertEquals(456, ObjectUtils.getInteger(Float.valueOf(456)));
		assertEquals(456, ObjectUtils.getInteger(Double.valueOf(456)));
		
		// TODO check if this is the wanted result
		assertEquals(456, ObjectUtils.getInteger(Double.valueOf(456.456)));
	}

	/**
	 * Test method for
	 * {@link org.apache.empire.commons.ObjectUtils#getLong(java.lang.Object, long)}
	 * .
	 */
	@Test
	public void testGetLongObjectLong()
	{
		assertEquals(Long.MAX_VALUE, ObjectUtils.getLong(null, Long.MAX_VALUE));
		assertEquals(Long.MIN_VALUE, ObjectUtils.getLong("", Long.MIN_VALUE));
		assertEquals(123L, ObjectUtils.getLong("JUnit", 123L));
		
		assertEquals(456L, ObjectUtils.getLong(456L, Long.MAX_VALUE));
		assertEquals(456L, ObjectUtils.getLong("456", Long.MAX_VALUE));
		assertEquals(456L, ObjectUtils.getLong(Long.valueOf(456L), Long.MAX_VALUE));
		assertEquals(456L, ObjectUtils.getLong(Integer.valueOf(456), Long.MAX_VALUE));
		assertEquals(456L, ObjectUtils.getLong(Float.valueOf(456), Long.MAX_VALUE));
		assertEquals(456L, ObjectUtils.getLong(Double.valueOf(456), Long.MAX_VALUE));
		
		// TODO check if this is the wanted result
		assertEquals(456L, ObjectUtils.getLong(Double.valueOf(456.456), Long.MAX_VALUE));
	}

	/**
	 * Test method for
	 * {@link org.apache.empire.commons.ObjectUtils#getLong(java.lang.Object)}.
	 */
	@Test
	public void testGetLongObject()
	{
		assertEquals(0, ObjectUtils.getLong(null));
		assertEquals(0, ObjectUtils.getLong(""));
		assertEquals(0, ObjectUtils.getLong("JUnit"));
		
		assertEquals(456L, ObjectUtils.getLong("456"));
		assertEquals(456L, ObjectUtils.getLong(Long.valueOf(456)));
		assertEquals(456L, ObjectUtils.getLong(Integer.valueOf(456)));
		assertEquals(456L, ObjectUtils.getLong(Float.valueOf(456)));
		assertEquals(456L, ObjectUtils.getLong(Double.valueOf(456)));
		
		// TODO check if this is the wanted result
		assertEquals(456L, ObjectUtils.getLong(Double.valueOf(456.456)));
	}

	/**
	 * Test method for
	 * {@link org.apache.empire.commons.ObjectUtils#getDouble(java.lang.Object, double)}
	 * .
	 */
	@Test
	public void testGetDoubleObjectDouble()
	{
		assertEquals(Double.MAX_VALUE, ObjectUtils.getDouble(null, Double.MAX_VALUE), 0);
		assertEquals(Double.MIN_VALUE, ObjectUtils.getDouble("", Double.MIN_VALUE), 0);
		assertEquals(123d, ObjectUtils.getDouble("JUnit", 123L), 0);
		
		assertEquals(456d, ObjectUtils.getDouble(456L, Double.MAX_VALUE), 0);
		assertEquals(456d, ObjectUtils.getDouble("456", Double.MAX_VALUE), 0);
		assertEquals(456d, ObjectUtils.getDouble(Long.valueOf(456L), Double.MAX_VALUE), 0);
		assertEquals(456d, ObjectUtils.getDouble(Integer.valueOf(456), Double.MAX_VALUE), 0);
		assertEquals(456d, ObjectUtils.getDouble(Float.valueOf(456), Double.MAX_VALUE),0.001);
		assertEquals(456d, ObjectUtils.getDouble(Double.valueOf(456), Double.MAX_VALUE),0.001);
	}

	/**
	 * Test method for
	 * {@link org.apache.empire.commons.ObjectUtils#getDouble(java.lang.Object)}
	 * .
	 */
	@Test
	public void testGetDoubleObject()
	{
		assertEquals(0, ObjectUtils.getDouble(null), 0);
		assertEquals(0, ObjectUtils.getDouble(""), 0);
		assertEquals(0, ObjectUtils.getDouble("JUnit"), 0);
		
		assertEquals(456.123d, ObjectUtils.getDouble("456.123"),0);
		assertEquals(456d, ObjectUtils.getDouble(Long.valueOf(456)),0);
		assertEquals(456d, ObjectUtils.getDouble(Integer.valueOf(456)),0);
		assertEquals(456.123d, ObjectUtils.getDouble(Float.valueOf(456.123f)),0.001);
		assertEquals(456.123d, ObjectUtils.getDouble(Double.valueOf(456.123)),0.001);
		
	}

	/**
	 * Test method for
	 * {@link org.apache.empire.commons.ObjectUtils#getBoolean(java.lang.Object)}
	 * .
	 */
	@Test
	public void testGetBoolean()
	{
		assertTrue(ObjectUtils.getBoolean(true));
		assertTrue(ObjectUtils.getBoolean(Boolean.TRUE));
		assertTrue(ObjectUtils.getBoolean(new Boolean("true")));
		assertTrue(ObjectUtils.getBoolean(Boolean.valueOf(true)));
		assertTrue(ObjectUtils.getBoolean(1));
		assertTrue(ObjectUtils.getBoolean(-123));
		assertTrue(ObjectUtils.getBoolean("Y"));
		assertTrue(ObjectUtils.getBoolean("y"));
		assertTrue(ObjectUtils.getBoolean("TrUe"));
		assertTrue(ObjectUtils.getBoolean("true"));
		assertTrue(ObjectUtils.getBoolean("TRUE"));
		
		assertFalse(ObjectUtils.getBoolean(null));
		assertFalse(ObjectUtils.getBoolean(false));
		assertFalse(ObjectUtils.getBoolean(Boolean.FALSE));
		assertFalse(ObjectUtils.getBoolean(new Boolean("false")));
		assertFalse(ObjectUtils.getBoolean(Boolean.valueOf(false)));
		assertFalse(ObjectUtils.getBoolean(0));
		assertFalse(ObjectUtils.getBoolean(Float.valueOf(0)));
		assertFalse(ObjectUtils.getBoolean(new Object()));
		assertFalse(ObjectUtils.getBoolean("JUnit"));
		assertFalse(ObjectUtils.getBoolean("false"));
	}

	/**
	 * Test method for
	 * {@link org.apache.empire.commons.ObjectUtils#getDate(java.lang.Object, java.util.Locale)}
	 * .
	 */
	@Test
	@Ignore("fix multithreaded stuff")
	public void testGetDateObjectLocale()
	{
		fail("Not yet implemented");
	}

	/**
	 * Test method for
	 * {@link org.apache.empire.commons.ObjectUtils#getDate(java.lang.Object)}.
	 */
	@Test
	@Ignore("fix multithreaded stuff")
	public void testGetDateObject()
	{
		fail("Not yet implemented");
	}

	/**
	 * Test method for
	 * {@link org.apache.empire.commons.ObjectUtils#formatDate(java.util.Date, boolean)}
	 * .
	 */
	@Test
	@Ignore("fix multithreaded stuff")
	public void testFormatDate()
	{
		fail("Not yet implemented");
	}

	/**
	 * Test method for
	 * {@link org.apache.empire.commons.ObjectUtils#convert(java.lang.Class, java.lang.Object)}
	 * .
	 */
	@Test
	public void testConvertClassOfTObject()
	{
		Object object = new Object();
		assertNull(ObjectUtils.convert(Void.class, (Object)null));
		assertEquals("JUnit", ObjectUtils.convert(String.class, "JUnit"));
		assertEquals(Integer.valueOf(123), ObjectUtils.convert(Integer.class, "123"));
		assertEquals(Long.valueOf(123), ObjectUtils.convert(Long.class, "123"));
		assertEquals(object.toString(), ObjectUtils.convert(String.class, object));
		assertEquals(Boolean.TRUE, ObjectUtils.convert(Boolean.class, 123));
		assertEquals(Long.valueOf(Long.MAX_VALUE), ObjectUtils.convert(Long.class, Long.MAX_VALUE));
		assertEquals(Float.valueOf(123.123f), ObjectUtils.convert(Float.class, 123.123f));
		assertEquals(Integer.valueOf(123), ObjectUtils.convert(Integer.class, 123.123));
		assertEquals("JUnit", ObjectUtils.convert(Object.class, "JUnit"));
	}

	/**
	 * Test method for
	 * {@link org.apache.empire.commons.ObjectUtils#isAssignmentCompatible(java.lang.Class, java.lang.Class)}
	 * .
	 */
	@Test
	public void testIsAssignmentCompatible()
	{
		// TODO handle null?
		assertTrue(ObjectUtils.isAssignmentCompatible(Object.class, String.class));
		assertTrue(ObjectUtils.isAssignmentCompatible(String.class, String.class));
		assertTrue(ObjectUtils.isAssignmentCompatible(Long.class, long.class));
		assertTrue(ObjectUtils.isAssignmentCompatible(long.class, Long.class));
		assertTrue(ObjectUtils.isAssignmentCompatible(long.class, Integer.class));
		assertTrue(ObjectUtils.isAssignmentCompatible(float.class, double.class));
		assertTrue(ObjectUtils.isAssignmentCompatible(DateFormat.class, SimpleDateFormat.class));
		
		assertFalse(ObjectUtils.isAssignmentCompatible(SimpleDateFormat.class, DateFormat.class));
		assertFalse(ObjectUtils.isAssignmentCompatible(String.class, Object.class));
	}

	/**
	 * Test method for
	 * {@link org.apache.empire.commons.ObjectUtils#convert(java.lang.Class, java.util.Collection)}
	 * .
	 */
	@Test
	public void testConvertClassOfTCollectionOfQextendsT()
	{
		List<String> objectList = new ArrayList<String>();
		objectList.add("JUnit");
		objectList.add("JUnit2");
		Collection<Object> results = ObjectUtils.convert(Object.class, objectList);
		assertEquals(2, objectList.size());
		Iterator<Object> it = results.iterator();
		assertEquals("JUnit", it.next());
		assertEquals("JUnit2", it.next());
		assertFalse(it.hasNext());
	}

	/**
	 * Test method for
	 * {@link org.apache.empire.commons.ObjectUtils#toStringArray(java.lang.Object[], java.lang.String)}
	 * .
	 */
	@Test
	public void testToStringArray()
	{
		assertNull(ObjectUtils.toStringArray(null, null));
		assertNull(ObjectUtils.toStringArray(null, "JUnit"));
		
		Object object = new Object();
		Object[] test = new Object[]{null, "", object, Integer.valueOf(123456)};
		String[] expected = new String[]{"DEFAULT", "", object.toString(), "123456"};
		assertArrayEquals(expected, ObjectUtils.toStringArray(test, "DEFAULT"));
	}

	/**
	 * Test method for
	 * {@link org.apache.empire.commons.ObjectUtils#contains(Object[], Object)}
	 * .
	 */
	@Test
	public void testContains()
	{
		Float toFind = new Float(456f);
		Float other = Float.valueOf(123f);
		Float[] empty = new Float[]{};
		Float[] one = new Float[]{toFind};
		Float[] both = new Float[]{toFind, other};
		assertTrue(ObjectUtils.contains(both, toFind));
		assertTrue(ObjectUtils.contains(both, other));
		assertTrue(ObjectUtils.contains(both, new Float(123f)));
		assertTrue(ObjectUtils.contains(both, new Float(456f)));
		assertTrue(ObjectUtils.contains(one, toFind));
		assertFalse(ObjectUtils.contains(one, other));
		assertFalse(ObjectUtils.contains(empty, toFind));
		assertFalse(ObjectUtils.contains(empty, other));
		
		assertFalse(ObjectUtils.contains(null, null));
		assertFalse(ObjectUtils.contains(null, other));
	}


}
