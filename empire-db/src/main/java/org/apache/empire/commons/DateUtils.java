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

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.Temporal;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

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
     * Returns a Timestamp for now()
     * The timstamp's nano seconds will be limited to 6 digits!
     * @return the Timestamp
     */
    public static Timestamp getTimestamp()
    {
        LocalDateTime ts = LocalDateTime.now();
        // limit nano to 6 digits (999,999,000)
        int nano = ts.getNano();
        int remain = nano % 1000;
        if (remain>0)
            ts = LocalDateTime.of(ts.getYear(), ts.getMonth(), ts.getDayOfMonth(), ts.getHour(), ts.getMinute(), ts.getSecond(), nano - remain);
        // convert to timestamp
        return Timestamp.valueOf(ts);
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
    
    public static int getDaysBetween(Date date1, Date date2)
    {
        if (date1==null)
            date1= getDateNow();
        if (date2==null)
            date2= getDateNow();
        // calc
        long diffInMillies = date2.getTime() - date1.getTime();
        return (int)TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS);
    }
    
    // ------- date-fields -----
    
    public static int getYear()
    {
        return Calendar.getInstance().get(Calendar.YEAR);    
    }
    
    public static int getYear(Date date)
    {
        // Get the year from the ZonedDateTime
        ZonedDateTime dateTime = date.toInstant().atZone(ZoneId.systemDefault()); 
        return dateTime.get(ChronoField.YEAR);         
    }
    
    public static int getMonth()
    {
        return Calendar.getInstance().get(Calendar.MONTH)+1;    
    }
    
    public static int getMonth(Date date)
    {
        // Get the year from the ZonedDateTime
        ZonedDateTime dateTime = date.toInstant().atZone(ZoneId.systemDefault()); 
        return dateTime.get(ChronoField.MONTH_OF_YEAR);         
    }
    
    public static int getDay()
    {
        return Calendar.getInstance().get(Calendar.DAY_OF_MONTH);    
    }
    
    public static int getDay(Date date)
    {
        // Get the year from the ZonedDateTime
        ZonedDateTime dateTime = date.toInstant().atZone(ZoneId.systemDefault()); 
        return dateTime.get(ChronoField.DAY_OF_MONTH);         
    }
    
    public static int getDayOfWeek()
    {
        // Monday (1) to Sunday (7)
        int dow = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)-1;
        return (dow<=0 ? 7 : dow);    
    }
    
    public static int getDayOfWeek(Date date)
    {
        // Get the year from the ZonedDateTime
        ZonedDateTime dateTime = date.toInstant().atZone(ZoneId.systemDefault()); 
        return dateTime.get(ChronoField.DAY_OF_WEEK);         
    }
    
    public static int getWeekOfYear(Date date, Locale locale)
    {
        // Gets the week of the year
        Calendar c = Calendar.getInstance(getSafeLocale(locale));
        c.setTime(date);
        return c.get(Calendar.WEEK_OF_YEAR);
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
    
    /**
     * get DateFormatSymbols
     */
    private static Map<Locale, DateFormatSymbols> sdfMap = new HashMap<Locale, DateFormatSymbols>(); 
    public static synchronized DateFormatSymbols getDateFormatSymbols(Locale locale)
    {
        if (locale==null)
            locale = getSafeLocale(locale);
        DateFormatSymbols dfs = sdfMap.get(locale);
        if (dfs==null) {
            dfs = new DateFormatSymbols(locale);
            sdfMap.put(locale, dfs);
        }
        return dfs;
    }
    
    public static String formatDayOfWeek(Date date, Locale locale, boolean longFormat)
    {
        if (date==null)
            return StringUtils.EMPTY;
        Calendar c = Calendar.getInstance(getSafeLocale(locale));
        c.setTime(date);
        int dayOfWeek = c.get(Calendar.DAY_OF_WEEK)-1;
        return formatDayOfWeek((dayOfWeek==0 ? 7 : dayOfWeek), locale, longFormat);
    }
    
    public static String formatDayOfWeek(Temporal date, Locale locale, boolean longFormat)
    {
        if (date==null)
            return StringUtils.EMPTY;
        int dayOfWeek = date.get(ChronoField.DAY_OF_WEEK);
        return formatDayOfWeek(dayOfWeek, locale, longFormat);
    }
    
    public static String formatDayOfWeek(int dayOfWeek, Locale locale, boolean longFormat)
    {
        if (dayOfWeek<1 || dayOfWeek>7)
            return StringUtils.EMPTY;
        if (dayOfWeek==7)
            dayOfWeek=0; // Sunday is Index 0
        DateFormatSymbols dfs = getDateFormatSymbols(locale);
        if (longFormat)
            return dfs.getWeekdays()[dayOfWeek+1];
        else
            return dfs.getShortWeekdays()[dayOfWeek+1];
    }
    
    public static String formatMonth(Date date, Locale locale, boolean longFormat)
    {
        if (date==null)
            return StringUtils.EMPTY;
        Calendar c = Calendar.getInstance(getSafeLocale(locale));
        c.setTime(date);
        int month = c.get(Calendar.MONTH)+1;
        return formatMonth(month, locale, longFormat);
    }
    
    public static String formatMonth(Temporal date, Locale locale, boolean longFormat)
    {
        if (date==null)
            return StringUtils.EMPTY;
        int month = date.get(ChronoField.MONTH_OF_YEAR);
        return formatMonth(month, locale, longFormat);
    }
    
    public static String formatMonth(int month, Locale locale, boolean longFormat)
    {
        if (month<1 || month>12)
            return StringUtils.EMPTY;
        DateFormatSymbols dfs = getDateFormatSymbols(locale);
        if (longFormat)
            return dfs.getMonths()[month-1];
        else
            return dfs.getShortMonths()[month-1];
    }
    
    public static String formatYear(Date date, Locale locale)
    {
        if (date==null)
            return StringUtils.EMPTY;
        Calendar c = Calendar.getInstance(getSafeLocale(locale));
        c.setTime(date);
        return (String.format("%04d", c.get(Calendar.YEAR)));
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
        // Sql Time
        if (date instanceof java.sql.Time)
            return toLocalDateTime(new java.util.Date(((java.sql.Time)date).getTime()));
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
    
    public static final String FORMAT_PATTERN_ISO_DATE ="yyyy-MM-dd";  
    public static final String FORMAT_PATTERN_ISO_TIME ="HH:mm:ss";
    public static final String FORMAT_PATTERN_ISO_DATETIME ="yyyy-MM-dd HH:mm:ss";        
    public static final String FORMAT_PATTERN_ISO_TIMESTAMP ="yyyy-MM-dd HH:mm:ss.SSS";
    public static final String FORMAT_PATTERN_ISO_TIMESTAMP6 ="yyyy-MM-dd HH:mm:ss.SSSSSS";
    public static final String FORMAT_PATTERN_ISO_TIMESTAMP9 ="yyyy-MM-dd HH:mm:ss.SSSSSSSSS";
    
    private static final Map<String, DateTimeFormatter> patternFormatterMap = new ConcurrentHashMap<String, DateTimeFormatter>();

    public static DateTimeFormatter getPatternFormatter(String pattern) 
    {
        return patternFormatterMap.computeIfAbsent(pattern, DateTimeFormatter::ofPattern);
    }

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
