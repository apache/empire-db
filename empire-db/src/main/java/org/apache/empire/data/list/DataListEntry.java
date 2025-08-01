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
package org.apache.empire.data.list;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Date;

import org.apache.empire.commons.BeanPropertyUtils;
import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.data.Column;
import org.apache.empire.data.ColumnExpr;
import org.apache.empire.data.Entity;
import org.apache.empire.data.RecordData;
import org.apache.empire.db.DBRecordBase;
import org.apache.empire.db.exceptions.NoPrimaryKeyException;
import org.apache.empire.exceptions.InvalidArgumentException;
import org.apache.empire.exceptions.ItemNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataListEntry implements RecordData, Serializable
{
    private static final long serialVersionUID = 1L;
    
    private static final Logger log  = LoggerFactory.getLogger(DataListEntry.class);
    
    protected final DataListHead head;
    protected final Object values[];
    protected int rownum;

    public DataListEntry(DataListHead head, Object values[], int rownum)
    {
        this.head = head;
        this.values = values;
        this.rownum = rownum;
        // check
        int headColumnCount = head.getColumns().length;
        if (values.length!=headColumnCount)
            log.warn("DataListEntry number of values {} do not match number of head columns {}!", values.length, headColumnCount);
    }
    
    public DataListEntry(DataListHead head, Object values[])
    {
        this(head, values, -1);
    }
    
    @SuppressWarnings("unchecked")
    public <T extends DataListHead> T getHead()
    {
        return (T)this.head;
    }
    
    /**
     * Returns the record key for a type of entity
     * @param entity the entity type or rowset for which to get key
     * @return the record key
     */
    public Object[] getRecordKey(Entity entity)
    {
        Column[] keyColumns = entity.getKeyColumns();
        if (keyColumns==null || keyColumns.length==0)
            throw new NoPrimaryKeyException(entity);
        // Collect key
        Object[] key = new Object[keyColumns.length];
        for (int i=0; i<key.length; i++)
            key[i] = this.get(keyColumns[i]);
        return key;
    }

    /**
     * Returns the record id for a type of entity which has a single numeric primary key
     * @param entity the entity type or rowset for which to get key
     * @return the record id
     * @throws InvalidArgumentException if the entity has not a single numeric primary key
     */
    public long getRecordId(Entity entity)
    {
        Column[] keyColumns = entity.getKeyColumns();
        if (keyColumns==null || keyColumns.length!=1)
            throw new InvalidArgumentException("entity", entity.getEntityName());
        // return id
        return ObjectUtils.getLong(get(keyColumns[0]));
    }
    
    /**
     * Compares a given record key with the key of the entry 
     * @param keyColumns the columns which make up the key
     * @param key the key to compare the current entry to
     * @return true if the keys match or false otherwise
     */
    public boolean compareKey(Column[] keyColumns, Object[] key)
    {
        for (int i=0; i<keyColumns.length; i++)
        {   // find field
            int index = getFieldIndex(keyColumns[i]);
            if (index<0)
                throw new ItemNotFoundException(keyColumns[i].getName());
            // compare
            if (!ObjectUtils.compareEqual(values[index], key[i]))
                return false; // not equal
        }
        // found
        return true;
    }
    
    /**
     * Updates the fields of the entry with the corresponding fields of a record.
     * @param recData the record with the updated (newer) fields
     */
    public void updateData(RecordData recData)
    {
        ColumnExpr[] cols = head.getColumns(); 
        DBRecordBase dbrb =(recData instanceof DBRecordBase) ? (DBRecordBase)recData : null;
        for (int i=0; i<cols.length; i++)
        {
            ColumnExpr col = ObjectUtils.unwrap(cols[i]);
            // must be a column!
            if (!(col instanceof Column))
            {   // not a true column
                log.info("Column expression {} is not a column. Skipping update.", col.getName());
                continue;
            }
            int ri = recData.getFieldIndex(col);
            if (ri<0)
                continue;
            // update
            try {
                // check valid
                if (dbrb!=null && !dbrb.isValueValid(ri))
                    continue;
                // set data
                values[i] = recData.getValue(ri);
            } catch(Exception e) {
                log.error("Failed to update value for column {}", cols[i].getName());
            }
        }
    }
    
    /**
     * Modifies a particular value
     * @param col the column to modify
     * @param value the new value
     */
    public void modifyValue(ColumnExpr col, Object value)
    {
        int i = getFieldIndex(col);
        if (i<0)
            throw new ItemNotFoundException(col);
        // update
        values[i] = value;
    }
    
    /**
     * Returns the row-number
     * This works only, if the row number has been set in the constructor
     * @return the row number
     */
    public int getRownum()
    {
        return rownum;
    }

    @Override
    public int getFieldCount()
    {
        return head.columns.length;
    }
    
    public boolean hasField(ColumnExpr column)
    {
        return (head.getColumnIndex(column)>=0);
    }
    
    @Override
    public int getFieldIndex(ColumnExpr column)
    {
        return head.getColumnIndex(column);
    }
    
    @Override
    public int getFieldIndex(String column)
    {
        return head.getColumnIndex(column);
    }
    
    @Override
    public ColumnExpr getColumn(int index)
    {
        if (index<0 || index>=values.length)
            throw new InvalidArgumentException("index", index);
        return head.columns[index];
    }
    
    /**
     * Returns the raw field value based on the field index.
     * @param index the field index
     * @return the raw field value
     */
    @Override
    public Object getValue(int index)
    {
        if (index<0 || index>=values.length)
            throw new InvalidArgumentException("index", index);
        return values[index];
    }
    
    /**
     * returns the record value for a particular column 
     * @param index the field index for which to return the value
     * @param valueType the desired value type
     * @return the record value for the given column
     */
    @Override
    public <V> V getValue(int index, Class<V> valueType)
    {
        return ObjectUtils.convertColumnValue(getColumn(index), getValue(index), valueType);
    }

    /**
     * returns the record value for a particular column 
     * @param column the column for which to return the value
     * @param valueType the desired value type
     * @return the record value for the given column
     */
    @Override
    public final <V> V get(ColumnExpr column, Class<V> valueType)
    {
        return getValue(getFieldIndex(column), valueType);
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
        return getValue(getFieldIndex(column), ObjectUtils.coalesce(column.getEnumType(), Object.class));
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
     * Returns true if a numeric value is Zero
     * For numeric columns only!
     * @param column the column
     * @return true if the value is zero or false otherwise 
     */
    public boolean isZero(ColumnExpr column)
    {
        if (column==null || !column.getDataType().isNumeric())
            throw new InvalidArgumentException("column", column);
        // check for zero
        Object v = get(column);
        return (v instanceof Number) ? ObjectUtils.isZero((Number)v) : true;
    }
    
    /*
     * Conversion functions
     */
    
    /**
     * Returns a data value identified by the column index.
     * The data value is converted to a string if necessary.
     * 
     * @param index index of the column
     * @return the value
     */
    public final String getString(int index)
    {
        // return ObjectUtils.getString(getValue(index));
        return getValue(index, String.class);
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
     * The value is converted to integer if necessary .
     * 
     * @param index index of the column
     * @return the record value
     */
    public final int getInt(int index)
    {
        // return ObjectUtils.getInteger(getValue(index));
        Integer value = getValue(index, Integer.class); 
        return (value!=null ? value : 0) ;
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
    public final long getLong(int index)
    {
        // return ObjectUtils.getLong(getValue(index));
        Long value = getValue(index, Long.class);
        return (value!=null ? value : 0l) ;
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
        // return ObjectUtils.getDouble(getValue(index));
        Double value = getValue(index, Double.class);
        return (value!=null ? value : 0.0d) ;
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
    public final BigDecimal getDecimal(int index)
    {
        // return ObjectUtils.getDecimal(getValue(index));
        return getValue(index, BigDecimal.class);
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
    public final boolean getBoolean(int index)
    {
        // return ObjectUtils.getBoolean(getValue(index));
        Boolean value = getValue(index, Boolean.class);
        return (value!=null ? value : false); 
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
     * Returns the value as as a java.util.Date object
     * The data value is converted to a Date if necessary.
     * 
     * @param index index of the column
     * @return the value
     */
    public final Date getDate(int index)
    {
        // return ObjectUtils.getDate(getValue(index));
        return getValue(index, Date.class);
    }
    
    /**
     * Returns the value as as a java.util.Date object
     * The data value is converted to a Date if necessary.
     * 
     * @param column identifying the column
     * @return the value
     */
    public final Date getDate(ColumnExpr column)
    {
        return getDate(getFieldIndex(column));
    }

    /**
     * Returns the value as as a java.sql.Timestamp object
     * @return the Timestamp
     */
    public final Timestamp getTimestamp(int index)
    {
        // return ObjectUtils.getTimestamp(getValue(index));
        return getValue(index, Timestamp.class);
    }

    /**
     * Returns the value as as a java.sql.Timestamp object
     * @return the Timestamp
     */
    public final Timestamp getTimestamp(ColumnExpr column)
    {
        return getTimestamp(getFieldIndex(column));
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
        // return ObjectUtils.getLocalDate(getValue(index));
        return getValue(index, LocalDate.class);
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
        // return ObjectUtils.getLocalDateTime(getValue(index));
        return getValue(index, LocalDateTime.class);
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
    public final <T extends Enum<?>> T getEnum(int index, Class<T> enumType)
    {   
        return getValue(index, enumType);
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
     * For numeric columns the value is assumed to be an ordinal of the enumeration item
     * 
     * @param <T> the enum type
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
     * Returns the value of a column as a formatted text
     * This converts the value to a string if necessary and performs an options lookup
     * To customize conversion please override convertToString()
     * @param column the column for which to get the formatted value
     * @return the formatted value
     */
    public String getText(ColumnExpr column)
    {
        int index = getFieldIndex(column);
        return head.getText(index, values[index]);
    }

    /**
     * Returns the value of a column as a formatted text
     * This converts the value to a string if necessary and performs an options lookup
     * To customize conversion please override convertToString()
     * @param name the column for which to get the formatted value
     * @return the formatted value
     */
    public final String getText(String name)
    {
        return getText(getColumn(getFieldIndex(name)));
    }

    /**
     * Injects the current field values into a java bean.
     * The property name is detected by ColumnExpr.getBeanPropertyName()
     * @param bean the Java Bean for which to set the properties
     * @param ignoreList list of columns to skip (optional)
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
            setBeanProperty(column, bean, this.getValue(i));
            count++;
        }
        return count;
    }

    /**
     * Injects the current field values into a java bean.
     * The property name is detected by ColumnExpr.getBeanPropertyName()
     * @param bean the Java Bean for which to set the properties
     * @return the number of bean properties set on the supplied bean
     */
    public final int setBeanProperties(Object bean)
    {
        return setBeanProperties(bean, null);
    }
    
    /**
     * Set a single property value on a java bean object
     *
     * @param column the column expression
     * @param bean the bean
     * @param value the value
     */
    protected void setBeanProperty(ColumnExpr column, Object bean, Object value)
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
            if (log.isDebugEnabled())
                log.debug("The bean property \"{}\" coult not be set on {} and will be ignored!", property, bean.getClass().getName());
        }
    }
    
    /*
     * Miscellaneous functions
     */
    
    @Override
    public String toString()
    {
        StringBuilder b = new StringBuilder();
        for (int i=0; i<values.length; i++)
        {
            b.append(head.getText(i, values[i]));
            b.append(head.columnSeparator);
        }
        return b.toString();
    }

    /**
     * Returns the field index
     * If the column cannot be found a ItemNotFoundException is thrown
     * @param column the column for which to return the index
     * @return the index
    protected int indexOf(ColumnExpr column)
    {
        int index = head.getColumnIndex(column);
        if (index<0)
            throw new ItemNotFoundException(column.getName());
        return index;
    }
     */
    
}
