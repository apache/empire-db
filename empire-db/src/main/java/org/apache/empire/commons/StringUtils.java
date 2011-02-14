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

/**
 * This class contains common functions for comparing and converting values of type String. 
 * 
 */
public class StringUtils
{
    private StringUtils()
    {
        // Static Function only
        // No instance may be created
    }

    /**
     * Converts a value to a string.
     * If the value is null then the default value is returned.
     * 
     * @param value the value to convert
     * @param defValue default value which to return if value is null
     * @return returns a String representation of the value or null if value is null
     */
    public static String toString(Object value, String defValue)
    {
        return ((value!=null) ? value.toString() : defValue);
    }

    /**
     * Converts a value to a string.
     * If the value is null then null will be returned.
     * 
     * @param value the value to convert
     * @return returns a String representation of the value or null if value is null
     */
    public static String toString(Object value)
    {
        return toString(value, null);
    }

    /**
     * Converts an array of objects to a string.
     * 
     * @param array array of objects
     * @param defValue default value which to return if array is null
     * @return returns a String representation of the array or the defaultValue if array is null
     */
    public static String toString(Object[] array, String defValue)
    {
        String s = arrayToString(array, "/");
        return (s!=null ? s : defValue);
    }

    /**
     * Converts an array of objects to a string.
     * 
     * @param array array of objects
     * @return returns a String representation of the array or null if the array is null
     */
    public static String toString(Object[] array)
    {
        return toString(array, null);
    }

    /**
     * Converts a value to a string.
     * if the value is null an empty string is returned.
     * 
     * @param value the value to convert
     * @return returns a String representation of the Object or an empty stringif o is null
     */
    public static String valueOf(Object value)
    {
        return toString(value, "");
    }

    /**
     * Converts an objects to a string.
     * 
     * @param array array of objects
     * @return returns a String representation of the array or an empty String if the array is null
     */
    public static String valueOf(Object[] array)
    {
        return toString(array, "");
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
        return isValid(preferred) ? preferred : alternative;        
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
        return ((strval.length()==0) ? null : strval);   
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
        if (array == null || array.length < 1)
            return null; // Empty
        if (array.length > 1)
        { // multi Column Key
            StringBuilder buf = new StringBuilder();
            for (int i = 0; i < array.length; i++)
            {
                if (i > 0)
                    buf.append(separator);
                buf.append(array[i]);
            }
            return buf.toString();
        }
        // Only one member
        return String.valueOf(array[0]);
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
        return s == null || s.trim().length() == 0;
    }
    
    /**
     * Checks if a string is not null or empty
     * 
     * @param s the string to validate
     * 
     * @return true if valid
     * 
     * @deprecated this has been renamed to isNotEmpty
     */
    @Deprecated
    public static boolean isValid(String s)
    {
        return s != null && s.trim().length() > 0;
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
        return s != null && s.trim().length() > 0;
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
     * Replaces all occurences of find in source by replace.
     * 
     * @param source the original String.
     * @param find the String to be replaced
     * @param replace the replacement string
     * 
     * @return a new string with all occurances of <code>find</code> in <code>source</code> replaced by <code>replace</code>
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
        return source.substring(0,index)
             + replace
             + replace(source.substring(index+len), find, replace); 
    }

    /**
     * Returns a String with all occurrences of <code>from</code> within <code>orig</code> replaced with <code>to</code>.
     * If <code>orig</code> contains no occurrences of <code>from</code>, or if <code>from</code> is equal to
     * <code>to</code>,<code>orig</code> itself is returned rather than a copy being made. If orig is <code>null</code>,
     * <code>null</code> is returned.
     * 
     * @param source the original String.
     * @param find the String to be replaced
     * @param replace the replacement string
     * 
     * @return a new string with all occurances of <code>find</code> in <code>source</code> replaced by <code>replace</code>
     */
    public static String replaceAll(String source, String find, String replace)
    {
        if (source == null)
            return null;
        if (find == null || "".equals(find))
        {
            return source;
        }
        if (replace == null)
        {
            replace = "";
        }
        int fromLength = find.length();
        int start = source.indexOf(find);
        if (start == -1)
            return source;

        boolean greaterLength = (replace.length() >= fromLength);

        StringBuilder buffer;
        // If the "to" parameter is longer than (or
        // as long as) "from", the final length will
        // be at least as large
        if (greaterLength)
        {
            if (find.equals(replace))
                return source;
            buffer = new StringBuilder(source.length());
        } 
        else
        {
            buffer = new StringBuilder();
        }

        char[] origChars = source.toCharArray();

        int copyFrom = 0;
        while (start != -1)
        {
            buffer.append(origChars, copyFrom, start - copyFrom);
            buffer.append(replace);
            copyFrom = start + fromLength;
            start = source.indexOf(find, copyFrom);
        }
        buffer.append(origChars, copyFrom, origChars.length - copyFrom);

        return buffer.toString();
    }
    
}
