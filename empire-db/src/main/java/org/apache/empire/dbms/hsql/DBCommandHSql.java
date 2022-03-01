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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.empire.data.DataType;
import org.apache.empire.db.DBCmdParam;
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
}
