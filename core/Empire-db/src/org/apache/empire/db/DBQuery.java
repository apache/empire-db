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
import java.util.Iterator;
import java.util.Map;

import org.apache.empire.commons.Errors;
import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.commons.Options;
import org.apache.empire.data.DataType;
import org.apache.empire.db.expr.compare.DBCompareColExpr;
import org.apache.empire.db.expr.compare.DBCompareExpr;
import org.apache.empire.db.expr.join.DBJoinExpr;
import org.w3c.dom.Element;


/**
 * This class can be used to wrap a query from a DBCommand and use it like a DBRowSet.<BR>
 * You may use this class for two purposes:
 * <UL>
 *  <LI>In oder to define subqueries simply define a command object with the subquery and wrap it inside a DBQuery.
 *    Then in a second command object you can reference this Query to join with your other tables and views.
 *    In order to join other columns with your query use findQueryColumn(DBColumnExpr expr) to get the 
 *    query column object for a given column expression in the orignial select clause.</LI> 
 *  <LI>With a key supplied you can have an updateable query that will update several records at once.</LI>
 * </UL>
 * @author ESTEAM software <A TARGET="esteam" HREF="http://www.esteam.de">www.esteam.de </A>
 */
public class DBQuery extends DBRowSet
{
    public static class DBQueryColumn extends DBColumn
    {
        protected DBColumnExpr expr;

        /**
         * Constructs a DBQueryColumn object set the specified parameters to this object.
         * <P>
         * @param query the DBQuery object
         * @param expr the concrete DBColumnExpr object
         */
        public DBQueryColumn(DBQuery query, DBColumnExpr expr)
        { // call base
            super(query, expr.getName());
            // set Expression
            this.expr = expr;
        }

        @Override
        public DataType getDataType()
        {
            return expr.getDataType();
        }

        @Override
        public double getSize()
        {
            DBColumn column = expr.getUpdateColumn();
            if (column==null)
                return 0.0;
            return column.getSize();
        }

        @Override
        public boolean isReadOnly()
        {
            DBColumn column = expr.getUpdateColumn();
            if (column==null)
                return true;
            return column.isReadOnly();
        }

        @Override
        public boolean isRequired()
        {
            DBColumn column = expr.getUpdateColumn();
            if (column==null)
                return false;
            return column.isRequired();
        }

        @Override
        public boolean checkValue(Object value)
        {
            DBColumn column = expr.getUpdateColumn();
            if (column==null)
                return true;
            return column.checkValue(value);
        }

        @Override
        public Object getAttribute(String name)
        {
            if (attributes != null && attributes.containsKey(name))
                return attributes.get(name);
            // Otherwise ask expression
            DBColumn column = expr.getUpdateColumn();
            if (column==null)
                return null;
            return column.getAttribute(name);
        }

        @Override
        public Options getOptions()
        {
            if (options != null)
                return options;
            // Otherwise ask expression
            DBColumn column = expr.getUpdateColumn();
            if (column==null)
                return null;
            return column.getOptions();
        }

        @Override
        public Element addXml(Element parent, long flags)
        {
            return expr.addXml(parent, flags);
        }
    }

    private static int        queryCount   = 1;

    protected DBCommand       cmd;
    protected DBColumn[]      keyColumns = null;
    protected DBQueryColumn[] queryColumns = null;
    protected String          alias;

    /**
     * Constructor initializes the query object.
     * Saves the columns and the primary keys of this query.
     * 
     * @param cmd the SQL-Command
     * @param keyColumns an array of the primary key columns
     */
    public DBQuery(DBCommand cmd, DBColumn[] keyColumns)
    { // Set the column expressions
        super(cmd.getDatabase());
        this.cmd = cmd;
        // Set Query Columns
        DBColumnExpr[] exprList = cmd.getSelectExprList();
        queryColumns = new DBQueryColumn[exprList.length];
        for (int i = 0; i < exprList.length; i++)
        {   // Init Columns 
            columns.add(exprList[i].getUpdateColumn());
            queryColumns[i] = new DBQueryColumn(this, exprList[i]);
        }
        // Set the key Column
        this.keyColumns = keyColumns;
        // set alias
        this.alias = "q" + String.valueOf(queryCount);
        queryCount++;
    }

    /**
     * Constructs a new DBQuery object initialize the query object.
     * Save the columns and the primary key of this query.
     * 
     * @param cmd the SQL-Command
     * @param keyColumn the primary key column
     */
    public DBQuery(DBCommand cmd, DBColumn keyColumn)
    { // Set the column expressions
        this(cmd, new DBColumn[] { keyColumn });
    }

    /**
     * Creaes a DBQuery object from a given command object.
     * 
     * @param cmd the command object representing an SQL-Command.
     */
    public DBQuery(DBCommand cmd)
    { // Set the column expressions
        this(cmd, (DBColumn[]) null);
    }

    /**
     * not applicable - returns null
     */
    @Override
    public String getName()
    {
        return null;
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
     * Gets all columns of this rowset (e.g. for cmd.select()).
     * 
     * @return all columns of this rowset
     */
    public DBQueryColumn[] getQueryColumns()
    {
        return queryColumns;
    }

    /**
     * This function searchs for equal columns given by the
     * specified DBColumnExpr object.
     * 
     * @param expr the DBColumnExpr object
     * @return the located column (only DBViewColumn onjects)
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
        {
            error(Errors.InvalidArg, record, "record");
            return null; // Invalid Argument
        }
        // get Key
        return (Object[]) record.getRowSetData();
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
        buf.append(cmd.getSelect());
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
     * @return true if successful
     */
    @Override
    public boolean initRecord(DBRecord rec, Object[] keyValues)
    {
        // Inititialisierung
        if (!prepareInitRecord(rec, DBRecord.REC_EMTPY, keyValues))
            return false;
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
        return completeInitRecord(rec);
    }
    
    /**
     * Returns an error, because querys could't add new records to the database.
     * 
     * @param rec the DBRecord object, contains all fields and the field properties
     * @param conn a valid database connection
     * @return an error, because querys could't add new records to the database
     */
    @Override
    public boolean createRecord(DBRecord rec, Connection conn)
    {
        return error(Errors.NotImplemented, "addRecord");
    }

    /**
     * Creates a select SQL-Command of the query call the InitRecord method to execute the SQL-Command.
     * 
     * @param rec rec the DBRecord object, contains all fields and the field properties
     * @param key an array of the primary key columns
     * @param conn a valid connection to the database.
     * @return true if successful
     */
    @Override
    public boolean readRecord(DBRecord rec, Object[] key, Connection conn)
    {
        if (conn == null || rec == null)
            return error(Errors.InvalidArg, null, "conn|rec");
        DBColumn[] keyColumns = getKeyColumns();
        if (key == null || keyColumns.length != key.length)
            return error(DBErrors.RecordInvalidKey, key);
        // Select
        for (int i = 0; i < keyColumns.length; i++)
            cmd.where(keyColumns[i].is(key[i]));
        // Read Record
        if (!readRecord(rec, cmd, conn))
        { // Record not found
            if (getErrorType() == DBErrors.QueryNoResult)
                return error(DBErrors.RecordNotFound, key);
            // Return given error
            return false;
        }
        // Set RowSetData
        rec.changeState(DBRecord.REC_VALID, key.clone());
        return success();
    }

    /**
     * Updates a query record by creating individual update commands for each table.
     * 
     * @param rec the DBRecord object. contains all fields and the field properties
     * @param conn a valid connection to the database.
     * @return true if succesfull
     */
    @Override
    public boolean updateRecord(DBRecord rec, Connection conn)
    {
        if (conn == null || rec == null)
            return error(Errors.InvalidArg, null, "conn|rec");
        // Has record been modified?
        if (rec.isModified() == false)
            return success(); // Nothing to update
        // Must have key Columns
        DBColumn[] keyColumns = getKeyColumns();
        if (keyColumns==null)
            return error(DBErrors.NoPrimaryKey, getAlias());
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
                if (!col.checkValue(fields[i]))
                    return error(col);
                // Set
                updCmd.set(col.to(fields[i]));
            }
        }
        // the commands
        Object[] keys = (Object[]) rec.getRowSetData();
        Iterator<DBRowSet> tables = updCmds.keySet().iterator();
        while (tables.hasNext())
        {
            int i = 0;
            // Iterate through options
            DBRowSet table = tables.next();
            DBCommand upd = updCmds.get(table);
            // Is there something to update
            if (upd.set == null)
                continue; // nothing to do for this table!
            // Evaluate Joins
            for (i = 0; cmd.joins != null && i < cmd.joins.size(); i++)
            {
                DBJoinExpr join = cmd.joins.get(i);
                DBColumn left  = join.getLeft() .getUpdateColumn();
                DBColumn right = join.getRight().getUpdateColumn();
                if (left.getRowSet()==table && table.isKeyColumn(left))
                    if (!addJoinRestriction(upd, left, right, keyColumns, rec))
                        return error(Errors.ItemNotFound, left.getFullName());
                if (right.getRowSet()==table && table.isKeyColumn(right))
                    if (!addJoinRestriction(upd, right, left, keyColumns, rec))
                        return error(Errors.ItemNotFound, right.getFullName());
            }
            // Evaluate Existing restrictions
            for (i = 0; cmd.where != null && i < cmd.where.size(); i++)
            {
                DBCompareExpr cmp = cmd.where.get(i);
                if (cmp instanceof DBCompareColExpr)
                { // Check whether
                    DBCompareColExpr cmpExpr = (DBCompareColExpr) cmp;
                    DBColumn col = cmpExpr.getColumnExpr().getUpdateColumn();
                    if (col.getRowSet() == table)
                        upd.where(cmp);
                } 
                else
                { // other constraints are not supported
                    return error(Errors.NotSupported, "updateRecord");
                }
            }
            // Add Restrictions
            for (i = 0; i < keyColumns.length; i++)
                if (keyColumns[i].getRowSet() == table)
                    upd.where(keyColumns[i].is(keys[i]));

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
                        upd.where(tsColumn.is(lastTS));
                    // Set new Timestamp
                    upd.set(tsColumn.to(timestampValue));
                }
                else
                {   // Timestamp columns has not been provided with the record
                    upd.set(tsColumn.to(DBDatabase.SYSDATE));
                }
            }
            
            // Execute SQL
            int affected = db.executeSQL(upd.getUpdate(), upd.getCmdParams(), conn);
            if (affected <= 0)
            {   // Error
                if (affected == 0)
                { // Record not found
                    error(DBErrors.RecordUpdateFailed, table.getName());
                }
                // Rollback
                db.rollback(conn);
                return false;
            } 
            else if (affected > 1)
            { // More than one record
                error(DBErrors.RecordUpdateInvalid, table.getName());
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
        rec.changeState(DBRecord.REC_VALID, keys);
        return success();
    }

    /**
     * Adds join restrictions to the supplied command object.
     */
    private boolean addJoinRestriction(DBCommand upd, DBColumn updCol, DBColumn keyCol, DBColumn[] keyColumns, DBRecord rec)
    {   // Find key for forein field
        Object rowsetData = rec.getRowSetData();
        for (int i = 0; i < keyColumns.length; i++)
            if (keyColumns[i]==keyCol && rowsetData!=null)
            {   // Set Field from Key
                upd.where(updCol.is(((Object[]) rowsetData)[i]));
                return true;
            }
        // Not found, what about the reocrd
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
     * Deletes a record identified by its primary key from the database.
     * 
     * @param keys array of primary key values
     * @param conn a valid database connection
     * @return true if the record has been successfully deleted or false otherwise
     */
    @Override
    public boolean deleteRecord(Object[] keys, Connection conn)
    {
        return error(Errors.NotImplemented, "deleteRecord");
    }

}