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
package org.apache.empire.db.expr.order;

import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBColumnExpr;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.DBExpr;

import java.util.Set;

public class DBOrderByExpr extends DBExpr
{
    // *Deprecated* private static final long serialVersionUID = 1L;
  
    protected final DBColumnExpr expr;
    protected boolean desc;

    /**
     * Construct a new DBOrderByInfo object set the specified
     * parameters to this object.
     * 
     * @param expr the column 
     * @param desc set true for descending or false for ascending
     */
    public DBOrderByExpr(DBColumnExpr expr, boolean desc)
    {
        this.expr = expr;
        this.desc = desc;
    }
    
    public DBColumnExpr getColumnExpr()
    {
        return expr;
    }
    
    public DBColumn getColumn()
    {
        return expr.getUpdateColumn();
    }

    public boolean isDescending()
    {
        return desc;
    }

    public void setDescending(boolean descending)
    {
        desc = descending;
    }

    /*
     * @see org.apache.empire.db.DBExpr#getDatabase()
     */
    @Override
    public final <T extends DBDatabase> T getDatabase()
    {
        return expr.getDatabase();
    }

    /*
     * @see org.apache.empire.db.DBExpr#addReferencedColumns(Set)
     */
    @Override
    public void addReferencedColumns(Set<DBColumn> list)
    {
        expr.addReferencedColumns(list);
    }

    /**
     * Adds a column expression to the orderBy clause followed by the desc keyword if the order should be descending 
     * 
     * @param buf the SQL-Command
     * @param context the current SQL-Command context
     */
    @Override
    public void addSQL(StringBuilder buf, long context)
    {   // Set SQL-Order By
        expr.addSQL(buf, context);
        // only need to add DESC as default is ASC
        if (desc)
        {
            buf.append(" DESC");
        }
    }

}
