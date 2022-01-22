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

import org.apache.empire.db.DBColumnExpr;
import org.apache.empire.db.DBJoinType;
import org.apache.empire.db.DBQuery;
import org.apache.empire.db.DBRowSet;
import org.apache.empire.db.expr.compare.DBCompareAndOrExpr;
import org.apache.empire.db.expr.compare.DBCompareColExpr;
import org.apache.empire.db.expr.compare.DBCompareExpr;
import org.apache.empire.db.expr.compare.DBParenthesisExpr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is used for building a join expression of an SQL statement.
 * <P>
 * There is no need to explicitly create instances of this class.<BR>
 * Instead use @link {@link org.apache.empire.db.DBCommand#join(DBRowSet, DBCompareExpr, DBJoinType)}
 * <P>
 *
 */
public class DBCompareJoinExpr extends DBColumnJoinExpr
{
    // *Deprecated* private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(DBCompareJoinExpr.class);
    
    protected final DBCompareExpr cmp;
    
    private static DBColumnExpr findFirstColumn(DBCompareExpr expr)
    {
        // DBParenthesisExpr
        if (expr instanceof DBParenthesisExpr)
            return findFirstColumn(((DBParenthesisExpr)expr).getWrapped());
        // DBCompareORExpr
        if (expr instanceof DBCompareAndOrExpr)
            return findFirstColumn(((DBCompareAndOrExpr)expr).getLeft());
        // Get Colum Expr
        if (expr instanceof DBCompareColExpr)
            return ((DBCompareColExpr)expr).getColumnExpr();
        // Error
        log.error("Unknown class found for building a valid JOIN Expression");
        return null;
    }
    
    private static DBColumnExpr firstRsetColumn(DBRowSet rset)
    {
        if (rset instanceof DBQuery)
           return ((DBQuery)rset).getQueryColumns()[0];
        else 
           return rset.getColumn(0);
    }
    
    /**
     * Constructs a new DBJoinExprEx
     * 
     * @param rset the rowset
     * @param cmp the compare expression
     * @param joinType the join type
     */
    public DBCompareJoinExpr(DBRowSet rset, DBCompareExpr cmp, DBJoinType joinType)
    {
        super(firstRsetColumn(rset), findFirstColumn(cmp), joinType);
        this.cmp = cmp;
    }
    
    public DBCompareExpr getJoinConstraint()
    {
        return cmp;
    }

    @Override
    public void addSQL(StringBuilder buf, long context)
    {
        // left 
        if ((context & CTX_NAME) != 0)
            getLeftTable().addSQL(buf, CTX_DEFAULT | CTX_ALIAS);
        if ((context & CTX_VALUE) != 0)
        { // Join Type
            switch(type)
            {
                case LEFT:  buf.append(" LEFT JOIN ");break;
                case INNER: buf.append(" INNER JOIN ");break;
                case RIGHT: buf.append(" RIGHT JOIN ");break;
                default:    buf.append(" JOIN "); // should not come here!
            }
            getRightTable().addSQL(buf, CTX_DEFAULT | CTX_ALIAS);
            // compare equal
            buf.append(" ON ");
            cmp.addSQL(buf, CTX_DEFAULT);
            // Compare Expression
            if (compExpr != null)
            {
                buf.append(" AND ");
                compExpr.addSQL(buf, CTX_DEFAULT);
            }
        }
    }
}