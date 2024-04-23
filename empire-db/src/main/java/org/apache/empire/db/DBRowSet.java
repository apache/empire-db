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
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.commons.StringUtils;
import org.apache.empire.data.Column;
import org.apache.empire.data.ColumnExpr;
import org.apache.empire.data.DataType;
import org.apache.empire.data.EntityType;
import org.apache.empire.data.Record;
import org.apache.empire.db.DBRelation.DBCascadeAction;
import org.apache.empire.db.DBRelation.DBReference;
import org.apache.empire.db.context.DBContextBase;
import org.apache.empire.db.exceptions.FieldNotNullException;
import org.apache.empire.db.exceptions.FieldReadOnlyException;
import org.apache.empire.db.exceptions.InvalidKeyException;
import org.apache.empire.db.exceptions.NoPrimaryKeyException;
import org.apache.empire.db.exceptions.QueryNoResultException;
import org.apache.empire.db.exceptions.RecordNotFoundException;
import org.apache.empire.db.exceptions.RecordUpdateAmbiguousException;
import org.apache.empire.db.exceptions.RecordUpdateFailedException;
import org.apache.empire.db.expr.column.DBCountExpr;
import org.apache.empire.db.expr.compare.DBCompareColExpr;
import org.apache.empire.db.expr.compare.DBCompareExpr;
import org.apache.empire.db.expr.join.DBCompareJoinExpr;
import org.apache.empire.db.expr.join.DBCrossJoinExpr;
import org.apache.empire.db.list.DBBeanFactoryCache;
import org.apache.empire.db.list.DBBeanListFactory;
import org.apache.empire.db.list.DBBeanListFactoryImpl;
import org.apache.empire.dbms.DBMSFeature;
import org.apache.empire.dbms.DBMSHandler;
import org.apache.empire.dbms.DBSqlPhrase;
import org.apache.empire.exceptions.InvalidArgumentException;
import org.apache.empire.exceptions.ItemNotFoundException;
import org.apache.empire.exceptions.NotSupportedException;
import org.apache.empire.exceptions.ObjectNotValidException;
import org.apache.empire.exceptions.UnexpectedReturnValueException;
import org.apache.empire.exceptions.InvalidOperationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class is the base class for all the DBTable,
 * DBView and DBQuery classes this class contains all the columns of the
 * tables, views or queries
 * <P>
 * 
 *
 */
public abstract class DBRowSet extends DBExpr implements EntityType
{
    // *Deprecated* private static final long serialVersionUID = 1L;

    public enum PartialMode
    {
        INCLUDE,
        EXCLUDE;
    }

    public enum FieldInitMode
    {
        NONE,
        SET_DEFAULTS,
        SET_DEFAULTS_DEFERRED;
    }
    
    /**
     * This class is used to set the auto generated key of a record if the database does not support sequences.
     * It is used with the executeSQL function and only required for insert statements
     */
    private static class DBSetRecordKey implements DBMSHandler.DBSetGenKeys
    {
        private Object[] fields;
        private int index; 
        public DBSetRecordKey(Object[] fields, int index)
        {
            this.fields = fields;
            this.index = index;
        }
        @Override
        public void set(int rownum, Object value)
        {
            fields[index]=value;
        }
    }

    // Logger
    protected static final Logger                log         = LoggerFactory.getLogger(DBRowSet.class);

    /*
     * Do we need a BeanRowsetMap?
     * 
    private static final Map<Class<?>, DBRowSet> beanRowsetMap = new HashMap<Class<?>, DBRowSet>(); // Concurrent ?
   
    public static synchronized DBRowSet getRowsetforType(Class<?> beanType, boolean checkExists)
    {
        DBRowSet rowset = beanRowsetMap.get(beanType); 
        if (rowset==null && checkExists)
            throw new UnknownBeanTypeException(beanType);
        return rowset;
    }

    public static synchronized void setRowsetForType(Class<?> beanType, DBRowSet rowset)
    {
        if (rowset!=null)
        {   // Check previous
            DBRowSet prev = beanRowsetMap.get(beanType);
            if (prev!=null && prev!=rowset)
                log.warn("The Java bean type '{}' has already been assigned to a different DBRowSet {}. Assiging now to {}", beanType.getName(), prev.getName(), rowset.getName());
            // Assign now
            beanRowsetMap.put(beanType, rowset);
        }
        else
            beanRowsetMap.remove(beanType);
    }
     */
    
    // Members
    protected final DBDatabase         db;     /* transient ? */
    protected String                   comment          = null;
    protected String                   entityName       = null;
    protected DBColumn                 timestampColumn  = null;
    protected Map<DBColumn, DBColumn>  columnReferences = null;
    protected List<DBColumn>           columns          = new ArrayList<DBColumn>();

    // associated Entity Bean Class (optional)
    protected Class<?>                 beanType         = null;

    /**
     * Internally used for parameter checking
     * @param record the record to check
     * @param checkValid flag whether to check the validity
     */
    protected void checkParamRecord(DBRecordBase record, boolean checkValid)
    {
        // special
        if ((record instanceof DBRecordBean) && !checkValid)
        {   // set rowset of DBRecordBean
            ((DBRecordBean)record).rowset = this;
        }
        // check param
        if (record==null || record.getRowSet()!=this)
            throw new InvalidArgumentException("record", record);
        // check valid
        if (checkValid && !record.isValid())
            throw new ObjectNotValidException(record);
    }
    
    /**
     * varArgs to Array
     * @param parts the parts
     * @return the key columns array
     */
    public static DBColumn[] key(DBColumn... parts)
    {
        return parts;
    }

    /**
     * Constructs a DBRecord object set the current database object.
     * @param db the database object
     */
    public DBRowSet(DBDatabase db)
    {
        this.db = db;
    }

    /**
     * Gets an identifier for this RowSet Object
     * @return the rowset identifier
     */
    public String getIdentifier()
    {
        return db.getIdentifier()+"."+getName();
    }

    /**
     * returns a rowset by its identifier
     * @param rowsetId the id of the rowset
     * @return the rowset object
     * 
    public static DBRowSet findByIdentifier(String rowsetId)
    {
        int i = rowsetId.lastIndexOf('.');
        if (i<0)
            throw new InvalidArgumentException("rowsetId", rowsetId);
        // database suchen
        String dbid = rowsetId.substring(0, i);
        DBDatabase db = DBDatabase.findByIdentifier(dbid);
        if (db==null)
            throw new ItemNotFoundException(dbid);
        // rowset suchen
        String rsname = rowsetId.substring(i+1);
        DBRowSet rset = db.getRowSet(rsname);
        if (rset==null)
            throw new ItemNotFoundException(rowsetId);
        return rset;
    }
     */
    
    /**
     * Custom serialization for transient database.
     * 
    private void writeObject(ObjectOutputStream strm) throws IOException 
    {   // Database
        strm.writeObject(db.getIdentifier());
        // write the rest
        strm.defaultWriteObject();
    }

    private void readObject(ObjectInputStream strm) throws IOException, ClassNotFoundException
    {   // Database
        String dbid = String.valueOf(strm.readObject());
        // find database
        DBDatabase dbo = DBDatabase.findByIdentifier(dbid);
        if (dbo==null)
            throw new ItemNotFoundException(dbid);
        // set final field
        ClassUtils.setPrivateFieldValue(DBRowSet.class, this, "db", dbo);
        // read the rest
        strm.defaultReadObject();
    }
     */
    
    @Override 
    public int hashCode() 
    {
    	String nameWithAlias = getFullName()+"_"+getAlias();
    	return nameWithAlias.hashCode();
    }

    @Override
    public boolean equals(Object other)
    {
        if (other==this)
            return true;
        if (db==null)
            return super.equals(other);
        if (other instanceof DBRowSet)
        {   // Database and name must match
            DBRowSet r = (DBRowSet) other; 
            if (db.equals(r.getDatabase())==false)
                return false;
            // Check Alias
            if (getAlias()==null)
                return super.equals(other);
            // check for equal names
            return StringUtils.compareEqual(getAlias(), r.getAlias(), true);
        }
        return false;
    }
    
    /**
     * Compares the rowset to another one
     * @param other the rowset to compare this rowset with
     * @return true if it is the same rowset (but maybe a different instance)
     */
    public boolean isSame(DBRowSet other)
    {
        // record
        if (!getDatabase().equals(other.getDatabase()))
            return false;
        // compare names
        return StringUtils.compareEqual(getName(), other.getName(), true);
    }

    // ------- Abstract Methods -------
    
    public abstract String getName();
    
    public abstract String getAlias();
    
    public abstract boolean isUpdateable();

    public abstract void createRecord(DBRecordBase record, Object[] initalKey, boolean deferredInit);

    public abstract void deleteRecord(Object[] key, DBContext context);
    
    @Override
    public abstract DBColumn[] getKeyColumns();
    
    /**
     * Returns the full qualified name of the rowset.
     * <P>
     * @return the full qualified name
     */
    public String getFullName()
    {
        String  name   = getName();
        String  schema = db.getSchema();
        return (schema!=null) ? StringUtils.concat(schema, ".", name) : name;
    }

    /**
     * Returns the entity name creating qualified names. 
     * This is usually the same as "getName()" but it may be overridden to return singular instead of plural
     * @return the entity name
     */
    @Override
    public String getEntityName()
    {
        return StringUtils.coalesce(entityName, getName());
    }

    /**
     * sets the entity name for creating qualified names. 
     * This is usefull if the table or view name is plural but you want to qualifiy names in singular
     * @param entityName the entity name
     */
    protected void setEntityName(String entityName)
    {
        this.entityName = entityName;
    }
 
    /**
     * returns the bean type for this rowset
     * @return the bean type
     */
    @Override
    public Class<?> getBeanType()
    {
        return beanType;
    }
    
    /**
     * sets the bean type for this rowset
     * @param beanType the bean type for this rowset
     */
    public void setBeanType(Class<?> beanType)
    {
        setBeanType(beanType, null);    
    }
    
    /**
     * sets the bean type for this rowset
     * @param beanType the bean type for this rowset
     */
    public <T> void setBeanType(Class<T> beanType, DBBeanListFactory<T> factory)
    {
        // set
        this.beanType = beanType;
        // set the entity name (if not already set)
        if (this.entityName==null)
            this.setEntityName(beanType.getSimpleName().toUpperCase());
        // create default factory if not provided
        if (factory==null)
            factory = new DBBeanListFactoryImpl<T>(beanType, getKeyColumns(), getColumns());
        // set to global map
        DBBeanFactoryCache.setFactoryForType(beanType, factory);
    }

    /**
     * @see org.apache.empire.db.DBExpr#addReferencedColumns(Set)
     */
    @Override
    public void addReferencedColumns(Set<DBColumn> list)
    {
        list.addAll(columns);
    }

    /**
     * Returns the current DBDatabase object.
     * <P>
     * @return the current DBDatabase object
     */
    @SuppressWarnings("unchecked")
    @Override
	public DBDatabase getDatabase()
    {
        return db;
    }

    /**
     * Gets all columns of this rowset (e.g. for cmd.select()).
     * @return all columns of this rowset
     */
    @Override
    public List<DBColumn> getColumns()
    {
        return Collections.unmodifiableList(columns);
    }

    /**
     * Gets the index of a particular column expression.
     * @param column column the DBColumn to get the index for
     * @return the position of a column expression
     */
    public int getColumnIndex(DBColumn column)
    {
        return columns.indexOf(column);
    }
    
    /**
     * Gets the index of a particular column expression.
     * @param columnExpr the Column to get the index for
     * @return the position of a column expression
     */
    public int getColumnIndex(ColumnExpr columnExpr)
    {
        columnExpr = ObjectUtils.unwrap(columnExpr);
        if (columnExpr instanceof DBColumn)
            return getColumnIndex((DBColumn)columnExpr);
        else {
            Column source = columnExpr.getUpdateColumn();
            if (source instanceof DBColumn)
                return getColumnIndex((DBColumn)source);
        }
        // not found
        return -1;
    }

    /**
     * Returns a DBColumn object by a specified index value.
     * @param iColumn the index to get the DBColumn for
     * @return the index value
     */
    public DBColumn getColumn(int iColumn)
    {
        if (iColumn < 0 || iColumn >= columns.size())
            return null;
        return columns.get(iColumn);
    }

    /**
     * Gets the column Expression with a particular name.
     * @param name the name of the column to look for 
     * @return the column Expression at position
     */
    public DBColumn getColumn(String name)
    {
        for (int i = 0; i < columns.size(); i++)
        {
            DBColumn col = columns.get(i);
            if (col.getName().equalsIgnoreCase(name))
                return col;
        }
        return null;
    }

    /**
     * Checks whether a column is read only or writable.
     * Only the timestamp column is read only by default.
     * The primary is read only if the column is of type.
     * @param col the column object 
     * @return a new DBCountExpr object
     */
    public boolean isColumnReadOnly(DBColumn col)
    {
        if (getColumnIndex(col)<0)
            return true; // not found!
        if (col.isAutoGenerated() || col==timestampColumn)
            return true; // timestamp column
        // Check Update Column
        return (col.isReadOnly());
    }
    
    /**
     * Checks whether a given column is part of the primary key for this RowSet 
     * @param column the column to check
     * @return true if the column is part of the primary key or false otherwise
     */
    public boolean isKeyColumn(DBColumn column)
    {
        Column[] keyColumns = getKeyColumns();
        return ObjectUtils.contains(keyColumns, column);
    }

    /**
     * @return Returns the comment.
     */
    public String getComment()
    {
        return comment;
    }

    /**
     * @param comment The comment to set.
     */
    public void setComment(String comment)
    {
        this.comment = comment;
    }
    
    /**
     * @return Returns the timestampColumn.
     */
    public DBColumn getTimestampColumn()
    {
        return timestampColumn;
    }
    
    /**
     * @param timestampColumn The timestampColumn to set.
     */
    public void setTimestampColumn(DBColumn timestampColumn)
    {
        if (timestampColumn!=null && timestampColumn.getRowSet()!=this)
            throw new InvalidArgumentException("timestampColumn", timestampColumn);
        if (timestampColumn!=null && this.timestampColumn!=null && this.timestampColumn!=timestampColumn)
            throw new InvalidOperationException("A Timestamp column has already been set for rowset "+getName());
        if (timestampColumn instanceof DBTableColumn)
            ((DBTableColumn) timestampColumn).setReadOnly(true);
        // set now
        this.timestampColumn = timestampColumn;
        // log
        log.debug("Timestamp column {} has been set for table {}", timestampColumn.getName(), getName());
    }
    
    /**
     * Returns the a list of column references.
     * 
     * @return a list of references
     */
    public Map<DBColumn, DBColumn> getColumnReferences()
    {
        return columnReferences; 
    }
    
    /**
     * Adds a column reference to the list of table references.
     * This method is internally called from DBDatabase.addReleation().
     * 
     * @param source a column reference for one of this table's column
     * @param target the target column to which the source column references
     */
    protected void addColumnReference(DBColumn source, DBColumn target)
    {
        if (source.getRowSet()!=this)
            throw new InvalidArgumentException("column", source.getFullName());
        if (columnReferences== null)
            columnReferences = new HashMap<DBColumn, DBColumn>();
        // Check if column is already there
        columnReferences.put(source, target);
    }
    
    /**
     * Returns a new DBCountExpr object.
     * 
     * @return a new DBCountExpr object
     */
    public DBColumnExpr count()
    {
        return new DBCountExpr(this);
    }
    
    /**
     * Returns the sql phrase for renaming tables.
     * usually just a space character ' '
     * 
     * @return the table rename phrase
     */
    protected String getRenameTablePhrase()
    {
        if (db==null || db.getDbms()==null)
            return " ";
        return db.getDbms().getSQLPhrase(DBSqlPhrase.SQL_RENAME_TABLE);
    }

    /**
     * Returns the column expression at a given column index
     * Allow overrides in derived classes
     * @param index
     * @return the column expression
     */
    protected DBColumnExpr getColumnExprAt(int index)
    {
        return columns.get(index);
    }
    
    /**
     * Creates a join expression based on a compare expression
     */
    public DBCompareJoinExpr on(DBCompareExpr cmp)
    {
        DBCompareJoinExpr join = new DBCompareJoinExpr(this, cmp, DBJoinType.INNER); 
        return join;
    }
    
    /**
     * Creates a cross join expression
     */
    public DBCrossJoinExpr on(DBRowSet right)
    {
        DBCrossJoinExpr join = new DBCrossJoinExpr(this, right);
        return join;
    }

    /**
     * Initializes a DBRecord for this RowSet and sets primary key values.
     * The record may then be modified and updated.<BR>
     * <P>
     * @param record the Record object
     * @param key an array of the values for the primary key
     * @param fieldInitMode indicates how to initializes the record fields
     * @param newRecord true if the record is new or false if it is an existing record
     */
    protected void initRecord(DBRecordBase record, Object[] key, FieldInitMode fieldInitMode, boolean newRecord)
    {
        // check param
        checkParamRecord(record, false);
        // Prepare
        prepareInitRecord(record, newRecord);
        /* 
         * DO NOT fill with ObjectUtils.NO_VALUE!
         */    
        // Init Key Values
        if (key != null)
        {   // Check Columns
            DBColumn[] keyColumns = getKeyColumns();
            if (keyColumns==null)
                throw new NoPrimaryKeyException(this);
            if (key.length!=keyColumns.length)
                throw new InvalidArgumentException("key", key);
            // Set key
            Object[] fields = record.getFields();
            for (int i = 0; i < keyColumns.length; i++)
            {   // ignore null (important!)
                if (key[i]==null)
                    continue; 
                // check key column
                DBColumn keyColumn = keyColumns[i]; 
                if (newRecord && keyColumn.isAutoGenerated())
                    throw new FieldReadOnlyException(keyColumn);
                // Ignore Validity Checks
                int field = getColumnIndex(keyColumn);
                fields[field] = key[i];
            }
        }
        // Set defaults (don't provide connection here)
        if (fieldInitMode!=FieldInitMode.NONE)
        {
            initRecordDefaultValues(record, fieldInitMode);
        }
        // Init
        completeInitRecord(record);
    }
    
    /**
     * Initializes a DBRecord for this rowset using the record data provided (i.e. from a DBReader)<BR>
     * The record may then be modified and updated.<BR>
     * At least all primary key columns must be supplied.<BR>
     * We strongly recommend to supply the value of the update timestamp column in order to detect concurrent changes.<BR>
     * <P>
     * @param record the record object
     * @param recData the record data from which to initialized the record
     */
    public void initRecord(DBRecordBase record, DBRecordData recData)
    {
        // check param
        checkParamRecord(record, false);
        // Initialize the record
        prepareInitRecord(record, false);
        // Get Record Field Values
        Object[] fields = record.getFields();
        DBColumn[] keyColumns = getKeyColumns();
        for (int i = 0; i < fields.length; i++)
        {   // Read a value
        	DBColumnExpr column = getColumnExprAt(i);
        	int rdi = recData.getFieldIndex(column);
        	if (rdi<0)
        	{	// Field not available in Record Data
        		if (keyColumns!=null && ObjectUtils.contains(keyColumns, column))
        		{	// Error: Primary Key not supplied
        		    throw new ItemNotFoundException(column.getName());
        		}
                if (timestampColumn == column)
                { // Check the update Time Stamp
                	if (log.isInfoEnabled())
                		log.info(getName() + "No record timestamp value has been provided. Hence concurrent changes will not be detected.");
                } 
        		// Set to NO_VALUE
                fields[i] = ObjectUtils.NO_VALUE;
        	}
        	else
        	{   // Copy field value (do not set to modified!)
        	    fields[i] = recData.getValue(rdi);
        	}
        }
        // Done
        completeInitRecord(record);
    }
    
    /**
     * initializes the Record Default Values
     * @param record the record
     * @param fieldInitMode the field initialization mode
     */
    protected void initRecordDefaultValues(DBRecordBase record, FieldInitMode fieldInitMode)
    {
        // check field init mode
        if (fieldInitMode==FieldInitMode.NONE)
            throw new InvalidArgumentException("fieldInitMode", fieldInitMode);
        /**
         * Overridden in DBTable
         * 
         * Set to NO_VALUE for Views and Queries
         */
        Object[] fields = record.getFields();
        // Set Default values
        for (int i = 0; i < fields.length; i++)
        {   // already set ?
            if (fields[i]!=null)
                continue; 
            // Set to NO_VALUE
            fields[i] = ObjectUtils.NO_VALUE;
        }
    }
    
    /**
     * Initialize this DBRowSet object and sets it's initial state.
     * 
     * @param record the DBRecord object to initialize this DBRowSet object
     * @param newRecord flag whether the record is new (non-existing) in the database
     */
    protected void prepareInitRecord(DBRecordBase record, boolean newRecord)
    {
        if (record==null || record.getRowSet()!=this)
            throw new InvalidArgumentException("rec", record);
        if (columns.size() < 1)
            throw new ObjectNotValidException(this);
        // Init
        record.initData(newRecord);
    }
    
    /**
     * Completes the record initialization.<BR>
     * Override this function to do post initialization processing.
     * 
     * @param record the DBRecord object to initialize
     */
    protected void completeInitRecord(DBRecordBase record)
    {
    	record.onRecordChanged();
    }
    
    /**
     * Set the constraints for a single record from a supplied key 
     * @param key the record key
     * @return the primary key constraint expression
     */
    protected DBCompareExpr getKeyConstraints(Object[] key)
    {
        // Check Primary key
        DBColumn[] keyColumns = getKeyColumns();
        if (keyColumns==null || keyColumns.length==0) 
            throw new NoPrimaryKeyException(this); // Invalid Argument
        // Check Columns
        if (key == null || key.length != keyColumns.length)
            throw new InvalidKeyException(this, key); // Invalid Argument
        // Add the key constraints
        DBCompareExpr compareExpr = keyColumns[0].is(key[0]);
        for (int i = 1; i < key.length; i++)
        {   // prepare key value
            DBColumn column = keyColumns[i];
            Object value = key[i];
            // set key column constraint
            compareExpr = compareExpr.and(column.is(value));
        }
        return compareExpr;
    }
    
    /**
     * Reads a single record from the database using the given command object.<BR>
     * If a record is found the DBRecord object will hold all record data. 
     * 
     * @param record the DBRecord object which holds the record data
     * @param cmd the SQL-Command used to query the record
     */
    protected void readRecord(DBRecordBase record, DBCommand cmd)
    {
        // check param
        checkParamRecord(record, false);
        // read now
        DBReader reader = null;
        try
        {   // read record using a DBReader
            reader = new DBReader(record.getContext(), false);
            reader.getRecordData(cmd);
            initRecord(record, reader);
        } catch (QueryNoResultException e) {
            // extract key from command 
            Object key[] = cmd.getParamValues();
            if (key==null)
            {   // extract key from where clause
                List<DBCompareExpr> where = cmd.getWhereConstraints();
                key = new Object[where.size()];
                int i=0;
                for (DBCompareExpr expr : where)
                {   // get value of compare expr
                    if (expr instanceof DBCompareColExpr)
                        key[i] = ((DBCompareColExpr)expr).getValue();
                    else
                        key[i] = "?"; // unknown
                    i++;
                }
            }
            log.warn("Record [{}] not found in {}", StringUtils.toString(key), getName());
            // throw RecordNotFoundException
            throw new RecordNotFoundException(this, key);
        } finally {
            reader.close();
        }
    }
    
    /**
     * Reads the record with the given primary key from the database.
     * If the record cannot be found, a RecordNotFoundException is thrown.
     * 
     * @param record the DBRecord object which will hold the record data
     * @param key the primary key values
    public <R extends DBRecordBase> void readRecord(R record, Object[] key)
    {
        // Check Arguments
        checkParamNull("key", key);
        // Select
        DBCommand cmd = createRecordCommand(record.getContext());
        cmd.select(columns);
        // Set key constraints
        setKeyConstraints(cmd, key);
        // Read Record
        readRecord(record, cmd);
    }
     */
   
    /**
     * Reads a record from the database
     * @param record the DBRecord object which will hold the record data
     * @param whereConstraints the constraint for querying the record
     */
    public void readRecord(DBRecordBase record, DBCompareExpr whereConstraints)
    {
        // Check Arguments
        checkParamNull("whereConstraints", whereConstraints);
        // check constraints
        Set<DBColumn> columns = new HashSet<DBColumn>();
        whereConstraints.addReferencedColumns(columns);
        for (DBColumn c : columns)
            if (c.getRowSet().equals(this)==false)
                throw new InvalidArgumentException("whereConstraints", c.getFullName());
        // read now
        DBCommand cmd = createRecordCommand(record.getContext());
        cmd.select(getColumns());
        cmd.where(whereConstraints);
        readRecord(record, cmd);
    }
    
    /**
     * Reads the partial record for a given primary key from the database
     * @param record the DBRecord object which will hold the record data
     * @param whereConstraints a compare expression for querying the record
     * @param mode flag whether to include only the given columns or whether to add all but the given columns
     * @param columns the columns to include or exclude (depending on mode)
     */
    public void readRecord(DBRecordBase record, DBCompareExpr whereConstraints, PartialMode mode, DBColumn... columns)
    {
        // Check Arguments
        checkParamNull("whereConstraints", whereConstraints);
        // create command
        DBCommand cmd = createRecordCommand(record.getContext());
        for (DBColumn column : this.columns)
        {   // key column?
            if (isKeyColumn(column))
            {   // always select key column
                cmd.select(column);
                continue;
            }
            // find in column list 
            for (int i=0; i<columns.length; i++)
            {   // compare column
                if (column.equals(columns[i]))
                {   // found: add for INCLUDE
                    if (mode==PartialMode.INCLUDE)
                        cmd.select(column);
                }
                else if (mode==PartialMode.EXCLUDE)
                {   // not found: add for EXCLUDE
                    cmd.select(column);
                }
            }
        }
        // Set key constraints
        cmd.where(whereConstraints);
        // Read Record
        readRecord(record, cmd);
    }

    /**
     * Returns true if the record exists in the database or false otherwise.
     * <P>
     * @param key an array of the primary key columns
     * @param context the DBContext
     * @return true if the record exists or false otherwise
     */
    public boolean recordExists(Object[] key, DBContext context)
    {
        // Check Arguments
        checkParamNull("key", key);
        checkParamNull("context", context);
        // Select
        DBCommand cmd = createRecordCommand(context);
        cmd.select(count());
        // Set key constraints
        cmd.where(getKeyConstraints(key));
        // check exits
        return (context.getUtils().querySingleInt(cmd, 0)==1);
    }

    /**
     * Returns true if the record exists in the database or false otherwise.
     * 
     * @param id id of the record
     * @param context the DBContext
     * @return true if the record exists or false otherwise
     */
    public final boolean recordExists(Object id, DBContext context)
    {
        return recordExists(Record.key(id), context); 
    }
    
    /**
     * Updates or Inserts a record in the database.<BR>
     * Whether an update or insert is performed depends on the record state.<BR>
     * Only modified fields will be inserted or updated in the database.<BR>
     * <P>
     * If a timestamp-column is set for this RowSet then a constraint will be added in the 
     * update statement in order to detect concurrent changes.<BR> 
     * If the record has been modified by another user, an error of type 
     * DBErrors.RecordUpdateFailed will be set.  
     * <P>
     * @param record the DBRecord object. contains all fields and the field properties
     */
    public <R extends DBRecordBase> void updateRecord(R record)
    {
        // check updateable
        if (isUpdateable()==false)
            throw new NotSupportedException(this, "updateRecord");
        // Check Arguments
        checkParamRecord(record, true);
        // the connection
        DBContext context = record.getContext();
        Connection conn = context.getConnection();
        // Get the new Timestamp
        String name = getName();
        Timestamp timestamp = (timestampColumn!=null) ? context.getDbms().getUpdateTimestamp(conn) : null;
        DBMSHandler.DBSetGenKeys setGenKey = null;
        // Get the fields and the flags
        Object[] fields = record.getFields();
        // Build SQL-Statement
        DBCommand cmd = createRecordCommand(context);
        String sql = null;
        int setCount = 0;
        // Perform action
        DBColumn[] keyColumns = getKeyColumns();
        DBRecordBase.State recordState = record.getState(); 
        if (recordState==DBRecordBase.State.New)
        {	// Insert Record
            for (int i = 0; i < columns.size(); i++)
            {   // search for the column
            	Object value = fields[i];
                DBTableColumn col = (DBTableColumn) columns.get(i);
                if (timestampColumn == col)
                {   // Make sure the update timestamp column is set
                    if (timestamp!=null)
                        cmd.set(col.to(timestamp));
                    continue;
                } 
                boolean empty = (value==ObjectUtils.NO_VALUE || ObjectUtils.isEmpty(value)); 
                if (empty && col.isAutoGenerated()) 
                {   // Check for AutoInc data type
                    if (col.getDataType()==DataType.AUTOINC && 
                        db.getDbms().isSupported(DBMSFeature.SEQUENCES)==false)
                    {  // Obtain value via JDBC Statement.RETURN_GENERATED_KEYS
                       setGenKey = new DBSetRecordKey(fields, i);
                       continue;
                    }
                    // get the auto-generated field value
                    fields[i] = value = col.getRecordDefaultValue(conn);
                    empty = ObjectUtils.isEmpty(value);
                }
                // Add the value to the command
                if (empty==false)
                {   // *** unnecessary check removed 2.5.0 ***
                    // if (col.isAutoGenerated()==false && rec.isValidateFieldValues())
                    //     col.validate(value);
                    // Insert a field
                    cmd.set(col.to(value));
                    setCount++;
                }
                else if (ObjectUtils.contains(keyColumns, col))
                {   // All primary key fields must be supplied
                    throw new FieldNotNullException(col);
                }
                else if (col.isRequired())
                {   // Error Column is required!
                    throw new FieldNotNullException(col);
                }
            }
            sql = cmd.getInsert();
        }
        else if (recordState==DBRecordBase.State.Modified)
        {	// Update Record
            if (keyColumns == null)
            { // Requires a primary key
                log.error("updateRecord: "  + name + " no primary key defined!");
                throw new NoPrimaryKeyException(this);
            }
            for (int i = 0; i < columns.size(); i++)
            { // search for the column
            	Object value = fields[i];
            	// check for NO_VALUE
            	if (value==ObjectUtils.NO_VALUE)
            	{   // Timestamp?
                    if (timestampColumn == columns.get(i))
                        log.info("Record has no value for timestamp column. Concurrent changes will not be detected.");
                    // next
                    continue;
            	}
            	boolean modified = record.wasModified(i);
            	boolean empty = ObjectUtils.isEmpty(value); 
                DBTableColumn col = (DBTableColumn) columns.get(i);
                if (ObjectUtils.contains(keyColumns, col))
                { 	// Check for Modification
                    if (modified == true)
                    { // Requires a primary key
                        log.warn("updateRecord: " + name + " primary has been modified!");
                    }
                    // set pk constraint
                    cmd.where(col.is(value));
                } 
                else if (timestampColumn == col)
                {   // Check the update-timestamp
                	if (empty==false) 
                	{   // set timestamp constraint
                        cmd.where(col.is(value));
                	}    
                	// set new timestamp
                	if (timestamp!=null)
                        cmd.set(col.to(timestamp)); 
                } 
                else if (modified)
                { 	// Update a field
                    if (col.isReadOnly())
                        log.warn("updateRecord: Read-only column '" + col.getName() + " has been modified!");
                    // *** unnecessary check removed 2.5.0 ***
                    // col.validate(value);  
                    // Set the column
                    cmd.set(col.to(value));
                    setCount++;
                }
            }
            // Get the SQL statement
            sql = cmd.getUpdate();
        }
        else
        {	// Not modified
            log.info("updateRecord: " + name + " record has not been modified! ");
            return;
        }
        if (setCount == 0)
        {   // Nothing to update
            log.info("updateRecord: " + name + " nothing to update or insert!");
            return;
        }
        // Perform action
        DBUtils utils = context.getUtils();
        int affected = utils.executeSQL(sql, cmd.getParamValues(), setGenKey);
        if (affected < 0)
        {   // Update Failed
            throw new UnexpectedReturnValueException(affected, "db.executeSQL()");
        } 
        else if (affected == 0)
        { // Record not found
            throw new RecordUpdateFailedException(this, record.getKey());
        } 
        else if (affected > 1)
        { // Multiple Records affected
            throw new RecordUpdateAmbiguousException(this, record.getKey());
        }
        // Correct Timestamp
        if (timestampColumn!=null && timestamp!=null)
        {   // Set the correct Timestamp
            int i = record.getFieldIndex(timestampColumn);
            if (i >= 0)
                fields[i] = timestamp;
        }
        // Change State
        record.updateComplete();        
    }
    
    /**
     * Deletes a single record from the database.<BR>
     * <P>
     * @param id the record's primary key
     * @param context the DBContext
     */
    public final void deleteRecord(long id, DBContext context)
    {
        deleteRecord(new Object[] { id }, context);
    }

    /**
     * Deletes all records which reference this table.
     * <P>
     * @param key the key the record to be deleted
     * @param context the DBContext
     */
    protected final void deleteAllReferences(Object[] key, DBContext context)
    {
        // Merge Sub-Records
        List<DBRelation> relations = db.getRelations();
        DBColumn[] keyColumns = getKeyColumns();
        if (keyColumns==null)
            return; // No primary key - no references!
        // Find all relations
        for (DBRelation rel : relations)
        {   // Check cascade
            if (rel.getOnDeleteAction()!=DBCascadeAction.CASCADE_RECORDS)
                continue;
            // References
            DBReference[] refs = rel.getReferences();
            for (int i=0; i<refs.length; i++)
            {
                if (refs[i].getTargetColumn().equals(keyColumns[0]))
                {   // Found a reference on RowSet
                    DBRowSet rs = refs[0].getSourceColumn().getRowSet(); 
                    rs.deleteReferenceRecords(refs, key, context);
                }
            }
        }
    }
    
    /**
     * Deletes all records which are referenced by a particular relation.
     * <P>
     * @param refs the reference columns belonging to the relation
     * @param parentKey the key of the parent element
     * @param context the DBContext
     */
    protected void deleteReferenceRecords(DBReference[] refs, Object[] parentKey, DBContext context)
    {
        // Key length and reference length must match
        if (refs.length!=parentKey.length)
            throw new InvalidArgumentException("refs", refs);
        // Rowset
        DBColumn[] keyColumns = getKeyColumns();
        if (keyColumns==null || keyColumns.length==0)
        {   // No Primary Key
            DBCommand cmd = createRecordCommand(context);
            for (int i=0; i<parentKey.length; i++)
                cmd.where(refs[i].getSourceColumn().is(parentKey[i]));
            if (context.executeDelete((DBTable)this, cmd)<0)
                throw new UnexpectedReturnValueException(-1, "db.executeSQL()");
        }
        else
        {   // Query all key
            DBCommand cmd = createRecordCommand(context);
            cmd.select(keyColumns);
            // Set constraints
            for (int i=0; i<parentKey.length; i++)
            {
                cmd.where(refs[i].getSourceColumn().is(parentKey[i]));
            }
            // Set order (descending)
            for (int i=0; i<keyColumns.length; i++)
            {
                cmd.orderBy(keyColumns[i], true);
            }
            // Query all key
            List<Object[]> recKeys = context.getUtils().queryObjectList(cmd);
            for (Object[] recKey : recKeys)
            {   
                log.info("Deleting Record " + StringUtils.valueOf(recKey) + " from table " + getName());
                deleteRecord(recKey, context);
            }
        }
        // Done
    }
    
    /**
     *  Mabe use Prepared statements even if disabled in context 
     */
    protected DBCommand createRecordCommand(DBContext context)
    {
        // Special behaviour for DBRecord operations
        if ((context instanceof DBContextBase) && !((DBContextBase)context).isPreparedStatementsEnabled())
        {   // Check PreparedStatementsEnabled in DBDatabase
            if (getDatabase().isPreparedStatementsEnabled())
            {   // Use PreparedStatement even though disabled in context
                return context.getDbms().createCommand(true);
            }
        }
        // just use the context
        return context.createCommand();
    }

    /**
     * Returns additional data stored on a record by the RowSet
     * @param record the record 
     * @return the rowset data
     */
    protected final Object getRowsetData(DBRecordBase record)
    {
        return record.rowsetData;
    }
    
    /**
     * May be used by a Rowset to store additional data on a record
     * @param record the record 
     * @param rowsetData the extra data to set on this record
     */
    protected final void setRowsetData(DBRecordBase record, Object rowsetData)
    {
        record.rowsetData = rowsetData;
    }
    
}

