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
import java.sql.Connection;
import java.util.ArrayList;

import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBCommand;
import org.apache.empire.db.DBContext;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.DBReader;
import org.apache.empire.db.DBRowSet;
import org.apache.empire.exceptions.BeanIncompatibleException;
import org.apache.empire.exceptions.InvalidArgumentException;
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
    private Class<T> clazz;

    /**
     * Create a bean result from a command object.
     * No checks will be performed here whether the command is compatible with the supplied class.
     * @param clazz
     * @param cmd
     */
    public BeanResult(Class<T> clazz, DBCommand cmd)
    {
        this.clazz = clazz;
        this.cmd = cmd;
        // Invalid Argument
        if (cmd==null || cmd.hasSelectExpr())
            throw new InvalidArgumentException("cmd", cmd);
    }
    
    /**
     * Creates a bean result for a Table, View or Query from the supplied columns.
     * At least one column must match the given getters / setters on the supplied class otherwise an BeanIncompatibleException will be thrown.
     * @param clazz the of T
     * @param rowset the rowset 
     */
    public BeanResult(Class<T> clazz, DBRowSet rowset)
    {
        this.clazz = clazz;
        // Create the command
        DBDatabase db = rowset.getDatabase();
        cmd = db.createCommand();
        // Select all accessible columns
        int count = 0;
        Method[] methods = clazz.getMethods();
        for (DBColumn col : rowset.getColumns())
        {   // obtain the bean property Name
            String property = col.getBeanPropertyName();
            if (!isPropertyAcessible(methods, property)) {
                // Property not found
                log.debug("Unable to access the property {} on {}. Column will be ignored.", property, clazz.getName());
                continue;
            }
            // Select
            cmd.select(col);
            count++;
        }    
        // Check
        if (count==0)
            throw new BeanIncompatibleException(clazz, rowset);
    }

    private boolean isPropertyAcessible(Method[] methods, String property)
    {
        property = "et"+property.substring(0,1).toUpperCase()+property.substring(1);
        for (int i=0; i<methods.length; i++)
        {   // Find a matching getter or setter method
            String name = methods[i].getName();
            if (name.endsWith(property))
                return true;
        }
        return false;
    }
    
    public DBCommand getCommand()
    {
        return cmd;
    }
    
    public int fetch(DBContext context, int maxItems)
    {
        clear();
        DBReader reader = new DBReader(context);
        try {
            // Open and Read
            reader.open(cmd);
            reader.getBeanList(this, clazz, maxItems);
            return size();
            
        } finally {
            reader.close();
        }
    }

    public final int fetch(DBContext context)
    {
        return fetch(context, -1);
    }
    
}
