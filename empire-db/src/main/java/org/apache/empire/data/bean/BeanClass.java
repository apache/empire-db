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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.empire.data.Column;
import org.apache.empire.data.DataType;
import org.apache.empire.data.DataMode;


/**
 * BeanObject
 * This class defines Metadata for any type of java class.
 * For each class you want to describe create one Metadata class and derive it from BeanClass.
 * A metadata definition consists primarily of the class name and a list of properties.  
 * @author Rainer
 */
public abstract class BeanClass
{
    private final String name;
    private final List<BeanProperty> properties = new ArrayList<BeanProperty>();
    private Column[] keyColumns;
    
    protected BeanDomain domain; // internal

    protected BeanClass(String name) 
    {
        this.name = name;
    }

    protected BeanClass(String name, BeanDomain dom) 
    {
        this(name);
        dom.addClass(this);
    }

    protected void addProp(BeanProperty prop)
    {
        properties.add(prop);
        prop.beanClass = this;
    }

    protected final BeanProperty addProp(String propname, DataType dataType, double size, DataMode dataMode, String controlType)
    {
        BeanProperty prop = new BeanProperty(propname, dataType, size, dataMode, controlType);
        addProp(prop);
        return prop;
    }

    protected final BeanProperty addProp(String propname, DataType dataType, double size, DataMode dataMode)
    {
        return addProp(propname, dataType, size, dataMode, "text");
    }

    protected final BeanProperty addProp(String propname, DataType dataType, double size, boolean required, String controlType, boolean readOnly)
    {
        BeanProperty prop = new BeanProperty(propname, dataType, size, required, controlType, readOnly);
        addProp(prop);
        return prop;
    }

    protected final BeanProperty addProp(String propname, DataType dataType, double size, boolean required, String controlType)
    {
        return addProp(propname, dataType, size, required, controlType, false);
    }

    protected final BeanProperty addProp(String propname, DataType dataType, double size, boolean required)
    {
        return addProp(propname, dataType, size, required, "text", false);
    }

    /**
     * Sets the list of key columns.
     * @param keyColumns the list of key columns.
     */
    protected void setKeyColumns(Column[] keyColumns)
    {
        this.keyColumns = keyColumns;
    }

    /**
     * Sets the key to a single column
     * @param keyColumn
     */
    protected final void setKeyColumn(Column keyColumn)
    {
        setKeyColumns(new Column[] { keyColumn });
    }
    
    /**
     * returns the name of this class
     * @return the class name
     */
    public String getName() 
    {
        return name;
    }

    /**
     * returns the list of properties for this class.
     * @return the list of properties for this class.
     */
    public List<BeanProperty> getProperties() 
    {
        return Collections.unmodifiableList(this.properties);
    }

    /**
     * returns the domain this class belongs to (if any)
     * @return the domain this class belongs to or null. 
     */
    public BeanDomain getDomain()
    {
        return domain;
    }

    /**
     * returns the list of key columns (if any)
     * @return the list of key columns or null.
     */
    public Column[] getKeyColumns()
    {
        return keyColumns;
    }
}
