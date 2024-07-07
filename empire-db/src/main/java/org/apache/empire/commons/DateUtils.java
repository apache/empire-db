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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import org.apache.empire.exceptions.InvalidArgumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class contains common functions for comparing and converting values of type Date. 
 * 
 */
public class DateUtils
{
    private static final Logger log = LoggerFactory.getLogger(DateUtils.class);
    
    public static final long MILLIS_IN_DAY = 86400000;
    
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
        if (month>0)
            calendar.set(Calendar.MONTH, month-1);
        if (day>0)
            calendar.set(Calendar.DAY_OF_MONTH, day);
        return calendar.getTime();
    }
    
    public static Date getDate(int year, int month, int day)
    {
        Calendar calendar = Calendar.getInstance();
        if (year>0)
            calendar.set(Calendar.YEAR, year);
        if (month>0)
            calendar.set(Calendar.MONTH, month-1);
        if (day>0)
            calendar.set(Calendar.DAY_OF_MONTH, day);
        // No Time
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }
    
    public static Date getDateTime(int year, int month, int day, int hours, int minutes, int seconds, int millis)
    {
        Calendar calendar = Calendar.getInstance();
        if (year>0)
            calendar.set(Calendar.YEAR, year);
        if (month>0)
            calendar.set(Calendar.MONTH, month-1);
        if (day>0)
            calendar.set(Calendar.DAY_OF_MONTH, day);
        // No Time
        calendar.set(Calendar.HOUR_OF_DAY, hours);
        calendar.set(Calendar.MINUTE, minutes);
        calendar.set(Calendar.SECOND, seconds);
        calendar.set(Calendar.MILLISECOND, millis);
        return calendar.getTime();
    }
    
    public static Date getDateTime(int year, int month, int day, int hours, int minutes, int seconds)
    {
        return getDateTime(year, month, day, hours, minutes, seconds, 0);
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

    public static Date getTimeOnly(Date date)
    {
        return setDate(date, 0, 0, 0);
    }

    public static long compareDates(Date date1, Date date2)
    {
        long time1 = setTime(date1, 0, 0, 0, 0).getTime();
        long time2 = setTime(date2, 0, 0, 0, 0).getTime();
        time1 = time1 / MILLIS_IN_DAY;
        time2 = time2 / MILLIS_IN_DAY;
        return time1-time2;
    }
    
    public static boolean compareEqual(Date date1, Date date2)
    {
        return (compareDates(date1, date2)==0);
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
    
    public static String formatDate(Date date, Locale locale)
    {
        if (date==null)
            return StringUtils.EMPTY;
        DateFormat df = DateFormat.getDateInstance(DateFormat.MEDIUM, getSafeLocale(locale));
        return df.format(date);
    }
    
    public static String formatTime(Date date, Locale locale, boolean withSeconds)
    {
        if (date==null)
            return StringUtils.EMPTY;
        int style = (withSeconds ? DateFormat.MEDIUM : DateFormat.SHORT);
        DateFormat df = DateFormat.getTimeInstance(style, getSafeLocale(locale));
        return df.format(date);
    }
    
    public static String formatDayOfWeek(Date date, Locale locale, boolean longFormat)
    {
        if (date==null)
            return StringUtils.EMPTY;
        SimpleDateFormat sdf = new SimpleDateFormat("", getSafeLocale(locale));
        Calendar c = Calendar.getInstance(getSafeLocale(locale));
        c.setTime(date);
        int dayOfWeek = c.get(Calendar.DAY_OF_WEEK);
        if (longFormat)
            return sdf.getDateFormatSymbols().getWeekdays()[dayOfWeek];
        else
            return sdf.getDateFormatSymbols().getShortWeekdays()[dayOfWeek];
    }
    
    public static String formatMonth(Date date, Locale locale, boolean longFormat)
    {
        if (date==null)
            return StringUtils.EMPTY;
        SimpleDateFormat sdf = new SimpleDateFormat("", getSafeLocale(locale));
        Calendar c = Calendar.getInstance(getSafeLocale(locale));
        c.setTime(date);
        int month = c.get(Calendar.MONTH);
        if (longFormat)
            return sdf.getDateFormatSymbols().getMonths()[month];
        else
            return sdf.getDateFormatSymbols().getShortMonths()[month];
    }
    
    public static int getWeekOfYear(Date date, Locale locale)
    {
        if (date==null)
            throw new InvalidArgumentException("date", date);
        Calendar c = Calendar.getInstance(getSafeLocale(locale));
        c.setTime(date);
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
    
    public static String formatYear(Date date, Locale locale)
    {
        if (date==null)
            return StringUtils.EMPTY;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy", getSafeLocale(locale));
        Calendar c = Calendar.getInstance(getSafeLocale(locale));
        c.setTime(date);
        return sdf.format(date);
    }
    
    /*
     * LocalDate
     */
    
    public static LocalDate toLocalDate(java.sql.Date date)
    {
        return date.toLocalDate();
    }
    
    public static LocalDate toLocalDate(java.sql.Timestamp timestamp)
    {
        return timestamp.toLocalDateTime().toLocalDate();
    }
    
    public static LocalDateTime toLocalDateTime(java.sql.Date date)
    {
        return date.toLocalDate().atStartOfDay();
    }
    
    public static LocalDateTime toLocalDateTime(java.sql.Timestamp timestamp)
    {
        return timestamp.toLocalDateTime();
    }
    
    public static LocalDate toLocalDate(Date date)
    {   // Sql Date
        if (date instanceof java.sql.Date)
            return toLocalDate((java.sql.Date)date);
        // Sql Timestamp
        if (date instanceof java.sql.Timestamp)
            return toLocalDate((java.sql.Timestamp)date);
        // other
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    public static LocalDateTime toLocalDateTime(Date date) 
    {   // Sql Date
        if (date instanceof java.sql.Date)
            return toLocalDateTime((java.sql.Date)date);
        // Sql Timestamp
        if (date instanceof java.sql.Timestamp)
            return toLocalDateTime((java.sql.Timestamp)date);
        // other
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }
    
    public static Date toDate(LocalDate localDate) {
        // return java.util.Date.from(localDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());        
        return java.sql.Date.valueOf(localDate);
    }
    
    public static Date toDate(LocalDateTime localDateTime) {
        // return java.util.Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());        
        return java.sql.Timestamp.valueOf(localDateTime);
    }
    
    public static LocalDate parseLocalDate(String date) {
        // DateTimeFormatter ISO_LOCAL_DATE
        return LocalDate.parse(date);
    }
    
    public static LocalDate parseLocalDate(String date, DateTimeFormatter formatter) {
        return LocalDate.parse(date, formatter);
    }
    
    public static LocalDateTime parseLocalDateTime(String date) {
        //  DateTimeFormatter.ISO_LOCAL_DATE_TIME
        return LocalDateTime.parse(date);
    }
    
    public static LocalDateTime parseLocalDateTime(String date, DateTimeFormatter formatter) {
        return LocalDateTime.parse(date, formatter);
    }
    
    /*
     * Local Date formatting
     */

    public static DateTimeFormatter getLocalDateFormatter(Locale locale)
    {
        return DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(getSafeLocale(locale));
    }
    
    public static DateTimeFormatter getLocalDateTimeFormatter(Locale locale, boolean withSeconds)
    {
        return DateTimeFormatter.ofLocalizedDateTime((withSeconds ? FormatStyle.MEDIUM : FormatStyle.SHORT)).withLocale(getSafeLocale(locale));
    }
    
    public static String formatDate(LocalDate localDate, Locale locale)
    {
        if (localDate==null)
            return StringUtils.EMPTY;
        return getLocalDateFormatter(locale).format(localDate);
    }
    
    public static String formatDate(LocalDateTime localDateTime, Locale locale)
    {
        if (localDateTime==null)
            return StringUtils.EMPTY;
        return getLocalDateFormatter(locale).format(localDateTime.toLocalDate());
    }
    
    public static String formatDateTime(LocalDateTime localDateTime, Locale locale, boolean withSeconds)
    {
        if (localDateTime==null)
            return StringUtils.EMPTY;
        return getLocalDateTimeFormatter(locale, withSeconds).format(localDateTime);
    }
    
}
