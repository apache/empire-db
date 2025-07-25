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

import java.util.Collection;
import java.util.List;

import org.apache.empire.commons.BeanPropertyUtils;
import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.commons.Options;
import org.apache.empire.data.Column;
import org.apache.empire.data.ColumnExpr;
import org.apache.empire.data.Entity;
import org.apache.empire.data.Record;
import org.apache.empire.exceptions.InvalidArgumentException;
import org.apache.empire.exceptions.ItemNotFoundException;
import org.apache.empire.exceptions.ObjectNotValidException;
import org.apache.empire.exceptions.PropertyReadOnlyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * BeanRecordProxy
 * This class defines proxy that allows any POJO to behave like a record object.
 *  
 * @param <T> the type of the class proxied by this {@code BeanRecordProxy}
 * 
 * @author Rainer
 */
public class BeanRecordProxy<T> implements Record
{
    protected static final Logger log = LoggerFactory.getLogger(BeanRecordProxy.class);
    
    protected final Entity entity;
    protected final List<Column> columns;
    protected final Column[] keyColumns;

    protected T data;
    protected boolean[] modified;

    public BeanRecordProxy(T data, List<Column> columns, Column[] keyColumns, Entity entity)
    {
        this.data = data;
        this.columns = columns;
        this.keyColumns = keyColumns;
        this.entity = entity;
    }

    public BeanRecordProxy(List<Column> columns, Column[] keyColumns, Entity entity)
    {
        this(null, columns, keyColumns, entity);
    }

    public BeanRecordProxy(T data, BeanClass beanClass)
    {
        this(data, 
             ObjectUtils.convert(Column.class, beanClass.getProperties()), 
             beanClass.getKeyColumns(),
             beanClass);
    }

    public BeanRecordProxy(BeanClass beanClass)
    {
        this(null, beanClass);
    }
    
    public T getBean()
    {
        return data;
    }

    public void setBean(T data)
    {
        this.data = data;
    }

    @Override
    public Column getColumn(int index)
    {
        return columns.get(index);
    }

    @Override
    public Column[] getKeyColumns()
    {
        return keyColumns;
    }

    /**
     * Returns the array of primary key columns.
     * @return the array of primary key columns
     */
    @Override
    public Object[] getKey()
    {
        if (keyColumns==null)
            return null;
        // Get key values
        Object[] key = new Object[keyColumns.length];
        for (int i=0; i<keyColumns.length; i++)
            key[i] = this.get(keyColumns[i]);
        // the key
        return key;
    }

    @Override
    public int getFieldCount()
    {
        return columns.size();
    }

    @Override
    public int getFieldIndex(ColumnExpr column)
    {
        for (int i=0; i<columns.size(); i++)
        {
            if (columns.get(i).equals(column))
                return i;
        }
        return -1;
    }

    @Override
    public int getFieldIndex(String columnName)
    {
        for (int i=0; i<columns.size(); i++)
        {
            if (columns.get(i).getName().equals(columnName))
                return i;
        }
        return -1;
    }

    @Override
    public Options getFieldOptions(Column column)
    {
        return column.getOptions();
    }

    @Override
    public boolean isFieldVisible(Column column)
    {
        return true;
    }

    @Override
    public boolean isFieldReadOnly(Column column)
    {
    	if (isNew()==false && ObjectUtils.contains(keyColumns, column))
    		return true;
    	if (column.isAutoGenerated())
    		return true;
        return column.isReadOnly();
    }

    @Override
    public boolean isFieldRequired(Column column)
    {
        return column.isRequired();
    }

    @Override
    public boolean isModified()
    {
        return (modified!=null);
    }

    @Override
    public boolean isNew()
    {
        if (!isValid())
            throw new ObjectNotValidException(this);
        // Record is new until all key fields have been supplied
        if (keyColumns!=null)
        {   // Check all Key Columns
            for (int i=0; i<keyColumns.length; i++)
            {
                Object value = get(keyColumns[i]);
                if ((value instanceof Number) && ((Number)value).longValue()==0)
                    return true;
                if (ObjectUtils.isEmpty(value))
                    return true;
            }
        }
        // Not new
        return false;
    }
    
    @Override
    public Entity getEntity()
    {
        return this.entity;
    }

    @Override
    public boolean isValid()
    {
        return (data!=null);
    }

    @Override
    public boolean isReadOnly()
    {
        return (isValid() ? false : true);
    }

    @Override
    public final Object getValue(int index)
    {
        return get(getColumn(index));
    }

    @Override
    public final <V> V getValue(int index, Class<V> valueType)
    {
        return get(getColumn(index), valueType);
    }

    @Override
    public <V> V get(ColumnExpr column, Class<V> valueType)
    {
        if (!isValid())
            throw new ObjectNotValidException(this);
        // getBeanPropertyValue 
        Object value = getBeanProperty(data, column);
        return ObjectUtils.convertColumnValue(column, value, valueType);
    }

    @Override
    public final Object get(ColumnExpr column)
    {
        return get(column, Object.class);
    }

    @Override
    public boolean isNull(ColumnExpr column)
    {
        return ObjectUtils.isEmpty(get(column));
    }

    @Override
    public boolean isNull(int index)
    {
        return isNull(getColumn(index));
    }

    /**
     * Validates a value before it is set in the record.
     */
    @Override
    public Object validateValue(Column column, Object value)
    {
        return column.validateValue(value);
    }

    /**
     * sets the value of a field.
     */
    @Override
    public BeanRecordProxy<T> set(Column column, Object value)
    {
        if (!isValid())
            throw new ObjectNotValidException(this);
        // Track modification status
        if (ObjectUtils.compareEqual(get(column), value)==false)
        {
            if (modified== null)
                modified = new boolean[columns.size()]; 
            modified[getFieldIndex(column)] = true;
        }
        // validate
        value = validateValue(column, value);
        // Set Value
        setBeanProperty(data, column, value);
        return this;
    }

    /**
     * sets the value of a field.
     */
    @Override
    public final void setValue(int i, Object value)
    {
        set(getColumn(i), value);
    }

    /**
     * Detects whether or not a particular field has been modified.
     */
    @Override
    public boolean wasModified(Column column)
    {
        int index = getFieldIndex(column);
        if (index<0)
            throw new ItemNotFoundException(column.getName());
        // check modified
        return (modified!=null && modified[index]);
    }

    /**
     * clears the modification status of the object and all fields.
     */
    public void clearModified()
    {
        modified = null;
    }

    /**
     * Returns the value of a column as a formatted text
     * This converts the value to a string if necessary and performs an options lookup
     * To customize conversion please override convertToString()
     * @param column the column for which to get the formatted value
     * @return the formatted value
     */
    public final String getText(ColumnExpr column)
    {   
        Object value = get(column);
        return formatValue(column, value);
    }

    // --------------- Bean support ------------------

    public int setBeanProperties(Object bean)
    {
        return setBeanProperties(bean, null);
    }

    @Override
    public int setBeanProperties(Object bean, Collection<? extends ColumnExpr> ignoreList)
    {
        // Add all Columns
        int count = 0;
        for (int i = 0; i < getFieldCount(); i++)
        { // Check Property
            Column column = getColumn(i);
            if (column.isReadOnly())
                continue;
            if (ignoreList != null && ignoreList.contains(column))
                continue; // ignore this property
            // Get Property Name
            setBeanProperty(bean, column, getValue(i));
        }
        return count;
    }

    @Override
    public int setRecordValues(Object bean, Collection<Column> ignoreList)
    {
        // Add all Columns
        int count = 0;
        for (int i = 0; i < getFieldCount(); i++)
        { // Check Property
            Column column = getColumn(i);
            if (column.isReadOnly())
                continue;
            if (ignoreList != null && ignoreList.contains(column))
                continue; // ignore this property
            // Get Property Name
            String property = column.getBeanPropertyName();
            Object value = getBeanProperty(bean, property);
            set(column, value);
            count++;
        }
        return count;
    }
    
    @Override
    public int setRecordValues(Object bean)
    {
        return setRecordValues(bean, null);
    }

    // --------------- protected ------------------

    protected final Object getBeanProperty(Object bean, ColumnExpr column)
    {
        // Check Params
        if (bean==null)
            throw new InvalidArgumentException("bean", bean);
        if (column==null)
            throw new InvalidArgumentException("column", column);
        // getBeanPropertyValue 
        return getBeanProperty(bean, column.getBeanPropertyName()); 
    }

    protected Object getBeanProperty(Object bean, String property)
    {
        return BeanPropertyUtils.getProperty(bean, property);
    }

    protected void setBeanProperty(Object bean, Column column, Object value)
    {
        if (value!=null)
        {   // Convert to enum
            Class<Enum<?>> enumType = column.getEnumType();
            if (enumType!=null)
                value = ObjectUtils.getEnum(enumType, value);
        }
        // set property
        String property = column.getBeanPropertyName();
        if (!BeanPropertyUtils.setProperty(bean, property, value))
        {   // Property has not been set
            if (BeanPropertyUtils.hasProperty(bean, property, true)==0)
                throw new PropertyReadOnlyException(property);
            else
                log.info("The bean property \"{}\" does not exist on {} and will be ignored!", property, bean.getClass().getName());
        }
    }

    /**
     * Convert a non-string value to a string
     * @param column the column expression 
     * @param value the value to format
     * @return the formatted string
     */
    protected String formatValue(ColumnExpr column, Object value)
    {
        return ObjectUtils.formatColumnValue(column, value, null);
    }
    
}
