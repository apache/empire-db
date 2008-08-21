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
package org.apache.empire.db;

// Java
import java.util.Set;

import org.apache.empire.commons.Options;
import org.apache.empire.data.Column;
import org.apache.empire.db.expr.set.DBSetExpr;
import org.w3c.dom.Element;


/**
 * This is the base class for all database columns that have a physical representation.
 * i.e. either table or view columns (DBTableColumn oder DBViewColumn)
 * <p>
 * The column object describes a database column and thus provides metadata.
 * Other non data model specific metadata may be added through attributes.
 *
 * @see org.apache.empire.db.DBTableColumn
 * @see org.apache.empire.db.DBView.DBViewColumn
 *
 *
 */
public abstract class DBColumn extends DBColumnExpr
    implements Column
{
    // Predefined column attributes
    public static final String DBCOLATTR_MANDATORY = "mandatory";
    public static final String DBCOLATTR_READONLY  = "readonly";

    // basic data
    protected final DBRowSet   rowset;
    protected final String     name;
    protected String           comment;

    /**
     * Constructs a DBColumn object and set the specified parameters to this object.
     *
     * @param rowset the rowset (i.e. Table or View) that this column belongs to
     * @param name the column name
     */
    protected DBColumn(DBRowSet rowset, String name)
    {
        this.rowset = rowset;
        this.name = name;
        this.comment = null;
    }

    /**
     * Returns the size of the column.
     *
     * @return Returns the size of the column
     */
    public abstract double getSize();

    /**
     * Returns true if the column is required.
     *
     * @return Returns true if the column is required
     */
    public abstract boolean isRequired();

    /**
     * Returns true if the column is read-only.
     *
     * @return Returns true if the column is read-only
     */
    public abstract boolean isReadOnly();


    public abstract boolean checkValue(Object value);

    @Override
    public abstract Element addXml(Element parent, long flags);

    /**
     * @return the current DBDatabase object
     */
    @Override
    public DBDatabase getDatabase()
    {
        return (rowset!=null ? rowset.getDatabase() : null);
    }

    /**
     * @see org.apache.empire.db.DBExpr#addReferencedColumns(Set)
     */
    @Override
    public void addReferencedColumns(Set<DBColumn> list)
    {
        list.add(this);
    }

    /**
     * Adds the colunm name to the SQL-Command. <br>
     * This can be either a qualified or unqualified name depending on the context.
     *
     * @param buf the SQL statment
     * @param context the current SQL-Command context
     */
    @Override
    public void addSQL(StringBuilder buf, long context)
    { // Alias verwenden wenn nicht select

        if ((context & CTX_FULLNAME) != 0 && rowset != null)
        { // Fully Qualified Name
            buf.append(rowset.getAlias());
            buf.append(".");
        }
        buf.append(name);
    }

    /**
     * Returns this object.
     *
     * @return this object
     */
    @Override
    public DBColumn getUpdateColumn()
    {
        return this;
    }

    /**
     * Always returns false since DBColumns cannot be aggregates.
     *
     * @return false
     */
    @Override
    public boolean isAggregate()
    {
        return false;
    }

    /**
     * Returns DBTable, DBQuery or DBView object.
     *
     * @return the DBTable, DBQuery or DBView object
     */
    public DBRowSet getRowSet()
    {
        return rowset;
    }

    /**
     * Returns the column name.
     *
     * @return the column name
     */
    @Override
    public String getName()
    {
        return name;
    }

    /**
     * Returns the full qualified column name.
     *
     * @return the full qualified column name
     */
    public String getFullName()
    {
        if (rowset==null)
            return name;
        return rowset.getFullName()+"."+name;
    }

    /**
     *  @see DBColumnExpr#getAttribute(String)
     */
    @Override
    public Object getAttribute(String name)
    {
        return (attributes != null ? attributes.get(name) : null);
    }

    /**
     *  @see DBColumnExpr#getOptions()
     */
    @Override
    public Options getOptions()
    {
        return options;
    }

    /**
     * Creates and returns a new DBSetExpr object.
     *
     * @see org.apache.empire.db.expr.set.DBSetExpr
     * @param value the Object value
     * @return the new DBSetExpr object
     */
    public DBSetExpr to(Object value)
    {
        return new DBSetExpr(this, value);
    }

    /**
     * Override the toString method.
     *
     * @return the table name and the column name (e.g. CUSTOMER.ID)
     */
    @Override
    public String toString()
    {
        if (rowset==null)
            return name;
        return rowset.getName()+"."+name;
    }

    /**
     * Returns a comment describing the column in the data scheme.
     *
     * @return Returns the comment.
     */
    public String getComment()
    {
        return comment;
    }

    /**
     * Sets a comment describing the current column.
     *
     * @param comment the column comment
     */
    public void setComment(String comment)
    {
        this.comment = comment;
    }

}