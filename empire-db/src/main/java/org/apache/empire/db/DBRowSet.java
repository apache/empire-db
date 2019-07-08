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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.commons.StringUtils;
import org.apache.empire.data.Column;
import org.apache.empire.data.DataType;
import org.apache.empire.db.DBRelation.DBCascadeAction;
import org.apache.empire.db.DBRelation.DBReference;
import org.apache.empire.db.exceptions.FieldNotNullException;
import org.apache.empire.db.exceptions.InvalidKeyException;
import org.apache.empire.db.exceptions.NoPrimaryKeyException;
import org.apache.empire.db.exceptions.QueryNoResultException;
import org.apache.empire.db.exceptions.RecordNotFoundException;
import org.apache.empire.db.exceptions.RecordUpdateFailedException;
import org.apache.empire.db.exceptions.RecordUpdateInvalidException;
import org.apache.empire.db.expr.column.DBCountExpr;
import org.apache.empire.exceptions.InvalidArgumentException;
import org.apache.empire.exceptions.ItemNotFoundException;
import org.apache.empire.exceptions.NotSupportedException;
import org.apache.empire.exceptions.ObjectNotValidException;
import org.apache.empire.exceptions.UnexpectedReturnValueException;
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
public abstract class DBRowSet extends DBExpr
{
    private final static long serialVersionUID = 1L;

    /**
     * This class is used to set the auto generated key of a record if the database does not support sequences.
     * It is used with the executeSQL function and only required for insert statements
     */
    private static class DBSetGenKey implements DBDatabaseDriver.DBSetGenKeys
    {
        private Object[] fields;
        private int index; 
        public DBSetGenKey(Object[] fields, int index)
        {
            this.fields = fields;
            this.index = index;
        }
        @Override
        public void set(Object value)
        {
            fields[index]=value;
        }
    }
    
    // Logger
    protected static final Logger log = LoggerFactory.getLogger(DBRowSet.class);
    // Members
    protected final transient DBDatabase db;
    protected String        comment           = null;
    protected DBIndex       primaryKey        = null;
    protected DBColumn      timestampColumn   = null; // Use SetUpdateTimestamp!
    protected Map<DBColumn, DBColumn> columnReferences = null;
    // The column List
    protected List<DBColumn> columns          = new ArrayList<DBColumn>();

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
    public String getId()
    {
        return db.getId()+"."+getName();
    }

    /**
     * returns a rowset by its identifier
     * @param rowsetId the id of the rowset
     * @return the rowset object
     */
    public static DBRowSet findById(String rowsetId)
    {
        int i = rowsetId.lastIndexOf('.');
        if (i<0)
            throw new InvalidArgumentException("rowsetId", rowsetId);
        // database suchen
        String dbid = rowsetId.substring(0, i);
        DBDatabase db = DBDatabase.findById(dbid);
        if (db==null)
            throw new ItemNotFoundException(dbid);
        // rowset suchen
        String rsname = rowsetId.substring(i+1);
        DBRowSet rset = db.getRowSet(rsname);
        if (rset==null)
            throw new ItemNotFoundException(rowsetId);
        return rset;
    }
    
    /**
    * Custom serialization for transient database.
    */
    private void writeObject(ObjectOutputStream strm) throws IOException 
    {
        if (db==null)
        {   // No database
            strm.writeObject("");
            strm.defaultWriteObject();
            return;
        }
        String dbid = db.getId(); 
        strm.writeObject(dbid);
        if (log.isDebugEnabled())
            log.debug("Serialization: writing DBRowSet "+dbid);
        // write the rest
        strm.defaultWriteObject();
    }

    /**
    * Custom deserialization for transient database.
    */
    private void readObject(ObjectInputStream strm) throws IOException, ClassNotFoundException,
        SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException
    {
        String dbid = String.valueOf(strm.readObject());
        if (StringUtils.isNotEmpty(dbid))
        {   // Find database
            if (log.isDebugEnabled())
                log.debug("Serialization: reading DBRowSet "+dbid);
            // find database
            DBDatabase sdb = DBDatabase.findById(dbid);
            if (sdb==null)
                throw new ClassNotFoundException(dbid);
            // set final field
            Field f = DBRowSet.class.getDeclaredField("db");
            f.setAccessible(true);
            f.set(this, sdb);
            f.setAccessible(false);
        }    
        // read the rest
        strm.defaultReadObject();
    }
    
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

    // ------- Abstract Methods -------
    
    public abstract String getName();
    
    public abstract String getAlias();
    
    public abstract boolean isUpdateable();

    public abstract void createRecord(DBRecord rec, Connection conn);

    public abstract void deleteRecord(Object[] keys, Connection conn);
    
    /**
     * Returns the full qualified name of the rowset.
     * <P>
     * @return the full qualified name
     */
    public String getFullName()
    {
        String  name   = getName();
        String  schema = db.getSchema();
        return (schema!=null) ? schema+"."+name : name;
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
    @Override
    public final DBDatabase getDatabase()
    {
        return db;
    }

    /**
     * Gets all columns of this rowset (e.g. for cmd.select()).
     * <P>
     * @return all columns of this rowset
     */
    public List<DBColumn> getColumns()
    {
        return Collections.unmodifiableList(columns);
    }

    /**
     * Gets the index of a particular column expression.
     * <P>
     * @param column column the DBColumn to get the index for
     * 
     * @return the position of a column expression
     */
    public int getColumnIndex(DBColumn column)
    {
        return columns.indexOf(column);
    }
    
    /**
     * Gets the index of a particular column expression.
     * <P>
     * @param column the Column to get the index for
     * 
     * @return the position of a column expression
     */
    public final int getColumnIndex(Column column)
    {
        return getColumnIndex((DBColumn)column);
    }

    /**
     * Returns a DBColumn object by a specified index value.
     * 
     * @param iColumn the index to get the DBColumn for
     * 
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
     * 
     * @param name the name of the column to look for 
     * 
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
     * 
     * @param col the column object 
     * 
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
     * Returns an array of all primary key columns.
     * 
     * @return an array of all primary key columns
     */
    public DBColumn[] getKeyColumns()
    {
        return ((primaryKey != null) ? primaryKey.getColumns() : null);
    }
    
    /**
     * Checks whether a given column is part of the primary key for this RowSet 
     * @param column the column to check
     * @return true if the column is part of the primary key or false otherwise
     */
    public boolean isKeyColumn(DBColumn column)
    {
        DBColumn[] keyColumns = getKeyColumns();
        for (int i=0; i<keyColumns.length; i++)
        {
            if (keyColumns[i]==column)
                return true;
        }
        return false;
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
        if (timestampColumn.getRowSet()!=this)
            throw new InvalidArgumentException("timestampColumn", timestampColumn);
        if (this.timestampColumn!=null && this.timestampColumn!=timestampColumn)
            log.warn("Timestamp column has already been set for rowset {}. Replacing with {}", getName(), timestampColumn.getName());
        if (timestampColumn instanceof DBTableColumn)
            ((DBTableColumn) timestampColumn).setReadOnly(true);
        // set now
        this.timestampColumn = timestampColumn;
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
        if (db==null || db.driver==null)
            return " ";
        return db.driver.getSQLPhrase(DBDatabaseDriver.SQL_RENAME_TABLE);
    }

    /**
     * Returns a array of primary key columns by a specified DBRecord object.
     * 
     * @param rec the DBRecord object, contains all fields and the field properties
     * @return a array of primary key columns
     */
    public Object[] getRecordKey(DBRecord rec)
    {
        if (rec.getRowSet() != this)
            return null; // Invalid Argument
        if (primaryKey == null)
            return null; // No primary key
        // Check Columns
        DBColumn[] keyColumns = primaryKey.getColumns();
        Object[] keys = new Object[keyColumns.length];
        for (int i = 0; i < keyColumns.length; i++)
        {
            keys[i] = rec.getValue(keyColumns[i]);
            if (keys[i] == null)
            { // Primary Key not set
                log.warn("getRecordKey: " + getName() + " primary key value is null!");
            }
        }
        return keys;
    }

    /**
     * Initialize this DBRowSet object and sets it's initial state.
     * 
     * @param rec the DBRecord object to initialize this DBRowSet object
     * @param rowSetData any further RowSet specific data
     * @param insert
     */
    protected void prepareInitRecord(DBRecord rec, Object rowSetData, boolean insert)
    {
        if (rec==null)
            throw new InvalidArgumentException("rec", rec);
        if (columns.size() < 1)
            throw new ObjectNotValidException(this);
        // Init
        rec.initData(this, rowSetData, insert);
    }

    /**
     * Initializes a DBRecord for this RowSet and sets primary key values (the Object[] keyValues).
     * The record may then be modified and updated.<BR>
     * <P>
     * @param rec the Record object
     * @param keyValues an array of the primary key columns
     */
    public void initRecord(DBRecord rec, Object[] keyValues, boolean insert)
    {
        // Prepare
        prepareInitRecord(rec, null, insert);
        // Initialize all Fields
        Object[] fields = rec.getFields();
        for (int i = 0; i < fields.length; i++)
            fields[i] = ObjectUtils.NO_VALUE;
        // Init Key Values
        if (keyValues != null && primaryKey != null)
        {
            // Check Columns
            DBColumn[] keyColumns = primaryKey.getColumns();
            for (int i = 0; i < keyColumns.length; i++)
            { // Ignore Validity Checks
                int field = getColumnIndex(keyColumns[i]);
                fields[field] = keyValues[i];
            }
        }
        // Init
        completeInitRecord(rec);
    }

    /**
     * Initializes a DBRecord for this rowset using the record data provided (i.e. from a DBReader)<BR>
     * The record may then be modified and updated.<BR>
     * At least all primary key columns must be supplied.<BR>
     * We strongly recommend to supply the value of the update timestamp column in order to detect concurrent changes.<BR>
     * Fields for which no value is supplied with the recData paramter are set to NO_VALUE<BR>
     * <P>
     * @param rec the record object
     * @param recData the record data from which to initialized the record
     */
    public void initRecord(DBRecord rec, DBRecordData recData)
    {
        // Initialize the record
        prepareInitRecord(rec, null, false);
        // Get Record Field Values
        Object[] fields = rec.getFields();
        for (int i = 0; i < fields.length; i++)
        {
            // Read a value
        	DBColumn column = columns.get(i);
        	int rdi = recData.getFieldIndex(column);
        	if (rdi<0)
        	{	// Field not available in Record Data
        		if (primaryKey!=null && primaryKey.contains(column))
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
        	{   // Get Field value
                fields[i] = recData.getValue(rdi);
        	}
        }
        // Done
        completeInitRecord(rec);
    }
    
    /**
     * Completes the record initialization.<BR>
     * Override this function to do post initialization processing.
     * <P>
     * @param rec the DBRecord object to initialize
     */
    protected void completeInitRecord(DBRecord rec)
    {
    	rec.onRecordChanged();
    }
    
    /**
     * Set the constraints for a single record from a supplied key 
     * @param cmd the command to which to add the constraints
     * @param key the record key
     */
    protected void setKeyConstraints(DBCommand cmd, Object[] key)
    {
        // Check Primary key
        if (primaryKey == null ) 
            throw new NoPrimaryKeyException(this); // Invalid Argument
        // Check Columns
        DBColumn[] keyColumns = primaryKey.getColumns();
        if (key == null || key.length != keyColumns.length)
            throw new InvalidKeyException(this, key); // Invalid Argument
        // Add the key constraints
        for (int i = 0; i < key.length; i++)
        {   // prepare key value
            Object value = key[i];
            if (db.isPreparedStatementsEnabled())
                value = cmd.addParam(keyColumns[i], value);
            // set key column constraint
            cmd.where(keyColumns[i].is(value));
        }    
    }
    
    /**
     * Reads a single record from the database using the given command object.<BR>
     * If a record is found the DBRecord object will hold all record data. 
     * <P>
     * @param rec the DBRecord object which holds the record data
     * @param cmd the SQL-Command used to query the record
     * @param conn a valid JDBC connection.
     */
    protected void readRecord(DBRecord rec, DBCommand cmd, Connection conn)
    {
        DBReader reader = null;
        try
        {   // read record using a DBReader
            reader = new DBReader(false);
            reader.getRecordData(cmd, conn);
            initRecord(rec, reader);
            
        } finally {
            reader.close();
        }
    }
    
    /**
     * Reads the record with the given primary key from the database.
     * If the record cannot be found, a RecordNotFoundException is thrown.
     * <P>
     * @param rec the DBRecord object which will hold the record data
     * @param key the primary key values
     * @param conn a valid JDBC connection.
     */
    public void readRecord(DBRecord rec, Object[] key, Connection conn)
    {
        // Check Arguments
        if (conn == null || rec == null)
            throw new InvalidArgumentException("conn|rec", null);
        // Select
        DBCommand cmd = db.createCommand();
        cmd.select(columns);
        // Set key constraints
        setKeyConstraints(cmd, key);
        try {
            // Read Record
            readRecord(rec, cmd, conn);
        } catch (QueryNoResultException e) {
            // Translate exception
            throw new RecordNotFoundException(this, key);
        }
    }

    /**
     * Returns true if the record exists in the database or false otherwise.
     * <P>
     * @param key an array of the primary key columns
     * @param conn a valid JDBC connection.
     * @return true if the record exists or false otherwise
     */
    public boolean recordExists(Object[] key, Connection conn)
    {
        // Check Arguments
        if (conn == null)
            throw new InvalidArgumentException("conn", conn);
        // Select
        DBCommand cmd = db.createCommand();
        cmd.select(count());
        // Set key constraints
        setKeyConstraints(cmd, key);
        // check exits
        return (db.querySingleInt(cmd, 0, conn)==1);
    }

    /**
     * Returns true if the record exists in the database or false otherwise.
     * <P>
     * @param id id of the record
     * @param conn a valid JDBC connection.
     * @return true if the record exists or false otherwise
     */
    public final boolean recordExists(Object id, Connection conn)
    {
        return recordExists(new Object[] { id }, conn); 
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
     * @param rec the DBRecord object. contains all fields and the field properties
     * @param conn a valid JDBC connection.
     */
    public void updateRecord(DBRecord rec, Connection conn)
    {
        // check updateable
        if (isUpdateable()==false)
            throw new NotSupportedException(this, "updateRecord");
        // Check Arguments
        if (rec == null)
            throw new InvalidArgumentException("record", rec);
        if (rec.isValid()==false)
            throw new ObjectNotValidException(rec);
        if (conn == null)
            throw new InvalidArgumentException("conn", conn);
        // Get the new Timestamp
        String name = getName();
        Timestamp timestamp = (timestampColumn!=null) ? db.getUpdateTimestamp(conn) : null;
        DBDatabaseDriver.DBSetGenKeys setGenKey = null;
        // Get the fields and the flags
        Object[] fields = rec.getFields();
        // Build SQL-Statement
        DBCommand cmd = db.createCommand();
        String sql = null;
        int setCount = 0;
        // Perform action
        DBRecord.State recordState = rec.getState(); 
        if (recordState==DBRecord.State.New)
        {	// Insert Record
            for (int i = 0; i < columns.size(); i++)
            {   // search for the column
            	Object value = fields[i];
                DBTableColumn col = (DBTableColumn) columns.get(i);
                if (timestampColumn == col)
                {   // Make sure the update timestamp column is set
                    cmd.set(col.to(timestamp));
                    continue;
                } 
                boolean empty = ObjectUtils.isEmpty(value); 
                if (empty && col.isAutoGenerated()) 
                {   // Check for AutoInc data type
                    if (col.getDataType()==DataType.AUTOINC && 
                        db.getDriver().isSupported(DBDriverFeature.SEQUENCES)==false)
                    {  // Obtain value via JDBC Statement.RETURN_GENERATED_KEYS
                       setGenKey = new DBSetGenKey(fields, i);
                       continue;
                    }
                    // get the auto-generated field value
                    fields[i] = value = col.getRecordDefaultValue(conn);
                    empty = ObjectUtils.isEmpty(value);
                }
                // Add the value to the command
                if (empty==false)
                {   // Check the value
                    if (col.isAutoGenerated()==false && rec.isValidateFieldValues())
                        col.validate(value);
                    // Insert a field
                    cmd.set(col.to(value));
                    setCount++;
                }
                else if (primaryKey!=null && primaryKey.contains(col))
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
        else if (recordState==DBRecord.State.Modified)
        {	// Update Record
            if (primaryKey == null)
            { // Requires a primary key
                log.error("updateRecord: "  + name + " no primary key defined!");
                throw new NoPrimaryKeyException(this);
            }
            for (int i = 0; i < columns.size(); i++)
            { // search for the column
            	Object value = fields[i];
            	boolean modified = rec.wasModified(i);
            	boolean empty = ObjectUtils.isEmpty(value); 
                DBTableColumn col = (DBTableColumn) columns.get(i);
                if (primaryKey.contains(col))
                { 	// Check for Modification
                    if (modified == true)
                    { // Requires a primary key
                        log.warn("updateRecord: " + name + " primary has been modified!");
                    }
                    // set pk constraint
                    if (db.isPreparedStatementsEnabled())
                        value = cmd.addParam(col, value);
                    cmd.where(col.is(value));
                } 
                else if (timestampColumn == col)
                {   // Check the update-timestamp
                	if (empty==false) 
                	{   // set timestamp constraint
                        if (db.isPreparedStatementsEnabled())
                            value = cmd.addParam(col, value);
                        cmd.where(col.is(value));
                	}    
                	else if (log.isDebugEnabled()) {
                		log.debug("updateRecord has no value for timestamp column. Concurrent changes will not be detected.");
                	}	
                    cmd.set(col.to(timestamp));
                } 
                else if (modified && value!=ObjectUtils.NO_VALUE)
                { 	// Update a field
                    if (col.isReadOnly())
                        log.warn("updateRecord: Read-only column '" + col.getName() + " has been modified!");
                    // Check the value
                    col.validate(value);
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
        int affected = db.executeSQL(sql, cmd.getParamValues(), conn, setGenKey);
        if (affected < 0)
        {   // Update Failed
            throw new UnexpectedReturnValueException(affected, "db.executeSQL()");
        } 
        else if (affected == 0)
        { // Record not found
            throw new RecordUpdateInvalidException(this, getRecordKey(rec));
        } 
        else if (affected > 1)
        { // Multiple Records affected
            throw new RecordUpdateFailedException(this, getRecordKey(rec));
        }
        // Correct Timestamp
        if (timestampColumn != null)
        { // Set the correct Timestamp
            int i = rec.getFieldIndex(timestampColumn);
            if (i >= 0)
                fields[i] = timestamp;
        }
        // Change State
        rec.updateComplete(rec.getRowSetData());
    }
    
    /**
     * Deletes a single record from the database.<BR>
     * <P>
     * @param id the record's primary key
     * @param conn a valid JDBC connection
     */
    public final void deleteRecord(Object id, Connection conn)
    {
        deleteRecord(new Object[] { id }, conn);
    }

    /**
     * Deletes all records which reference this table.
     * <P>
     * @param key the key the record to be deleted
     * @param conn a valid connection
     */
    protected final void deleteAllReferences(Object[] key, Connection conn)
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
                    rs.deleteReferenceRecords(refs, key, conn);
                }
            }
        }
    }
    
    /**
     * Deletes all records which are referenced by a particular relation.
     * <P>
     * @param refs the reference columns belonging to the relation
     * @param parentKey the key of the parent element
     * @param conn a valid connection
     */
    protected void deleteReferenceRecords(DBReference[] refs, Object[] parentKey, Connection conn)
    {
        // Key length and reference length must match
        if (refs.length!=parentKey.length)
            throw new InvalidArgumentException("refs", refs);
        // Rowset
        DBColumn[] keyColumns = getKeyColumns();
        if (keyColumns==null || keyColumns.length==0)
        {   // No Primary Key
            DBCommand cmd = db.createCommand();
            for (int i=0; i<parentKey.length; i++)
                cmd.where(refs[i].getSourceColumn().is(parentKey[i]));
            if (db.executeSQL(cmd.getDelete((DBTable)this), cmd.getParamValues(), conn)<0)
                throw new UnexpectedReturnValueException(-1, "db.executeSQL()");
        }
        else
        {   // Query all keys
            DBCommand cmd = db.createCommand();
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
            // Query all keys
            List<Object[]> recKeys = db.queryObjectList(cmd, conn);
            for (Object[] recKey : recKeys)
            {   
                log.info("Deleting Record " + StringUtils.valueOf(recKey) + " from table " + getName());
                deleteRecord(recKey, conn);
            }
        }
        // Done
    }
    
}

