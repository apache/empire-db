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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.empire.commons.Errors;
import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.commons.StringUtils;
import org.apache.empire.data.Column;
import org.apache.empire.data.DataType;
import org.apache.empire.db.DBRelation.DBReference;
import org.apache.empire.db.expr.column.DBCountExpr;


/**
 * This class is the base class for all the DBTable,
 * CBView and DBQuery classes this class contains all the columns of the
 * tables, views or querys
 * <P>
 * 
 *
 */
public abstract class DBRowSet extends DBExpr
{
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
        public void set(Object value)
        {
            fields[index]=value;
        }
    }
    
    // Logger
    @SuppressWarnings("hiding")
    protected static final Log log = LogFactory.getLog(DBRowSet.class);
    // Members
    protected final DBDatabase db;
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

    // ------- Abstract Methods -------
    
    public abstract String getName();
    
    public abstract String getAlias();

    public abstract boolean createRecord(DBRecord rec, Connection conn);

    public abstract boolean deleteRecord(Object[] keys, Connection conn);
    
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
        return columns;
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
        if (col==timestampColumn)
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
     * Adds a column reference to the ist of table references.
     * This method ist internally called from DBDatabase.addReleation().
     * 
     * @param source a column reference for one of this table's column
     * @param target the target column to which the source column references
     */
    protected boolean addColumnReference(DBColumn source, DBColumn target)
    {
        if (source.getRowSet()!=this)
            return error(Errors.InvalidArg, source.getFullName(), "column");
        if (columnReferences== null)
            columnReferences = new HashMap<DBColumn, DBColumn>();
        // Check if column is already there
        columnReferences.put(source, target);
        return success();
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
     * Initialise this DBRowSet object and sets it's initial state.
     * 
     * @param rec the DBRecord object to initialise this DBRowSet object
     * @param state the state of this DBRowSet object
     * @return true if successful
     */
    protected boolean prepareInitRecord(DBRecord rec, int state, Object rowSetData)
    {
        if (columns.size() < 1)
            return error(Errors.ObjectNotValid, getClass().getName());
        // Init
        rec.init(this, state, rowSetData);
        return success();
    }

    /**
     * Initializes a DBRecord for this RowSet and sets primary key values (the Object[] keyValues).
     * The record may then be modified and updated.<BR>
     * <P>
     * @param rec the Record object
     * @param keyValues an array of the primary key columns
     * @return true if successful
     */
    public boolean initRecord(DBRecord rec, Object[] keyValues)
    {
        // Inititialisierung
        if (!prepareInitRecord(rec, DBRecord.REC_EMTPY, null))
            return false;
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
        return completeInitRecord(rec);
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
     * @return true if successful
     */
    public boolean initRecord(DBRecord rec, DBRecordData recData)
    {
        // Initialize the record
        prepareInitRecord(rec, DBRecord.REC_VALID, null);
        // Get Record Field Values
        Object[] fields = rec.getFields();
        for (int i = 0; i < fields.length; i++)
        {
            try
            {   // Read a value
            	DBColumn column = columns.get(i);
            	int rdi = recData.getFieldIndex(column);
            	if (rdi<0)
            	{	// Field not available in Record Data
            		if (primaryKey!=null && primaryKey.contains(column))
            		{	// Error: Primary Key not supplied
            			return error(DBErrors.RecordInvalidKey, column.toString());
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
                    // Check for error
                    if (fields[i]==null && recData.hasError())
                        return error(recData);
            	}
            } catch (Exception e)
            {   // Unknown exception
                log.error("initRecord exception: " + e.toString());
                rec.close();
                return error(e);
            }
        }
        // Done
        return completeInitRecord(rec);
    }
    
    /**
     * Reads a single record from the database using the given command object.<BR>
     * If a reocord is found the DBRecord object will hold all record data. 
     * <P>
     * @param rec the DBRecord object which holds the record data
     * @param cmd the SQL-Command used to query the record
     * @param conn a valid JDBC connection.
     * @return true if successful
     */
    protected boolean readRecord(DBRecord rec, DBCommand cmd, Connection conn)
    {
        DBReader reader = null;
        try
        {
            clearError();
            reader = new DBReader();
            if (reader.getRecordData(cmd, conn)==false)
                return error(reader);
            if (initRecord(rec, reader)==false)
            	return false;
            // Done
            return success();
            
        } finally
        {
        	reader.close();
        }
    }
    
    /**
     * Completes the record initialisation.<BR>
     * Override this function to do post initialisation processing.
     * <P>
     * @param rec the DBRecord object to initialise
     * @return true if successful
     */
    protected boolean completeInitRecord(DBRecord rec)
    {
    	rec.onRecordChanged();
        return success();
    }
    
    /**
     * Reads the record with the given primary key from the database.
     * <P>
     * @param rec the DBRecord object which will hold the record data
     * @param key the primary key values
     * @param conn a valid JDBC connection.
     * @return true if successful
     */
    public boolean readRecord(DBRecord rec, Object[] key, Connection conn)
    {
        // Check Arguments
        if (conn == null || rec == null)
            return error(Errors.InvalidArg, null, "conn|rec");
        // Check Primary key
        if (primaryKey == null ) 
            return error(DBErrors.NoPrimaryKey, getName()); // Invalid Argument
        // Check Columns
        DBColumn[] keyColumns = primaryKey.getColumns();
        if (key == null || key.length != keyColumns.length)
            return error(DBErrors.RecordInvalidKey, key); // Invalid Argument
        // Select
        DBCommand cmd = db.createCommand();
        cmd.select(columns);
        for (int i = 0; i < key.length; i++)
            cmd.where(keyColumns[i].is(key[i]));
        // Read Record
        if (!readRecord(rec, cmd, conn))
        {   // Record not found
            if (getErrorType()==DBErrors.QueryNoResult)
                return error(DBErrors.RecordNotFound, key);
            // Return given error
            return false;
        }
        // Done
        return success();
    }

    /**
     * Returns true if the record exists in the database or false otherwise.
     * <P>
     * @param key an array of the primary key columns
     * @param conn a valid JDBC connection.
     * @return true if successful or false otherwise
     */
    public boolean recordExists(Object[] key, Connection conn)
    {
        // Check Arguments
        if (conn == null)
            return error(Errors.InvalidArg, conn, "conn");
        // Check Primary key
        if (primaryKey == null ) 
            return error(DBErrors.NoPrimaryKey, getName()); // Invalid Argument
        // Check Columns
        DBColumn[] keyColumns = primaryKey.getColumns();
        if (key == null || key.length != keyColumns.length)
            return error(DBErrors.RecordInvalidKey, key); // Invalid Argument
        // Select
        DBCommand cmd = db.createCommand();
        cmd.select(count());
        for (int i = 0; i < key.length; i++)
            cmd.where(keyColumns[i].is(key[i]));
        // check exits
        return (db.querySingleInt(cmd.getSelect(), conn)==1);
    }

    /**
     * Returns true if the record exists in the database or false otherwise.
     * <P>
     * @param id id of the record
     * @param conn a valid JDBC connection.
     * @return true if successful or false otherwise
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
     * @return true if the update was sucessful or false otherwise
     */
    public boolean updateRecord(DBRecord rec, Connection conn)
    {
        // Check Arguments
        if (conn == null)
            return error(Errors.InvalidArg, conn, "conn");
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
        switch (rec.getState())
        {
            case DBRecord.REC_MODIFIED:
                if (primaryKey == null)
                { // Requires a primary key
                    log.error("updateRecord: "  + name + " no primary key defined!");
                    return error(DBErrors.NoPrimaryKey, name);
                }
                for (int i = 0; i < columns.size(); i++)
                { // search for the column
                	Object value = fields[i];
                	boolean modified = rec.wasModified(i);
                	boolean empty = ObjectUtils.isEmpty(value); 
                    DBTableColumn col = (DBTableColumn) columns.get(i);
                    if (primaryKey.contains(col))
                    { // Check for Modification
                        if (modified == true)
                        { // Requires a primary key
                            log.warn("updateRecord: " + name + " primary has been modified!");
                        }
                        cmd.where(col.is(value));
                    } 
                    else if (timestampColumn == col)
                    { // Check the update Time Stamp
                    	if (empty==false)
	                        cmd.where(col.is(value));
                    	else if (log.isDebugEnabled())
                    		log.debug("updateRecord has no value for timestamp column. Concurrent changes will not be detected.");
                        cmd.set(col.to(timestamp));
                    } 
                    else if (modified && value!=ObjectUtils.NO_VALUE)
                    { // Update a field
                        if (col.isReadOnly())
                            log.warn("updateRecord: Read-only column '" + col.getName() + " has been modified!");
                        // Check the value
                        if (!col.checkValue(value))
                            return error(col);
                        // Set the column
                        cmd.set(col.to(value));
                        setCount++;
                    }
                }
                // Get the SQL statement
                sql = cmd.getUpdate();
                break;

            case DBRecord.REC_NEW:
                for (int i = 0; i < columns.size(); i++)
                { // search for the column
                	Object value = fields[i];
                	boolean empty = ObjectUtils.isEmpty(value); 
                    DBTableColumn col = (DBTableColumn) columns.get(i);
                    if (col.getDataType()==DataType.AUTOINC && empty) 
                    { // Set Autoinc value if not already done
                        if (db.getDriver().isSupported(DBDriverFeature.SEQUENCES)==false)
                        {  // Post Dectect Autoinc Value
                           setGenKey = new DBSetGenKey(fields, i);
                           continue;
                        }
                        // Get Next Sequence value
                        fields[i] = value = col.getRecordDefaultValue(conn);
                        empty = ObjectUtils.isEmpty(value);
                    }
                    if (primaryKey!=null && primaryKey.contains(col) && empty)
                    { // All primary key fields must be supplied
                        return error(DBErrors.FieldNotNull, col.getTitle());
                    }
                    if (timestampColumn == col)
                    { // Make sure the upate Timestamp Column is set
                        cmd.set(col.to(timestamp));
                    } 
                    else if (empty==false)
                    { // Check the value
                        if (!col.checkValue(value))
                            return error(col);
                        // Insert a field
                        cmd.set(col.to(value));
                        setCount++;
                    } 
                    else if (col.required == true)
                    { // Error Column is required!
                        return error(DBErrors.FieldNotNull, col.getTitle());
                    }
                }
                sql = cmd.getInsert();
                break;

            default:
                log.warn("updateRecord: " + name + " record has not been modified! ");
                return success();
        }
        if (setCount == 0)
        { // Cannot update or insert fields
            log.info("updateRecord: " + name + " nothing to update or insert!");
            return success();
        }
        // Perform action
        int affected = db.executeSQL(sql, cmd.getCmdParams(), conn, setGenKey);
        if (affected < 0)
        { // Update Failed
            return error(db);
        } 
        else if (affected == 0)
        { // Record not found
            return error(DBErrors.RecordUpdateFailed, name);
        } 
        else if (affected > 1)
        { // Multiple Records affected
            return error(DBErrors.RecordUpdateInvalid, name);
        }
        // Correct Timestamp
        if (timestampColumn != null)
        { // Set the correct Timestamp
            int i = rec.getFieldIndex(timestampColumn);
            if (i >= 0)
                fields[i] = timestamp;
        }
        // Change State
        rec.changeState(DBRecord.REC_VALID, null);
        return success();
    }
    
    /**
     * Deletes a single record from the database.<BR>
     * <P>
     * @param id the record's primary key
     * @param conn a valid JDBC connection
     * @return true if the record has been successfully deleted or false otherwise
     */
    public final boolean deleteRecord(Object id, Connection conn)
    {
        return deleteRecord(new Object[] { id }, conn);
    }

    /**
     * Deletes all records which reference this table.
     * <P>
     * @param key the key the record to be deleted
     * @param conn a valid connection
     * @return true if all reference records could be deleted
     */
    protected final boolean deleteAllReferences(Object[] key, Connection conn)
    {
        // Merge Sub-Records
        List<DBRelation> relations = db.getRelations();
        DBColumn[] keyColumns = getKeyColumns();
        if (keyColumns==null)
            return success(); // No primary key - no references!
        // Find all relations
        for (DBRelation rel : relations)
        {   // References
            DBReference[] refs = rel.getReferences();
            for (int i=0; i<refs.length; i++)
            {
                if (refs[i].getTargetColumn().equals(keyColumns[0]))
                {   // Found a reference on RowSet
                    DBRowSet rs = refs[0].getSourceColumn().getRowSet(); 
                    if (rs.deleteReferenceRecords(refs, key, conn)==false)
                        return false;
                }
            }
        }
        // No delete this record
        return success();
    }
    
    /**
     * Deletes all records which are referenced by a particular relation.
     * <P>
     * @param refs the reference columns belonging to the releation
     * @param parentKey the key of the parent element
     * @param conn a valid connection
     * @return true if all records could be deleted or false otherwise
     */
    protected boolean deleteReferenceRecords(DBReference[] refs, Object[] parentKey, Connection conn)
    {
        // Key length and reference length must match
        if (refs.length!=parentKey.length)
            return error(DBErrors.RecordInvalidKey);
        // Rowset
        DBColumn[] keyColumns = getKeyColumns();
        if (keyColumns==null || keyColumns.length==0)
        {   // No Primary Key
            DBCommand cmd = db.createCommand();
            for (int i=0; i<parentKey.length; i++)
                cmd.where(refs[i].getSourceColumn().is(parentKey[i]));
            if (db.executeSQL(cmd.getDelete((DBTable)this), conn)<0)
                return error(db);
        }
        else
        {   // Query all keys
            DBCommand cmd = db.createCommand();
            cmd.select(keyColumns);
            for (int i=0; i<parentKey.length; i++)
            {
                cmd.where(refs[i].getSourceColumn().is(parentKey[i]));
                cmd.orderBy(keyColumns[i], true);
            }
            // Query all keys
            List<Object[]> recKeys = db.queryObjectList(cmd.getSelect(), conn);
            for (Object[] recKey : recKeys)
            {   
                log.info("Deleting Record " + StringUtils.valueOf(recKey) + " from table " + getName());
                if (deleteRecord(recKey, conn)==false)
                    return false;
            }
        }
        // Done
        return success();
    }
    
}

