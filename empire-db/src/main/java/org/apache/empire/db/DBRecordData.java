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
package org.apache.empire.db;
import java.beans.PropertyDescriptor;
// XML
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Date;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.commons.Options;
import org.apache.empire.commons.StringUtils;
import org.apache.empire.data.Column;
import org.apache.empire.data.ColumnExpr;
import org.apache.empire.data.RecordData;
import org.apache.empire.db.context.DBContextAware;
import org.apache.empire.db.exceptions.FieldIllegalValueException;
import org.apache.empire.exceptions.BeanPropertySetException;
import org.apache.empire.exceptions.InvalidArgumentException;
import org.apache.empire.exceptions.ItemNotFoundException;
import org.apache.empire.exceptions.PropertyReadOnlyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;


/**
 * This class provides access to the fields of one data-row of a table, view or query
 * The fields can be accessed either by Column or by index
 * There are various accessor functions for many data types.
 * The field values are converted to the desired type if possible.
 * The field values can be transferred to a classical Java-bean (aka Pojo)
 */
public abstract class DBRecordData extends DBObject 
	implements DBContextAware, RecordData
{
    // *Deprecated* private static final long serialVersionUID = 1L;

    // Logger
    private static final Logger log = LoggerFactory.getLogger(DBRecordData.class);

    // Field Info
    @Override
    public abstract int getFieldCount();

    @Override
    public abstract int getFieldIndex(ColumnExpr column);

    @Override
    public abstract int getFieldIndex(String column);

    // xml
    public abstract int addXmlMeta(Element parent);
    public abstract int addXmlData(Element parent);
    public abstract Document getXmlDocument();

    /**
     * Returns a value based on a field index.
     * @param index the field index
     * @return the field value
     */
    @Override
    public abstract Object getValue(int index);
    
    /**
     * Deprecated Renamed to get(...)   
     * @param column the column for which to obtain the value
     * @return the record value
     */
    @Deprecated
    public Object getValue(ColumnExpr column)
    {
        return get(column);
    }

    /**
     * Deprecated Renamed to get(...)   
     * @param <T> the value type
     * @param column the column for which to retrieve the value
     * @param returnType the type of the returned value
     * @return the value
     */
    @Deprecated
    public final <T> T getValue(Column column, Class<T> returnType)
    {
        return get(column, returnType);
    }

    /**
     * Deprecated Renamed to get(...)   
     * @param columns the columns for which to obtain the values
     * @return the record values
     */
    @Deprecated
    public final Object[] getValues(ColumnExpr... columns)
    {
        return getArray(columns);
    }
    
    /**
     * Returns a data value for the desired column .
     * 
     * @param column the column for which to obtain the value
     * @return the record value
     */
    @Override
    public final Object get(ColumnExpr column)
    {
        int index = getFieldIndex(column);
        if (index<0)
            throw new ItemNotFoundException(column.getName()); 
        return getValue(index);
    }

    /**
     * Returns the value of a field as an object of a given (wrapper)type
     * @param <T> the value type
     * @param column the column for which to retrieve the value
     * @param returnType the type of the returned value
     * @return the value
     */
    public final <T> T get(Column column, Class<T> returnType)
    {
        return ObjectUtils.convert(returnType, get(column));
    }

    /**
     * Returns an array of values for the given column expressions
     * 
     * @param columns the column expressions
     * @return the corresponding record values
     */
    public final Object[] getArray(ColumnExpr... columns)
    {
        Object[] values = new Object[columns.length];
        for (int i=0; i<columns.length; i++)
        {
            int index = getFieldIndex(columns[i]);
            if (index<0)
                throw new ItemNotFoundException(columns[i].getName()); 
            values[i] = getValue(index);
        }
        return values;
    }

    /**
     * Returns a data value identified by the column index.
     * The value is converted to integer if necessary .
     * 
     * @param index index of the column
     * @return the record value
     */
    public int getInt(int index)
    {
        return ObjectUtils.getInteger(getValue(index));
    }
    
    /**
     * Returns a data value for the desired column.
     * The data value is converted to integer if necessary.
     * 
     * @param column identifying the column
     * @return the value
     */
    public final int getInt(ColumnExpr column)
    {
        return getInt(getFieldIndex(column));
    }

    /**
     * Returns a data value identified by the column index.
     * The data value is converted to a long if necessary.
     * 
     * @param index index of the column
     * @return the value
     */
    public long getLong(int index)
    {
        return ObjectUtils.getLong(getValue(index));
    }
    
    /**
     * Returns a data value for the desired column.
     * The data value is converted to a long if necessary.
     * 
     * @param column identifying the column
     * @return the value
     */
    public final long getLong(ColumnExpr column)
    {
        return getLong(getFieldIndex(column));
    }

    /**
     * Returns a data value identified by the column index.
     * The data value is converted to double if necessary.
     * 
     * @param index index of the column
     * @return the value
     */
    public double getDouble(int index)
    {
        return ObjectUtils.getDouble(getValue(index));
    }

    /**
     * Returns a data value for the desired column.
     * The data value is converted to double if necessary.
     * 
     * @param column identifying the column
     * @return the value
     */
    public final double getDouble(ColumnExpr column)
    {
        return getDouble(getFieldIndex(column));
    }

    /**
     * Returns a data value identified by the column index.
     * The data value is converted to double if necessary.
     * 
     * @param index index of the column
     * @return the value
     */
    public BigDecimal getDecimal(int index)
    {
        return ObjectUtils.getDecimal(getValue(index));
    }

    /**
     * Returns a data value for the desired column.
     * The data value is converted to BigDecimal if necessary.
     * 
     * @param column identifying the column
     * @return the value
     */
    public final BigDecimal getDecimal(ColumnExpr column)
    {
        return getDecimal(getFieldIndex(column));
    }
    
    /**
     * Returns a data value identified by the column index.
     * The data value is converted to boolean if necessary.
     * 
     * @param index index of the column
     * @return the value
     */
    public boolean getBoolean(int index)
    {
        return ObjectUtils.getBoolean(getValue(index));
    }
    
    /**
     * Returns a data value for the desired column.
     * The data value is converted to boolean if necessary.
     * 
     * @param column identifying the column
     * @return the value
     */
    public final boolean getBoolean(ColumnExpr column)
    { 
        return getBoolean(getFieldIndex(column));
    }
    
    /**
     * Returns a data value identified by the column index.
     * The data value is converted to a string if necessary.
     * 
     * @param index index of the column
     * @return the value
     */
    public String getString(int index)
    {
        return ObjectUtils.getString(getValue(index));
    }

    /**
     * Returns a data value for the desired column.
     * The data value is converted to a string if necessary.
     * 
     * @param column identifying the column
     * @return the value
     */
    public final String getString(ColumnExpr column)
    {
        return getString(getFieldIndex(column));
    }

    /**
     * Returns a data value identified by the column index.
     * The data value is converted to a Date if necessary.
     * 
     * @param index index of the column
     * @return the value
     */
    public final Date getDateTime(int index)
    {
        return ObjectUtils.getDate(getValue(index));
    }
    
    /**
     * Returns a data value for the desired column.
     * The data value is converted to a Date if necessary.
     * 
     * @param column identifying the column
     * @return the value
     */
    public final Date getDateTime(ColumnExpr column)
    {
        return getDateTime(getFieldIndex(column));
    }

    /**
     * Returns a data value identified by the column index.
     * The data value is converted to a Date if necessary.
     * 
     * @param index index of the column
     * @return the value
     */
    public final LocalDate getLocalDate(int index)
    {
        return ObjectUtils.getLocalDate(getValue(index));
    }
    
    /**
     * Returns a data value for the desired column.
     * The data value is converted to a Date if necessary.
     * 
     * @param column identifying the column
     * @return the value
     */
    public final LocalDate getLocalDate(ColumnExpr column)
    {
        return getLocalDate(getFieldIndex(column));
    }

    /**
     * Returns a data value identified by the column index.
     * The data value is converted to a Date if necessary.
     * 
     * @param index index of the column
     * @return the value
     */
    public final LocalDateTime getLocalDateTime(int index)
    {
        return ObjectUtils.getLocalDateTime(getValue(index));
    }
    
    /**
     * Returns a data value for the desired column.
     * The data value is converted to a Date if necessary.
     * 
     * @param column identifying the column
     * @return the value
     */
    public final LocalDateTime getLocalDateTime(ColumnExpr column)
    {
        return getLocalDateTime(getFieldIndex(column));
    }

    /**
     * Returns the value of a field as an enum
     * For numeric columns the value is assumed to be an ordinal of the enumeration item
     * For non numeric columns the value is assumed to be the name of the enumeration item
     * 
     * @param <T> the enum type
     * @param index index of the field
     * @param enumType the enum type class
     * @return the enum value
     */
    public <T extends Enum<?>> T getEnum(int index, Class<T> enumType)
    {   // check for null
        if (isNull(index))
            return null;
        // convert
        ColumnExpr col = getColumn(index);
        try {
            // Convert to enum, depending on DataType
            boolean numeric = col.getDataType().isNumeric();
            return ObjectUtils.getEnum(enumType, (numeric ? getInt(index) : getValue(index)));

        } catch (Exception e) {
            // Illegal value
            String value = StringUtils.valueOf(getValue(index));
            log.error("Unable to resolve enum value of '{}' for type {}", value, enumType.getName());
            throw new FieldIllegalValueException(col.getUpdateColumn(), value, e);
        }
    }

    /**
     * Returns the value of a field as an enum
     * For numeric columns the value is assumed to be an ordinal of the enumeration item
     * For non numeric columns the value is assumed to be the name of the enumeration item
     * 
     * @param <T> the enum type
     * @param column the column for which to retrieve the value
     * @param enumType the enum type class
     * @return the enum value
     */
    public final <T extends Enum<?>> T getEnum(ColumnExpr column, Class<T> enumType)
    {
        return getEnum(getFieldIndex(column), enumType);
    }

    /**
     * Returns the value of a field as an enum
     * This assumes that the column attribute "enumType" has been set to an enum type
     * 
     * @param column the column for which to retrieve the value
     * @return the enum value
     */
    @SuppressWarnings("unchecked")
    public final <T extends Enum<?>> T getEnum(Column column)
    {
        Class<Enum<?>> enumType = column.getEnumType();
        if (enumType==null)
        {   // Not an enum column (Attribute "enumType" has not been set)
            throw new InvalidArgumentException("column", column);
        }
        return getEnum(getFieldIndex(column), (Class<T>)enumType);
    }
    
    /**
     * Checks whether or not the value for the given column is null.
     * 
     * @param index index of the column
     * @return true if the value is null or false otherwise
     */
    @Override
    public boolean isNull(int index)
    {
        return ObjectUtils.isEmpty(getValue(index));
    }

    /**
     * Checks whether or not the value for the given column is null.
     * 
     * @param column identifying the column
     * @return true if the value is null or false otherwise
     */
    @Override
    public final boolean isNull(ColumnExpr column)
    {
        return isNull(getFieldIndex(column));
    }
    
    /**
     * Returns the value of a column as a formatted text
     * This converts the value to a string if necessary and performs an options lookup
     * To customize conversion please override convertToString()
     * @param column the column for which to get the formatted value
     * @return the formatted value
     */
    public String getText(ColumnExpr column)
    {   
        String text;
        Object value = get(column);
        // check options first
        Options options = column.getOptions();
        if (options!=null && options.has(value))
        {   // lookup option
            text = options.get(value);
        }
        else if (value instanceof String)
        {   // we already have a string
            text = (String)value;
        }
        else if (ObjectUtils.isEmpty(value))
        {   // empty
            value = column.getAttribute(Column.COLATTR_NULLTEXT);
            text = (value!=null ? value.toString() : StringUtils.EMPTY);
        }
        else
        {   // format value
            text = formatValue(column, value);
            if (text== null)
                text = StringUtils.EMPTY; 
        }
        // done
        return text;
    }

    /**
     * Convert a non-string value to a string
     * @param column the column expression 
     * @param value the value to format
     * @return the formatted string
     */
    protected String formatValue(ColumnExpr column, Object value)
    {
        return ObjectUtils.getString(value);
    }
    
    /**
     * Set a single property value of a java bean object used by readProperties.
     *
     * @param column the column expression
     * @param bean the bean
     * @param property the property
     * @param value the value
     */
    protected void setBeanProperty(ColumnExpr column, Object bean, String property, Object value)
    {
        if (StringUtils.isEmpty(property))
            property = column.getBeanPropertyName();
        try
        {
            if (bean==null)
                throw new InvalidArgumentException("bean", bean);
            if (StringUtils.isEmpty(property))
                throw new InvalidArgumentException("property", property);
            if (log.isTraceEnabled())
                log.trace("{}: setting property '{}' to {}", bean.getClass().getName(), property, value);
            /*
            if (value instanceof Date)
            {   // Patch for date bug in BeanUtils
                value = DateUtils.addDate((Date)value, 0, 0, 0);
            }
            */
            // Set Property Value
            if (value!=null)
            {   // Convert to enum
                Class<Enum<?>> enumType = column.getEnumType();
                if (enumType!=null)
                    value = ObjectUtils.getEnum(enumType, value);
                // Bean utils will convert if necessary
                BeanUtils.setProperty(bean, property, value);
            }
            else
            {   // Don't convert, just set
                PropertyDescriptor pd = PropertyUtils.getPropertyDescriptor(bean, property);
                if (pd==null)
                    return; // No such property
                // get the write method
                final Method method = PropertyUtils.getWriteMethod(pd);
                if (method == null)
                    throw new PropertyReadOnlyException(property);
                // invoke
                method.invoke(bean, value);
            }
          // IllegalAccessException
        } catch (IllegalAccessException e)
        {   log.error(bean.getClass().getName() + ": unable to set property '" + property + "'");
            throw new BeanPropertySetException(bean, property, e);
          // InvocationTargetException  
        } catch (InvocationTargetException e)
        {   log.error(bean.getClass().getName() + ": unable to set property '" + property + "'");
            throw new BeanPropertySetException(bean, property, e);
          // NoSuchMethodException   
        } catch (NoSuchMethodException e)
        {   log.error(bean.getClass().getName() + ": unable to set property '" + property + "'");
            throw new BeanPropertySetException(bean, property, e);
        } catch (NullPointerException e)
        {   log.error(bean.getClass().getName() + ": unable to set property '" + property + "'");
            throw new BeanPropertySetException(bean, property, e);
        }
    }

    /**
     * Injects the current field values into a java bean.
     * 
     * @return the number of bean properties set on the supplied bean
     */
    @Override
    public int setBeanProperties(Object bean, Collection<? extends ColumnExpr> ignoreList)
    {
        // Add all Columns
        int count = 0;
        for (int i = 0; i < getFieldCount(); i++)
        {   // Check Property
            ColumnExpr column = getColumn(i);
            if (ignoreList != null && ignoreList.contains(column))
                continue; // ignore this property
            // Get Property Name
            String property = column.getBeanPropertyName();
            if (property!=null)
                setBeanProperty(column, bean, property, this.getValue(i));
            count++;
        }
        return count;
    }

    /**
     * Injects the current field values into a java bean.
     * @param bean the bean
     * @return the number of bean properties set on the supplied bean
     */
    public final int setBeanProperties(Object bean)
    {
        return setBeanProperties(bean, null);
    }
    
}
