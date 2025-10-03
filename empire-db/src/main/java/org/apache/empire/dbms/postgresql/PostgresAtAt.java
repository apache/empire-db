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
package org.apache.empire.dbms.postgresql;

import java.util.Set;

import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBColumnExpr;
import org.apache.empire.db.DBCommand;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.DBExpr;
import org.apache.empire.db.DBRowSet;
import org.apache.empire.db.DBSQLBuilder;
import org.apache.empire.db.expr.compare.DBCompareExpr;

/**
 * PostgresAtAt
 * create a Postgres @@ comparator 
 */
public class PostgresAtAt extends DBCompareExpr
{
    private final DBColumnExpr left;
    private final DBColumnExpr right;

    public PostgresAtAt(DBColumnExpr left, DBColumnExpr right)
    {
        this.left = left;
        this.right = right;
    }

    @SuppressWarnings("unchecked")
    @Override
    public DBDatabase getDatabase()
    {
        return this.left.getDatabase();
    }

    @Override
    public void prepareParams(DBCommand cmd, DBExpr parent)
    {
        /* nothing */
    }

    @Override
    public DBCompareExpr copy(DBCommand newCmd)
    {
        return new PostgresAtAt(this.left, this.right);
    }

    @Override
    public boolean isMutuallyExclusive(DBCompareExpr other)
    {
        if (ObjectUtils.isWrapper(other))
        {   // unwrap
            other = ObjectUtils.unwrap(other);
        }
        if (other instanceof PostgresAtAt)
        {   // compare
            PostgresAtAt otherAtAt = (PostgresAtAt)other;
            return this.left.equals(otherAtAt.left) &&
                   this.right.equals(otherAtAt.right);
        }
        return false;
    }

    @Override
    public boolean isConstraintOn(DBColumnExpr colExpr)
    {
        // compare columns
        if (ObjectUtils.compareEqual(left, colExpr) ||
            ObjectUtils.compareEqual(right, colExpr))
            return true;
        // not equal
        return false;
    }
    
    @Override
    public DBRowSet getRowSet()
    {
        DBRowSet rowset = left.getRowSet(); 
        return (rowset!=null ? rowset : right.getRowSet());
    }
    
    @Override
    public void addReferencedColumns(Set<DBColumn> list)
    {
        // forward
        this.left.addReferencedColumns(list);
        this.right.addReferencedColumns(list);
    }

    @Override
    public void addSQL(DBSQLBuilder sql, long context)
    {
        this.left.addSQL(sql, context);
        sql.append(" @@ ");
        this.right.addSQL(sql, context);
    }
}
