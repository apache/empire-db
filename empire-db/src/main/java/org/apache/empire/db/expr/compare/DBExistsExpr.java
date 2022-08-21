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
import org.apache.empire.db.DBCommandExpr;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.DBSQLBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class is used for building up the SQL-Command for the EXISTS syntax.
 * <P>
 * 
 *
 */
public class DBExistsExpr extends DBCompareExpr
{
    // *Deprecated* private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(DBExistsExpr.class);
  
    public final DBCommandExpr cmd;
    public final DBCompareExpr compareExpr;

    /**
     * Constructs a DBExistsExpr object set the specified parameters to this object.
     * 
     * @param cmd the DBCommandExpr object
     */
    public DBExistsExpr(DBCommandExpr cmd)
    {
        this(cmd, null);
    }

    /**
     * Constructs a DBExistsExpr object set the specified parameters to this object.
     * 
     * @param cmd the DBCommandExpr object
     * @param compareExpr The expression to append to the end of the exists statement
     */
    public DBExistsExpr(DBCommandExpr cmd, DBCompareExpr compareExpr)
    {
        this.cmd = cmd;
        this.compareExpr = compareExpr;
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
        return cmd.getDatabase();
    }

    /**
     * Prepare function
     * @param cmd
     */
    @Override
    public void prepareCommand(DBCommand cmd) 
    {
        if (compareExpr!=null)
            compareExpr.prepareCommand(cmd);
    }

    /**
     * Copy Command
     * @param cmd
     */
    @Override
    public DBCompareExpr copy(DBCommand newCmd)
    {
        if (compareExpr==null)
            return this;
        return new DBExistsExpr(cmd, compareExpr.copy(newCmd));
    }

    /**
     * @see org.apache.empire.db.DBExpr#addReferencedColumns(Set)
     */
    @Override
    public void addReferencedColumns(Set<DBColumn> list)
    {
        // nothing to do
    }

    /**
     * Creates the SQL-Command.
     * 
     * @param sql the SQL-Command
     * @param context the current SQL-Command context (CTX_DEFAULT, CTX_SELECT, CTX_NAME, CTX_VALUE)
     */
    @Override
    public void addSQL(DBSQLBuilder sql, long context)
    {
        // Name Only ?
        if ((context & CTX_VALUE) == 0)
        {
            log.warn("cannot add name only of exits expression");
            return;
        }
        // Value Only ?
        if ((context & CTX_NAME) == 0)
        {
            log.warn("cannot add value only of exits expression");
            return;
        }
        sql.append(" exists (");
        cmd.getSelect(sql);

        if (compareExpr != null)
        {
            if (cmd instanceof DBCommand && ((DBCommand) cmd).getWhereConstraints() == null)
            {
                sql.append(" where ");
            } 
            else
            {
                sql.append(" and ");
            }
            sql.append("(");
            compareExpr.addSQL(sql, context);
            sql.append(") ");
        }
        sql.append(") ");
    }

    /**
     * Returns the subquery params if the compare expression
     * @return the subquery params or null
    @Override
    public Object[] getSubqueryParams()
    {
        // none
        return cmd.getParamValues();
    }
     */
   
    /**
     * Returns wheter the constraint should replace another one or not.
     * @return true it the constraints are mutually exclusive or false otherwise
     */
    @Override
    public boolean isMutuallyExclusive(DBCompareExpr other)
    {
    	if (other instanceof DBExistsExpr)
    	{
    		DBExistsExpr o = (DBExistsExpr)other;
    		if (cmd.equals(o.cmd) && compareExpr.equals(o.compareExpr))
    			return true;
    	}
    	return false;
    }
    
}