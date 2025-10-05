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

import org.apache.empire.commons.StringUtils;

/**
 * This class handles the primary key for the tables.
 * The primary key contains one or more columns.
 */
public class DBIndex extends DBObject
{
    // *Deprecated* private static final long serialVersionUID = 1L;
  
    public enum DBIndexType
    {
        STANDARD(false),
        FULLTEXT(false), // non-unique optimized for text search (if supported)
        UNIQUE(true),
        UNIQUE_ALLOW_NULL(true),
        PRIMARY_KEY(true);
        
        private final boolean unique;
        
        DBIndexType(boolean unique)
        {
            this.unique = unique;
        }

        public boolean isUnique()
        {
            return unique;
        }
    }

    private String          name;
    private DBIndexType     type;
    private DBColumn[]      columns;
    private DBTable         table;
    private String          options; // additional index type options

    /**
     * Constructs a DBIndex object set the specified parameters to this object.
     * 
     * @param name the primary key name
     * @param type the primary key type (only PRIMARYKEY)
     * @param columns an array of one or more columns of the primary key
     */
    public DBIndex(String name, DBIndexType type, DBColumn[] columns)
    {
        this.name = name;
        this.type = type;
        this.columns = columns;
    }

    /** 
     * returns the table this index belongs to.
     * Valid only if the index has been added to a table (DBTable.addIndex)
     * @return the corresponding table
     */
    public DBTable getTable()
    {
        return table;
    }

    /** 
     * internally used to set the table for this index.
     * The table must be added to the table's index list beforehand 
     * @param table
     */
    void setTable(DBTable table)
    {
        this.table = table;
    }
    
    @SuppressWarnings("unchecked")
    @Override
	public DBDatabase getDatabase()
    {
        return (table!=null) ? table.getDatabase() : null;
    }

    /**
     * Returns the primary key name.
     * 
     * @return the primary key name
     */
    public String getName()
    {
        return name;
    }

    /**
     * Returns the columns belonging to this index.
     * 
     * @return the columns belonging to this index
     */
    public DBColumn[] getColumns()
    {
        return columns;
    }

    /**
     * Returns the columns belonging to this index.
     * @param index the index of the desired column 
     * @return the columns belonging to this index
     */
    public DBColumn getColumn(int index)
    {
        return columns[index];
    }
    
    /**
     * Returns the number of columns belonging to this index.
     * 
     * @return the number of columns belonging to this index
     */
    public int getColumnCount()
    {
        return columns.length;
    }
    
    /**
     * checks whether the columns of this index match the supplied columns
     * @param columns the columns
     * @return true if columns match or false otherwise
     */
    public boolean compareColumns(DBColumn[] columns)
    {
        if (columns==null || columns.length!=this.columns.length)
            return false;
        // Compare all columns
        for (int i=0; i<columns.length; i++)
        {
            if (!columns[i].equals(this.columns[i]))
                return false;
        }
        return true;
    }

    /**
     * Returns the columnExpressions belonging to this index.
     * 
     * @return the columnExpressions belonging to this index
     */
    public DBColumnExpr[] getExpressions()
    {
        return columns;
    }

    /**
     * Returns the full qualified table name.
     * 
     * @return the full qualified table name
     */
    public String getFullName()
    {
        String  schema = getDatabase().getSchema();
        return (schema!=null) ? StringUtils.concat(schema, ".", name) : name;
    }
    
    /**
     * Returns the index type.
     * 
     * @return the type of this index 
     */
    public DBIndexType getType()
    {
        return type;
    }

    /**
     * Returns additional database specific index options
     * @return the index type options
     */
    public String getOptions()
    {
        return options;
    }

    /**
     * Sets additional database specific index options
     * @param using the index type options
     */
    public DBIndex setOptions(String options)
    {
        this.options = options;
        return this;
    }

    /**
     * Checks if this index contains the column col  
     * 
     * @param col the column
     * 
     * @return true if this index contains this column
     */
    public boolean contains(DBColumn col)
    {
        for (int i = 0; i < columns.length; i++)
            if (col.equals(columns[i]))
                return true;
        return false;
    }

    /**
     * Gets the position of a specified DBColumn object.
     * 
     * @param col the column 
     * 
     * @return the position or -1 if the column was not found
     */
    public int getColumnPos(DBColumn col)
    {
        for (int i = 0; i < columns.length; i++)
            if (col.equals(columns[i]))
                return i;
        return -1;
    }

}

