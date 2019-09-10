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
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.util.Set;

import org.apache.empire.commons.Options;
import org.apache.empire.commons.StringUtils;
import org.apache.empire.data.Column;
import org.apache.empire.db.exceptions.DatabaseNotOpenException;
import org.apache.empire.db.expr.set.DBSetExpr;
import org.apache.empire.exceptions.InvalidArgumentException;
import org.apache.empire.exceptions.ItemNotFoundException;
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
    private final static long serialVersionUID = 1L;
  
    private static final Logger log = LoggerFactory.getLogger(DBColumn.class);
    
    /**
     * Read only column (Boolean)
     */
    // private static final String DBCOLATTR_READONLY  = "readonly";
    
    /**
     * Read only column (Boolean)
     */
    public static final String DBCOLATTR_SINGLEBYTECHARS  = "singleByteChars";

    // basic data
    protected final transient DBRowSet rowset;
    protected final String     name;
    protected String           comment;

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
    public String getId()
    {
        return rowset.getId()+"."+name;
    }

    /**
     * returns a rowset by its identifier
     * @param columnId the id of the column
     * @return the DBColumn object
     */
    public static DBColumn findById(String columnId)
    {
        int i = columnId.lastIndexOf('.');
        if (i<0)
            throw new InvalidArgumentException("columnId", columnId);
        // rowset suchen
        String rsid = columnId.substring(0, i);
        DBRowSet rset = DBRowSet.findById(rsid);
        // column suchen
        String colname = columnId.substring(i+1);
        DBColumn col = rset.getColumn(colname);
        if (col==null)
            throw new ItemNotFoundException(columnId);
        return col;
    }
    
    /**
     * Custom serialization for transient rowset.
     */
    private void writeObject(ObjectOutputStream strm) throws IOException 
    {
        if (rowset==null)
        {   // No rowset
            strm.writeObject("");
            strm.defaultWriteObject();
            return;
        }
        // write dbid and rowset-name
        String dbid   = rowset.getDatabase().getId(); 
        String rsname = rowset.getName(); 
        strm.writeObject(dbid);
        strm.writeObject(rsname);
        if (log.isDebugEnabled())
            log.debug("Serialization: writing DBColumn "+dbid+"."+rsname);
        strm.defaultWriteObject();
    }

    /**
     * Custom serialization for transient rowset.
     */
    private void readObject(ObjectInputStream strm) throws IOException, ClassNotFoundException, 
        SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException
    {
        String dbid = String.valueOf(strm.readObject());
        if (StringUtils.isNotEmpty(dbid))
        {   // Find Rowset
            String rsname = String.valueOf(strm.readObject());
            if (log.isDebugEnabled())
                log.debug("Serialization: reading DBColumn "+dbid+"."+rsname);
            // find database
            DBDatabase db = DBDatabase.findById(dbid);
            if (db==null)
                throw new ClassNotFoundException(dbid);
            // find database
            DBRowSet srs = db.getRowSet(rsname);
            if (srs==null)
                throw new ClassNotFoundException(dbid+"."+rsname);
            // set final field
            Field f = DBColumn.class.getDeclaredField("rowset");
            f.setAccessible(true);
            f.set(this, srs);
            f.setAccessible(false);
        }
        // read the rest
        strm.defaultReadObject();
    }

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
    public abstract Object validate(Object value);
    
    /**
     * @deprecated use validate() instead 
     */
    @Deprecated
    public final void checkValue(Object value)
    {
        validate(value);
    }
    
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
    { 
        // Append rowset alias
        if ((context & CTX_FULLNAME) != 0)
        {   // Fully Qualified Name
            buf.append(rowset.getAlias());
            buf.append(".");
        }
        // Append the name
        DBDatabaseDriver driver = getDatabase().getDriver();
        if (driver==null)
        	throw new DatabaseNotOpenException(getDatabase());
        if (quoteName==null)
            quoteName = driver.detectQuoteName(name);
        // Append the name
        driver.appendElementName(buf, name, quoteName.booleanValue());
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
            throw new ObjectNotValidException(this);
        return rowset.getFullName()+"."+name;
    }

    /**
     * returns the qualified alias name for this column
     */
    public String getAlias()
    {
        if (rowset==null)
            throw new ObjectNotValidException(this);
        String rsName = rowset.getName();
        if (StringUtils.isEmpty(rsName))
            return name;
        return rsName + "_" + name;
    }

    /**
     * returns an expression that renames the column with its alias name
     */
    public DBColumnExpr qualified()
    {
        return this.as(getAlias());
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
     * Returns true if an enum type has been set for this column
     * <P>
     * @return eturns true if an enum type has been set for this column
     */
    @Override
    public boolean isEnum()
    {
        return (attributes!=null && getAttribute(COLATTR_ENUMTYPE)!=null);
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