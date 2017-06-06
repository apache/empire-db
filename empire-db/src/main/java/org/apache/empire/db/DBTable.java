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
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.empire.data.DataMode;
import org.apache.empire.data.DataType;
import org.apache.empire.db.DBRelation.DBCascadeAction;
import org.apache.empire.db.exceptions.NoPrimaryKeyException;
import org.apache.empire.db.exceptions.RecordDeleteFailedException;
import org.apache.empire.db.exceptions.RecordUpdateInvalidException;
import org.apache.empire.exceptions.InvalidArgumentException;
import org.apache.empire.exceptions.ItemExistsException;
import org.apache.empire.exceptions.UnexpectedReturnValueException;


/**
 * This class represent one table of the database.
 * It contains methods to get, add, update and delete records from the database.
 *
 */
public class DBTable extends DBRowSet implements Cloneable
{
    // Integer size definitions
    public static final int DEFAULT   = 0;
    public static final int SMALLINT  = 2;
    public static final int MEDIUMINT = 4;
    public static final int BIGINT    = 8;

    private final static long    serialVersionUID    = 1L;
    private static AtomicInteger tableCount          = new AtomicInteger(0);

    private final String         name;
    private String               alias;
    private final List<DBIndex>  indexes             = new ArrayList<DBIndex>();
    private Boolean              quoteName           = null;
    private DBCascadeAction      cascadeDeleteAction = DBCascadeAction.NONE;
    
    /**
     * Construct a new DBTable object set the specified parameters
     * to this object and add this object to the current database.
     * 
     * @param name the table name
     * @param db the valid database object
     */
    public DBTable(String name, DBDatabase db, String alias)
    { 
        super(db);
        // generate alias
        if (alias==null)
            alias = "t" + String.valueOf(tableCount.incrementAndGet());
        // init
        this.name = name;
        this.alias = alias;
        // Add Table to Database
        if (db != null)
            db.addTable(this);
    }

    /**
     * Construct a new DBTable object set the specified parameters
     * to this object and add this object to the current database.
     * 
     * @param name the table name
     * @param db the valid database object
     */
    public DBTable(String name, DBDatabase db)
    { 
        this(name, db, null);
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
     * Returns whether or not the table supports record updates. Default is true.
     * @return true if the table allows record updates
     */
    @Override
    public boolean isUpdateable()
    {
        return true;
    }

    /**
     * Clones this table and assigns a new table alias.
     * This second instance of the same table can be used for self-joins.
     * <pre>
     * This method requires that all declared column fields are NOT declared final.
     * i.e. instead of:
     * 
     *      public final DBTableColumn MYCOL;
     * 
     * columns must be declared:
     * 
     *      public DBTableColumn MYCOL;
     *
     * A runtime exception for the CloneNotSupported will be thrown if references cannot be adjusted.
     *
     * Alternatively a second table instance may be created manually like this:
     *      
     *      public final MyTable MYTABLE1 = new MyTable();
     *      public final MyTable MYTABLE2 = new MyTable();
     *
     *      ...
     *      cmd.join(MYTABLE1.ID, MYTABLE2.PARENTID); // self-join
     *      ...
     * </pre>
     * @return a table clone with new table alias
     */
    @Override
    public Object clone() throws CloneNotSupportedException {
        try {
            DBTable clone = (DBTable) super.clone();
            // clone all columns
            Class<?> colClass = columns.get(0).getClass();
            Class<?> colBase = colClass.getSuperclass();
            clone.columns = new ArrayList<DBColumn>();
            Field[] fields = getClass().getFields();
            for (int i = 0; i < columns.size(); i++)
            {
                DBTableColumn srcCol = (DBTableColumn) columns.get(i);
                DBTableColumn newCol = new DBTableColumn(clone, srcCol);
                // Replace all references for oldCol to newCol
                for (int j = 0; j < fields.length; j++)
                { // Find a class of Type DBColumn or DBTableColumn
                    Class<?> type = fields[j].getType();
                    if (type == colClass || type == colBase)
                    {
                        try
                        {   // Check if the field points to the old Value
                            if (fields[j].get(clone) == srcCol)
                              fields[j].set(clone, newCol);
                        } catch (Exception e)  {
                            // IllegalAccessException or IllegalArgumentException
                            String fieldName = fields[j].getName();
                            log.error("Cannot adjust declared table field: " + fieldName + ". Reason is: " + e.getMessage());
                            // throw CloneNotSupportedException
                            CloneNotSupportedException cnse = new CloneNotSupportedException("Unable to replace field reference for field " + fieldName);
                            cnse.initCause(e);
                            throw cnse;
                        }
                    }
                }
            }
            // set new alias
            clone.alias = "t" + String.valueOf(tableCount.incrementAndGet());
            // done
            log.info("clone: Table " + name + " cloned! Alias old=" + alias + " new=" + clone.alias);
            return clone;

        } catch (CloneNotSupportedException e) {
            // unable to clone table
            log.error("Unable to clone table " + getName());
            throw new RuntimeException(e);
        }
    }

    /**
     * Adds a column to this table's column list.
     * 
     * @param column a column object
     */
    protected void addColumn(DBTableColumn column)
    { // find column by name
        if (column==null || column.getRowSet()!=this)
            throw new InvalidArgumentException("column", column);
        if (getColumn(column.getName())!=null)
            throw new ItemExistsException(column.getName());
        // add now
        columns.add(column);
    }

    /**
     * Creates a new DBTableColumn object and adds it to the column collection.
     * 
     * @param columnName the column name
     * @param type the type of the column e.g. integer, text, date
     * @param size the column width
     * @param dataMode determines whether this column is optional, required or auto-generated 
     * @param defValue a Object object
     * @return the created DBTableColumn object
     */
    public final DBTableColumn addColumn(String columnName, DataType type, double size, DataMode dataMode, Object defValue)
    { 
        return new DBTableColumn(this, type, columnName, size, dataMode, defValue);
    }

    /**
     * Creates a new DBTableColumn object and adds it to the column collection.
     * 
     * @param columnName the column name
     * @param type the type of the column e.g. integer, text, date
     * @param size the column width
     * @param dataMode determines whether this column is optional, required or auto-generated 
     * @return the created DBTableColumn object
     */
    public final DBTableColumn addColumn(String columnName, DataType type, double size, DataMode dataMode)
    { 
        return new DBTableColumn(this, type, columnName, size, dataMode, null);
    }

    /**
     * Creates a new DBTableColumn object and adds it to the column collection.
     * Instead of the data mode enum, a boolean flag is used to indicate whether the column is required or optional.
     * 
     * @param columnName the column name
     * @param type the type of the column e.g. integer, text, date
     * @param size the column width
     * @param required true if not null column
     * @param defValue a Object object
     * @return the created DBTableColumn object
     */
    public final DBTableColumn addColumn(String columnName, DataType type, double size, boolean required, Object defValue)
    { 
        DataMode dm = (required ? DataMode.NotNull : DataMode.Nullable);
        return new DBTableColumn(this, type, columnName, size, dm, defValue);
    }

    /**
     * Creates a new DBTableColumn object and adds it to the column collection.
     * Instead of the data mode enum, a boolean flag is used to indicate whether the column is required or optional. 
     * 
     * @param columnName the column name
     * @param type the type of the column e.g. integer, text, date
     * @param size the column width
     * @param required true if not null column
     * @return the created DBTableColumn object
     */
    public final DBTableColumn addColumn(String columnName, DataType type, double size, boolean required)
    { 
        DataMode dm = (required ? DataMode.NotNull : DataMode.Nullable);
        return new DBTableColumn(this, type, columnName, size, dm, null);
    }

    /**
     * Returns the primary key.
     * 
     * @return the the DBIndex object -&gt; primary key
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
     */
    public void setPrimaryKey(DBColumn... columns)
    {
        if (columns==null || columns.length==0)
            throw new InvalidArgumentException("columns", columns);
        // All columns must belong to this table
        for (int i=0; i<columns.length; i++)
            if (columns[i].getRowSet()!=this)
                throw new InvalidArgumentException("columns["+String.valueOf(i)+"]", columns[i].getFullName());
        // Set primary Key now
        primaryKey = new DBIndex(name + "_PK", DBIndex.PRIMARYKEY, columns);
        indexes.add(primaryKey);
        primaryKey.setTable(this);
    }

    /**
     * Adds an index.
     * 
     * @param index the index to add
     */
    public DBIndex addIndex(DBIndex index)
    {
        if (index==null)
            throw new InvalidArgumentException("index", null);
        // Check index name
        String name = index.getName();
        for (DBIndex i : indexes)
        {
            if (i==index || name.equalsIgnoreCase(i.getName()))
            {
                throw new ItemExistsException(name);
            }
        }        
        // add Index now
        indexes.add(index);
        index.setTable(this);
        return index;
    }

    /**
     * Adds an index.
     * 
     * @param name the index name
     * @param unique is this a unique index
     * @param columns the columns indexed by this index
     * 
     * @return the Index object
     */
    public final DBIndex addIndex(String name, boolean unique, DBColumn... columns)
    {
        if (name==null || columns==null || columns.length==0)
            throw new InvalidArgumentException("name|columns", null);
        // add Index now
        DBIndex index = new DBIndex(name, (unique) ? DBIndex.UNIQUE : DBIndex.STANDARD, columns);
        addIndex(index);
        return index;
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
        DBTableColumn col = addColumn(columnName, DataType.DATETIME, 0, DataMode.AutoGenerated, DBDatabase.SYSDATE);
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
             buf.append(getAlias());
        }
    }

    /**
     * Gets all table fields and the fields properties. 
     * Set this to the specified DBRecord object.
     * 
     * @param rec the DBRecord object. contains all fields and the field properties
     * @param conn a valid connection to the database.
     */
    @Override
    public void createRecord(DBRecord rec, Connection conn)
    {
        // Prepare
        prepareInitRecord(rec, null, true);
        // Set Defaults
        int count = columns.size();
        for (int i = 0; i < count; i++)
        {
            DBTableColumn column = (DBTableColumn)columns.get(i);
            Object value = column.getRecordDefaultValue(conn);
            if (value!=null)
                rec.modifyValue(i, value, true); 
        }
        // Init
        completeInitRecord(rec);
    }
    
    /**
     * Checks weather a unique constraint is violated when inserting or updating a record.<BR>
     * <P>
     * @param id the record's primary key
     * @param conn a valid JDBC connection
     */
    public DBIndex checkUniqueConstraints(DBRecord rec, Connection conn)
    {
        for (DBIndex idx : getIndexes())
        {
            if (idx.getType()==DBIndex.PRIMARYKEY)
            {   // Only for new records
                if (!rec.isNew())
                    continue; // not new
            }
            else if (idx.getType()==DBIndex.UNIQUE)
            {   // check if any of the fields were actually changed
                if (!rec.isNew() && !rec.wasAnyModified(idx.getColumns()))
                    continue; // not modified
            }
            else 
            {   // No unique index
                continue;
            }
            // Check index
            DBCommand cmd = db.createCommand();
            cmd.select(count());
            for (DBColumn c : idx.getColumns())
            {
                Object value = rec.getValue(c);
                cmd.where(c.is(value));
            }
            int count = db.querySingleInt(cmd, conn);
            if (count>0)
            {   // Index is violated
                return idx;
            }
        }
        // no index violation detected
        return null;
    }
    
    /**
     * returns the default cascade action for deletes on this table.
     * This is used as the default for newly created relations on this table and does not affect existing relations.
     * @return the delete cascade action for new relations (DBRelation.DBCascadeAction.CASCADE_RECORDS) are enabled
     */
    public DBCascadeAction getDefaultCascadeDeleteAction()
    {
        return cascadeDeleteAction;
    }

    /**
     * sets the default cascade action for deletes on foreign key relations.
     * @param cascadeDeleteAction cascade action for deletes (DBRelation.DBCascadeAction.CASCADE_RECORDS)
     */
    public void setDefaultCascadeDeleteAction(DBCascadeAction cascadeDeleteAction)
    {
        this.cascadeDeleteAction = cascadeDeleteAction;
    }

    /**
     * Creates a delete SQL-Command by using the DBCommand getDelete method
     * execute the the SQL-Command with the DBDatabase
     * executeSQL method.
     * 
     * @param key an array of the primary key columns
     * @param conn a valid connection to the database.
     */
    @Override
    public void deleteRecord(Object[] key, Connection conn)
    {
        // Check Primary key
        if (primaryKey == null )
            throw new NoPrimaryKeyException(this);

        // Check Columns
        DBColumn[] keyColumns = primaryKey.getColumns();
        if (key == null || key.length != keyColumns.length)
            throw new InvalidArgumentException("key", key);

        // Delete References
        deleteAllReferences(key, conn);
        
        // Build SQL-Statement
        DBCommand cmd = db.createCommand();
        // Set key constraints
        setKeyConstraints(cmd, key);
        // Perform delete
        String sqlCmd = cmd.getDelete(this);
        int affected  = db.executeSQL(sqlCmd, cmd.getParamValues(), conn);
        if (affected < 0)
        { // Delete Failed
            throw new UnexpectedReturnValueException(affected, "db.executeSQL()");
        } 
        else if (affected == 0)
        { // Record not found
            throw new RecordDeleteFailedException(this, key);
        } 
        else if (affected > 1)
        { // Multiple Records affected
            throw new RecordUpdateInvalidException(this, key);
        }
    }
    
    /**
     * Returns a list of all foreign key relations for this table
     * @return the list of foreign key relations
     */
    public List<DBRelation> getForeignKeyRelations()
    {
        List<DBRelation> relations = new ArrayList<DBRelation>();
        for (DBRelation r : getDatabase().getRelations())
        {   // check relation
            if (this.equals(r.getForeignKeyTable()))
                relations.add(r);
        }
        return relations;
    }

}