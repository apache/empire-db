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

import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.commons.Options;
import org.apache.empire.commons.StringUtils;
import org.apache.empire.data.Column;
import org.apache.empire.data.ColumnExpr;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.DBObject;

public class DataListHead implements Serializable
{
    private static final long serialVersionUID = 1L;
    // private static final Logger log  = LoggerFactory.getLogger(DataListHead.class);
    
    protected final ColumnExpr[] columns;
    
    protected String columnSeparator = "\t";

    /**
     * Constructs a DataListHead based on an DataListEntry constructor
     * @param constructor the DataListEntry constructor
     * @param columns the list entry columns
     */
    public DataListHead(ColumnExpr[] columns) 
    {
        this.columns = columns;
    }
    
    public ColumnExpr[] getColumns()
    {
        return columns; 
    }

    public int getColumnIndex(ColumnExpr column)
    {
        // find
        int i = ObjectUtils.indexOf(columns, column);
        if (i>=0)
            return i;
        // Not found, try by name
        return getColumnIndex(column.getName());
    }
    
    public int getColumnIndex(String columnName)
    {
        for (int i=0; i<columns.length; i++)
            if (columnName.equalsIgnoreCase(columns[i].getName()))
                return i; 
        // not found
        return -1;
    }
    
    /**
     * Returns the database instance associated with this DataList (if any)
     * The database is extracted from the column list 
     * @param dbClass the desired subclass of DBDatabase
     * @return the database instance or null if no Database instance of this type could be found
     */
    @SuppressWarnings("unchecked")
    public <T extends DBDatabase> T getDatabase(Class<T> dbClass)
    {
        for (int i=0; i<columns.length; i++)
        {
            if (columns[i] instanceof DBObject)
            {
                DBDatabase db = ((DBObject)columns[i]).getDatabase();
                if (db!=null && dbClass.isAssignableFrom(db.getClass()))
                    return (T)db;
            }
        }
        return null;
    }
    
    /**
     * Returns the value of a column as a formatted text
     * This converts the value to a string if necessary and performs an options lookup
     * To customize conversion please override convertToString()
     * @param column the column for which to get the formatted value
     * @param value the value to format
     * @return the formatted value
     */
    public String getText(int idx, Object value)
    {   // find text
        String text;
        ColumnExpr column = columns[idx];
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
        // Done
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
    
}
