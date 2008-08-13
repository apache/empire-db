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

import org.apache.empire.commons.Options;
import org.apache.empire.data.Column;
import org.apache.empire.data.DataType;

/**
 * BeanObject
 * This class defines Metadata for a single property.
 * @author Rainer
 */
public class BeanProperty implements Column
{
    private String   name;        // required
    private DataType dataType;    // required
    private double   size;        // required
    private boolean  required;    // required
    private String   controlType; // optional (default is 'text')
    private boolean  readOnly;    // optional
    private String   title;       // optional
    private Options  options;     // optional
    
    protected BeanClass beanClass;  // internal;
    
    // --- ColumnExpr interface implementation ---
    
    public BeanProperty(String name, DataType dataType, double size, boolean required, String controlType, boolean readOnly)
    {
        this.name = name;
        this.dataType = dataType;
        this.size = size;
        this.required = required;
        this.controlType = controlType;
        this.readOnly = readOnly;
    }

    public String getName()
    {
        return name;
    }

    public DataType getDataType()
    {
        return dataType;
    }

    public Object getAttribute(String name)
    {
        return null;
    }

    public String getTitle()
    {
        if (title==null)
            return name;
        return title;
    }

    public Options getOptions()
    {
        return options;
    }

    public String getControlType()
    {
        return controlType;
    }

    public String getBeanPropertyName()
    {
        return name;
    }

    public Column getSourceColumn()
    {
        return this;
    }

    // --- Column interface implementation ---
    
    public double getSize()
    {
        return size;
    }

    public boolean isReadOnly()
    {
        return readOnly;
    }

    public boolean isRequired()
    {
        return required;
    }

    // --- others ---

    /**
     * returns the bean class of this property.
     * May be NULL if BeanProperty is used 'stand alone'
     */
    public BeanClass getBeanClass()
    {
        return beanClass;
    }
    
    // --- setters ---

    public void setControlType(String controlType)
    {
        this.controlType = controlType;
    }

    public void setOptions(Options options) 
    {
        this.options = options;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

}
