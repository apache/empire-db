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

import java.util.Collections;
import java.util.Set;

import org.apache.empire.commons.Attributes;
import org.apache.empire.commons.Options;
import org.apache.empire.commons.StringUtils;
import org.apache.empire.data.Column;
import org.apache.empire.data.DataType;
import org.apache.empire.data.Entity;
import org.apache.empire.db.exceptions.DatabaseNotOpenException;
import org.apache.empire.db.expr.column.DBAliasExpr;
import org.apache.empire.db.expr.column.DBValueExpr;
import org.apache.empire.db.expr.set.DBSetExpr;
import org.apache.empire.dbms.DBMSHandler;
import org.apache.empire.exceptions.ObjectNotValidException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    // *Deprecated* private static final long serialVersionUID = 1L;
  
    private static final Logger log = LoggerFactory.getLogger(DBColumn.class);

    // basic data
    protected final DBRowSet  rowset; /* transient */
    protected final String    name;
    protected String          comment;

    private Boolean quoteName = null;
    
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
     * Gets an identifier for this RowSet Object
     * @return the rowset identifier
     */
    public String getIdentifier()
    {
        return rowset.getIdentifier()+"."+name;
    }

    /**
     * returns a rowset by its identifier
     * @param columnId the id of the column
     * @return the DBColumn object
    public static DBColumn findByIdentifier(String columnId)
    {
        int i = columnId.lastIndexOf('.');
        if (i<0)
            throw new InvalidArgumentException("columnId", columnId);
        // rowset suchen
        String rsid = columnId.substring(0, i);
        DBRowSet rset = DBRowSet.findByIdentifier(rsid);
        // column suchen
        String colname = columnId.substring(i+1);
        DBColumn col = rset.getColumn(colname);
        if (col==null)
            throw new ItemNotFoundException(columnId);
        return col;
    }
     */
    
    /**
     * Custom serialization for transient rowset.
     * 
    private void writeObject(ObjectOutputStream strm) throws IOException 
    {   // RowSet
        strm.writeObject(rowset.getDatabase().getIdentifier());
        strm.writeObject(rowset.getName());
        // write default
        strm.defaultWriteObject();
    }

    private void readObject(ObjectInputStream strm) throws IOException, ClassNotFoundException 
    {
        String dbid = String.valueOf(strm.readObject());
        String rsid = String.valueOf(strm.readObject());
        // find database
        DBDatabase dbo = DBDatabase.findByIdentifier(dbid);
        if (dbo==null)
            throw new ClassNotFoundException(dbid);
        // find rowset
        DBRowSet rso = dbo.getRowSet(rsid);
        if (rso==null)
            throw new ClassNotFoundException(dbid+"."+rsid);
        // set final field
        ClassUtils.setPrivateFieldValue(DBColumn.class, this, "rowset", rso);
        // read the rest
        strm.defaultReadObject();
    }
     */

    @Override
    public boolean equals(Object other)
    {
        if (other==this)
            return true;
        if (rowset==null)
            return super.equals(other);
        if (other instanceof DBColumn)
        {   // Rowset and name must match
            DBColumn c = (DBColumn) other;
            if (rowset.equals(c.getRowSet())==false)
                return false;
            // check for equal names
            return StringUtils.compareEqual(name, c.getName(), true);
        }
        else if (other instanceof DBAliasExpr)
        {   // Compare with an alias expression (added 2024-07-18 EMPIREDB-434)
            DBAliasExpr otherExpr = ((DBAliasExpr)other);
            return name.equalsIgnoreCase(otherExpr.getName());
        }        
        return false;
    }
    
    @Override
    public int hashCode()
    {
    	if (rowset==null || name==null)
    		return super.hashCode();
    	// rowset and name
    	return rowset.hashCode()+name.hashCode();
    }
     
    /**
     * Returns the size of the column.
     *
     * @return Returns the size of the column
     */
    @Override
    public abstract double getSize();

    /**
     * Returns true if the column is required.
     *
     * @return Returns true if the column is required
     */
    @Override
    public abstract boolean isRequired();
    
    /**
     * Returns true if column is a columns value is an automatically generated value
     * 
     * @return true if column is auto-generated
     */
    @Override
    public abstract boolean isAutoGenerated();

    /**
     * Returns true if the column is read-only.
     *
     * @return Returns true if the column is read-only
     */
    @Override
    public abstract boolean isReadOnly();

    /**
     * Checks if the given value is a valid value for this column 
     * If not, an exception is thrown
     */
    @Override
    public abstract Object validateValue(Object value);

    /**
     * Appends column meta information to the parent element
     */
    @Override
    public abstract Element addXml(Element parent, long flags);

    /**
     * @return the current DBDatabase object
     */
    @SuppressWarnings("unchecked")
    @Override
    public final DBDatabase getDatabase()
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
     * @param sql the SQL statment
     * @param context the current SQL-Command context
     */
    @Override
    public void addSQL(DBSQLBuilder sql, long context)
    { 
        // Append rowset alias
        if ((context & CTX_FULLNAME) != 0)
        {   // Fully Qualified Name
            sql.append(rowset.getAlias());
            sql.append(".");
        }
        // Append the name
        DBMSHandler dbms = getDatabase().getDbms();
        if (dbms==null)
        	throw new DatabaseNotOpenException(getDatabase());
        // Append the name
        dbms.appendObjectName(sql, name, quoteName);
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
     * Returns DBTable, DBQuery or DBView object.
     *
     * @return the DBTable, DBQuery or DBView object
     */
    @Override
    public DBRowSet getRowSet()
    {
        return rowset;
    }
    
    /**
     * Returns the Entity Type
     * Same as getRowSet()
     */
    @Override
    public Entity getEntity()
    {
        return rowset;
    }
    
    /**
     * Returns itself as the underlying column.
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
     * Returns a reference expression for this column
     * This can be used to reference a parent column in a subquery 
     * @return the column value expression
     */
    public DBColumnExpr reference()
    {
        return new DBValueExpr(getDatabase(), this, DataType.UNKNOWN);    
    }

    /**
     * Returns the full qualified column name.
     * @return the full qualified column name
     */
    public String getFullName()
    {
        if (rowset==null)
            throw new ObjectNotValidException(this);
        // concat
        return StringUtils.concat(rowset.getFullName(), ".", name);
    }

    /**
     * returns the qualified alias name for this column
     * @return the alias
     */
    public String getAlias()
    {
        if (rowset==null)
            throw new ObjectNotValidException(this);
        String rsName = rowset.getEntityName();
        if (StringUtils.isEmpty(rsName))
            return name;
        // concat
        return StringUtils.concat(rsName, "_", name);
    }

    /**
     * returns an expression that renames the column with its alias name
     * @return a qualified expression for this column
     */
    @Override
    public DBColumnExpr qualified()
    {
        return this.as(getAlias());
    }

    /**
     * Creates a column expression with coalesce and renames it to this column
     * 
     * @param nullValue the alternative value when this column is null
     * @return the coalesce and alias expression
     */
    public DBColumnExpr coalesceColumn(Object nullValue)
    {
        return coalesce(nullValue).as(this);
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
     * Returns all metadata attributes.
     * @return set of metadata attributes
     */
    @Override
    @SuppressWarnings("unchecked")
    public Set<Attributes.Attribute> getAttributes()
    {
        return (attributes!=null ? Collections.unmodifiableSet(attributes)
                                 : Collections.EMPTY_SET);
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
     * Returns true if an enum type has been set for this column
     * <P>
     * @return true if an enum type has been set for this column or false otherwise
     */
    public final boolean isEnum()
    {
        return (getEnumType()!=null);
    }

    /**
     * Returns the enum type for this column
     * <P>
     * @return the enum type
     */
    @Override
    @SuppressWarnings("unchecked")
    public Class<Enum<?>> getEnumType()
    {
        return (attributes!=null ? (Class<Enum<?>>)getAttribute(COLATTR_ENUMTYPE) : null);
    }
    
    @Override
    public Class<?> getJavaType()
    {
        Class<Enum<?>> enumType = getEnumType();
        if (enumType!=null)
            return enumType;
        // default
        return super.getJavaType();
    }

    /**
     * Creates and returns a sql-expression that maps enum values by name or ordinal to their string representation 
     * 
     * @return a DBDecodeExpr object
     */
    public DBColumnExpr decodeEnum()
    {
        return super.decodeEnum(getEnumType(), null);
    }

    /**
     * Creates and returns a sql-expression that maps enum values from name to ordinal
     * @param defaultToEnd flag whether to put the default item to the end
     * @return a DBDecodeExpr object
     */
    public DBColumnExpr decodeSort(boolean defaultToEnd)
    {
        if (getDataType().isNumeric())
        {
            log.warn("Unnecessary decode for numeric column");
            return this;
        }
        return super.decodeSort(getEnumType(), defaultToEnd);
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
        // concat
        return StringUtils.concat(rowset.getName(), ".", name);
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