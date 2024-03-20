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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import org.apache.empire.exceptions.NotSupportedException;

/**
 * This class is a lightweight Set<E> implementation using an ArrayList
 * @param <K>
 * @param <V>
 */
public class ArraySet<E> extends ArrayList<E> implements Set<E>
{
    private static final long serialVersionUID = 1L;
    
    @SuppressWarnings("hiding")
    private class ImmutableArraySet<E> implements Set<E>
    {
        @SuppressWarnings("unchecked")
        @Override
        public Object clone() 
        {
            return new ArraySet<E>((ArraySet<E>)ArraySet.this); 
        }
        
        @Override
        public int size()
        {
            return ArraySet.this.size();
        }

        @Override
        public boolean isEmpty()
        {
            return ArraySet.this.isEmpty();
        }

        @Override
        public boolean contains(Object o)
        {
            return ArraySet.this.contains(o);
        }

        @SuppressWarnings("unchecked")
        @Override
        public Iterator<E> iterator()
        {
            return (Iterator<E>) ArraySet.this.iterator();
        }

        @Override
        public Object[] toArray()
        {
            return ArraySet.this.toArray();
        }

        @Override
        public <T> T[] toArray(T[] a)
        {
            return ArraySet.this.toArray(a);
        }

        @Override
        public boolean containsAll(Collection<?> c)
        {
            return ArraySet.this.containsAll(c);
        }

        @Override
        public boolean add(E e)
        {
            throw new NotSupportedException(this, "set");
        }

        @Override
        public boolean addAll(Collection<? extends E> c)
        {
            throw new NotSupportedException(this, "addAll");
        }

        @Override
        public boolean retainAll(Collection<?> c)
        {
            throw new NotSupportedException(this, "retainAll");
        }

        @Override
        public boolean remove(Object o)
        {
            throw new NotSupportedException(this, "remove");
        }

        @Override
        public boolean removeAll(Collection<?> c)
        {
            throw new NotSupportedException(this, "removeAll");
        }

        @Override
        public void clear()
        {
            throw new NotSupportedException(this, "clear");
        }
    }
    
    /**
     * Default constructor 
     */
    public ArraySet()
    {
        super();
    }

    /**
     * Constructor with initialCapacity 
     */
    public ArraySet(int initialCapacity)
    {
        super(initialCapacity);
    }
    
    /**
     * Copy constructor
     * @param other
     */
    public ArraySet(ArraySet<E> other)
    {
        super(other.size());
        // copy
        for (E e : other)
            add(e);
    }
    
    public Set<E> immutable()
    {
        return new ImmutableArraySet<E>();
    }
    
    @Override
    public boolean add(E e)
    {
        if (this.contains(e))
            return false;
        return super.add(e);
    }

    @Override
    public void add(int index, E element)
    {
        this.remove(element);
        super.add(index, element);
    }
    
    /*
     * internal
     */
    
    protected void fastAdd(E element)
    {
        super.add(element);
    }
    
    protected void fastAdd(int index, E element)
    {
        super.add(index, element);
    }
}

