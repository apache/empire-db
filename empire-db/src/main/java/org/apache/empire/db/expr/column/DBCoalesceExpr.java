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
package org.apache.empire.db.expr.column;

import java.util.Set;

import org.apache.empire.commons.StringUtils;
import org.apache.empire.commons.Unwrappable;
import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBColumnExpr;
import org.apache.empire.db.DBExpr;
import org.apache.empire.db.DBSQLBuilder;
import org.apache.empire.dbms.DBSqlPhrase;

public class DBCoalesceExpr extends DBAbstractFuncExpr implements Unwrappable<DBColumnExpr>
{
    private final Object nullValue;
    
    public DBCoalesceExpr(DBColumnExpr expr, Object nullValue)
    {
        super(expr, false, expr.getDataType());
        // set the null value
        this.nullValue = nullValue;
    }

    /**
     * This is a transparent wrapper
     */
    @Override
    public boolean isWrapper()
    {   // yep
        return true;
    }

    @Override
    public DBColumnExpr unwrap()
    {
        return expr;
    }

    @Override
    protected String getFunctionName()
    {
        return StringUtils.EMPTY;
    }
    
    /**
     * Returns the column name.
     */
    @Override
    public String getName()
    {
        return expr.getName();
    }
    
    /**
     * Returns the column enum type
     */
    @Override
    public Class<Enum<?>> getEnumType()
    {
        return expr.getEnumType();
    }
    
    /**
     * Returns true if other is equal to this expression  
     */
    @Override
    public boolean equals(Object other)
    {
        if (other==this)
            return true;
        // Check Type
        if (other instanceof DBCoalesceExpr)
        {   // Compare expressions
            DBColumnExpr otherExpr = ((DBCoalesceExpr)other).expr;
            return this.expr.equals(otherExpr);
        }
        return false;
    }

    /**
     * @see org.apache.empire.db.DBExpr#addReferencedColumns(Set)
     */
    @Override
    public void addReferencedColumns(Set<DBColumn> list)
    {
        super.addReferencedColumns(list);
        // add referenced columns
        if (nullValue instanceof DBExpr)
            ((DBExpr)nullValue).addReferencedColumns(list);
    }

    @Override
    public void addSQL(DBSQLBuilder sql, long context)
    {
        // Get the template
        String template = getDbms().getSQLPhrase(DBSqlPhrase.SQL_FUNC_COALESCE);
        // Add SQL
        super.addSQL(sql, template, new Object[] { nullValue }, context);
    }

}
