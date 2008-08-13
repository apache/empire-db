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
 * @author ESTEAM software <A TARGET="esteam" HREF="http://www.esteam.de">www.esteam.de </A>
 */
public class DBIndex extends DBObject
{
    // Index Types
    public static final int STANDARD   = 0;
    public static final int UNIQUE     = 1;
    public static final int PRIMARYKEY = 2;

    private String          name;
    private int             type;
    private DBColumn[]      columns;

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

    @Override
    public DBDatabase getDatabase()
    {
        return columns[0].getDatabase();
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
     * Returns the primary key type (only PRIMARYKEY).
     */
    public int getType()
    {
        return type;
    }

    /**
     * Returns true if a specified DBColumn object exits in the internal DBColumn vector.
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
     */
    public int getColumnPos(DBColumn col)
    {
        for (int i = 0; i < columns.length; i++)
            if (col.equals(columns[i]))
                return i;
        return -1;
    }

}

