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

import org.apache.empire.commons.StringUtils;
import org.apache.empire.data.DataType;
import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBColumnExpr;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.DBExpr;
import org.apache.empire.db.DBSQLBuilder;
import org.apache.empire.db.exceptions.DatabaseNotOpenException;
import org.apache.empire.dbms.DBMSHandler;
import org.apache.empire.exceptions.InvalidArgumentException;
import org.apache.empire.xml.XMLUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;


/**
 * This implements some basic functionality for SQL functions based on a column expression 
 */
public abstract class DBAbstractFuncExpr extends DBColumnExpr
{
    // *Deprecated* private static final long serialVersionUID = 1L;
    protected static final Logger log = LoggerFactory.getLogger(DBAbstractFuncExpr.class);
  
    protected final DBColumnExpr expr;
    protected final boolean      isAggregate;
    protected final DataType     dataType;

    /**
     * Constructs a new DBFuncExpr object set the specified parameters to this object.
     * Do not use directly - use any of the DBColumnExpr.??? factory functions instead!
     * 
     * @param expr the DBColumnExpr object
     * @param isAggregate indicates whether the function is an aggregate function (sum, min, max, avg, ...)
     * @param dataType indicates the data type of the function result 
     */
    public DBAbstractFuncExpr(DBColumnExpr expr, boolean isAggregate, DataType dataType)
    {
        this.expr = expr;
        this.isAggregate = isAggregate || expr.isAggregate(); 
        this.dataType = dataType;
    }

    /**
     * returns the name of the function
     * @return the function name
     */
    protected abstract String getFunctionName();
    
    /**
     * returns the Database dbms or null if the Expression is not attached to an open database<BR>
     * This function is intended for convenience only.
     */
    protected DBMSHandler getDbms()
    {
        DBDatabase db = expr.getDatabase();
        if (db==null)
            throw new InvalidArgumentException("expr", expr);
        DBMSHandler dbms = db.getDbms();
        if (dbms==null)
            throw new DatabaseNotOpenException(db);
        return dbms;
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
     * Maybe an Enum...
     */
    @Override
    public Class<Enum<?>> getEnumType()
    {
        // Override to set EnumType 
        return null; 
    }

    /**
     * Returns the column name.
     * 
     * @return the column name
     */
    @Override
    public String getName()
    {
        String exprName = expr.getName();
        String funcName = getFunctionName();
        return (StringUtils.isNotEmpty(exprName) ? exprName+"_"+funcName : funcName);
    }

    /**
     * Returns the expression the source column.
     */
    @Override
    public DBColumn getSourceColumn()
    {
        return expr.getSourceColumn();
    }

    /**
     * Returns the DBColunm object.
     * 
     * @return the DBColunm object
     */
    @Override
    public DBColumn getUpdateColumn()
    {
        return expr.getUpdateColumn();
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
        return isAggregate ;
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
     * Returns true if other is equal to this expression  
     */
    @Override
    public boolean equals(Object other)
    {
        if (other==this)
            return true;
        // Check Type
        if (other instanceof DBAbstractFuncExpr)
        {   // Compare
            DBAbstractFuncExpr otherFunc = (DBAbstractFuncExpr)other;
            // Expression must match
            if (!expr.equals(otherFunc.expr))
                return false;
            // Function must match
            String tname = getFunctionName();
            String oname = otherFunc.getFunctionName();
            return StringUtils.compareEqual(tname, oname);
        }
        return false;
    }

    /**
     * Creates the SQL-Command adds a function to the SQL-Command.
     * 
     * The sql function string is built from a string template.
     * The template string must contain a ? which is a placeholder for the column expression.
     * 
     * @param sql the SQL-Command
     * @param template the function template string. 
     * @param params an array of function parameters 
     * @param context the current SQL-Command context
     */
    /*
    public final void addSQL(StringBuilder sql, String template, Object[] params, long context)
    {
        // Get Template
        if (params != null)
        {   // Replace Params 
            for (int i = 0; i < params.length; i++)
            {   // Detect placeholder and data type
                DataType paramDataType = expr.getDataType();
                int idx;
                String ph = "{"+String.valueOf(i);
                if ((idx=template.indexOf(ph))>=0) {
                    // param found
                    idx += ph.length();
                    int end = template.indexOf('}', idx);
                    if (end<idx)
                        throw new InvalidArgumentException("template", template); 
                    // check if DataType is specified
                    if (template.charAt(idx)==':')
                    {   // Yes, get the DataType name and look it up
                        String typeName = ((end>=idx) ? template.substring(idx+1, end) : null);
                        if (StringUtils.isNotEmpty(typeName) && !typeName.equals("*"))
                            paramDataType = DataType.valueOf(typeName);
                        else if (typeName.equals("*") || params[i]==null || (params[i] instanceof DBExpr))
                            paramDataType = DataType.UNKNOWN;
                        else
                            paramDataType = DataType.fromJavaType(params[i].getClass());
                    }
                    // complete placeholder
                    ph += template.substring(idx, end+1);
                    
                } else {
                    log.info("No placeholder found in template {} for paramter {}", template, i);
                    continue;
                }
                // get param and replace      
                String paramAsString = getObjectValue(paramDataType, params[i], CTX_DEFAULT, ",");
                // template = template.replaceAll("\\{" + String.valueOf(i) + "\\}", value);
                template = StringUtils.replaceAll(template, ph, paramAsString);
            }
        }        
        // Assemble SQL
        int beg = 0;
        while (true)
        {
            int end = template.indexOf("?", beg);
            if (end < 0)
                break;
            // part
            sql.append(template.substring(beg, end));
            expr.addSQL(sql, (context & ~CTX_ALIAS));
            beg = end + 1;
        }
        if (beg < template.length())
        {   // add the rest
            sql.append(template.substring(beg));
            // special case: Nothing added yet
            if (beg==0)
                log.warn("No Placeholder for Column found in function template.");
        }
    }
    */

    public final void addSQL(DBSQLBuilder sql, String template, Object[] params, long context)
    {
        // parse template
        int pos=0, prev=0, len=template.length();
        while (pos<len)
        {
            char c = template.charAt(pos);
            // Expression
            if (c=='?') {
                if (prev<pos)
                    sql.append(template.substring(prev, pos));
                // expression
                expr.addSQL(sql, (context & ~CTX_ALIAS));
                // next
                prev = ++pos;
            }
            // Placeholder
            else if (c=='{') {
                if (prev<pos)
                    sql.append(template.substring(prev, pos));
                // find index
                int end = ++pos;
                for (;end<len;end++)
                {   // check digit
                    char digit = template.charAt(end);
                    if (digit<'0' || digit>'9')
                        break;
                }
                if (end>=len)
                    throw new InvalidArgumentException("template", template);
                // parse index
                int iParam = Integer.parseInt(template.substring(pos, end));
                if (iParam<0 || iParam>=params.length)
                    throw new InvalidArgumentException("params", params);
                // find end
                for (end=++pos;end<len;end++)
                {   // check terminator
                    if (template.charAt(end)=='}')
                        break;
                }
                if (end>=len)
                    throw new InvalidArgumentException("template", template);
                // DataType
                DataType paramDataType = expr.getDataType();
                if (template.charAt(pos)==':')
                {   // Yes, get the DataType name and look it up
                    String typeName = template.substring(pos+1, end);
                    if (StringUtils.isNotEmpty(typeName) && !typeName.equals("*"))
                        paramDataType = DataType.valueOf(typeName);
                    else if (typeName.equals("*") || params[iParam]==null || (params[iParam] instanceof DBExpr))
                        paramDataType = DataType.UNKNOWN;   /* use as literal */
                    else
                        paramDataType = DataType.fromJavaType(params[iParam].getClass());
                }
                // append value
                sql.appendValue(paramDataType, params[iParam], CTX_DEFAULT, ",");
                // next
                prev = pos = end+1;
            }
            else 
                pos++; // next
        }
        if (prev < len)
        {   // add the rest
            sql.append(template.substring(prev));
            // special case: Nothing added yet
            if (prev==0)
                log.warn("No Placeholder found in template {}", template);
        }
    }
    
    @Override
    public Element addXml(Element parent, long flags)
    {
        // Add a column expression for this function
        Element elem = XMLUtil.addElement(parent, "column");
        elem.setAttribute("name", getName());
        elem.setAttribute("function", getFunctionName());
        elem.setAttribute("dataType", getDataType().name());
        elem.setAttribute("aggregate", String.valueOf(isAggregate));
        DBColumn source = getSourceColumn();
        if (source!=null)
        {   Element elemSource = XMLUtil.addElement(elem, "source");
            source.addXml(elemSource, flags);
        }
        // Add Other Attributes
        if (attributes!=null)
            attributes.addXml(elem, flags);
        // add All Options
        if (options!=null)
            options.addXml(elem, this.dataType);
        // Done
        return elem;
    }
}