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

import org.apache.empire.data.DataType;
import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBColumnExpr;
import org.apache.empire.db.DBSQLBuilder;
import org.apache.empire.dbms.DBSqlPhrase;

/**
 * DBConcatExpression
 * @author doebele
 */
public class DBConcatFuncExpr extends DBAbstractFuncExpr
{
    private final DBColumnExpr first;
    private final String separator;
    private final DBColumnExpr[] others;

    /**
     * create concat expression
     * @param first the first expression
     * @param separator the separator
     * @param others the remaining expressions
     */
    public DBConcatFuncExpr(DBColumnExpr first, String separator, DBColumnExpr... others)
    {
        super(first, false, DataType.VARCHAR);
        // remember
        this.first = first;
        this.separator = separator;
        this.others = others;
    }

    /**
     * create concat expression
     * @param first the first expression
     * @param others the remaining expressions
     */
    public DBConcatFuncExpr(DBColumnExpr first, DBColumnExpr... others)
    {
        this(first, null, others);
    }

    @Override
    public String getName()
    { // Get the expression name
        String name = first.getName();
        for (int i=0; i<others.length; i++)
        {   // add other names
            if (others[i] instanceof DBValueExpr)
                continue; // Ignore literals
            name += "_";
            name += others[i].getName();
        }
        return name;
    }

    @Override
    protected String getFunctionName()
    {
        return "CONCAT";
    }

    @Override
    public DBColumn getUpdateColumn()
    {
        return null;
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
        if (other instanceof DBConcatFuncExpr)
        {   // Compare
            DBConcatFuncExpr otherFunc = (DBConcatFuncExpr)other;
            // Expression must match
            if (!first.equals(otherFunc.first))
                return false;
            // all parts must match
            for (int i=0; i<others.length; i++)
                if (!others[i].equals(otherFunc.others[i]))
                    return false;
            // OK
            return true;
        }
        return false;
    }

    @Override
    public void addSQL(DBSQLBuilder sql, long context)
    {
        // get template
        String template = getDatabase().getDbms().getSQLPhrase(DBSqlPhrase.SQL_FUNC_CONCAT);
        int placeholder = template.indexOf('?');
        if (placeholder>=0)
            sql.append(template.substring(0, placeholder));
        // concat 
        first.addSQL(sql, context);
        for (int i=0; i<others.length; i++)
        {
            if (placeholder>=0)
                sql.append(", ");
            else 
                sql.append(template);
            // insert separator string
            if (separator!=null && separator.length()>0)
            {   // add a separator
                sql.append("'");
                sql.append(separator);
                sql.append("', ");
            }
            others[i].addSQL(sql, context);
        }
        if (placeholder>=0)
            sql.append(template.substring(placeholder+1));
    }

}
