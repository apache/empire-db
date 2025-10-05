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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.commons.StringUtils;
import org.apache.empire.data.DataType;
import org.apache.empire.data.RecordData;
import org.apache.empire.db.exceptions.NoPrimaryKeyException;
import org.apache.empire.db.expr.column.DBAliasExpr;
import org.apache.empire.db.expr.column.DBValueExpr;
import org.apache.empire.db.expr.compare.DBCompareAndOrExpr;
import org.apache.empire.db.expr.compare.DBCompareColExpr;
import org.apache.empire.db.expr.compare.DBCompareExpr;
import org.apache.empire.db.expr.compare.DBCompareNotExpr;
import org.apache.empire.db.expr.join.DBColumnJoinExpr;
import org.apache.empire.db.expr.join.DBCompareJoinExpr;
import org.apache.empire.db.expr.join.DBCrossJoinExpr;
import org.apache.empire.db.expr.join.DBJoinExpr;
import org.apache.empire.db.expr.order.DBOrderByExpr;
import org.apache.empire.db.expr.set.DBSetExpr;
import org.apache.empire.dbms.DBMSHandler;
import org.apache.empire.dbms.DBSqlPhrase;
import org.apache.empire.exceptions.InvalidArgumentException;
import org.apache.empire.exceptions.ItemNotFoundException;
import org.apache.empire.exceptions.NotSupportedException;
import org.apache.empire.exceptions.ObjectNotValidException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This abstract class handles the creation of the SQL-Commands. 
 * There are methods to create SQL-Commands, like update, insert,
 * delete and select.
 */
public abstract class DBCommand extends DBCommandExpr
    implements Cloneable
{
    // *Deprecated* private static final long serialVersionUID = 1L;
    
    // Logger
    protected static final Logger log             = LoggerFactory.getLogger(DBCommand.class);

    @Override
    protected DBSQLBuilder createSQLBuilder(String initalSQL)
    {
        DBSQLBuilder sql = super.createSQLBuilder(initalSQL);
        sql.setCmdParams(this.cmdParams);
        return sql;
    }

    // Distinct Select
    protected boolean              selectDistinct  = false;
    // Lists
    protected List<DBColumnExpr>   select          = null;
    protected List<DBSetExpr>      set             = null;
    protected List<DBJoinExpr>     joins           = null;
    protected List<DBCompareExpr>  where           = null;
    protected List<DBCompareExpr>  having          = null;
    protected List<DBColumnExpr>   groupBy         = null;
    
    protected Set<DBRowSet>        parentTables    = null; // omit parent tables in subqueries

    // Parameters for prepared Statements generation
    protected boolean              autoPrepareStmt = false;
    protected DBCmdParamList       cmdParams;

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
    {
        String dbid = String.valueOf(strm.readObject());
        // find database
        DBDatabase dbo = DBDatabase.findByIdentifier(dbid);
        if (dbo==null)
            throw new ItemNotFoundException(dbid);
        // set final field
        ClassUtils.setPrivateFieldValue(DBCommand.class, this, "db", dbo);
        // read the rest
        strm.defaultReadObject();
    }
     */
    
    /**
     * Constructs a new DBCommand object and set the specified DBDatabase object.
     * 
     * @param dbms the database handler
     * @param autoPrepareStmt flag whether to automatically use literal values as prepared statement params
     * @param cmdParams the command params list
     */
    protected DBCommand(DBMSHandler dbms, boolean autoPrepareStmt, DBCmdParamList cmdParams)
    {
        super(dbms);
        this.autoPrepareStmt = autoPrepareStmt;
        this.cmdParams = cmdParams;
    }

    /**
     * Constructs a new DBCommand object and set the specified DBDatabase object.
     * 
     * @param dbms the database handler
     * @param autoPrepareStmt flag whether to automatically use literal values as prepared statement params
     */
    protected DBCommand(DBMSHandler dbms, boolean autoPrepareStmt)
    {
        this(dbms, autoPrepareStmt, new DBCmdParamList());
    }
    
    /**
     * @return true if auto Prepared Statements is activated for this record
     */
    public final boolean isAutoPrepareStmt()
    {
        return autoPrepareStmt;
    }
    
    /**
     * Creates a clone of this class.
     */
    @Override
    public DBCommand clone()
    {
        DBCommand clone = (DBCommand)super.clone();
        // Clone lists
        if (select!=null)
            clone.select = new ArrayList<DBColumnExpr>(select);
        if (set!=null)
            clone.set = new ArrayList<DBSetExpr>(set);
        if (joins!=null)
            clone.joins = new ArrayList<DBJoinExpr>(joins);
        if (where!=null)
            clone.where = new ArrayList<DBCompareExpr>(where);
        if (groupBy!=null)
            clone.groupBy = new ArrayList<DBColumnExpr>(groupBy);
        if (having!=null)
            clone.having = new ArrayList<DBCompareExpr>(having);
        // clone params
        clone.cmdParams = new DBCmdParamList(cmdParams.size());
        if (!cmdParams.isEmpty())
        {   // clone set
            for (int i=0; (clone.set!=null && i<clone.set.size()); i++)
                clone.set.set(i, clone.set.get(i).copy(clone));
            // clone joins
            for (int i=0; (clone.joins!=null && i<clone.joins.size()); i++)
                clone.joins.set(i, clone.joins.get(i).copy(clone));
            // clone where and having
            for (int i=0; (clone.where!=null && i<clone.where.size()); i++)
                clone.where.set(i, clone.where.get(i).copy(clone));
            for (int i=0; (clone.having!=null && i<clone.having.size()); i++)
                clone.having.set(i, clone.having.get(i).copy(clone));
        }
        // check params
        if (clone.cmdParams.size()!=this.cmdParams.size())
        {   // Failed to copy all DBCmdParams
            log.error("DBCommand.clone failed: Not all DBCmdParams could be replaced.");
            throw new NotSupportedException(this, "clone");
        }
        // done
        return clone;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public final DBDatabase getDatabase()
    {
        if (hasSelectExpr())
            return this.select.get(0).getDatabase();
        if (hasSetExpr())
            return this.set.get(0).getDatabase();
        // two more chances (should we?)
        if (notEmpty(where))
            return where.get(0).getDatabase();
        if (notEmpty(orderBy))
            return orderBy.get(0).getDatabase();
        // not valid yet
        throw new ObjectNotValidException(this);
    }

    @Override
    public DBCmdParams getParams()
    {
        return cmdParams;
    }
    
    /**
     * internally used to reset the command param usage count.
     * Note: Only one thread my generate an SQL statement 
     */
    protected void resetParamUsage()
    {
        cmdParams.resetParamUsage(this);
    }
    
    /**
     * internally used to remove unused Command Params from list
     * Note: Only one thread my generate an SQL statement 
     */
    protected void completeParamUsage()
    {
        cmdParams.completeParamUsage(this);
    }
    
    /**
     * internally used to remove the command param used in a constraint
     * @param cmpExpr the compare expression 
     */
   	protected void removeCommandParams(DBCompareExpr cmpExpr) 
   	{
   	    if (cmdParams.isEmpty())
   	        return; // Nothing to do
   	    // check type
   	    if (cmpExpr instanceof DBCompareColExpr)
   	    {   // DBCompareColExpr
   	        DBCompareColExpr cmp = ((DBCompareColExpr)cmpExpr);
            if (cmp.getValue() instanceof DBCmdParam) {
                // remove param
                DBCmdParam param = (DBCmdParam)cmp.getValue();
                cmp.setValue(param.getValue());
                cmdParams.remove(param);
            }
   	    }
        else if (cmpExpr instanceof DBCompareAndOrExpr) 
        {   // DBCompareAndOrExpr
            removeCommandParams(((DBCompareAndOrExpr)cmpExpr).getLeft());
            removeCommandParams(((DBCompareAndOrExpr)cmpExpr).getRight());
        }
        else if (cmpExpr instanceof DBCompareNotExpr) 
        {   // DBCompareNotExpr
            removeCommandParams(((DBCompareNotExpr)cmpExpr).getExpr());
        }
        else if (ObjectUtils.isWrapper(cmpExpr))
        {   // unwrap
            removeCommandParams(ObjectUtils.unwrap(cmpExpr));
        }
   	}

    /**
     * internally used to remove all command params used in a list of constraints
     * @param list the list of compare expressions
     */
   	protected void removeAllCommandParams(List<DBCompareExpr> list)
    {
        if (list==null)
        	return;
        for(DBCompareExpr cmp : list)
        {   // Check the value is a DBCommandParam
        	removeCommandParams(cmp);
        }
    }
    
    /**
     * Set parent tables for subquery command generation.
     * Parent tables will be omitted to the FROM clause.
     * @param rowSets the parent rowsets
     */
    public void setParentTables(DBRowSet... rowSets)
    {
        if (rowSets.length>0)
        {   // add all rowsets
            this.parentTables = new HashSet<DBRowSet>(rowSets.length);
            for (DBRowSet r : rowSets)
                this.parentTables.add(r);
        }
        else this.parentTables = null;
    }

    /**
     * Returns true if the this command has either Select or Set expressions
     */
    @Override
    public boolean isValid()
    {
    	return hasSelectExpr() || hasSetExpr();
    }

    /**
     * Sets whether or not the select statement should contain
     * the distinct directive .
     * @return itself (this)
     */
    public DBCommand selectDistinct()
    {
    	this.selectDistinct = true;
    	return this;
    }

    /**
     * Returns whether or not the select statement will be distinct or not.
     *  
     * @return true if the select will contain the distinct directive or false otherwise.
     */
    public boolean isSelectDistinct()
    {
    	return selectDistinct;
    }
    
    /**
     * returns whether or not the command has any select expression 
     * @return true if the command has any select expression of false otherwise
     */
    @Override
    public boolean hasSelectExpr()
    {
        return notEmpty(select);
    }

    /**
     * returns whether or not the command has a specific select expression 
     * @return true if the command contains the given select expression of false otherwise
     */
    @Override
    public boolean hasSelectExpr(DBColumnExpr expr)
    {
        return (select!=null ? (select.indexOf(expr)>=0) : false);
    }

    /**
    * @return the DataType of the selected expression or DataType.UNKNOWN
    */
    @Override
    public DataType getDataType()
    {
        if (select==null || select.size()!=1)
            return DataType.UNKNOWN;
        return select.get(0).getDataType(); 
    }
    
    /**
     * Adds a DBColumnExpr object to the Select collection
     * 
     * @param expr the DBColumnExpr object
     * @return itself (this)
     */
    public DBCommand select(DBColumnExpr expr)
    {
        if (expr==null)
            return this; // ignore null
        // Select this column
        if (select == null)
            select = new ArrayList<DBColumnExpr>();
        // find and replace
        int index = select.indexOf(expr);
        if (index>=0)
            select.set(index, expr); // replace (added 2024-07-18 EMPIREDB-434)
        else
            select.add(expr);
        // done
        return this;
    }

    /**
     * Adds a list of columns to the select phrase of an sql statement.
     * 
     * @param exprs an vararg of DBColumnExpr's to select
     * @return itself (this)
     */
    public final DBCommand select(DBColumnExpr... exprs)
    {
        for (DBColumnExpr expr : exprs)
        {
            select(expr);
        }
        return this;
    }

    /**
     * Adds a collection of columns to the select phrase of an sql statement.
     * 
     * @param columns the column expressions to add
     * @return itself (this)
     */
    public final DBCommand select(Collection<? extends DBColumnExpr> columns)
    {
        for (DBColumnExpr expr : columns)
        {
            select(expr);
        }
        return this;
    }

    /**
     * Selects all set expressions
     * i.e. converts all calls like  
     *      cmd.set(COL.to(VALUE))
     * into a select of the form
     *      cmd.select(VALUE.as(COL))  
     * @return itself (this)
     */
    public final DBCommand selectSetExpressions(List<DBSetExpr> setExprList)
    {
        // Check null or empty
        if (setExprList==null || setExprList.isEmpty())
            return this;
        // convert set to select
        for (DBSetExpr se : setExprList)
        {
            DBColumnExpr VAL = (DBColumnExpr)se.getValue();
            if (VAL==null)
                VAL= se.getDatabase().getValueExpr(null, DataType.UNKNOWN);
            select(VAL.as(se.getColumn()));
        }
        return this;
    }

    /**
     * Selects all set expressions that have been set for this command
     * @return itself (this)
     */
    public final DBCommand selectSetExpressions()
    {
        return selectSetExpressions(this.set);  
    }
    
    /**
     * Adds a list of columns with their qualified name to the select phrase of an sql statement.
     * 
     * @param columns one or more columns to select
     * @return itself (this)
     */
    public DBCommand selectQualified(DBColumnExpr... columns)
    {
        for (DBColumnExpr col : columns)
        {
            select(col.qualified());
        }
        return this;
    }

    /**
     * Adds a collection of columns to the select phrase of an sql statement.
     * 
     * @param columns the column expressions to add
     * @return itself (this)
     */
    public final DBCommand selectQualified(Collection<? extends DBColumnExpr> columns)
    {
        for (DBColumnExpr col : columns)
        {
            select(col.qualified());
        }
        return this;
    }

    /**
     * Makes sure all selected columns are identified by their proper names (qualified)
     * @return itself (this)
     */
    public DBCommand qualifyAll()
    {
        if (select == null)
            return this;
        // check select expression
        for (int i=0; i<select.size(); i++)
        {
            DBColumnExpr expr = select.get(i);
            if (expr instanceof DBColumn)
                continue; // No need to qualify
            if (expr instanceof DBAliasExpr)
                continue; // Already qualified
            // qualify now
            select.set(i, expr.qualified());
        }
        return this;
    }
    
    /**
     * Returns an array of all select expressions
     * 
     * @return an array of all DBColumnExpr objects or <code>null</code> if there is nothing to select
     */
    @Override
    public DBColumnExpr[] getSelectExprList()
    {
        int count = (select != null) ? select.size() : 0;
        if (count < 1)
            return null;
        // The List
        DBColumnExpr[] exprList = new DBColumnExpr[count];
        for (int i = 0; i < count; i++)
            exprList[i] = select.get(i);
        // The expression List
        return exprList;
    }

    /**
     * Returns all select expressions as unmodifiable list
     * @return the list of DBColumnExpr used for select
     */
    @Override
    public List<DBColumnExpr> getSelectExpressions()
    {
        return (this.select!=null ? Collections.unmodifiableList(this.select) : null);
    }

    /**
     * replaces a select expression with another or removes a select expression
     * In order to remove the expression, set the replWith parameter to null
     * If the replace expression is not found, an ItemNotFoundException is thrown 
     * @param replExpr the expression to replace
     * @param replWith the expression to replace with
     */
    public void replaceSelect(DBColumnExpr replExpr, DBColumnExpr replWith)
    {
        int idx = (select != null ? select.indexOf(replExpr) : -1);
        if (idx < 0)
            throw new ItemNotFoundException(replExpr);
        // replace now
        if (replWith!=null)
            select.set(idx, replWith);
        else
            select.remove(idx);
    }

    /**
     * removes one or more expressions from the Select expression list
     * @param exprs the expression(s) to be removed
     */
    public void removeSelect(DBColumnExpr... exprs)
    {
        if (select==null)
            return;
        for (int i=0; i<exprs.length; i++)
        {
            int idx = select.indexOf(exprs[i]);
            if (idx>=0)
                select.remove(idx);
        }
    }
    
    /**
     * Checks whether or not there are any aggregate functions in the Select
     * @return true if at least on of the selected expressions is an aggregate
     */
    public boolean hasAggegation() 
    {
        for (DBColumnExpr expr : this.select)
        {
            if (expr.isAggregate())
                return true;
        }
        return false;
    }
    
    /**
     * Adds a single set expressions to this command
     * Use column.to(...) to create a set expression 
     * 
     * @param expr the DBSetExpr object(s)
     * @return itself (this)
     */
    public DBCommand set(DBSetExpr expr)
    {
        // add to list
        if (set == null)
            set = new ArrayList<DBSetExpr>();
        for (int i = 0; i < set.size(); i++)
        {
            DBSetExpr chk = set.get(i);
            if (chk.column.equals(expr.column))
            {   // Overwrite existing value
                if (useCmdParam(expr.column, expr.value))
                {   // Use parameter value
                    if (chk.value instanceof DBCmdParam)
                    {   // reuse the old paramter
                        ((DBCmdParam)chk.value).setValue(expr.value);
                        expr.value = chk.value;
                        chk.value = null;
                    }
                    else
                    {   // create new one
                        expr.value = addParam(expr.column.getDataType(), expr.value);
                    }
                } 
                else
                {   // remove from parameter list (if necessary)
                    if (chk.value instanceof DBCmdParam)
                        cmdParams.remove((DBCmdParam)chk.value);
                }
                // replace now
                set.set(i, expr);
                return this;
            }
        }
        // Replace with parameter 
        if (useCmdParam(expr.column, expr.value))
            expr.value = addParam(expr.column.getDataType(), expr.value);
        // new Value!
        set.add(expr);
        return this;
    }
    
    /**
     * Adds a list of set expressions to this command
     * Use column.to(...) to create a set expression
     *  
     * @param exprs the DBSetExpr object(s)
     * @return itself (this)
     */
    public final DBCommand set(DBSetExpr... exprs)
    {
        for (int i=0; i<exprs.length; i++)
            set(exprs[i]);
        return this;
    }
    
    /**
     * Returns whether or not the command has group by set
     * @return true if at least one set expression is present
     */
    public boolean hasSetExpr()
    {
        return notEmpty(set);
    }

    /**
     * Checks whether a column is in the list of set expressions
     * @param column the column to check
     * @return <code>true</code> if there is a set expression 
     */
    protected boolean hasSetExprOn(DBColumn column)
    {
        if (set==null)
            return false;
        for (DBSetExpr setExpr : set)
        {   // Find column
            if (setExpr.column.equals(column))
                return true;
            
        }
        return false;
    }

    /**
     * Returns all set expressions as unmodifiable list
     * @return the list of DBSetExpr used for set
     */
    public List<DBSetExpr> getSetExpressions()
    {
        return (this.set!=null ? Collections.unmodifiableList(this.set) : null);
    }

    /**
     * Adds an command parameter which will be used in a prepared statement.
     * The command parameter returned may be used to alter the value.
     * 
     * @param type the data type of the parameter
     * @param value the initial parameter value 
     * 
     * @return the command parameter object 
     */
    public DBCmdParam addParam(DataType type, Object value)
    {
        // Create and add the parameter to the parameter list 
        DBCmdParam param = new DBCmdParam(this, type, value);
        cmdParams.add(param);
        // done
        return param;
    }

    /**
     * Adds an command parameter which will be used in a prepared statement.
     * The initial value of the command parameter is null but can be modified using the setValue method.
     *  
     * @param colExpr the column expression for which to create the parameter
     * @param value the initial parameter value 
     * 
     * @return the command parameter object 
     */
    public final DBCmdParam addParam(DBColumnExpr colExpr, Object value)
    {
        return addParam(colExpr.getDataType(), value);
    }

    /**
     * Adds an command parameter which will be used in a prepared statement.
     * The initial value of the command parameter is null but can be modified using the setValue method.
     * @param value the initial value of the added param
     * @return the command parameter object
     */
    public final DBCmdParam addParam(Object value)
    {
        if (value instanceof DataType)
            return addParam((DataType)value, null);
        else if (value!=null)
            return addParam(DataType.fromJavaType(value.getClass()), value);
        else
            return addParam(DataType.UNKNOWN, null);
    }

    /**
     * Adds an command parameter which will be used in a prepared statement.
     * The initial value of the command parameter is null but can be modified using the setValue method.
     *  
     * @return the command parameter object
     */
    public final DBCmdParam addParam()
    {
        return addParam(DataType.UNKNOWN, null);
    }

    /**
     * Adds a join to the list of join expressions.
     * 
     * @param join the join expression
     * @return itself (this)
     */
    public DBCommand join(DBJoinExpr join)
    {
        // Rowsets must be different
        if (join.getLeftTable().equals(join.getRightTable()))
            throw new ObjectNotValidException(join, "The rowsets of a join expression must not be the same!");     
        // create list
        if (joins == null)
            joins = new ArrayList<DBJoinExpr>();
        // check join list
        for (int i = 0; i < joins.size(); i++)
        { // Check whether join already exists
            DBJoinExpr item = joins.get(i);
            if (item.equals(join))
                return this;
        }
        joins.add(join);
        // Check if prepared statements are enabled
        if (isPreparedStatementsEnabled())
        {   // use command params
            join.prepareParams(this, null);
        }
        return this;
    }

    /**
     * Adds a join to the list of join expressions.
     * 
     * @param join the join expression
     * @param joinType the type of join
     * @return itself (this) 
     */
    public final DBCommand join(DBJoinExpr join, DBJoinType joinType)
    {
        join.setType(joinType);
        return join(join);
    }

    /**
     * Adds a left join to the list of join expressions.
     * @param join the join expression
     * @return itself (this) 
     */
    public final DBCommand joinLeft(DBJoinExpr join)
    {
        return join(join, DBJoinType.LEFT);
    }

    /**
     * Adds a left join to the list of join expressions.
     * @param join the join expression
     * @return itself (this) 
     */
    public final DBCommand joinRight(DBJoinExpr join)
    {
        return join(join, DBJoinType.RIGHT);
    }

    /**
     * Adds an inner join based on two columns to the list of join expressions.
     * 
     * New in release 3.1: Use join(left.on(right).and(addlConstraint)) instead
     * 
     * @param left the left join value
     * @param right the right join
     * @param addlConstraints additional compare expressions
     * @return itself (this) 
     */
    public final DBCommand join(DBColumnExpr left, DBColumn right, DBCompareExpr... addlConstraints)
    {
        return join(left, right, DBJoinType.INNER, addlConstraints);
    }

    /**
     * Adds a left join based on two columns to the list of join expressions.
     * 
     * New in release 3.1: Use joinLeft(left.on(right).and(addlConstraint)) instead
     * 
     * @param left the left join value
     * @param right the right join
     * @param addlConstraints additional compare expressions
     * @return itself (this) 
     */
    public final DBCommand joinLeft(DBColumnExpr left, DBColumn right, DBCompareExpr... addlConstraints)
    {
        return join(left, right, DBJoinType.LEFT, addlConstraints);
    }

    /**
     * Adds a right join based on two columns to the list of join expressions.
     * 
     * New in release 3.1: Use joinRight(left.on(right).and(addlConstraint)) instead
     * 
     * @param left the left join value
     * @param right the right join
     * @param addlConstraints additional compare expressions
     * @return itself (this) 
     */
    public final DBCommand joinRight(DBColumnExpr left, DBColumn right, DBCompareExpr... addlConstraints)
    {
        return join(left, right, DBJoinType.RIGHT, addlConstraints);
    }

    /**
     * Adds a join based on two columns to the list of join expressions.
     * 
     * Migration hint from 2.x: replace ").where(" with just "," 
     * 
     * @param left the left join value
     * @param right the right join
     * @param joinType type of join ({@link DBJoinType#INNER}, {@link DBJoinType#LEFT}, {@link DBJoinType#RIGHT})
     * @param addlConstraints additional compare expressions
     * @return itself (this) 
     */
    public final DBCommand join(DBColumnExpr left, DBColumn right, DBJoinType joinType, DBCompareExpr... addlConstraints)
    {
        if (left==null || right==null || left.getRowSet()==null)
            throw new InvalidArgumentException("left|right", left);
        if (left.getRowSet()==right.getRowSet())
            throw new InvalidArgumentException("rowset", left.getRowSet().getName()+"|"+right.getRowSet().getName());
        // additional constraints
        DBCompareExpr where = null;
        for (int i=0; i<addlConstraints.length; i++)
        {
            DBCompareExpr cmpExpr = addlConstraints[i];
            if (cmpExpr==null)
                continue;
            // Chain with previouss
            where = (where!=null ? where.and(cmpExpr) : cmpExpr);
        }
        // create the expression
        DBColumnJoinExpr join = new DBColumnJoinExpr(left, right, joinType, where);
        join(join);
        return this;
    }

    /**
     * Multi-Column version of column based join expression
     * @param left the columsn on the left
     * @param right the columns on the right
     * @param joinType the joinType
     * @param addlConstraints additional compare expressions
     * @return itself (this) 
     */
    public final DBCommand join(DBColumn[] left, DBColumn[] right, DBJoinType joinType, DBCompareExpr... addlConstraints)
    {
        // check params
        if (left==null || right==null || left.length==0 || left.length!=right.length)
            throw new InvalidArgumentException("left|right", left);
        if (left[0].getRowSet()==right[0].getRowSet())
            throw new InvalidArgumentException("rowset", left[0].getRowSet().getName()+"|"+right[0].getRowSet().getName());
        /*
         * TODO: Find a better solution / Make DBColumnJoinExpr multi-column
         */
        // compare the columns except the first
        DBCompareExpr where = null;
        for (int i=1; i<left.length; i++)
        {   // add to where list
            DBCompareExpr cmpExpr = right[i].is(left[i]);
            where = (where!=null ? where.and(cmpExpr) : cmpExpr);
        }
        // additional constraints
        for (int i=0; i<addlConstraints.length; i++)
        {
            DBCompareExpr cmpExpr = addlConstraints[i];
            where = (where!=null ? where.and(cmpExpr) : cmpExpr);
        }
        // create the expression
        DBColumnJoinExpr join = new DBColumnJoinExpr(left[0], right[0], joinType, where);
        join(join);
        return this;
    }
    
    /**
     * Adds a cross join for two tables or views 
     * 
     * New in release 3.1: Use left.on(right)) instead
     * 
     * @param left the left RowSet
     * @param right the right RowSet
     * @return itself (this) 
     */
    public final DBCommand join(DBRowSet left, DBRowSet right)
    {
        DBCrossJoinExpr join = new DBCrossJoinExpr(left, right);
        join(join);
        // done
        return this;
    }

    /**
     * Adds a join based on a compare expression to the command.
     * 
     * New in release 3.1: Use joinLeft(rowset.on(cmp)) instead
     * 
     * @param rowset table or view to join
     * @param cmp the compare expression with which to join the table
     * @param joinType type of join ({@link DBJoinType#INNER}, {@link DBJoinType#LEFT}, {@link DBJoinType#RIGHT})
     * @return itself (this) 
     */
    public final DBCommand join(DBRowSet rowset, DBCompareExpr cmp, DBJoinType joinType)
    {
        DBCompareJoinExpr join = new DBCompareJoinExpr(rowset, cmp, joinType); 
        join(join);
        return this;
    }

    /**
     * Adds an inner join based on a compare expression to the command.
     * 
     * New in release 3.1: Use rowset.on(cmp) instead
     * 
     * @param rowset table of view which to join
     * @param cmp the compare expression with wich to join the table
     * @return itself (this) 
     */
    public final DBCommand join(DBRowSet rowset, DBCompareExpr cmp)
    {
        return join(rowset, cmp, DBJoinType.INNER);
    }

    /**
     * Adds a list of join expressions to the command.
     * 
     * @param joinExprList list of join expressions
     */
    public void addJoins(List<DBJoinExpr> joinExprList)
    {
        if (joins == null)
        {
            joins = new ArrayList<DBJoinExpr>();
        }
        this.joins.addAll(joinExprList);
    }
    
    /**
     * Returns a copy of the defined joins.
     * 
     * @return the list of joins
     */
    public List<DBJoinExpr> getJoins()
    {
        return (this.joins!=null ? Collections.unmodifiableList(this.joins) : null);
    }
    
    /**
     * Returns true if the command has a join on the given table or false otherwise.
     * 
     * @param rowset rowset table or view to join
     * 
     * @return true if the command has a join on the given table or false otherwise
     */
    public boolean hasJoinOn(DBRowSet rowset)
    {
        if (joins==null)
            return false;
        // Examine all joins
        for (DBJoinExpr join : joins)
        {
            if (join.isJoinOn(rowset))
                return true;
        }
        // not found
        return false;
    }
    
    /**
     * Returns true if the command has a join on the given column or false otherwise.
     * 
     * @param column the column to test
     * 
     * @return true if the command has a join on the given column or false otherwise
     */
    public boolean hasJoinOn(DBColumn column)
    {
        if (joins==null)
            return false;
        // Examine all joins
        for (DBJoinExpr join : joins)
        {
            if (join.isJoinOn(column))
                return true;
        }
        // not found
        return false;
    }
    
    /**
     * removes all joins to a given table or view
     * 
     * @param rowset the table or view for which to remove all joins
     * 
     * @return true if any joins have been removed or false otherwise
     */
    public boolean removeJoinsOn(DBRowSet rowset)
    {
        if (joins==null)
            return false;
        // Examine all joins
        int size = joins.size();
        for (int i=size-1; i>=0; i--)
        {
            if (joins.get(i).isJoinOn(rowset))
                joins.remove(i);
        }
        return (size!=joins.size());
    }
    
    /**
     * removes all joins to a given column
     * 
     * @param column the column for which to remove all joins
     * 
     * @return true if any joins have been removed or false otherwise
     */
    public boolean removeJoinsOn(DBColumn column)
    {
        if (joins==null)
            return false;
        // Examine all joins
        int size = joins.size();
        for (int i=size-1; i>=0; i--)
        {
            if (joins.get(i).isJoinOn(column))
                joins.remove(i);
        }
        return (size!=joins.size());
    }

    /**
     * Adds a constraint to the where phrase of the sql statement
     * If another restriction already exists for the same column it will be replaced.
     * 
     * @param expr the DBCompareExpr object
     * @return itself (this)
     */
    public DBCommand where(DBCompareExpr expr)
    {
        if (where == null)
            where = new ArrayList<DBCompareExpr>();
        setConstraint(where, expr);
        return this;
    }
    
    /**
     * Adds a list of constraints to the where phrase of the sql statement
     * If another restriction already exists for the same column it will be replaced.
     * 
     * @param exprs the DBCompareExpr object
     * @return itself (this)
     */
    public final DBCommand where(DBCompareExpr... exprs)
    {
        for (int i=0; i<exprs.length; i++)
            where(exprs[i]);
        return this;
    }

    /**
     * Returns true if the command has constraints or false if not.
     * 
     * @return true if constraints have been set on the command
     */
    public boolean hasWhereConstraints()
    {
        return (where!=null && where.size()>0);
    }

    /**
     * Returns a copy of the defined where clauses.
     * 
     * @return vector of where clauses
     */
    public List<DBCompareExpr> getWhereConstraints()
    {
        return (this.where!=null ? Collections.unmodifiableList(this.where) : null);
    }
    
    /**
     * removes a constraint on a particular column from the where clause
     * @param cmpExpr the compare expression which to remove
     * @return true if the constraint was removed
     */
    public boolean removeWhereConstraint(DBCompareExpr cmpExpr)
    {
        return removeConstraint(where, cmpExpr);
    }
    
    /**
     * removes a constraint on a particular column from the where clause
     * @param col the column expression for which to remove the constraint
     * @return the constraint on the given column if present or null otherwise 
     */
    public DBCompareExpr removeWhereConstraintOn(DBColumnExpr col)
    {
        return removeConstraintOn(where, col);
    }
    
    /**
     * Checks whether the command has a constraint on a particular column expression
     * @param col the column expression which to check
     * @return true if a where constraint for the given column exists
     */
    public boolean hasWhereConstraintOn(DBColumnExpr col)
    {
        return (findConstraintOn(where, col)!=null);
    }

    /**
     * Adds a list of constraints to the command.
     * @param constraints list of constraints
     */
    public void addWhereConstraints(List<DBCompareExpr> constraints)
    {
        // allocate
        if (where == null)
            where = new ArrayList<DBCompareExpr>();
        // add
        this.where.addAll(constraints);
    }
    
    /**
     * Adds key constraints the command
     * @param rowset the rowset for which to add constraints
     * @param data the record data from which to take the key values
     */
    public void addKeyConstraints(DBRowSet rowset, RecordData data)
    {
        DBColumn[] keyColumns = rowset.getKeyColumns();
        if (keyColumns==null || keyColumns.length==0)
            throw new NoPrimaryKeyException(rowset);
        // Collect key
        for (int i=0; i<keyColumns.length; i++)
            where(keyColumns[i].is(data.get(keyColumns[i])));
    }

    /**
     * adds a constraint to the having clause.
     * @param expr the DBCompareExpr object
     * @return itself (this)
     */
    public DBCommand having(DBCompareExpr expr)
    {
        if (having == null)
            having = new ArrayList<DBCompareExpr>();
        setConstraint(having, expr);
        return this;
    }

    /**
     * Returns true if the command has having-constraints or false if not.
     * 
     * @return true if constraints have been set on the command
     */
    public boolean hasHavingConstraints()
    {
        return (having!=null && having.size()>0);
    }

    /**
     * Returns a copy of the defined having clauses.
     * 
     * @return list of having constraints
     */
    public List<DBCompareExpr> getHavingConstraints()
    {
        return (this.having!=null ? Collections.unmodifiableList(this.having) : null);
    }
    
    /**
     * removes a constraint on a particular column from the where clause
     * @param cmpExpr the compare expression which to remove
     * @return true if the constraint was removed
     */
    public boolean removeHavingConstraint(DBCompareExpr cmpExpr)
    {
        return removeConstraint(having, cmpExpr);
    }
    
    /**
     * removes a constraint on a particular column from the having clause
     * @param col the column expression for which to remove the constraint
     * @return the constraint on the given column if present or null otherwise 
     */
    public DBCompareExpr removeHavingConstraintOn(DBColumnExpr col)
    {
        return removeConstraintOn(having, col);
    }
    
    /**
     * Checks whether the command has a constraint on a particular column expression
     * @param col the column expression which to check
     * @return true if a having constraint for the given column exists
     */
    public boolean hasHavingConstraintOn(DBColumnExpr col)
    {
        return (findConstraintOn(having, col)!=null);
    }
    
    /**
     * Returns true if the command has a constraint on the given table or false otherwise.
     * 
     * @param rowset rowset table or view to join
     * 
     * @return true if the command has a join on the given table or false otherwise
     */
    public boolean hasConstraintOn(DBRowSet rowset)
    {
        if (where==null && having==null)
            return false;
        // Examine all constraints
        int i = 0;
        Set<DBColumn> columns = new HashSet<DBColumn>();
        for (i = 0; where != null && i < where.size(); i++)
            ((DBExpr) where.get(i)).addReferencedColumns(columns);
        for (i = 0; having != null && i < having.size(); i++)
            ((DBExpr) having.get(i)).addReferencedColumns(columns);
        // now we have all columns
        Iterator<DBColumn> iterator = columns.iterator();
        while (iterator.hasNext())
        { // get the table
            DBColumn col = iterator.next();
            DBRowSet table = col.getRowSet();
            if (table.equals(rowset))
                return true;
        }
        // not found
        return false;
    }
    
    /**
     * Returns whether or not the command has group by set
     * @return true if a group by expression exists
     */
    public boolean hasGroupBy()
    {
        return notEmpty(groupBy);
    }

    /**
     * Returns a copy of the defined where clauses.
     * 
     * @return vector of where clauses
     */
    public List<DBColumnExpr> getGroupBy()
    {
        return (this.groupBy!=null ? Collections.unmodifiableList(this.groupBy) : null);
    }
    
    /**
     * Adds a column expression to the Group By clause of an sql statement.
     * 
     * @param columnExpr the column expression
     * @return itself (this)
     */
    public DBCommand groupBy(DBColumnExpr columnExpr)
    {
        if (groupBy == null)
            groupBy = new ArrayList<DBColumnExpr>();
        // Add all
        if (columnExpr.isAggregate())
            return this;
        // Unwrap DBAliasExpr only
        if (columnExpr instanceof DBAliasExpr)
            columnExpr = ((DBAliasExpr)columnExpr).unwrap();
        // Ignore Value expr (added 20241224)
        if (columnExpr instanceof DBValueExpr)
            return this;
        // Already present?
        if (groupBy.contains(columnExpr))
            return this;
        // add
        groupBy.add(columnExpr);
        // done
        return this;
    }
    
    /**
     * Adds a list of columns to the Group By clause of an sql statement.
     * 
     * @param exprs vararg of columns by which to group the rows
     * @return itself (this)
     */
    public final DBCommand groupBy(DBColumnExpr...exprs)
    {
        for(DBColumnExpr expr : exprs)
        {
            groupBy(expr);
        }
        return this;
    }

    /**
     * Adds a collection of columns to the Group By clause of an sql statement.
     * 
     * @param columns the column expressions to add
     * @return itself (this)
     */
    public final DBCommand groupBy(Collection<? extends DBColumnExpr> columns)
    {
        for (DBColumnExpr expr : columns)
        {
            groupBy(expr);
        }
        return this;
    }

    /**
     * Adds all select expressions which are not aggregates to the Group By clause
     * @return itself (this)
     */
    public final DBCommand groupAll()
    {
        clearGroupBy();
        // check select expression
        if (select == null)
            return this;
        // group all columns
        for (DBColumnExpr expr : select)
        {
            if (expr.isAggregate())
                continue; // ignore aggregates
            // append
            groupBy(expr);
        }
        return this;
    }

    /**
     * Clears the select distinct option.
     */
    public void clearSelectDistinct()
    {
        this.selectDistinct = false;
    }

    /**
     * Clears the list of selected columns.
     */
    public void clearSelect()
    {
        select = null;
    }

    /**
     * Clears the Set clause
     */
    public void clearSet()
    {
        if (set!=null && !cmdParams.isEmpty())
        {   // remove params
            for (DBSetExpr set : this.set)
            {   // remove all
                Object value = set.getValue();
                if (value instanceof DBCmdParam)
                    cmdParams.remove((DBCmdParam)value);
            }
        }
        set = null;
    }

    /**
     * Clears the From / Join clause
     */
    public void clearJoin()
    {
        joins = null;
    }

    /**
     * Removes all constraints from the Where clause
     */
    public void clearWhere()
    {
    	removeAllCommandParams(where);
        where = null;
    }

    /**
     * Removes all constraints from the Having clause
     */
    public void clearHaving()
    {
    	removeAllCommandParams(having);
        having = null;
    }

    /**
     * Clears the Group By clause
     */
    public void clearGroupBy()
    {
        groupBy = null;
    }

    /**
     * Overridden to change return type from DBCommandExpr to DBCommand
     * @see org.apache.empire.db.DBCommandExpr#orderBy(org.apache.empire.db.expr.order.DBOrderByExpr...)
     */
    @Override
    public DBCommand orderBy(DBOrderByExpr... exprs)
    {
        return (DBCommand)super.orderBy(exprs);
    }

    /**
     * Overridden to change return type from DBCommandExpr to DBCommand
     * @see org.apache.empire.db.DBCommandExpr#orderBy(org.apache.empire.db.DBColumnExpr...)
     */
    @Override
    public DBCommand orderBy(DBColumnExpr... exprs)
    {
        return (DBCommand)super.orderBy(exprs);
    }

    /**
     * Overridden to change return type from DBCommandExpr to DBCommand
     * @see org.apache.empire.db.DBCommandExpr#orderBy(org.apache.empire.db.DBColumnExpr, boolean)
     */
    @Override
    public DBCommand orderBy(DBColumnExpr expr, boolean desc)
    {
        return (DBCommand)super.orderBy(expr, desc);
    }

    /**
     * Overridden to change return type from DBCommandExpr to DBCommand
     * @see org.apache.empire.db.DBCommandExpr#orderByUpper(org.apache.empire.db.DBColumnExpr, boolean)
     */
    @Override
    public DBCommand orderByUpper(DBColumnExpr expr, boolean desc)
    {
        return (DBCommand)super.orderByUpper(expr, desc);
    }

    /**
     * Overridden to change return type from DBCommandExpr to DBCommand
     * @see org.apache.empire.db.DBCommandExpr#orderByUpper(org.apache.empire.db.DBColumnExpr...)
     */
    @Override
    public DBCommand orderByUpper(DBColumnExpr... expr)
    {
        return (DBCommand)super.orderByUpper(expr);
    }

    /**
     * Overridden to change return type from DBCommandExpr to DBCommand
     */
    @Override
    public DBCommand limitRows(int limitRows)
    {
        return (DBCommand)super.limitRows(limitRows);
    }

    /**
     * Overridden to change return type from DBCommandExpr to DBCommand
     */
    @Override
    public DBCommand skipRows(int skipRows)
    {
        return (DBCommand)super.skipRows(skipRows);
    }
    
    /**
     * Clears the entire command object.
     */
    public void clear()
    {
        cmdParams.clear(0);
        parentTables = null;
        clearSelectDistinct();
        clearSelect();
        clearSet();
        clearJoin();
        clearWhere();
        clearHaving();
        clearGroupBy();
        clearOrderBy();
        clearLimit();
        // cmdParams.resetParamUsage(this);
    }
    
    /**
     * returns true if prepared statements are enabled for this command
     * @return true if prepared statements are enabled for this command
     */
    protected boolean isPreparedStatementsEnabled()
    {
        return this.autoPrepareStmt;
    }
    
    /**
     * returns true if a cmdParam should be used for the given column or false otherwise
     * @param col the column expression
     * @param value the parameter value
     * @return true if a cmdParam should be used for the given column
     */
    protected boolean useCmdParam(DBColumnExpr col, Object value)
    {
        // Cannot wrap DBExpr or DBSystemDate
        if (value==null || value instanceof DBExpr || value instanceof DBDatabase.DBSystemDate)
            return false;
        // Check if prepared statements are enabled
        if (isPreparedStatementsEnabled())
            return true;
        // Only use a command param if column is of type BLOB or CLOB
        DataType dt = col.getDataType();
        return ( dt==DataType.BLOB || dt==DataType.CLOB );
    }

    /**
     * adds a constraint to the 'where' or 'having' collections 
     * @param list the 'where' or 'having' list
     * @param expr the DBCompareExpr object
     */
    protected void setConstraint(List<DBCompareExpr> list, DBCompareExpr expr)
    {
        // Check if prepared statements are enabled
        if (isPreparedStatementsEnabled())
        {   // use command params
            expr.prepareParams(this, this);
        }
        // adds a comparison to the where or having list
        for (int i = 0; i < list.size(); i++)
        {   // check expression
        	DBCompareExpr other = list.get(i);
            if (expr.isMutuallyExclusive(other)==false)
                continue;
            // Check if we replace a DBCommandParam
            removeCommandParams(other);
            // columns match
            list.set(i, expr);
            return;
        }
        // add expression
        list.add(expr);
    }
    
    /**
     * removes a constraint on a particular column to the 'where' or 'having' collections 
     * @param list the 'where' or 'having' list
     * @param cmpExpr the compare expression which to remove
     * @return true if the constraint was removed
     */
    protected boolean removeConstraint(List<DBCompareExpr> list, DBCompareExpr cmpExpr)
    {
        if (list == null)
            return false;
        for (DBCompareExpr cmp : list)
        {   // Compare columns
            if (cmp.isMutuallyExclusive(cmpExpr))
            {   // Check if we replace a DBCommandParam
                removeCommandParams(cmp);
                // remove the constraint
                list.remove(cmp);
                return true;
            }
        }
        return false;
    }
    
    /**
     * removes a constraint on a particular column to the 'where' or 'having' collections 
     * @param list the 'where' or 'having' list
     * @param colExpr the column expression for which to remove the constraint
     * @return the removed constraint
     */
    protected DBCompareExpr removeConstraintOn(List<DBCompareExpr> list, DBColumnExpr colExpr)
    {
        DBCompareExpr cmpExpr = findConstraintOn(list, colExpr);
        if (cmpExpr!=null)
        {   // Check if we replace a DBCommandParam
            removeCommandParams(cmpExpr);
            // remove the constraint
            list.remove(cmpExpr);
        }
        return cmpExpr;
    }
    
    /**
     * finds a constraint on a particular column to the 'where' or 'having' collections 
     * @param list the 'where' or 'having' list
     * @param colExpr the column expression for which to remove the constraint
     * @return the constraint for the given column or null if not found
     */
    protected DBCompareExpr findConstraintOn(List<DBCompareExpr> list, DBColumnExpr colExpr)
    {
        if (list == null)
            return null;
        for (DBCompareExpr cmp : list)
        {   // Compare columns
            if (cmp.isConstraintOn(colExpr))
            {   // found
                return cmp;
            }
        }
        return null;
    }
    
    /**
     * Gets a list of all tables referenced by the query.
     *  
     * @return list of all rowsets (tables or views) used by the query
     */
    protected List<DBRowSet> getRowSetList()
    {
        // Check all tables
        int i = 0;
        Set<DBColumn> columns = new HashSet<DBColumn>();
        for (i = 0; select != null && i < select.size(); i++)
            ((DBExpr) select.get(i)).addReferencedColumns(columns);
        for (i = 0; joins != null && i < joins.size(); i++)
            ((DBExpr) joins.get(i)).addReferencedColumns(columns);
        for (i = 0; where != null && i < where.size(); i++)
            ((DBExpr) where.get(i)).addReferencedColumns(columns);
        for (i = 0; groupBy != null && i < groupBy.size(); i++)
            ((DBExpr) groupBy.get(i)).addReferencedColumns(columns);
        for (i = 0; having != null && i < having.size(); i++)
            ((DBExpr) having.get(i)).addReferencedColumns(columns);
        for (i = 0; orderBy != null && i < orderBy.size(); i++)
            ((DBExpr) orderBy.get(i)).addReferencedColumns(columns);
        // now we have all columns
        List<DBRowSet> tables = new ArrayList<DBRowSet>();
        Iterator<DBColumn> iterator = columns.iterator();
        while (iterator.hasNext())
        { // get the table
            DBColumn col = iterator.next();
            DBRowSet table = col.getRowSet();
            if (table == cmdQuery)
            { // Recursion
                log.error("Recursive Column Selection in Command!");
                continue;
            }
            if (tables.contains(table) == false && table != null)
            {   // Add table
                tables.add(table);
            }
        }
        return tables;
    }
    
    /**
     * Adds Columns
     */
    @Override
    public void addReferencedColumns(Set<DBColumn> list)
    {
        // nothing to do!
        return;
    } 
    
    /**
     * Returns an array of parameter values for a prepared statement.
     * The parameters are supplied only after getSelect(), getUpdate(), getInsert() or getDelete() have been called
     * @return an array of parameter values for a prepared statement 
     */
    @Override
    public Object[] getParamValues()
    {
        // values
        return cmdParams.getParamValues();
    }
    
    /**
     * Creates a select SQL-Statement
     */
    @Override
    public void getSelect(DBSQLBuilder sql, int flags)
    {
        // Prepares statement
        resetParamUsage();
        // Select clause
        if (not(flags, SF_SKIP_SELECT))
        {   // check
            if (select == null)
                throw new ObjectNotValidException(this); // invalid!
            // add select
            addSelect(sql);
        }
        // From clause
        if (not(flags, SF_SKIP_FROM))
            addFrom(sql);
        // Add Where
        if (not(flags, SF_SKIP_WHERE))
            addWhere(sql);
        // Add Grouping
        if (not(flags, SF_SKIP_GROUP))
            addGrouping(sql);
        // Add Order
        if (not(flags, SF_SKIP_ORDER))
            addOrder(sql);
        // done
        completeParamUsage();
    }

    /**
     * Creates an insert SQL-Statement
     * @return an insert SQL-Statement
     */
    // get Insert
    public String getInsert()
    {
        resetParamUsage();
        if (set==null || set.get(0)==null)
            return null;
        DBSQLBuilder sql = createSQLBuilder("INSERT INTO ");
        // addTableExpr(sql, CTX_NAME);
        DBRowSet table =  set.get(0).getTable();
        table.addSQL(sql, CTX_FULLNAME);
        // Set Expressions
        sql.append("( ");
        // Set Expressions
        ArrayList<DBCompareColExpr> compexpr = null;
        if (notEmpty(where))
        {   // Convert ColumnExpression List to Column List
            compexpr = new ArrayList<DBCompareColExpr>(where.size());
            for (DBCompareExpr expr : where)
            {
                appendCompareColExprs(table, expr, compexpr);
            }
            // Add Column Names from where clause
            if (compexpr.size()>0)
            {
                // add List
                addListExpr(sql, compexpr, CTX_NAME, ", ");
                // add separator
                if (set != null)
                    sql.append(", ");
            }
            else
            {   // No columns to set
                compexpr = null;
            }
        }
        if (set != null)
            addListExpr(sql, set, CTX_NAME, ", ");
        // Values
        sql.append(") VALUES ( ");
        if (compexpr != null)
            addListExpr(sql, compexpr, CTX_VALUE, ", ");
        if (compexpr != null && set != null)
            sql.append(", ");
        if (set != null)
            addListExpr(sql, set, CTX_VALUE, ", ");
        // End
        sql.append(")");
        // done
        completeParamUsage();
        return sql.toString();
    }
    
    /**
     * Appends all nested DBCompareColExpr for a particular RowSet to a list
     * @param table the rowset for which to collect the DBCompareColExpr 
     * @param expr a compare expression
     * @param list the list of compare expressions
     */
    protected void appendCompareColExprs(DBRowSet table, DBCompareExpr expr, List<DBCompareColExpr> list)
    {
        if (expr instanceof DBCompareColExpr)
        {   // DBCompareColExpr
            DBColumn column = ((DBCompareColExpr)expr).getColumnExpr().getUpdateColumn();
            if (column!=null && column.getRowSet().equals(table) && !hasSetExprOn(column))
                list.add((DBCompareColExpr)expr);
        }
        else if (expr instanceof DBCompareAndOrExpr) 
        {   // DBCompareAndOrExpr
            appendCompareColExprs(table, ((DBCompareAndOrExpr)expr).getLeft(),  list);
            appendCompareColExprs(table, ((DBCompareAndOrExpr)expr).getRight(), list);
        }
        else if (expr instanceof DBCompareNotExpr) 
        {   // DBCompareNotExpr
            appendCompareColExprs(table, ((DBCompareNotExpr)expr).getExpr(),  list);
        }
        else if (ObjectUtils.isWrapper(expr))
        {   // unwrap
            appendCompareColExprs(table, ObjectUtils.unwrap(expr), list);
        }
    }

    /**
     * Creates an update SQL-Statement
     * 
     * @return an update SQL-Statement
     */
    public final String getUpdate()
    {
        resetParamUsage();
        if (set == null)
            return null;
        DBSQLBuilder sql = createSQLBuilder("UPDATE ");
        DBRowSet table =  set.get(0).getTable();
        if (notEmpty(joins))
        {   // Join Update
            addUpdateWithJoins(sql, table);
        }
        else
        {   // Simple Statement
            addUpdateForTable(sql, table);
        }
        // done
        completeParamUsage();
        return sql.toString();
    }

    protected void addUpdateForTable(DBSQLBuilder sql, DBRowSet table)
    {   // Simple Statement
        table.addSQL(sql, CTX_FULLNAME);
        long context = CTX_NAME | CTX_VALUE;
        // Set Expressions
        sql.append("\r\nSET ");
        addListExpr(sql, set, context, ", ");
        // Add Where
        addWhere(sql, context);
    }
    
    protected void addUpdateWithJoins(DBSQLBuilder sql, DBRowSet table)
    {   // Join Update
        sql.append( table.getAlias() );
        long context = CTX_DEFAULT;
        // Set Expressions
        sql.append("\r\nSET ");
        addListExpr(sql, set, context, ", ");
        // From clause
        addFrom(sql);
        // Add Where
        addWhere(sql, context);
    }
    
    /**
     * Creates a delete SQL-Statement
     * 
     * @param table the table object 
     * 
     * @return a delete SQL-Statement
     */
    public final String getDelete(DBTable table)
    {
        resetParamUsage();
        DBSQLBuilder sql = createSQLBuilder("DELETE ");
        // joins or simple
        if (notEmpty(joins))
        {   // delete with joins
            addDeleteWithJoins(sql, table);
        }
        else
        {   // Simple Statement
            addDeleteForTable(sql, table);
        }
        // done
        completeParamUsage();
        return sql.toString();
    }

    protected void addDeleteForTable(DBSQLBuilder sql, DBRowSet table)
    {   // Simple Statement
        sql.append("FROM ");
        table.addSQL(sql, CTX_FULLNAME);
        // where
        addWhere(sql, CTX_NAME|CTX_VALUE);
    }
    
    protected void addDeleteWithJoins(DBSQLBuilder sql, DBRowSet table)
    {   // delete with joins
        table.addSQL(sql, CTX_FULLNAME);
        // From clause
        addFrom(sql);
        // Add Where
        addWhere(sql, CTX_DEFAULT);
    }
    
    // ------- Select Statement Parts -------

    protected void addSelect(DBSQLBuilder sql)
    {
        // Prepares statement
        sql.append("SELECT ");
        if (selectDistinct)
            sql.append("DISTINCT ");
        // Add Select Expressions
        addListExpr(sql, select, CTX_ALL, ", ");
    }

    protected void addFrom(DBSQLBuilder sql)
    {
        int originalLength = sql.length();
        sql.append("\r\nFROM ");
        // Join
        boolean sep = false;
        // int whichParams = 0;
        List<DBRowSet> tables = getRowSetList();
        if (joins!=null && joins.size()>0)
        {   // Join
            List<DBRowSet> joinTables = new ArrayList<DBRowSet>();
            for (int i=0; i<joins.size(); i++)
            {    // append join
                 long context;
                 DBJoinExpr join = joins.get(i);
                 if (i<1)
                 {   // Add Join Tables
                     joinTables.add(join.getLeftTable());
                     joinTables.add(join.getRightTable());
                     // Remove from List
                     tables.remove(join.getLeftTable());
                     tables.remove(join.getRightTable());
                     // Context
                     context = CTX_NAME|CTX_VALUE;
                     // whichParams = 0;
                 }
                 else
                 {   // Extend the join                    
                     if (joinTables.contains(join.getRightTable()))
                         join.reverse();
                     // Add Right Table     
                     joinTables.add(join.getRightTable());
                     tables .remove(join.getRightTable());
                     // Context
                     context = CTX_VALUE;
                     sql.append( "\t" );
                     // whichParams = 1;
                 }
                 // check
                 join.addSQL(sql, context);
                 // cmdParams.addJoin(sql, join, context, whichParams);
                 // add CRLF
                 if( i!=joins.size()-1 )
                     sql.append("\r\n");
            }
            sep = true;
        }
        for (int i=0; i<tables.size(); i++)
        {
            DBRowSet t = tables.get(i);
            // check whether it's a parent table
            if (this.parentTables!=null && this.parentTables.contains(t))
                continue; // yes, ignore
            // append
            if (sep) sql.append(", ");
            t.addSQL(sql, CTX_DEFAULT|CTX_ALIAS);
            sep = true;
        }
        if (sep==false)
        {   // add pseudo table or omitt from
            String pseudoTable = getDatabase().getDbms().getSQLPhrase(DBSqlPhrase.SQL_PSEUDO_TABLE);
            if (StringUtils.isNotEmpty(pseudoTable))
            {   // add pseudo table
                sql.append(pseudoTable);
            }    
            else
            {   // remove from
                sql.reset(originalLength);
            }
        }
    }

    protected void addWhere(DBSQLBuilder sql, long context)
    {
        if (notEmpty(where))
        {   
            sql.append("\r\nWHERE ");
            // add where expression
            addListExpr(sql, where, context, " AND ");
        }
    }

    protected final void addWhere(DBSQLBuilder sql)
    {
        addWhere(sql, CTX_DEFAULT);
    }

    protected void addGrouping(DBSQLBuilder sql)
    {
        if (notEmpty(groupBy))
        {   // Group by
            sql.append("\r\nGROUP BY ");
            addListExpr(sql, groupBy, CTX_DEFAULT, ", ");
        }
        if (notEmpty(having))
        {   // Having
            sql.append("\r\nHAVING ");
            addListExpr(sql, having, CTX_DEFAULT, " AND ");
        }
    }

    protected void addOrder(DBSQLBuilder sql)
    {
        if (notEmpty(orderBy))
        { // order By
            sql.append("\r\nORDER BY ");
            addListExpr(sql, orderBy, CTX_DEFAULT, ", ");
        }
    }
   
}