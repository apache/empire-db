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
import org.apache.empire.db.DBColumnExpr;
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
     * @param first
     * @param separator
     * @param others
     */
    public DBConcatFuncExpr(DBColumnExpr first, String separator, DBColumnExpr... others)
    {
        super(first, null, false, DataType.VARCHAR);
        // remember
        this.first = first;
        this.separator = separator;
        this.others = others;
    }

    /**
     * create concat expression
     * @param first
     * @param others
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
        { // add other names
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
    public void addSQL(StringBuilder buf, long context)
    {
        // get template
        String template = getDatabase().getDbms().getSQLPhrase(DBSqlPhrase.SQL_FUNC_CONCAT);
        int placeholder = template.indexOf('?');
        if (placeholder>=0)
            buf.append(template.substring(0, placeholder));
        // concat 
        first.addSQL(buf, context);
        for (int i=0; i<others.length; i++)
        {
            if (placeholder>=0)
                buf.append(", ");
            else 
                buf.append(template);
            // insert separator string
            if (separator!=null && separator.length()>0)
            {   // add a separator
                buf.append("'");
                buf.append(separator);
                buf.append("', ");
            }
            others[i].addSQL(buf, context);
        }
        if (placeholder>=0)
            buf.append(template.substring(placeholder+1));
    }

}
