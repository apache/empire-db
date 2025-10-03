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

// Java
import org.apache.empire.commons.StringUtils;
import org.apache.empire.commons.Unwrappable;
import org.apache.empire.data.DataType;
import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBColumnExpr;
import org.apache.empire.db.DBCommand;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.DBExpr;
import org.apache.empire.db.DBRowSet;
import org.apache.empire.db.DBSQLBuilder;
import org.apache.empire.dbms.DBMSHandler;
import org.apache.empire.dbms.DBSqlPhrase;
import org.w3c.dom.Element;

/**
 * This class allows column renaming in SQL.
 * <P>
 * There is no need to explicitly create instances of this class.<BR>
 * Instead use {@link DBColumnExpr#as(String) }
 */
public class DBAliasExpr extends DBColumnExpr
    implements DBPreparable, Unwrappable<DBColumnExpr>
{
    // *Deprecated* private static final long serialVersionUID = 1L;
  
    private final DBColumnExpr expr;
    private final String       alias;

    /**
     * Constructs a DBAliasExpr object combine the DBColumnExpr object with the alias name.
     *
     * @param expr an DBColumnExpr object, one column
     * @param alias the alias name of the column
     */
    public DBAliasExpr(DBColumnExpr expr, String alias)
    {
        // Check whether already a AliasExpr
        if (expr instanceof DBAliasExpr)
            this.expr = ((DBAliasExpr) expr).expr;
        else
            this.expr = expr;
        // Set alias name
        this.alias = alias; // .toUpperCase() Why?;
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
     * Returns the data type of the DBColumnExpr object.
     *
     * @return the data type
     */
    @Override
    public DataType getDataType()
    {
        return expr.getDataType();
    }

    /**
     * Returns the enum type of this Expression (if any)
     * @return the enum type or null
     */
    @Override
    public Class<Enum<?>> getEnumType()
    {
        return expr.getEnumType();
    }

    /**
     * This helper function returns the alias name.
     *
     * @return the alias name
     */
    @Override
    public String getName()
    {
        return alias;
    }

    /**
     * Returns the underlying rowset
     */
    @Override
    public DBRowSet getRowSet()
    {
        return expr.getRowSet();
    }

    /**
     * Returns the DBColunm object.
     *
     * @return the DBColunm object
     */
    @Override
    public DBColumn getUpdateColumn()
    {
        return expr.getUpdateColumn();
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
        return expr;
    }

    /**
     * Forward to expression
     *
     * @return false
     */
    @Override
    public boolean isAggregate()
    {
        return expr.isAggregate();
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
        // column
        if (other instanceof DBColumn)
        {   // Compare with a DBColumn name (added 2024-07-18 EMPIREDB-434)
            String otherName = ((DBColumn)other).getName();
            if (alias.equalsIgnoreCase(otherName))
                 return true;
        }
        // Check for another Alias Expression
        else if (other instanceof DBAliasExpr)
        {   // Compare with another alias expression (changed 2024-07-18 EMPIREDB-434)
            DBAliasExpr otherExpr = ((DBAliasExpr)other);
            return alias.equalsIgnoreCase(otherExpr.getName());
        }        
        return false;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.empire.db.expr.column.DBPreparable#prepareParams(org.apache.empire.db.DBCommand, org.apache.empire.db.DBExpr)
     */
    @Override
    public void prepareParams(DBCommand cmd, DBExpr parent) 
    {
        // forward?
        if (expr instanceof DBPreparable)
            ((DBPreparable)expr).prepareParams(cmd, parent);
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
     * Creates the SQL-Command adds the alias name to the SQL-Command.
     *
     * @param sql the SQL statment
     * @param context the current SQL-Command context
     */
    @Override
    public void addSQL(DBSQLBuilder sql, long context)
    { // Append alias
        if((context & CTX_ALIAS)!=0)
        {   // Add the column expression
            expr.addSQL(sql, (context & ~CTX_ALIAS));
            // Rename
            DBMSHandler dbms = getDatabase().getDbms();
            String asExpr = dbms.getSQLPhrase(DBSqlPhrase.SQL_RENAME_COLUMN);
            if (asExpr!=null)
            {
                sql.append(asExpr);
                dbms.appendObjectName(sql, alias, null);
            }
        } 
        else
        {
            expr.addSQL(sql, context);
        }
    }

    /**
     * This function set the alias name to the XML tag.
     *
     * @return the XML tag (with the alias name)
     */
    @Override
    public Element addXml(Element parent, long flags)
    { // Set name to Alias
        Element field = expr.addXml(parent, flags);
        if (field != null)
        {   // Set Name
            if (field.hasAttribute("name"))
                field.setAttribute("source", StringUtils.toString(field.getAttribute("name")));
            field.setAttribute("name", alias);
        }
        return field;
    }

    /**
     * Overrides the toString method.
     *
     * @return the alias name
     */
    @Override
    public String toString()
    {
        return alias;
    }
}