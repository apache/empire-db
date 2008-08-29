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

import java.sql.Connection;

import org.apache.empire.commons.Errors;
import org.apache.empire.commons.Options;
import org.apache.empire.data.DataType;
import org.apache.empire.db.expr.column.DBValueExpr;
import org.apache.empire.xml.XMLUtil;
import org.w3c.dom.Element;


/**
 * This class represent a view of the database.
 * It consits methods to get and update records from the database
 * <P>
 * 
 *
 */
public abstract class DBView extends DBRowSet
{
    public static class DBViewColumn extends DBColumn
    {
        protected final DBColumnExpr expr;
        protected final DBColumn     updateColumn;

        /**
         * Constructs a DBViewColumn object set the specified parameters to this object.
         * 
         * @param view the DBView object
         * @param expr the DBColumnExpr of the source table
         */
        protected DBViewColumn(DBView view, String name, DBColumnExpr expr)
        { // call base
            super(view, name);
            // set Expression
            this.expr = expr;
            // Update Column
            this.updateColumn = expr.getUpdateColumn();
        }

        public DBColumnExpr getSourceColumnExpr()
        {
            return expr;
        }
        
        public DBView getView()
        {
            return (DBView)getRowSet();
        }

        @Override
        public DataType getDataType()
        {
            return expr.getDataType();
        }

        @Override
        public double getSize()
        {
            if (updateColumn==null)
                return 0.0;
            return updateColumn.getSize();
        }

        @Override
        public boolean isReadOnly()
        {   
            if (getView().isUpdateable()==false)
                return true;
            // Check ReadOnly attribute 
            if (attributes!=null &&
                attributes.containsKey(DBCOLATTR_READONLY))
                return true;
            // AUTOINC's are read only
            return (getDataType() == DataType.AUTOINC);
        }

        @Override
        public boolean isRequired()
        {   
            if (getView().isUpdateable()==false)
                return false;
            // Check update Column
            if (updateColumn==null)
                return false;
            return updateColumn.isRequired();
        }    

        @Override
        public boolean checkValue(Object value)
        {
            if (updateColumn==null)
                return true;
            return updateColumn.checkValue(value);
        }

        @Override
        public Object getAttribute(String name)
        {
            if (attributes != null && attributes.containsKey(name))
                return attributes.get(name);
            // Otherwise ask expression
            if (updateColumn==null)
                return null;
            return updateColumn.getAttribute(name);
        }

        @Override
        public Options getOptions()
        {
            if (options != null)
                return options;
            // Otherwise ask expression
            if (updateColumn==null)
                return null;
            return updateColumn.getOptions();
        }

        @Override
        public Element addXml(Element parent, long flags)
        {
            Element elem = XMLUtil.addElement(parent, "column");
            elem.setAttribute("name", name);
            // set default attributes
            if (updateColumn != null)
                elem.setAttribute("source", updateColumn.getFullName());
            // size
            double size = getSize();
            if (size > 0)
                elem.setAttribute("size", String.valueOf(size));
            // add All Attributes
            if (attributes != null)
                attributes.addXml(elem, flags);
            // add All Options
            Options fieldOptions = getOptions();
            if (fieldOptions != null)
                fieldOptions.addXml(elem, flags);
            // done
            return elem;
        }

    }

    private static int viewCount = 1;

    private String     name;
    private String     alias;
    private boolean    updateable;  // true if the view is updateable

    /**
     * Creates a view object for a given view in the database.
     * 
     * @param name the name of the view
     * @param db the database this view belongs to.
     * @param isUpdateable true if the records of this view can be updated 
     */
    public DBView(String name, DBDatabase db, boolean isUpdateable)
    { // Set the column expressions
        super(db);
        // Add View to Database
        if (db != null && name != null)
            db.addView(this);
        // Set Name and Alias
        this.name = name;
        this.alias = "v" + String.valueOf(viewCount);
        this.updateable = isUpdateable;
        viewCount++;
    }

    /**
     * Creates a view object for a given view in the database.
     * 
     * @param name the name of the view
     * @param db the database this view belongs to.
     */
    public DBView(String name, DBDatabase db)
    { // Set the column expressions
        this(name, db, false);
    }
    
    /**
     * identifies the columns that uniquely identify a row in the view
     * @param keyColumns list of columns that uniquely identify a row 
     */
    protected void setKeyColumns(DBViewColumn[] keyColumns)
    { // Set Key Columns
        if (keyColumns != null)
            primaryKey = new DBIndex(null, DBIndex.PRIMARYKEY, keyColumns);
    }

    /**
     * identifies the column that uniquely identifies a row in the view
     * @param keyColumn the column that uniquely identifies a row 
     */
    protected void setKeyColumn(DBViewColumn keyColumn)
    {
        if (keyColumn != null)
            setKeyColumns(new DBViewColumn[] { keyColumn });
        else
            setKeyColumns((DBViewColumn[]) null);
    }

    /**
     * Returns the command required to create the view<br>
     * This is function is only used for the creation of DDL statements
     * @return a command expression that is used to create the view
     */
    public abstract DBCommandExpr createCommand();

    /**
     * Returns the view name of this object.
     * @return the view name of this object
     */
    @Override
    public String getName()
    {
        return name;
    }

    /**
     * Returns the full qualified table name.
     * @return the full qualified table name
     */
    @Override
    public String getFullName()
    {
        String schema = db.getSchema();
        return (schema != null) ? schema + "." + name : name;
    }

    /**
     * Returns the alias name of this object.
     * @return the alias name of this object
     */
    @Override
    public String getAlias()
    {
        return alias;
    }
    
    /**
     * Returns whether or not the view is updateable
     * @return true if the view is updateable or false if not 
     */
    public boolean isUpdateable()
    {
        return this.updateable;
    }

    /**
     * Adds a column to the view.
     * 
     * @param col a view column object
     * @return true if the column was successfully added or false otherwise
     */
    protected boolean addColumn(DBViewColumn col)
    { // find column by name
        if (col == null || col.getRowSet() != this)
            return error(Errors.InvalidArg, col, "col");
        if (columns.contains(col) == true)
            return error(Errors.ItemExists, col.getName());
        // add now
        columns.add(col);
        return true;
    }

    /**
     * Adds a column to the view.
     * 
     * @param columnName name of the column in the view
     * @param dataType the data type of the column
     * @return true if the column was successfully added or false otherwise
     */
    protected final DBViewColumn addColumn(String columnName, DataType dataType)
    { // find column by name
        DBViewColumn vc = new DBViewColumn(this, columnName, new DBValueExpr(db, null, dataType));
        return (addColumn(vc) ? vc : null);
    }

    /**
     * Adds a column to the view.
     * 
     * @param columnName name of the column in the view
     * @param columnExpr the column expression that builds the column
     * @return true if the column was successfully added or false otherwise
     */
    protected final DBViewColumn addColumn(String columnName, DBColumnExpr columnExpr)
    { // find column by name
        DBViewColumn vc = new DBViewColumn(this, columnName, columnExpr);
        return (addColumn(vc) ? vc : null);
    }

    /**
     * Adds a column to the view based on an existing column in another table or view.
     * 
     * @param sourceColumn existing column in another table or view  
     * @return the view column object
     */
    protected final DBViewColumn addColumn(DBTableColumn sourceColumn)
    { // find column by name
        DBViewColumn vc = new DBViewColumn(this, sourceColumn.getName(), sourceColumn);
        return (addColumn(vc) ? vc : null);
    }

    /**
     * This function searchs for equal columns given by the specified DBColumnExpr object.
     * 
     * @param expr the DBColumnExpr object
     * @return the located column (only DBViewColumn onjects)
     */
    public DBViewColumn findViewColumn(DBColumnExpr expr)
    {
        for (int i = 0; i < columns.size(); i++)
        {
            DBViewColumn vc = (DBViewColumn) columns.get(i);
            if (vc.expr.equals(expr))
                return vc;
        }
        // not found
        return null;
    }

    /**
     * Creates the SQL-Command adds the alias name to the SQL-Command.
     * 
     * @param buf the SQL-Command
     * @param context the current SQL-Command context
     */
    @Override
    public void addSQL(StringBuilder buf, long context)
    {
        if ((context & CTX_FULLNAME) != 0 && db != null)
        { // Add Schema
            db.appendQualifiedName(buf, name);
        } 
        else
        { // Simple Name only
            buf.append(name);
        }
        // Add Alias
        if ((context & CTX_ALIAS) != 0 && alias != null)
        { // append alias
            buf.append(getRenameTablePhrase());
            buf.append(alias);
        }
    }

    @Override
    public boolean updateRecord(DBRecord rec, Connection conn)
    {
        if (updateable==false)
            return error(Errors.NotSupported, "updateRecord");
        // Update the record
        return super.updateRecord(rec, conn);
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.apache.empire.db.DBRowSet#addRecord(org.apache.empire.db.DBRecord, java.sql.Connection)
     */
    @Override
    public boolean createRecord(DBRecord rec, Connection conn)
    {
        return error(Errors.NotSupported, "addRecord");
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.empire.db.DBRowSet#deleteRecord(java.lang.Object[], java.sql.Connection, boolean)
     */
    @Override
    public boolean deleteRecord(Object[] keys, Connection conn)
    {
        return error(Errors.NotSupported, "deleteRecord");
    }
}