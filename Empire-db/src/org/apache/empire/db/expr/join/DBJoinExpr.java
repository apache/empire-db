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

import java.util.*;

import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBColumnExpr;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.DBExpr;
import org.apache.empire.db.DBJoinType;
import org.apache.empire.db.DBRowSet;
import org.apache.empire.db.expr.compare.DBCompareExpr;


/**
 * This class is used for building a join expression of an SQL statement.
 * <P>
 * There is no need to explicitly create instances of this class.<BR>
 * Instead use @link {@link org.apache.empire.db.DBCommand#join(DBColumnExpr, DBColumn, DBJoinType)}
 * <P>
 * @author ESTEAM software <A TARGET="esteam" HREF="http://www.esteam.de">www.esteam.de </A>
 */
public class DBJoinExpr extends DBExpr
{
    protected DBColumnExpr  left;
    protected DBColumnExpr  right;
    protected DBJoinType    type;

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
    public DBJoinExpr(DBColumnExpr left, DBColumnExpr right, DBJoinType type)
    {
        this.left = left;
        this.right = right;
        this.type = type;
    }

    /**
     * Returns the current DBDatabase object.
     * 
     * @return the current DBDatabase object
     */
    @Override
    public DBDatabase getDatabase()
    {
        return left.getDatabase();
    }

    public DBColumnExpr getLeft()
    {
        return left;
    }

    public DBColumnExpr getRight()
    {
        return right;
    }

    public DBJoinType getType()
    {
        return type;
    }
    
    public boolean isJoinOn(DBRowSet rowset)
    {
        if (rowset==null)
            return false;
        DBColumn l = (left !=null ? left .getUpdateColumn() : null);
        DBColumn r = (right!=null ? right.getUpdateColumn() : null);
        DBRowSet rsl = (l!=null ? l.getRowSet() : null);
        DBRowSet rsr = (r!=null ? r.getRowSet() : null);
        return rowset.equals(rsl) || rowset.equals(rsr);
    }

    /**
     * Returns the left table name if the data type= JOIN_LEFT and returns
     * the right table if the data type= JOIN_RIGHT. If the
     * data type = JOIN_INNER the return value is null.
     * 
     * @return the current DBDatabase object
     */
    public DBRowSet getOuterTable()
    {
        switch(type)
        {
            case LEFT:  return right.getUpdateColumn().getRowSet();
            case RIGHT: return left .getUpdateColumn().getRowSet();
            default:    return null; // no outer table!
        }
    }

    /**
     * This function swaps the left and the right statements of the join expression.
     */
    public void reverse()
    { // Swap Type of Join
        DBColumnExpr swap = left;
        left = right;
        right = swap;
        type = DBJoinType.reversed(type); // (type * -1);
    }

    /**
     * This function adds an additional constraint to the join.
     */
    public void where(DBCompareExpr expr)
    { // Set Compare Expression
        compExpr = expr;
    }

    /**
     * This function adds an additional constraint to the join.
     */
    public DBCompareExpr and(DBColumnExpr c1, DBColumnExpr c2)
    { // Set Compare Expression
        compExpr = c1.is(c2);
        return compExpr;
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
        if ((context & CTX_NAME) != 0)
            left.getUpdateColumn().getRowSet().addSQL(buf, CTX_DEFAULT | CTX_ALIAS);
        if ((context & CTX_VALUE) != 0)
        { // Join Type
            switch(type)
            {
                case LEFT:  buf.append(" LEFT JOIN ");break;
                case INNER: buf.append(" INNER JOIN ");break;
                case RIGHT: buf.append(" RIGHT JOIN ");break;
                default:    buf.append(" JOIN "); // should not come here!
            }
            right.getUpdateColumn().getRowSet().addSQL(buf, CTX_DEFAULT | CTX_ALIAS);
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
        if (obj==null || obj.getClass()!=getClass())
            return super.equals(obj);
        // object
        DBJoinExpr other = (DBJoinExpr) obj;
        if (left.equals(other.left) && right.equals(other.right) && type == other.type)
            return true;
        // reversed
        if (left.equals(other.right) && right.equals(other.left) && type == DBJoinType.reversed(other.type))
            return true;
        // not equal
        return false;
    }

}