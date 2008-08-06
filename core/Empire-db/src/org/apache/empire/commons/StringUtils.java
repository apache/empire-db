/*
 * ESTEAM Software GmbH
 */
package org.apache.empire.commons;

import java.util.Collection;
import java.util.Iterator;

/**
 * This class contains common functions for comparing and converting values of type String. 
 * 
 * @author ESTEAM software <A TARGET="esteam" HREF="http://www.esteam.de">www.esteam.de </A>
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
        return array[0].toString();
    }

    /**
     * Converts a string to an array of objects.
     * 
     * @param string the source string to parse
     * @param separator the separator string by which the parts are separated
     * @return returns a String
     */
    public static String[] stringToArray(String string, String separator)
    {
        if (string == null || isEmpty(separator))
            return null; // Empty
        // Count the items first
        int sepLength = separator.length();
        int count = 0;
        int pos = -1;
        while ((pos=string.indexOf(separator, pos+sepLength))>=0)
                count++;
        // Alloc an array
        String[] array = new String[count+1];
        if (count>0)
        {
            int beg = 0;
            for (int i=0; true; i++)
            {
                int end = string.indexOf(separator, beg);
                if (end>beg)
                {   // Add Item
                    array[i] = string.substring(beg, end);
                    beg = end + sepLength;
                }
                else
                {   // Last Item
                    array[i] = string.substring(beg);
                    break;
                }
            }
        }
        else
        {
            array[0] = string;
        }
        // Only one member
        return array;
    }

    /**
     * Converts an collection of objects to a string.
     * 
     * @param c the collection to add
     * @return returns a String
     */
    public static String collectionToString(Collection<? extends Object> c, String separator)
    {
        if (c == null || c.size()==0)
            return null; // Empty
        // Iterator
        StringBuilder buf = new StringBuilder();
        boolean addSep = false;
        Iterator i = c.iterator();
        while (i.hasNext())
        {
            if (addSep)
                buf.append(separator);
            buf.append(valueOf(i.next()));
            addSep = true;
        }
        return buf.toString();
    }
    
    public static boolean isEmpty(String s)
    {
        return (s==null || s.trim().length()==0);
    }
    
    public static boolean isValid(String s)
    {
        return (s!=null && s.trim().length()>0);
    }

    public static boolean isEmail(String s)
    {
        int indexOfAtChar = s.indexOf("@");
        if (indexOfAtChar > 0)
        {
            int indexOfDotChar = s.indexOf(".", indexOfAtChar);
            if (indexOfDotChar > 0)
            {
                return true;
            }
            return false;
        }
        return false;
    }
    
    /**
     * Validates a given string. If the string is empty then null is returned. 
     * Otherwise the trimmed string is returned. 
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
     * Replaces all occances of first character in a string by a string.
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
        if (find == null)
        {
            find = "";
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

    public static String replaceBRbyLF(String s)
    {
        return replaceAll(replaceAll(s, "<br/>", "\n\n"), "<br />", "\n\n");
    }

    public static String trimAll(String orig)
    {
        if (orig == null)
            return null;
        String str = orig.trim();
        StringBuilder strBuf = new StringBuilder(str.length());
        boolean hasSpace = false;
        for (int i = 0, j = 0; i < str.length(); i++)
        {
            if (str.charAt(i) == ' ')
            {
                if (!hasSpace)
                {
                    strBuf.append(' ');
                    hasSpace = true;
                }
            } 
            else
            {
                j = str.indexOf(' ', i);
                if (j == -1)
                    j = str.length();
                strBuf.append(str.substring(i, j));
                hasSpace = false;
                i = j - 1;
            }
        }

        return strBuf.toString();
    }
}
