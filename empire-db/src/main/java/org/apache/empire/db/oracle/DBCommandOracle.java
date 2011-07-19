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
package org.apache.empire.db.oracle;

// Imports
import org.apache.empire.EmpireException;
import org.apache.empire.commons.Errors;
import org.apache.empire.db.DBCommand;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.DBTable;
import org.apache.empire.db.expr.compare.DBCompareExpr;

/**
 * This class handles the special features of an oracle database.
 * 
 *
 */
public class DBCommandOracle extends DBCommand
{
    private final static long serialVersionUID = 1L;
  
    // Oracle Connect By / Start With
    protected DBCompareExpr connectBy  = null;
    protected DBCompareExpr startWith  = null;
    // optimizerHint
    protected String        optimizerHint  = null;
    protected OracleRowNumExpr	rowNumExpr = null;

    /**
     * Constructs an oracle command object.
     * 
     * @see org.apache.empire.db.DBCommand
     * 
     * @param db the oracle database object this command belongs to
     */
    public DBCommandOracle(DBDatabase db)
    {
        super(db);
    }

    public String getOptimizerHint()
    {
        return optimizerHint;
    }

    public void setOptimizerHint(String optimizerHint)
    {
        this.optimizerHint = optimizerHint;
    }

    /**
     * @see DBCommand#clear()
     */
    @Override
    public void clear()
    {
        super.clear();
        // Clear oracle specific properties
        clearConnectBy();
        optimizerHint = null;
    }

    /**
     * Clears the connectBy Expression.
     */
    public void clearConnectBy()
    {
        connectBy = startWith = null;
    }

    public void connectByPrior(DBCompareExpr expr)
    {
        this.connectBy = expr;
    }

    public void startWith(DBCompareExpr expr)
    {
        this.startWith = expr;
    }
    
    @Override
    public void limitRows(int numRows)
    {
    	if (rowNumExpr==null)
    		rowNumExpr = new OracleRowNumExpr(getDatabase());
    	// Add the constraint
    	where(rowNumExpr.isLessOrEqual(numRows));
    }
     
    @Override
    public void clearLimit()
    {
    	if (rowNumExpr!=null)
    		removeWhereConstraintOn(rowNumExpr);
    	// constraint removed
    	rowNumExpr = null;
    }

    /**
     * Creates the SQL statement the special characteristics of
     * the Oracle database are supported.
     * 
     * @param buf the SQL statement
     * @return true if the creation was successful
     */
    @Override
    public synchronized void getSelect(StringBuilder buf)
    {
        resetParamUsage();
        if (select == null)
            throw new EmpireException(Errors.ObjectNotValid, getClass().getName()); // invalid!
        // Prepares statement
        buf.append("SELECT ");
        if (optimizerHint != null)
        {
            // Append an optimizer hint to the select statement e.g. SELECT /*+ RULE */
            buf.append("/*+ ").append(optimizerHint).append(" */ ");
        }
        if (selectDistinct)
            buf.append("DISTINCT ");
        // Add Select Expressions
        addListExpr(buf, select, CTX_ALL, ", ");
        // Join
        addFrom(buf);
        // Where
        addWhere(buf);
        // Connect By
        if (connectBy != null)
        {   // Add 'Connect By Prior' Expression
        	buf.append("\r\nCONNECT BY PRIOR ");
            connectBy.addSQL(buf, CTX_DEFAULT | CTX_NOPARENTHESES);
            // Start With
            if (startWith != null)
            {	// Add 'Start With' Expression
            	buf.append("\r\nSTART WITH ");
                startWith.addSQL(buf, CTX_DEFAULT);
            }
        }
        // Grouping
        addGrouping(buf);
        // Order
        if (orderBy != null)
        { // Having
            if (connectBy != null)
                buf.append("\r\nORDER SIBLINGS BY ");
            else
                buf.append("\r\nORDER BY ");
            // Add List of Order By Expressions
            addListExpr(buf, orderBy, CTX_DEFAULT, ", ");
        }
    }
    
    /**
     * Creates the delete SQL-Command.
     * 
     * @return the delete SQL-Command
     */
    @Override
    public synchronized String getDelete(DBTable table)
    {
        resetParamUsage();
        StringBuilder buf = new StringBuilder("DELETE ");
        if (optimizerHint != null)
        {   // Append an optimizer hint to the select statement e.g. SELECT /*+ RULE */
            buf.append("/*+ ").append(optimizerHint).append(" */ ");
        }
        buf.append("FROM ");
        table.addSQL(buf, CTX_FULLNAME);
        // Set Expressions
        if (where != null || having != null)
        { // add where condition
            buf.append("\r\nWHERE ");
            if (where != null)
                addListExpr(buf, where, CTX_NAME|CTX_VALUE, " AND ");
        }
        return buf.toString();
    }

}