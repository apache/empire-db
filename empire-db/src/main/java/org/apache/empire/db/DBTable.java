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
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.commons.Options;
import org.apache.empire.commons.StringUtils;
import org.apache.empire.data.DataType;
import org.apache.empire.db.DBIndex.DBIndexType;
import org.apache.empire.db.DBRelation.DBCascadeAction;
import org.apache.empire.db.exceptions.NoPrimaryKeyException;
import org.apache.empire.db.exceptions.RecordDeleteFailedException;
import org.apache.empire.db.exceptions.RecordUpdateFailedException;
import org.apache.empire.exceptions.InvalidArgumentException;
import org.apache.empire.exceptions.ItemExistsException;
import org.apache.empire.exceptions.NotSupportedException;
import org.apache.empire.exceptions.ObjectNotValidException;
import org.apache.empire.exceptions.UnexpectedReturnValueException;
import org.apache.empire.exceptions.InvalidOperationException;


/**
 * This class represent one table of the database.
 * It contains methods to get, add, update and delete records from the database.
 *
 */
public class DBTable extends DBRowSet implements Cloneable
{
    // *Deprecated* private static final long serialVersionUID = 1L;
    
    // Integer size definitions
    public static final int INT_SIZE_DEFAULT = 0;
    public static final int INT_SIZE_SMALL   = 2;
    public static final int INT_SIZE_MEDIUM  = 4;
    public static final int INT_SIZE_BIG     = 8;

    private static AtomicInteger tableCount          = new AtomicInteger(0);

    private final String         name;
    private String               alias;
    private DBIndex              primaryKey          = null;
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
     * Returns an array of all primary key columns.
     * 
     * @return an array of all primary key columns
     */
    @Override
    public DBColumn[] getKeyColumns()
    {
        return ((primaryKey != null) ? primaryKey.getColumns() : null);
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
    public Object clone() throws CloneNotSupportedException 
    {
        DBTable clone = (DBTable) super.clone();
        initClonedFields(clone);
        // set new alias
        clone.alias = "t" + String.valueOf(tableCount.incrementAndGet());
        // done
        log.info("clone: Table " + name + " cloned! Alias old=" + alias + " new=" + clone.alias);
        return clone;
    }
    
    @SuppressWarnings("unchecked")
    public <T extends DBTable> T clone(String newAlias) 
    {
        try {
            DBTable clone = (DBTable) super.clone();
            initClonedFields(clone);
            // set new alias
            if (StringUtils.isEmpty(newAlias))
                clone.alias = "t" + String.valueOf(tableCount.incrementAndGet());
            else
                clone.alias = newAlias;
            // done
            log.info("clone: Table " + name + " cloned! Alias old=" + alias + " new=" + clone.alias);
            return (T)clone;
        } catch (CloneNotSupportedException e) {
            // unable to clone table
            log.error("Unable to clone table " + getName());
            throw new RuntimeException(e);
        }
    }
    
    protected <T extends DBTable> void initClonedFields(T clone) throws CloneNotSupportedException
    {
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
            {   // Find a class of Type DBColumn or DBTableColumn
                Field f = fields[j];
                Class<?> type = f.getType();
                if (type == colClass || type == colBase)
                {   try
                    {   // Check if the field points to the old Value
                        if (f.get(clone) == srcCol)
                        {   // Check accessible
                            if (f.isAccessible()==false) 
                            {   // not accessible
                                f.setAccessible(true);
                                try {
                                    f.set(clone, newCol);
                                } finally {
                                    f.setAccessible(false);
                                }
                            }
                            else
                            {   // already accessible
                                f.set(clone, newCol);
                            }
                        }
                    } catch (Exception e)  {
                        // IllegalAccessException or IllegalArgumentException
                        String fieldName = fields[j].getName();
                        log.error("Failed to modify declared table field: " + fieldName + ". Reason is: " + e.toString());
                        // throw CloneNotSupportedException
                        CloneNotSupportedException cnse = new CloneNotSupportedException("Unable to replace field reference for field " + fieldName);
                        cnse.initCause(e);
                        throw cnse;
                    }
                }
            }
        }
    }

    /**
     * Adds a column to this table's column list.
     * @param column a column object
     */
    protected void addColumn(DBTableColumn column)
    {   // find column by name
        if (column==null || column.getRowSet()!=this)
            throw new InvalidArgumentException("column", column);
        if (getColumn(column.getName())!=null)
            throw new ItemExistsException(column.getName());
        // add now
        columns.add(column);
    }

    /**
     * Creates a new Column object and appends it to the column list
     * @param columnName the column name
     * @param type the type of the column e.g. integer, text, date
     * @param size the column width
     * @param required true if not null column
     * @param defValue a Object object
     * @return the new column object
     */
    protected DBTableColumn createAndAppendColumn(String columnName, DataType type, double size, boolean required, Object defValue)
    {
        // Check exists
        if (getColumn(columnName)!=null)
            throw new ItemExistsException(columnName);
        // Make sure (DataType.INTEGER & DataMode.AutoGenerated) = DataType.AUTOINC
        boolean autoGenerated = (type==DataType.AUTOINC || type==DataType.UNIQUEID);
        DBTableColumn column = new DBTableColumn(this, type, columnName, size, required, autoGenerated, defValue);
        // auto-set primary key
        if (column.getDataType()==DataType.AUTOINC)
        {   // Automatically set primary key
            if (this.primaryKey==null)
                this.setPrimaryKey(column);
            else
                throw new InvalidOperationException("Table "+getName()+" already has a Primary-Key! No column of type AUTOINC can be added.");
        }
        // add now
        addColumn(column);
        return column;
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
     * @return the new column object 
     */
    public final DBTableColumn addColumn(String columnName, DataType type, double size, boolean required, Object defValue)
    {
        if (defValue instanceof Class<?>)
        {
            log.warn("Column {}: a class object of type \"{}\" has been passed as default value. Please check!", columnName, ((Class<?>)defValue).getName());
        }
        return this.createAndAppendColumn(columnName, type, size, required, defValue);
    }

    /**
     * Creates a new table column and adds it to the table's column list
     * 
     * @param columnName the column name
     * @param type the type of the column e.g. integer, text, date
     * @param size the column width
     * @param required true if not null column
     * @return the new column object 
     */
    public final DBTableColumn addColumn(String columnName, DataType type, double size, boolean required)
    { 
        return this.createAndAppendColumn(columnName, type, size, required, null);
    }

    /**
     * Creates a new table column with options and adds it to the table's column list
     * This overload should be used for column containing enum values which have no default value.
     * 
     * @param columnName the column name
     * @param type the type of the column e.g. integer, text, date
     * @param size the column width
     * @param required true if not null column
     * @param options this list of options
     * @return the new column object 
     */
    public final DBTableColumn addColumn(String columnName, DataType type, double size, boolean required, Options options)
    {
        DBTableColumn col = this.createAndAppendColumn(columnName, type, size, required, null);
        col.setOptions(options);
        return col;
    }

    /**
     * Creates a new table column with options and adds it to the table's column list
     * This overload should be used for column containing enum values which have a default value.
     * 
     * @param columnName the column name
     * @param type the type of the column e.g. integer, text, date
     * @param size the column width
     * @param required true if not null column
     * @param options this list of options
     * @param defValue the default value
     * @return the new column object 
     */
    public final DBTableColumn addColumn(String columnName, DataType type, double size, boolean required, Options options, Object defValue)
    { 
        // defValue must be part of options
        if (defValue!=null && !options.contains(defValue))
            throw new InvalidArgumentException("devValue", defValue);
        // add
        DBTableColumn col = this.createAndAppendColumn(columnName, type, size, required, defValue);
        col.setOptions(options);
        return col;
    }

    /**
     * Creates a new table column with Enum-Options and adds it to the table's column list
     * This overload should be used for column containing enum values which have no default value.
     * 
     * @param columnName the column name
     * @param type the type of the column e.g. integer, text, date
     * @param size the column width
     * @param required true if not null column
     * @param enumType  the class of the enum type
     * @return the new column object 
     */
    public final DBTableColumn addColumn(String columnName, DataType type, double size, boolean required, Class<?> enumType)
    {
        if (!enumType.isEnum())
        {   // Class must be an enum type
            throw new InvalidArgumentException("enumType", enumType);
        }
        DBTableColumn col = this.createAndAppendColumn(columnName, type, size, required, null);
        col.setEnumOptions(enumType);
        return col;
    }

    /**
     * Creates a new table column with Enum-Options and adds it to the table's column list
     * This overload should be used for column containing enum values which have a default value.
     * 
     * @param columnName the column name
     * @param type the type of the column e.g. integer, text, date
     * @param size the column width
     * @param required true if not null column
     * @param enumValue the default value
     * @return the new column object 
     */
    public final DBTableColumn addColumn(String columnName, DataType type, double size, boolean required, Enum<?> enumValue)
    { 
        Object defValue = ObjectUtils.getEnumValue(enumValue, type.isNumeric());
        DBTableColumn col = this.createAndAppendColumn(columnName, type, size, required, defValue);
        col.setEnumOptions(enumValue.getClass());
        return col;
    }
    
    /**
     * Adds an Identity column to the table which also serves as the PrimaryKey
     * An Identity Column is always an auto-generated Integer(Long) value
     * @param name the name of the identity column
     * @param seqName (optional) name of the sequence if supported by DBMS
     * @return the Identity column
     */
    public DBTableColumn addIdentity(String name, String seqName)
    {
        return addColumn(name, DataType.AUTOINC, INT_SIZE_BIG, true, seqName);
    }
    
    /**
     * Adds an Identity column to the table which also serves as the PrimaryKey
     * An Identity Column is always an auto-generated Integer(Long) value
     * @param name the name of the identity column
     * @return the Identity column
     */
    public final DBTableColumn addIdentity(String name)
    {
        return addIdentity(name, null);
    }
    
    /**
     * Adds a new ForgeinKey table column the column list
     * The foreign table must have a single column foreign key
     * @param name the name of the new column
     * @param target the table on which to reference
     * @param required true if the value is required
     * @param options (optional) a set of allowed values for this column
     * @return the new column
     */
    public DBTableColumn addForeignKey(String name, DBTable target, boolean required, Options options, DBCascadeAction cascadeAction)
    {
        // Check target: If null then Table has not been defined yet!
        if (target==null)
            throw new ObjectNotValidException(target);
        // Check key
        DBColumn[] keyCols = target.getKeyColumns();
        if (keyCols==null || keyCols.length!=1)
            throw new NotSupportedException(target, "addForeignKey");
        // add column
        DBTableColumn keyCol = (DBTableColumn)keyCols[0];
        DataType keyDataType = keyCol.getDataType();
        if (keyDataType==DataType.AUTOINC)
            keyDataType =DataType.INTEGER;
        DBTableColumn referenceColumn = addColumn(name, keyDataType, keyCol.getSize(), required, options); 
        // Adapter foreign key
        String fkName = getName() + "_" + name.replace("_ID", "_FK");
        DBRelation relation = db.addRelation(fkName, referenceColumn.referenceOn(keyCol));
        if (cascadeAction!=null)
            relation.setOnDeleteAction(cascadeAction);
        return referenceColumn;
    }
    
    /**
     * Adds a new ForgeinKey table column the column list
     * The foreign table must have a single column foreign key
     * @param name the name of the new column
     * @param target the table on which to reference
     * @param required true if the value is required
     * @param cascade whether or not to cascade deletes for this relation 
     * @return the new column
     */
    public final DBTableColumn addForeignKey(String name, DBTable target, boolean required, boolean cascade)
    {
        return addForeignKey(name, target, required, null, (cascade  ? DBCascadeAction.CASCADE : DBCascadeAction.NONE));
    }
    
    /**
     * Adds a new ForgeinKey table column the column list
     * The foreign table must have a single column foreign key
     * @param name the name of the new column
     * @param target the table on which to reference
     * @param required true if the value is required
     * @return the new column
     */
    public final DBTableColumn addForeignKey(String name, DBTable target, boolean required)
    {
        return addForeignKey(name, target, required, false);
    }
    
    /**
     * Adds a Timestamp column to the current table which will be used for optimistic locking.
     * There can only be one timestamp column per table
     * @param name the name of the new column
     * @return the new column
     */
    public DBTableColumn addTimestamp(String name)
    {
        // check
        if (this.timestampColumn!=null) {
            String msg = MessageFormat.format("A Timestamp column ({0}) already exists for table {1}", this.timestampColumn.getName(), this.getName());
            throw new InvalidOperationException(msg); 
        }
        // Add now
        DBTableColumn tsColumn = addColumn(name, DataType.TIMESTAMP, 0, true);
        this.setTimestampColumn(tsColumn);
        return tsColumn;
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
        return Collections.unmodifiableList(this.indexes);        
    }
    
    /**
     * Sets the primary key.
     * 
     * @param columns a array with one or more DBColumn objects
     */
    public void setPrimaryKey(DBColumn... columns)
    {
        checkParamNull("columns", columns);
        // All columns must belong to this table
        for (int i=0; i<columns.length; i++)
            if (columns[i].getRowSet()!=this)
                throw new InvalidArgumentException("columns["+String.valueOf(i)+"]", columns[i].getFullName());
        // Check if already exists
        if (primaryKey!=null)
        {   // compare columns
            if (primaryKey.compareColumns(columns))
                return; // already set
            // warn
            log.warn("A PrimaryKey for the Table {} has already been set. Replacing with new one.", getName());
            // new key
            removeIndex(primaryKey);
        }
        // Set primary Key now
        if (columns.length>0)
        {   // create primary key
            primaryKey = new DBIndex(name + "_PK", DBIndexType.PRIMARY_KEY, columns);
            addIndex(primaryKey);
            // log
            log.debug("PrimaryKey {} of length {} has been set for table {}", primaryKey.getName(), columns.length, getName());
        }
        else
        {   // No primary Key
            primaryKey = null;
        }
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
     * @param type the index type
     * @param columns the columns indexed by this index
     * 
     * @return the Index object
     */
    public final DBIndex addIndex(String name, DBIndexType type, DBColumn... columns)
    {
        if (name==null || columns==null || columns.length==0)
            throw new InvalidArgumentException("name|columns", null);
        if (type==DBIndexType.PRIMARY_KEY && this.primaryKey!=null)
            throw new InvalidArgumentException("type", DBIndexType.PRIMARY_KEY.name());
        // add Index now
        DBIndex index = new DBIndex(name, type, columns);
        addIndex(index);
        return index;
    }
    
    /**
     * Adds an index.
     * Overload for convenience
     */
    public final DBIndex addIndex(String name, boolean unique, DBColumn... columns)
    {
        return addIndex(name, (unique) ? DBIndexType.UNIQUE : DBIndexType.STANDARD, columns);
    }

    /**
     * removes an index.
     * 
     * @param index the index to remove
     */
    public void removeIndex(DBIndex index)
    {
        if (index.getTable()!=this || !indexes.contains(index))
            throw new InvalidArgumentException("index", index);
        // table
        indexes.remove(index);
        index.setTable(null);
    }

    /**
     * Adds the table's name to the supplied sql command buffer.
     * 
     * @param sql the SQL-Command
     * @param context the current SQL-Command context
     */
    @Override
    public void addSQL(DBSQLBuilder sql, long context)
    {
        // Append Name
        if ((context & CTX_NAME|CTX_FULLNAME)!=0)
        {   // append Qualified Name 
            db.appendQualifiedName(sql, name, quoteName);
        }
        // Append Alias
        if ((context & CTX_ALIAS)!=0 && alias!=null)
        {    // append alias
             sql.append(getRenameTablePhrase());
             sql.append(getAlias());
        }
    }

    /**
     * Gets all table fields and the fields properties. 
     * Set this to the specified DBRecord object.
     * 
     * @param record the DBRecord object. contains all fields and the field properties
     * @param initalKey the initial key values
     * @param deferredInit flag whether to defer initialization
     */
    @Override
    public void createRecord(DBRecordBase record, Object[] initalKey, boolean deferredInit)
    {
        FieldInitMode fieldInitMode = (deferredInit ? FieldInitMode.SET_DEFAULTS_DEFERRED : FieldInitMode.SET_DEFAULTS);
        super.initRecord(record, initalKey, fieldInitMode, true);
    }
    
    /**
     * Checks weather a unique constraint is violated when inserting or updating a record.<BR>
     * 
     * @param record the record to check
     */
    public DBIndex checkUniqueConstraints(DBRecordBase record)
    {
        for (DBIndex idx : getIndexes())
        {
            if (idx.getType()==DBIndexType.PRIMARY_KEY)
            {   // Only for new records
                if (!record.isNew())
                    continue; // not new
            }
            else if (idx.getType().isUnique())
            {   // check if any of the fields were actually changed
                if (!record.isNew() && !record.wasAnyModified(idx.getColumns()))
                    continue; // not modified
            }
            else 
            {   // No unique index
                continue;
            }
            // Check index
            DBCommand cmd = createRecordCommand(record.getContext());
            cmd.select(count());
            for (DBColumn c : idx.getColumns())
            {
                Object value = record.get(c);
                cmd.where(c.is(value));
            }
            DBUtils utils = record.getContext().getUtils();
            int count = utils.querySingleInt(cmd);
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
     * @param context the database context
     */
    @Override
    public void deleteRecord(Object[] key, DBContext context)
    {
        // Check Primary key
        if (primaryKey == null )
            throw new NoPrimaryKeyException(this);

        // Check Columns
        DBColumn[] keyColumns = primaryKey.getColumns();
        if (key == null || key.length != keyColumns.length)
            throw new InvalidArgumentException("key", key);

        // Delete References
        deleteAllReferences(key, context);
        
        // Build SQL-Statement
        DBCommand cmd = createRecordCommand(context);
        // Set key constraints
        cmd.where(getKeyConstraints(key));
        // Perform delete
        String sqlCmd = cmd.getDelete(this);
        int affected  = context.executeSQL(sqlCmd, cmd.getParamValues());
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
            throw new RecordUpdateFailedException(this, key);
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
        return Collections.unmodifiableList(relations);        
    }

    /**
     * validates a column value
     * @return the validated (possibly converted) value
     */
    protected Object validateValue(DBTableColumn column, Object value)
    {
        return db.validateValue(column, value);
    }
    
    /**
     * initializes the Record Default Values
     * @param record the record
     * @param fieldInitMode the field initialization mode
     */
    @Override
    protected void initRecordDefaultValues(DBRecordBase record, FieldInitMode fieldInitMode)
    {
        // check field init mode
        if (fieldInitMode==FieldInitMode.NONE)
            throw new InvalidArgumentException("fieldInitMode", fieldInitMode);
        // Use connection if not deferred
        Connection conn = (fieldInitMode==FieldInitMode.SET_DEFAULTS_DEFERRED ? null : record.getContext().getConnection());
        Object[] fields = record.getFields();
        // Set Default values
        // ATTENTION: Do not set to ObjectUtils.NO_VALUE
        for (int i = 0; i < fields.length; i++)
        {   // already set ?
            if (fields[i]!=null)
                continue; 
            // check default
            DBColumn column = columns.get(i);
            // getDefaultValue
            Object value = ((DBTableColumn)column).getRecordDefaultValue(conn);
            if (ObjectUtils.isEmpty(value))
                continue;
            // Initial value
            // Don't set to modified: record.modifyValue(i, value, false); 
            fields[i] = value;
        }
    }
    
}