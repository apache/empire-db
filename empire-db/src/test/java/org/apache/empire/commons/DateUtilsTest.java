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

import static org.junit.Assert.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author francisdb
 * 
 */
public class DateUtilsTest
{
	
	/**
	 * Test method for {@link org.apache.empire.commons.DateUtils#getDateTimeFromNow(boolean, int, int, int)}.
	 */
	@Test
	public void testGetDateTimeFromNow()
	{
		Calendar now = Calendar.getInstance();
		Date date = DateUtils.getDateTimeFromNow(false, 1, 2, 3);
		Calendar future = Calendar.getInstance();
		future.setTime(date);
		future.add(Calendar.YEAR, -1);
		future.add(Calendar.MONTH, -2);
		future.add(Calendar.DAY_OF_MONTH, -3);
		// this might fail when running exacly over midnight...
		assertEquals(now.get(Calendar.YEAR), future.get(Calendar.YEAR));
		assertEquals(now.get(Calendar.MONTH), future.get(Calendar.MONTH));
		assertEquals(now.get(Calendar.DAY_OF_MONTH), future.get(Calendar.DAY_OF_MONTH));
	}

	/**
	 * Test method for {@link org.apache.empire.commons.DateUtils#getDateNow()}.
	 */
	@Test
	public void testGetDateNow()
	{
		Date now = DateUtils.getDateNow();
		Calendar cal = Calendar.getInstance();
		cal.setTime(now);
		assertEquals(0, cal.get(Calendar.HOUR_OF_DAY));
		assertEquals(0, cal.get(Calendar.MINUTE));
		assertEquals(0, cal.get(Calendar.SECOND));
		assertEquals(0, cal.get(Calendar.MILLISECOND));
	}

	/**
	 * Test method for {@link org.apache.empire.commons.DateUtils#getTimeNow()}.
	 */
	@Test
	public void testGetTimeNow()
	{
		Calendar cal = Calendar.getInstance();
		// TODO can't the implementation of this be simpler?
		Date date = DateUtils.getTimeNow();
		// less than one sec difference
		assertTrue(date.getTime() - cal.getTimeInMillis() < 1000);
	}

	/**
	 * Test method for {@link org.apache.empire.commons.DateUtils#addDate(java.util.Date, int, int, int)}.
	 */
	@Test
	public void testAddDate()
	{
		Calendar now = Calendar.getInstance();
		Date date = now.getTime();
		Date pastDate = DateUtils.addDate(date, -1, -2, -3);
		Calendar past = Calendar.getInstance();
		past.setTime(pastDate);
		past.add(Calendar.YEAR, 1);
		past.add(Calendar.MONTH, 2);
		past.add(Calendar.DAY_OF_MONTH, 3);
		assertEquals(now.get(Calendar.YEAR), past.get(Calendar.YEAR));
		assertEquals(now.get(Calendar.MONTH), past.get(Calendar.MONTH));
		assertEquals(now.get(Calendar.DAY_OF_MONTH), past.get(Calendar.DAY_OF_MONTH));
	}

	/**
	 * Test method for {@link org.apache.empire.commons.DateUtils#setDate(java.util.Date, int, int, int)}.
	 */
	@Test
	public void testSetDate()
	{
		Calendar now = Calendar.getInstance();
		Date date = now.getTime();
		Date pastDate = DateUtils.setDate(date, 1952, 11, 21);
		Calendar past = Calendar.getInstance();
		past.setTime(pastDate);
		assertEquals(1952, past.get(Calendar.YEAR));
		assertEquals(11, past.get(Calendar.MONTH));
		assertEquals(21, past.get(Calendar.DAY_OF_MONTH));
		assertEquals(now.get(Calendar.HOUR_OF_DAY), past.get(Calendar.HOUR_OF_DAY));
		assertEquals(now.get(Calendar.MINUTE), past.get(Calendar.MINUTE));
		assertEquals(now.get(Calendar.SECOND), past.get(Calendar.SECOND));
		assertEquals(now.get(Calendar.MILLISECOND), past.get(Calendar.MILLISECOND));
	}

	/**
	 * Test method for {@link org.apache.empire.commons.DateUtils#getDate(int, int, int)}.
	 */
	@Test
	public void testGetDate()
	{
		Date date = DateUtils.getDate(1, 2, 3);
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		assertEquals(1, cal.get(Calendar.YEAR));
		assertEquals(2, cal.get(Calendar.MONTH));
		assertEquals(3, cal.get(Calendar.DAY_OF_MONTH));
		assertEquals(0, cal.get(Calendar.HOUR_OF_DAY));
		assertEquals(0, cal.get(Calendar.MINUTE));
		assertEquals(0, cal.get(Calendar.SECOND));
		assertEquals(0, cal.get(Calendar.MILLISECOND));
	}

	/**
	 * Test method for {@link org.apache.empire.commons.DateUtils#setTime(java.util.Date, int, int, int, int)}.
	 */
	@Test
	public void testSetTime()
	{
		Calendar now = Calendar.getInstance();
		Date date = now.getTime();
		Date newDate = DateUtils.setTime(date, 1, 2, 3, 4);
		Calendar cal = Calendar.getInstance();
		cal.setTime(newDate);
		assertEquals(now.get(Calendar.YEAR), cal.get(Calendar.YEAR));
		assertEquals(now.get(Calendar.MONTH), cal.get(Calendar.MONTH));
		assertEquals(now.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.DAY_OF_MONTH));
		assertEquals(1, cal.get(Calendar.HOUR_OF_DAY));
		assertEquals(2, cal.get(Calendar.MINUTE));
		assertEquals(3, cal.get(Calendar.SECOND));
		assertEquals(4, cal.get(Calendar.MILLISECOND));
	}

	/**
	 * Test method for {@link org.apache.empire.commons.DateUtils#getDateOnly(java.util.Date)}.
	 */
	@Test
	public void testGetDateOnly()
	{
		Calendar now = Calendar.getInstance();
		Date date = now.getTime();
		Date newDate = DateUtils.getDateOnly(date);
		Calendar cal = Calendar.getInstance();
		cal.setTime(newDate);
		assertEquals(now.get(Calendar.YEAR), cal.get(Calendar.YEAR));
		assertEquals(now.get(Calendar.MONTH), cal.get(Calendar.MONTH));
		assertEquals(now.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.DAY_OF_MONTH));
		assertEquals(0, cal.get(Calendar.HOUR_OF_DAY));
		assertEquals(0, cal.get(Calendar.MINUTE));
		assertEquals(0, cal.get(Calendar.SECOND));
		assertEquals(0, cal.get(Calendar.MILLISECOND));
	}

	/**
	 * Test method for {@link org.apache.empire.commons.DateUtils#parseDate(java.lang.String, java.util.Locale)}.
	 */
	@Test
	public void testParseDate()
	{
		// french has day first
		Date date = DateUtils.parseDate("2/10/2008", Locale.FRANCE);
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		assertEquals(2008, cal.get(Calendar.YEAR));
		assertEquals(9, cal.get(Calendar.MONTH));
		assertEquals(2, cal.get(Calendar.DAY_OF_MONTH));
		assertEquals(0, cal.get(Calendar.HOUR_OF_DAY));
		assertEquals(0, cal.get(Calendar.MINUTE));
		assertEquals(0, cal.get(Calendar.SECOND));
		assertEquals(0, cal.get(Calendar.MILLISECOND));
	}

	/**
	 * Test method for {@link org.apache.empire.commons.DateUtils#formatDate(java.util.Date, java.util.Locale)}.
	 */
	@Test
	public void testFormatDate()
	{
		Calendar cal = simpleCalendar();
		String formatted = DateUtils.formatDate(cal.getTime(), Locale.FRANCE);
		assertEquals("3 mars 0001", formatted);
	}

	/**
	 * Test method for {@link org.apache.empire.commons.DateUtils#formatTime(java.util.Date, java.util.Locale, boolean)}.
	 */
	@Test
	public void testFormatTime()
	{
		Calendar cal = simpleCalendar();
		String formatted = DateUtils.formatTime(cal.getTime(), Locale.FRANCE, true);
		assertEquals("16:05:06", formatted);
		formatted = DateUtils.formatTime(cal.getTime(), Locale.FRANCE, false);
		assertEquals("16:05", formatted);
	}

	/**
	 * Test method for {@link org.apache.empire.commons.DateUtils#formatDayOfWeek(java.util.Date, java.util.Locale, boolean)}.
	 */
	@Test
	public void testFormatDayOfWeek()
	{
		Calendar cal = simpleCalendar();
		String formatted = DateUtils.formatDayOfWeek(cal.getTime(), Locale.FRANCE, true);
		assertEquals("jeudi", formatted);
		formatted = DateUtils.formatDayOfWeek(cal.getTime(), Locale.FRANCE, false);
		assertEquals("jeu.", formatted);
	}

	/**
	 * Test method for {@link org.apache.empire.commons.DateUtils#formatMonth(java.util.Date, java.util.Locale, boolean)}.
	 */
	@Test
	public void testFormatMonthDateLocaleBoolean()
	{
		Calendar cal = simpleCalendar();
		String formatted = DateUtils.formatMonth(cal.getTime(), Locale.ITALY, true);
		assertEquals("marzo", formatted);
		formatted = DateUtils.formatMonth(cal.getTime(), Locale.ITALY, false);
		assertEquals("mar", formatted);
	}

	/**
	 * Test method for {@link org.apache.empire.commons.DateUtils#getWeekOfYear(java.util.Date, java.util.Locale)}.
	 */
	@Test
	public void testGetWeekOfYear()
	{
		Calendar cal = simpleCalendar();
		int weekOfYear = DateUtils.getWeekOfYear(cal.getTime(), Locale.ITALY);
		assertEquals(9, weekOfYear);
		weekOfYear = DateUtils.getWeekOfYear(cal.getTime(), Locale.CHINA);
		assertEquals(10, weekOfYear);
	}

	/**
	 * Test method for {@link org.apache.empire.commons.DateUtils#formatMonth(int, java.util.Locale, boolean)}.
	 */
	@Test
	public void testFormatMonthIntLocaleBoolean()
	{
		// month to big -> return ""
		String formatted = DateUtils.formatMonth(12, Locale.ITALY, true);
		assertEquals("", formatted);
		formatted = DateUtils.formatMonth(8, Locale.ITALY, true);
		assertEquals("settembre", formatted);
		formatted = DateUtils.formatMonth(5, Locale.ITALY, false);
		assertEquals("giu", formatted);
	}

	/**
	 * Test method for {@link org.apache.empire.commons.DateUtils#formatYear(java.util.Date, java.util.Locale)}.
	 */
	@Test
	public void testFormatYear()
	{
		Calendar cal = simpleCalendar();
		String formatted = DateUtils.formatYear(cal.getTime(), Locale.GERMANY);
		assertEquals("0001", formatted);
	}
	
	/**
	 * Returns a test calendar
	 * @return the calendar
	 */
	private Calendar simpleCalendar(){
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.YEAR, 1);
		cal.set(Calendar.MONTH, 2);
		cal.set(Calendar.DAY_OF_MONTH, 3);
		cal.set(Calendar.HOUR, 4);
		cal.set(Calendar.MINUTE, 5);
		cal.set(Calendar.SECOND, 6);
		cal.set(Calendar.MILLISECOND, 7);
		return cal;
	}

}
