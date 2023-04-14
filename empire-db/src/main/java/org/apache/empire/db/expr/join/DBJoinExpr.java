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
package org.apache.empire.db.expr.join;

import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBCommand;
import org.apache.empire.db.DBExpr;
import org.apache.empire.db.DBJoinType;
import org.apache.empire.db.DBRowSet;

/**
 * This class is used for building a join expression of an SQL statement.
 * 
 * There is no need to explicitly create instances of this class.
 */
public abstract class DBJoinExpr extends DBExpr
{
    // *Deprecated* private static final long serialVersionUID = 1L;
  
    protected DBJoinType    type;

    /**
     * Constructs a new DBJoinExpr object initialize this object with
     * the left and right column and the data type of the join
     * expression.
     * 
     * @param type the join type (JOIN_INNER, JOIN_LEFT or JOIN_RIGHT)
     */
    protected DBJoinExpr(DBJoinType type)
    {
        this.type = type;
    }

    /**
     * returns the join type for this join
     */
    public DBJoinType getType()
    {
        return type;
    }
    
    /**
     * alters the join type for this join
     */
    public void setType(DBJoinType type)
    {
        this.type = type;
    }

    /**
     * returns the RowSet on the left of the join
     */
    public abstract DBRowSet getLeftTable();
    
    /**
     * returns the RowSet on the right of the join
     */
    public abstract DBRowSet getRightTable();
    
    /**
     * returns true if this join is using the given table or view or false otherwise
     */
    public abstract boolean isJoinOn(DBRowSet rowset);
    
    /**
     * returns true if this join is using the given column or false otherwise
     */
    public abstract boolean isJoinOn(DBColumn column);

    /**
     * Returns the left table name if the data type= JOIN_LEFT and returns
     * the right table if the data type= JOIN_RIGHT. If the
     * data type = JOIN_INNER the return value is null.
     * 
     * @return the current DBDatabase object
     */
    public abstract DBRowSet getOuterTable();
    
    /**
     * This function swaps the left and the right statements of the join expression.
     */
    public abstract void reverse();
    
    /**
     * internally used for preapred statement generation
     * @param cmd
     */
    public abstract void prepareCommand(DBCommand cmd); 

    /**
     * Copy Command
     * @param newCmd the new command object
     */
    public abstract DBJoinExpr copy(DBCommand newCmd);

}