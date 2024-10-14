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
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

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
		assertEquals(null,StringUtils.toString((Object[])null));
        assertEquals(null,StringUtils.toString(new Number[]{}));
        assertEquals("",StringUtils.toString(new Number[]{null}));
        assertEquals("null",StringUtils.toString(new Number[]{null}, StringUtils.NULL));
		assertEquals("default",StringUtils.toString((Object[])null, "default"));
		assertEquals("default",StringUtils.toString(new Number[]{}, "default"));
        assertEquals("default",StringUtils.toString(new Number[]{null}, "default"));
        assertEquals("123",StringUtils.toString(new Number[]{Integer.valueOf("123")}, "default"));
		assertEquals("123|12.3",StringUtils.toString(new Number[]{Integer.valueOf("123"), Double.valueOf("12.3")}, "default"));
	}

    @Test
    public void testAsStringObjectArrayString()
    {
        assertEquals("",StringUtils.asString((Object[])null));
        assertEquals("",StringUtils.asString(new Number[]{}));
        assertEquals("[]",StringUtils.asString(new Number[]{null}));
        assertEquals("[123]",StringUtils.asString(new Number[]{Integer.valueOf("123")}));
        assertEquals("[123|12.3]",StringUtils.asString(new Number[]{Integer.valueOf("123"), Double.valueOf("12.3")}));
        assertEquals("[123|[aaa|[bbb|xxx]|yyy]|12.3]", StringUtils.asString(new Object[]{Integer.valueOf("123"), new Object[]{ "aaa", new Object[]{ "bbb", "xxx" }, "yyy" }, Double.valueOf("12.3")}));
    }

    @Test
    public void testToStringCollections()
    {
        ArrayList<String> array = new ArrayList<String>();
        assertEquals(null, StringUtils.toString(array, null));
        array.add(null);
        assertEquals("", StringUtils.toString(array, null));
        assertEquals("null", StringUtils.toString(array, StringUtils.NULL));
        assertEquals("{null=empty}|{1=one}|{2=two}", StringUtils.toString(new Options().add(null, "empty").add("1", "one").add("2", "two")));
        array.add("end");
        assertEquals("default|end",StringUtils.toString(array, "default"));
        array.clear();
        array.add("one");
        assertEquals("one",StringUtils.toString(array, "default"));
        array.add(null);
        array.add("end");
        assertEquals("one|default|end",StringUtils.toString(array, "default"));
        assertEquals("one||end",StringUtils.listToString(array, "|", StringUtils.EMPTY));
        // Special case with SPACE
        assertEquals("one end",StringUtils.listToString(array, StringUtils.SPACE, StringUtils.EMPTY));
        array.clear();
        array.add("   ");
        assertEquals("null",StringUtils.listToString(array, StringUtils.SPACE, StringUtils.NULL));
    }

	@Test
	public void testToStringObjectArray()
	{
		assertEquals(null,StringUtils.toString((Object[])null));
		assertEquals(null,StringUtils.toString(new Number[]{}));
		assertEquals("123|12.3",StringUtils.toString(new Number[]{Integer.valueOf("123"), Double.valueOf("12.3")}));
	}

	@Test
	public void testValueOfObject()
	{
		assertEquals("",StringUtils.valueOf((Object)null));
        assertEquals("one|two",StringUtils.valueOf(new String[] { "one", "two" }));
		assertEquals("",StringUtils.valueOf(""));
		assertEquals("123",StringUtils.valueOf(Long.valueOf("123")));
	}

	@Test
	public void testValueOfObjectArray()
	{
		assertEquals("",StringUtils.valueOf((Object[])null));
        assertEquals("",StringUtils.valueOf(new Object[]{}));
		assertEquals("",StringUtils.valueOf(new Object[]{null}));
		assertEquals("123|12.3",StringUtils.valueOf(new Number[]{Integer.valueOf("123"), Double.valueOf("12.3")}));
	}

	@Test
	public void testCoalesce()
	{
		assertEquals("ok",StringUtils.coalesce("ok", "fail"));
        assertEquals("ok",StringUtils.coalesce(" ", "ok"));
		assertEquals("ok",StringUtils.coalesce("   \r \n \t  ", "ok"));
		assertEquals("ok",StringUtils.coalesce(null, "ok"));
		assertEquals(null,StringUtils.coalesce(null, null));
	}

	@Test
	public void testNullIfEmpty()
	{
		assertEquals(null, StringUtils.nullIfEmpty(null));
		assertEquals(null, StringUtils.nullIfEmpty(""));
		assertEquals(null, StringUtils.nullIfEmpty("   "));
		assertEquals(null, StringUtils.nullIfEmpty("\r\n\t"));
        assertEquals("\r\nOk\t", StringUtils.nullIfEmpty("\r\nOk\t"));
		assertEquals(" value ", StringUtils.nullIfEmpty(" value "));
	}

	@Test
	public void testArrayToString()
	{
		assertEquals(null, StringUtils.arrayToString(null , null));
		assertEquals(null, StringUtils.arrayToString(null , "/"));
		assertEquals(null, StringUtils.arrayToString(new String[]{} , StringUtils.NULL));
        assertEquals("null", StringUtils.arrayToString(new String[]{null}, ",", StringUtils.NULL));
		assertEquals("test", StringUtils.arrayToString(new String[]{"test"} , "|"));
		assertEquals("12312.3", StringUtils.arrayToString(new Number[]{Integer.valueOf("123"), Double.valueOf("12.3")} , ""));
		assertEquals("firstsecondthird", StringUtils.arrayToString(new String[]{"first", "second", "third"} , null));
		assertEquals(" first \t second \t third ", StringUtils.arrayToString(new String[]{" first ", " second ", " third "} , "\t"));
        assertEquals("/", StringUtils.arrayToString(new String[]{null, null} , "/"));
        assertEquals("null/null", StringUtils.arrayToString(new String[]{null, null} , "/", StringUtils.NULL));
		assertEquals("null", StringUtils.arrayToString(new String[]{null} , "/", StringUtils.NULL));
        // Special case with SPACE
        assertEquals("Hello", StringUtils.arrayToString(new Object[]{"Hello","",null," ","  "}, StringUtils.SPACE));
        assertEquals("Hello World", StringUtils.arrayToString(new Object[]{"Hello","",null," ","World"}, StringUtils.SPACE));
        assertEquals("Hello World", StringUtils.toString(new Object[]{"Hello","",null," ","World"}, StringUtils.SPACE, null));
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
		assertFalse(StringUtils.isNotEmpty(null));
		assertFalse(StringUtils.isNotEmpty(""));
		assertFalse(StringUtils.isNotEmpty("\t\r\n"));
		assertTrue (StringUtils.isNotEmpty(" test "));
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

    @Test
    public void testCamelCase()
    {
        assertEquals("helloWorld", StringUtils.toCamelCase("HELLO_WORLD", false));
        assertEquals("helloWorld", StringUtils.toCamelCase(" HELLO WORLD ", false));
        assertEquals("HelloWorld", StringUtils.toCamelCase(" HELLO WORLD ", true));
        assertEquals("123Hello456", StringUtils.toCamelCase("123_hello_456", true));
        assertEquals("", StringUtils.toCamelCase(" _ _ _ ", true));
        assertEquals("ABC", StringUtils.toCamelCase(" _a_ _b_ _c_ ", true));
        assertEquals("aBC", StringUtils.toCamelCase(" _a_ _b_ _c_ ", false));
    }

}
