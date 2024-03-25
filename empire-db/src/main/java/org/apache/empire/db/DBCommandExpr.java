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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.empire.commons.Options;
import org.apache.empire.data.DataType;
import org.apache.empire.db.expr.column.DBCmdResultExpr;
import org.apache.empire.db.expr.compare.DBCompareExpr;
import org.apache.empire.db.expr.order.DBOrderByExpr;
import org.apache.empire.dbms.DBMSHandler;
import org.apache.empire.exceptions.InternalException;
import org.apache.empire.exceptions.InvalidArgumentException;
import org.apache.empire.exceptions.NotSupportedException;
import org.apache.empire.exceptions.ObjectNotValidException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
/**
 * This abstract class handles the creation of the SQL-Commands.
 * There are inner classes to construct different SQL-Commands.
 */
public abstract class DBCommandExpr extends DBExpr
{
    // *Deprecated* private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(DBCommandExpr.class);

    // Internal Classes
    protected static class DBCmdQuery extends DBRowSet
    {
        // *Deprecated* private static final long serialVersionUID = 1L;
        private DBCommandExpr cmd;

        /**
         * Creates a new DBCmdQueryObject
         * @param cmd the command expression
         * @param exprList 
         */
        public DBCmdQuery(DBCommandExpr cmd, DBDatabase db, DBColumnExpr[] exprList)
        { // Set the column expressions
            super(db);
            this.cmd = cmd;
            // Add Expressions to list
            for (int i = 0; i < exprList.length; i++)
                columns.add(exprList[i].getUpdateColumn());
        }

        /** Not applicable - returns null */
        @Override
        public String getName()
        {
            return null;
        }

        /** Not applicable - returns null */
        @Override
        public String getAlias()
        {
            return null;
        }
        
        /** Not applicable - returns false */
        @Override
        public boolean isUpdateable()
        {
            return false;
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
         * Creates the SQL-Command adds the select statement into the SQL-Command.
         * 
         * @param sql the SQL-Command
         * @param context the current SQL-Command context
         */
        @Override
        public void addSQL(DBSQLBuilder sql, long context)
        {
            sql.append("(");
            sql.append(cmd);
            sql.append(")");
        }

        /**
         * Prints the error message: ERR_NOTSUPPORTED.
         * 
         * @return null
         */
        @Override
        public DBColumn[] getKeyColumns()
        {
            throw new NotSupportedException(this, "getKeyColumns");
        }

        /** throws ERR_NOTSUPPORTED */
        @Override
        public void createRecord(DBRecordBase record, Object[] initalKey, boolean deferredInit)
        {
            throw new NotSupportedException(this, "addRecord");
        }

        /** throws ERR_NOTSUPPORTED */
        @Override
        public void readRecord(DBRecordBase record, DBCompareExpr whereConstraints)
        {
            throw new NotSupportedException(this, "getRecord");
        }

        /** throws ERR_NOTSUPPORTED */
        @Override
        public void updateRecord(DBRecordBase rec)
        {
            throw new NotSupportedException(this, "updateRecord");
        }

        /** throws ERR_NOTSUPPORTED */
        @Override
        public void deleteRecord(Object[] key, DBContext context)
        {
            throw new NotSupportedException(this, "deleteRecord");
        }
    }

    /**
     * This class wraps a column of sql command in a special command column object. 
     */
    protected static class DBCmdColumn extends DBColumn
    {
        // *Deprecated* private static final long serialVersionUID = 1L;
        private DBColumnExpr expr;

        /**
         * Constructs a new DBCmdColumn object
         * 
         * @param query the row set
         * @param expr the column
         */
        public DBCmdColumn(DBRowSet query, DBColumnExpr expr)
        { // call base
            super(query, expr.getName());
            // set Expression
            this.expr = expr;
        }

        /**
         * create the SQL-Command set the expression name to the SQL-Command
         * 
         * @param sql the SQL-Command
         * @param context the current SQL-Command context
         */
        @Override
        public void addSQL(DBSQLBuilder sql, long context)
        { // append UNQUALIFIED name only!
            sql.append(expr.getName());
        }

        /**
         * Returns the data type of the DBColumnExpr object.
         * 
         * @return the data type
         */
        @Override
        public DataType getDataType()
        {
            return expr.getDataType();
        }

        /**
         * Not applicable - always returns 0.
         */
        @Override
        public double getSize()
        {
            return 0;
        }

        /**
         * Not applicable - always returns true
         */
        @Override
        public boolean isReadOnly()
        {
            return true; // expr.isReadOnly();
        }

        /**
         * Not applicable - always returns false
         */
        @Override
        public boolean isAutoGenerated()
        {
            return false; // expr.isReadOnly();
        }

        /**
         * Checks whether the column is mandatory.
         * 
         * @return true if the column is mandatory or false otherwise
         */
        @Override
        public boolean isRequired()
        {
            return false;
        }

        /**
         * Get Attributes of underlying table column.
         */
        @Override
        public Object getAttribute(String name)
        {
            if (attributes != null && attributes.indexOf(name)>=0)
                return attributes.get(name);
            // Otherwise ask expression
            return expr.getAttribute(name);
        }

        /**
         * Get Options of underlying table column.
         */
        @Override
        public Options getOptions()
        {
            if (options != null)
                return options;
            // Otherwise ask expression
            return expr.getOptions();
        }

        /**
         * Not applicable - always return the value.
         */
        @Override
        public Object validateValue(Object value)
        {   // Nothing to check.
            return value;
        }

        /**
         * Adds the expression definition to the xml element.
         */
        @Override
        public Element addXml(Element parent, long flags)
        {
            return expr.addXml(parent, flags);
        }

    }

    // Members
    private final DBMSHandler     dbms;
    protected DBCmdQuery          cmdQuery = null;
    protected List<DBOrderByExpr> orderBy  = null;

    /**
     * Constructs an empty DBCommandExpr object 
     * @param dbms the database handler
     */
    public DBCommandExpr(DBMSHandler dbms)
    {
        // Default Constructor
        this.dbms = dbms;
    }
    
    /**
     * returns the DBMSHandler this command belongs to
     * @return the DBMSHandler
     */
    public final DBMSHandler getDbms()
    {
        return dbms;
    }

    /**
     * Creates a clone of this class.
     */
    @Override
    public DBCommandExpr clone()
    {
        try 
        {   DBCommandExpr clone = (DBCommandExpr)super.clone();
            // Clone lists
            if (orderBy!=null)
                clone.orderBy = new ArrayList<DBOrderByExpr>(orderBy);
            // done
            return clone;
        } catch (CloneNotSupportedException e) {
            log.error("Cloning DBCommand object failed!", e);
            throw new InternalException(e); 
        }
    }
    
    public abstract boolean isValid();
    
    /**
     * returns whether or not the command has any select expression 
     * @return true if the command has any select expression of false otherwise
     */
    public abstract boolean hasSelectExpr();

    /**
     * returns whether or not the command has a specific select expression
     * @param expr the column expr 
     * @return true if the command contains the given select expression of false otherwise
     */
    public abstract boolean hasSelectExpr(DBColumnExpr expr);

    /**
     * Returns the list of all select expressions as an array
     * Used internally only
     * @return the select expression array
     */
    public abstract DBColumnExpr[] getSelectExprList();
    
    /**
     * returns a list of expressions for the SELECT part of the sql statement
     * @return the select expression list
     */
    public abstract List<DBColumnExpr> getSelectExpressions();

    /**
     * returns an SQL select command
     * @param sql the sql builder to add the command to
     */
    public abstract void getSelect(DBSQLBuilder sql);

    /**
     * returns an SQL select command for querying records.
     * @return the SQL-Command
     */
    public final String getSelect()
    {
        DBSQLBuilder sql = createSQLBuilder(null);
        getSelect(sql);
        return sql.toString();
    }
    
    public abstract DBCmdParams getParams();

    /**
     * returns an array holding all parameter values in the order of their occurrence.
     * To ensure the correct order, getSelect() must be called first.
     * @return an array of command parameter values 
     */
    public abstract Object[] getParamValues();

    /**
     * Returns the DataType selected by this command if only one column is returned
     * If the command has more than one select expression DataType.UNKNOWN will be returned
     * @return the DataType of the selected expression or DataType.UNKNOWN
     */
    public abstract DataType getDataType();
    
    /**
     * Returns a ColumnExpr for the result of the query
     * If the command must have exactly one select column otherwise a NotSupportedException is thrown;
     * @return the result expression
     */
    public DBColumnExpr result()
    {   try {
            return new DBCmdResultExpr(this);        
        } catch(InvalidArgumentException e) {
            throw new NotSupportedException(this, "result");
        }
    }
    
    /**
     * Creates the SQL-Command.
     * 
     * @param sql the SQL-Command
     * @param context the current SQL-Command context
     */
    @Override
    public void addSQL(DBSQLBuilder sql, long context)
    {
        // parenthesis open
        if ((context & CTX_NOPARENTHESIS)==0)
            sql.append("(");
        // append commaand
        sql.append(this);
        // parenthesis close
        if ((context & CTX_NOPARENTHESIS)==0)
            sql.append(")");
    }

    /**
     * Constructs a new DBCombinedCmd object with this object,
     * the key word= "UNION" and the selected DBCommandExpr.
     * 
     * @see org.apache.empire.db.DBCombinedCmd
     * @param other the second DBCommandExpr
     * @return the new DBCombinedCmd object
     */
    public DBCommandExpr union(DBCommandExpr other)
    {   // give dbms a chance to subclass DBCombinedCmd
    	return dbms.createCombinedCommand(this, "UNION", other);
    }

    /**
     * Constructs a new DBCombinedCmd object with this object,
     * the key word= "UNION ALL" and the selected DBCommandExpr.
     * 
     * @see org.apache.empire.db.DBCombinedCmd
     * @param other the second DBCommandExpr
     * @return the new DBCombinedCmd object
     */
    public DBCommandExpr unionAll(DBCommandExpr other)
    {   // give dbms a chance to subclass DBCombinedCmd
        return dbms.createCombinedCommand(this, "UNION ALL", other);
    }

    /**
     * Constructs a new DBCombinedCmd object with this object,
     * the key word= "INTERSECT" and the selected DBCommandExpr.
     * 
     * @param other the second DBCommandExpr
     * @return the new DBCombinedCmd object
     */
    public DBCommandExpr intersect(DBCommandExpr other)
    {   // give dbms a chance to subclass DBCombinedCmd
    	return dbms.createCombinedCommand(this, "INTERSECT", other);
    }
    
    /**
     * Returns whether or not the command has order by set
     * @return flag whether an order by is provided
     */
    public boolean hasOrderBy()
    {
        return (this.orderBy!=null ? !this.orderBy.isEmpty() : false);
    }

    /**
     * Returns a copy of the defined order-bys.
     * 
     * @return list of order-bys
     */
    public List<DBOrderByExpr> getOrderBy()
    {
        return (this.orderBy!=null ? Collections.unmodifiableList(this.orderBy) : null);
    }
    
    /**
     * Clears the list of order by expressions.
     */
    public void clearOrderBy()
    {
        orderBy = null;
    }

    /**
     * Adds an order by expression the command
     * @see org.apache.empire.db.DBCommandExpr#orderBy(DBColumnExpr, boolean)
     * 
     * @param exprs vararg of orderBy expressions
     * @return itself (this)
     */
    public DBCommandExpr orderBy(DBOrderByExpr... exprs)
    {
        if (orderBy == null)
            orderBy = new ArrayList<DBOrderByExpr>();
        // Add order by expression
        for (DBOrderByExpr expr : exprs)
        {   // find existing
            for (DBOrderByExpr ob : orderBy)
            {   // Compare expression
                if (ob.getColumn().equals(expr.getColumn()))
                {   // already there, replace
                    ob.setDescending(expr.isDescending());
                    expr = null;
                    break;
                }
            }
            // add, if not replaced
            if (expr!=null)
            {   // add expression
                orderBy.add(expr);
            }
        }
        return this;
    }

    /**
     * Adds a list of columns to the orderBy clause in ascending order
     * 
     * @param exprs vararg of column expressions
     * @return itself (this)
     */
    public DBCommandExpr orderBy(DBColumnExpr... exprs)
    {
        for (DBColumnExpr expr : exprs)
        {
            orderBy(new DBOrderByExpr(expr, false));
        }
        return this;
    }

    /**
     * Adds an order by with ascending or descending order
     * 
     * @param expr the DBColumnExpr object
     * @param desc if true, the results from select statement will sort top down
     * @return itself (this)
     */
    public DBCommandExpr orderBy(DBColumnExpr expr, boolean desc)
    {
        return orderBy(new DBOrderByExpr(expr, desc));
    }

    /**
     * set the maximum number of rows to return when executing a query command
     * A negative value will remove the limit.
     * 
     * @param numRows the number of rows to limit
     * @return itself (this)
     */
    public DBCommandExpr limitRows(int numRows)
    {
        throw new NotSupportedException(this, "limitRows");
    }

    /**
     * sets the offset of the first row to return when executing a query command.
     * A negative value will remove the offset.
     * 
     * @param numRows the number of rows to skip
     * @return itself (this)
     */
    public DBCommandExpr skipRows(int numRows)
    {
        throw new NotSupportedException(this, "skipRows");
    }
    
    /**
     * Clears a limit or offset set by calling limit() or offset()
     */
    public void clearLimit()
    {
        // Nothing to do!
    }

    /**
     * Create the insert into SQL-Command which copies
     * data from a select statement to a destination table.
     * 
     * @param table the table 
     * @param columns the columns
     * 
     * @return the insert into SQL-Command
     */
    public final String getInsertInto(DBTable table, List<DBColumnExpr> columns)
    {
        return getInsertInto(table, getSelectExprList(), columns);
    }

    /**
     * Create the insert into SQL-Command which copies
     * data from a select statement to a destination table.
     * 
     * @param table the table 
     * 
     * @return the insert into SQL-Command
     */
    public final String getInsertInto(DBTable table)
    {
        DBColumnExpr[] select = getSelectExprList();
        if (select == null || select.length < 1)
            throw new ObjectNotValidException(this);
        // Match Columns
        List<DBColumnExpr> inscols = new ArrayList<DBColumnExpr>(select.length);
        for (int i = 0; i < select.length; i++)
        {
            DBColumnExpr expr = select[i];
            DBColumn col = table.getColumn(expr.getName());
            if (col == null)
            { // Cannot find a match for that name
                log.warn("InsertInto: Column " + expr.getName() + " not found!");
                col = table.getColumn(i);
            }
            inscols.add(col);
        }
        return getInsertInto(table, select, inscols);
    }

    /**
     * creates a new DBSQLBuilder 
     * @param initalSQL
     * @return the new DBSQLBuilder
     */
    protected DBSQLBuilder createSQLBuilder(String initalSQL)
    {
        DBSQLBuilder sql = dbms.createSQLBuilder();
        if (initalSQL!=null)
            sql.append(initalSQL);
        return sql;
    }
    
    /**
     * wraps a column expression such that is specific for to this command and detached from its source.
     * @param col the column expression to wrap
     * @return column expression that is specific for to this command
     */
    protected DBColumnExpr getCmdColumn(DBColumnExpr col)
    {
        // Check the Instance of col
        if ((col instanceof DBCmdColumn))
        { // Check Owner
            DBCmdColumn c = (DBCmdColumn) col;
            if (c.getRowSet() == cmdQuery)
                return col; // it's already a command column
            // extract the expression
            col = c.expr;
        }
        // Check if we already have a Command Query
        if (cmdQuery == null)
            cmdQuery = new DBCmdQuery(this, col.getDatabase(), getSelectExprList());
        // create a command column
        return new DBCmdColumn(cmdQuery, col);
    }

    /**
     * Internally used to build a string from a list of database expressions
     * @param sql the sql target buffer
     * @param list the list of expressions to add to the sql
     * @param context the sql command context
     * @param separator string to use as separator between list items
     */
    protected void addListExpr(DBSQLBuilder sql, List<? extends DBExpr> list, long context, String separator)
    {
        for (int i = 0; i < list.size(); i++)
        {   // assemble select columns
            if (i > 0)
                sql.append(separator);
            // append
            list.get(i).addSQL(sql, context);
        }
    }
    
    /**
     * Create the insert into SQL-Command which copies data
     * from a select statement to a destination table.
     * 
     * @param table the table to insert 
     * @param select the selected columns
     * @param columns the list of column expressions
     * @return the insert into SQL-Command
     */
    protected String getInsertInto(DBTable table, DBColumnExpr[] select, List<DBColumnExpr> columns)
    {
        if (select == null)
            throw new ObjectNotValidException(this);
        // prepare buffer
        DBSQLBuilder sql = createSQLBuilder("INSERT INTO ");
        table.addSQL(sql, CTX_FULLNAME);
        // destination columns
        if (columns != null && columns.size() > 0)
        { // Check Count
            if (columns.size() != select.length)
            {
                throw new InvalidArgumentException("columns", "size()!=select.length");
            }
            // Append Names
            sql.append(" (");
            addListExpr(sql, columns, CTX_NAME, ", ");
            sql.append(")");
        }
        // append select statement
        sql.append("\r\n");
        getSelect(sql);
        // done
        return sql.toString();
    }
    
}
