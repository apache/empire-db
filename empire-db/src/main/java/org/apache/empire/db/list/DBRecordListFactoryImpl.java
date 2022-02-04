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
import org.apache.empire.exceptions.InvalidPropertyException;
import org.apache.empire.exceptions.UnsupportedTypeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DBRecordListFactoryImpl
 * Implements the DBRecordListFactory interface
 * @author rainer
 */
public class DBRecordListFactoryImpl<T extends DBRecord> implements DBRecordListFactory<T>
{
    // Logger
    protected static final Logger log = LoggerFactory.getLogger(DBRecordListFactoryImpl.class);
    
    /**
     * Finds a constructor for recordClass
     * @param recordClass the DBRecord class to instantiate
     * @param contextClass the context param
     * @param rowsetClass the rowset param
     * @return the constructor
     */
    @SuppressWarnings("unchecked")
    protected static <T extends DBRecord> Constructor<T> findRecordConstructor(Class<T> recordClass, Class<? extends DBContext> contextClass, Class<? extends DBRowSet> rowsetClass)
    {
        // try (context+rowset or just context)
        Constructor<?> constructor = ClassUtils.findMatchingAccessibleConstructor(recordClass, 1, contextClass, rowsetClass);
        if (constructor==null)
        {   // nothing suitable
            throw new UnsupportedTypeException(recordClass);
        }
        // found
        return (Constructor<T>)constructor;
    }
    
    /*
     * Members
     */
    protected final Constructor<T> constructor;
    protected final DBRowSet rowset;
    
    /**
     * Constructs a DBRecordListFactoryImpl based on an DBRecord constructor
     * @param constructor the DBRecord constructor
     * @param context the database context
     * @param rowset the rowset for the created records
     */
    public DBRecordListFactoryImpl(Constructor<T> constructor, DBRowSet rowset) 
    {
        this.constructor = constructor;
        this.rowset = rowset;
    }
    
    /**
     * Constructs a DBRecordListFactoryImpl based on an DBRecord class
     * @param recordClass the record class to be created for this list
     * @param context the database context
     * @param rowset the rowset for the created records
     */
    public DBRecordListFactoryImpl(Class<T> recordClass, Class<? extends DBContext> contextClass, DBRowSet rowset) 
    {
        this(findRecordConstructor(recordClass, contextClass, rowset.getClass()), rowset);
    }
    
    @Override
    public void prepareQuery(DBCommand cmd, DBContext context)
    {
        // complete select
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
    public T newRecord(int rownum, DBRecordData recData)
    {   try
        {   // create item
            T record;
            switch(constructor.getParameterCount())
            {
                case 2: record = constructor.newInstance(recData.getContext(), rowset);break;
                case 1: record = constructor.newInstance(recData.getContext());break;
                default:
                    throw new UnsupportedTypeException(constructor.getClass()); 
            }
            // check
            if (rowset.isSame(record.getRowSet())==false)
            {   // log warning
                log.warn("DBRecordListFactoryImpl rowset ({}) and actual record rowset ({}) don't match!", rowset.getName(), record.getRowSet().getName());
                throw new InvalidPropertyException("rowset", record.getRowSet());
            }
            rowset.initRecord(record, recData);
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
        /* Nothing */
    }
    
}
