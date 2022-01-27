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

import org.apache.empire.db.DBCmpType;
import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBColumnExpr;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.DBExpr;
import org.apache.empire.db.expr.column.DBAbstractFuncExpr;
import org.apache.empire.db.expr.column.DBAliasExpr;


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

    /**
     * Constructs a DBCompareColExpr object set the specified parameters to this object.
     * 
     * @param expr the DBColumnExpr object
     * @param op the comparative context e.g. (CMP_EQUAL, CMP_SMALLER)
     * @param value the comparative value
     */
    public DBCompareColExpr(DBColumnExpr expr, DBCmpType op, Object value)
    {
        this.expr = expr;
        this.cmpop = op;
        this.value = value;
    }

    /**
     * Returns the current DBDatabase object.
     * 
     * @return the current DBDatabase object
     */
    @Override
    public final <T extends DBDatabase> T getDatabase()
    {
        return expr.getDatabase();
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
    public DBCmpType getCmpop()
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
     * @param buf the SQL-Command
     * @param context the comparative context e.g. (CMP_EQUAL, CMP_SMALLER)
     */
    public void addCompareExpr(StringBuilder buf, long context)
    {   // Assemble expression
        String arraySep = "+";
        DBCmpType op = cmpop;
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
                // Nothing to do
                break;
        }
        // Value
        String valsql = getObjectValue(expr.getDataType(), value, context, arraySep);
        if (valsql == null || valsql.equalsIgnoreCase("null"))
        { // Null oder Not Null!
            op = DBCmpType.getNullType(op);
        }
        // Add comparison operator and value
        switch (op)
        {
            case EQUAL:
                buf.append("=");
                break;
            case NOTEQUAL:
                buf.append("<>");
                break;
            case LESSTHAN:
                buf.append("<");
                break;
            case MOREOREQUAL:
                buf.append(">=");
                break;
            case GREATERTHAN:
                buf.append(">");
                break;
            case LESSOREQUAL:
                buf.append("<=");
                break;
            case LIKE:
                buf.append(" LIKE ");
                break;
            case NOTLIKE:
                buf.append(" NOT LIKE ");
                break;
            case NULL:
                buf.append(" IS NULL");
                valsql = null;
                break;
            case NOTNULL:
                buf.append(" IS NOT NULL");
                valsql = null;
                break;
            case BETWEEN:
                buf.append(" BETWEEN ");
                break;
            case NOTBETWEEN:
                buf.append(" NOT BETWEEN ");
                break;
            case IN:
                buf.append(" IN (");
                buf.append(valsql);
                buf.append(")");
                valsql = null;
                break;
            case NOTIN:
                buf.append(" NOT IN (");
                buf.append(valsql);
                buf.append(")");
                valsql = null;
                break;
            default:
                // NONE
                buf.append(" ");
        }
        if (valsql != null)
            buf.append(valsql);
    }

    /**
     * Creates the SQL-Command.
     * 
     * @param buf the SQL-Command
     * @param context the current SQL-Command context
     */
    @Override
    public void addSQL(StringBuilder buf, long context)
    {
        // Name Only ?
        if ((context & CTX_VALUE) == 0)
        {
            expr.addSQL(buf, context);
            return;
        }
        // Value Only ?
        if ((context & CTX_NAME) == 0)
        {
            String valsql = getObjectValue(expr.getDataType(), value, context, "+");
            buf.append((valsql != null) ? valsql : "null");
            return;
        }
        // Add Compare Expression
        expr.addSQL(buf, context);
        addCompareExpr(buf, context);
    }

    /**
     * Returns whether the constraint should replace another one or not.
     * 
     * @return true it the constraints are mutually exclusive or false otherwise
     */
    @Override
    public boolean isMutuallyExclusive(DBCompareExpr other)
    {
    	if (other instanceof DBCompareColExpr)
    	{
    		DBCompareColExpr o = (DBCompareColExpr)other;
    		DBColumnExpr oexpr = o.getColumnExpr();
    		if (expr.equals(oexpr))
    			return true;
    		// unwrap
            DBColumnExpr texpr = expr;
            if (texpr instanceof DBAliasExpr)
                texpr = ((DBAliasExpr) texpr).unwrap();
            if (oexpr instanceof DBAliasExpr)
                oexpr = ((DBAliasExpr) oexpr).unwrap();
            // check function expression
            boolean tfunc = (texpr instanceof DBAbstractFuncExpr);
            boolean ofunc = (oexpr instanceof DBAbstractFuncExpr); 
            if (tfunc || ofunc) 
            {   // check if both are the same
                if (tfunc && ofunc)
                {   // both are functions
                    return ((DBAbstractFuncExpr)texpr).isMutuallyExclusive((DBAbstractFuncExpr)oexpr);
                }
                else
                {   // not the same
                    return false; 
                }
            }
            // finally check update columns
    		DBColumn tcol = texpr.getSourceColumn();
    		DBColumn ocol = oexpr.getSourceColumn();
    		return (tcol!=null) ? (tcol.equals(ocol)) : false;
    	}
    	return false;
    }
    
}