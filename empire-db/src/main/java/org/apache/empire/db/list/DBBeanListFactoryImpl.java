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
import org.apache.empire.db.DBColumnExpr;
import org.apache.empire.db.DBCommand;
import org.apache.empire.db.DBContext;
import org.apache.empire.db.DBRecordData;
import org.apache.empire.exceptions.InternalException;
import org.apache.empire.exceptions.InvalidArgumentException;

/**
 * DBRecordListFactoryImpl
 * Implements the DBRecordListFactory interface
 * @author rainer
 */
public class DBBeanListFactoryImpl<T> implements DBBeanListFactory<T>
{
    /**
     * Finds a suitable constructor for the beanClass
     * @param beanType the bean class to instantiate
     * @param params the constructor params
     * @return the constructor
     */
    @SuppressWarnings("unchecked")
    protected static <T> Constructor<T> findBeanConstructor(Class<T> beanType, List<? extends DBColumnExpr> params)
    {
        // find a suitable constructor
        Constructor<?> ctor = null;
        if (params!=null)
        {   // param type array
            Class<?>[] paramTypes = new Class[params.size()];
            for (int i=0; i<paramTypes.length; i++)
                paramTypes[i] = params.get(i).getJavaType(); 
            // find constructor
            ctor = ClassUtils.findMatchingAccessibleConstructor(beanType, paramTypes);
        }
        if (ctor==null)
        {   // find default constructor
            ctor = ClassUtils.findMatchingAccessibleConstructor(beanType, new Class<?>[] {});
        }    
        return (Constructor<T>)ctor;
    }
    
    /*
     * Members
     */
    protected final Constructor<T> constructor;
    protected final List<? extends DBColumnExpr> constructorParams;
    
    /**
     * Constructs a DBRecordListFactoryImpl based on an DBRecord constructor
     * @param constructor the DBRecord constructor
     * @param context the database context
     * @param rowset the rowset for the created records
     */
    public DBBeanListFactoryImpl(Constructor<T> constructor, List<? extends DBColumnExpr> constructorParams) 
    {
        this.constructor = constructor;
        this.constructorParams = constructorParams;
        // Check constructor
        if (constructor.getParameterCount()>0 && (constructorParams==null || constructor.getParameterCount()<constructorParams.size()))
            throw new InvalidArgumentException("constructor", constructor);
    }
    
    /**
     * Constructs a DBRecordListFactoryImpl based on an DBRecord class
     * @param recordClass the record class to be created for this list
     * @param context the database context
     * @param rowset the rowset for the created records
     */
    public DBBeanListFactoryImpl(Class<T> beanType, List<? extends DBColumnExpr> constructorParams) 
    {
        this(findBeanConstructor(beanType, constructorParams), constructorParams);
    }

    @Override
    public void prepareQuery(DBCommand cmd, DBContext context)
    {
        if (constructorParams==null)
            return;
        // check if constructor params are selected and add if appropriate
        for (DBColumnExpr expr : constructorParams)
        {
            if (cmd.hasSelectExpr(expr)==false)
                cmd.select(expr);
        }
    }
    
    @Override
    public List<T> newList(int capacity)
    {
        return new ArrayList<T>(capacity);        
    }
    
    @Override
    public T newItem(int rownum, DBRecordData dataRow)
    {   try
        {   T bean;
            if (constructorParams!=null && constructor.getParameterCount()>0)
            {   // Param constructor
                Object[] params = new Object[constructor.getParameterCount()];
                int i=0;
                for (DBColumnExpr expr : constructorParams)
                    params[i++] = dataRow.getValue(expr);
                // create item
                bean = constructor.newInstance(params);
                // set remaining properties
                if (params.length < dataRow.getFieldCount())
                    dataRow.setBeanProperties(bean, constructorParams);
            }
            else
            {   // Standard constructor
                bean = constructor.newInstance();
                // set the properties
                dataRow.setBeanProperties(bean);
            }
            return bean;
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
