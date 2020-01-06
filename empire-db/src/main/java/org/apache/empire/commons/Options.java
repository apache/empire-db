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
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.empire.exceptions.InvalidArgumentException;
import org.apache.empire.xml.XMLUtil;
import org.w3c.dom.Element;


/**
 * This class represents a list of possible values that are valid for a particular field.<BR>
 * Additionally the class provides a text string describing the value for display purposes.<BR>
 * The class is implemented as a set of OptionEntry objects 
 * where the entry value is used as the key for the set and thus must be unique.<BR>
 * <P> 
 */
public class Options extends AbstractSet<OptionEntry> implements Cloneable, Serializable
{
	private static final long serialVersionUID = 1L;

    private static final String EMPTY_STRING = "";

	public enum InsertPos
    {
        Top, Bottom, Sort
    }

    private final ArrayList<OptionEntry> list;
    
    public Options()
    {   // Default constructor
        this.list = new ArrayList<OptionEntry>();
    }
    
    public Options(Options other)
    {
        this.list = new ArrayList<OptionEntry>(other.size());
        this.addAll(other);
    }
    
    public Options(OptionEntry [] entries)
    {
        this.list = new ArrayList<OptionEntry>(entries.length);
        for (int i=0; i<entries.length; i++)
        {
            this.add(entries[i]);
        }
    }
    
    public Options(Class<?> enumType)
    {   // must be an enum
        if (enumType==null || !enumType.isEnum())
            throw new InvalidArgumentException("enumType", enumType);
        // create options from enum
        @SuppressWarnings("unchecked")
        Enum<?>[] items = ((Class<Enum<?>>)enumType).getEnumConstants();
        this.list = new ArrayList<OptionEntry>(items.length);
        for (int i=0; i<items.length; i++)
        {
            Enum<?> item = items[i];
            add(item, item.toString(), true);
        }
    }

    @Override
    public Options clone()
    {
        return new Options(this);
    }

    protected int getIndex(Object value)
    {
        if (value instanceof OptionEntry)
        {   // already an option entry
            int index = list.indexOf(value);
            if (index>=0)
                return index;
            // second try
            value = ((OptionEntry) value).getValue();
        }
        // find entry
        OptionEntry oe = getEntry(value);
        return (oe!=null ? list.indexOf(oe) : -1);
    }
    
    protected OptionEntry createOptionEntry(Object value, String text, boolean active)
    {
        return new OptionEntry(value, text, active);
    }

    public OptionEntry getEntry(Object value)
    {   // Find an Entry
        if (value instanceof Entry<?,?>)
            value = ((Entry<?,?>) value).getKey();
        if (value instanceof OptionEntry)
            value = ((OptionEntry) value).getValue();
        // Find it now
        for (OptionEntry oe : list)
        {   // Search List for Index
            if (oe.valueEquals(value))
                return oe;
        }
        return null;
    }

    public String get(Object value)
    {
        OptionEntry oe = getEntry(value);
        return (oe!=null ? oe.getText() : EMPTY_STRING);
    }

    public boolean isActive(Object value)
    {
        OptionEntry oe = getEntry(value);
        return (oe!=null ? oe.isActive() : false);
    }

    /**
     * Gets the value of the entry at index i
     * 
     * @param i the index
     * 
     * @return the value or <code>null</code> if not found 
     */
    public Object getValueAt(int i)
    {
        return (i>=0 && i<list.size() ? list.get(i).getValue() : null);
    }

    /**
     * Gets the text of the entry at index i
     * 
     * @param i the index
     * 
     * @return the text or an empty String if not found 
     */
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

    /**
     * Sets or Adds an option at a certain position
     * 
     * @param value the value object
     * @param text the text
     * @param pos the position, see {@link InsertPos}
     */
    public void set(Object value, String text, boolean active, InsertPos pos)
    {
        if (text == null)
        { // text must not be null!
            return;
        }
        // Find Entry
        OptionEntry oe = getEntry(value);
        if (oe!=null)
        {   // already present
            oe.setText(text);
            oe.setActive(active);
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
            list.add(index, createOptionEntry(value, text, active));
        }
    }

    /**
     * Sets or Adds an option at a certain position
     * 
     * @param value the value object
     * @param text the text
     * @param pos the position, see {@link InsertPos}
     */
    public void set(Object value, String text, InsertPos pos)
    {
        set(value, text, true, pos);
    }
    
    /**
     * Sets or adds Adds an option at the bottom
     * 
     * @param value the value object
     * @param text the text
     */
    public void set(Object value, String text)
    {
        set(value, text, InsertPos.Bottom);
    }

    /**
     * Adds an object, the check for an existing can be skipped for
     * performance issues (not recommended!)
     * 
     * @param value the value
     * @param text the text
     * @param noCheck set to true to skip testing for an existing key (handle with care!)
     */
    public void add(Object value, String text, boolean active)
    {
        OptionEntry oe = getEntry(value);
        if (oe!=null)
        {
            oe.setText(text);
            oe.setActive(active);
        }
        else
        {
            list.add(createOptionEntry(value, text, active));
        }
    }

    @Override
    public boolean add(OptionEntry option)
    {
        if (option.getText() == null)
        { // text must not be null!
            return false;
        }
        OptionEntry oe = getEntry(option.getValue());
        if (oe!=null)
            list.set(getIndex(oe), option);
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
    public boolean contains(Object value)
    {   // Check if exits
        return (getEntry(value)!=null);
    }

    public boolean containsNull()
    {   // Check if exits
        return (getEntry("")!=null);
    }
    
    /**
     * same as contains(), but IDE may not issue warning
     */
    public boolean exists(Object value)
    {   // Check if exits
        return (getEntry(value)!=null);
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
    public boolean remove(Object value)
    {
        // Check if exits
        OptionEntry oe = getEntry(value);
        if (oe==null)
            return false; // Element not found
        // remove
        list.remove(getIndex(oe));
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

    @Override
    public String toString()
    {
        StringBuilder b = new StringBuilder();
        b.append("{");
        boolean first = true;
        for (OptionEntry e : list)
        {
            b.append((first) ? "\"" : ",\r\n \"");
            b.append(StringUtils.toString(e.getValue()));
            b.append("\":\"");
            b.append(e.getText());
            b.append("\":");
            b.append(e.isActive() ? "1" : "0");
            first = false;
        }    
        b.append("}");
        return b.toString();
    }

    /**
     * Adds all these options to the xml element
     * 
     * @param element the element to add the option tags to
     * @param flags not used for now
     */
    public void addXml(Element element, long flags)
    { 
        // add All Options
        for(OptionEntry e:list){
            String value = String.valueOf(e.getValue());
            // Create Option Element
            Element opt = XMLUtil.addElement(element, "option", e.getText());
            opt.setAttribute("value", value);
            if (e.isActive()==false)
                opt.setAttribute("disabled", "true");
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
