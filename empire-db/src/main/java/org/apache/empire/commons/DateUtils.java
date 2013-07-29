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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class contains common functions for comparing and converting values of type Date. 
 * 
 */
public class DateUtils
{
    private static final Logger log = LoggerFactory.getLogger(DateUtils.class);
    
    private DateUtils()
    {
        // Static Function only
        // No instance may be created
    }

    /**
     * Returns the current date without time
     * @return the date
     */
    public static Date getDateNow()
    {
    	Calendar calendar = Calendar.getInstance();
	    calendar.set(Calendar.HOUR_OF_DAY, 0);
	    calendar.set(Calendar.MINUTE, 0);
	    calendar.set(Calendar.SECOND, 0);
	    calendar.set(Calendar.MILLISECOND, 0);
	    return calendar.getTime();
    }

    /**
     * Returns the current date and time
     * @return the date
     */
    public static Date getTimeNow()
    {
        return Calendar.getInstance().getTime();
    }

    /**
     * Calculates a date relative to the supplied date.
     * @param date date to calculate from 
     * @param years number of years to add or subtract from the supplied date
     * @param months number of months to add or subtract from the supplied date
     * @param days number of days to add or subtract from the supplied date
     * @return the target date
     */
    public static Date addDate(Date date, int years, int months, int days)
    {
        Calendar calendar = Calendar.getInstance();
        if (date!=null)
            calendar.setTime(date);
        if (years!=0)
            calendar.add(Calendar.YEAR, years);
        if (months!=0)
            calendar.add(Calendar.MONTH, months);
        if (days!=0)
            calendar.add(Calendar.DAY_OF_MONTH, days);
        return calendar.getTime();
    }
    
    public static Date setDate(Date date, int year, int month, int day)
    {
        Calendar calendar = Calendar.getInstance();
        if (date!=null)
            calendar.setTime(date);
        if (year>0)
            calendar.set(Calendar.YEAR, year);
        if (month>=0)
            calendar.set(Calendar.MONTH, month);
        if (day>0)
            calendar.set(Calendar.DAY_OF_MONTH, day);
        return calendar.getTime();
    }
    
    public static Date getDate(int year, int month, int day)
    {
        Calendar calendar = Calendar.getInstance();
        if (year>0)
            calendar.set(Calendar.YEAR, year);
        if (month>=0)
            calendar.set(Calendar.MONTH, month);
        if (day>0)
            calendar.set(Calendar.DAY_OF_MONTH, day);
        // No Time
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    public static Date setTime(Date date, int hours, int minutes, int seconds, int millis)
    {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, hours);
        calendar.set(Calendar.MINUTE, minutes);
        calendar.set(Calendar.SECOND, seconds);
        calendar.set(Calendar.MILLISECOND, millis);
        return calendar.getTime();
    }

    public static Date getDateOnly(Date date)
    {
        return setTime(date, 0, 0, 0, 0);
    }
    
    // ------- parsing functions -----

    public static Date parseDate(String sDate, Locale locale)
    {
        // Try to parse
        try 
        {
            DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT, getSafeLocale(locale));
            df.setLenient(true);
            return df.parse(sDate);
        } catch (ParseException e)
        {
            log.error("Invalid date value", e);
            return null;
        }
    }
    
    // ------- formating functions -------
    
    private static Locale getSafeLocale(Locale locale)
    {
        return (locale==null) ? Locale.getDefault() : locale;        
    }
    
    public static String formatDate(Date d, Locale locale)
    {
        DateFormat df = DateFormat.getDateInstance(DateFormat.MEDIUM, getSafeLocale(locale));
        return df.format(d);
    }
    
    public static String formatTime(Date d, Locale locale, boolean withSeconds)
    {
        int style = (withSeconds ? DateFormat.MEDIUM : DateFormat.SHORT);
        DateFormat df = DateFormat.getTimeInstance(style, getSafeLocale(locale));
        return df.format(d);
    }
    
    public static String formatDayOfWeek(Date d, Locale locale, boolean longFormat)
    {
        SimpleDateFormat sdf = new SimpleDateFormat("", getSafeLocale(locale));
        Calendar c = Calendar.getInstance(getSafeLocale(locale));
        c.setTime(d);
        int dayOfWeek = c.get(Calendar.DAY_OF_WEEK);
        if (longFormat)
            return sdf.getDateFormatSymbols().getWeekdays()[dayOfWeek];
        else
            return sdf.getDateFormatSymbols().getShortWeekdays()[dayOfWeek];
    }
    
    public static String formatMonth(Date d, Locale locale, boolean longFormat)
    {
        SimpleDateFormat sdf = new SimpleDateFormat("", getSafeLocale(locale));
        Calendar c = Calendar.getInstance(getSafeLocale(locale));
        c.setTime(d);
        int month = c.get(Calendar.MONTH);
        if (longFormat)
            return sdf.getDateFormatSymbols().getMonths()[month];
        else
            return sdf.getDateFormatSymbols().getShortMonths()[month];
    }
    
    public static int getWeekOfYear(Date d, Locale locale)
    {
        Calendar c = Calendar.getInstance(getSafeLocale(locale));
        c.setTime(d);
        return c.get(Calendar.WEEK_OF_YEAR);
    }
    
    public static String formatMonth(int month, Locale locale, boolean longFormat)
    {
        SimpleDateFormat sdf = new SimpleDateFormat("", getSafeLocale(locale));
        if (longFormat)
            return sdf.getDateFormatSymbols().getMonths()[month];
        else
            return sdf.getDateFormatSymbols().getShortMonths()[month];
    }
    
    public static String formatYear(Date d, Locale locale)
    {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy", getSafeLocale(locale));
        Calendar c = Calendar.getInstance(getSafeLocale(locale));
        c.setTime(d);
        return sdf.format(d);
    }
}
