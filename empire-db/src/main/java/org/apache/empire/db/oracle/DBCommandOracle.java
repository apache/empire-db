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

import java.util.ArrayList;
// Imports
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.empire.commons.StringUtils;
import org.apache.empire.data.DataType;
import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBColumnExpr;
import org.apache.empire.db.DBCommand;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.DBIndex;
import org.apache.empire.db.DBRowSet;
import org.apache.empire.db.DBTable;
import org.apache.empire.db.expr.column.DBAliasExpr;
import org.apache.empire.db.expr.column.DBValueExpr;
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
    private final static long serialVersionUID = 1L;
  
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
    public void limitRows(int limitRows)
    {
        // set limit
        this.limitRows = limitRows;
    }

    @Override
    public void skipRows(int skipRows)
    {
        if (skipRows<0)
            throw new InvalidArgumentException("skipRows", skipRows);
        // set skip
        this.skipRows = skipRows; 
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
    }

    @Override
    public Object[] getParamValues()
    {
        Object[] params = super.getParamValues();
        if (limitRows<0 || !getDatabase().isPreparedStatementsEnabled())
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
    
    /**
     * Creates an Oracle specific update statement.
     * If a join is required, this method creates a "MERGE INTO" expression 
     */
    @Override
    public synchronized String getUpdate()
    {
        // No Joins: Use Default
        if (joins==null || set==null)
            return getSimpleUpdate();
        else
            return getUpdateWithJoins();
    }

    protected String getSimpleUpdate()
    {
        resetParamUsage();
        if (set == null)
            return null;
        StringBuilder buf = new StringBuilder("UPDATE ");
        DBRowSet table =  set.get(0).getTable();
        long context = CTX_FULLNAME;
        // Optimizer Hint
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
        // done
        return buf.toString();
    }
    
    protected String getUpdateWithJoins()
    {
        // Generate Merge expression
        resetParamUsage();
        StringBuilder buf = new StringBuilder("MERGE INTO ");
        DBRowSet table =  set.get(0).getTable();
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
        Set<DBColumn> joinColumns = new HashSet<DBColumn>();
        updateJoin.addReferencedColumns(joinColumns);
        // using
        buf.append("\r\nUSING ");
        DBCommand inner = this.clone();
        inner.clearSelect();
        inner.clearOrderBy();
        for (DBColumn jcol : joinColumns)
        {   // Select join columns
            if (jcol.getRowSet()!=table)
                inner.select(jcol);
        }
        // find the source table
        DBColumnExpr left  = updateJoin.getLeft();
        DBColumnExpr right = updateJoin.getRight();
        DBRowSet source = right.getUpdateColumn().getRowSet();
        if (source==table)
            source = left.getUpdateColumn().getRowSet();
        // Add set expressions
        String sourceAliasPrefix = source.getAlias()+".";
        List<DBSetExpr> mergeSet = new ArrayList<DBSetExpr>(set.size());   
        for (DBSetExpr sex : set)
        {   // Select set expressions
            Object val = sex.getValue();
            if (val instanceof DBColumnExpr)
            {
                DBColumnExpr expr = ((DBColumnExpr)val);
                if (!(expr instanceof DBColumn) && !(expr instanceof DBAliasExpr))
                {   // rename column
                    String name = "COL_"+String.valueOf(mergeSet.size());
                    expr = expr.as(name);
                }
                // select
                inner.select(expr);
                // Name
                DBValueExpr NAME_EXPR = getDatabase().getValueExpr(sourceAliasPrefix+expr.getName(), DataType.UNKNOWN);
                mergeSet.add(sex.getColumn().to(NAME_EXPR));
            }
            else
            {   // add original
                mergeSet.add(sex);
            }
        }
        // remove join (if not necessary)
        if (inner.hasConstraintOn(table)==false)
            inner.removeJoinsOn(table);
        // add SQL for inner statement
        inner.addSQL(buf, CTX_DEFAULT);
        // add Alias
        buf.append(" ");
        buf.append(source.getAlias());
        buf.append("\r\nON (");
        left.addSQL(buf, CTX_DEFAULT);
        buf.append(" = ");
        right.addSQL(buf, CTX_DEFAULT);
        // Compare Expression
        if (updateJoin.getWhere() != null)
        {   buf.append(" AND ");
            updateJoin.getWhere().addSQL(buf, CTX_DEFAULT);
        }
        // Set Expressions
        buf.append(")\r\nWHEN MATCHED THEN UPDATE ");
        buf.append("\r\nSET ");
        addListExpr(buf, mergeSet, CTX_DEFAULT, ", ");
        // done
        return buf.toString();
    }
    
    /**
     * Creates an Oracle specific delete statement.
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