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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public class StringUtilsTest
{

	@Test
	public void testToStringObjectString()
	{
		assertEquals(null,StringUtils.toString((Object)null, null));
		assertEquals("default",StringUtils.toString(null, "default"));
		assertEquals("123",StringUtils.toString(Long.valueOf("123"), "456"));
	}

	@Test
	public void testToStringObject()
	{
		assertEquals(null,StringUtils.toString((Object)null));
		assertEquals("test",StringUtils.toString("test"));
		assertEquals(Boolean.FALSE.toString(),StringUtils.toString(Boolean.FALSE));
	}

	@Test
	public void testToStringObjectArrayString()
	{
		assertEquals(null,StringUtils.toString((Object[])null, null));
		assertEquals("default",StringUtils.toString((Object[])null, "default"));
		assertEquals("default",StringUtils.toString(new Number[]{}, "default"));
		assertEquals("123/12.3",StringUtils.toString(new Number[]{Integer.valueOf("123"), Double.valueOf("12.3")}, "default"));
	}

	@Test
	public void testToStringObjectArray()
	{
		assertEquals(null,StringUtils.toString((Object[])null));
		assertEquals(null,StringUtils.toString(new Number[]{}));
		assertEquals("123/12.3",StringUtils.toString(new Number[]{Integer.valueOf("123"), Double.valueOf("12.3")}));
	}

	@Test
	public void testValueOfObject()
	{
		assertEquals("",StringUtils.valueOf(null));
		assertEquals("",StringUtils.valueOf(""));
		assertEquals("123",StringUtils.valueOf(Long.valueOf("123")));
	}

	@Test
	public void testValueOfObjectArray()
	{
		assertEquals("",StringUtils.valueOf(null));
		assertEquals("",StringUtils.valueOf(new Object[]{}));
		assertEquals("123/12.3",StringUtils.valueOf(new Number[]{Integer.valueOf("123"), Double.valueOf("12.3")}));
	}

	@Test
	public void testCoalesce()
	{
		assertEquals("ok",StringUtils.coalesce("ok", "fail"));
		assertEquals("ok",StringUtils.coalesce("   \r \n \t  ", "ok"));
		assertEquals("ok",StringUtils.coalesce(null, "ok"));
		assertEquals(null,StringUtils.coalesce(null, null));
	}

	@Test
	public void testNullIfEmpty()
	{
		assertEquals(null, StringUtils.nullIfEmpty(null));
		assertEquals(null, StringUtils.nullIfEmpty(""));
		assertEquals(" ", StringUtils.nullIfEmpty(" "));
		assertEquals("\r\n\t", StringUtils.nullIfEmpty("\r\n\t"));
		assertEquals(" value ", StringUtils.nullIfEmpty(" value "));
	}

	@Test
	public void testArrayToString()
	{
		assertEquals(null, StringUtils.arrayToString(null , null));
		assertEquals(null, StringUtils.arrayToString(null , "/"));
		assertEquals(null, StringUtils.arrayToString(new String[]{} , ""));
		assertEquals("12312.3", StringUtils.arrayToString(new Number[]{Integer.valueOf("123"), Double.valueOf("12.3")} , ""));
		assertEquals("firstnullsecondnullthird", StringUtils.arrayToString(new String[]{"first", "second", "third"} , null));
		assertEquals(" first \t second \t third ", StringUtils.arrayToString(new String[]{" first ", " second ", " third "} , "\t"));
		assertEquals("null/null", StringUtils.arrayToString(new String[]{null, null} , "/"));
		// FIXME see what this should return + implement (throws nullpointer now)
		// assertEquals("null", StringUtils.arrayToString(new String[]{null} , "/"));
	}

	@Test
	public void testStringToArray()
	{
		// TODO see if we want to always return empty arrays instead of nulls
		assertArrayEquals(null, StringUtils.stringToArray(null , null));
		assertArrayEquals(null, StringUtils.stringToArray("first null second" , null));
		assertArrayEquals(null, StringUtils.stringToArray(null , "/"));
		assertArrayEquals(new String[]{"test"}, StringUtils.stringToArray("test" , "/"));
		assertArrayEquals(new String[]{"test "," test2"}, StringUtils.stringToArray("test and test2" , "and"));
		// FIXME this is returning strange results !!!
		//assertArrayEquals(new String[]{"","","",""}, StringUtils.stringToArray("///" , "/"));
	}

	@Test
	public void testCollectionToString()
	{
		assertEquals(null, StringUtils.collectionToString(null , null));
		assertEquals(null, StringUtils.collectionToString(null , "/"));
		assertEquals(null, StringUtils.collectionToString(Collections.emptySet() , null));
		List<String> test = new ArrayList<String>();
		Collections.addAll(test, "first","second","third");
		assertEquals("firstsecondthird", StringUtils.collectionToString(test , ""));
		assertEquals("firstnullsecondnullthird", StringUtils.collectionToString(test , null));
		assertEquals("first \t second \t third", StringUtils.collectionToString(test , " \t "));
		test.clear();
		Collections.addAll(test, "first", null, "third");
		// TODO should null be converted to string null as with the arry method?
		assertEquals("first//third", StringUtils.collectionToString(test , "/"));
		test.clear();
		Collections.addAll(null);
		// FIXME see what this should return + implement (throws nullpointer now)
		// assertEquals("null", StringUtils.arrayToString(new String[]{null} , "/"));
	}

	@Test
	public void testIsEmpty()
	{
		assertTrue(StringUtils.isEmpty(null));
		assertTrue(StringUtils.isEmpty(""));
		assertTrue(StringUtils.isEmpty("\t\r\n"));
		assertFalse(StringUtils.isEmpty(" test "));
	}

	@Test
	public void testIsValid()
	{
		assertFalse(StringUtils.isValid(null));
		assertFalse(StringUtils.isValid(""));
		assertFalse(StringUtils.isValid("\t\r\n"));
		assertTrue(StringUtils.isValid(" test "));
	}

	@Test
	public void testValidate()
	{
		assertEquals(null, StringUtils.validate(null));
		assertEquals(null, StringUtils.validate(""));
		assertEquals(null, StringUtils.validate(" \r\n\t "));
		assertEquals("azerty\r\n\tazerty", StringUtils.validate(" \r azerty\r\n\tazerty\t "));
	}

	@Test
	public void testReplace()
	{
		assertEquals(null, StringUtils.replace(null, null, null));
		assertEquals("", StringUtils.replace("", null, null));
		assertEquals("test null test", StringUtils.replace("test null test", null, ""));
		assertEquals("test  test", StringUtils.replace("test a test", "a", null));
		assertEquals("test test", StringUtils.replace("test test", "", "oops"));
		assertEquals("test test", StringUtils.replaceAll("test test", null, "oops"));
		assertEquals("testoopsoopstest", StringUtils.replace("test  test", " ", "oops"));
		assertEquals("1-two-3", StringUtils.replace("1 2 3", " 2 ", "-two-"));
	}

	@Test
	public void testReplaceAll()
	{
		// TODO what is the difference with the other replace method, merge???
		assertEquals(null, StringUtils.replaceAll(null, null, null));
		assertEquals("", StringUtils.replaceAll("", null, null));
		assertEquals("test null test", StringUtils.replaceAll("test null test", null, ""));
		assertEquals("test  test", StringUtils.replaceAll("test a test", "a", null));
		assertEquals("test test", StringUtils.replaceAll("test test", "", "oops"));
		assertEquals("test test", StringUtils.replaceAll("test test", null, "oops"));
		assertEquals("testoopsoopstest", StringUtils.replaceAll("test  test", " ", "oops"));
		assertEquals("1-two-3", StringUtils.replaceAll("1 2 3", " 2 ", "-two-"));
	}

}
