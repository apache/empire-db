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
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.empire.data.DataType;
import org.apache.empire.exceptions.InvalidArgumentException;
import org.apache.empire.exceptions.NotSupportedException;
import org.apache.empire.xml.XMLUtil;
import org.w3c.dom.Element;


/**
 * This class represents a list of possible values that are valid for a particular field.<BR>
 * Additionally the class provides a text string describing the value for display purposes.<BR>
 * The class is implemented as a set of OptionEntry objects 
 * where the entry value is used as the key for the set and thus must be unique.<BR>
 */
public class Options extends AbstractSet<OptionEntry> implements Cloneable, Serializable
{
	private static final long serialVersionUID = 1L;
	
	@FunctionalInterface
	public interface OptionGroupResolver 
	{
	    Object getGroup(OptionEntry oe);
	}

    /**
     * Implements the Map interface for Options
     */
    private class ImmutableMap<T> implements Map<T,String>
    {
        @Override
        public int size()
        {
            return Options.this.size();
        }

        @Override
        public boolean isEmpty()
        {
            return Options.this.isEmpty();
        }

        @Override
        public boolean containsKey(Object key)
        {
            return contains(key);
        }
    
        @Override
        public boolean containsValue(Object value)
        {
            for (OptionEntry e : Options.this.list)
            {
                if (ObjectUtils.compareEqual(value, e.getText()))
                    return true;
            }
            return false;
        }

        @SuppressWarnings("unchecked")
        @Override
        public Set<T> keySet()
        {
            Set<T> keySet = new ArraySet<T>(this.size());
            for (OptionEntry e : list)
                keySet.add((T)e.getValue());
            return keySet;
        }
    
        @Override
        public Collection<String> values()
        {
            Collection<String> textValues = new ArrayList<String>(this.size());
            for (OptionEntry e : list)
                textValues.add(e.getText());
            return textValues;
        }

        @SuppressWarnings("unchecked")
        @Override
        public Set<Entry<T, String>> entrySet()
        {
            ArraySet<Entry<T, String>> set = new ArraySet<Entry<T, String>>(list.size());
            for (OptionEntry e : list)
                set.add(new ArrayMap.Entry<T,String>((T)e.getValue(), e.getText()));
            return set.immutable();
        }

        @Override
        public String get(Object key)
        {
            return Options.this.get(key);
        }

        @Override
        public String put(Object key, String value)
        {
            throw new NotSupportedException(this, "put");
        }
    
        @Override
        public void putAll(Map<? extends T, ? extends String> map)
        {
            throw new NotSupportedException(this, "putAll");
        }

        @Override
        public String remove(Object key)
        {
            throw new NotSupportedException(this, "remove");
        }

        @Override
        public void clear()
        {
            throw new NotSupportedException(this, "clear");
        }
        
        @Override
        public String toString()
        {
            return Options.this.toString();
        }
    }
    
    /**
     * InsertPos enum
     */
	public enum InsertPos
    {
        Top, Bottom, Sort
    }

    private final ArrayList<OptionEntry> list;
    
    private OptionGroupResolver optionGroupResolver;
    
    public Options()
    {   // Default constructor
        this.list = new ArrayList<OptionEntry>();
    }
    
    public Options(int initialCapacity)
    {   // Default constructor
        this.list = new ArrayList<OptionEntry>(initialCapacity);
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
            append(item, item.toString(), true);
        }
    }

    @Override
    public boolean addAll(Collection<? extends OptionEntry> source)
    {
        for (OptionEntry e : source)
            add((OptionEntry)e.clone());
        return true;
    }

    @Override
    public Options clone()
    {
        return new Options(this);
    }

    /**
     * Returns the function that determines the group to which an option entry belongs.
     * @return the group resolver function or null
     */
    public OptionGroupResolver getOptionGroupResolver()
    {
        return optionGroupResolver;
    }

    /**
     * Sets a function that determines the group to which an option entry belongs.
     * e.g.:
     * options.setOptionGroupResolver((oe) -> ((MyEnum)oe.getValue()).getCategory());
     * 
     * @param optionGroupResolver the group resolver function
     */
    public void setOptionGroupResolver(OptionGroupResolver optionGroupResolver)
    {
        this.optionGroupResolver = optionGroupResolver;
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
        return (oe!=null ? oe.getText() : StringUtils.EMPTY);
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
        return (i>=0 && i<list.size() ? list.get(i).getText() : StringUtils.EMPTY);
    }

    /**
     * Returns all values as a set
     * boolean activeOnly flag whether to return the active items only
     * @return the value set
     */
    public Set<Object> getValues(boolean activeOnly)
    {
        int count = (activeOnly ? getActiveCount() : list.size());
        ArraySet<Object> values = new ArraySet<Object>(count);
        for (OptionEntry e : list)
            if (activeOnly==false || e.isActive())
                values.add(e.getValue());
        return values;
    }
    
    /**
     * Returns all values as a set
     * @return the value set
     */
    public final Set<Object> getValues()
    {
        return getValues(false);
    }

    /**
     * Returns the number of active elements
     * @return the number of active elements
     */
    public int getActiveCount()
    {
        int count = 0;
        for (OptionEntry e : list)
            if (e.isActive())
                count++;
        return count;
    }
    
    /**
     * Returns the subset of active options 
     * @return the active options
     */
    public Options getActive()
    {
        Options o = new Options(getActiveCount());
        for (OptionEntry e : list)
            if (e.isActive())
                o.list.add(e);
        return o;
    }

    /**
     * Sets or Adds an option at a certain position
     * 
     * @param value the value object
     * @param text the text
     * @param active flag if element is active (selectable)
     * @param pos the position, see {@link InsertPos}
     */
    public void set(Object value, String text, Boolean active, InsertPos pos)
    {
        if (text == null)
            text = StringUtils.EMPTY;
        // Find Entry
        OptionEntry oe = getEntry(value);
        if (oe!=null)
        {   // replace
            oe.setText(text);
            if (active!=null)
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
            list.add(index, createOptionEntry(value, text, (active!=null ? active :true )));
        }
    }

    /**
     * Sets or Adds an option at a certain position
     * 
     * @param value the value object
     * @param text the text
     * @param pos the position, see {@link InsertPos}
     */
    public final void set(Object value, String text, InsertPos pos)
    {
        set(value, text, (Boolean)null, pos);
    }
    
    /**
     * Sets or adds Adds an option at the bottom
     * 
     * @param value the value object
     * @param text the text
     */
    public void set(Object value, String text, Boolean active)
    {
        if (text == null)
            text = StringUtils.EMPTY;
        // check if it exists
        OptionEntry oe = getEntry(value);
        if (oe!=null)
        {   // replace
            oe.setText(text);
            if (active!=null)
                oe.setActive(active);
        }
        else
        {   // add new Option
            list.add(createOptionEntry(value, text, (active!=null ? active :true )));
        }
    }
    
    /**
     * Sets or adds Adds an option at the bottom
     * 
     * @param value the value object
     * @param text the text
     */
    public final void set(Object value, String text)
    {
        set(value, text, (Boolean)null);
    }

    /**
     * Adds or updates an option
     * Same as set() but allows Option building 
     * @param value the value
     * @param text the text for this value
     * @param active flag if element is active (selectable)
     */
    public final Options add(Object value, String text, boolean active)
    {
        set(value, text, active);
        return this;
    }

    /**
     * Adds or updates an option
     * Same as set() but allows Option building 
     * @param value the value
     * @param text the text for this value
     */
    public final Options add(Object value, String text)
    {
        set(value, text, (Boolean)null);
        return this;
    }

    @Override
    public boolean add(OptionEntry option)
    {
        if (option==null || option.getText() == null)
            throw new InvalidArgumentException("option", option);
        // find and add or replace
        OptionEntry oe = getEntry(option.getValue());
        if (oe!=null)
            list.set(getIndex(oe), option);
        else
            list.add(option);
        return true;
    }
    
    /**
     * Appends an option
     * Useful for fast loading when it is certain that there are no duplicates
     * WARNING: Does not check if the entry already exists 
     * @param value the value object
     * @param text the text
     * @param active the flag whether or not this item is active
     */
    public void append(Object value, String text, boolean active)
    {
        if (text == null)
            text = StringUtils.EMPTY;
        list.add(createOptionEntry(value, text, active));
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

    /**
     * Checks if the Option list contains an empty value
     * @return true if it contains an empty value or false otherwise
     */
    public boolean containsNull()
    {   // Check if exits
        return (getEntry(StringUtils.EMPTY)!=null);
    }
    
    /**
     * same as contains(), but IDE may not issue warning
     * @param value the value to check
     * @return true if it exists in the options list
     */
    public boolean has(Object value)
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
    
    /**
     * Returns an immutable Map for the options 
     * @return the map of options
     */
    public Map<Object, String> map()
    {
        return map(Object.class);
    }
    
    /**
     * Returns an immutable Map for the options 
     * @return the map of options
     */
    public <T> Map<T, String> map(Class<T> type)
    {
        return new ImmutableMap<T>();
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
     * @param dataType the dataType of the element
     */
    public void addXml(Element element, DataType dataType)
    { 
        // add All Options
        for(OptionEntry e:list){
            Object value = e.getValue();
            if (value instanceof Enum<?>)
            {   // Take either the name or the ordinal
                value = ObjectUtils.getEnumValue((Enum<?>)value, dataType.isNumeric());
            }
            // Create Option Element
            Element opt = XMLUtil.addElement(element, "option", e.getText());
            opt.setAttribute("value", String.valueOf(value));
            if (e.isActive()==false)
                opt.setAttribute("disabled", "true");
        }
    }

    protected int findInsertPos(String text)
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
