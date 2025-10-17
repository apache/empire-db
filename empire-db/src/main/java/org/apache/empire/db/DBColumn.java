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
import java.util.regex.Pattern;

import org.apache.empire.commons.Attributes;
import org.apache.empire.commons.Options;
import org.apache.empire.commons.StringUtils;
import org.apache.empire.data.Column;
import org.apache.empire.data.DataType;
import org.apache.empire.data.Entity;
import org.apache.empire.db.exceptions.DatabaseNotOpenException;
import org.apache.empire.db.expr.column.DBAliasExpr;
import org.apache.empire.db.expr.column.DBFuncExpr;
import org.apache.empire.db.expr.set.DBSetExpr;
import org.apache.empire.dbms.DBMSHandler;
import org.apache.empire.exceptions.InvalidArgumentException;
import org.apache.empire.exceptions.NotSupportedException;
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

    protected Boolean quoteName = null;
    
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
        if (quoteName==null)
            quoteName=dbms.detectQuoteName(this, name);
        // append
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
        return getDatabase().getValueExpr(this, DataType.UNKNOWN);    
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
    public DBAliasExpr qualified()
    {
        return this.as(getAlias());
    }

    /**
     * Creates a column expression with coalesce and renames it to this column
     * 
     * @param nullValue the alternative value when this column is null
     * @return the coalesce and alias expression
     */
    public DBAliasExpr coalesceColumn(Object nullValue)
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

    /*
     * (non-Javadoc)
     * @see org.apache.empire.db.DBColumnExpr#getOptions()
     */
    @Override
    public Options getOptions()
    {
        return options;
    }

    /**
     * Returns true if an enum type has been set for this column
     * @return true if an enum type has been set for this column or false otherwise
     */
    public final boolean isEnum()
    {
        return (getEnumType()!=null);
    }
    
    /**
     * Returns the enum type for this column
     * @return the enum type
     */
    @Override
    @SuppressWarnings("unchecked")
    public Class<Enum<?>> getEnumType()
    {
        return (Class<Enum<?>>)getAttribute(COLATTR_ENUMTYPE);
    }
    
    /**
     * Returns the java type for this column
     * @return the java type
     */
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
     * Sets the case sensitivity of the columnExpr
     * @param caseSensitiv the character casing. This may be true, false or null (default)
     * @return self (this)
     */
    @SuppressWarnings("unchecked")
    public <T extends DBColumn> T setCaseSensitive(Boolean caseSensitiv)
    {
        if (!getDataType().isText())
            throw new NotSupportedException(this, "setCaseInsensitive");
        // set now
        setAttribute(Column.COLATTR_CASESENSITIVE, caseSensitiv);
        return (T)this;
    }
    
    /**
     * Sets the case sensitivity of the column to insensitive
     * Text columns are case sensitive by default
     * @return self (this)
     */
    public final <T extends DBColumn> T setCaseInsensitive()
    {
        return setCaseSensitive(false);
    }

    /**
     * Sets the number type to a given value
     * @param numberType the number type. See Column.NUMTYPE_... constants
     * @return self (this)
     */
    @SuppressWarnings("unchecked")
    public final <T extends DBColumn> T setNumberType(String numberType)
    {
        // only for text expressions
        if (!getDataType().isNumeric())
            throw new NotSupportedException(this, "setNumberType");
        // set now
        setAttribute(Column.COLATTR_NUMBER_TYPE, numberType);
        return (T)this;
    }

    /**
     * Returns the maximum allowed value 
     * @return the minimum length 
     */
    public Pattern getRegExPattern()
    {
        return (Pattern)getAttribute(Column.COLATTR_REGEXP);
    }
    
    /**
     * Set the regular expression to validate the columnExpr value 
     * @param regex the regular expression
     * @return self (this)
     */
    @SuppressWarnings("unchecked")
    public <T extends DBColumn> T setRegExPattern(String regex)
    {
        setAttribute(Column.COLATTR_REGEXP, Pattern.compile(regex));
        return (T)this;
    }
    
    /**
     * Returns the normalized column for the columnExpr (if any)
     * @return the normalized column or null
     */
    public DBColumnExpr getNormalizedColumn()
    {   // return attribute
        return (DBColumn)getAttribute(Column.COLATTR_NORMCOLUMN);
    }
    
    /**
     * Sets a normalized columnExpr for this columnExpr
     * @param normalizedColumn the normalized columnExpr
     * @return self (this)
     */
    @SuppressWarnings("unchecked")
    public <T extends Column> T setNormalizedColumn(DBColumnExpr normalizedColumn)
    { 
        setAttribute(Column.COLATTR_NORMCOLUMN, normalizedColumn);
        return (T)this;
    }
    
    /**
     * Returns the sort expression for a given column
     * If no sort expression is explicitly set then the column itself is returned
     * The returned expression should be assigned to an DBCommand.orderBy() function.
     * 
     * @return the sort expression or the column itself if not sort expression is set
     */
    public DBColumnExpr getSortExpr()
    {
        Object value = getAttribute(Column.COLATTR_SORTEXPRESSION);
        if (value==null)
        {   // not set
            return this;
        }
        else if (value instanceof DBColumnExpr)
        {   // return expression
            return ((DBColumnExpr)value); 
        }
        else if ((value instanceof String) && ((String)value).indexOf('?')>=0)
        {   // create a sort function expression
            String sortFunctionTemplate = StringUtils.toString(value);
            return new DBFuncExpr(this, sortFunctionTemplate, null, false, getDataType());
        }
        else
        {   // unknown value
            log.warn("Invalid value for {}: {}", Column.COLATTR_SORTEXPRESSION, value);
            return this; 
        }
    }
    
    /**
     * Sets a sort function expression for a given column
     * @param sortExpression the expression which to use for sorting
     * @return self (this)
     */
    @SuppressWarnings("unchecked")
    public <T extends DBColumn> T setSortExpr(DBColumnExpr sortExpression)
    {   // set sort expression
        setAttribute(Column.COLATTR_SORTEXPRESSION, sortExpression);
        return (T)this;
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
     * Sets a sort function template for a given column
     * @param sortFunctionTemplate the template which must contain a ? which will be replaced with the column name
     * @return return the column 
     */
    @SuppressWarnings("unchecked")
    public <T extends DBColumn> T setSortExpr(String sortFunctionTemplate)
    {
        // check param
        if (sortFunctionTemplate!=null && sortFunctionTemplate.indexOf('?')<0)
            throw new InvalidArgumentException("sortFunctionTemplate", sortFunctionTemplate);
        // set sort expression
        setAttribute(Column.COLATTR_SORTEXPRESSION, sortFunctionTemplate);
        return (T)this;
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