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

import org.apache.empire.commons.Attributes;
import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.commons.Options;
import org.apache.empire.data.Column;
import org.apache.empire.data.DataMode;
import org.apache.empire.data.DataType;
import org.apache.empire.db.exceptions.FieldNotNullException;

/**
 * BeanObject
 * This class defines Metadata for a single property.
 * @author Rainer
 */
public class BeanProperty implements Column
{
    private final String   name;
    private final DataType dataType;
    private final DataMode dataMode;
    private final double   size;
    
    private String   controlType; // optional (default is 'text')
    private String   title;       // optional
    private Options  options;     // optional
    private Attributes attributes;// optional
    
    protected BeanClass beanClass;  // internal;
    
    // --- ColumnExpr interface implementation ---
    
    /**
     * Constructs a bean property definition
     * @param name the name of the property (case insensitive)
     * @param dataType the data type
     * @param size size depending on data type. For data type TEXT the maximum number of characters.
     * @param dataMode determines whether this property is read only, optional, required or auto-generated 
     * @param controlType the control type to be used for editing this value. Depends on the client. Default is "text"
     */
    public BeanProperty(String name, DataType dataType, double size, DataMode dataMode, String controlType)
    {
        this.name = name;
        this.dataType = dataType;
        this.size = size;
        this.dataMode = dataMode;
        this.controlType = controlType;
    }

    /**
     * Constructs a bean property definition
     * @param name
     * @param dataType
     * @param size
     * @param required
     * @param controlType
     * @param readOnly
     */
    public BeanProperty(String name, DataType dataType, double size, boolean required, String controlType, boolean readOnly)
    {
        this(name, dataType, size, (readOnly ? DataMode.ReadOnly : (required ? DataMode.NotNull : DataMode.Nullable)), controlType);
    }
    
    /**
     * Constructs a bean property definition
     * @param name
     * @param dataType
     * @param size
     * @param required
     */
    public BeanProperty(String name, DataType dataType, double size, boolean required)
    {
        this(name, dataType, size, (required ? DataMode.NotNull : DataMode.Nullable), "text");
    }

    /**
     * Returns the name of the property.
     * @return the property name
     */
    public String getName()
    {
        return name;
    }

    /**
     * Returns the data type of the bean property.
     * @see org.apache.empire.data.DataType
     *
     * @return the property data type
     */
    public DataType getDataType()
    {
        return dataType;
    }
    
    /**
     * Returns the value of a column attribute.
     * Column attributes are used to provide metadata for a property.
     * 
     * @param name the attribute name
     * @return value of the attribute if it exists or null otherwise
     */
    public Object getAttribute(String name)
    {
        return (attributes!=null) ? attributes.get(name) : null;
    }

    /**
     * Returns the title attribute.
     * @return the column title
     */
    public String getTitle()
    {
        if (title==null)
            return name;
        return title;
    }

    /**
     * Returns the list of options for this column
     * containing all allowed field values.
     * 
     * @return the list of options
     */
    public Options getOptions()
    {
        return options;
    }

    /**
     * Returns the columns control type.
     * The control type is a client specific name for the type of input control 
     * that should be used to display and edit values for this column. 
     * 
     * @return the columns control type
     */
    public String getControlType()
    {
        return controlType;
    }

    /**
     * Gets the Java bean property name.
     * This function should return the same string as getName()
     * @return the name of the bean property 
     */
    public String getBeanPropertyName()
    {
        return name;
    }

    /**
     * Returns the column 
     * This function should return the same string as getName()
     * @return the name of the bean property 
     */
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
        return (dataMode==DataMode.ReadOnly || dataMode==DataMode.AutoGenerated);
    }

    public boolean isAutoGenerated()
    {
        return (dataMode==DataMode.AutoGenerated);
    }

    public boolean isRequired()
    {
        return (dataMode==DataMode.NotNull);
    }

    public Object validate(Object value)
    {
        if (ObjectUtils.isEmpty(value) && isRequired())
            throw new FieldNotNullException(this);
        return value;
    }

    // --- others ---

    /**
     * returns the bean class of this property.
     * 
     * @return the BeanClass or <code>null</code> if BeanProperty is used 'stand alone'
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

    public void setAttribute(String name, Object value)
    {
        if (attributes== null)
            attributes = new Attributes();
        attributes.set(name, value);
    }

}
