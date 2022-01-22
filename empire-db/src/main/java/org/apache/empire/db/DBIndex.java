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
  
    public enum DBIndexType
    {
        STANDARD(false),
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
    public <T extends DBDatabase> T getDatabase()
    {
        return (table!=null) ? (T)table.getDatabase() : null;
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
     * @param columns
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
    public DBIndexType getType()
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

