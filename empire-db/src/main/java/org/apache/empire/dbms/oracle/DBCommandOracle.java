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

import java.util.ArrayList;
import java.util.List;

import org.apache.empire.commons.StringUtils;
import org.apache.empire.data.DataType;
import org.apache.empire.db.DBCmdParamList;
import org.apache.empire.db.DBCmdParams;
import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBColumnExpr;
import org.apache.empire.db.DBCommand;
import org.apache.empire.db.DBIndex;
import org.apache.empire.db.DBRowSet;
import org.apache.empire.db.DBSQLBuilder;
import org.apache.empire.db.exceptions.NoPrimaryKeyException;
import org.apache.empire.db.expr.column.DBAliasExpr;
import org.apache.empire.db.expr.column.DBValueExpr;
import org.apache.empire.db.expr.compare.DBCompareExpr;
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
    public DBCommandOracle(DBMSHandlerOracle dbms, boolean autoPrepareStmt)
    {
        super(dbms, autoPrepareStmt);
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
     * @param sql the SQL statement
     */
    @Override
    public synchronized void getSelect(DBSQLBuilder sql)
    {        
        resetParamUsage();
        if (select == null)
            throw new ObjectNotValidException(this);
        // limit rows
        boolean usePreparedStatements = isPreparedStatementsEnabled();
        if (limitRows>=0)
        {   // add limitRows and skipRows wrapper
            sql.append("SELECT * FROM (");
            if (skipRows>0)
                sql.append("SELECT row_.*, rownum rownum_ FROM (");
        }
        // Prepares statement
        sql.append("SELECT ");
        if (StringUtils.isNotEmpty(optimizerHint))
        {   // Append an optimizer hint to the select statement e.g. SELECT /*+ RULE */
            sql.append("/*+ ").append(optimizerHint).append(" */ ");
        }
        if (selectDistinct)
            sql.append("DISTINCT ");
        // Add Select Expressions
        addListExpr(sql, select, CTX_ALL, ", ");
        // Join
        addFrom(sql);
        // Where
        addWhere(sql);
        // Connect By
        if (connectBy != null)
        {   // Add 'Connect By Prior' Expression
        	sql.append("\r\nCONNECT BY PRIOR ");
            connectBy.addSQL(sql, CTX_DEFAULT | CTX_NOPARENTHESIS);
            // Start With
            if (startWith != null)
            {	// Add 'Start With' Expression
            	sql.append("\r\nSTART WITH ");
                startWith.addSQL(sql, CTX_DEFAULT);
            }
        }
        // Grouping
        addGrouping(sql);
        // Order
        if (orderBy != null)
        { // Having
            if (connectBy != null)
                sql.append("\r\nORDER SIBLINGS BY ");
            else
                sql.append("\r\nORDER BY ");
            // Add List of Order By Expressions
            addListExpr(sql, orderBy, CTX_DEFAULT, ", ");
        }
        // limit rows end
        if (limitRows>=0)
        {   // add limitRows and skipRows constraints
            sql.append(") row_ WHERE rownum<=");
            sql.append(usePreparedStatements ? "?" : String.valueOf(skipRows+limitRows));
            if (skipRows>0)
            {   // add skip rows
                sql.append(") WHERE rownum_>");
                sql.append(usePreparedStatements ? "?" : String.valueOf(skipRows));
            }
        }
        completeParamUsage();
    }
    
    @Override
    public DBCmdParams getParams()
    {
        DBCmdParams params = super.getParams();
        if (limitRows<0 || !isPreparedStatementsEnabled())
            return params;
        // extended with limitRows and skipRows
        DBCmdParamList extended = new DBCmdParamList(params);
        extended.add(this, DataType.INTEGER, this.limitRows);
        if (skipRows>0)
            extended.add(this, DataType.INTEGER, this.skipRows);
        return extended;
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
    protected void addUpdateForTable(DBSQLBuilder sql, DBRowSet table)
    {
        // Optimizer Hint
        long context = CTX_FULLNAME;
        if (StringUtils.isNotEmpty(optimizerHint))
        {   // Append an optimizer hint to the select statement e.g. SELECT /*+ RULE */
            sql.append("/*+ ").append(optimizerHint).append(" */ ");
            // Append alias (if necessary)
            if (optimizerHint.contains(table.getAlias()))
                context |= CTX_ALIAS;
        }
        // table
        table.addSQL(sql, context);
        // Simple Statement
        context = CTX_NAME | CTX_VALUE;
        // Set Expressions
        sql.append("\r\nSET ");
        addListExpr(sql, set, context, ", ");
        // Add Where
        addWhere(sql, context);
    }
    
    @Override
    protected void addUpdateWithJoins(DBSQLBuilder sql, DBRowSet table)
    {
        // The update table
        DBColumn[] keyColumns = table.getKeyColumns();
        if (keyColumns==null || keyColumns.length==0)
            throw new NoPrimaryKeyException(table);
        // Generate Merge expression
        sql.reset(0);
        sql.append("MERGE INTO ");
        table.addSQL(sql, CTX_FULLNAME|CTX_ALIAS);
        // Using
        sql.append("\r\nUSING (");
        // Add set expressions
        List<DBColumnExpr> using = new ArrayList<DBColumnExpr>();
        // Add key columns
        for (DBColumn col : keyColumns)
            using.add(col);
        // Select Set-Expressions
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
                using.add(expr);
                // Name
                DBValueExpr NAME_EXPR = getDatabase().getValueExpr("q0."+expr.getName(), DataType.UNKNOWN);
                mergeSet.add(sex.getColumn().to(NAME_EXPR));
            }
            else
            {   // add original
                mergeSet.add(sex);
            }
        }
        // Add select
        sql.append("SELECT ");
        addListExpr(sql, using, CTX_ALL, ", ");
        // From clause
        addFrom(sql);
        // Add Where
        addWhere(sql);
        // Add Grouping
        addGrouping(sql);
        // on
        sql.append(") q0\r\nON (");
        for (DBColumn col : keyColumns)
        {   // compare 
            sql.append(" q0.");
            col.addSQL(sql, CTX_NAME);
            sql.append("=");
            sql.append(table.getAlias());
            sql.append(".");
            col.addSQL(sql, CTX_NAME);
        }
        // Set Expressions
        sql.append(")\r\nWHEN MATCHED THEN UPDATE ");
        sql.append("\r\nSET ");
        addListExpr(sql, mergeSet, CTX_DEFAULT, ", ");
    }
    
    /**
     * Creates an Oracle specific delete statement.
     * @return the delete SQL-Command
     */
    
    @Override
    protected void addDeleteForTable(DBSQLBuilder sql, DBRowSet table)
    {
        if (StringUtils.isNotEmpty(optimizerHint))
        {   // Append an optimizer hint to the select statement e.g. SELECT /*+ RULE */
            sql.append("/*+ ").append(optimizerHint).append(" */ ");
        }
        super.addDeleteForTable(sql, table);
    }
    
    @Override
    protected void addDeleteWithJoins(DBSQLBuilder sql, DBRowSet table)
    {
        if (StringUtils.isNotEmpty(optimizerHint))
        {   // Append an optimizer hint to the select statement e.g. SELECT /*+ RULE */
            sql.append("/*+ ").append(optimizerHint).append(" */ ");
        }
        super.addDeleteWithJoins(sql, table);
    }

}