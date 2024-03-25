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

import org.apache.empire.commons.Unwrappable;
import org.apache.empire.data.DataType;
import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBColumnExpr;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.DBRowSet;
import org.apache.empire.db.DBSQLBuilder;
import org.w3c.dom.Element;

/**
 * This class allows column renaming in SQL.
 * <P>
 * There is no need to explicitly create instances of this class.<BR>
 * Instead use {@link DBColumnExpr#as(String) }
 */
public class DBParenthesisExpr extends DBColumnExpr implements Unwrappable<DBColumnExpr>
{
    // *Deprecated* private static final long serialVersionUID = 1L;
  
    private final DBColumnExpr wrapped;

    /**
     * Constructs a DBAliasExpr object combine the DBColumnExpr object with the alias name.
     *
     * @param expr an DBColumnExpr object, one column
     */
    public DBParenthesisExpr(DBColumnExpr expr)
    {
        // Check whether already a DBParenthesisExpr
        if (expr instanceof DBParenthesisExpr)
            this.wrapped = ((DBParenthesisExpr) expr).wrapped;
        else
            this.wrapped = expr;
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
        return wrapped.getDatabase();
    }
    
    /**
     * Returns the data type of the DBColumnExpr object.
     *
     * @return the data type
     */
    @Override
    public DataType getDataType()
    {
        return wrapped.getDataType();
    }

    /**
     * Returns the enum type of this Expression (if any)
     * @return the enum type or null
     */
    @Override
    public Class<Enum<?>> getEnumType()
    {
        return wrapped.getEnumType();
    }

    /**
     * This helper function returns the alias name.
     *
     * @return the alias name
     */
    @Override
    public String getName()
    {
        return wrapped.getName();
    }

    /**
     * Returns the underlying rowset
     */
    @Override
    public DBRowSet getRowSet()
    {
        return wrapped.getRowSet();
    }

    /**
     * Returns the DBColunm object.
     *
     * @return the DBColunm object
     */
    @Override
    public DBColumn getUpdateColumn()
    {
        return wrapped.getUpdateColumn();
    }
    
    /**
     * Indicates that we are actually an expression wrapper
     * @return true
     */
    @Override
    public boolean isWrapper()
    {   // Yep
        return true;
    }

    /**
     * Returns the underlying column expression.
     * @return the underlying column expression
     */
    @Override
    public DBColumnExpr unwrap()
    {
        return wrapped;
    }

    /**
     * Forward to expression
     *
     * @return false
     */
    @Override
    public boolean isAggregate()
    {
        return wrapped.isAggregate();
    }
    
    /**
     * Overrides the equals method
     * @return true if alias name and expression match
     */
    @Override
    public boolean equals(Object other)
    {
        if (other==this)
            return true;
        // Check for another expression
        if (other instanceof DBParenthesisExpr)
        {   // Compare expr
            DBParenthesisExpr otherExpr = ((DBParenthesisExpr)other);
            return wrapped.equals(otherExpr.wrapped);
        }
        return false;
    }

    /**
     * @see org.apache.empire.db.DBExpr#addReferencedColumns(Set)
     */
    @Override
    public void addReferencedColumns(Set<DBColumn> list)
    {
        wrapped.addReferencedColumns(list);
    }

    /**
     * Creates the SQL-Command adds the alias name to the SQL-Command.
     *
     * @param sql the SQL statment
     * @param context the current SQL-Command context
     */
    @Override
    public void addSQL(DBSQLBuilder sql, long context)
    {   // Append alias
        sql.append("(");
        wrapped.addSQL(sql, context); // |CTX_NOPARENTHESES
        sql.append(")");
    }
    
    /**
     * For Debugging
     */
    @Override
    public String toString()
    {
        StringBuilder b = new StringBuilder("(");
        b.append(wrapped.toString());
        b.append(")");
        return b.toString();
    }

    /**
     * This function set the alias name to the XML tag.
     * @return the XML tag (with the alias name)
     */
    @Override
    public Element addXml(Element parent, long flags)
    { // Set name to Alias
        Element field = wrapped.addXml(parent, flags);
        field.setAttribute("parenthesis", String.valueOf(true));
        return field;
    }
}
