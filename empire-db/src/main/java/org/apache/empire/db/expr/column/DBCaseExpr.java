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

import java.util.Map;

import org.apache.empire.data.DataType;
import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBColumnExpr;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.DBExpr;
import org.apache.empire.db.expr.compare.DBCompareColExpr;
import org.apache.empire.db.expr.compare.DBCompareExpr;
import org.apache.empire.xml.XMLUtil;
import org.w3c.dom.Element;

/**
 * This class is used to add the "case when ?=A then X else Y end" statement to the SQL-Command.
 * <P>
 * There is no need to explicitly create instances of this class.<BR>
 * Instead use {@link DBColumnExpr#when(DBCompareExpr, Object) }
 * <P>
 * @author doebele
 */
public abstract class DBCaseExpr extends DBColumnExpr
{
    // detect 
    private DBColumn sourceColumn = null;
    private DataType dataType = DataType.UNKNOWN;
    private Class<Enum<?>> enumType = null;
    private boolean aggregateFunc = false;
    
    protected DBCaseExpr()
    {
        /* Nothing yet */
    }

    @SuppressWarnings("unchecked")
    @Override
    public final DBDatabase getDatabase()
    {
        return sourceColumn.getDatabase();
    }

    @Override
    public DataType getDataType()
    {
        return dataType;
    }

    @Override
    public Class<Enum<?>> getEnumType()
    {
        return enumType;
    }

    @Override
    public DBColumn getSourceColumn()
    {
        return sourceColumn;
    }

    @Override
    public DBColumn getUpdateColumn()
    {
        return null;
    }

    @Override
    public boolean isAggregate()
    {
        return aggregateFunc;
    }

    @Override
    public Element addXml(Element parent, long flags)
    {
        Element elem = XMLUtil.addElement(parent, "column");
        elem.setAttribute("name", getName());
        // Add Other Attributes
        if (attributes!=null)
            attributes.addXml(elem, flags);
        // add All Options
        if (options!=null)
            options.addXml(elem, getDataType());
        // Done
        elem.setAttribute("function", "case");
        return elem;
    }

    /**
     * helper to check if an expression is null
     * @param value
     * @return true if null or false otherwise
     */
    protected boolean isNull(Object value)
    {
        return (value==null || ((value instanceof DBValueExpr) && ((DBValueExpr)value).getValue()==null));
    }

    /**
     * helper to check if an expression is not null
     * @param value
     * @return true if not null or false otherwise
     */
    protected boolean isNotNull(Object value)
    {
        return !isNull(value);
    }

    /**
     * Init case expression. 
     * Must be called from all constructors!
     * @param sourceColumn
     * @param valueMap
     * @param elseValue
     */
    protected void init(DBColumn sourceColumn, Map<?,?> valueMap, Object elseValue)
    {
        this.sourceColumn = sourceColumn;
        for (Map.Entry<?, ?> entry : valueMap.entrySet())
        {   // check compare expr
            registerCompareValue(entry.getKey());
            registerTargetValue (entry.getValue());
        }
        registerTargetValue(elseValue);
    }
    
    private void registerCompareValue(Object value)
    {
        if (value instanceof DBCompareColExpr)
            value = ((DBCompareColExpr)value).getColumnExpr();
        if (!(value instanceof DBColumnExpr))
            return; // not a function
        DBColumnExpr colExpr = (DBColumnExpr)value;
        if (this.sourceColumn==null && colExpr.getSourceColumn()!=null)
            this.sourceColumn = colExpr.getSourceColumn();
        if (colExpr.isAggregate())
            this.aggregateFunc = true;
    }

    @SuppressWarnings("unchecked")
    private void registerTargetValue(Object value)
    {
        if (isNull(value))
            return;
        // check
        if (value instanceof DBColumnExpr)
        {   // Column Expression
            DBColumnExpr colExpr = ((DBColumnExpr)value);
            if (this.dataType==DataType.UNKNOWN)
                this.dataType = colExpr.getDataType();
            if (this.enumType== null && colExpr.getEnumType()!=null)
                this.enumType = colExpr.getEnumType();
            if (colExpr.isAggregate())
                this.aggregateFunc = true;
        }
        else if (!(value instanceof DBExpr))
        {   // Simple Value
            if (this.dataType==DataType.UNKNOWN)
                this.dataType = DataType.fromJavaType(value.getClass());
            if (this.enumType== null && (value instanceof Enum<?>))
                this.enumType = (Class<Enum<?>>)value.getClass();
        }
    }
    
}
