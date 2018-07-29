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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.apache.empire.commons.StringUtils;
import org.apache.empire.data.DataType;
import org.apache.empire.db.exceptions.DatabaseMismatchException;
import org.apache.empire.db.expr.compare.DBCompareColExpr;
import org.apache.empire.db.expr.compare.DBCompareExpr;
import org.apache.empire.db.expr.join.DBColumnJoinExpr;
import org.apache.empire.db.expr.join.DBCompareJoinExpr;
import org.apache.empire.db.expr.join.DBCrossJoinExpr;
import org.apache.empire.db.expr.join.DBJoinExpr;
import org.apache.empire.db.expr.set.DBSetExpr;
import org.apache.empire.exceptions.InternalException;
import org.apache.empire.exceptions.MiscellaneousErrorException;
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
    private final static long serialVersionUID = 1L;

    // Logger
    protected static final Logger log = LoggerFactory.getLogger(DBCommand.class);
    // Distinct Select
    protected boolean                selectDistinct = false;
    // Lists
    protected List<DBColumnExpr>     select         = null;
    protected List<DBSetExpr>        set            = null;
    protected List<DBJoinExpr>       joins          = null; // Join Info
    protected List<DBCompareExpr>    where          = null;
    protected List<DBCompareExpr>    having         = null;
    protected List<DBColumnExpr>     groupBy        = null;
    // Parameters for prepared Statements
    protected Vector<DBCmdParam>     cmdParams      = null;
    private int                      paramUsageCount= 0;
    // Database
    private transient DBDatabase     db;

    /**
     * Constructs a new DBCommand object and set the specified DBDatabase object.
     * 
     * @param db the current database object
     */
    protected DBCommand(DBDatabase db)
    {
        this.db = db;
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
            log.debug("Serialization: writing DBCommand "+dbid);
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
                log.debug("Serialization: reading DBCommand "+dbid);
            // find database
            DBDatabase sdb = DBDatabase.findById(dbid);
            if (sdb==null)
                throw new ClassNotFoundException(dbid);
            // set final field
            Field f = DBCommand.class.getDeclaredField("db");
            f.setAccessible(true);
            f.set(this, sdb);
            f.setAccessible(false);
        }    
        // read the rest
        strm.defaultReadObject();
    }
    
    /**
     * internally used to reset the command param usage count.
     * Note: Only one thread my generate an SQL statement 
     */
    protected void resetParamUsage()
    {
        paramUsageCount = 0;
    }
    
    /**
     * internally used to reorder the command params to match their order of occurance
     */
    protected synchronized void notifyParamUsage(DBCmdParam param)
    {
        int index = cmdParams.indexOf(param);
        if (index < paramUsageCount)
        {   // Error: parameter probably used twice in statement!
            throw new MiscellaneousErrorException("A parameter may only be used once in a command.");
        }
        if (index > paramUsageCount)
        {   // Correct parameter order
            cmdParams.remove(index);
            cmdParams.insertElementAt(param, paramUsageCount);
        }
        paramUsageCount++;
    }

    /**
     * internally used to remove the command param used in a constraint
     */
   	private void removeCommandParam(DBCompareColExpr cmp) 
   	{
        if (cmdParams!=null && (cmp.getValue() instanceof DBCmdParam))
   			cmdParams.remove(cmp.getValue());
   	}

    /**
     * internally used to remove all command params used in a list of constraints
     */
   	private void removeAllCommandParams(List<DBCompareExpr> list)
    {
        if (cmdParams == null)
        	return;
        for(DBCompareExpr cmp : list)
        {	// Check whether it is a compare column expr.
            if (!(cmp instanceof DBCompareColExpr))
            	continue;
            // Check the value is a DBCommandParam
        	removeCommandParam((DBCompareColExpr)cmp);
        }
    }
   	
    
    /**
     * Creates a clone of this class.
     */
    @Override
    public DBCommand clone()
    {
        try 
        {
            DBCommand clone = (DBCommand)super.clone();
            clone.db = db;
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
            if (cmdParams!=null)
            {   // clone params
                clone.paramUsageCount = 0;
                clone.cmdParams = new Vector<DBCmdParam>();
                for (DBCmdParam p : cmdParams)
                {
                    DBCmdParam param = new DBCmdParam(this, p.getDataType(), p.getValue());
                    clone.cmdParams.add(param);
                }
            }
            // done
            return clone;
            
        } catch (CloneNotSupportedException e) {
            log.error("Cloning DBCommand object failed!", e);
            throw new InternalException(e); 
        }
    }

    /**
     * Returns the current DBDatabase object.
     * 
     * @return the current DBDatabase object
     */
    @Override
    public DBDatabase getDatabase()
    {
        return db;
    }

    @Override
    public boolean isValid()
    {
    	return isValidQuery() || isValidUpdate();
    }

    /**
     * Returns whether the command object can produce a select sql-statement.
     * 
     * @return true if at least one select expression has been set
     */
    public boolean isValidQuery()
    {
    	return (select != null);
    }

    /**
     * Returns whether the command object can produce a update sql-statement.
     * 
     * @return true if a set expression has been set.
     */
    public boolean isValidUpdate()
    {
        return (set != null);
    }

    /**
     * Sets whether or not the select statement should contain
     * the distinct directive .
     */
    public void selectDistinct()
    {
    	this.selectDistinct = true;
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
     * Adds a DBColumnExpr object to the Vector: 'select'.
     * 
     * @param expr the DBColumnExpr object
     */
    public void select(DBColumnExpr expr)
    {   // Select this column
        checkDatabase(expr);
        if (select == null)
            select = new ArrayList<DBColumnExpr>();
        if (expr != null && select.contains(expr) == false)
            select.add(expr);
    }

    /**
     * Adds a list of columns to the select phrase of an sql statement.
     * 
     * @param exprs an vararg of DBColumnExpr's to select
     */
    public final void select(DBColumnExpr... exprs)
    {
        for (DBColumnExpr expr : exprs)
        {
            select(expr);
        }
    }

    /**
     * Adds a collection of columns to the select phrase of an sql statement.
     * 
     * @param columns the column expressions to add
     */
    public final void select(Collection<? extends DBColumnExpr> columns)
    {
        for (DBColumnExpr expr : columns)
        {
            select(expr);
        }
    }

    /**
     * Adds a list of columns with their qualified name to the select phrase of an sql statement.
     * 
     * @param exprs one or more columns to select
     */
    public void selectQualified(DBColumn... columns)
    {
        for (DBColumn col : columns)
        {
            select(col.qualified());
        }
    }

    /**
     * Adds a collection of columns to the select phrase of an sql statement.
     * 
     * @param columns the column expressions to add
     */
    public final void selectQualified(Collection<? extends DBColumn> columns)
    {
        for (DBColumn col : columns)
        {
            select(col.qualified());
        }
    }
    
    /**
     * returns true if prepared statements are enabled for this database
     */
    protected boolean isPreparedStatementsEnabled()
    {
        return db.isPreparedStatementsEnabled();
    }
    
    /**
     * returns true if a cmdParam should be used for the given column or false otherwise
     */
    protected boolean useCmdParam(DBColumn col, Object value)
    {
        // Cannot wrap DBExpr or DBSystemDate
        if (value instanceof DBExpr || value instanceof DBDatabase.DBSystemDate)
            return false;
        // Check if prepared statements are enabled
        if (isPreparedStatementsEnabled())
            return true;
        // Only use a command param if column is of type BLOB or CLOB
        DataType dt = col.getDataType();
        return ( dt==DataType.BLOB || dt==DataType.CLOB );
    }
    
    /**
     * Adds a single set expressions to this command
     * Use column.to(...) to create a set expression 
     * @param expr the DBSetExpr object(s)
     */
    public void set(DBSetExpr expr)
    {
        checkDatabase(expr);
        if (set == null)
            set = new ArrayList<DBSetExpr>();
        for (int i = 0; i < set.size(); i++)
        {
            DBSetExpr chk = set.get(i);
            if (chk.column.equals(expr.column))
            { // Overwrite existing value
                if (useCmdParam(expr.column, expr.value))
                {   // replace parameter value
                    // int index = ((DBCommandParam) chk.value).index;
                    // this.setCmdParam(index, getCmdParamValue(expr.column, expr.value));
                    if (chk.value instanceof DBCmdParam)
                        ((DBCmdParam)chk.value).setValue(expr.value);
                    else
                        chk.value = addParam(expr.column.getDataType(), expr.value);
                } 
                else
                {   // remove from parameter list (if necessary)
                    if (cmdParams!=null && chk.value instanceof DBCmdParam)
                        cmdParams.remove(chk.value);
                    // replace value
                    chk.value = expr.value;
                }
                return;
            }
        }
        // Replace with parameter 
        if (useCmdParam(expr.column, expr.value))
            expr.value = addParam(expr.column.getDataType(), expr.value);
        // new Value!
        set.add(expr);
    }
    
    /**
     * Adds a list of set expressions to this command
     * Use column.to(...) to create a set expression 
     * @param expr the DBSetExpr object(s)
     */
    public final void set(DBSetExpr... exprs)
    {
        for (int i=0; i<exprs.length; i++)
            set(exprs[i]);
    }

    /**
     * Checks whether a column is in the list of set expressions
     * @param column
     * @return <code>true</code> if there is a set expression 
     */
    protected boolean hasSetExprOn(DBColumn column)
    {
        if (set==null)
            return false;
        Iterator<DBSetExpr> i = set.iterator();
        while (i.hasNext())
        {
            DBSetExpr chk = i.next();
            if (chk.column.equals(column))
                return true;
        }
        return false;
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
        if (cmdParams==null)
            cmdParams= new Vector<DBCmdParam>();
        // Adds the parameter 
        DBCmdParam param = new DBCmdParam(this, type, value);
        if (cmdParams.add(param)==false)
            return null; // unknown error
        // Creates a Parameter expression
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
     *  
     * @return the command parameter object
     */
    public final DBCmdParam addParam(Object value)
    {
        return addParam(DataType.UNKNOWN, value);
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
     */
    public void join(DBJoinExpr join)
    {
        checkDatabase(join);
        if (joins == null)
            joins = new ArrayList<DBJoinExpr>();
        // Create a new join
        for (int i = 0; i < joins.size(); i++)
        { // Check whether join exists
            DBJoinExpr item = joins.get(i);
            if (item.equals(join))
                return;
        }
        joins.add(join);
    }
    
    /**
     * Adds a cross join for two tables or views 
     * @param left the left RowSet
     * @param right the right RowSet
     * @return the join expression
     */
    public final DBCrossJoinExpr join(DBRowSet left, DBRowSet right)
    {
        DBCrossJoinExpr join = new DBCrossJoinExpr(left, right);
        join(join);
        return join;
    }

    /**
     * Adds a join based on two columns to the list of join expressions.
     * 
     * @param left the left join value
     * @param right the right join
     * @param joinType type of join ({@link DBJoinType#INNER}, {@link DBJoinType#LEFT}, {@link DBJoinType#RIGHT})
     * 
     * @return the join expression 
     */
    public final DBColumnJoinExpr join(DBColumnExpr left, DBColumnExpr right, DBJoinType joinType)
    {
        DBColumnJoinExpr join = new DBColumnJoinExpr(left, right, joinType); 
        join(join);
        return join;
    }

    /**
     * Adds an inner join based on two columns to the list of join expressions.
     * 
     * @param left the left join value
     * @param right the right join
     * 
     * @return the join expresion 
     */
    public final DBColumnJoinExpr join(DBColumnExpr left, DBColumn right)
    {
        return join(left, right, DBJoinType.INNER);
    }

    /**
     * Adds a join based on a compare expression to the command.
     * 
     * @param rowset table or view to join
     * @param cmp the compare expression with which to join the table
     * @param joinType type of join ({@link DBJoinType#INNER}, {@link DBJoinType#LEFT}, {@link DBJoinType#RIGHT})
     * 
     * @return the join expresion 
     */
    public final DBCompareJoinExpr join(DBRowSet rowset, DBCompareExpr cmp, DBJoinType joinType)
    {
        DBCompareJoinExpr join = new DBCompareJoinExpr(rowset, cmp, joinType); 
        join(join);
        return join;
    }

    /**
     * Adds an inner join based on a compare expression to the command.
     * 
     * @param rowset table of view which to join
     * @param cmp the compare expression with wich to join the table
     * 
     * @return the join expresion 
     */
    public final DBCompareJoinExpr join(DBRowSet rowset, DBCompareExpr cmp)
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
        /*
        for (i = 0; groupBy != null && i < groupBy.size(); i++)
            ((DBExpr) groupBy.get(i)).addReferencedColumns(columns);
        */
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
     */
    public void where(DBCompareExpr expr)
    {
        checkDatabase(expr);
        if (where == null)
            where = new ArrayList<DBCompareExpr>();
        setConstraint(where, expr);
    }
    
    /**
     * Adds a list of constraints to the where phrase of the sql statement
     * If another restriction already exists for the same column it will be replaced.
     * 
     * @param expr the DBCompareExpr object
     */
    public final void where(DBCompareExpr... exprs)
    {
        for (int i=0; i<exprs.length; i++)
            where(exprs[i]);
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
     * @param col the column expression for which to remove the constraint
     */
    public void removeWhereConstraintOn(DBColumnExpr col)
    {
        if (where == null)
        	return;
        removeConstraintOn(where, col);
    }

    /**
     * Returns a copy of the defined joins.
     * 
     * @return vector of joins
     */
    public List<DBJoinExpr> getJoins()
    {
        return (this.joins!=null ? Collections.unmodifiableList(this.joins) : null);
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
     * adds a constraint to the having clause.
     * @param expr the DBCompareExpr object
     */
    public void having(DBCompareExpr expr)
    {
        checkDatabase(expr);
        if (having == null)
            having = new ArrayList<DBCompareExpr>();
        setConstraint(having, expr);
    }
    
    /**
     * removes a constraint on a particular column from the where clause
     * @param col the column expression for which to remove the constraint
     */
    public void removeHavingConstraintOn(DBColumnExpr col)
    {
        if (having == null)
        	return;
        removeConstraintOn(having, col);
    }

    /**
     * Adds a list of columns to the group by phrase of an sql statement.
     * 
     * @param exprs vararg of columns by which to group the rows
     */
    public void groupBy(DBColumnExpr...exprs)
    {
        if (groupBy == null)
            groupBy = new ArrayList<DBColumnExpr>();
        // Add all
        for(DBColumnExpr expr : exprs)
        {
            checkDatabase(expr);
            if (expr.isAggregate()==false && groupBy.contains(expr)==false)
                groupBy.add(expr);
        }
    }

    /**
     * Adds a collection of columns to the group by phrase of an sql statement.
     * 
     * @param columns the column expressions to add
     */
    public final void groupBy(Collection<? extends DBColumnExpr> columns)
    {
        for (DBColumnExpr expr : columns)
        {
            groupBy(expr);
        }
    }
    
    public boolean hasSelectExpr()
    {
        return (select!=null && select.size()>0);
    }
    
    @Override
    public synchronized void getSelect(StringBuilder buf)
    {
        resetParamUsage();
        if (select == null)
            throw new ObjectNotValidException(this); // invalid!
        // Prepares statement
        addSelect(buf);
        // From clause
        addFrom(buf);
        // Add Where
        addWhere(buf);
        // Add Grouping
        addGrouping(buf);
        // Add Order
        addOrder(buf);
    }
    
    /**
     * Returns a array of all select DBColumnExpr for this command 
     * 
     * @return a array of all DBColumnExpr objects or <code>null</code> if there are no selects
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
     * Clears the list of set expressions.
     */
    public void clearSet()
    {
        set = null;
        cmdParams = null;
    }

    /**
     * Clears the list of join expressions.
     */
    public void clearJoin()
    {
        joins = null;
    }

    /**
     * Clears the list of where constraints.
     */
    public void clearWhere()
    {
    	removeAllCommandParams(where);
        where = null;
    }

    /**
     * Clears the list of having constraints.
     */
    public void clearHaving()
    {
    	removeAllCommandParams(having);
        having = null;
    }

    /**
     * Clears the list of group by constraints.
     */
    public void clearGroupBy()
    {
        groupBy = null;
    }

    /**
     * Clears the entire command object.
     */
    public void clear()
    {
        cmdParams = null;
        clearSelectDistinct();
        clearSelect();
        clearSet();
        clearJoin();
        clearWhere();
        clearHaving();
        clearGroupBy();
        clearOrderBy();
        clearLimit();
        resetParamUsage();
    }

    /**
     * adds a constraint to the 'where' or 'having' collections 
     * @param list the 'where' or 'having' list
     * @param expr the DBCompareExpr object
     */
    protected void setConstraint(List<DBCompareExpr> list, DBCompareExpr expr)
    { // adds a comparison to the where or having list
        for (int i = 0; i < list.size(); i++)
        { // check expression
        	DBCompareExpr other = list.get(i);
            if (expr.isMutuallyExclusive(other)==false)
                continue;
            // Check if we replace a DBCommandParam
            if (other instanceof DBCompareColExpr)
            	removeCommandParam((DBCompareColExpr)other);
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
     * @param col the column expression for which to remove the constraint
     */
    protected void removeConstraintOn(List<DBCompareExpr> list, DBColumnExpr col)
    {
        if (list == null)
        	return;
        for(DBCompareExpr cmp : list)
        {	// Check whether it is a compare column expr.
            if (!(cmp instanceof DBCompareColExpr))
            	continue;
            // Compare columns
            DBColumnExpr c = ((DBCompareColExpr)cmp).getColumnExpr();
            DBColumn udc = c.getUpdateColumn();
            if (c.equals(col) || (udc!=null && udc.equals(col.getUpdateColumn())))
            {   // Check if we replace a DBCommandParam
            	removeCommandParam((DBCompareColExpr)cmp);
            	// remove the constraint
            	list.remove(cmp);
            	return;
            }
        }
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
            { // Add table
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
     * To ensure that all values are in the order of their occurrence, getSelect() should be called first.
     * @return an array of parameter values for a prepared statement 
     */
    @Override
    public Object[] getParamValues()
    {
        if (cmdParams==null || cmdParams.size()==0)
            return null;
        // Check whether all parameters have been used
        if (paramUsageCount>0 && paramUsageCount!=cmdParams.size())
	        log.warn("DBCommand parameter count ("+String.valueOf(cmdParams.size())
	        	   + ") does not match parameter use count ("+String.valueOf(paramUsageCount)+")");
        // Create result array
        Object[] values = new Object[cmdParams.size()];
        for (int i=0; i<values.length; i++)
            values[i]=cmdParams.get(i).getValue();
        // values
        return values;
    }

    /**
     * Creates the update SQL-Command.
     * 
     * @return the update SQL-Command
     */
    public synchronized String getUpdate()
    {
        resetParamUsage();
        if (set == null)
            return null;
        StringBuilder buf = new StringBuilder("UPDATE ");
        DBRowSet table =  set.get(0).getTable();
        if (joins!=null && !joins.isEmpty())
        {   // Join Update
            buf.append( table.getAlias() );
            long context = CTX_DEFAULT;
            // Set Expressions
            buf.append("\r\nSET ");
            addListExpr(buf, set, context, ", ");
            // From clause
            addFrom(buf);
            // Add Where
            addWhere(buf, context);
        }
        else
        {   // Simple Statement
            table.addSQL(buf, CTX_FULLNAME);
            long context = CTX_NAME | CTX_VALUE;
            // Set Expressions
            buf.append("\r\nSET ");
            addListExpr(buf, set, context, ", ");
            // Add Where
            addWhere(buf, context);
        }
        // done
        return buf.toString();
    }

    /**
     * Creates the insert SQL-Command.
     * 
     * @return the insert SQL-Command
     */
    // get Insert
    public synchronized String getInsert()
    {
        resetParamUsage();
        if (set==null || set.get(0)==null)
            return null;
        StringBuilder buf = new StringBuilder("INSERT INTO ");
        // addTableExpr(buf, CTX_NAME);
        DBRowSet table =  set.get(0).getTable();
        table.addSQL(buf, CTX_FULLNAME);
        // Set Expressions
        buf.append("( ");
        // Set Expressions
        ArrayList<DBCompareColExpr> compexpr = null;
        if (where!=null && !where.isEmpty())
        {   // Convert ColumnExpression List to Column List
            compexpr = new ArrayList<DBCompareColExpr>(where.size());
            for (DBCompareExpr expr : where)
            {   if (expr instanceof DBCompareColExpr)
                {   DBColumn column = ((DBCompareColExpr)expr).getColumnExpr().getUpdateColumn();
                    if (column!=null && hasSetExprOn(column)==false)
                        compexpr.add((DBCompareColExpr)expr);
                }
            }
            // Add Column Names from where clause
            if (compexpr.size()>0)
            {
                // add List
                addListExpr(buf, compexpr, CTX_NAME, ", ");
                // add separator
                if (set != null)
                    buf.append(", ");
            }
            else
            {   // No columns to set
                compexpr = null;
            }
        }
        if (set != null)
            addListExpr(buf, set, CTX_NAME, ", ");
        // Values
        buf.append(") VALUES ( ");
        if (compexpr != null)
            addListExpr(buf, compexpr, CTX_VALUE, ", ");
        if (compexpr != null && set != null)
            buf.append(", ");
        if (set != null)
            addListExpr(buf, set, CTX_VALUE, ", ");
        // End
        buf.append(")");
        return buf.toString();
    }
    
    /**
     * Creates the delete SQL-Command.
     * 
     * @param table the table object 
     * 
     * @return the delete SQL-Command
     */
    public synchronized String getDelete(DBTable table)
    {
        resetParamUsage();
        StringBuilder buf = new StringBuilder("DELETE FROM ");
        table.addSQL(buf, CTX_FULLNAME);
        // Set Expressions
        if (where!=null && !where.isEmpty())
        { // add where condition
            buf.append("\r\nWHERE ");
            addListExpr(buf, where, CTX_NAME|CTX_VALUE, " AND ");
        }
        return buf.toString();
    }
    
    // ------- Select Statement Parts -------

    protected void addSelect(StringBuilder buf)
    {
        // Prepares statement
        buf.append("SELECT ");
        if (selectDistinct)
            buf.append("DISTINCT ");
        // Add Select Expressions
        addListExpr(buf, select, CTX_ALL, ", ");
    }

    protected void addFrom(StringBuilder buf)
    {
        int originalLength = buf.length();
        buf.append("\r\nFROM ");
        // Join
        boolean sep = false;
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
                 }
                 else
                 {   // Extend the join                    
                     if ( joinTables.contains(join.getRightTable()))
                          join.reverse();
                     // Add Right Table     
                     joinTables.add(join.getRightTable());
                     tables .remove(join.getRightTable());
                     // Context
                     context = CTX_VALUE;
                     buf.append( "\t" );
                 }
                 join.addSQL(buf, context);
                 // add CRLF
                 if( i!=joins.size()-1 )
                     buf.append("\r\n");
            }
            sep = true;
        }
        for (int i=0; i<tables.size(); i++)
        {
            if (sep) buf.append(", ");
            DBRowSet t = tables.get(i); 
            t.addSQL(buf, CTX_DEFAULT|CTX_ALIAS);
            sep = true;
        }
        if (sep==false)
        {   // add pseudo table or omitt from
            String pseudoTable = db.getDriver().getSQLPhrase(DBDatabaseDriver.SQL_PSEUDO_TABLE);
            if (StringUtils.isNotEmpty(pseudoTable))
            {   // add pseudo table
                buf.append(pseudoTable);
            }    
            else
            {   // remove from
                buf.setLength(originalLength);
            }
        }
    }

    protected void addWhere(StringBuilder buf, long context)
    {
        if (where!=null && !where.isEmpty())
        {   
            buf.append("\r\nWHERE ");
            // add where expression
            addListExpr(buf, where, context, " AND ");
        }
    }

    protected final void addWhere(StringBuilder buf)
    {
        addWhere(buf, CTX_DEFAULT);
    }

    protected void addGrouping(StringBuilder buf)
    {
        if (groupBy!=null && !groupBy.isEmpty())
        { // Having
            buf.append("\r\nGROUP BY ");
            addListExpr(buf, groupBy, CTX_DEFAULT, ", ");
        }
        if (having!=null && !having.isEmpty())
        { // Having
            buf.append("\r\nHAVING ");
            addListExpr(buf, having, CTX_DEFAULT, " AND ");
        }
    }

    protected void addOrder(StringBuilder buf)
    {
        if (orderBy!=null && !orderBy.isEmpty())
        { // order By
            buf.append("\r\nORDER BY ");
            addListExpr(buf, orderBy, CTX_DEFAULT, ", ");
        }
    }
     
    protected void checkDatabase(DBExpr expr)
    {
        if (getDatabase().equals(expr.getDatabase()))
            return;
        // not the same database
        throw new DatabaseMismatchException(this, expr);
    }
   
}