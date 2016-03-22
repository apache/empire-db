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
// XML
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Date;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.commons.StringUtils;
import org.apache.empire.data.Column;
import org.apache.empire.data.ColumnExpr;
import org.apache.empire.data.RecordData;
import org.apache.empire.exceptions.BeanPropertySetException;
import org.apache.empire.exceptions.InvalidArgumentException;
import org.apache.empire.exceptions.ItemNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;


/**
 * This interface defines for the classes DDRecordSet and DBRecord.
 * <P>
 * 
 *
 */
public abstract class DBRecordData extends DBObject
	implements RecordData
{
    private final static long serialVersionUID = 1L;
  
    // Logger
    private static final Logger log = LoggerFactory.getLogger(DBRecordData.class);
    
    // Field Info
    @Override
    public abstract int     getFieldCount();
    @Override
    public abstract int  	getFieldIndex(ColumnExpr column);
    @Override
    public abstract int  	getFieldIndex(String column);
    // Column lookup
    @Override
    public abstract ColumnExpr getColumnExpr(int i);
    // xml
    public abstract int     addColumnDesc(Element parent);
    public abstract int     addRowValues (Element parent);
    public abstract Document getXmlDocument();
    // others
    public abstract void    close();

    /**
     * Returns a value based on an index.
     */
    @Override
    public abstract Object  getValue(int index);
    
    /**
     * Returns a data value for the desired column .
     * 
     * @param column the column for which to obtain the value
     * @return the record value
     */
    @Override
    public final Object getValue(ColumnExpr column)
    {
        int index = getFieldIndex(column);
        if (index<0)
            throw new ItemNotFoundException(column.getName()); 
        return getValue(index);
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
        // Get Integer value
        Object o = getValue(index);
        return ObjectUtils.getInteger(o);
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
        // Get Integer value
        Object o = getValue(index);
        return ObjectUtils.getLong(o);
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
        // Get Double value
        Object v = getValue(index);
        return ObjectUtils.getDouble(v);
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
        // Get Double value
        Object v = getValue(index);
        return ObjectUtils.getDecimal(v);
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
        // Get Boolean value
        Object o = getValue(index);
        return ObjectUtils.getBoolean(o);
    }
    
    /**
     * Returns a data value for the desired column.
     * The data value is converted to boolean if necessary.
     * 
     * @param column identifying the column
     * @return the value
     */
    public final boolean getBoolean(ColumnExpr column)
    { return getBoolean(getFieldIndex(column)); }
    
    /**
     * Returns a data value identified by the column index.
     * The data value is converted to a string if necessary.
     * 
     * @param index index of the column
     * @return the value
     */
    public String getString(int index)
    {
        // Get Integer value
        Object o = getValue(index);
        return StringUtils.toString(o);
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
    public Date getDateTime(int index)
    {
        // Get DateTime value
        Object o = getValue(index);
        return ObjectUtils.getDate(o);
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
     * Checks whether or not the value for the given column is null.
     * 
     * @param index index of the column
     * @return true if the value is null or false otherwise
     */
    @Override
    public boolean isNull(int index)
    {
        return (getValue(index) == null);
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
     * Set a single property value of a java bean object used by readProperties.
     */
    @SuppressWarnings("rawtypes")
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
            /*
            if (log.isTraceEnabled())
                log.trace(bean.getClass().getName() + ": setting property '" + property + "' to " + String.valueOf(value));
            */
            /*
            if (value instanceof Date)
            {   // Patch for date bug in BeanUtils
                value = DateUtils.addDate((Date)value, 0, 0, 0);
            }
            */
            Object type = column.getAttribute(Column.COLATTR_ENUMTYPE);
            if (type!=null && value!=null)
            {
                String name = value.toString();
                @SuppressWarnings("unchecked")
                Class<Enum> enumType = (Class<Enum>)type;
                for (Enum e : enumType.getEnumConstants())
                    if (e.name().equals(name))
                    {
                        value = e;
                        break;
                    }
            }
            // Set Property Value
            if (value!=null)
            {   // Bean utils will convert if necessary
                BeanUtils.setProperty(bean, property, value);
            }
            else
            {   // Don't convert, just set
                PropertyUtils.setProperty(bean, property, null);
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
    public int setBeanProperties(Object bean, Collection<ColumnExpr> ignoreList)
    {
        // Add all Columns
        int count = 0;
        for (int i = 0; i < getFieldCount(); i++)
        { // Check Property
            ColumnExpr column = getColumnExpr(i);
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
     * 
     * @return the number of bean properties set on the supplied bean
     */
    @Override
    public final int setBeanProperties(Object bean)
    {
        return setBeanProperties(bean, null);
    }
    
}
