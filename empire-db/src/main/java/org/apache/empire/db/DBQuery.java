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
import org.apache.empire.db.exceptions.InvalidKeyException;
import org.apache.empire.db.exceptions.NoPrimaryKeyException;
import org.apache.empire.db.exceptions.QueryNoResultException;
import org.apache.empire.db.exceptions.RecordNotFoundException;
import org.apache.empire.db.exceptions.RecordUpdateFailedException;
import org.apache.empire.db.exceptions.RecordUpdateInvalidException;
import org.apache.empire.db.expr.column.DBAliasExpr;
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
 *    In order to join other columns with your query use findQueryColumn(DBColumnExpr expr) to get the 
 *    query column object for a given column expression in the original select clause.</LI> 
 *  <LI>With a key supplied you can have an updateable query that will update several records at once.</LI>
 * </UL>
 *
 */
public class DBQuery extends DBRowSet
{
    private final static long serialVersionUID = 1L;

    private static AtomicInteger queryCount = new AtomicInteger(0);
    
    /**
     * DBQueryExprColumn 
     * @author doebele
     */
    protected static class DBQueryExprColumn extends DBQueryColumn
    {
        private static final long serialVersionUID = 1L;
        
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

    /**
     * Constructor initializes the query object.
     * Saves the columns and the primary keys of this query.
     * 
     * @param cmd the SQL-Command
     * @param keyColumns an array of the primary key columns
     * @param the query alias
     */
    public DBQuery(DBCommandExpr cmd, DBColumn[] keyColumns, String alias)
    { // Set the column expressions
        super(cmd.getDatabase());
        this.cmdExpr = cmd;
        this.alias = alias;
        // Set Query Columns
        DBColumnExpr[] exprList = cmd.getSelectExprList();
        this.queryColumns = new DBQueryColumn[exprList.length];
        for (int i = 0; i < exprList.length; i++)
        {   // Init Columns 
            queryColumns[i] = createQueryColumn(exprList[i], i);
            // add column
            DBColumn column = exprList[i].getUpdateColumn();
            if (column==null || (exprList[i] instanceof DBAliasExpr))
            {   // user QueryColumn
                column = new DBQueryExprColumn(this, queryColumns[i].getName(), exprList[i]); 
            }
            columns.add(column);
        }
        // Set the key Column
        this.keyColumns = keyColumns;
    }

    /**
     * Constructor initializes the query object.
     * Saves the columns and the primary keys of this query.
     * 
     * @param cmd the SQL-Command
     * @param keyColumns an array of the primary key columns
     */
    public DBQuery(DBCommandExpr cmd, DBColumn[] keyColumns)
    {   // Set the column expressions
        this(cmd, keyColumns, "q" + String.valueOf(queryCount.incrementAndGet()));
    }
    
    /**
     * Constructs a new DBQuery object initialize the query object.
     * Save the columns and the primary key of this query.
     * 
     * @param cmd the SQL-Command
     * @param keyColumn the primary key column
     * @param the query alias
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
     * Creaes a DBQuery object from a given command object.
     * 
     * @param cmd the command object representing an SQL-Command.
     * @param the query alias
     */
    public DBQuery(DBCommandExpr cmd, String alias)
    { // Set the column expressions
        this(cmd, (DBColumn[]) null, alias);
    }

    /**
     * Creaes a DBQuery object from a given command object.
     * 
     * @param cmd the command object representing an SQL-Command.
     */
    public DBQuery(DBCommandExpr cmd)
    { // Set the column expressions
        this(cmd, (DBColumn[]) null);
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
        return (getKeyColumns()!=null);
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
    public DBQueryColumn findQueryColumn(DBColumnExpr expr)
    {
        for (int i = 0; i < queryColumns.length; i++)
        {
            if (queryColumns[i].expr.equals(expr))
                return queryColumns[i];
        }
        // not found
        return null;
    }
    
    /**
     * This function provides the query column object for a particular query command expression 
     * 
     * @param the column name
     * @return the query column
     */
    public DBQueryColumn findQueryColumn(String name)
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
        return findQueryColumn(expr);
    }
    
    /**
     * This is a convenience shortcut for findQueryColumn
     * 
     * @param the column name
     * @return the located column
     */
    public DBQueryColumn column(String name)
    {
        return findQueryColumn(name);
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
    @Override
    public Object[] getRecordKey(DBRecord record)
    {
        if (record == null || record.getRowSet() != this)
            throw new InvalidArgumentException("record", record);
        // get Key
        Object rowSetData = record.getRowSetData();
        if (rowSetData==null)
            log.warn("No Record-key provided for query record!");
        return (Object[])rowSetData;
    }

    /**
     * Adds the select SQL Command of this object to the specified StringBuilder object.
     * 
     * @param buf the SQL-Command
     * @param context the current SQL-Command context
     */
    @Override
    public void addSQL(StringBuilder buf, long context)
    {
        buf.append("(");
        buf.append(cmdExpr.getSelect());
        buf.append(")");
        // Add Alias
        if ((context & CTX_ALIAS) != 0 && alias != null)
        { // append alias
            buf.append(" ");
            buf.append(alias);
        }
    }

    /**
     * Initialize specified DBRecord object with primary key
     * columns (the Object[] keyValues).
     * 
     * @param rec the Record object
     * @param keyValues an array of the primary key columns
     */
    @Override
    public void initRecord(DBRecord rec, Object[] keyValues, boolean insert)
    {
        // Prepare
        prepareInitRecord(rec, keyValues, insert);
        // Initialize all Fields
        Object[] fields = rec.getFields();
        for (int i = 0; i < fields.length; i++)
            fields[i] = ObjectUtils.NO_VALUE;
        // Set primary key values
        if (keyValues != null)
        { // search for primary key fields
            DBColumn[] keyColumns = getKeyColumns();
            for (int i = 0; i < keyColumns.length; i++)
                if (columns.contains(keyColumns[i]))
                    fields[columns.indexOf(keyColumns[i])] = keyValues[i];
        }
        // Init
        completeInitRecord(rec);
    }
    
    /**
     * Returns an error, because it is not possible to add a record to a query.
     * 
     * @param rec the DBRecord object, contains all fields and the field properties
     * @param conn a valid database connection
     * @throws NotImplementedException because this is not implemented
     */
    @Override
    public void createRecord(DBRecord rec, boolean deferredInit)
    {
        throw new NotImplementedException(this, "createRecord");
    }

    /**
     * Creates a select SQL-Command of the query call the InitRecord method to execute the SQL-Command.
     * 
     * @param rec the DBRecord object, contains all fields and the field properties
     * @param key an array of the primary key columns
     * @param conn a valid connection to the database.
     */
    @Override
    public void readRecord(DBRecord rec, Object[] key)
    {
        if (rec == null)
            throw new InvalidArgumentException("conn|rec", null);
        DBColumn[] keyColumns = getKeyColumns();
        if (key == null || keyColumns.length != key.length)
            throw new InvalidKeyException(this, key);
        // Select
        DBCommand cmd = getCommandFromExpression();
        for (int i = 0; i < keyColumns.length; i++)
        {   // Set key column constraint
            Object value = key[i];
            if (db.isPreparedStatementsEnabled())
                value = cmd.addParam(keyColumns[i], value);
            cmd.where(keyColumns[i].is(value));
        }    
        // Read Record
        try {
            // Read Record
            readRecord(rec, cmd, key.clone()); 
        } catch (QueryNoResultException e) {
            // Record not found
            throw new RecordNotFoundException(this, key);
        }
    }

    /**
     * Updates a query record by creating individual update commands for each table.
     * 
     * @param rec the DBRecord object. contains all fields and the field properties
     * @param conn a valid connection to the database.
     */
    @Override
    public void updateRecord(DBRecord rec)
    {
        // check updateable
        if (isUpdateable()==false)
            throw new NotSupportedException(this, "updateRecord");
        // check params
        if (rec == null)
            throw new InvalidArgumentException("record", null);
        // Has record been modified?
        if (rec.isModified() == false)
            return; // Nothing to update
        // Must have key Columns
        DBColumn[] keyColumns = getKeyColumns();
        if (keyColumns==null)
            throw new NoPrimaryKeyException(this);
        // Get the fields and the flags
        Object[] fields = rec.getFields();
        // Get all Update Commands
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
                updCmd = db.createCommand();
                updCmds.put(table, updCmd);
            }
            /*
             * if (updateTimestampColumns.contains( col ) ) { // Check the update timestamp cmd.set( col.to( DBDatabase.SYSDATE ) ); }
             */
            // Set the field Value
            boolean modified = rec.wasModified(i);
            if (modified == true)
            { // Update a field
                if (col.isReadOnly() && log.isDebugEnabled())
                    log.debug("updateRecord: Read-only column '" + col.getName() + " has been modified!");
                // Check the value
                col.validate(fields[i]);
                // Set
                updCmd.set(col.to(fields[i]));
            }
        }
        // the connection
        Connection conn = rec.getContext().getConnection();
        // the commands
        DBCommand cmd = getCommandFromExpression();
        Object[] key  = getRecordKey(rec);
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
                    if (!addJoinRestriction(upd, left, right, keyColumns, key, rec))
                        throw new ItemNotFoundException(left.getFullName());
                if (right.getRowSet()==table && table.isKeyColumn(right))
                    if (!addJoinRestriction(upd, right, left, keyColumns, key, rec))
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
                    		cmp = new DBCompareColExpr(colExpr, cmpExpr.getCmpop(), value);
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
                    if (db.isPreparedStatementsEnabled())
                        value = upd.addParam(keyColumns[i], value);
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
                    timestampValue = db.getUpdateTimestamp(conn); 
                    Object lastTS = fields[timestampIndex];
                    if (ObjectUtils.isEmpty(lastTS)==false)
                    {   // set timestamp constraint
                        if (db.isPreparedStatementsEnabled())
                            lastTS = upd.addParam(tsColumn, lastTS);
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
            DBUtils utils = rec.getContext().getUtils(); 
            int affected = utils.executeSQL(upd.getUpdate(), upd.getParamValues());
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
                throw new RecordUpdateInvalidException(this, key);
            } 
            else
            { // success
                log.info("Record for table '" + table.getName() + " sucessfully updated!");
            }
            // Correct Timestamp
            if (timestampIndex >= 0)
            {   // Set the correct Timestamp
                fields[timestampIndex] = timestampValue;
            }
        }
        // success
        rec.updateComplete(key);
    }

    /**
     * Deletes a record identified by its primary key from the database.
     * 
     * @param keys array of primary key values
     * @param conn a valid database connection
     */
    @Override
    public void deleteRecord(Object[] keys, DBContext context)
    {
        throw new NotImplementedException(this, "deleteRecord()");
    }

    /**
     * Adds join restrictions to the supplied command object.
     */
    protected boolean addJoinRestriction(DBCommand upd, DBColumn updCol, DBColumn keyCol, DBColumn[] keyColumns, Object[] keyValues, DBRecord rec)
    {   // Find key for foreign field
        for (int i = 0; keyValues!=null && i < keyColumns.length; i++)
            if (keyColumns[i]==keyCol)
            {   // Set Field from Key
                upd.where(updCol.is(keyValues[i]));
                return true;
            }
        // Not found, what about the record
        int index = this.getColumnIndex(updCol);
        if (index<0)
            index = this.getColumnIndex(keyCol);
        if (index>=0)
        {   // Field Found
            if (rec.wasModified(index))
                return false; // Ooops, Key field has changed
            // Set Constraint
            upd.where(updCol.is(rec.getValue(index)));
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

    @Override
    public int getColumnIndex(DBColumn column)
    {
        int index = columns.indexOf(column);
        if (index>=0)
            return index;
        // find by update column
        index=0;
        for (DBColumn c : columns)
        {   // check update column
            if ((c instanceof DBQueryExprColumn) && column.equals(c.getUpdateColumn()))
                 return index;
            // next
            index++;
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
  
}
