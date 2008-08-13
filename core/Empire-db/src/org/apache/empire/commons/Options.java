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

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.empire.xml.XMLUtil;
import org.w3c.dom.Element;


/**
 * This class represents a list of possible values that are valid for a particular field.<BR>
 * Additionally the class provides a text string describing the value for display purposes.<BR>
 * The class is implemented as a set of OptionEntry objects 
 * where the entry value is used as the key for the set and thus must be unique.<BR>
 * <P> 
 * @author ESTEAM software <A TARGET="esteam" HREF="http://www.esteam.de">www.esteam.de </A>
 */
@SuppressWarnings("serial")
public class Options extends AbstractSet<OptionEntry>
{
    public enum InsertPos
    {
        Top, Bottom, Sort
    }

    private static final String EMPTY_STRING = "";

    private ArrayList<OptionEntry> list = new ArrayList<OptionEntry>();
    
    public Options()
    {
        // Default constructor
    }
    
    public Options(Options other)
    {
        this.addAll(other);
    }
    
    public Options(OptionEntry [] entries)
    {
        for (int i=0; i<entries.length; i++)
            this.add(entries[i]);
    }

    protected int getIndex(Object value)
    {
        // Find an Entry
        if (value instanceof Entry)
            value = ((Entry) value).getKey();
        if (value instanceof OptionEntry)
            value = ((OptionEntry) value).getValue();
        // Find it now
        int size = list.size();
        for (int i = 0; i < size; i++)
        { // Search List for Index
            Object v = list.get(i).getValue();
            if (ObjectUtils.compareEqual(value, v))
                return i;
        }
        return -1;
    }
    
    protected OptionEntry createOptionEntry(Object value, String text)
    {
        return new OptionEntry(value, text);
    }

    public OptionEntry getEntry(Object value)
    {
        int i = getIndex(value);
        return (i >= 0 ? list.get(i) : null);
    }

    public String get(Object value)
    {
        int i = getIndex(value);
        return (i >= 0 ? list.get(i).getText() : EMPTY_STRING);
    }

    public Object getValueAt(int i)
    {
        return (i>=0 && i<list.size() ? list.get(i).getValue() : null);
    }

    public String getTextAt(int i)
    {
        return (i>=0 && i<list.size() ? list.get(i).getText() : EMPTY_STRING);
    }

    public Set<Object> getValues()
    {
        HashSet<Object> set = new HashSet<Object>(list.size());
        for (OptionEntry e : list)
            set.add(e.getValue());
        return set;
    }

    public void set(Object value, String text, InsertPos pos)
    {
        if (text == null)
        { // text must not be null!
            return;
        }
        // Find Index
        int i = getIndex(value);
        if (i >= 0)
        { // already present
            list.get(i).setText(text);
        } 
        else
        {   // find insert pos
            int index;
            if (pos == InsertPos.Top)
                index = 0;
            else if (pos == InsertPos.Sort)
                index = findInsertPos(text);
            else // bottom is default
                index = list.size();
            // add entry now
            list.add(index, createOptionEntry(value, text));
        }
    }

    public void set(Object value, String text)
    {
        set(value, text, InsertPos.Bottom);
    }

    public void add(Object value, String text, boolean noCheck)
    {
        if (noCheck)
        { // fast add to list, without check for existing key
            // handle with care!
            list.add(createOptionEntry(value, text));
        } 
        else
            set(value, text);
    }

    @Override
    public boolean add(OptionEntry option)
    {
        if (option.getText() == null)
        { // text must not be null!
            return false;
        }
        int i = getIndex(option.getValue());
        if (i >= 0)
            list.set(i, option);
        else
            list.add(option);
        return true;
    }

    @Override
    public void clear()
    {
        list.clear();
    }

    @Override
    public boolean contains(Object object)
    {
        // Check if exits
        return (getIndex(object) >= 0);
    }

    public boolean containsValue(Object object)
    {
        // Check if exits
        return (getIndex(object) >= 0);
    }

    @Override
    public boolean isEmpty()
    {
        return (list.size() == 0);
    }

    @Override
    public Iterator<OptionEntry> iterator()
    {
        return list.iterator();
    }

    @Override
    public boolean remove(Object object)
    {
        // Check if exits
        int i = getIndex(object);
        if (i < 0)
            return false; // Element not found
        // remove
        list.remove(i);
        return true;
    }

    @Override
    public int size()
    {
        return list.size();
    }

    @Override
    public Object[] toArray()
    {
        return list.toArray();
    }

    public void addXml(Element element, long flags)
    { // add All Options
        Iterator<OptionEntry> i = iterator();
        while (i.hasNext())
        {
            OptionEntry e = i.next();
            String value = String.valueOf(e.getValue());
            // Create Option Element
            Element opt = XMLUtil.addElement(element, "option", e.getText());
            opt.setAttribute("value", value);
        }
    }

    private int findInsertPos(String text)
    {
        int i = 0;
        for (; i < list.size(); i++)
        {
            OptionEntry e = list.get(i);
            if (text.compareToIgnoreCase(e.getText()) <= 0)
                break;
        }
        return i;
    }
}
