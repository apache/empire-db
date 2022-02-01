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
package org.apache.empire.data.bean;

import java.lang.reflect.Method;
import java.util.ArrayList;

import org.apache.empire.data.DataType;
import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBCommand;
import org.apache.empire.db.DBContext;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.DBObject;
import org.apache.empire.db.DBReader;
import org.apache.empire.db.DBRowSet;
import org.apache.empire.db.exceptions.CommandWithoutSelectException;
import org.apache.empire.exceptions.BeanIncompatibleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * BeanResult
 * This is a simple helper class that performs reading a list of beans from a query
 * Internally DBReader.getBeanList() is used.
 *  
 * @author doebele
 */
public class BeanResult<T> extends ArrayList<T>
{
    /**
     * Comment for <code>serialVersionUID</code>
     */
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(BeanResult.class);

    private DBCommand cmd;
    private Class<T> beanType;

    /**
     * Create a bean result from a command object.
     * No checks will be performed here whether the command is compatible with the supplied class.
     * @param beanType
     * @param cmd
     */
    public BeanResult(Class<T> beanType, DBCommand cmd)
    {
        DBObject.checkParamNull("beanType", beanType);
        DBObject.checkParamNull("cmd", cmd);
        this.beanType = beanType;
        this.cmd = cmd;
    }
    
    /**
     * Creates a bean result for a Table, View or Query from the supplied columns.
     * At least one column must match the given getters / setters on the supplied class otherwise an BeanIncompatibleException will be thrown.
     * @param beanType the of T
     * @param rowset the rowset 
     */
    public BeanResult(Class<T> beanType, DBRowSet rowset)
    {
        DBObject.checkParamNull("beanType", beanType);
        DBObject.checkParamNull("rowset", rowset);
        this.beanType = beanType;
        // Create the command
        DBDatabase db = rowset.getDatabase();
        cmd = db.createCommand();
        autoSelectColumns(rowset);
    }
    
    /**
     * Creates a bean result for a Table, View or Query from the supplied columns.
     * A rowset must be registered for this beanType @see DBRowSet.getRowsetforType()
     * At least one column must match the given getters / setters on the supplied class otherwise an BeanIncompatibleException will be thrown.
     * @param beanType the of T
     */
    public BeanResult(Class<T> beanType)
    {
        this(beanType, DBRowSet.getRowsetforType(beanType, true));
    }
    
    /**
     * Returns the current command 
     * Used to add constraints, order, grouping etc.
     * @return the command
     */
    public DBCommand getCommand()
    {
        return cmd;
    }
    
    /**
     * Executes the query and fetches the result
     * @param context
     * @param maxItems the maximum number of items to query
     * @return the number of items fetched by the query
     */
    public int fetch(DBContext context, int maxItems)
    {
        // Check command
        if (!cmd.hasSelectExpr())
            throw new CommandWithoutSelectException(cmd); 
        // OK, fetch now
        clear();
        DBReader reader = new DBReader(context);
        try {
            // Open and Read
            reader.open(cmd);
            reader.getBeanList(this, beanType, maxItems);
            return size();
            
        } finally {
            reader.close();
        }
    }

    /**
     * Executes the query and fetches the result
     * @param context
     * @return the number of items fetched by the query
     */
    public final int fetch(DBContext context)
    {
        return fetch(context, -1);
    }
    
    /**
     * Selects all columns for a given rowset
     * @param rowset
     */
    protected void autoSelectColumns(DBRowSet rowset)
    {
        // Select all accessible columns
        int count = 0;
        Method[] methods = beanType.getMethods();
        for (DBColumn col : rowset.getColumns())
        {   // obtain the bean property Name
            String property = col.getBeanPropertyName();
            if (!isPropertyAcessible(methods, property, col.getDataType())) {
                // Property not found
                log.info("Unable to access the property {} on {}. Column will be ignored.", property, beanType.getName());
                continue;
            }
            // Select
            cmd.select(col);
            count++;
        }    
        log.debug("{} columns have been selected for beanType {}", count, beanType.getName());
        // Check
        if (count==0)
            throw new BeanIncompatibleException(beanType, rowset);
    }

    /**
     * Checks if the property is accessible i.e. has a getter method on the beanType
     * @param methods the beanType methods
     * @param propety the property to check
     * @param dataType the dataType
     */
    protected boolean isPropertyAcessible(Method[] methods, String property, DataType dataType)
    {
        String prefix = (dataType.isBoolean() ? "is" : "et");
        String getter = prefix+property.substring(0,1).toUpperCase()+property.substring(1);
        for (int i=0; i<methods.length; i++)
        {   // Find a matching getter or setter method
            String name = methods[i].getName();
            if (name.endsWith(getter))
                return true;
        }
        return false;
    }
    
}
