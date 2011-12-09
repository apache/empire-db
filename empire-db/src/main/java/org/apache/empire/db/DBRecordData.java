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
import org.apache.empire.commons.DateUtils;
import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.commons.StringUtils;
import org.apache.empire.data.ColumnExpr;
import org.apache.empire.data.RecordData;
import org.apache.empire.exceptions.BeanPropertySetException;
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
    public abstract int     getFieldCount();
    public abstract int  	getFieldIndex(ColumnExpr column);
    public abstract int  	getFieldIndex(String column);
    // Column lookup
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
    public abstract Object  getValue(int index);
    
    /**
     * Returns a data value for the desired column .
     * 
     * @param column the column for which to obtain the value
     * @return the record value
     */
    public final Object getValue(ColumnExpr column)
    {
        return getValue(getFieldIndex(column));
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
    public final boolean isNull(ColumnExpr column)
    {
        return isNull(getFieldIndex(column));
    }

    /**
     * Set a single property value of a java bean object used by readProperties.
     */
    protected void getBeanProperty(Object bean, String property, Object value)
    {
        try
        {   /*
            if (log.isTraceEnabled())
                log.trace(bean.getClass().getName() + ": setting property '" + property + "' to " + String.valueOf(value));
            */    
            if (value instanceof Date)
            {   // Patch for Stage Date Bug in BeanUtils
                value = DateUtils.addDate((Date)value, 0, 0, 0);
            }
            // Set Property Value
            // Should check whether property exists
            BeanUtils.setProperty(bean, property, value);
            // Check result
            /*
             * String res = BeanUtils.getProperty(bean, property); if (res!=value && res.equals(String.valueOf(value))==false) { //
             * Property value cannot be set // (missing setter?) String msg = bean.getClass().getName() + ": unable to set
             * property '" + property + "' to " + String.valueOf(value); return error(ERR_INTERNAL, msg); } else if
             * (log.isInfoEnabled()) { log.info(bean.getClass().getName() + ": property '" + property + "' has been set to " +
             * res); }
             */
            // done

        } catch (IllegalAccessException e)
        {   log.error(bean.getClass().getName() + ": unable to set property '" + property + "'");
            throw new BeanPropertySetException(bean, property, e);
            
        } catch (InvocationTargetException e)
        {   log.error(bean.getClass().getName() + ": unable to set property '" + property + "'");
            throw new BeanPropertySetException(bean, property, e);
            /*
             * } catch(NoSuchMethodException e) { log.warn(bean.getClass().getName() + ": cannot check value of property '" +
             * property + "'"); return true;
             */
        }
    }

    /**
     * Injects the current field values into a java bean.
     * 
     * @return true if successful
     */
    public int getBeanProperties(Object bean, Collection<ColumnExpr> ignoreList)
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
            getBeanProperty(bean, property, this.getValue(i));
            count++;
        }
        return count;
    }

    /**
     * Injects the current field values into a java bean.
     * 
     * @return true if successful
     */
    public final int getBeanProperties(Object bean)
    {
        return getBeanProperties(bean, null);
    }
    
}
