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

import org.apache.empire.data.DataType;
import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBColumnExpr;
import org.apache.empire.db.DBExpr;
import org.apache.empire.db.DBSQLBuilder;
import org.apache.empire.db.expr.column.DBAbstractFuncExpr;

public class PostgresFuncExpr extends DBAbstractFuncExpr
{
    // *Deprecated* private static final long serialVersionUID = 1L;

    protected final PostgresSqlPhrase  phrase;
    protected final Object[]     params;

    /**
     * @param expr the DBColumnExpr object
     * @param phrase the SQL-phrase
     * @param params an array of params which will be replaced in the template
     * @param dataType indicates the data type of the function result 
     */
    public PostgresFuncExpr(DBColumnExpr expr, PostgresSqlPhrase phrase, Object[] params, DataType dataType)
    {
        super(expr, phrase.isAggregate(), dataType);
        // Set Phrase and Params
        this.phrase = phrase;
        this.params = params;
    }
    
    @Override
    protected String getFunctionName()
    {
        return phrase.name();
    }

    /**
     * @see org.apache.empire.db.DBExpr#addReferencedColumns(Set)
     */
    @Override
    public void addReferencedColumns(Set<DBColumn> list)
    {
        super.addReferencedColumns(list);
        if (this.params==null)
            return;
        // Check params
        for (int i=0; i<this.params.length; i++)
        {   // add referenced columns
            if (params[i] instanceof DBExpr)
               ((DBExpr)params[i]).addReferencedColumns(list);
        }
    }

    /**
     * Creates the SQL-Command adds a function to the SQL-Command.
     * 
     * @param sql the SQL-Command
     * @param context the current SQL-Command context
     */
    @Override
    public void addSQL(DBSQLBuilder sql, long context)
    {
        // Add SQL
        super.addSQL(sql, phrase.getSQL(), params, context);
    }
    
}
