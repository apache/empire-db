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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.Temporal;
import java.util.Collection;
import java.util.Date;

import org.apache.commons.beanutils.MethodUtils;
import org.apache.empire.data.DataType;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.DBExpr;
import org.apache.empire.db.expr.column.DBValueExpr;
import org.apache.empire.exceptions.InvalidValueException;
import org.apache.empire.exceptions.ItemNotFoundException;
import org.apache.empire.exceptions.ValueConversionException;

/**
 * This class allows to customize value type conversion as well as other value related functions.
 * The functions and methods are called indirectly via the corresponding static functions in the ObjectUtils class. * 
 * You may create your own derived class and overwrite the methods.
 * In order to activate your ValueUtils implementation use the static function
 * 
 *      OjectUtils.setValueUtils(myValueUtils) 
 * 
 * @author doebele
 */
public class ValueUtils
{
    // Logger
    // private static final Logger log = LoggerFactory.getLogger(ValueUtils.class);

    protected static final String DATE_FORMAT = "yyyy-MM-dd";
    protected static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    
    // DateOnly Formatter
    protected static final ThreadLocal<SimpleDateFormat> dateOnlyFormatter = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue()
        {
            return new SimpleDateFormat(DATE_FORMAT);
        }
    };

    // DateTime Formatter
    protected static final ThreadLocal<SimpleDateFormat> dateTimeFormatter = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue()
        {
            return new SimpleDateFormat(DATETIME_FORMAT);
        }
    };
    
    protected ValueUtils()
    {
        /* subclass to override */
    }
    
    /**
     * Checks whether an object has no value.
     * A Object is empty if and only if the Object is null or if its an empty string. 
     * @param o the object to check
     * @return true if the Object is null or if its an empty string. 
     */
    public boolean isEmpty(Object o)
    {
        if (o==null)
            return true;
        if (o==ObjectUtils.NO_VALUE)
            throw new InvalidValueException(o);
        if ((o instanceof String) && ((String)o).length()==0)
            return true;
        if ((o instanceof Object[]) && ((Object[])o).length==0)
            return true;
        if ((o instanceof Collection<?>) && ((Collection<?>)o).isEmpty())
            return true;
        if (o instanceof DBValueExpr)
            return isEmpty(((DBValueExpr)o).getValue());
        if (o instanceof Enum)
            return isEmpty(getString((Enum<?>)o));
        // not empty
        return false;
    }
    
    /**
     * Checks whether a number is null or zero
     * @param value the number to check
     * @return true if the value is null or zero 
     */
    public boolean isZero(Number value)
    {
        if (value==null)
            return true;
        if (value instanceof BigDecimal)
            return (BigDecimal.ZERO.compareTo((BigDecimal)value) == 0);
        if (value instanceof Float)
            return (((Float) value).compareTo(0.0f)==0);
        if (value instanceof Double)
            return (((Double) value).compareTo(0.0d)==0);
        if (value instanceof Long)
            return (value.longValue()==0l);
        // default: check int value
        return (value.intValue()==0);
    }
    
    /**
     * Compares two objects for equality 
     * 
     * @param o1 the first object
     * @param o2 the second object
     * 
     * @return true if both objects are equal or false otherwise
     */
    @SuppressWarnings("unchecked")
    public boolean compareEqual(Object o1, Object o2)
    {
        // simple case
        if (o1==o2)
            return true;
        // Check for Empty Values
        if (isEmpty(o1))
            return isEmpty(o2);
        if (isEmpty(o2))
            return isEmpty(o1);
        // Check classes
        if (o1.getClass().equals(o2.getClass()))
        {   // Check simple array
            if ((o1 instanceof Object[]) && (o2 instanceof Object[])) 
                return compareEqual(o1, o2);
            // Check if object implements comparable
            if (o1 instanceof Comparable)
                return (((Comparable<Object>)o1).compareTo(o2)==0);
            else
                return o1.equals(o2);
        }
        // Classes don't match
        // Use equal check first
        if (o1.equals(o2) || o2.equals(o1))
            return true;
        // DBExpr 
        if ((o1 instanceof DBExpr) || (o2 instanceof DBExpr))
            return false;
        // Compare Numbers
        if (o1 instanceof Number && o2 instanceof Number)
        {   // boolean test = obj1.equals(obj2);
            double d1 = ((Number)o1).doubleValue();
            double d2 = ((Number)o2).doubleValue(); 
            return (d1==d2);
        }
        // Compare Date with LocalDate / LocalDateTime
        if (o1 instanceof Temporal && o2 instanceof Date)
        {   // swap
            Object tmp = o2; o2 = o1; o1 = tmp; 
        }
        if (o1 instanceof Date && o2 instanceof LocalDate)
            return o1.equals(DateUtils.toDate((LocalDate)o2));
        if (o1 instanceof Date && o2 instanceof LocalDateTime)
            return o1.equals(DateUtils.toDate((LocalDateTime)o2));
        // Enum
        if (o1 instanceof Enum<?>)
        {   // Special enum handling   
            if (o2 instanceof Number)
                return ((Enum<?>)o1).ordinal()==((Number)o2).intValue();
            // Compare Strings
            String strVal = getString((Enum<?>)o1);
            return StringUtils.compareEqual(strVal, getString(o2));
        }
        else if (o2 instanceof Enum<?>)
        {   // Special enum handling   
            if (o1 instanceof Number)
                return ((Enum<?>)o2).ordinal()==((Number)o1).intValue();
            // Compare Strings
            String strVal = getString((Enum<?>)o2); 
            return StringUtils.compareEqual(strVal, getString(o1));
        }
        // Compare Strings
        if (o1 instanceof String)
            return ((String)o1).equals(o2.toString());
        if (o2 instanceof String)
            return ((String)o2).equals(o1.toString());
        // Not equal
        return false;
    }
    
    /**
     * Compares two objects for equality 
     * 
     * @param o1 the first object
     * @param o2 the second object
     * 
     * @return true if both objects are equal or false otherwise
     */
    @SuppressWarnings("unchecked")
    public int compare(Object o1, Object o2)
    {
        // simple case
        if (o1==o2)
            return 0;
        // Check for Empty Values
        if (isEmpty(o1))
            return isEmpty(o2) ? 0 : -1;
        if (isEmpty(o2))
            return isEmpty(o1) ? 0 :  1;
        // Check classes
        if (o1.getClass().equals(o2.getClass()))
        {   // Check if object implements comparable
            if (o1 instanceof Comparable)
                return ((Comparable<Object>)o1).compareTo(o2);
            if (o2 instanceof Comparable)
                return ((Comparable<Object>)o2).compareTo(o1);
        }    
        // Use equal check first
        if (o1.equals(o2) || o2.equals(o1))
            return 0;
        // Compare Numbers
        if (o1 instanceof Number && o2 instanceof Number)
        {   // boolean test = obj1.equals(obj2);
            double d1 = ((Number)o1).doubleValue();
            double d2 = ((Number)o2).doubleValue();
            return ((d1<d2) ? -1 : ((d1>d2) ? 1 : 0));
        }
        // Compare Date with LocalDate / LocalDateTime
        if (o1 instanceof Temporal && o2 instanceof Date)
        {   // swap
            Object tmp = o2; o2 = o1; o1 = tmp; 
        }
        if (o1 instanceof Date && o2 instanceof LocalDate)
            return compare(o1, DateUtils.toDate((LocalDate)o2));
        if (o1 instanceof Date && o2 instanceof LocalDateTime)
            return compare(o1, DateUtils.toDate((LocalDateTime)o2));
        // Compare Strings
        return o1.toString().compareTo(o2.toString());
    }

    /**
     * converts an object to an integer. If conversion is not possible, an error is thrown
     * @param v the value to convert
     * @return the integer value
     */
    public int toInteger(Object v)
    {
        if (isEmpty(v))
            return 0;
        if (v instanceof Number)
            return ((Number)v).intValue();
        // Try to convert
        String str = v.toString();
        return Integer.parseInt(str);
    }

    /**
     * converts an object to a long. If conversion is not possible, an error is thrown
     * @param v the value to convert
     * @return the long value
     */
    public long toLong(Object v)
    {
        if (isEmpty(v))
            return 0;
        if (v instanceof Number)
            return ((Number)v).longValue();
        // Try to convert
        String str = v.toString();
        return Long.parseLong(str);
    }

    /**
     * converts an object to a double. If conversion is not possible, an error is thrown
     * @param v the value to convert
     * @return the double value
     */
    public double toDouble(Object v)
    {
        // Get Double value
        if (isEmpty(v))
            return 0.0;
        if (v instanceof Number)
            return ((Number)v).doubleValue();
        // parse String for Integer value
        String val = v.toString(); 
        return Double.parseDouble(val);
    }

    /**
     * converts an object to a decimal. If conversion is not possible, an error is thrown
     * @param v the value to convert
     * @return the decimal value
     */
    public BigDecimal toDecimal(Object v)
    {
        // Get Double value
        if (isEmpty(v))
            return null;
        if (v instanceof BigDecimal)
            return ((BigDecimal)v);
        // Find a suitable converter
        if (v instanceof Number)
        {   // check other number types
            if (v instanceof BigInteger)
                return new BigDecimal((BigInteger)v);
            if (v instanceof Integer)
                return BigDecimal.valueOf(((Number)v).intValue());
            if (v instanceof Long)
                return BigDecimal.valueOf(((Number)v).longValue());
            // Default: convert via double
            return BigDecimal.valueOf(((Number)v).doubleValue());
        }
        // parse String for Integer value
        // Last-Chance > Try a string conversion
        return new BigDecimal(v.toString());
    }
    
    /**
     * Converts an object value to a boolean.
     * <P>
     * If the object value supplied is empty then the defValue is returned 
     * Numbers are considered true if they are not equal to zero
     * String are considered true only if the string is "Y" or "true"
     * @param v the object to convert
     * @param defValue the default value
     * @return the boolean value or defValue if v is null or empty
     */
    public boolean getBoolean(Object v, boolean defValue)
    {
        // Get Boolean value
        if (isEmpty(v))
            return defValue;
        if (v instanceof Boolean)
            return ((Boolean)v).booleanValue();
        if (v instanceof Number)
            return (((Number)v).intValue()!=0);
        if (v instanceof String) {
            String val = ((String)v);
            if (StringUtils.isEmpty(val))
                return defValue;
            // check for allowed true values
            return (val.equalsIgnoreCase("true") || val.equalsIgnoreCase("Y"));
        }
        return defValue;
    }
    
    /**
     * Converts an object to an enum of the given type
     * @param <T> the type of the enum
     * @param enumType the enum type
     * @param value the value to convert
     * @return the enum
     */
    @SuppressWarnings("unchecked")
    public <T extends Enum<?>> T getEnum(Class<T> enumType, Object value)
    {   // check for null
        if (isEmpty(value))
            return null;
        // check enum
        if (value instanceof Enum<?>)
        {   // already an enum: Check type
            if (value.getClass().equals(enumType))
                return (T)value;
            // try to match names
            value = ((Enum<?>)value).name();
        }
        // check column data type
        boolean numeric = (value instanceof Number);
        T[] items = enumType.getEnumConstants();
        if (items.length>0 && (items[0] instanceof EnumValue))
        {   // custom conversion
            for (T e : items)
            {
                Object eVal = ((EnumValue)e).toValue(numeric);
                if (compareEqual(eVal, value))
                    return e;
            }
            // error: not found
            throw new ItemNotFoundException(StringUtils.toString(value));
        }
        else if (numeric)
        {   // by ordinal
            int ordinal = ((Number)value).intValue();
            // check range
            if (ordinal<0 || ordinal>=items.length)
                throw new ItemNotFoundException(String.valueOf(ordinal));
            // return enum
            return items[ordinal]; 
        }
        else
        {   // by name
            String name = StringUtils.toString(value);
            // find name
            for (T e : items)
                if (e.name().equals(name))
                    return e;
            // error: not found
            throw new ItemNotFoundException(name);
        }
    }

    /**
     * find by name
     * @param <T> the type of the enum
     * @param enumType the enum type
     * @param name the enum name
     * @return the enum
     */
    public <T extends Enum<?>> T getEnumByName(Class<T> enumType, String name)
    {   // check for null
        if (isEmpty(name))
            return null;
        // check column data type
        T[] items = enumType.getEnumConstants();
        for (T e : items)
            if (e.name().equals(name))
                return e;
        // error: not found
        throw new ItemNotFoundException(name);
    }
    
    /**
     * Convert Enum to Object
     * @param enumValue the enum
     * @param isNumeric flag if number or string is required
     * @return the number or string representing this enum
     */
    public Object getEnumValue(Enum<?> enumValue, boolean isNumeric)
    {
        // convert
        if (enumValue instanceof EnumValue)
            return ((EnumValue)enumValue).toValue(isNumeric);
        // default
        return (isNumeric ? enumValue.ordinal() : getString(enumValue));
    }
    
    /**
     * Converts an Enum to a String
     * @param enumValue the enum
     * @return the corresponding string value
     */
    public String getString(Enum<?> enumValue)
    {
        // convert
        if (enumValue instanceof EnumValue)
            return StringUtils.toString(((EnumValue)enumValue).toValue(false));
        /* special case */
        if (enumValue==null || enumValue.name().equalsIgnoreCase("NULL"))
            return StringUtils.EMPTY;
        /* use name */
        return enumValue.name();
    }
    
    /**
     * Converts an Object to a String
     * @param value the value to convert
     * @return the corresponding string value
     */
    public String getString(Object value)
    {
        if (value==null)
            return null;
        if (value instanceof String)
            return (String)value;
        if (value==ObjectUtils.NO_VALUE)
            throw new InvalidValueException(value);
        // convert
        if (value instanceof Enum<?>)
            return getString((Enum<?>)value);
        if (value instanceof Date)
            return formatDate((Date)value, true);
        // default
        return value.toString();
    }
    
    /**
     * returns the string length of an object
     * @param o the object to check
     * @return the string length of the object
     */
    public int lengthOf(Object o)
    {
        if (o==null || o==ObjectUtils.NO_VALUE)
            return 0;
        if ((o instanceof String))
            return ((String)o).length();
        // convert
        return o.toString().length();
    }
    
    /**
     * Converts an object value to a Date.
     * <P>
     * @param v the object to convert
     * @return the Date value of o or null
     * @throws ParseException
     */
    public Date toDate(Object v)
        throws ParseException
    {
        // Get DateTime value
        if (isEmpty(v))
            return null;
        if (v instanceof java.util.Date)
            return ((java.util.Date)v);
        if (v instanceof java.time.LocalDate)
            return DateUtils.toDate((LocalDate)v);
        if (v instanceof java.time.LocalDateTime)
            return DateUtils.toDate((LocalDateTime)v);
        if (v instanceof Number)
        {   // Get Date from a number
            long l = ((Number)v).longValue();
            return (l==0 ? null : new Date(l));
        }
        // Convert from String
        String str = v.toString();
        if (str.length() > 20)
            return Timestamp.valueOf(str);
        if (str.length() > 10)
            return dateTimeFormatter.get().parse(str);
        else
            return dateOnlyFormatter.get().parse(str);
    }
    
    /**
     * Converts an object value to a Date.
     * <P>
     * @param v the object to convert
     * @return the LocalDate value of o or null
     */
    public LocalDate toLocalDate(Object v)
    {
        // Get DateTime value
        if (isEmpty(v))
            return null;
        if (v instanceof java.time.LocalDate)
            return (LocalDate)v;
        if (v instanceof java.time.LocalDateTime)
            return ((LocalDateTime)v).toLocalDate();
        if (v instanceof java.sql.Timestamp)
            return ((java.sql.Timestamp)v).toLocalDateTime().toLocalDate();
        if (v instanceof java.sql.Date)
            return ((java.sql.Date)v).toLocalDate();
        if (v instanceof java.util.Date)
            return DateUtils.toLocalDate((Date)v);
        // Convert from String
        // DateTimeFormatter.ISO_LOCAL_DATE_TIME
        String str = v.toString();
        return LocalDate.parse(str);
    }
    
    /**
     * Converts an object value to a Date.
     * <P>
     * @param v the object to convert
     * @return the LocalDateTime value of o or null
     */
    public LocalDateTime toLocalDateTime(Object v)
    {
        // Get DateTime value
        if (isEmpty(v))
            return null;
        if (v instanceof java.time.LocalDate)
            return ((LocalDate)v).atStartOfDay();
        if (v instanceof java.time.LocalDateTime)
            return (LocalDateTime)v;
        if (v instanceof java.sql.Timestamp)
            return ((java.sql.Timestamp)v).toLocalDateTime();
        if (v instanceof java.sql.Date)
            return ((java.sql.Date)v).toLocalDate().atStartOfDay();
        if (v instanceof java.util.Date)
            return DateUtils.toLocalDateTime((Date)v);
        // Convert from String
        // DateTimeFormatter.ISO_LOCAL_DATE_TIME
        String str = v.toString();
        return LocalDateTime.parse(str);
    }
    
    /**
     * Formats a given date object to a standard ISO date string.
     * The format is "yyyy-MM-dd hh:mm:ss"      
     * 
     * @param date the date to be formated
     * @param withTime indicates whether the date string should include the time or not
     * @return the date string
     */
    public String formatDate(Date date, boolean withTime)
    {
        if (date==null)
            return null;
        if (withTime)
            return dateTimeFormatter.get().format(date);
        else
            return dateOnlyFormatter.get().format(date);
    }
    
    /**
     * Generic conversion function that will convert a object to another value type.
     * This function is intended to be used for converting values coming from the database
     * to be used by the program
     * 
     * @param c the class type to convert to
     * @param v the value to convert
     * 
     * @return the converted value
     * 
     * @throws ClassCastException if the object is not null and is not assignable to the type T.
     */
    @SuppressWarnings("unchecked")
    public <T> T convert(Class<T> c, Object v)
        throws ClassCastException
    {
        if (v==null || c.isInstance(v))
            return (T)v;
        if (v==ObjectUtils.NO_VALUE)
            throw new InvalidValueException(v);
        // Get Class form Primitive Type
        if (c.isPrimitive())
        {   // Get's the Java Class representing the primitive type
            c = (Class<T>) MethodUtils.getPrimitiveWrapper(c);
        }    
        // Convert
        if (c.isEnum())
        {   // convert to enum
            Object ev = getEnum((Class<? extends Enum<?>>)c, v); 
            return (T)ev;
        }
        if (c.isAssignableFrom(Boolean.class))
            return c.cast(getBoolean(v, false));
        if (c.isAssignableFrom(Integer.class))
            return c.cast(isEmpty(v) ? 0 : toInteger(v));
        if (c.isAssignableFrom(Long.class))
            return c.cast(isEmpty(v) ? 0 : toLong(v));
        if(c.isAssignableFrom(Double.class))
            return c.cast(isEmpty(v) ? 0.0f : toDouble(v));
        if(c.isAssignableFrom(BigDecimal.class))
            return c.cast(isEmpty(v) ? BigDecimal.ZERO : toDecimal(v));
        if (c.isAssignableFrom(String.class))
            return c.cast(getString(v));
        // other
        return c.cast(v);
    }

    /**
     * Converts a value to a specific DataType
     * The returned value is used for generating SQL statements
     * @param dataType the target data type
     * @param value the value to convert
     * @return the value to be used in SQL statements
     */
    public Object convertValue(DataType dataType, Object value)
    {
        // check null
        if (value == null)
            return null;
        // Cannot convert DBEXpr, return as is
        if (value instanceof DBExpr)
            return value;
        // Strings special
        if ((value instanceof String) && ((String)value).length()==0)
            return null;
        // check option entry
        if (value instanceof OptionEntry)
        {   // option value
            value = ((OptionEntry)value).getValue();
        }
        // check for enum
        if (value instanceof Enum<?>)
        {   // Convert Enum now (optional)
            // value = getEnumValue((Enum<?>)value, dataType.isNumeric());
            // Otherwise Enum will be converted later
            return value; 
        }
        // check type
        switch (dataType)
        {
            case BLOB:
                return value; // unchanged
            case BOOL:
                return getBoolean(value, false);
            case DATE:
            case DATETIME:
            case TIMESTAMP:
                // check type
                if ((value instanceof Date) || (value instanceof Temporal))
                    return value; // already a date or temporal
                // sysdate
                if (DBDatabase.SYSDATE.equals(value))
                    return value; // leave SYSDATE as is
                // try to convert
                try {
                    return toDate(value);
                } catch (ParseException e) {
                    throw new ValueConversionException(Date.class, value, e);
                }
            case INTEGER:
                return (value instanceof Number) ? value : toLong(value);
            case FLOAT:
                return (value instanceof Number) ? value : toDouble(value);
            case DECIMAL:
                return (value instanceof Number) ? value : toDecimal(value);
            case CHAR:
            case CLOB:
            case VARCHAR:                
                return (value instanceof String) ? value : value.toString(); // not not call getString(...);
            default:
                // use as is
                return value;
        }
    }
}
