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
package org.apache.empire.jsf2.utils;

import org.apache.empire.commons.StringUtils;
import org.apache.empire.exceptions.InvalidArgumentException;
import org.apache.empire.exceptions.InvalidOperationException;
import org.apache.empire.exceptions.NotSupportedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * StyleClass
 * Represents the "class" attribute of a html tag, and thus may contain none, one or many style class names.
 * This class acts as a builder and is thus mutable. 
 * @author rainer
 */
public final class StyleClass
{
    private static final Logger log = LoggerFactory.getLogger(StyleClass.class);
    
    private static int MAX_STYLE_CLASSES = 10;
    
    public static void setMaxStyleClasses(int max)
    {
        MAX_STYLE_CLASSES = max;
    }
    
    /*
     * properties
     */
    private final String[] parts;
    private short count = 0;
    
    // private static int[] counts = new int[12]; 
    
    private static final char SPACE = ' ';
    
    public StyleClass()
    {
        parts = new String[MAX_STYLE_CLASSES]; // initial maximum!        
    }
    
    public StyleClass(Object initial)
    {
        this();
        add(initial);
    }
    
    public StyleClass(Object initial, boolean immutable)
    {
        if (immutable)
        {   // immutable
            parts= new String[] { initial.toString() };
            count=1;
        }
        else
        {   // mutable
            parts = new String[12]; // initial maximum!
            add(initial);
        }
    }
    
    public boolean isEmpty()
    {
        return (prepareBuild()<=0);
    }
    
    public boolean isNotEmpty()
    {
        return (prepareBuild()>0);
    }
    
    public StyleClass add(String styleClass)
    {
        if (styleClass==null || styleClass.length()==0)
            return this;
        // trim spaces
        if (styleClass.charAt(0)==SPACE)
            styleClass=styleClass.trim();
        // split mulit
        if (styleClass.indexOf(SPACE)>0)
        {   // add all parts
            int i=0;
            int n=0;
            while((n=styleClass.indexOf(' ', i))>0)
            {
                add(styleClass.substring(i, n));
                i=n+1;
            }
            if (i<styleClass.length())
                add(styleClass.substring(i));
            // done
            return this;
        }
        // no dublicates
        for (int i=0; i<count; i++)
            if (parts[i].equals(styleClass))
                return this;
        // Check capacity
        if (count==parts.length)
        {   // check type
            if (parts.length==1)
                throw new NotSupportedException(this, "add"); // immutable style class
            else
                throw new InvalidOperationException("Too many style classes: "+build());
        }
        // add now
        parts[count++]=styleClass;
        return this;
    }

    public StyleClass add(String... parts)
    {
        for (int i=0; i<parts.length; i++)
            add(parts[i]);
        return this;
    }

    public StyleClass add(Object styleClass)
    {
        if (styleClass==null)
            return this;
        // convert to string and add
        return add(styleClass.toString());
    }
    
    public StyleClass add(boolean condition, Object styleClass)
    {
        if (condition)
            return add(styleClass);
        else
            return this;
    }

    public StyleClass remove(String styleClass)
    {
        if (styleClass==null || styleClass.length()==0)
            return this;
        // sinlge item only
        if (styleClass.indexOf(SPACE)>=0)
            throw new InvalidArgumentException("styleClass", styleClass);
        // find
        for (int i=0; i<count; i++)
            if (parts[i].equals(styleClass))
            {   // found: remove item
                remove(i);
                break;
            }
        return this;
    }
    
    public String build()
    {
        // prepare
        int length = prepareBuild();
        if (length<=0)
            return StringUtils.EMPTY;
        // shortcut
        if (count==1)
            return parts[0]; // only one!
        // build now
        StringBuilder b = new StringBuilder(length);
        for (int i=0; i<count; i++)
        {   String s = parts[i];
            // ignore
            if (s.charAt(0)=='-')
                continue;
            // separator
            if (b.length()>0)
                b.append(SPACE);
            // append
            b.append(parts[i]);
        }
        if (b.length()!=length)
            log.warn("Length estimate ({}) was wrong: {}", length, b.length());
        return b.toString();
    }
    
    @Override
    public String toString()
    {
        return build();
    }
    
    /*
     * private
     */
    
    private boolean contains(String[] list, short count, String s)
    {
        for (int i=0; i<count; i++)
            if (list[i].equals(s))
                return true;
        return false;
    }
    
    private int prepareBuild()
    {
        String[] remove = null;
        short removeCount = 0;
        for (int i=0; i<count; i++)
        {
            if (parts[i].charAt(0)!='-')
                continue;
            // add remove
            if (remove== null)
                remove = new String[4];
            remove[removeCount++]=parts[i].substring(1);
        }
        // count total length
        int length=-1;
        for (int i=0; i<count; i++)
        {
            String s = parts[i];
            // ignore
            if (s.charAt(0)=='-')
                continue;
            // check remove
            if (contains(remove, removeCount, s))
            {   // remove this one
                remove(i--);
                continue;
            }
            length+= s.length() + 1; // +1 for separator
        }
        return (length>0 ? length : 0);
    }
    
    private void remove(int index)
    {
        if (index<0 || index>=count)
            return;
        // remove index
        count--;
        for (; index<count; index++)
            parts[index]=parts[index+1];
        parts[count]=null; // the last one
    }
    
    /*
     * Static String operations
     */

    public static final boolean existsIn(String styleClasses, String styleClassName)
    {   // find
        return (find(styleClasses, styleClassName, 0)>=0);
    }

    public static final String addTo(String styleClasses, String styleClassName)
    {
        // check 
        if (styleClasses==null || styleClasses.length()==0)
            return styleClassName;
        if (existsIn(styleClasses, styleClassName))
            return styleClasses;
        // add with space
        return StringUtils.concat(styleClasses, " ", styleClassName);
    }
    
    public static final String removeFrom(String styleClasses, String styleClassName)
    {
        // Check name
        if (styleClassName==null)
            return styleClasses;
        // find
        int idx = find(styleClasses, styleClassName, 0);
        if (idx<0)
            return styleClasses; // not contained
        // remove now
        if (idx<=1)
        {   // remove from start
            idx += styleClassName.length();
            if (styleClasses.length()>idx && styleClasses.charAt(idx)==SPACE)
                idx++;
            return styleClasses.substring(idx);
        }
        if (idx+styleClassName.length()==styleClasses.length())
        {   // remove from end
            return styleClasses.substring(0, idx-1); // at the end
        }
        // in between
        int after  = idx + styleClassName.length();
        int before = idx - 1; // SPACE assumed!
        return StringUtils.concat(styleClasses.substring(0, before), styleClasses.substring(after));
    }

    public final static String addOrRemove(String styleClasses, String styleClassName, boolean add)
    {
        // add or remove
        if (add)
            return StyleClass.addTo(styleClasses, styleClassName);
        else
            return StyleClass.removeFrom(styleClasses, styleClassName);
    }
    
    private static final int find(String styleClasses, String styleClassName, int fromIdx)
    {
        if (styleClasses==null)
            return -1;
        // find
        int idx = styleClasses.indexOf(styleClassName, fromIdx);
        if (idx<0)
            return -1;
        // starts with space?
        if (idx>0 && styleClasses.charAt(idx-1)!=SPACE)
            return find(styleClasses, styleClassName, idx+1); // recurse
        // ends with space?
        int end = idx+styleClassName.length(); 
        if (end<styleClasses.length() && styleClasses.charAt(end)!=SPACE)
            return find(styleClasses, styleClassName, idx+1); // recurse
        // found
        return idx;
    }
   
}
