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

import org.apache.empire.db.DBColumnExpr;
import org.apache.empire.db.DBCommand;
import org.apache.empire.db.DBExpr;
import org.apache.empire.db.DBRowSet;

/**
 * This class is a common base class for all SQL filter constraints classes<br>
 */
public abstract class DBCompareExpr extends DBExpr
{
  // *Deprecated* private static final long serialVersionUID = 1L;
  
	public abstract boolean isMutuallyExclusive(DBCompareExpr other);

	public abstract boolean isConstraintOn(DBColumnExpr colExpr);
	
	/**
	 * Creates a new DBCompareAndOrExpr object.
	 * 
	 * @param expr the right part of the AND expression
	 * @return the and compare expression
	 */
	public DBCompareAndOrExpr and(DBCompareExpr expr)
	{
		// " AND "
		return new DBCompareAndOrExpr(this, expr, false);
	}

	/**
	 * Create a new DBCompareAndOrExpr object.
	 * @param expr the right part of the OR expression
	 * @return the or compare expression
	 */
	public DBCompareAndOrExpr or(DBCompareExpr expr)
	{
		// " OR "
		return new DBCompareAndOrExpr(this, expr, true);
	}

	/**
	 * Creates a sql-expression for the not() function.
	 * 
	 * @return the new DBCompareColExpr object
	 */
	public DBCompareExpr not()
	{
		return new DBCompareNotExpr(this);
	}

    /**
     * Returns the underlying rowset containing this column
     * For functions involving none or more than one physical column this function return the first one
     * @return a column used for this expression
     */
    public abstract DBRowSet getRowSet();
	
	/**
	 * internally used for preapred statement generation
     * @param cmd the command
	 */
	public abstract void prepareCommand(DBCommand cmd); 
    
    /**
     * internally used for command cloning
     * @param newCmd the new command object
     */
    public abstract DBCompareExpr copy(DBCommand newCmd); 

}
