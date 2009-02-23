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

// java
import java.lang.reflect.Field;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import org.apache.empire.commons.Errors;
import org.apache.empire.data.DataType;


/**
 * This class represent one table of the database.
 * It consits methods to get, add, update and delete records from the database.
 * <P>
 * 
 *
 */
public class DBTable extends DBRowSet implements Cloneable
{
    private static int     tableCount    = 1;
    private final String   name;
    private String         alias;
    private List<DBIndex>  indexes       = new ArrayList<DBIndex>();
    private boolean        cascadeDelete = false;
    private Boolean        quoteName     = null;

    /**
     * Construct a new DBTable object set the specified parameters
     * to this object and add this object to the current database.
     * 
     * @param name the table name
     * @param db the valid database object
     */
    public DBTable(String name, DBDatabase db)
    { 
        super(db);
        // init
        this.name = name;
        this.alias = "t" + String.valueOf(tableCount);
        tableCount++;
        // Add Table to Database
        if (db != null)
            db.addTable(this);
    }

    /**
     * Returns the table name of this object.
     * 
     * @return the table name of this object
     */
    @Override
    public String getName()
    {
        return name;
    }

    /**
     * Returns the table alias name of this object.
     * 
     * @return the table alias name of this object
     */
    @Override
    public String getAlias()
    {
        return alias;
    }

    /**
     * Clones this object.
     * 
     * @return this cloned Object
     */
    @SuppressWarnings("unchecked")
    @Override
    public Object clone()
    {
        try
        {
            DBTable clone = (DBTable) super.clone();
            // clone all columns
            Class colClass = columns.get(0).getClass();
            Class colBase = colClass.getSuperclass();
            clone.columns = new ArrayList<DBColumn>();
            Field[] fields = getClass().getDeclaredFields();
            for (int i = 0; i < columns.size(); i++)
            {
                DBTableColumn srcCol = (DBTableColumn) columns.get(i);
                DBTableColumn newCol = new DBTableColumn(clone, srcCol);
                // Replace all references for oldCol to newCol
                for (int j = 0; j < fields.length; j++)
                { // Find a class of Type DBColumn or DBTableColumn
                    Class type = fields[j].getType();
                    if (type == colClass || type == colBase)
                    {
                        try
                        {
                            // Check if the field points to the old Value
                            if (fields[j].get(clone) == srcCol)
                                fields[j].set(clone, newCol);
                        } catch (Exception e)
                        {
                            // IllegalAccessException or IllegalArgumentException
                            log.error("clone: Cannot clone table-member: " + fields[j].getName() + "-->" + e.getMessage());
                        }
                    }
                }
            }
            // set new alias
            clone.alias = "t" + String.valueOf(tableCount);
            tableCount++;
            // done
            log.info("clone: Table " + name + " cloned! Alias old=" + alias + " new=" + clone.alias);
            return clone;
        } catch (CloneNotSupportedException e)
        {
            return null;
        }
    }

    /**
     * Adds a column to this table's column list.
     * 
     * @param column a column object
     */
    protected boolean addColumn(DBTableColumn column)
    { // find column by name
        if (column==null || column.getRowSet()!=this)
            return error(Errors.InvalidArg, column, "column");
        if (columns.contains(column) == true)
            return error(Errors.ItemExists, column.getName());
        // add now
        columns.add(column);
        return true;
    }

    /**
     * Creates a new DBTableColumn object and add it to this object.
     * 
     * @param columnName the column name
     * @param type the type of the column e.g. integer, text, date
     * @param size the column width
     * @param required true if not null column
     * @param defValue a Object object
     * @return the created DBTableColumn object
     */
    public final DBTableColumn addColumn(String columnName, DataType type, double size, boolean required, Object defValue)
    { // find column by name
        return new DBTableColumn(this, type, columnName, size, required, defValue);
    }

    /**
     * Creates a new DBTableColumn object and add it to this object.
     * 
     * @param columnName the column name
     * @param type the type of the column e.g. integer, text, date
     * @param size the column width
     * @param required true if not null column
     * @return the created DBTableColumn object
     */
    public final DBTableColumn addColumn(String columnName, DataType type, double size, boolean required)
    { // find column by name
        return addColumn(columnName, type, size, required, null);
    }

    /**
     * Returns the primary key.
     * 
     * @return the the DBIndex object ->primary key
     */
    public DBIndex getPrimaryKey()
    {
        return primaryKey;
    }

    /**
     * Returns the list of indexes (except the primary key).
     * 
     * @return a list of DBIndex objects
     */
    public List<DBIndex> getIndexes()
    {
        return indexes;
    }
    
    /**
     * Sets the primary keys.
     * 
     * @param columns a array with one or more DBColumn objects
     * 
     * @return true on succes
     */
    public boolean setPrimaryKey(DBColumn[] columns)
    {
        if (columns==null || columns.length==0)
            return error(Errors.InvalidArg, columns, "columns");
        // All columns must belong to this table
        for (int i=0; i<columns.length; i++)
            if (columns[i].getRowSet()!=this)
                return error(Errors.InvalidArg, columns[i].getFullName(), "columns");
        // Set primary Key now
        primaryKey = new DBIndex(name + "_PK", DBIndex.PRIMARYKEY, columns);
        return true;
    }

    /**
     * Sets the primary key to a single column.
     * 
     * @param column the primary key column
     */
    public final void setPrimaryKey(DBColumn column)
    {
        setPrimaryKey(new DBColumn[] { column });
    }

    /**
     * Adds two columns to the primary key list.
     * 
     * @param col1 the first column 
     * @param col2 the second column
     */
    public final void setPrimaryKey(DBColumn col1, DBColumn col2)
    {
        setPrimaryKey(new DBColumn[] { col1, col2 });
    }

    /**
     * Adds three columns to the primary key list.
     * 
     * @param col1 the first column
     * @param col2 the second column
     * @param col3 the third column
     */
    public final void setPrimaryKey(DBColumn col1, DBColumn col2, DBColumn col3)
    {
        setPrimaryKey(new DBColumn[] { col1, col2, col3 });
    }

    /**
     * Adds an index.
     * 
     * @param indexName the index name
     * @param unique is this a unique index
     * @param indexColumns the columns indexed by this index
     * 
     * @return true on succes
     */
    public boolean addIndex(String indexName, boolean unique, DBColumn[] indexColumns)
    {
        if (indexName==null || indexColumns==null || indexColumns.length==0)
            return error(Errors.InvalidArg, null, "name|columns");
        // add Index now
        indexes.add(new DBIndex(indexName, (unique) ? DBIndex.UNIQUE : DBIndex.STANDARD, indexColumns));
        return true;
    }

    /**
     * Adds a timestamp column to the table used for optimistic locking.
     * 
     * @param columnName the column name
     * 
     * @return the timestamp table column object
     */
    public DBTableColumn addTimestampColumn(String columnName)
    {
        DBTableColumn col = addColumn(columnName, DataType.DATETIME, 0, true, DBDatabase.SYSDATE);
        setTimestampColumn(col);
        return col;
    }

    /**
     * Adds the table's name to the supplied sql command buffer.
     * 
     * @param buf the SQL-Command
     * @param context the current SQL-Command context
     */
    @Override
    public void addSQL(StringBuilder buf, long context)
    {
        // Append Name
        if ((context & CTX_NAME|CTX_FULLNAME)!=0)
        {   // Append the name
            DBDatabaseDriver driver = getDatabase().getDriver();
            if (quoteName==null)
                quoteName = driver.detectQuoteName(name);
            // append Qualified Name 
            db.appendQualifiedName(buf, name, quoteName);
        }
        // Append Alias
        if ((context & CTX_ALIAS)!=0 && alias!=null)
        {    // append alias
             buf.append(getRenameTablePhrase());
             buf.append(alias);
        }
    }

    /**
     * Gets all table fields and the fields properties. 
     * Set this to the specified DBRecord object.
     * 
     * @param rec the DBRecord object. contains all fields and the field properties
     * @param conn a valid connection to the database.
     * 
     * @return true if successful
     */
    @Override
    public boolean createRecord(DBRecord rec, Connection conn)
    {
        // Inititialisierung
        if (!prepareInitRecord(rec, DBRecord.REC_NEW, null))
            return false;
        // Set Defaults
        int count = columns.size();
        for (int i = 0; i < count; i++)
        {
            DBTableColumn column = (DBTableColumn)columns.get(i);
            Object value = column.getRecordDefaultValue(conn);
            if (value!=null)
                rec.modifyValue(i, value); 
        }
        // Init
        return completeInitRecord(rec);
    }

    /**
     * Returns true if cascaded deletes are enabled or false otherwise.
     *  
     * @return true if cascade deletes are enabled
     */
    public boolean isCascadeDelete()
    {
        return cascadeDelete;
    }

    /**
     * Sets true if delete records will.
     *  
     * @param cascadeDelete use cascade deletes or not
     */
    public void setCascadeDelete(boolean cascadeDelete)
    {
        this.cascadeDelete = cascadeDelete;
    }

    /**
     * Creates a delete SQL-Command by using the DBCommand getDelete method
     * execute the the SQL-Command with the DBDatabase
     * executeSQL method.
     * 
     * @param key an array of the primary key columns
     * @param conn a valid connection to the database.
     * @return true if successful
     */
    @Override
    public boolean deleteRecord(Object[] key, Connection conn)
    {
        // Check Primary key
        if (primaryKey == null )
            return error(DBErrors.NoPrimaryKey, getName());

        // Check Columns
        DBColumn[] keyColumns = primaryKey.getColumns();
        if (key == null || key.length != keyColumns.length)
            return error(Errors.InvalidArg, key); // Invalid Argument

        // Delete References
        if (isCascadeDelete() && deleteAllReferences(key, conn)==false)
            return false; // Error deleting referenced records
        
        // Build SQL-Statement
        DBCommand cmd = db.createCommand();
        for (int i = 0; i < key.length; i++)
            cmd.where(keyColumns[i].is(key[i]));

        // Perform delete
        String sqlCmd = cmd.getDelete(this);
        int affected  = db.executeSQL(sqlCmd, null, conn);
        if (affected < 0)
        { // Delete Failed
            return error(db);
        } 
        else if (affected == 0)
        { // Record not found
            return error(DBErrors.RecordDeleteFailed, name);
        } 
        else if (affected > 1)
        { // Multiple Records affected
            return error(DBErrors.RecordUpdateInvalid, name);
        }
        // success
        return success();
    }

}