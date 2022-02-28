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
import java.util.Collection;
import java.util.Date;

import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.data.Column;
import org.apache.empire.data.ColumnExpr;
import org.apache.empire.data.EntityType;
import org.apache.empire.data.RecordData;
import org.apache.empire.db.exceptions.NoPrimaryKeyException;
import org.apache.empire.exceptions.InvalidArgumentException;
import org.apache.empire.exceptions.ItemNotFoundException;
import org.apache.empire.exceptions.NotImplementedException;
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
     * @param entityType the entity type or rowset for which to get key
     * @return the record key
     */
    public Object[] getRecordKey(EntityType entityType)
    {
        Column[] keyColumns = entityType.getKeyColumns();
        if (keyColumns==null || keyColumns.length==0)
            throw new NoPrimaryKeyException(entityType);
        // Collect key
        Object[] key = new Object[keyColumns.length];
        for (int i=0; i<key.length; i++)
            key[i] = this.get(keyColumns[i]);
        return key;
    }

    /**
     * Returns the record id for a type of entity which has a single numeric primary key
     * @param entityType the entity type or rowset for which to get key
     * @return the record id
     * @throws InvalidArgumentException if the entity has not a single numeric primary key
     */
    public long getRecordId(EntityType entityType)
    {
        Column[] keyColumns = entityType.getKeyColumns();
        if (keyColumns==null || keyColumns.length!=1)
            throw new InvalidArgumentException("entityType", entityType.getEntityName());
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
        for (int i=0; i<cols.length; i++)
        {
            ColumnExpr col = cols[i].unwrap();
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
                values[i] = recData.getValue(ri);
            } catch(Exception e) {
                log.error("Failed to update value for column {}", cols[i].getName());
            }
        }
    }
    
    public void modifyValue(ColumnExpr col, Object value)
    {
        int i = getFieldIndex(col);
        if (i<0)
            throw new ItemNotFoundException(col);
        // update
        values[i] = value;
    }
    
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
    
    @Override
    public Object getValue(int index)
    {
        if (index<0 || index>=values.length)
            throw new InvalidArgumentException("index", index);
        return values[index];
    }
    
    @Override
    public final Object get(ColumnExpr column)
    {
        return getValue(indexOf(column));
    }
    
    /**
     * @Deprecated Renamed to get(...)   
     */
    @Deprecated
    public Object getValue(ColumnExpr column)
    {
        return get(column);
    }

    public final <T> T get(Column column, Class<T> returnType)
    {
        return ObjectUtils.convert(returnType, get(column));
    }

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
    
    @Override
    public boolean isNull(int index)
    {
        return ObjectUtils.isEmpty(getValue(index));
    }
    
    /*
     * Conversion functions
     */

    public String getString(int index)
    {   // Get String value
        Object o = getValue(index);
        return ObjectUtils.getString(o);
    }

    public final String getString(ColumnExpr column)
    {
        return getString(indexOf(column));
    }

    public int getInt(int index)
    {   // Get Integer value
        Object o = getValue(index);
        return ObjectUtils.getInteger(o);
    }
    
    public final int getInt(ColumnExpr column)
    {
        return getInt(indexOf(column));
    }

    public long getLong(int index)
    {   // Get Long value
        Object o = getValue(index);
        return ObjectUtils.getLong(o);
    }
    
    public final long getLong(ColumnExpr column)
    {
        return getLong(indexOf(column));
    }

    public BigDecimal getDecimal(int index)
    {   // Get Integer value
        Object o = getValue(index);
        return ObjectUtils.getDecimal(o);
    }
    
    public final BigDecimal getDecimal(ColumnExpr column)
    {
        return getDecimal(indexOf(column));
    }
    
    public boolean getBoolean(int index)
    {   // Get Integer value
        Object o = getValue(index);
        return ObjectUtils.getBoolean(o);
    }
    
    public final boolean getBoolean(ColumnExpr column)
    {
        return getBoolean(indexOf(column));
    }

    public <T extends Enum<?>> T getEnum(int index, Class<T> enumType)
    {   // check for null
        if (isNull(index))
            return null;
        // check column data type
        ColumnExpr col = getColumn(index);
        boolean numeric = col.getDataType().isNumeric();
        return ObjectUtils.getEnum(enumType, (numeric ? getInt(index) : getValue(index)));
    }

    public final <T extends Enum<?>> T getEnum(ColumnExpr column, Class<T> enumType)
    {
        return getEnum(indexOf(column), enumType);
    }

    @SuppressWarnings("unchecked")
    public final <T extends Enum<?>> T getEnum(Column column)
    {
        Class<T> enumType = (Class<T>)column.getEnumType();
        if (enumType==null)
        {   // Not an enum column (Attribute "enumType" has not been set)
            throw new InvalidArgumentException("column", column);
        }
        return getEnum(indexOf(column), enumType);
    }
    
    public Date getDate(int index)
    {
        Object o = getValue(index);
        return ObjectUtils.getDate(o);
    }
        
    public final Date getDate(ColumnExpr column)
    {
        return getDate(indexOf(column));
    }
    
    @Override
    public final boolean isNull(ColumnExpr column)
    {
        return isNull(indexOf(column));
    }
    
    @Override
    public int setBeanProperties(Object bean, Collection<? extends ColumnExpr> ignoreList)
    {
        throw new NotImplementedException(this, "setBeanProperties");
    }
    
    public int setBeanProperties(Object bean)
    {
        return setBeanProperties(bean, null);
    }
    
    /*
     * Miscellaneous functions
     */

    public String getText(String name)
    {
        int idx = getFieldIndex(name);
        return head.getText(idx, values[idx]);
    }

    public String getText(ColumnExpr col)
    {
        int idx = getFieldIndex(col);
        return head.getText(idx, values[idx]);
    }
    
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

    protected int indexOf(ColumnExpr column)
    {
        int index = head.getColumnIndex(column);
        if (index<0)
            throw new ItemNotFoundException(column.getName());
        return index;
    }
    
}
