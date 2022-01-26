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
     * Empty String
     */
    public static final String EMPTY = "";

    /**
     * Single Space
     */
    public static final String SPACE = " ";

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
        return toString(value, "null");
    }

    /**
     * Converts an objects to a string.
     * 
     * @param array array of objects
     * @return returns a String representation of the array or an empty String if the array is null
     */
    public static String valueOf(Object[] array)
    {
        return toString(array, "null");
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
        return isNotEmpty(preferred) ? preferred : alternative;        
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
                if (i>0 && separator!=null)
                    buf.append(separator);
                buf.append(array[i]);
            }
            return buf.toString();
        }
        // Only one member
        return String.valueOf(array[0]);
    }

    /**
     * Converts an array of objects to a string.
     * 
     * @param array array of objects
     * @param separator the separator to put between the object strings
     * @return returns a String
     */
    public static String concat(String separator, Object... params)
    {
        return arrayToString(params, separator);
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
        {   // find non-space character
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
        if (s1==null || s2==null)
            return (s1==s2);
        // Empty
        if (isEmpty(s1) && isEmpty(s2))
            return true;
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
        if (s1==s2)
            return true;
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
     * @param the maximum number of chars
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
    
}
