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
package org.apache.empire.data.list;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.apache.empire.commons.ClassUtils;
import org.apache.empire.data.ColumnExpr;
import org.apache.empire.data.RecordData;
import org.apache.empire.exceptions.InternalException;
import org.apache.empire.exceptions.InvalidArgumentException;
import org.apache.empire.exceptions.NotSupportedException;
import org.apache.empire.exceptions.UnsupportedTypeException;

/**
 * DataListFactoryImpl
 * Implements the DataListFactory
 * @author rainer
 */
public class DataListFactoryImpl<T extends DataListEntry> implements DataListFactory<T>
{
    /**
     * Finds a constructor for listEntryClass
     * @param listEntryClass the listEntryClass to instantiate
     * @param listHeadClass the DataListHead class
     * @return the constructor
     */
    @SuppressWarnings("unchecked")
    protected static <T extends DataListEntry> Constructor<T> findEntryConstructor(Class<?> listEntryClass, Class<? extends DataListHead> listHeadClass)
    {
        Constructor<?> constructor = ClassUtils.findMatchingConstructor(listEntryClass, -1, listHeadClass, int.class, Object[].class);
        if (constructor==null)
            throw new UnsupportedTypeException(listEntryClass);
        // found
        return (Constructor<T>)constructor;
    }

    protected final Constructor<T> constructor;
    protected final DataListHead head;

    /**
     * Constructs a DataListFactoryImpl based on a DateListEntry constructor and a DataListHead
     * @param constructor the DataListEntry constructor
     * @param head the listHead object
     */
    public DataListFactoryImpl(Constructor<T> constructor, DataListHead head) 
    {
        this.constructor = constructor;
        this.head = head;
    }
    
    /**
     * Constructs a DataListFactoryImpl based on a DateListEntry class and a DataListHead
     * @param listEntryClass the class of the DataListEntry
     * @param head the listHead object
     */
    public DataListFactoryImpl(Class<T> listEntryClass, DataListHead head) 
    {
        this(findEntryConstructor(listEntryClass, head.getClass()), head);
    }
    
    @Override
    public void prepareQuery(Object cmd, Object context)
    {
        /* Nothing */
    }

    @Override
    public List<T> newList(int capacity)
    {
        return new ArrayList<T>(capacity);
    }

    @Override
    public T newEntry(int rownum, RecordData dataRow)
    {   // check
        ColumnExpr[] columns = head.getColumns();
        if (columns.length!=dataRow.getFieldCount())
            throw new InvalidArgumentException("dataRow", dataRow);
        // copy values
        Object[] values = new Object[columns.length];
        for (int i=0; i<columns.length; i++)
            values[i] = dataRow.getValue(i);
        // create
        return newEntry(rownum, values);
    }

    protected T newEntry(int rownum, Object[] values)
    {   try
        {   // must override newEntry if no constructor is provided
            if (constructor==null)
                throw new NotSupportedException(this, "newEntry");
            // create item
            return constructor.newInstance(head, rownum, values);
        }
        catch (InstantiationException e)
        {
            throw new InternalException(e);
        }
        catch (IllegalAccessException e)
        {
            throw new InternalException(e);
        }
        catch (IllegalArgumentException e)
        {
            throw new InternalException(e);
        }
        catch (InvocationTargetException e)
        {
            throw new InternalException(e);
        }
    }
    
    @Override
    public void completeQuery(List<T> list)
    {
        /* Nothing */
    }

}
