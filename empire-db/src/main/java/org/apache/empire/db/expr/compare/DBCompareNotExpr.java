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

import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBCommand;
import org.apache.empire.db.DBDatabase;


/**
 * This class is used for defining filter constraints based on a column expression in SQL<br>
 * <P>
 * There is no need to explicitly create instances of this class.<BR>
 * Instead use {@link DBCompareExpr#not()}<BR>
 * <P>
 *
 */
public class DBCompareNotExpr extends DBCompareExpr
{
    // *Deprecated* private static final long serialVersionUID = 1L;
  
    public final DBCompareExpr expr;

    /**
     * Constructs a DBCompareColExpr object
     * 
     * @param expr the compare expression to negate
     */
    public DBCompareNotExpr(DBCompareExpr expr)
    {
        this.expr = expr;
    }

    /**
     * Returns the current DBDatabase object.
     * 
     * @return the current DBDatabase object
     */
    @SuppressWarnings("unchecked")
    @Override
    public final DBDatabase getDatabase()
    {
        return expr.getDatabase();
    }

    /**
     * Prepare function
     * @param cmd
     */
    @Override
    public void prepareCommand(DBCommand cmd) 
    {
        expr.prepareCommand(cmd);
    }

    /**
     * Copy Command
     * @param cmd
     */
    @Override
    public DBCompareExpr copy(DBCommand newCmd)
    {
        return new DBCompareNotExpr(expr.copy(newCmd));
    }

    /**
     * @see org.apache.empire.db.DBExpr#addReferencedColumns(Set)
     */
    @Override
    public void addReferencedColumns(Set<DBColumn> list)
    { 
        expr.addReferencedColumns(list);
    }

    /**
     * Creates the SQL-Command sets the specified compare value
     * (the varible boolOP) between the two DBCompareExpr objects.
     * 
     * @param buf the SQL statment
     * @param context the current SQL-Command context
     */
    @Override
    public void addSQL(StringBuilder buf, long context)
    {
        // Name Only ?
        if ((context & CTX_VALUE)==0)
        { // add both values separated by ","
            expr.addSQL(buf, context);
            return;
        }
        // add SQL
        buf .append(" NOT(");
        expr.addSQL(buf, context);
        buf .append(" )");
    }
    
    /**
     * Returns wheter the constraint should replace another one or not.
     * 
     * @return true it the constraints are mutually exclusive or false otherwise
     */
    @Override
    public boolean isMutuallyExclusive(DBCompareExpr other)
    {
    	return false;
    }
    
}