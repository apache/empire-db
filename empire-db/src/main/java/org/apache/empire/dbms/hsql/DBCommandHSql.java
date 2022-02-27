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
package org.apache.empire.dbms.hsql;

import java.util.ArrayList;
// Imports
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.empire.data.DataType;
import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBColumnExpr;
import org.apache.empire.db.DBCommand;
import org.apache.empire.db.DBRowSet;
import org.apache.empire.db.expr.column.DBAliasExpr;
import org.apache.empire.db.expr.column.DBValueExpr;
import org.apache.empire.db.expr.compare.DBCompareColExpr;
import org.apache.empire.db.expr.compare.DBCompareExpr;
import org.apache.empire.db.expr.join.DBColumnJoinExpr;
import org.apache.empire.db.expr.join.DBJoinExpr;
import org.apache.empire.db.expr.set.DBSetExpr;
import org.apache.empire.exceptions.ObjectNotValidException;

/**
 * This class handles the special features of an HSqlDB database.
 */
public class DBCommandHSql extends DBCommand
{
    // *Deprecated* private static final long serialVersionUID = 1L;
  
    protected int limitRows = -1;
    protected int skipRows  =  0;

    /**
     * Constructs an HSqlDB command object.
     */
    public DBCommandHSql(boolean autoPrepareStmt)
    {
        super(autoPrepareStmt);
    }

    @Override
    public DBCommandHSql limitRows(int limitRows)
    {
        // set limit
        this.limitRows = limitRows;
        return this;
    }

    @Override
    public DBCommandHSql skipRows(int skipRows)
    {
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
        
    @Override
    public void getSelect(StringBuilder buf)
    {   // call base class
        super.getSelect(buf);
        // add limit and offset
        if (limitRows>=0)
        {   buf.append("\r\nLIMIT ");
            buf.append(String.valueOf(limitRows));
            // Offset
            if (skipRows>0) 
            {   buf.append(" OFFSET ");
                buf.append(String.valueOf(skipRows));
            }    
        }
    }
    
    /**
     * Creates an update statement.
     * If a join is required, this method creates a "MERGE INTO" expression 
     */
    @Override
    public synchronized String getUpdate()
    {
        // No Joins: Use Default
        if (joins==null || set==null)
            return super.getUpdate();
        else
            return getUpdateWithJoins();
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
        DBRowSet outerTable = updateJoin.getOuterTable();
        if (outerTable==null)
            outerTable=table;
        for (DBColumn jcol : joinColumns)
        {   // Select join columns
            if (jcol.getRowSet().equals(outerTable)==false)
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
        // More constraints
        for (DBCompareExpr we : this.where) 
        {
            if (we instanceof DBCompareColExpr)
            {   // a compare column expression
                DBCompareColExpr cce = (DBCompareColExpr)we;
                DBColumn ccecol = cce.getColumn().getUpdateColumn();
                if (table.isKeyColumn(ccecol)&& !isSetColumn(ccecol))  
                {
                    buf.append(" AND ");
                    cce.addSQL(buf, CTX_DEFAULT);
                }
            }
            else
            {   // just add
                buf.append(" AND ");
                we.addSQL(buf, CTX_DEFAULT);
            }
        }
        // Set Expressions
        buf.append(")\r\nWHEN MATCHED THEN UPDATE ");
        buf.append("\r\nSET ");
        addListExpr(buf, mergeSet, CTX_DEFAULT, ", ");
        // done
        return buf.toString();
    }
        
    protected boolean isSetColumn(DBColumn col)
    {
        for (DBSetExpr se : this.set)
        {
            if (se.getColumn().equals(col))
                return true;
        }
        return false;
    }
}
