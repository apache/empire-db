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
import org.apache.empire.commons.StringUtils;
import org.apache.empire.data.Column;
import org.apache.empire.data.ColumnExpr;
import org.apache.empire.data.Entity;
import org.apache.empire.data.RecordData;
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
    
    public DataListEntry(DataListHead head, int rownum, Object values[])
    {
        this.head = head;
        this.rownum = rownum;
        this.values = values;
    }
    
    @SuppressWarnings("unchecked")
    public <T extends DataListHead> T getHead()
    {
        return (T)this.head;
    }
    
    public Object[] getRecordKey(Entity entity)
    {
        Column[] keyColumns = entity.getKeyColumns();
        Object[] key = new Object[keyColumns.length];
        for (int i=0; i<key.length; i++)
            key[i] = this.getValue(keyColumns[i]);
        return key;
    }

    public long getRecordId(Entity entity)
    {
        Column[] keyColumns = entity.getKeyColumns();
        if (keyColumns.length!=1)
            throw new InvalidArgumentException("entity", entity.getName());
        // return id
        return ObjectUtils.getLong(getValue(keyColumns[0]));
    }
    
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
    public ColumnExpr getColumnExpr(int index)
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
    public Object getValue(ColumnExpr column)
    {
        return getValue(indexOf(column));
    }
    
    @Override
    public boolean isNull(int index)
    {
        if (index<0 || index>=values.length)
            throw new InvalidArgumentException("index", index);
        return ObjectUtils.isEmpty(values[index]);
    }
    
    @Override
    public boolean isNull(ColumnExpr column)
    {
        return isNull(indexOf(column));
    }
    
    @Override
    public int setBeanProperties(Object bean, Collection<? extends ColumnExpr> ignoreList)
    {
        throw new NotImplementedException(this, "setBeanProperties");
    }
    
    @Override
    public int setBeanProperties(Object bean)
    {
        return setBeanProperties(bean, null);
    }
    
    /*
     * Conversion functions
     */

    public String getString(int index)
    {   // Get String value
        Object o = getValue(index);
        return StringUtils.toString(o);
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
        ColumnExpr col = getColumnExpr(index);
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
    
    /*
     * Miscellaneous functions
     */

    public String format(String name)
    {
        int idx = getFieldIndex(name);
        return head.formatValue(idx, values[idx]);
    }

    public String format(ColumnExpr col)
    {
        int idx = getFieldIndex(col);
        return head.formatValue(idx, values[idx]);
    }
    
    @Override
    public String toString()
    {
        StringBuilder b = new StringBuilder();
        for (int i=0; i<values.length; i++)
        {
            b.append(head.formatValue(i, values[i]));
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
