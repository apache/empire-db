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
import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.data.Column;
import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBColumnExpr;
import org.apache.empire.db.DBCommand;
import org.apache.empire.db.DBCommandExpr;
import org.apache.empire.db.DBContext;
import org.apache.empire.db.DBRecordData;
import org.apache.empire.db.exceptions.CommandWithoutSelectException;
import org.apache.empire.exceptions.InternalException;
import org.apache.empire.exceptions.InvalidArgumentException;
import org.apache.empire.exceptions.UnsupportedTypeException;

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
    protected static <T> Constructor<T> findBeanConstructor(Class<T> beanType, List<? extends DBColumnExpr> params)
    {   // find a suitable constructor (but not the default constructor!)
        if (params==null || params.isEmpty())
            return null;
        // param type array
        Class<?>[] paramTypes = new Class[params.size()];
        for (int i=0; i<paramTypes.length; i++)
            paramTypes[i] = params.get(i).getJavaType(); 
        // find constructor
        return ClassUtils.findMatchingAccessibleConstructor(beanType, -1, paramTypes);
    }

    protected static <T> Constructor<T> findBeanConstructor(Class<T> beanType)
    {   // find default constructor
        return ClassUtils.findMatchingAccessibleConstructor(beanType, 0);
    }
    
    /*
     * Members
     */
    protected final Constructor<T> constructor;
    protected final List<? extends DBColumnExpr> constructorParams;
    protected final List<? extends DBColumnExpr> setterColumns;
    
    /**
     * Constructs a DBRecordListFactoryImpl based on an DBRecord constructor
     * @param constructor the constructor to be used to create the bean
     * @param constructorParams (optional) the columns to be used for the constructor. Must match the constructor!
     * @param setterColumns (optional) the columns to be set through setter methods. List may include constructorParams
     */
    public DBBeanListFactoryImpl(Constructor<T> constructor, List<? extends DBColumnExpr> constructorParams, List<? extends DBColumnExpr> setterColumns) 
    {
        this.constructor = constructor;
        this.constructorParams = constructorParams;
        this.setterColumns = setterColumns;
        // Check constructor
        if (constructor!=null && constructor.getParameterCount()>0 && (constructorParams==null || constructor.getParameterCount()<constructorParams.size()))
            throw new InvalidArgumentException("constructor||constructorParams", constructor);
    }
    
    /**
     * Constructs a DBRecordListFactoryImpl based on an DBRecord class
     * @param beanType the bean type to be instantiated
     * @param selectColumns (optional) the columns to be selected
     */
    public DBBeanListFactoryImpl(Class<T> beanType, List<? extends DBColumnExpr> selectColumns) 
    {
        Constructor<T> constructor = findBeanConstructor(beanType, selectColumns);
        if (constructor!=null)
        {   // construct with key columns
            this.constructorParams = selectColumns;
            this.setterColumns = null;
        }
        else
        {   // find default constructor
            constructor = findBeanConstructor(beanType);
            if (constructor==null)
                throw new UnsupportedTypeException(beanType);
            // use default constructor
            this.constructorParams = null;
            this.setterColumns = selectColumns;
        }
        this.constructor = constructor;
    }
    
    /**
     * Constructs a DBRecordListFactoryImpl based on an DBRecord class
     * @param beanType the bean type to be instantiated
     * @param keyColumns (optional) the columns to be used for the constructor
     * @param selectColumns (optional) the columns to be set through setter methods. List may include constructorParams
     */
    protected DBBeanListFactoryImpl(Class<T> beanType, List<? extends DBColumnExpr> keyColumns, List<? extends DBColumnExpr> selectColumns) 
    {
        Constructor<T> constructor = findBeanConstructor(beanType, selectColumns);
        if (constructor!=null)
        {   // construct with all columns
            this.constructorParams = selectColumns;
            this.setterColumns = null;
        }
        else if ((constructor = findBeanConstructor(beanType, keyColumns))!=null) 
        {   // construct with key columns
            this.constructorParams = keyColumns;
            this.setterColumns = selectColumns;
        }
        else
        {   // find default constructor
            constructor = findBeanConstructor(beanType);
            if (constructor==null)
                throw new UnsupportedTypeException(beanType);
            // use default constructor
            this.constructorParams = null;
            this.setterColumns = selectColumns;
        }
        // found one
        this.constructor = constructor;
    }
    
    /**
     * Constructs a DBRecordListFactoryImpl based on an DBRecord class
     * @param beanType the bean type to be instantiated
     * @param keyColumns (optional) the columns to be used for the constructor. Important: Must be a DBColumn array!
     * @param selectColumns (optional) the columns to be set through setter methods. List may include constructorParams
     */
    public DBBeanListFactoryImpl(Class<T> beanType, Column[] keyColumns, List<? extends DBColumnExpr> selectColumns) 
    {
        this(beanType, ObjectUtils.arrayToList(DBColumn.class, (DBColumn[])keyColumns), selectColumns);
    }

    @Override
    public void prepareQuery(DBCommandExpr cmd, DBContext context)
    {
        boolean hasSelect = cmd.hasSelectExpr();
        // check if constructor params are selected and add if appropriate
        if (constructorParams!=null)
        {   // select all columns which are not already selected
            for (DBColumnExpr expr : constructorParams)
            {
                if (cmd.hasSelectExpr(expr)==false)
                {
                    if (cmd instanceof DBCommand)
                        ((DBCommand)cmd).select(expr);
                    else
                        throw new InvalidArgumentException("cmd", cmd);
                }
            }
        }
        // check the rest of the columns, but only if no select is present
        if (setterColumns!=null && !hasSelect && (cmd instanceof DBCommand))
        {   // select all columns which are not already selected
            for (DBColumnExpr expr : setterColumns)
            {
                if (constructorParams!=null && constructorParams.contains(expr))
                    continue; // already added
                if (cmd.hasSelectExpr(expr)==false)
                    ((DBCommand)cmd).select(expr);
            }
        }
        // still no select ?
        if (!cmd.hasSelectExpr())
            throw new CommandWithoutSelectException(cmd); 
    }
    
    @Override
    public List<T> newList(int capacity)
    {
        return new ArrayList<T>(capacity);        
    }
    
    @Override
    public T newItem(int rownum, DBRecordData recData)
    {   try
        {   T bean;
            if (constructorParams!=null && constructor.getParameterCount()>0)
            {   // Param constructor
                Object[] params = new Object[constructor.getParameterCount()];
                int i=0;
                for (DBColumnExpr expr : constructorParams)
                    params[i++] = recData.getValue(expr);
                // create item
                bean = constructor.newInstance(params);
                // set remaining properties
                if (params.length < recData.getFieldCount())
                    recData.setBeanProperties(bean, constructorParams);
            }
            else
            {   // Standard constructor
                bean = constructor.newInstance();
                // set the properties
                recData.setBeanProperties(bean);
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
        /* Nothing */
    }
    
}
