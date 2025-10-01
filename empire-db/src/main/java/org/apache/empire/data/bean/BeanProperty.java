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

import java.util.Collections;
import java.util.Set;

import org.apache.empire.commons.Attributes;
import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.commons.Options;
import org.apache.empire.data.Column;
import org.apache.empire.data.ColumnExpr;
import org.apache.empire.data.DataType;
import org.apache.empire.data.Entity;
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
    private final double   size;
    private final boolean  required;
    
    private String      controlType; // optional (default is 'text')
    private String      title;       // optional
    private Options     options;     // optional
    private Attributes  attributes;  // optional
    private boolean     readOnly;
    
    protected BeanClass beanClass;  // internal;
    
    // --- ColumnExpr interface implementation ---

    /**
     * Constructs a bean property definition
     * @param name the name of the property
     * @param dataType the type of the property
     * @param size the size
     * @param required flag true if required
     * @param controlType string indication which type of control to use
     * @param readOnly flag true if read-only
     */
    public BeanProperty(String name, DataType dataType, double size, boolean required, String controlType, boolean readOnly)
    {
        this.name = name;
        this.dataType = dataType;
        this.size = size;
        this.required = required;
        this.controlType = controlType;
        this.readOnly = readOnly;
    }

    /**
     * Constructs a bean property definition
     * @param name the name of the property
     * @param dataType the type of the property
     * @param size the size
     * @param required flag true if required
     * @param controlType string indication which type of control to use
     */
    public BeanProperty(String name, DataType dataType, double size, boolean required, String controlType)
    {
        this(name, dataType, size, required, controlType, false);
    }
    
    /**
     * Constructs a bean property definition
     * @param name the name of the property
     * @param dataType the type of the property
     * @param size the size
     * @param required flag true if required
     */
    public BeanProperty(String name, DataType dataType, double size, boolean required)
    {
        this(name, dataType, size, required, "text", false);
    }

    @Override
    public Entity getEntity()
    {
        return beanClass;
    }
    
    /**
     * Returns the name of the property.
     * @return the property name
     */
    @Override
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
    @Override
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
    @Override
    public Object getAttribute(String name)
    {
        return (attributes!=null) ? attributes.get(name) : null;
    }

    /**
     * Sets an attribute for this column
     * @param name the attribute name
     * @param value the attribute value
     * @return the column itself
     */
    @SuppressWarnings("unchecked")
    @Override
    public synchronized <T extends ColumnExpr> T setAttribute(String name, Object value)
    {
        if (attributes== null)
            attributes = new Attributes();
        if (value!=null)
            attributes.set(name, value);
        else
            attributes.remove(name);
        return (T)this;
    }

    /**
     * Returns all metadata attributes.
     * @return set of metadata attributes
     */
    @Override
    @SuppressWarnings("unchecked")
    public Set<Attributes.Attribute> getAttributes()
    {
        return (attributes!=null ? Collections.unmodifiableSet(attributes)
                                 : Collections.EMPTY_SET);
    }

    /**
     * Returns the title attribute.
     * @return the column title
     */
    @Override
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
    @Override
    public Options getOptions()
    {
        return options;
    }

    /**
     * Returns the enum type for this column
     * <P>
     * @return the enum type
     */
    @Override
    @SuppressWarnings("unchecked")
    public Class<Enum<?>> getEnumType()
    {
        return (attributes!=null ? (Class<Enum<?>>)getAttribute(COLATTR_ENUMTYPE) : null);
    }

    /**
     * Returns the columns control type.
     * The control type is a client specific name for the type of input control 
     * that should be used to display and edit values for this column. 
     * 
     * @return the columns control type
     */
    @Override
    public String getControlType()
    {
        return controlType;
    }

    /**
     * Gets the Java bean property name.
     * This function should return the same string as getName()
     * @return the name of the bean property 
     */
    @Override
    public String getBeanPropertyName()
    {
        return name;
    }

    @Override
    public Column getUpdateColumn()
    {
        return this;
    }

    /**
     * Use getUpdateColumn() instead!
     */
    @Override
    @Deprecated
    public final Column getSourceColumn()
    {
        return getUpdateColumn();
    }

    // --- Column interface implementation ---
    
    @Override
    public double getSize()
    {
        return size;
    }

    @Override
    public boolean isReadOnly()
    {
        return (this.readOnly || dataType==DataType.AUTOINC);
    }

    @Override
    public boolean isAutoGenerated()
    {
        return (dataType==DataType.AUTOINC);
    }

    @Override
    public boolean isRequired()
    {
        return this.required;
    }

    @Override
    public Object validateValue(Object value)
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

}
