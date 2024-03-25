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
package org.apache.empire.db.expr.column;

// Java
import java.util.Date;
import java.util.Set;

import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.commons.StringUtils;
import org.apache.empire.data.DataType;
import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBColumnExpr;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.DBExpr;
import org.apache.empire.db.DBRowSet;
import org.apache.empire.db.DBSQLBuilder;
import org.w3c.dom.Element;


/**
 * This class is used for performing calculations in SQL<br>
 * It handles the mathematical operations ("+", "-", "*", "/") for the current column.
 * <P>
 * There is no need to explicitly create instances of this class.<BR>
 * Instead use {@link DBColumnExpr#plus(Object) }, {@link DBColumnExpr#minus(Object) },
 * {@link DBColumnExpr#multiplyWith(Object) }, {@link DBColumnExpr#divideBy(Object) }
 */
public class DBCalcExpr extends DBColumnExpr
{
    // *Deprecated* private static final long serialVersionUID = 1L;
  
    private final DBColumnExpr expr;
    private final String       op;
    private final Object       value;

    /**
     * Constructs a DBCalcExpr object Sets the mathematical operations ("+", "-", "*", "/") <br>
     * for the specified DBColumnExpr object and value.
     * 
     * @param expr an DBColumnExpr object, one column
     * @param op the mathematical operation ("+", "-", "*" or "/")
     * @param value the value to multiply, divide, sum or subtract 
     */
    public DBCalcExpr(DBColumnExpr expr, String op, Object value)
    {
        this.expr = expr;
        this.value = value;
        this.op = op;
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
     * Returns the data type: DBDataType.DECIMAL.
     * 
     */
    @Override
    public DataType getDataType()
    {
        DataType type = expr.getDataType();
        // Special treatment for adding days to dates
        if (type.isDate())
        {   // see whether the value is a date too
            if ((value instanceof Date) || 
                (value instanceof DBDatabase.DBSystemDate) ||
               ((value instanceof DBColumnExpr) && ((DBColumnExpr)value).getDataType().isDate()))
            {   // Yes, result is a decimal
                return DataType.DECIMAL;
            }
        }
        else if ((value instanceof DBColumnExpr))
        {   // Use the value type?
            DataType type2 =  ((DBColumnExpr)value).getDataType();
            if (type2.isNumeric() && type2.ordinal()>type.ordinal())
                return type2;
        }
        // type
        return type;
    }

    /**
     * Not an Enum. Returns null
     */
    @Override
    public Class<Enum<?>> getEnumType()
    {
        return null;
    }

    /** Returns the given expression name. */
    @Override
    public String getName()
    {   // Get the expression name
        String exprName = expr.getName();
        String valName  =((value instanceof DBColumnExpr) ? ((DBColumnExpr)value).getName() : null);
        if (StringUtils.isNotEmpty(exprName))
             return ((valName!=null) ? exprName+"_"+valName : exprName);
        else return StringUtils.coalesce(valName, "CALC");
    }

    /**
     * Returns the underlying rowset
     */
    @Override
    public DBRowSet getRowSet()
    {
        return expr.getRowSet();
    }

    /**
     * Returns the underlying column
     */
    @Override
    public DBColumn getUpdateColumn()
    {
        return expr.getUpdateColumn();
    }
    
    /**
     * Always returns false since CalcExpressions cannot be aggregates.
     * 
     * @return false
     */
    @Override
    public boolean isAggregate()
    {
        return false;
    }
    
    /**
     * Returns true if other is equal to this expression  
     */
    @Override
    public boolean equals(Object other)
    {
        if (other==this)
            return true;
        // Check Type
        if (other instanceof DBCalcExpr)
        {   // Compare
            DBCalcExpr otherCalc = (DBCalcExpr)other;
            // Expression must match
            if (!expr.equals(otherCalc.expr))
                return false;
            // Operators must match
            if (!StringUtils.compareEqual(this.op, otherCalc.op))
                return false;
            // Compare values
            return ObjectUtils.compareEqual(this.value, otherCalc.value);
        }
        return false;
    }

    /**
     * @see org.apache.empire.db.DBExpr#addReferencedColumns(Set)
     */
    @Override
    public void addReferencedColumns(Set<DBColumn> list)
    {
        expr.addReferencedColumns(list);
        // Check if value is a DBExpr
        if (value instanceof DBExpr)
            ((DBExpr)value).addReferencedColumns(list);
    }

    /**
     * Creates the SQL-Command adds the mathematical operations for
     * the specified DBColumnExpr object and value to the.
     * SQL-Command
     * 
     * @param sql the SQL statment
     * @param context the current SQL-Command context
     */
    @Override
    public void addSQL(DBSQLBuilder sql, long context)
    {
        context &= ~CTX_ALIAS; // No column aliases
        // Zusammenbauen
        expr.addSQL(sql, context);
        sql.append(op);
        // Special treatment for adding days to dates
        DataType type = expr.getDataType();
        if (type.isNumeric()==false && (value instanceof Number))
            type = DataType.DECIMAL;
        // append
        sql.appendValue(type, value, context, op);
    }

    /**
     * This function set the specified mathematical operations to the XML tag.
     * 
     * @return the XML tag (with the mathematical operations)
     */
    @Override
    public Element addXml(Element parent, long flags)
    { // Add Expressions
        expr.addXml(parent, flags);
        // Add Vaue
        if (value instanceof DBColumnExpr)
            ((DBColumnExpr) value).addXml(parent, flags);
        // done
        return parent;
    }

    @Override
    public String toString()
    { // Get a unique Name
        String name = expr.toString();
        name += "_X_";
        name += value.toString();
        // name
        return name;
    }

}