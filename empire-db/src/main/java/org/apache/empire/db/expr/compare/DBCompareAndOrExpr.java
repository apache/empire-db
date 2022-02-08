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

import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBDatabase;


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
     * @param buf the SQL statment
     * @param context the current SQL-Command context
     */
    @Override
    public void addSQL(StringBuilder buf, long context)
    {
        // Name or Value Only ?
        if ((context & CTX_NAME )==0 ||
            (context & CTX_VALUE)==0)
        { // add both values separated by ","
            left.addSQL(buf, context);
            buf.append(",");
            right.addSQL(buf, context);
            return;
        }
        // Parenthesis
        boolean parenthesis = ((context & CTX_NOPARENTHESES) == 0) && or;
        boolean nested = ((left instanceof DBCompareAndOrExpr) && ((DBCompareAndOrExpr)left).or==false);
        if (parenthesis)
            buf.append("(");
        if (parenthesis && nested)
            buf.append("(");
        // the left expression
        left.addSQL(buf, context);
        // Parenthesis
        if (parenthesis && nested)
            buf.append(")");
        // Combine operator
        buf.append((or ? " OR " : " AND "));
        // Parenthesis
        nested = ((right instanceof DBCompareAndOrExpr) && ((DBCompareAndOrExpr)right).or==false);
        if (parenthesis && nested)
            buf.append("(");
        // the right expression
        right.addSQL(buf, context);
        if (parenthesis && nested)
            buf.append(")");
        // Parenthesis
        if (parenthesis)
            buf.append(")");
    }
    
    /**
     * Returns wheter the constraint should replace another one or not.
     * 
     * @return true it the constraints are mutually exclusive or false otherwise
     */
    @Override
    public boolean isMutuallyExclusive(DBCompareExpr other)
    {
    	if (other instanceof DBCompareAndOrExpr)
    	{
    		DBCompareAndOrExpr o = (DBCompareAndOrExpr)other;
    		if (left.equals(o.left) && right.equals(o.right))
    			return true;
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
		return new DBParenthesisExpr(this);
	}
    
}