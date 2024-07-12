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
package org.apache.empire.db.expr.compare;

import java.util.Set;

import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.commons.StringUtils;
import org.apache.empire.data.DataType;
import org.apache.empire.db.DBCmdParam;
import org.apache.empire.db.DBCmpType;
import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBColumnExpr;
import org.apache.empire.db.DBCommand;
import org.apache.empire.db.DBCommandExpr;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.DBExpr;
import org.apache.empire.db.DBRowSet;
import org.apache.empire.db.DBSQLBuilder;
import org.apache.empire.db.expr.column.DBAliasExpr;
import org.apache.empire.dbms.DBSqlPhrase;


/**
 * This class is used for defining filter constraints based on a column expression in SQL<br>
 * <P>
 * There is no need to explicitly create instances of this class.<BR>
 * Instead use any of the following functions:<BR>
 * {@link DBColumnExpr#is(Object) }, {@link DBColumnExpr#isBetween(Object, Object) }, {@link DBColumnExpr#isGreaterThan(Object) }, 
 * {@link DBColumnExpr#isLessOrEqual(Object) }, {@link DBColumnExpr#isMoreOrEqual(Object) }, {@link DBColumnExpr#isNot(Object) }, 
 * {@link DBColumnExpr#isNotBetween(Object, Object) }, {@link DBColumnExpr#isSmallerThan(Object) }, {@link DBColumnExpr#like(Object) }, 
 * {@link DBColumnExpr#like(String, char) }, {@link DBColumnExpr#likeLower(String) }, {@link DBColumnExpr#likeUpper(String) } 
 * 
 *
 */
public class DBCompareColExpr extends DBCompareExpr
{
    // *Deprecated* private static final long serialVersionUID = 1L;
  
    protected final DBColumnExpr expr;
    protected final DBCmpType    cmpop;
    protected Object value;
    protected boolean parenthesis = false;
    /**
     * Constructs a DBCompareColExpr object set the specified parameters to this object.
     * 
     * @param expr the DBColumnExpr object
     * @param op the comparative context e.g. (CMP_EQUAL, CMP_SMALLER)
     * @param value the comparative value
     */
    public DBCompareColExpr(DBColumnExpr expr, DBCmpType op, Object value)
    {
        // unwrap DBAliasExpr only
        if (expr instanceof DBAliasExpr)
            expr = ((DBAliasExpr)expr).unwrap();
        // set
        this.expr = expr;
        this.cmpop = op;
        this.value = value;
    }

    /**
     * Returns the current DBDatabase object.
     * 
     * @return the current DBDatabase object
     */
    @SuppressWarnings("unchecked")
    @Override
    public final DBDatabase getDatabase()
    {
        return expr.getDatabase();
    }
    
    /**
     * Returns the underlying rowset containing this column
     */
    @Override
    public DBRowSet getRowSet()
    {
        return expr.getRowSet();
    }

    /** 
     * Gets the DBColumnExpr object 
     * @return the DBColumnExpr object 
     */
    public DBColumnExpr getColumnExpr()
    {
        return expr;
    }

    /**
     * Gets the comparison operator
     * @return the comparison operator
     */
    public DBCmpType getCmpOperator()
    {
        return cmpop;
    }

    /**
     * Gets the value to compare the column expression with
     * @return the value to compare the column expression with
     */
    public Object getValue()
    {
        return value;
    }

    /**
     * The value to compare the column expression with
     * @param value the comparison value 
     */
    public void setValue(Object value)
    {
        this.value = value;
    }

    /**
     * wraps the expression in parenthesises
     */
    public DBCompareColExpr parenthesis()
    {
        this.parenthesis = true;
        return this;
    }
    
    /**
     * Prepare function
     * @param cmd
     */
    @Override
    public void prepareCommand(DBCommand cmd) 
    {
        // Cannot use DBExpr or DBSystemDate as parameter
        if (value==null || value instanceof DBCmdParam || value instanceof DBExpr || value instanceof DBDatabase.DBSystemDate)
            return;
        // check operator
        switch(cmpop)
        {
            case EQUAL:
            case NOTEQUAL:
            case LESSTHAN:
            case MOREOREQUAL:
            case GREATERTHAN:
            case LESSOREQUAL:
            case LIKE:
            case NOTLIKE:
                // create command param
                value = cmd.addParam(expr.getDataType(), value);
                break;
            default:
                // not supported
                return;
        }
    }

    /**
     * Copy Command
     * @param newCmd the new command object
     */
    @Override
    public DBCompareExpr copy(DBCommand newCmd)
    {
        Object valueCopy = value;
        if (value instanceof DBCmdParam) 
            valueCopy = newCmd.addParam(DataType.UNKNOWN, ((DBCmdParam)value).getValue());
        return new DBCompareColExpr(expr, cmpop, valueCopy);
    }
    
    /**
     * @see org.apache.empire.db.DBExpr#addReferencedColumns(Set)
     */
    @Override
    public void addReferencedColumns(Set<DBColumn> list)
    {   // return all Columns
        expr.addReferencedColumns(list);
        // Check if Value is a Column Expression
        if (value instanceof DBExpr)
            ((DBExpr)value).addReferencedColumns(list);
    }

    /**
     * Add the comparison operator and value to the SQL-Command.
     * 
     * @param sql the SQL-Command
     * @param context the comparative context e.g. (CMP_EQUAL, CMP_SMALLER)
     */
    public void addCompareExpr(DBSQLBuilder sql, long context)
    {   // Assemble expression
        DBCmpType op = cmpop;
        if (ObjectUtils.isEmpty(value))
        { // Null oder Not Null!
            op = DBCmpType.getNullType(op);
        }
        // Array Separator
        String arraySep;
        switch (op)
        { // other than default:
            case BETWEEN:
            case NOTBETWEEN:
                arraySep = " AND ";
                break;
            case IN:
            case NOTIN:
                arraySep = ", ";
                break;
            default:
                arraySep = (expr.getDataType().isNumeric() ? "+" : sql.getPhrase(DBSqlPhrase.SQL_CONCAT_EXPR));
                break;
        }
        // Add comparison operator and value
        String suffix = null;
        switch (op)
        {
            case EQUAL:
                sql.append("=");
                break;
            case NOTEQUAL:
                sql.append("<>");
                break;
            case LESSTHAN:
                sql.append("<");
                break;
            case MOREOREQUAL:
                sql.append(">=");
                break;
            case GREATERTHAN:
                sql.append(">");
                break;
            case LESSOREQUAL:
                sql.append("<=");
                break;
            case LIKE:
                sql.append(" LIKE ");
                break;
            case NOTLIKE:
                sql.append(" NOT LIKE ");
                break;
            case NULL:
                sql.append(" IS NULL");
                return;
            case NOTNULL:
                sql.append(" IS NOT NULL");
                return;
            case BETWEEN:
                sql.append(" BETWEEN ");
                break;
            case NOTBETWEEN:
                sql.append(" NOT BETWEEN ");
                break;
            case IN:
                sql.append(" IN (");
                if (value instanceof DBCommandExpr)
                    context |= CTX_NOPARENTHESIS;
                suffix = ")";
                break;
            case NOTIN:
                sql.append(" NOT IN (");
                if (value instanceof DBCommandExpr)
                    context |= CTX_NOPARENTHESIS;
                suffix = ")";
                break;
            default:
                // NONE
                sql.append(" ");
        }
        // append value
        sql.appendValue(expr.getDataType(), value, context, arraySep);
        // append suffix
        if (suffix != null)
            sql.append(suffix);
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
        // Name Only ?
        if ((context & CTX_VALUE) == 0)
        {
            expr.addSQL(sql, context);
            return;
        }
        // Value Only ?
        if ((context & CTX_NAME) == 0)
        {   // add SQL
            sql.appendValue(expr.getDataType(), value, context, null);
            return;
        }
        // begin
        if (this.parenthesis)
            sql.append("(");
        // Add Compare Expression
        expr.addSQL(sql, context);
        // Add Comparison Value
        addCompareExpr(sql, context);
        // end
        if (this.parenthesis)
            sql.append(")");
    }
    
    /**
     * For Debugging
     */
    @Override
    public String toString()
    {
        StringBuilder b = new StringBuilder(expr.toString());
        b.append(cmpop.toString());
        if (value instanceof DBCmdParam)
            b.append("?");
        else
            b.append(StringUtils.toString(value, StringUtils.NULL));
        return b.toString();
    }

    /**
     * Returns whether the constraint should replace another one or not.
     * 
     * @return true it the constraints are mutually exclusive or false otherwise
     */
    @Override
    public boolean isMutuallyExclusive(DBCompareExpr other)
    {
        // check type 
    	if (other instanceof DBCompareColExpr)
    	{   // other
            DBCompareColExpr o = (DBCompareColExpr)other;
            DBColumnExpr oexpr = o.getColumnExpr();
    		// Compare
    		if (ObjectUtils.compareEqual(expr, oexpr))
    			return true;
    		/*
            // probably not a good idea to do this:
    		DBColumn tcol = texpr.getSourceColumn();
    		DBColumn ocol = oexpr.getSourceColumn();
    		return (tcol!=null) ? (tcol.equals(ocol)) : false;
    		*/
            return false;
    	}
    	// other types
    	return equals(other);
    }

    /**
     * Returns whether the constraint is on the given column
     * 
     * @return true it the constraint is on the given column or false otherwise
     */
    @Override
    public boolean isConstraintOn(DBColumnExpr colExpr)
    {
        // compare columns
        if (ObjectUtils.compareEqual(expr, colExpr))
            return true;
        // Update column
        if ((colExpr instanceof DBColumn) && !(expr instanceof DBColumn) && colExpr.equals(expr.getUpdateColumn()))
            return true;
        // not found
        return false;
    }
    
}