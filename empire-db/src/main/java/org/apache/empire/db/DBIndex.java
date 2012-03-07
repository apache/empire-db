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

import org.apache.empire.exceptions.InvalidArgumentException;

/**
 * This class handles the primary key for the tables.
 * The primary key contains one or more columns.
 * <P>
 * 
 *
 */
public class DBIndex extends DBObject
{
    private final static long serialVersionUID = 1L;
  
    // Index Types
    /**
     * SQL Standard index
     */
    public static final int STANDARD   = 0;
    
    /**
     * SQL Unique index
     */
    public static final int UNIQUE     = 1;
    
    /**
     * SQL Primary key index
     */
    public static final int PRIMARYKEY = 2;

    private String          name;
    private int             type;
    private DBColumn[]      columns;
    private DBTable         table;

    /**
     * Constructs a DBIndex object set the specified parameters to this object.
     * 
     * @param name the primary key name
     * @param type the primary key type (only PRIMARYKEY)
     * @param columns an array of one or more columns of the primary key
     */
    public DBIndex(String name, int type, DBColumn[] columns)
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
        if (table==null || !table.getIndexes().contains(this))
            throw new InvalidArgumentException("table", table);
        // table
        this.table = table;
    }

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
     * Returns the columnExpressions belonging to this index.
     * 
     * @return the columnExpressions belonging to this index
     */
    public DBExpr[] getExpressions()
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
        return (schema!=null) ? schema+"."+name : name;
    }
    
    /**
     * Returns the index type.
     * 
     * @return the type of this index ({@link #PRIMARYKEY}, {@link #UNIQUE}, {@link #STANDARD}) 
     */
    public int getType()
    {
        return type;
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

