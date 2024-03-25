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
package org.apache.empire.db.expr.compare;

import java.util.Set;

import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBCommand;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.DBRowSet;
import org.apache.empire.db.DBSQLBuilder;


/**
 * This class is used for combining two filter constraints by and / or operator<br>
 * <P>
 * There is no need to explicitly create instances of this class.<BR>
 * Instead use {@link DBCompareExpr#and(DBCompareExpr)} or {@link DBCompareExpr#or(DBCompareExpr)} 
 * <P>
 *
 */
public class DBCompareAndOrExpr extends DBCompareExpr
{
    // *Deprecated* private static final long serialVersionUID = 1L;
  
    protected final DBCompareExpr left;
    protected final DBCompareExpr right;
    protected final boolean       or;

    /**
     * Constructs a DBCompareColExpr object
     * 
     * @param left the left side of the expression
     * @param right the right side of the expression
     * @param or true for OR operation, false for AND
     */
    public DBCompareAndOrExpr(DBCompareExpr left, DBCompareExpr right, boolean or)
    {
        this.left = left;
        this.right = right;
        this.or = or;
    }

    public DBCompareExpr getLeft()
    {
        return left;
    }

    public DBCompareExpr getRight()
    {
        return right;
    }

    public boolean isOr()
    {
        return or;
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
     * Returns the underlying rowset containing this column
     */
    @Override
    public DBRowSet getRowSet()
    {
        DBRowSet rowset = left.getRowSet(); 
        return (rowset!=null ? rowset : right.getRowSet());
    }

    /**
     * Prepare function
     * @param cmd
     */
    @Override
    public void prepareCommand(DBCommand cmd) 
    {
        left.prepareCommand(cmd);
        right.prepareCommand(cmd);
    }

    /**
     * Copy Command
     * @param newCmd the new command object
     */
    @Override
    public DBCompareExpr copy(DBCommand newCmd)
    {
        return new DBCompareAndOrExpr(left.copy(newCmd), right.copy(newCmd), or);
    }

    /**
     * @see org.apache.empire.db.DBExpr#addReferencedColumns(Set)
     */
    @Override
    public void addReferencedColumns(Set<DBColumn> list)
    { // return all Columns
        left.addReferencedColumns(list);
        right.addReferencedColumns(list);
    }

    /**
     * Creates the SQL-Command sets the specified compare value 
     * (the varible boolOP) between the two DBCompareExpr objects.
     * 
     * @param sql the SQL statment
     * @param context the current SQL-Command context
     */
    @Override
    public void addSQL(DBSQLBuilder sql, long context)
    {
        // Name or Value Only ?
        if ((context & CTX_NAME )==0 ||
            (context & CTX_VALUE)==0)
        { // add both values separated by ","
            left.addSQL(sql, context);
            sql.append(",");
            right.addSQL(sql, context);
            return;
        }
        // Parenthesis
        boolean parenthesis = ((context & CTX_NOPARENTHESIS) == 0) && or;
        boolean nested = ((left instanceof DBCompareAndOrExpr) && ((DBCompareAndOrExpr)left).or==false);
        if (parenthesis)
            sql.append("(");
        if (parenthesis && nested)
            sql.append("(");
        // the left expression
        left.addSQL(sql, context);
        // Parenthesis
        if (parenthesis && nested)
            sql.append(")");
        // Combine operator
        sql.append((or ? " OR " : " AND "));
        // Parenthesis
        nested = ((right instanceof DBCompareAndOrExpr) && ((DBCompareAndOrExpr)right).or==false);
        if (parenthesis && nested)
            sql.append("(");
        // the right expression
        right.addSQL(sql, context);
        if (parenthesis && nested)
            sql.append(")");
        // Parenthesis
        if (parenthesis)
            sql.append(")");
    }
    
    /**
     * For Debugging
     */
    @Override
    public String toString()
    {
        StringBuilder b = new StringBuilder(left.toString());
        b.append((or ? " OR " : " AND "));
        b.append(right.toString());
        return b.toString();
    }
   
    /**
     * Returns whether the constraint should replace another one or not.
     * 
     * @return true it the constraints are mutually exclusive or false otherwise
     */
    @Override
    public boolean isMutuallyExclusive(DBCompareExpr other)
    {
        if (ObjectUtils.isWrapper(other))
        {   // unwrap
            other = ObjectUtils.unwrap(other);
        }
    	if (other instanceof DBCompareAndOrExpr)
    	{   // check other
    		DBCompareAndOrExpr otherExpr = (DBCompareAndOrExpr)other;
            if (left .isMutuallyExclusive(otherExpr.left) &&
                right.isMutuallyExclusive(otherExpr.right))
                return (this.or==otherExpr.or);
    	}
    	return false;
    }
	
	/**
	 * wraps the current expression with parenthesis. 
	 * 
	 * @return the new DBCompareColExpr object
	 */
	public DBCompareExpr parenthesis()
	{
		// create parenthesis Expression
		return new DBCompareParenthesisExpr(this);
	}
    
}