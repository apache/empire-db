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
import java.util.Set;

import org.apache.empire.data.DataType;
import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBColumnExpr;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.DBExpr;
import org.w3c.dom.Element;


/**
 * This class is used for performing calculations in SQL<br>
 * It handles the mathematical operations ("+", "-", "*", "/") for the current column.
 * <P>
 * There is no need to explicitly create instances of this class.<BR>
 * Instead use {@link DBColumnExpr#plus(Object) }, {@link DBColumnExpr#minus(Object) },
 * {@link DBColumnExpr#multiplyWith(Object) }, {@link DBColumnExpr#divideBy(Object) }
 * <P>
 */
public class DBCalcExpr extends DBColumnExpr
{
    private final DBColumnExpr expr;
    private final String       op;
    private final Object       value;

    /**
     * Constructs a DBCalcExpr object Sets the mathematical operations ("+", "-", "*", "/") <br>
     * for the specified DBColumnExpr object and value.
     * 
     * @param expr an DBColumnExpr object, one column
     * @param op the mathematical operation ("+", "-", "*" or "/")
     * @param value the multiply, divide, summate or subtract value
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
    @Override
    public DBDatabase getDatabase()
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
        return DataType.DECIMAL;
    }

    /** Returns the given expression name. */
    @Override
    public String getName()
    {   // Get the expression name
        return expr.getName();
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

    /** returns null */
    @Override
    public DBColumn getUpdateColumn()
    {
        return null;
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
     * @param buf the SQL statment
     * @param context the current SQL-Command context
     */
    @Override
    public void addSQL(StringBuilder buf, long context)
    {
        // Zusammenbauen
        expr.addSQL(buf, context);
        buf.append(op);
        buf.append(getObjectValue(getDataType(), value, context, op));
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