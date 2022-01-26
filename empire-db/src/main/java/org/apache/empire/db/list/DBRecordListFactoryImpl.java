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
package org.apache.empire.db.list;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.apache.empire.commons.ClassUtils;
import org.apache.empire.db.DBCommand;
import org.apache.empire.db.DBContext;
import org.apache.empire.db.DBRecord;
import org.apache.empire.db.DBRecordData;
import org.apache.empire.db.DBRowSet;
import org.apache.empire.exceptions.InternalException;
import org.apache.empire.exceptions.UnsupportedTypeException;

/**
 * DBRecordListFactoryImpl
 * Implements the DBRecordListFactory interface
 * @author rainer
 */
public class DBRecordListFactoryImpl<T extends DBRecord> implements DBRecordListFactory<T>
{
    /**
     * Finds a constructor for recordClass
     * @param recordClass the DBRecord class to instantiate
     * @param contextClass the context param
     * @param rowsetClass the rowset param
     * @return the constructor
     */
    @SuppressWarnings("unchecked")
    protected static <T extends DBRecord> Constructor<T> findRecordConstructor(Class<T> recordClass, Class<? extends DBContext> contextClass, Class<? extends DBRowSet> rowsetClass)
    {   try
        {   // find the constructor
            return recordClass.getDeclaredConstructor(contextClass, rowsetClass);
        }
        catch (NoSuchMethodException | SecurityException e)
        {   // second try
            Constructor<?> constructor = ClassUtils.findMatchingAccessibleConstructor(recordClass, new Class<?>[] { contextClass, rowsetClass });
            if (constructor==null)
                throw new UnsupportedTypeException(recordClass);
            // found
            return (Constructor<T>)constructor;
        }
    }
    
    /*
     * Members
     */
    protected final Constructor<T> constructor;
    protected final DBContext context;
    protected final DBRowSet rowset;

    /**
     * Constructs a DBRecordListFactoryImpl based on an DBRecord constructor
     * @param constructor the DBRecord constructor
     * @param context the database context
     * @param rowset the rowset for the created records
     */
    public DBRecordListFactoryImpl(Constructor<T> constructor, DBContext context, DBRowSet rowset) 
    {
        this.constructor = constructor;
        this.context = context;
        this.rowset = rowset;
    }
    
    /**
     * Constructs a DBRecordListFactoryImpl based on an DBRecord class
     * @param recordClass the record class to be created for this list
     * @param context the database context
     * @param rowset the rowset for the created records
     */
    public DBRecordListFactoryImpl(Class<T> recordClass, DBContext context, DBRowSet rowset) 
    {
        this(findRecordConstructor(recordClass, DBContext.class, DBRowSet.class), context, rowset);
    }
    
    @Override
    public void prepareQuery(DBCommand cmd, DBContext context)
    {
        if (cmd.hasSelectExpr())
        {   // Already has select expressions. 
            // Check against Rowset
            return;
        }
        // otherwise select
        cmd.select(rowset.getColumns());
    }
    
    @Override
    public List<T> newList(int capacity)
    {
        return new ArrayList<T>(capacity);        
    }
    
    @Override
    public T newRecord(int rownum, DBRecordData dataRow)
    {   try
        {   // create item
            T record = constructor.newInstance(context, rowset);
            rowset.initRecord(record, dataRow);
            return record;
        }
        catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e)
        {
            throw new InternalException(e);
        }
    }
    
    @Override
    public void completeQuery(List<T> list)
    {
        
    }
    
}
