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

import java.io.Serializable;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.apache.commons.beanutils.MethodUtils;
import org.apache.empire.data.ColumnExpr;
import org.apache.empire.data.DataType;
import org.apache.empire.exceptions.InvalidArgumentException;
import org.apache.empire.exceptions.ValueConversionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class contains common functions for comparing and converting values from and to the database
 * As well as other useful Array and List related functions. 
 * 
 * Value and value related conversion functions my be overridden by setting a customized version of the
 * ValueUtils object via 
 *      setValueUtils(ValueUtils valueUtils)
 */
public final class ObjectUtils
{
    // Logger
    private static final Logger log = LoggerFactory.getLogger(ObjectUtils.class);
    
    /**
     * This class explicitly defines that an Object has not been assigned a value.<BR>
     * This may be used in cases where the value of null may be a valid value.
     */
    private static final class NoValue implements Serializable
    {
        private static final long serialVersionUID = 1L;
        private NoValue()
        { /* dummy */
        }
        @Override
        public String toString()
        {
        	return "[NO-VALUE]";
        }
    }
    
    /**
     * Constant that defines a object of type NoValue.
     * This may be used in cases where the value of null is a valid value.
     */
    public static final NoValue NO_VALUE = new NoValue();
    
    /**
     * The instance of ValueUtils to be used for value type conversion
     */
    static private ValueUtils valueUtils = new ValueUtils();
    
    public static ValueUtils getValueUtils()
    {
        return valueUtils;
    }

    public static void setValueUtils(ValueUtils valueUtils)
    {
        ObjectUtils.valueUtils = valueUtils;
    }

    /**
     * Private Constructor
     * Static functions only
     * No instance may be created
     */
    private ObjectUtils()
    {
    }
    
    /**
     * Checks whether an object has no value.
     * A Object is empty if and only if the Object is null or if its an empty string. 
     * @param o the object to check
     * @return true if the Object is null or if its an empty string. 
     */
    public static boolean isEmpty(Object o)
    {
        return valueUtils.isEmpty(o);
    }
    
    /**
     * Checks whether an object has a value.
     * A Object is considered to have a value if it is not null and not an empty string 
     * @param o the object to check
     * @return true if the Object is neither null nor an empty string. 
     */
    public static boolean isNotEmpty(Object o)
    {
        return !isEmpty(o);
    }
    
    /**
     * Checks whether a number is null or zero
     * @param value the number to check
     * @return true if the value is null or zero 
     */
    public static boolean isZero(Number value)
    {
        return valueUtils.isZero(value);
    }

    /**
     * Checks whether a number is NOT null or zero
     * @param value the number to check
     * @return true if the value is NOT null or zero 
     */
    public static boolean isNonZero(Number value)
    {
        return !isZero(value);
    }
    
    /**
     * returns the string length of an object
     * @param o the object to check
     * @return the string length of the object
     */
    public static int lengthOf(Object o)
    {
        return valueUtils.lengthOf(o);
    }
    
    /**
     * Compares two objects for equality 
     * 
     * @param o1 the first object
     * @param o2 the second object
     * 
     * @return true if both objects are equal or false otherwise
     */
    public static boolean compareEqual(Object o1, Object o2)
    {
        return valueUtils.compareEqual(o1, o2);
    }
    
    /**
     * Compares two objects for equality 
     * 
     * @param o1 the first object
     * @param o2 the second object
     * 
     * @return true if both objects are equal or false otherwise
     */
    public static int compare(Object o1, Object o2)
    {
        return valueUtils.compare(o1, o2);
    }
    
    /**
     * Checks whether a preferred value is valid and returns an alternative value if not.
     * @param <T> the type of the values
     * @param preferred the preferred return value
     * @param alternative the alternative return value used if the preferred value is null 
     * @return the preferred value if it is not null or the alternative value otherwise
     */
    public static <T> T coalesce(T preferred, T alternative)
    {
        return (preferred!=null ? preferred : alternative);
    }

    /**
     * Deprecated. Use getValueUtils().toInteger() instead
     * @param v the value to convert
     * @return the integer value
     */
    @Deprecated
    public static int toInteger(Object v)
    {
        return valueUtils.toInteger(v);
    }
    
    /**
     * Converts an object value to an integer.
     * <P>
     * If the object value supplied is null or if conversion is not possible then the default value is returned.
     * @param v the obect to convert
     * @param defValue the default value if o is null or conversion is not possible 
     * @return the Integer value of v or a default value
     */
    public static int getInteger(Object v, int defValue)
    {
        // Check empty
        if (ObjectUtils.isEmpty(v))
            return defValue;
        try
        {   // Try to convert
            return valueUtils.toInteger(v);
        } catch (NumberFormatException e) {
            log.error(String.format("Cannot convert value [%s] to int", v));
            return defValue;
        }
    }
    
    /**
     * Converts an object value to an integer.
     * <P>
     * If the object value supplied is null or if conversion is not possible then 0 is returned.
     * @param v the object value to convert
     * @return the Integer value of v or 0
     */
    public static int getInteger(Object v)
    {
        return getInteger(v, 0); 
    }

    /**
     * Deprecated. Use getValueUtils().toLong() instead
     * @param v the value to convert
     * @return the long value
     */ 
    @Deprecated
    public static long toLong(Object v)
    {
        return valueUtils.toLong(v);
    }
    
    /**
     * Converts an object value to a long.
     * <P>
     * If the object value supplied is null or if conversion is not possible then the default value is returned.
     * @param v the obect to convert
     * @param defValue the default value if o is null or conversion is not possible 
     * @return the Integer value of v or a default value
     */
    public static long getLong(Object v, long defValue)
    {
        // Check empty
        if (ObjectUtils.isEmpty(v))
            return defValue;
        try
        {   // Try to convert
            return valueUtils.toLong(v);
        } catch (NumberFormatException e) {
            log.error(String.format("Cannot convert value [%s] to long", v));
            return defValue;
        }
    }
    
    /**
     * Converts an object value to a long.
     * <P>
     * If the object value supplied is null or if conversion is not possible then 0 is returned.
     * @param v the object value to convert
     * @return the Long value of v or 0
     */
    public static long getLong(Object v)
    {
        return getLong(v, 0); 
    }

    /**
     * Deprecated. Use getValueUtils().toDouble() instead
     * @param v the value to convert
     * @return the double value
     */
    @Deprecated
    public static double toDouble(Object v)
    {
        return valueUtils.toDouble(v);
    }
    
    /**
     * Converts an object value to a double.
     * <P>
     * If the object value supplied is null or if conversion is not possible then defValue is returned.
     * @param v the object value to convert
     * @param defValue the default value
     * @return the Long value of v or defValue
     */
    public static double getDouble(Object v, double defValue)
    {
        // Check empty
        if (ObjectUtils.isEmpty(v))
            return defValue;
        try
        {   // Try to convert
            return valueUtils.toDouble(v);
        } catch (NumberFormatException e) {
            log.error(String.format("Cannot convert value [%s] to double", v));
            return defValue;
        }
    }

    /**
     * Converts an object value to a double.
     * <P>
     * If the object value supplied is null or if conversion is not possible then 0.0 is returned.
     * @param v the object value to convert
     * @return the Long value of v or 0
     */
    public static double getDouble(Object v)
    {
        return getDouble(v, 0.0d);
    }

    /**
     * Deprecated. Use getValueUtils().toDecimal() instead
     * @param v the value to convert
     * @return the decimal value
     */
    @Deprecated
    public static BigDecimal toDecimal(Object v)
    {
        return valueUtils.toDecimal(v);
    }
    
    /**
     * Converts an object value to a BigDecimal.
     * <P>
     * If the object value supplied is null or if conversion is not possible then defValue is returned.
     * @param v the object value to convert
     * @param defValue the default value
     * @return the BigDecimal value of v or defValue
     */
    public static BigDecimal getDecimal(Object v, BigDecimal defValue)
    {
        // Check empty
        if (ObjectUtils.isEmpty(v))
            return defValue;
        try
        {   // Try to convert
            return valueUtils.toDecimal(v);
        } catch (NumberFormatException e) {
            log.error(String.format("Cannot convert value [%s] to BigDecimal", v));
            return defValue;
        }
    }

    /**
     * Converts an object value to a BigDecimal.
     * <P>
     * If the object value supplied is null or if conversion is not possible then 0.0 is returned.
     * @param v the object value to convert
     * @return the Long value of v or 0
     */
    public static BigDecimal getDecimal(Object v)
    {
        return getDecimal(v, BigDecimal.ZERO);
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
    public static boolean getBoolean(Object v, boolean defValue)
    {
        return valueUtils.toBoolean(v, defValue);
    }
    
    /**
     * Converts an object value to a boolean.
     * see getBoolean(Object v, boolean defValue) for details.
     * @param v the object to convert
     * @return the boolean value or false if v is null or empty
     */
    public static boolean getBoolean(Object v)
    {
        return valueUtils.toBoolean(v, false);
    }
    
    /**
     * Converts an object to an enum of the given type
     * @param <T> the type of the enum
     * @param enumType the enum type
     * @param value the value to convert
     * @return the enum
     */
    public static <T extends Enum<?>> T getEnum(Class<T> enumType, Object value)
    {
        return valueUtils.toEnum(enumType, value);
    }

    /**
     * find by name
     * @param <T> the type of the enum
     * @param enumType the enum type
     * @param name the enum name
     * @return the enum
     */
    public static <T extends Enum<?>> T getEnumByName(Class<T> enumType, String name)
    {
        return valueUtils.toEnumByName(enumType, name);
    }
    
    /**
     * Convert Enum to Object
     * @param enumValue the enum
     * @param isNumeric flag if number or string is required
     * @return the number or string representing this enum
     */
    public static Object getEnumValue(Enum<?> enumValue, boolean isNumeric)
    {
        return valueUtils.enumToValue(enumValue, isNumeric);
    }
    
    /**
     * Converts an Enum to a String
     * @param enumValue the enum
     * @return the corresponding string value
     */
    public static String getString(Enum<?> enumValue)
    {
        return valueUtils.enumToString(enumValue);
    }
    
    /**
     * Converts an Object to a String
     * @param value the value to convert
     * @return the corresponding string value
     */
    public static String getString(Object value)
    {
        return valueUtils.toString(value);
    }
    
    /**
     * Returns a formatted column value 
     * @param column the column
     * @param value the value to convert
     * @param locale the locale (optional)
     * @return the corresponding string value
     */
    public static String formatColumnValue(ColumnExpr column, Object value, Locale locale)
    {
        return valueUtils.formatColumnValue(column, value, locale);
    }
    
    /**
     * Converts an object value to a Date.
     * <P>
     * @param v the object to convert
     * @return the Date value of v or null
     */
    public static Date getDate(Object v)
    {
        try {
            return valueUtils.toDate(v);
        } catch (ParseException e) {
            throw new ValueConversionException(Date.class, v, e);
        }
    }
    
    /**
     * Converts an object value to a LocalDate.
     * <P>
     * @param v the object to convert
     * @return the LocalDate value of v or null
     */
    public static LocalDate getLocalDate(Object v)
    {
        try {   
            // DateTimeFormatter.ISO_LOCAL_DATE_TIME
            return valueUtils.toLocalDate(v);
        } catch (DateTimeParseException e) {
            throw new ValueConversionException(LocalDate.class, v, e);
        }
    }
    
    /**
     * Converts an object value to a LocalDateTime.
     * <P>
     * @param v the object to convert
     * @return the LocalDateTime value of v or null
     */
    public static LocalDateTime getLocalDateTime(Object v)
    {
        try {
            // DateTimeFormatter.ISO_LOCAL_DATE_TIME
            return valueUtils.toLocalDateTime(v);
        } catch (DateTimeParseException e) {
            throw new ValueConversionException(LocalDateTime.class, v, e);
        }
    }
    
    /**
     * Converts an object value to a LocalDateTime.
     * <P>
     * @param v the object to convert
     * @return the LocalDateTime value of v or null
     */
    public static Timestamp getTimestamp(Object v)
    {
        try {
            // DateTimeFormatter.ISO_LOCAL_DATE_TIME
            return valueUtils.toTimestamp(v);
        } catch (DateTimeParseException e) {
            throw new ValueConversionException(Timestamp.class, v, e);
        }
    }
    
    /**
     * Formats a given date object to a standard ISO date string.
     * The format is "yyyy-MM-dd hh:mm:ss"      
     * 
     * @param date the date to be formated
     * @param withTime indicates whether the date string should include the time or not
     * @return the date string
     */
    public static String formatDate(Date date, boolean withTime)
    {
        return valueUtils.formatDate(date, withTime);
    }
    
    /**
     * Generic conversion function that will convert a object to another value type.
     * 
     * @param <T> the type to convert to
     * @param c the class type to convert to
     * @param v the object to convert
     * 
     * @return the converted value of v or null
     * 
     * @throws ClassCastException if the object is not null and is not assignable to the type T.
     */
    public static <T> T convert(Class<T> c, Object v)
        throws ClassCastException
    {
        return valueUtils.convertToJava(c, v);
    }
    
    /**
     * Converts a column value to a Java type
     * 
     * @param <T> the type to convert to
     * @param column the column expression for metadata access
     * @param value the value to convert
     * @param valueType the desired value type
     * 
     * @return the converted value of v or null
     * 
     * @throws ClassCastException if the object is not null and is not assignable to the type T.
     */
    public static <T> T convertColumnValue(ColumnExpr column, Object value, Class<T> valueType)
        throws ClassCastException
    {
        return valueUtils.convertColumnValue(column, value, valueType);
    }

    /**
     * Converts a value to a specific DataType
     * The returned value is used for generating SQL statements
     * @param type the target data type
     * @param value the value to convert
     * @return the value to be used in SQL statements
     */
    public static Object convertValue(DataType type, Object value)
    {
        return valueUtils.convertToData(type, value);
    }

    /**
     * Checks if a class is assignment compatible with another class
     * @param target the target class
     * @param source the source class
     * @return true if assignment compatible or false otherwise
     */
    public static boolean isAssignmentCompatible(Class<?> target, Class<?> source)
    {
        // try plain assignment
        if (target.isAssignableFrom(source))
            return true;
        // Get Class form Primitive Type
        if (source.isPrimitive())
        {   // Get's the Java Class representing the primitive type
            source = MethodUtils.getPrimitiveWrapper(source);
            if (source == null)
                return false;
            if (target.isAssignableFrom(source))
                return true;
        }
        // Get Class form Primitive Type
        if (target.isPrimitive())
        {   // Get's the Java Class representing the primitive type
            target = MethodUtils.getPrimitiveWrapper(target);
            if (target == null)
                return false;
            if (target.isAssignableFrom(source))
                return true;
        }
        // Assume all numeric types can be converted to target class
        Class<Number> numberClass = Number.class;
        if (numberClass.isAssignableFrom(target) &&
            numberClass.isAssignableFrom(source))
        {   // Both are numeric
            return true;
        }
        // Special case: Allow character to string assignment
        if (source==Character.class && target==String.class)
        {
            return true;
        }    
        // Not compatible
        return false;
    }

    /**
     * Compares two arrays for equality
     *
     * @param array1    the first array
     * @param array2    the second array
     *
     * @return true if both arrays are equal or false otherwise
     */
    public static boolean compareEqual(Object[] array1, Object[] array2)
    {   // Compare Length
        int len1 = (array1!=null ? array1.length : 0);
        int len2 = (array2!=null ? array2.length : 0);
        if (len1!= len2)
            return false;
        // Compare Key Values
        for (int i = 0; i < len1; i++)
        {   // Check String Values
            if (!ObjectUtils.compareEqual(array1[i], array2[i]))
                return false;
        }
        return true;
    }
    
    /**
     * Compares two ColumnExpr for equality
     *
     * @param expr a column expression
     * @param other a column expression
     *
     * @return true if both expressions are equal or false otherwise
     */
    public static boolean compareEqual(ColumnExpr expr, ColumnExpr other)
    {
        if (isWrapper(other) && !isWrapper(expr))
            return expr.equals(unwrap(other));
        else  if (!isWrapper(other) && isWrapper(expr))
            return unwrap(expr).equals(other);
        // both wrapped or both unwrapped
        return expr.equals(other);
    }
    
    /**
     * Generic conversion function that will convert a list to another list type.
     * 
     * @param <T> the type of elements
     * @param t the type class
     * @param source the source collection
     * 
     * @return the new list type
     */
    public static <T> List<T> convert(Class<T> t, Collection<? extends T> source)
    {
        if (source==null)
            return null;
        List<T> target = new ArrayList<T>(source.size());
        target.addAll(source);
        return target;
    }
    
    /**
     * Converts varArgs to an array
     * 
     * @param <T> the type of elements
     * @param t the type of the array
     * @param values the array values
     * 
     * @return the array
     */
    @SafeVarargs
    public static <T> T[] toArray(Class<T> t, T... values)
    {
        if (values.length==0)
            throw new InvalidArgumentException("values", values);
        return values;
    }
    
    /**
     * Converts an Object array to a String array.
     * @param objArray the object array to convert
     * @param defValue default value which will be set for all null objects 
     * @return the String array
     */
    public static String[] toStringArray(Object[] objArray, String defValue)
    {
        if (objArray==null)
            return null;
        String[] strArray = new String[objArray.length];
        for (int i=0; i<objArray.length; i++)
        {
            if (objArray[i]!=null)
                strArray[i]=objArray[i].toString();
            else 
                strArray[i]=defValue;
        }
        return strArray;
    }
    
    /**
     * Converts an array to a list
     * 
     * @param <T> the type of elements
     * @param t the type of the list items
     * @param array the array to be converted
     * 
     * @return the list
     */
    public static <T> List<T> arrayToList(Class<T> t, T[] array)
    {
        if (array==null)
            return null;
        List<T> list = new ArrayList<T>(array.length);
        for (int i=0; i<array.length; i++)
            list.add(array[i]);
        return list;
    }
    
    /**
     * Converts a list to an array
     * e.g.:
     * MyItem[] array = ObjectUtils.listToArray(MyItem[].class, myList)
     * 
     * @param <T> the type of the items in the resulting array
     * @param type the array type
     * @param list the item list
     * 
     * @return the array
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] listToArray(Class<? extends T[]> type, List<? extends T> list)
    {
        if (list==null)
            return null;
        T[] arr = ((Object)type == (Object)Object[].class)
                ? (T[]) new Object[list.size()]
                : (T[]) Array.newInstance(type.getComponentType(), list.size());
        for (int i=0; i<arr.length; i++)
            arr[i] = list.get(i);
        return arr;
    }

    /**
     * Checks whether a object implements the Unwrappable interface and is also a wrapper
     * If the object does not Implement the Interface or is not a wrapper then false is returned 
     * @param object the object to check
     * @return true if the object is a wrapper or false otherwise
     */
    public static boolean isWrapper(Object object)
    {
        return ((object instanceof Unwrappable<?>)) && ((Unwrappable<?>)object).isWrapper();
    }

    /**
     * Unwraps an object implementing the Unwrappable interface
     * If the object does not Implement the Interface or is not a wrapper then the object itself is returned 
     * @param <T> the type of the object
     * @param object the object to unwrap
     * @return the unwrapped object or the object itself
     */
    @SuppressWarnings("unchecked")
    public static <T> T unwrap(T object)
    {
        if ((object instanceof Unwrappable<?>) && ((Unwrappable<?>)object).isWrapper())
        {   // recursive
            return unwrap(((Unwrappable<T>)object).unwrap());
        }
        return object;
    }
    
    /**
     * returns whether or not a array contains a certain item
     * performs a quick (==) comparison first
     * if not found a second check is made using equals and unwrapping of items 
     * 
     * @param <T> the type of the object
     * @param array the array to search
     * @param item the item to search for
     * 
     * @return true if the array contains the item or false otherwise
     */
    @SuppressWarnings("unchecked")
    public static <T> int indexOf(T[] array, T item)
    {
        if (array==null || array.length==0)
            return -1;
        // 1st try (quick)
        for (int i=0; i<array.length; i++)
        {
            if (array[i]==item)
                return i;
        }
        // 2nd try (equals)
        for (int i=0; i<array.length; i++)
        {
            T ai = array[i];
            if (ai!=null && ai.equals(item))                
                return i;
        }
        // 3rd try (unwrap)
        for (int i=0; i<array.length; i++)
        {
            T ai = array[i];
            // check wrapper
            if ((ai instanceof Unwrappable) && ((Unwrappable<?>)ai).isWrapper())
            {   // unwrap
                Object unwrapped = ((Unwrappable<?>)ai).unwrap();
                if (unwrapped==item || unwrapped.equals(item))
                    return i;
            }
        }
        // 3rd try (unwrap)
        if ((item instanceof Unwrappable) && ((Unwrappable<?>)item).isWrapper())
        {   // unwrap
            return indexOf(array, ((Unwrappable<T>)item).unwrap());
        }
        // not found
        return -1;
    }
    
    /**
     * returns whether or not a array contains a certain item
     * performs a simple (==) comparison (fast)
     * 
     * @param <T> the type of elements
     * @param array the array to search
     * @param item the item to search for
     * 
     * @return true if the array contains the item or false otherwise
     */
    public static <T> boolean contains(T[] array, T item)
    {
        return (indexOf(array, item)>=0);
    }

    /**
     * combines two arrays
     * @param <T> the type of the array items
     * @param left the left array
     * @param right the right array
     * @return the combined array
     */
    public static <T> T[] combine(T[] left, T[] right)
    {
        if (left==null || left.length==0)
            return right;
        if (right==null || right.length==0)
            return left;
        // combine both
        T[] result = Arrays.copyOf(left, left.length + right.length);
        System.arraycopy(right, 0, result, left.length, right.length);
        return result;
    }

    /**
     * appends an element to an array
     * @param <T> the type of the array items
     * @param array the array
     * @param element the new element
     * @return the combined array
     */
    public static <T> T[] append(T[] array, T element)
    {
        if (array==null)
            throw new InvalidArgumentException("array", array);
        // append element
        T[] result = Arrays.copyOf(array, array.length + 1);
        result[array.length] = element;
        return result;
    }
}
