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

import java.util.Set;

import org.apache.empire.commons.Unwrappable;
import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBCommand;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.DBSQLBuilder;

/**
 * This class wraps an existing compare expression with parenthesis.
 * <P>
 */
public class DBCompareParenthesisExpr extends DBCompareExpr implements Unwrappable<DBCompareExpr>
{
    // *Deprecated* private static final long serialVersionUID = 1L;
    private final DBCompareExpr wrapped;
    
    public DBCompareParenthesisExpr(DBCompareExpr expr)
    {
        this.wrapped = expr;
    }

    @Override
    public boolean isWrapper()
    {
        return true;
    }

    @Override
    public DBCompareExpr unwrap()
    {
        return wrapped;
    }

    @SuppressWarnings("unchecked")
    @Override
    public final DBDatabase getDatabase()
    {
        return wrapped.getDatabase();
    }
    
    @Override
    public boolean isMutuallyExclusive(DBCompareExpr other)
    {
        return wrapped.isMutuallyExclusive(other);
    }

    @Override
    public void prepareCommand(DBCommand cmd) 
    {
        wrapped.prepareCommand(cmd);
    }

    @Override
    public DBCompareExpr copy(DBCommand newCmd)
    {
        return new DBCompareParenthesisExpr(wrapped.copy(newCmd));
    }

    @Override
    public void addReferencedColumns(Set<DBColumn> list)
    {
        wrapped.addReferencedColumns(list);
    }

    @Override
    public void addSQL(DBSQLBuilder sql, long context)
    {
        sql.append("(");
        wrapped.addSQL(sql, context|CTX_NOPARENTHESIS);
        sql.append(")");
    }
}
