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
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class contains common functions for comparing and converting values of type String. 
 * 
 */
public class StringUtils
{
    // Logger
    private static final Logger log = LoggerFactory.getLogger(StringUtils.class);
    
    /**
     * ObjectStringifier
     * Converts an object to a String
     */
    public static class ObjectStringifier
    { 
        /**
         * Returns whether or not to ignore empty values in arrays
         * @param separator the list separator
         * @return true if empty values should be treated as non-existent or false otherwise
         */
        public boolean isIgnoreEmpty(String separator)
        {
            return SPACE.equals(separator);
        }

        /**
         * Convert a single root element
         * @param value the value to convert
         * @param defValue the default value
         * @return the value string 
         */
        public String toString(Object value, String defValue)
        {
            return toString(value, DEFAULT_ITEM_SEPARATOR, defValue);
        }
        
        /**
         * Convert a single element inside an array or collection
         * @param value the value to convert
         * @param listTemplate the list template
         * @param defValue the default value
         * @return the value string 
         */
        public String toString(Object value, String listTemplate, String defValue)
        {
            // Collections
            if (value instanceof Object[])
                value = arrayToString((Object[])value, listTemplate, (defValue!=null ? defValue : EMPTY), this);
            else if (value instanceof Collection<?>)
                value = listToString((Collection<?>)value, listTemplate, (defValue!=null ? defValue : EMPTY), this);
            // default
            return ((value!=null) ? value.toString() : defValue);
        }
        
        /**
         * Estimates the buffer size required for the target String
         * This function should be fast and rather estimate to much than to little
         * @param value the value for which to estimate
         * @param defValueLength the length of the default value
         * @return the estimated buffer size
         */
        public int estimateBufferSize(Object value, int defValueLength)
        {
            if (value instanceof String)
                return ((String)value).length();
            else if (value instanceof BigDecimal) { 
                BigDecimal bd = (BigDecimal)value;
                return bd.precision()+1;
            }
            else if (value instanceof Number) {
                long lval =((Number)value).longValue();
                long temp = 1;
                int len =0;
                while (temp <= lval) {
                    len++;
                    temp *= 10;
                }
                if ((value instanceof Double) || (value instanceof Float))
                    len += 5; // add 4 for decimal places plus separator
                return len;
            }
            else if (value instanceof Character)
                return 1;
            else if (value instanceof Enum<?>)
                return value.toString().length(); // assume this is quick
            else
                return 20; // Unknown type
        }
    }
    
    /**
     * StringUtils constructor
     * No instances allowed
     */
    private StringUtils()
    {
        // Static Function only
        // No instance may be created
    }

    /**
     * Empty String
     */
    public static final String EMPTY = "";

    /**
     * Single Space
     */
    public static final String SPACE = " ";

    /**
     * Null String
     */
    public static final String NULL = "null";

    /**
     * Default Item Separator
     */
    public static String DEFAULT_ITEM_SEPARATOR = "|";

    /**
     * Default List Template
     */
    public static String LIST_TEMPLATE = "[*|*]";
    public static char   TEMPLATE_SEP_CHAR = '*';
    
    private static ObjectStringifier defaultStringifier = new ObjectStringifier(); 

    public static ObjectStringifier getObjectStringifier()
    {
        return defaultStringifier; 
    }
    
    public static ObjectStringifier setObjectStringifier(ObjectStringifier stringifier)
    {
        ObjectStringifier prev = defaultStringifier; 
        defaultStringifier = stringifier; 
        return prev;
    }
    
    /**
     * Converts a value to a string.
     * If the value is null then the default value is returned.
     * 
     * @param value the value to convert
     * @param listTemplate the list template or item separator to be used for arrays and lists
     * @param defValue default value which to return if value is null
     * @return returns a string for the object or the defValue if null
     
    public static String toString(Object value, String listTemplate, String defValue)
    {
        // Collections
        if (value instanceof Object[])
            value = arrayToString((Object[])value, listTemplate, (defValue!=null ? defValue : EMPTY));
        else if (value instanceof Collection<?>)
            value = listToString((Collection<?>)value, listTemplate, (defValue!=null ? defValue : EMPTY));
        // default
        return ((value!=null) ? value.toString() : defValue);
    }
    */
    
    /**
     * Converts a value to a string.
     * If the value is null then the default value is returned.
     * 
     * @param value the value to convert
     * @param defValue default value which to return if value is null
     * @return returns a string for the object or the defValue if null
     */
    public static String toString(Object value, String defValue)
    {
        return defaultStringifier.toString(value, defValue);
    }

    /**
     * Converts a value to a string.
     * If the value is null then null will be returned.
     * 
     * @param value the value to convert
     * @return returns a string for the object or null
     */
    public static String toString(Object value)
    {
        return toString(value, null);
    }

    /**
     * Converts a value to a string.
     * if the value is null an empty string is returned.
     * 
     * @param value the value to convert
     * @return returns a string for the object or an empty string if null
     */
    public static String valueOf(Object value)
    {
        return toString(value, EMPTY);
    }

    /**
     * Converts a value to a string.
     * Almost same as toString() but Lists and Arrays are wrapped with the standard list template (LIST_TEMPLATE)
     * if the value is null an empty string is returned.
     * 
     * @param value the value to convert
     * @return returns a string for the object or an empty string if null
     */
    public static String asString(Object value)
    {
        if (value==null)
            return EMPTY;
        // array or list
        if (value.getClass().isArray() || (value instanceof Collection<?>))
            return defaultStringifier.toString(value, LIST_TEMPLATE, EMPTY);
        // single value
        return defaultStringifier.toString(value, EMPTY);
    }
    
    /**
     * Returns the length of a String
     * @param value the string value
     * @return length of the string value or 0
     */
    public static int length(String value)
    {
        return (value!=null ? value.length() : 0);
    }
    
    /**
     * Returns the preferred String if it is not empty
     * ot the alternative String otherwise.
     * 
     * @param preferred the preferred String
     * @param alternative the alternative String if the preferred String is not valid
     * @return the preferred String if it is not empty ot the alternative String otherwise 
     */
    public static String coalesce(String preferred, String alternative)
    {
        return isEmpty(preferred) ? alternative : preferred;        
    }

    /**
     * Returns null if the value supplied is null or an empty String. 
     * 
     * @param value the value to check
     * @return null if the value supplied is null or an empty String or the value as a string otherwise 
     */
    public static String nullIfEmpty(Object value)
    {
        if (value==null)
            return null;
        String strval = value.toString();
        return (isEmpty(strval) ? null : strval);   
    }

    /**
     * Returns true if the given substring is part of the string provided by value 
     * 
     * @param value the value to check
     * @param substring the substring
     * @return true if the given substring is part of the string provided by value 
     */
    public static boolean contains(String value, String substring)
    {
        if (value==null || substring==null)
            return false;
        return ((value.indexOf(substring))>=0);
    }

    /**
     * Returns true if the given substring is part of the string provided by value 
     * 
     * @param value the value to check
     * @param substring the substring
     * @return true if the given substring is part of the string provided by value 
     */
    public static boolean notContains(String value, String substring)
    {
        if (value==null || substring==null)
            return true;
        return ((value.indexOf(substring))<0);
    }

    /**
     * Returns the first index of any of the given characters in value starting from the beginning 
     * 
     * @param value the value to check
     * @param chars the characters to search
     * @return the index 
     */
    public static int indexOfAny(String value, char... chars)
    {
        if (value==null || chars.length==0)
            return -1;
        // search
        for (int i=0; i<value.length(); i++) {
            char c = value.charAt(i);
            for (int j=0; j<chars.length; j++)
                if (c==chars[j])
                    return i;
        }
        return -1; // Not found
    }

    /**
     * Returns the first index of any of the given characters in value starting from the end 
     * 
     * @param value the value to check
     * @param chars the characters to search
     * @return the index 
     */
    public static int lastIndexOfAny(String value, char... chars)
    {
        if (value==null || chars.length==0)
            return -1;
        // search
        for (int i=value.length(); i>=0; i--) {
            char c = value.charAt(i);
            for (int j=0; j<chars.length; j++)
                if (c==chars[j])
                    return i;
        }
        return -1; // Not found
    }
    
    /**
     * Converts an array of objects to a string.
     * 
     * @param array array of objects
     * @param template the list template or item separator
     * @param defItemValue the default item value
     * @param stringifier interface to convert an object to a string 
     * @return returns a String or null if the array is null or empty
     */
    public static String arrayToString(Object[] array, String template, String defItemValue, ObjectStringifier stringifier)
    {
        if (array==null || array.length==0)
            return null;
        // check 
        int tbeg = (template!=null ? template.indexOf(TEMPLATE_SEP_CHAR) : -1);
        if (array.length>1 || tbeg>0)
        {   // build the list
            int tend =(tbeg>=0 ? template.lastIndexOf(TEMPLATE_SEP_CHAR) : -1);
            String separator = ((tbeg>0) ? (tend>tbeg ? template.substring(tbeg+1, tend) : DEFAULT_ITEM_SEPARATOR) : template);
            // create StringBuilder
            boolean ignoreEmpty = stringifier.isIgnoreEmpty(separator);
            int bufferLen = estimateArrayBufferSize(array, stringifier, length(separator), length(defItemValue), ignoreEmpty);
            if (template!=null && template.length()>separator.length())
            {   // template extra
                if (tbeg>0)
                    bufferLen += tbeg;
                if (tend>0)
                    bufferLen += (template.length()-tend-1); 
            }
            StringBuilder buf = new StringBuilder(bufferLen);
            if (tend>0)
                buf.append(template.substring(0, tbeg)); // add template prefix
            boolean isEmpty = true;
            boolean hasValue = false;
            for (int i = 0; i < array.length; i++)
            {   // append separator
                if (hasValue && separator!=null)
                    buf.append(separator);
                // append value
                String value = stringifier.toString(array[i], template, defItemValue);
                hasValue = (value!=null && !(ignoreEmpty && StringUtils.isEmpty(value)));
                isEmpty &= !hasValue;
                if (hasValue)
                    buf.append(value);
            }
            if (hasValue==false && !isEmpty && separator!=null)
                buf.setLength(buf.length()-separator.length()); // remove last separator
            if (tend>0)
                buf.append(template.substring(tend+1)); // add template suffix
            if (buf.length()!=bufferLen)
                log.debug("estimateArrayBufferSize returned {} but string length is {}", bufferLen, buf.length());
            return buf.toString();
        }
        // Only one member
        String value = stringifier.toString(array[0], template, defItemValue);
        if (stringifier.isIgnoreEmpty(template) && StringUtils.isEmpty(value))
            return defItemValue;
        return value;
    }
    
    /**
     * Converts an array of objects to a string.
     * 
     * @param array array of objects
     * @param template the list template or item separator
     * @param defItemValue the default item value
     * @return returns a String or null if the array is null or empty
     */
    public static String arrayToString(Object[] array, String template, String defItemValue)
    {
        return arrayToString(array, template, defItemValue, defaultStringifier);
    }

    /**
     * Converts an array of objects to a string.
     * 
     * @param array array of objects
     * @param separator the separator to put between the object strings
     * @return returns a String
     */
    public static String arrayToString(Object[] array, String separator)
    {
        return arrayToString(array, separator, EMPTY);
    }

    /**
     * Converts an array of objects to a string.
     * 
     * @param array array of objects
     * @return returns a String
     */
    public static String arrayToString(Object[] array)
    {
        return arrayToString(array, DEFAULT_ITEM_SEPARATOR, EMPTY);
    }

    /**
     * Estimates the buffer size needed to convert an Array into a String
     * @param array the array
     * @param stringifier interface to convert an object to a string
     * @param separatorLength the separator length
     * @param defValueLength the default length
     * @param ignoreEmpty flag whether to ignore empty parts
     * @return the estimated length of the collection parts
     */
    public static int estimateArrayBufferSize(Object[] array, ObjectStringifier stringifier, int separatorLength, int defValueLength, boolean ignoreEmpty)
    {
        int estimate = 0;
        for (int i = 0; i < array.length; i++) 
        {
            Object item = array[i];
            int len = 0;
            if (item instanceof String) {
                // when ignoreEmpty check string for emptyness
                if (ignoreEmpty && StringUtils.isEmpty(((String)item)))
                    len=0;
                else
                    len = ((String)item).length();
            }
            else if (item instanceof Object[])
                len = estimateArrayBufferSize((Object[])item, stringifier, separatorLength, defValueLength, ignoreEmpty) + 2;
            else if (item instanceof Collection<?>)
                len = estimateListBufferSize((Collection<?>)item, stringifier, separatorLength, defValueLength, ignoreEmpty) + 2;
            else if (item!=null)
                len = stringifier.estimateBufferSize(item, defValueLength);
            else
                len = defValueLength; // the default when null
            // adjust estimate
            if (i>0 && (len>0 || !ignoreEmpty))
                estimate += separatorLength;
            estimate += len;
        }
        return estimate; 
    }
    
    /**
     * Converts a list (Collection) of objects to a string.
     * 
     * @param list the collection of objects
     * @param template the list template or item separator
     * @param defItemValue the default item value
     * @param stringifier interface to convert an object to a string
     * @return returns a String or null if the list is null
     */
    public static String listToString(Collection<?> list, String template, String defItemValue, ObjectStringifier stringifier)
    {
        if (list==null || list.isEmpty())
            return null;
        // check 
        int tbeg = (template!=null ? template.indexOf(TEMPLATE_SEP_CHAR) : -1);
        if (list.size()>1 || tbeg>0)
        {   // build the list
            int tend =(tbeg>=0 ? template.lastIndexOf(TEMPLATE_SEP_CHAR) : -1);
            String separator = ((tbeg>0) ? (tend>tbeg ? template.substring(tbeg+1, tend) : DEFAULT_ITEM_SEPARATOR) : template);
            // create StringBuilder
            boolean ignoreEmpty = stringifier.isIgnoreEmpty(separator);
            int bufferLen = estimateListBufferSize(list, stringifier, length(separator), length(defItemValue), ignoreEmpty);
            if (template!=null && template.length()>separator.length())
            {   // template extra
                if (tbeg>0)
                    bufferLen += tbeg;
                if (tend>0)
                    bufferLen += (template.length()-tend-1); 
            }
            StringBuilder buf = new StringBuilder(bufferLen);
            if (tend>0)
                buf.append(template.substring(0, tbeg)); // add template prefix
            boolean isEmpty = true;
            boolean hasValue = false;
            for (Object item : list)
            {   // append separator
                if (hasValue && separator!=null)
                    buf.append(separator);
                // append value
                String value = stringifier.toString(item, template, defItemValue);
                hasValue = (value!=null && !(ignoreEmpty && StringUtils.isEmpty(value)));
                isEmpty &= !hasValue;
                if (hasValue)
                    buf.append(value);
            }
            if (hasValue==false && !isEmpty && separator!=null)
                buf.setLength(buf.length()-separator.length()); // remove last separator
            if (tend>0)
                buf.append(template.substring(tend+1)); // add template suffix
            if (buf.length()!=bufferLen)
                log.debug("estimateListBufferSize returned {} but string length is {}", bufferLen, buf.length());
            return buf.toString();
        }
        // Only one member
        String value = stringifier.toString(list.iterator().next(), template, defItemValue);
        if (stringifier.isIgnoreEmpty(template) && StringUtils.isEmpty(value))
            return defItemValue;
        return value;
    }
    
    /**
     * Converts a list (Collection) of objects to a string.
     * 
     * @param list the collection of objects
     * @param template the list template or item separator
     * @param defItemValue the default item value
     * @return returns a String
     */
    public static String listToString(Collection<?> list, String template, String defItemValue)
    {
        return listToString(list, template, defItemValue, defaultStringifier);
    }
    
    /**
     * Converts a list (Collection) of objects to a string.
     * 
     * @param list the collection of objects
     * @param separator the separator to put between the object strings
     * @return returns a String
     */
    public static String listToString(Collection<?> list, String separator)
    {
        return listToString(list, separator, EMPTY);
    }
    
    /**
     * Converts a list (Collection) of objects to a string.
     * 
     * @param list the collection of objects
     * @return returns a String
     */
    public static String listToString(Collection<?> list)
    {
        return listToString(list, DEFAULT_ITEM_SEPARATOR, EMPTY);
    }

    /**
     * Estimates the buffer size needed to convert a Collection into a String
     * @param list the list to estimate
     * @param stringifier interface to convert an object to a string
     * @param separatorLength the separator length
     * @param defValueLength the default length
     * @param ignoreEmpty flag whether to ignore empty parts
     * @return the estimated length of the collection parts
     */
    public static int estimateListBufferSize(Collection<?> list, ObjectStringifier stringifier, int separatorLength, int defValueLength, boolean ignoreEmpty)
    {
        boolean first = true;
        int estimate = 0;
        for (Object item : list)
        {
            int len = 0;
            if (item instanceof String) {
                // when ignoreEmpty check string for emptyness
                if (ignoreEmpty && StringUtils.isEmpty(((String)item)))
                    len=0;
                else
                    len = ((String)item).length();
            }
            else if (item instanceof Object[])
                len = estimateArrayBufferSize((Object[])item, stringifier, separatorLength, defValueLength, ignoreEmpty) + 2;
            else if (item instanceof Collection<?>)
                len = estimateListBufferSize((Collection<?>)item, stringifier, separatorLength, defValueLength, ignoreEmpty) + 2;
            else if (item!=null)
                len = stringifier.estimateBufferSize(item, defValueLength);
            else
                len = defValueLength; // the default when null
            // adjust estimate
            if (!first && (len>0 || !ignoreEmpty))
                estimate += separatorLength;
            estimate += len;
            first = false;
        }
        return estimate; 
    }

    /**
     * Assembles a string from parts with a separator char
     * 
     * @param separator the separator to put between the object strings
     * @param params array of objects
     * @return returns a String
     */
    public static String concatItems(char separator, Object... params)
    {
        return arrayToString(params, String.valueOf(separator));
    }

    /**
     * Assembles a string from several parts
     * 
     * @param parts the parts to concatenate
     * @return returns a String
     */
    public static String concat(String... parts)
    {
        int totalLength=0;
        for (int i=0; i<parts.length; i++)
            if (parts[i]!=null)
                totalLength+=parts[i].length();
        // concat now
        StringBuilder b = new StringBuilder(totalLength);
        for (int i=0; i<parts.length; i++)
            if (parts[i]!=null)
                b.append(parts[i]);
        return b.toString();
    }
    
    /**
     * Checks if a string is empty
     * 
     * @param s the String to check
     * 
     * @return true if s is empty or <code>null</code>
     */
    public static boolean isEmpty(String s)
    {
        if (s!=null)
        {   // check for any non-space character
            for (int i=0; i<s.length(); i++)
                if (s.charAt(i)>' ')
                    return false;
        }
        // empty
        return true;
    }
    
    /**
     * Checks if a string is not null or empty
     * 
     * @param s the string to validate
     * 
     * @return true if valid
     */
    public static boolean isNotEmpty(String s)
    {
        return !isEmpty(s);
    }
    
    /**
     * Compares two Strings with each other - either with or without character case. Both arguments may be null.
     * @param s1 the first String
     * @param s2 the second String
     * @param ignoreCase whether to ignore the character casing or not
     * @return true if the two strings supplied are equal 
     */
    public static boolean compareEqual(String s1, String s2, boolean ignoreCase)
    {
        // Empty
        if (isEmpty(s1))
            return isEmpty(s2);
        else if (isEmpty(s2))
            return false;
        // Compare 
        return (ignoreCase) ? s1.equalsIgnoreCase(s2) : s1.equals(s2);
    }
    
    /**
     * Compares two Strings with each other - either with or without character case. Both arguments may be null.
     * @param s1 the first String
     * @param s2 the second String
     * @return true if the two strings supplied are equal 
     */
    public static boolean compareEqual(String s1, String s2)
    {
        // Empty
        if (isEmpty(s1))
            return isEmpty(s2);
        else if (isEmpty(s2))
            return false;
        // Compare 
        return s1.equals(s2);
    }
    
    /**
     * Compares two Strings with each other - either with or without character case. Both arguments may be null.
     * @param s1 the first String
     * @param s2 the second String
     * @param ignoreCase whether to ignore the character casing or not
     * @return true if the two strings supplied are not equal 
     */
    public static boolean compareNotEqual(String s1, String s2, boolean ignoreCase)
    {
        return !compareEqual(s1, s2, ignoreCase);
    }
    
    /**
     * Compares two Strings with each other - either with or without character case. Both arguments may be null.
     * @param s1 the first String
     * @param s2 the second String
     * @return true if the two strings supplied are not equal 
     */
    public static boolean compareNotEqual(String s1, String s2)
    {
        return !compareEqual(s1, s2);
    }
    
    /**
     * Validates a given string. If the string is empty then null is returned. 
     * Otherwise the trimmed string is returned.
     * 
     * @param s the string to validate
     * @return the string or null if s was empty.
     */
    public static String validate(String s)
    {
        if (s==null)
            return null;
        s = s.trim();
        if (s.length()==0)
            return null;
        return s;
    }
    
    /**
     * Replaces all occurrences of find in source by replace.
     * 
     * @param source the original String.
     * @param find the String to be replaced
     * @param replace the replacement string
     * 
     * @return a new string with all occurrences of <code>find</code> in <code>source</code> replaced by <code>replace</code>
     */
    public static String replace(String source, String find, String replace)
    {
        // Check params
        if (source == null || find == null || find.length()==0)
            return source;
        // Find the character
        int index = source.indexOf(find);
        if (index < 0)
            return source;
        if (replace==null)
            replace="";
        // replace and find again
        int len = find.length();
        return concat(source.substring(0,index), replace, replace(source.substring(index+len), find, replace));
    }

    /**
     * Returns a String with all occurrences of <code>from</code> within <code>orig</code> replaced with <code>to</code>.
     * If <code>orig</code> contains no occurrences of <code>from</code>, or if <code>from</code> is equal to
     * <code>to</code>,<code>orig</code> itself is returned rather than a copy being made. If orig is <code>null</code>,
     * <code>null</code> is returned.
     * 
     * @param s the original String.
     * @param find the String to be replaced
     * @param replace the replacement string
     * 
     * @return a new string with all occurrences of <code>find</code> in <code>source</code> replaced by <code>replace</code>
     */
    public static String replaceAll(String s, String find, String replace)
    {
        if (s == null)
            return null;
        if (replace == null)
            replace = "";
        if (find == null || "".equals(find) || find.equals(replace))
        {   // Nothing to find
            return s;
        }
        int start = s.indexOf(find);
        if (start < 0) 
        {   // Nothing to replace
            return s;
        }    
        // Rebuild string
        StringBuilder b = new StringBuilder(s.length());
        char[] origChars = s.toCharArray();
        int findLength = find.length();
        int copyFrom = 0;
        while (start>= 0)
        {   // append part
            b.append(origChars, copyFrom, start - copyFrom);
            if (replace.length()>0)
                b.append(replace);
            copyFrom = start + findLength;
            start = s.indexOf(find, copyFrom);
        }
        // append the rest
        if (origChars.length > copyFrom)
            b.append(origChars, copyFrom, origChars.length - copyFrom);
        // done
        return b.toString();
    }
    
    /**
     * Removes all occurrences of remove from s 
     * @param s the source string
     * @param remove the string to remove
     * @return the result string
     */
    public static String remove(String s, String remove)
    {
        return replaceAll(s, remove, null);
    }
 
    /**
     * Removes all occurrences of c from s 
     * @param s the source string
     * @param c the character to remove
     * @return the result string
     */
    public static String remove(String s, char c)
    {
        return replaceAll(s, String.valueOf(c), null);
    }
 
    /**
     * removes all blanks from s
     * @param s the source string
     * @return the result string
     */
    public static String removeBlanks(String s)
    {
        return remove(s, " ");
    }

    /**
     * returns true if the character c is between the characters beg and end
     * @param c the source character
     * @param beg the lower end character
     * @param end the higher end character
     * @return true if the c is between beg and end, or false otherwise
     */
    public static boolean isCharBetween(char c, char beg, char end)
    {
        return (c>=beg && c<=end);
    }

    /**
     * returns true if the character c is a number digit ('0'-'9')
     * @param c the source character
     * @return true if the c is between 0 and 9
     */
    public static boolean isNumber(char c)
    {
        return (c>='0' && c<='9');
    }
 
    /**
     * returns true if the string s is a number (contains only the characters 0 to 9)
     * @param s the source string
     * @return true if s contains only the characters 0 to 9
     */
    public static boolean isNumber(String s)
    {   if (isEmpty(s))
            return false;
        // check all chars
        for (int i=0; i<s.length(); i++)
            if (!isNumber(s.charAt(i)))
                return false;
        return true;
    }

    /**
     * returns true if the character c is a numeric digit ('+' || '-' || '.' || ',' || '0'-'9')
     * @param c the source character
     * @param decimal flag to indicate whether the decimal and grouping separators ('.' || ',') are allowed
     * @return true if the c is a valid numeric character
     */
    public static boolean isNumeric(char c, boolean decimal)
    {   // is sign
        if (c=='+' || c=='-')
            return true;
        // is decimal char
        if (decimal && (c=='.' || c==','))
            return true;
        // number?
        return isNumber(c);
    }
 
    /**
     * returns true if the string s contains only numeric digits ('+' || '-' || '.' || ',' || '0'-'9')
     * @param s the source string
     * @param decimal flag to indicate whether the decimal and grouping separators ('.' || ',') are allowed
     * @return true if s contains only numeric digits
     */
    public static boolean isNumeric(String s, boolean decimal)
    {   if (isEmpty(s))
            return false;
        // check all chars
        for (int i=0; i<s.length(); i++)
        {
            char c = s.charAt(i);
            if ((c=='+' || c=='-') && i>0)
                return false;
            if (!isNumeric(c, decimal))
                return false;
        }    
        return true;
    }
 
    /**
     * returns true if the character c is an upper case character ('A'-'Z')
     * @param c the character
     * @return true if c is an upper case character 
     */
    public static boolean isUpper(char c)
    {
        return (c>='A' && c<='Z') || (c>='À' && c<='Ý');
    }
 
    /**
     * returns true if the first count characters of s are all upper case (or other non-case sensitive characters)
     * @param s the source string
     * @param count number of characters to check
     * @return true if the first count characters of s are all upper case
     */
    public static boolean isUpper(String s, int count)
    {   if (isEmpty(s))
            return false;
        if (count>s.length())
            count=s.length();
        for (int i=0; i<count; i++)
            if (isLower(s.charAt(i)))
                return false;
        return true;
    }
 
    /**
     * returns true if the character c is an upper case character ('a'-'z')
     * @param c the character
     * @return true if c is an upper case character 
     */
    public static boolean isLower(char c)
    {
        return (c>='a' && c<='z') || (c>='ß' && c<='ÿ');
    }
 
    /**
     * returns true if the first count characters of s are all lower case (or other non-case sensitive characters)
     * @param s the source string
     * @param count number of characters to check
     * @return true if the first count characters of s are all lower case
     */
    public static boolean isLower(String s, int count)
    {   if (isEmpty(s))
            return false;
        if (count>s.length())
            count=s.length();
        for (int i=0; i<count; i++)
            if (isUpper(s.charAt(i)))
                return false;
        return true;
    }
    
    /**
     * makes the first n characters of s upper case 
     * @param s the source string
     * @param count the number of characters to convert
     * @return the result string
     */
    public static String toUpper(String s, int count)
    {
        if (isEmpty(s))
            return s;
        if (s.length()<=count)
            return s.toUpperCase();
        // Upper
        String start = s.substring(0, count);
        return start.toUpperCase()+s.substring(count);
    }
    
    /**
     * makes the first n characters of s lower case 
     * @param s the source string
     * @param count the number of characters to convert
     * @return the result string
     */
    public static String toLower(String s, int count)
    {
        if (isEmpty(s))
            return s;
        if (s.length()<=count)
            return s.toLowerCase();
        // Lower
        String start = s.substring(0, count);
        return start.toLowerCase()+s.substring(count);
    }
    
    /**
     * truncates a string to a maximum number of chars 
     * @param s the source string
     * @param maxChar the maximum number of chars
     * @return the result string
     */
    public static String truncate(String s, int maxChar)
    {
        if (isEmpty(s))
            return StringUtils.EMPTY;
        if (maxChar<1 || s.length()<maxChar)
            return s;
        // trunc
        return s.substring(0, maxChar);
    }

    /**
     * Pads a String to the left
     * @param s the string to pad
     * @param size the desired size
     * @param padChar the padding char
     * @return the padded string
     */
    public static String padLeft(String s, int size, char padChar)
    {
        if (s==null)
            s=EMPTY;
        if (s.length()>=size)
            return s;
        // padding required
        int padCount = size-s.length(); 
        if (padCount==1)
            return String.valueOf(padChar)+s;
        // more than one
        StringBuilder b = new StringBuilder(size);
        while (b.length()<padCount) 
               b.append(padChar);
        b.append(s);
        return b.toString();
    }

    /**
     * Pads a String to the right
     * @param s the string to pad
     * @param size the desired size
     * @param padChar the padding char
     * @return the padded string
     */
    public static String padRight(String s, int size, char padChar)
    {
        if (s==null)
            s=EMPTY;
        if (s.length()>=size)
            return s;
        // padding required
        if (size-s.length()==1)
            return s+String.valueOf(padChar);
        // more than one
        StringBuilder b = new StringBuilder(size);
        b.append(s);
        while (b.length()<size) 
               b.append(padChar);
        return b.toString();
    }

    /**
     * Converts a String to camel case
     * Words must be separated by underscore
     * @param text the string to convert
     * @param firstCharUpper flag wether the first character should be upper case (true) or lower case (false)
     * @return the camel case string
     */
    public static String toCamelCase(String text, boolean firstCharUpper)
    {
        // remove spaces
        if (text.indexOf(' ')>=0)
            text = text.trim().replace(' ', '_');
        // begin
        StringBuilder b = new StringBuilder(text.length());
        int beg=0;
        while (beg<text.length())
        {
            int end = text.indexOf('_', beg);
            if (end<0)
                end = text.length();
            // assemble
            if (end>beg)
            {
                if (b.length()==0 && !firstCharUpper)
                {   // begin with all lower cases
                    b.append(text.substring(beg, end).toLowerCase());
                }
                else
                {   // add word where first letter is upper case 
                    b.append(text.substring(beg, beg+1).toUpperCase());
                    if (end-beg>1)
                    {
                        b.append(text.substring(beg+1, end).toLowerCase());
                    }
                }
            }
            // next
            beg = end + 1;
        }
        // Result
        return b.toString();
    }
    
}
