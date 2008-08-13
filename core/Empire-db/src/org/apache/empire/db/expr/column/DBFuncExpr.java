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
import org.apache.empire.xml.XMLUtil;
import org.w3c.dom.Element;


/**
 * This class is used for performing various SQL functions on a column or column expression. 
 * <P>
 * There is no need to explicitly create instances of this class.<BR>
 * Instead use any of the following functions:<BR>
 * {@link DBColumnExpr#abs() }, {@link DBColumnExpr#coalesce(Object) }, {@link DBColumnExpr#convertTo(DataType) }, 
 * {@link DBColumnExpr#decode(java.util.Map, Object) }, {@link DBColumnExpr#lower() }, {@link DBColumnExpr#min() }, 
 * {@link DBColumnExpr#max() }, {@link DBColumnExpr#month() }, {@link DBColumnExpr#sum() }, 
 * {@link DBColumnExpr#trim() }, {@link DBColumnExpr#upper() }, {@link DBColumnExpr#year() } 
 * <P>
 * @author ESTEAM software <A TARGET="esteam" HREF="http://www.esteam.de">www.esteam.de </A>
 */
public class DBFuncExpr extends DBColumnExpr
{
    protected final DBColumnExpr expr;
    protected final DBColumn     updateColumn; // optional

    protected final String       prefix;
    protected final String       postfix;
    protected final boolean      isAggregate;
    protected final DataType     dataType;

    /**
     * Constructs a new DBFuncExpr object set the specified parameters to this object.
     * Do not use directly - use any of the DBColumnExpr.??? factory functions instead!
     * 
     * The sql function string is built from a string template.
     * The template string must contain a ? which is a placeholder for the column expression.
     * 
     * @param expr the DBColumnExpr object
     * @param template specifies a template for the expression. The template must contain a ? placeholder for the column expression
     * @param updateColumn optional update column if any. This parameter may be null
     * @param isAggregate indicates whether the function is an aggregate function (sum, min, max, avg, ...)
     * @param dataType indicates the data type of the function result 
     */
    public DBFuncExpr(DBColumnExpr expr, String template, DBColumn updateColumn, boolean isAggregate, DataType dataType)
    {
        this.expr = expr;
        this.dataType = dataType;
        this.isAggregate = isAggregate;
        this.updateColumn = updateColumn;

        int sep = template.indexOf("?");
        if (sep >= 0)
        {
            prefix = template.substring(0, sep);
            postfix = template.substring(sep + 1);
        } 
        else
        {
            prefix = template;
            postfix = "";
        }
    }

    /**
     * Constructs a new DBFuncExpr object set the specified parameters to this object.
     * Do not use directly - use any of the DBColumnExpr.??? factory functions instead!
     * 
     * The function sql string is built from a template string.
     * The template string must contain a ? which is a placeholder for the column expression.
     * 
     * @param expr the DBColumnExpr object
     * @param template specifies a template for the expression. The template must contain a ? placeholder for the column expression
     * @param updateColumn optional update column if any. This parameter may be null
     * @param isAggregate indicates whether the function is an aggregate function (sum, min, max, avg, ...)
     */
    public DBFuncExpr(DBColumnExpr expr, String template, DBColumn updateColumn, boolean isAggregate)
    {
        this(expr, template, updateColumn, isAggregate, expr.getDataType());
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
     * Returns the data type of the DBColumnExpr object.
     * 
     * @return the data type
     */
    @Override
    public DataType getDataType()
    {
        return this.dataType;
    }

    /**
     * Returns the column name.
     * 
     * @return the column name
     */
    @Override
    public String getName()
    {
        return expr.getName();
    }

    @Override
    public Element addXml(Element parent, long flags)
    {
        Element elem;
        if (updateColumn!=null)
        {   // Update Column
            elem = updateColumn.addXml(parent, flags);
        }
        else
        {   // Add a column expression for this function
            elem = XMLUtil.addElement(parent, "column");
            elem.setAttribute("name", getName());
            // Add Other Attributes
            if (attributes!=null)
                attributes.addXml(elem, flags);
            // add All Options
            if (options!=null)
                options.addXml(elem, flags);
        }
        // Done
        elem.setAttribute("function", getFunctionName());
        return elem;
    }
    
    private String getFunctionName()
    {
        String s = prefix.trim();
        int i=0;
        for (; i<s.length(); i++)
            if (s.charAt(i)<'A')
                break;
        // return name 
        return (i>0) ? s.substring(0,i) : postfix.trim();
    }

    /**
     * Returns the DBColunm object.
     * 
     * @return the DBColunm object
     */
    @Override
    public DBColumn getUpdateColumn()
    {
        return updateColumn;
    }

    /**
     * Returns whether the function is an aggegation function<br>
     * that combines multiple rows to one result row.
     * 
     * @return true if the function is an aggregate or false otherwise
     */
    @Override
    public boolean isAggregate()
    {
        return isAggregate || expr.isAggregate();
    }

    /**
     * @see org.apache.empire.db.DBExpr#addReferencedColumns(Set)
     */
    @Override
    public void addReferencedColumns(Set<DBColumn> list)
    {
        expr.addReferencedColumns(list);
    }

    /**
     * Creates the SQL-Command adds a function to the SQL-Command.
     * 
     * @param buf the SQL-Command
     * @param context the current SQL-Command context
     */
    @Override
    public void addSQL(StringBuilder buf, long context)
    { // Expression
        buf.append(prefix);
        expr.addSQL(buf, context & ~CTX_ALIAS );
        buf.append(postfix);
    }
}