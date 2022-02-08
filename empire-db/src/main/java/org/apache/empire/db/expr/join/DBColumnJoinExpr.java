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

import java.util.HashSet;
import java.util.Set;

import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBColumnExpr;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.DBJoinType;
import org.apache.empire.db.DBRowSet;
import org.apache.empire.db.expr.compare.DBCompareExpr;
import org.apache.empire.exceptions.InvalidPropertyException;

public class DBColumnJoinExpr extends DBJoinExpr
{
    // *Deprecated* private static final long serialVersionUID = 1L;
  
    protected DBColumnExpr  left;
    protected DBColumnExpr  right;

    // Additional
    public DBCompareExpr compExpr = null;

    /**
     * Constructs a new DBJoinExpr object initialize this object with
     * the left and right column and the data type of the join
     * expression.
     * 
     * @param left left value
     * @param right right value
     * @param type data type (JOIN_INNER, JOIN_LEFT or JOIN_RIGHT)
     */
    public DBColumnJoinExpr(DBColumnExpr left, DBColumnExpr right, DBJoinType type)
    {
        super(type);
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
     * returns the left join expression
     */
    public DBColumnExpr getLeft()
    {
        return left;
    }

    /**
     * returns the right join expression
     */
    public DBColumnExpr getRight()
    {
        return right;
    }
    
    @Override
    public DBRowSet getLeftTable()
    {
        DBColumn col = left.getSourceColumn();
        if (col==null)
            throw new InvalidPropertyException("left", left);
        return col.getRowSet();
    }
    
    /**
     * returns the RowSet on the right of the join
     */
    @Override
    public DBRowSet getRightTable()
    {
        DBColumn col = right.getSourceColumn();
        if (col==null)
            throw new InvalidPropertyException("right", right);
        return col.getRowSet();
    }
    
    /**
     * returns true if this join is using the given table or view or false otherwise
     */
    @Override
    public boolean isJoinOn(DBRowSet rowset)
    {
        if (rowset==null)
            return false;
        // compare rowsets
        return rowset.equals(getLeftTable()) || rowset.equals(getRightTable());
    }
    
    /**
     * returns true if this join is using the given column or false otherwise
     */
    @Override
    public boolean isJoinOn(DBColumn column)
    {
        if (column==null)
            return false;
        // Check Update Columns
        if (column.equals(left.getSourceColumn()) ||
            column.equals(right.getSourceColumn()))
            return true;
        if (compExpr!=null)
        {   // Check expression
            HashSet<DBColumn> set = new HashSet<DBColumn>();
            compExpr.addReferencedColumns(set);
            return set.contains(column);
        }
        // not found
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
        switch(type)
        {
            case LEFT:  return getRightTable();
            case RIGHT: return getLeftTable();
            default:    return null; // no outer table!
        }
    }

    /**
     * This function swaps the left and the right statements of the join expression.
     */
    @Override
    public void reverse()
    { // Swap Type of Join
        DBColumnExpr swap = left;
        left = right;
        right = swap;
        type = DBJoinType.reversed(type); // (type * -1);
    }

    /**
     * Returns any additional constraints to the join
     * @return a compare expression containing additional constraints or null 
     */
    public DBCompareExpr getWhere()
    {
        return compExpr;
    }

    /**
     * This function adds an additional constraint to the join.
     * 
     * @param expr the compare expression
     */
    public void where(DBCompareExpr expr)
    { // Set Compare Expression
        compExpr = expr;
    }

    /**
     * This function adds an additional constraint to the join.
     * 
     * @param c1 the first column
     * @param c2 the second column
     * 
     * @return the object itself
     */
    public DBJoinExpr and(DBColumnExpr c1, DBColumnExpr c2)
    {   // Set Compare Expression
        if (compExpr==null)
            compExpr = c1.is(c2);
        else
            compExpr = compExpr.and(c1.is(c2));
        return this;
    }

    /**
     * @see org.apache.empire.db.DBExpr#addReferencedColumns(Set)
     */
    @Override
    public void addReferencedColumns(Set<DBColumn> list)
    {
        left.addReferencedColumns(list);
        right.addReferencedColumns(list);
        // Compare Expression
        if (compExpr != null)
            compExpr.addReferencedColumns(list);
    }

    /** Not allowed, this operation have to be done in the DBCommand object. */
    @Override
    public void addSQL(StringBuilder buf, long context)
    {
        // left 
        if ((context & CTX_NAME) != 0)
            getLeftTable().addSQL(buf, CTX_DEFAULT | CTX_ALIAS);
        if ((context & CTX_VALUE) != 0)
        { // Join Type
            switch(type)
            {
                case LEFT:  buf.append(" LEFT JOIN ");break;
                case INNER: buf.append(" INNER JOIN ");break;
                case RIGHT: buf.append(" RIGHT JOIN ");break;
                case FULL:  buf.append(" FULL JOIN ");break;
                default:    buf.append(" JOIN "); // should not come here!
            }
            getRightTable().addSQL(buf, CTX_DEFAULT | CTX_ALIAS);
            // compare equal
            buf.append(" ON ");
            right.addSQL(buf, CTX_DEFAULT);
            buf.append(" = ");
            left.addSQL(buf, CTX_DEFAULT);
            // Compare Expression
            if (compExpr != null)
            {
                buf.append(" AND ");
                compExpr.addSQL(buf, CTX_DEFAULT);
            }
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
        if (!(obj instanceof DBColumnJoinExpr))
            return super.equals(obj);
        // object
        DBColumnJoinExpr other = (DBColumnJoinExpr) obj;
        if (left.equals(other.left) && right.equals(other.right) && type == other.type)
            return true;
        // reversed
        if (left.equals(other.right) && right.equals(other.left) && type == DBJoinType.reversed(other.type))
            return true;
        // not equal
        return false;
    }

}
