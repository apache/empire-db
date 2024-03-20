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

import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;

import org.apache.empire.exceptions.InvalidArgumentException;

/**
 * This class is a lightweight Map<K,V> implementation using an ArrayList
 * @param <K>
 * @param <V>
 */
public class ArrayMap<K,V> extends AbstractMap<K,V>
{    
    /**
     * The Entry class represents a map entry
     * @param <K>
     * @param <V>
     */
    public static class Entry<K,V> implements Map.Entry<K, V>
    {
        private final K key;
        private V value;
        
        public Entry(K key, V value)
        {
            this.key = key;
            this.value = value;
        }
        
        @Override
        public K getKey()
        {
            return key;
        }

        @Override
        public V getValue()
        {
            return value;
        }

        @Override
        public V setValue(V value)
        {
            V prev = this.value;
            this.value = value;
            return prev;
        }
        
        @Override
        public boolean equals(Object obj)
        {
            if (super.equals(obj))
                return true;
            // compare
            if (obj instanceof Entry<?,?>)
            {   // Key and value must match
                Entry<?,?> other = ((Entry<?,?>)obj);
                return ObjectUtils.compareEqual(key, other.getKey())
                    && ObjectUtils.compareEqual(value, other.getValue());
            }
            return false;
        }
        
        @Override
        public String toString()
        {
            String k = (key instanceof Enum<?>) ? ((Enum<?>)key).name() : String.valueOf(key);
            String v = String.valueOf(value);
            StringBuilder b = new StringBuilder(k.length()+v.length()+4);
            b.append("{");
            b.append(k);
            b.append("=");
            b.append(v);
            b.append("}");
            return b.toString();
        }
    }
    
    private ArraySet<Map.Entry<K, V>> entries;
    
    /**
     * Default constructor 
     */
    public ArrayMap()
    {
        this.entries = new ArraySet<Map.Entry<K, V>>();
    }

    /**
     * Constructor with initialCapacity 
     */
    public ArrayMap(int initialCapacity)
    {
        this.entries = new ArraySet<Map.Entry<K, V>>(initialCapacity);
    }
    
    /**
     * Copy Constructor
     * @param other
     */
    public ArrayMap(final Map<K,V> other)
    {
        this.entries = new ArraySet<Map.Entry<K, V>>(other.size());
        for (Map.Entry<K,V> e : other.entrySet())
            this.entries.fastAdd(new Entry<K, V>(e.getKey(), e.getValue()));
    }
    
    /**
     * Builder method to build an ArrayMap
     * @param key
     * @param value
     * @return the ArrayMap
     */
    public ArrayMap<K,V> append(K key, V value)
    {
        put(key, value);
        return this;
    }
    
    @Override
    public Set<Map.Entry<K, V>> entrySet()
    {
        return entries;
    }

    @Override
    public V remove(Object key) 
    {
        @SuppressWarnings("unchecked")
        int index = getIndex((K)key);
        if (index<0)
            return null; // not found
        // found
        Map.Entry<K, V> entry = entries.get(index);
        entries.remove(index);
        return entry.getValue();
    }
    
    @Override
    public V put(K key, V value)
    {
        Map.Entry<K, V> entry = getEntry(key);
        if (entry!=null)
            return entry.setValue(value);
        // not found, so add
        entries.fastAdd(new Entry<K, V>(key, value));
        return null;
    }
    
    public void add(K key, V value)
    {
        this.put(key, value);
    }
    
    public void add(K key, V value, int index)
    {
        if (index<0)
        {   // add to end
            this.put(key, value);
            return;
        }
        // index
        if (index>entries.size())
            throw new InvalidArgumentException("index", index);
        // exists
        int current = getIndex(key);
        if (current==index)
        {   // correct position
            Map.Entry<K, V> entry = entries.get(index);
            entry.setValue(value);
            return;
        }
        if (current>=0)
        {   // remove at this position
            entries.remove(current);
            if (current<index)
                index--;
        }
        entries.fastAdd(index, new Entry<K, V>(key, value));
    }
    
    public int getIndex(K key)
    {
        for (int index=0; index<entries.size(); index++)
        {
            Map.Entry<K, V> entry = entries.get(index);
            if (ObjectUtils.compareEqual(entry.getKey(), key))
                return index;
        }
        return -1;
    }
    
    public Map.Entry<K, V> getEntry(K key)
    {
        int index = getIndex(key);
        return (index>=0 ? entries.get(index) : null);
    }

    
}
