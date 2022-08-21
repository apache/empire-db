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

import java.util.Set;

import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBColumnExpr;
import org.apache.empire.db.DBCommand;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.DBJoinType;
import org.apache.empire.db.DBRowSet;
import org.apache.empire.db.DBSQLBuilder;

/**
 * This class is used for building a join expression of an SQL statement.
 * <P>
 * There is no need to explicitly create instances of this class.<BR>
 * Instead use @link {@link org.apache.empire.db.DBCommand#join(DBColumnExpr, DBColumn, DBJoinType)}
 * <P>
 *
 */
public class DBCrossJoinExpr extends DBJoinExpr
{
    // *Deprecated* private static final long serialVersionUID = 1L;
  
    protected DBRowSet left;
    protected DBRowSet right;
    
    /**
     * Constructs a new DBJoinExpr object initialize this object with
     * the left and right column and the data type of the join
     * expression.
     * 
     * @param left left value
     * @param right right value
     * @param type data type (JOIN_INNER, JOIN_LEFT or JOIN_RIGHT)
     */
    public DBCrossJoinExpr(DBRowSet left, DBRowSet right)
    {
        super(DBJoinType.FULL);
        this.left = left;
        this.right = right;
    }

    /**
     * Returns the current DBDatabase object.
     * 
     * @return the current DBDatabase object
     */
    @SuppressWarnings("unchecked")
    @Override
    public final DBDatabase getDatabase()
    {
        return left.getDatabase();
    }
    
    /**
     * returns the RowSet on the left of the join
     */
    @Override
    public DBRowSet getLeftTable()
    {
        return left;
    }
    
    /**
     * returns the RowSet on the right of the join
     */
    @Override
    public DBRowSet getRightTable()
    {
        return right;
    }
    
    /**
     * returns true if this join is using the given table or view or false otherwise
     */
    @Override
    public boolean isJoinOn(DBRowSet rowset)
    {
        if (rowset==null)
            return false;
        return rowset.equals(left) || rowset.equals(right);
    }
    
    /**
     * returns true if this join is using the given column or false otherwise
     */
    @Override
    public boolean isJoinOn(DBColumn column)
    {
        return false;
    }

    /**
     * Returns the left table name if the data type= JOIN_LEFT and returns
     * the right table if the data type= JOIN_RIGHT. If the
     * data type = JOIN_INNER the return value is null.
     * 
     * @return the current DBDatabase object
     */
    @Override
    public DBRowSet getOuterTable()
    {
        return null; // no outer table!
    }

    /**
     * This function swaps the left and the right statements of the join expression.
     */
    @Override
    public void reverse()
    { // Swap Type of Join
        DBRowSet swap = left;
        left = right;
        right = swap;
    }
    
    /**
     * Copy Command
     * @param cmd
     */
    @Override
    public DBJoinExpr copy(DBCommand newCmd)
    {   // copy
        // return new DBCrossJoinExpr(left, right);
        return this; // not necessary
    }

    /**
     * @see org.apache.empire.db.DBExpr#addReferencedColumns(Set)
     */
    @Override
    public void addReferencedColumns(Set<DBColumn> list)
    {
        // No referenced columns
    }

    /** Not allowed, this operation have to be done in the DBCommand object. */
    @Override
    public void addSQL(DBSQLBuilder sql, long context)
    {
        if ((context & CTX_NAME) != 0)
            left.addSQL(sql, CTX_DEFAULT | CTX_ALIAS);
        if ((context & CTX_VALUE) != 0)
        { // Join Type
            sql.append(" CROSS JOIN ");
            right.addSQL(sql, CTX_DEFAULT | CTX_ALIAS);
        }
    }

    /**
     * Compares two DBJoinExpr objects.
     * 
     * @param obj other DBJoinExpr object
     * @return true if the other DBJoinExpr object is equal to this object
     */
    @Override
    public boolean equals(Object obj)
    {
        if (!(obj instanceof DBCrossJoinExpr))
            return super.equals(obj);
        // object
        DBCrossJoinExpr other = (DBCrossJoinExpr) obj;
        if (left.equals(other.left) && right.equals(other.right))
            return true;
        // reversed
        if (left.equals(other.right) && right.equals(other.left))
            return true;
        // not equal
        return false;
    }

}