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
package org.apache.empire.dbms.oracle;

import java.util.List;

import org.apache.empire.commons.StringUtils;
import org.apache.empire.db.DBCommand;
import org.apache.empire.db.DBIndex;
import org.apache.empire.db.DBRowSet;
import org.apache.empire.db.expr.compare.DBCompareExpr;
import org.apache.empire.db.expr.join.DBColumnJoinExpr;
import org.apache.empire.db.expr.join.DBJoinExpr;
import org.apache.empire.db.expr.set.DBSetExpr;
import org.apache.empire.exceptions.InvalidArgumentException;
import org.apache.empire.exceptions.ObjectNotValidException;

/**
 * This class handles the special features of an oracle database.
 * 
 *
 */
public class DBCommandOracle extends DBCommand
{
    // *Deprecated* private static final long serialVersionUID = 1L;
  
    // Oracle Connect By / Start With
    protected DBCompareExpr connectBy  = null;
    protected DBCompareExpr startWith  = null;
    // optimizerHint
    protected String        optimizerHint  = null;
    // protected OracleRowNumExpr	rowNumExpr = null;
    protected int limitRows = -1;
    protected int skipRows  =  0;

    /**
     * Constructs an oracle command object.
     * 
     * @see org.apache.empire.db.DBCommand
     * 
     * @param db the oracle database object this command belongs to
     */
    public DBCommandOracle(boolean autoPrepareStmt)
    {
        super(autoPrepareStmt);
    }

    public String getOptimizerHint()
    {
        return optimizerHint;
    }

    public void setOptimizerHint(String optimizerHint)
    {
        this.optimizerHint = optimizerHint;
    }

    public void setOptimizerIndexHint(DBIndex index)
    {
        if (index==null || index.getTable()==null)
            throw new InvalidArgumentException("index", index);
        // Set Index Hint
        String tableAlias = index.getTable().getAlias();
        String indexName  = index.getName();
        String indexHint  = "INDEX ("+tableAlias+" "+indexName+")";
        if (StringUtils.isNotEmpty(this.optimizerHint) && this.optimizerHint.indexOf(indexHint)<0)
            this.optimizerHint = this.optimizerHint + " " + indexHint;
        else
            this.optimizerHint = indexHint;
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
    public DBCommandOracle limitRows(int limitRows)
    {
        // set limit
        this.limitRows = limitRows;
        return this;
    }

    @Override
    public DBCommandOracle skipRows(int skipRows)
    {
        if (skipRows<0)
            throw new InvalidArgumentException("skipRows", skipRows);
        // set skip
        this.skipRows = skipRows;
        return this;
    }
     
    @Override
    public void clearLimit()
    {
    	// remove skip and limit
        this.limitRows = -1;
        this.skipRows  =  0;
    }

    /**
     * Creates an Oracle specific select statement
     * that supports special features of the Oracle DBMS
     * like e.g. CONNECT BY PRIOR
     * @param buf the SQL statement
     */
    @Override
    public synchronized void getSelect(StringBuilder buf)
    {        
        resetParamUsage();
        if (select == null)
            throw new ObjectNotValidException(this);
        // limit rows
        boolean usePreparedStatements = isPreparedStatementsEnabled();
        if (limitRows>=0)
        {   // add limitRows and skipRows wrapper
            buf.append("SELECT * FROM (");
            if (skipRows>0)
                buf.append("SELECT row_.*, rownum rownum_ FROM (");
        }
        // Prepares statement
        buf.append("SELECT ");
        if (StringUtils.isNotEmpty(optimizerHint))
        {   // Append an optimizer hint to the select statement e.g. SELECT /*+ RULE */
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
        // limit rows end
        if (limitRows>=0)
        {   // add limitRows and skipRows constraints
            buf.append(") row_ WHERE rownum<=");
            buf.append(usePreparedStatements ? "?" : String.valueOf(skipRows+limitRows));
            if (skipRows>0)
            {   // add skip rows
                buf.append(") WHERE rownum_>");
                buf.append(usePreparedStatements ? "?" : String.valueOf(skipRows));
            }
        }
        completeParamUsage();
    }

    @Override
    public Object[] getParamValues()
    {
        Object[] params = super.getParamValues();
        if (limitRows<0 || !isPreparedStatementsEnabled())
            return params;
        // add limit and skip params
        int newSize = (params!=null ? params.length : 0)+(skipRows>0 ? 2 : 1);
        Object[] newParams = new Object[newSize];
        // copy params
        for (int i=0; params!=null && i<params.length; i++)
            newParams[i]=params[i];
        // set skip
        if (skipRows>0)
            newParams[--newSize]=skipRows;
        // set limit
        newParams[--newSize]=skipRows+limitRows;    
        return newParams;
    }

    @Override
    protected void addUpdateForTable(StringBuilder buf, DBRowSet table)
    {
        // Optimizer Hint
        long context = CTX_FULLNAME;
        if (StringUtils.isNotEmpty(optimizerHint))
        {   // Append an optimizer hint to the select statement e.g. SELECT /*+ RULE */
            buf.append("/*+ ").append(optimizerHint).append(" */ ");
            // Append alias (if necessary)
            if (optimizerHint.contains(table.getAlias()))
                context |= CTX_ALIAS;
        }
        // table
        table.addSQL(buf, context);
        // Simple Statement
        context = CTX_NAME | CTX_VALUE;
        // Set Expressions
        buf.append("\r\nSET ");
        addListExpr(buf, set, context, ", ");
        // Add Where
        addWhere(buf, context);
    }
    
    @Override
    protected void addUpdateWithJoins(StringBuilder buf, DBRowSet table)
    {
        // Generate Merge expression
        buf.setLength(0);
        buf.append("MERGE INTO ");
        table.addSQL(buf, CTX_FULLNAME|CTX_ALIAS);
        // join (only one allowed yet)
        DBColumnJoinExpr updateJoin = null;
        for (DBJoinExpr jex : joins)
        {   // The join
            if (!(jex instanceof DBColumnJoinExpr))
                continue;
            if (jex.isJoinOn(table)==false)
                continue;
            // found the join
            updateJoin = (DBColumnJoinExpr)jex;
            break;
        }
        if (updateJoin==null)
            throw new ObjectNotValidException(this);
        // using
        DBMergeCommand merge = createMergeCommand();
        List<DBSetExpr> mergeSet = merge.addUsing(buf, table, updateJoin);
        // Set Expressions
        buf.append(")\r\nWHEN MATCHED THEN UPDATE ");
        buf.append("\r\nSET ");
        addListExpr(buf, mergeSet, CTX_DEFAULT, ", ");
    }
    
    /**
     * Creates an Oracle specific delete statement.
     * @return the delete SQL-Command
     */
    
    @Override
    protected void addDeleteForTable(StringBuilder buf, DBRowSet table)
    {
        if (StringUtils.isNotEmpty(optimizerHint))
        {   // Append an optimizer hint to the select statement e.g. SELECT /*+ RULE */
            buf.append("/*+ ").append(optimizerHint).append(" */ ");
        }
        super.addDeleteForTable(buf, table);
    }
    
    @Override
    protected void addDeleteWithJoins(StringBuilder buf, DBRowSet table)
    {
        if (StringUtils.isNotEmpty(optimizerHint))
        {   // Append an optimizer hint to the select statement e.g. SELECT /*+ RULE */
            buf.append("/*+ ").append(optimizerHint).append(" */ ");
        }
        super.addDeleteWithJoins(buf, table);
    }

}