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
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.commons.StringUtils;
import org.apache.empire.data.ColumnExpr;
import org.apache.empire.db.exceptions.NoPrimaryKeyException;
import org.apache.empire.db.exceptions.RecordUpdateAmbiguousException;
import org.apache.empire.db.exceptions.RecordUpdateFailedException;
import org.apache.empire.db.expr.compare.DBCompareColExpr;
import org.apache.empire.db.expr.compare.DBCompareExpr;
import org.apache.empire.db.expr.join.DBColumnJoinExpr;
import org.apache.empire.db.expr.join.DBJoinExpr;
import org.apache.empire.exceptions.InvalidArgumentException;
import org.apache.empire.exceptions.ItemNotFoundException;
import org.apache.empire.exceptions.NotImplementedException;
import org.apache.empire.exceptions.NotSupportedException;


/**
 * This class can be used to wrap a query from a DBCommand and use it like a DBRowSet.<BR>
 * You may use this class for two purposes:
 * <UL>
 *  <LI>In oder to define subqueries simply define a command object with the subquery and wrap it inside a DBQuery.
 *    Then in a second command object you can reference this Query to join with your other tables and views.
 *    In order to join other columns with your query use findColumn(DBColumnExpr expr) to get the 
 *    query column object for a given column expression in the original select clause.</LI> 
 *  <LI>With a key supplied you can have an updateable query that will update several records at once.</LI>
 * </UL>
 *
 */
public class DBQuery extends DBRowSet
{
    // *Deprecated* private static final long serialVersionUID = 1L;

    private static AtomicInteger queryCount = new AtomicInteger(0);
    /**
     * Automatically generates a new alias for this Object 
     * @param prefix the alias prefix
     * @return an alias consisting of the prefix and a unique number
     */
    protected String generateAlias(String prefix)
    {
        return prefix + String.valueOf(queryCount.incrementAndGet());
    }

    /**
     * DBQueryExprColumn 
     * @author doebele
     */
    protected static class DBQueryExprColumn extends DBQueryColumn
    {
        // *Deprecated* private static final long serialVersionUID = 1L;
        
        protected DBQueryExprColumn(DBQuery q, String name, DBColumnExpr expr)
        {
            super(q, name, expr);
        }
        
        @Override
        public DBColumn getUpdateColumn()
        {
            return expr.getUpdateColumn();
        }
        
        @Override
        public boolean equals(Object other)
        {
            if (super.equals(other))
                return true;
            if (other instanceof DBQueryColumn)
            {   // compare expressions
                DBQueryColumn oc = (DBQueryColumn)other;
                return (this.rowset.equals(oc.getRowSet()) && this.expr.equals(oc.getExpr()));
            }
            return false;
        }
    }

    protected final DBCommandExpr   cmdExpr;
    protected final DBColumn[]      keyColumns;
    protected final DBQueryColumn[] queryColumns;
    protected final String          alias;
    protected boolean               updateable;

    /**
     * Constructor initializes the query object.
     * Saves the columns and the primary key of this query.
     * 
     * @param cmd the SQL-Command
     * @param keyColumns an array of the primary key columns
     * @param alias the query alias
     */
    public DBQuery(DBCommandExpr cmd, DBColumn[] keyColumns, String alias)
    { // Set the column expressions
        super(cmd.getDatabase());
        // generate alias
        if (StringUtils.isEmpty(alias))
            alias = generateAlias("q");
        // set 
        this.cmdExpr = cmd;
        this.alias = alias;
        // Set Query Columns
        DBColumnExpr[] exprList = cmd.getSelectExprList();
        this.queryColumns = new DBQueryColumn[exprList.length];
        for (int i = 0; i < exprList.length; i++)
        {   // Init Columns 
            queryColumns[i] = createQueryColumn(exprList[i], i);
            // add column
            DBColumn column;
            if (exprList[i] instanceof DBColumn)
            {   // use directly
                column = (DBColumn)exprList[i];
            }
            else
            {   // create Wrapper
                column = new DBQueryExprColumn(this, queryColumns[i].getName(), exprList[i]); 
            }
            columns.add(column);
        }
        // Set the key Column
        this.keyColumns = keyColumns;
        this.updateable = false;
    }

    /**
     * Constructor initializes the query object.
     * Saves the columns and the primary key of this query.
     * 
     * @param cmd the SQL-Command
     * @param keyColumns an array of the primary key columns
     */
    public DBQuery(DBCommandExpr cmd, DBColumn[] keyColumns)
    {   // Set the column expressions
        this(cmd, keyColumns, null);
    }
    
    /**
     * Constructs a new DBQuery object initialize the query object.
     * Save the columns and the primary key of this query.
     * 
     * @param cmd the SQL-Command
     * @param keyColumn the primary key column
     * @param alias the query alias
     */
    public DBQuery(DBCommandExpr cmd, DBColumn keyColumn, String alias)
    { // Set the column expressions
        this(cmd, new DBColumn[] { keyColumn }, alias);
    }
    
    /**
     * Constructs a new DBQuery object initialize the query object.
     * Save the columns and the primary key of this query.
     * 
     * @param cmd the SQL-Command
     * @param keyColumn the primary key column
     */
    public DBQuery(DBCommandExpr cmd, DBColumn keyColumn)
    { // Set the column expressions
        this(cmd, new DBColumn[] { keyColumn });
    }

    /**
     * Creates a DBQuery object from a given command object.
     * 
     * @param cmd the command object representing an SQL-Command.
     * @param alias the query alias
     */
    public DBQuery(DBCommandExpr cmd, String alias)
    { // Set the column expressions
        this(cmd, (DBColumn[]) null, alias);
    }

    /**
     * Creates a DBQuery object from a given command object.
     * 
     * @param cmd the command object representing an SQL-Command.
     */
    public DBQuery(DBCommandExpr cmd)
    { // Set the column expressions
        this(cmd, (DBColumn[]) null);
    }

    /**
     * Creates a copy of an existing DBQuery object.
     */
    public DBQuery(DBQuery other, String newAlias)
    { 
        this(other.cmdExpr, other.keyColumns, newAlias);
        this.updateable = other.updateable;
    }

    /**
     * returns the underlying command expression
     * @return the command used for this query
     */
    public DBCommandExpr getCommandExpr()
    {
        return cmdExpr;
    }

    /**
     * not applicable - returns null
     */
    @Override
    public String getName()
    {
        return alias;
    }

    /**
     * not applicable - returns null
     */
    @Override
    public String getAlias()
    {
        return alias;
    }
    
    /**
     * Returns whether or not the table supports record updates.
     * @return true if the table allows record updates
     */
    @Override
    public boolean isUpdateable()
    {
        return updateable;
    }
    
    /**
     * Makes the Query updateable. Queries are not updateable by default. 
     * For a query to be updateable it must have key columns
     */
    public void setUpdateable(boolean updateable)
    {
        if (updateable && getKeyColumns()==null)
            throw new NotSupportedException(this, "setUpdateable");
        // set updateable
        this.updateable = updateable;
    }

    /**
     * Gets all columns of this rowset (e.g. for cmd.select()).
     * 
     * @return all columns of this rowset
     */
    public DBQueryColumn[] getQueryColumns()
    {
        return queryColumns;
    }

    /**
     * This function provides the query column object for a particular query command expression 
     * 
     * @param expr the DBColumnExpr object
     * @return the query column
     */
    public DBQueryColumn findColumn(DBColumnExpr expr)
    {
        for (int i = 0; i < queryColumns.length; i++)
        {
            if (ObjectUtils.compareEqual(queryColumns[i].getExpr(), expr))
                return queryColumns[i];
        }
        // not found
        return null;
    }
    
    /**
     * This function provides the query column object for a particular query command expression 
     * 
     * @param name the column name
     * @return the query column
     */
    public DBQueryColumn findColumn(String name)
    {
        for (int i = 0; i < queryColumns.length; i++)
        {
            if (StringUtils.compareEqual(queryColumns[i].getName(), name, true))
                return queryColumns[i];
        }
        // not found
        return null;
    }

    /**
     * This is a convenience shortcut for findQueryColumn
     * 
     * @param expr the DBColumnExpr object
     * @return the query column
     */
    public DBQueryColumn column(DBColumnExpr expr)
    {
        DBQueryColumn col = findColumn(expr);
        if (col==null)
            throw new ItemNotFoundException(expr);
        return col;
    }
    
    /**
     * This is a convenience shortcut for findQueryColumn
     * 
     * @param name the column name
     * @return the located column
     */
    public DBQueryColumn column(String name)
    {
        DBQueryColumn col = findColumn(name);
        if (col==null)
            throw new ItemNotFoundException(name);
        return col;
    }
    
    /**
     * return query key columns
     */
    @Override
    public DBColumn[] getKeyColumns()
    {
        return keyColumns;
    }
    
    /**
     * Returns a array of primary key columns by a specified DBRecord object.
     * 
     * @param record the DBRecord object, contains all fields and the field properties
     * @return a array of primary key columns
     */
    protected Object[] getRecordKey(DBRecordBase record)
    {
        if (record == null || record.getRowSet() != this)
            throw new InvalidArgumentException("record", record);
        // get Key
        Object rowSetData = getRowsetData(record);
        if (rowSetData instanceof Object[])
            return (Object[])rowSetData;
        // generate key now
        return record.getKey();
    }

    /**
     * Adds the select SQL Command of this object to the specified StringBuilder object.
     * 
     * @param sql the SQL-Command
     * @param context the current SQL-Command context
     */
    @Override
    public void addSQL(DBSQLBuilder sql, long context)
    {
        sql.append("(");
        sql.append(cmdExpr);
        sql.append(")");
        // Add Alias
        if ((context & CTX_ALIAS) != 0 && alias != null)
        { // append alias
            sql.append(" ");
            sql.append(alias);
        }
    }
    
    /**
     * Add rowset data
     */
    @Override
    public void initRecord(DBRecordBase record, DBRecordData recData, boolean newRecord)
    {
        // init
        super.initRecord(record, recData, newRecord);
        // set record key as rowset data (optional)
        if (keyColumns!=null)
        {   // check
            Object rowsetData = getRowsetData(record);
            if (rowsetData!=null && !(rowsetData instanceof Object[]) && ((Object[])rowsetData).length!=keyColumns.length)
                throw new InvalidArgumentException("rowSetData", rowsetData);
            // create key if not already set
            if (rowsetData==null)
            {   // create key
                Object[] recordKey = new Object[keyColumns.length];
                for (int i=0; i<recordKey.length; i++)
                    recordKey[i]=recData.get(keyColumns[i]);
                rowsetData = recordKey;
                setRowsetData(record, rowsetData);
            }
        }
    }
    
    /**
     * Returns an error, because it is not possible to add a record to a query.
     * 
     * @param record the DBRecord object, contains all fields and the field properties
     * @param initalKey the initial record key
     * @param deferredInit flag whether to defer record initialization
     * @throws NotImplementedException because this is not implemented
     */
    @Override
    public void createRecord(DBRecordBase record, Object[] initalKey, boolean deferredInit)
    {
        throw new NotImplementedException(this, "createRecord");
    }

    /**
     * Creates a select SQL-Command of the query call the InitRecord method to execute the SQL-Command.
     * 
     * @param record the DBRecord object, contains all fields and the field properties
     * @param whereConstraints the constraint for querying the record
     */
    @Override
    public void readRecord(DBRecordBase record, DBCompareExpr whereConstraints)
    {
        if (record==null || whereConstraints==null)
            throw new InvalidArgumentException("record|key", null);
        // Select
        DBCommand cmd = getCommandFromExpression();
        cmd.where(whereConstraints);
        // Read Record
        readRecord(record, cmd);
    }

    /**
     * Updates a query record by creating individual update commands for each table.
     * 
     * @param record the DBRecord object. contains all fields and the field properties
     */
    @Override
    public void updateRecord(DBRecordBase record)
    {
        // check updateable
        if (isUpdateable()==false)
            throw new NotSupportedException(this, "updateRecord");
        // check params
        if (record == null)
            throw new InvalidArgumentException("record", null);
        // Has record been modified?
        if (record.isModified() == false)
            return; // Nothing to update
        // Must have key Columns
        DBColumn[] keyColumns = getKeyColumns();
        if (keyColumns==null)
            throw new NoPrimaryKeyException(this);
        // Get the fields and the flags
        Object[] fields = record.getFields();
        // Get all Update Commands
        DBContext context = record.getContext();
        Map<DBRowSet, DBCommand> updCmds = new HashMap<DBRowSet, DBCommand>(3);
        for (int i = 0; i < columns.size(); i++)
        { // get the table
            DBColumn col = columns.get(i);
            if (col == null)
                continue;
            DBRowSet table = col.getRowSet();
            DBCommand updCmd = updCmds.get(table);
            if (updCmd == null)
            { // Add a new Command
                updCmd = createRecordCommand(context);
                updCmds.put(table, updCmd);
            }
            /*
             * if (updateTimestampColumns.contains( col ) ) { // Check the update timestamp cmd.set( col.to( DBDatabase.SYSDATE ) ); }
             */
            // Set the field Value
            boolean modified = record.wasModified(i);
            if (modified == true)
            { // Update a field
                if (col.isReadOnly() && log.isDebugEnabled())
                    log.debug("updateRecord: Read-only column '" + col.getName() + " has been modified!");
                // Check the value
                col.validateValue(fields[i]);
                // Set
                updCmd.set(col.to(fields[i]));
            }
        }
        // the connection
        Connection conn = context.getConnection();
        // the commands
        DBCommand cmd = getCommandFromExpression();
        Object[] key  = getRecordKey(record);
        DBRowSet table= null;
        DBCommand upd = null;
        for(Entry<DBRowSet,DBCommand> entry:updCmds.entrySet())
        {
            int i = 0;
            // Iterate through options
            table = entry.getKey();
            upd = entry.getValue();
            // Is there something to update
            if (upd.set == null)
                continue; // nothing to do for this table!
            // Evaluate Joins
            for (i = 0; cmd.joins != null && i < cmd.joins.size(); i++)
            {
                DBJoinExpr jex = cmd.joins.get(i);
                if (!(jex instanceof DBColumnJoinExpr))
                    continue;
                DBColumnJoinExpr join = (DBColumnJoinExpr)jex;
                DBColumn left  = join.getLeft() .getUpdateColumn();
                DBColumn right = join.getRight().getUpdateColumn();
                if (left.getRowSet()==table && table.isKeyColumn(left))
                    if (!addJoinRestriction(upd, left, right, keyColumns, key, record))
                        throw new ItemNotFoundException(left.getFullName());
                if (right.getRowSet()==table && table.isKeyColumn(right))
                    if (!addJoinRestriction(upd, right, left, keyColumns, key, record))
                        throw new ItemNotFoundException(right.getFullName());
            }
            // Evaluate Existing restrictions
            for (i = 0; cmd.where != null && i < cmd.where.size(); i++)
            {
                DBCompareExpr cmp = cmd.where.get(i);
                if (cmp instanceof DBCompareColExpr)
                { 	// Check whether constraint belongs to update table
                    DBCompareColExpr cmpExpr = (DBCompareColExpr) cmp;
                    DBColumn col = cmpExpr.getColumnExpr().getUpdateColumn();
                    if (col!=null && col.getRowSet() == table)
                    {	// add the constraint
                    	if (cmpExpr.getValue() instanceof DBCmdParam)
                    	{	// Create a new command param
                    		DBColumnExpr colExpr = cmpExpr.getColumnExpr();
                    		DBCmdParam param =(DBCmdParam)cmpExpr.getValue(); 
                    		DBCmdParam value = upd.addParam(colExpr, param.getValue());
                    		cmp = new DBCompareColExpr(colExpr, cmpExpr.getCmpOperator(), value);
                    	}
                        upd.where(cmp);
                    }    
                } 
                else
                {	// other constraints are not supported
                    throw new NotSupportedException(this, "updateRecord with "+cmp.getClass().getName());
                }
            }
            // Add Restrictions
            for (i = 0; i < keyColumns.length; i++)
            {
                if (keyColumns[i].getRowSet() == table)
                {   // Set key column constraint
                    Object value = key[i];
                    upd.where(keyColumns[i].is(value));
                }
            }    

            // Set Update Timestamp
            int timestampIndex = -1;
            Object timestampValue = null;
            if (table.getTimestampColumn() != null)
            {
                DBColumn tsColumn = table.getTimestampColumn();
                timestampIndex = this.getColumnIndex(tsColumn);
                if (timestampIndex>=0)
                {   // The timestamp is availabe in the record
                    timestampValue = context.getDbms().getUpdateTimestamp(conn); 
                    Object lastTS = fields[timestampIndex];
                    if (ObjectUtils.isEmpty(lastTS)==false)
                    {   // set timestamp constraint
                        upd.where(tsColumn.is(lastTS));
                    }    
                    // Set new Timestamp
                    upd.set(tsColumn.to(timestampValue));
                }
                else
                {   // Timestamp columns has not been provided with the record
                    upd.set(tsColumn.to(DBDatabase.SYSDATE));
                }
            }
            
            // Execute SQL
            DBUtils utils = context.getUtils(); 
            int affected = utils.executeSQL(upd.getUpdate(), upd.getParamValues(), null);
            if (affected<= 0)
            {   // Error
                if (affected == 0)
                { // Record not found
                    throw new RecordUpdateFailedException(this, key);
                }
                // Rollback
                // context.rollback();
                return;
            } 
            else if (affected > 1)
            { // More than one record
                throw new RecordUpdateAmbiguousException(this, key);
            } 
            else
            { // success
                log.info("Record for table '" + table.getName() + " successfully updated!");
            }
            // Correct Timestamp
            if (timestampIndex >= 0)
            {   // Set the correct Timestamp
                fields[timestampIndex] = timestampValue;
            }
        }
        // success
        record.updateComplete();
    }

    /**
     * Deletes a record identified by its primary key from the database.
     * 
     * @param key array of primary key values
     * @param context the database context
     */
    @Override
    public void deleteRecord(Object[] key, DBContext context)
    {
        throw new NotImplementedException(this, "deleteRecord()");
    }

    /**
     * Adds join restrictions to the supplied command object.
     * @param cmd the command
     * @param updCol the update column
     * @param joinCol the join column
     * @param keyColumns the key columns
     * @param key the record key
     * @param record the record
     * @return flag whether the join restriction could be added
     */
    protected boolean addJoinRestriction(DBCommand cmd, DBColumn updCol, DBColumn joinCol, DBColumn[] keyColumns, Object[] key, DBRecordBase record)
    {   // Find key for foreign field
        for (int i = 0; key!=null && i < keyColumns.length; i++)
            if (keyColumns[i]==joinCol)
            {   // Set Field from Key
                cmd.where(updCol.is(key[i]));
                return true;
            }
        // Not found, what about the record
        int index = this.getColumnIndex(updCol);
        if (index<0)
            index = this.getColumnIndex(joinCol);
        if (index>=0)
        {   // Field Found
            if (record.wasModified(index))
                return false; // Ooops, Key field has changed
            // Set Constraint
            cmd.where(updCol.is(record.getValue(index)));
            return true;
        }
        return false;
    }

    /**
     * returns the command from the underlying command expression or throws an exception
     * @return the command used for this query
     */
    protected DBCommand getCommandFromExpression()
    {
        if (cmdExpr instanceof DBCommand)
            return ((DBCommand)cmdExpr);
        // not supported
        throw new NotSupportedException(this, "getCommand");
    }
    
    /**
     * factory method for column expressions in order to allow overrides 
     * @param expr
     * @return the query column
     */
    protected DBQueryColumn createQueryColumn(DBColumnExpr expr, int index)
    {
        String name = expr.getName();
        if (StringUtils.isEmpty(name))
            name = "COL_"+String.valueOf(index);
        // create wrapper
        return new DBQueryColumn(this, name, expr);
    }
    
    /**
     * Gets the index of a particular column expression.
     * 
     * @param columnExpr the column expression for which to get the index of
     * 
     * @return the position of a column expression
     */
    @Override
    public int getColumnIndex(ColumnExpr columnExpr)
    {
        if (columnExpr instanceof DBColumn)
            return getColumnIndex((DBColumn)columnExpr);
        else
            for (int i=0; i<queryColumns.length; i++)
            {   // find expression in QueryColumns
                DBColumnExpr expr = queryColumns[i].getExpr();
                if (expr.equals(columnExpr))
                    return i; // found
            }
        // try unwrap
        ColumnExpr unwrapped = ObjectUtils.unwrap(columnExpr);
        if (unwrapped!=columnExpr)
            return getColumnIndex(unwrapped);
        // not found
        return -1;
    }

    @Override
    public int getColumnIndex(DBColumn column)
    {
        // (changed 2024-07-18 EMPIREDB-434)
        // 1st try: compare columns
        int count = columns.size();
        for (int index=0; index<count; index++)
        {   // check update column
            if (columns.get(index).equals(column))
                return index;
        }
        // 2nd try: Match update column
        for (int index=0; index<count; index++)
        {   // check update column
            DBColumn c = columns.get(index);
            if ((c instanceof DBQueryExprColumn))
            {   // Update columns match
                if (column.equals(c.getUpdateColumn()))
                    return index;
            }
        }
        // not found
        return -1;
    }

    @Override
    protected DBColumnExpr getColumnExprAt(int index)
    {
        DBColumn column = columns.get(index);
        if (column instanceof DBQueryColumn)
            return ((DBQueryExprColumn)column).expr;  // unwrap
        // use column
        return column;
    }

    @Override
    protected DBColumn cloneColumn(DBRowSet clone, DBColumn scourceColumn)
    {
        throw new NotSupportedException(this, "cloneColumn");
    }
  
}
