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
 *
 */
public class DBExpressionIndex extends DBIndex
{
    private final static long serialVersionUID = 1L;
    
    private DBExpr[] columnExpressions;
    
    /**
     * Constructs a DBExpresionIndex
     * 
     * @param name the index name
     * @param unique true if the index has only unique values or false otherwise
     * @param columnExpressions an array of one or more column expressions of the index
     */
    public DBExpressionIndex(String name, DBIndexType type, DBExpr... columnExpressions)
    {
        super(name, type, null);
        // columnExpressions
        if (columnExpressions==null || columnExpressions.length==0)
            throw new InvalidArgumentException("columnExpressions", columnExpressions);
        // set expression
        this.columnExpressions = columnExpressions;
    }

    /**
     * Constructs a DBExpresionIndex
     * Overload for convenience
     */
    public DBExpressionIndex(String name, boolean unique, DBExpr... columnExpressions)
    {
        this(name, (unique ? DBIndexType.UNIQUE : DBIndexType.STANDARD), columnExpressions);
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public <T extends DBDatabase> T getDatabase()
    {
        return (T)(columnExpressions[0].getDatabase());
    }

    /**
     * Returns the columnExpressions belonging to this index.
     * 
     * @return the columnExpressions belonging to this index
     */
    @Override
    public DBExpr[] getExpressions()
    {
        return columnExpressions;
    }

    /**
     * Checks if this index contains the column col  
     * 
     * @param col the column
     * 
     * @return true if this index contains this column
     */
    @Override
    public boolean contains(DBColumn col)
    {
        for (DBExpr columnExpression : columnExpressions)
        {
            if (!(columnExpression instanceof DBColumnExpr))
                continue;
            if (col.equals(((DBColumnExpr) columnExpression).getUpdateColumn()))
                return true;
        }    
        return false;
    }

    /**
     * Gets the position of a specified DBColumn object.
     * 
     * @param col the column 
     * 
     * @return the position or -1 if the column was not found
     */
    @Override
    public int getColumnPos(DBColumn col)
    {
        return -1;
    }

}

