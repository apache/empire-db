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
import org.apache.empire.db.DBCommand;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.DBJoinType;
import org.apache.empire.db.DBRowSet;
import org.apache.empire.db.DBSQLBuilder;
import org.apache.empire.db.expr.compare.DBCompareExpr;

public class DBColumnJoinExpr extends DBJoinExpr
{
    // *Deprecated* private static final long serialVersionUID = 1L;
  
    protected DBColumnExpr  left;
    protected DBColumnExpr  right;

    // Additional
    public DBCompareExpr compExpr; // should be final but still used by where()

    /**
     * Constructs a join expression based on two columns or column expressions
     * 
     * @param left left value
     * @param right right value
     * @param type data type (JOIN_INNER, JOIN_LEFT or JOIN_RIGHT)
     * @param addlConstraint additional constraint
     */
    public DBColumnJoinExpr(DBColumnExpr left, DBColumnExpr right, DBJoinType type, DBCompareExpr addlConstraint)
    {
        super(type);
        this.left = left;
        this.right = right;
        this.compExpr = addlConstraint;
    }

    public DBColumnJoinExpr(DBColumnExpr left, DBColumnExpr right, DBJoinType type)
    {
        this(left, right, type, null);
    }
    
    /**
     * Copy and concat constructor
     * 
     * @param joinExpr the original joinExpr
     * @param addlConstraint additional constraint
     */
    public DBColumnJoinExpr(DBColumnJoinExpr joinExpr, DBCompareExpr addlConstraint)
    {
        this(joinExpr.left, joinExpr.right, joinExpr.type, (joinExpr.compExpr!=null ? joinExpr.compExpr.and(addlConstraint) : addlConstraint));
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
        return left.getRowSet();
    }
    
    /**
     * returns the RowSet on the right of the join
     */
    @Override
    public DBRowSet getRightTable()
    {
        return right.getRowSet();
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
        if (column.equals(left.getUpdateColumn()) ||
            column.equals(right.getUpdateColumn()))
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
     * prepareCommand
     */
    @Override
    public void prepareCommand(DBCommand cmd)
    {
        if (compExpr!=null)
            compExpr.prepareCommand(cmd);
    }

    /**
     * Copy Command
     * @param newCmd the new command object
     */
    @Override
    public DBJoinExpr copy(DBCommand newCmd)
    {   // check additional compare expr
        if (this.compExpr==null)
            return this; // not necessary
        // Copy compareExpr 
        DBColumnJoinExpr join = new DBColumnJoinExpr(left, right, type, compExpr.copy(newCmd));       
        return join;
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
    {   // Set Compare Expression
        this.compExpr = expr;
    }

    /**
     * This function adds an additional constraint to the join.

     * @param expr the expression to add
     * @return the object itself
     */
    public DBColumnJoinExpr and(DBCompareExpr expr)
    {   // Add Compare Expression
        if (expr==null || expr.equals(this.compExpr))
            return this; // no change
        return new DBColumnJoinExpr(this, expr);
    }

    /**
     * This function adds an additional constraint to the join.
     * 
     * @param c1 the first column
     * @param c2 the second column
     * @return the object itself
     */
    public DBColumnJoinExpr and(DBColumnExpr c1, DBColumnExpr c2)
    {   // Add Compare Expression
        return and(c1.is(c2));
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
    public void addSQL(DBSQLBuilder sql, long context)
    {
        // left 
        if ((context & CTX_NAME) != 0)
            getLeftTable().addSQL(sql, CTX_DEFAULT | CTX_ALIAS);
        if ((context & CTX_VALUE) != 0)
        { // Join Type
            switch(type)
            {
                case LEFT:  sql.append(" LEFT JOIN ");break;
                case INNER: sql.append(" INNER JOIN ");break;
                case RIGHT: sql.append(" RIGHT JOIN ");break;
                case FULL:  sql.append(" FULL JOIN ");break;
                default:    sql.append(" JOIN "); // should not come here!
            }
            getRightTable().addSQL(sql, CTX_DEFAULT | CTX_ALIAS);
            // compare equal
            sql.append(" ON ");
            right.addSQL(sql, CTX_DEFAULT);
            sql.append(" = ");
            left.addSQL(sql, CTX_DEFAULT);
            // Compare Expression
            if (compExpr != null)
            {
                sql.append(" AND ");
                compExpr.addSQL(sql, CTX_DEFAULT);
            }
        }
    }
    
    /**
     * For Debugging
     */
    @Override
    public String toString()
    {
        StringBuilder b = new StringBuilder(left.toString());
        b.append("=");
        b.append(right.toString());
        if (this.compExpr!=null)
        {
            b.append(" AND ");
            b.append(this.compExpr.toString());
        }
        return b.toString();
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
