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

import java.util.Collection;
import java.util.Map;

import org.apache.empire.data.DataType;
import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBColumnExpr;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.DBExpr;
import org.apache.empire.db.DBRowSet;
import org.apache.empire.db.expr.compare.DBCompareColExpr;
import org.apache.empire.db.expr.compare.DBCompareExpr;
import org.apache.empire.exceptions.InvalidPropertyException;
import org.apache.empire.xml.XMLUtil;
import org.w3c.dom.Element;

/**
 * This class represents a SQL case expression
 * like "case when ?=A then X else Y end"
 *   or "case ? when A then X else Y end"
 * <P>
 * This abstract class is implemented by DBCaseMapExpr and DBCaseWhenExpr
 * <P>
 * @author doebele
 */
public abstract class DBCaseExpr extends DBColumnExpr
{
    // detect 
    private DBRowSet rowset = null;
    private DBColumn updateColumn = null;
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
        return rowset.getDatabase();
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
    public DBRowSet getRowSet()
    {
        return rowset;
    }

    @Override
    public DBColumn getUpdateColumn()
    {
        return updateColumn;
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
     * @param caseExpr the case expr (if any)
     * @param valueMap the value or conditions map
     * @param elseValue the else value
     */
    protected void init(DBColumnExpr caseExpr, Map<?,?> valueMap, Object elseValue)
    {
        /*
         * Important: caseExpr is not the sourceColumn
         * sourceColumn must be set from target values!
         */
        if (caseExpr!=null)
        {   // init from caseExpr
            this.rowset = caseExpr.getRowSet();
            this.aggregateFunc = caseExpr.isAggregate();
        }
        // find source column
        DBColumnExpr sourceExpr = getSourceColumnExpr(valueMap.values(), elseValue);
        if (sourceExpr!=null)
        {   // set type
            this.rowset = sourceExpr.getRowSet();
            this.updateColumn = sourceExpr.getUpdateColumn();
            this.dataType = sourceExpr.getDataType();
            this.enumType = sourceExpr.getEnumType();
        }
        // Check rest
        for (Map.Entry<?, ?> entry : valueMap.entrySet())
        {   // key
            Object key = entry.getKey();
            if (key instanceof DBCompareExpr && this.rowset==null)
                this.rowset = ((DBCompareExpr)key).getRowSet();
            if (key instanceof DBCompareColExpr)
                key = ((DBCompareColExpr)key).getColumnExpr();
            if (key instanceof DBColumnExpr && ((DBColumnExpr)key).isAggregate())
                this.aggregateFunc = true;
            // value
            Object value = entry.getValue(); 
            if ((value instanceof DBColumnExpr) && ((DBColumnExpr)entry.getValue()).isAggregate())
                this.aggregateFunc = true;
            if (dataType==DataType.UNKNOWN)
                initDataTypeFromValue(value);
        }
        if (dataType==DataType.UNKNOWN)
            initDataTypeFromValue(elseValue);
        // Check
        if (this.rowset==null)
            throw new InvalidPropertyException("rowset", rowset);
    }
    
    protected DBColumnExpr getSourceColumnExpr(Collection<?> values, Object elseValue)
    {
        for (Object val : values)
        {
            if (val instanceof DBColumnExpr)
                return (DBColumnExpr)val;
        }
        if (elseValue instanceof DBColumnExpr)
            return (DBColumnExpr)elseValue;
        // No DBColumnExpr found
        return null;
    }
    
    @SuppressWarnings("unchecked")
    protected void initDataTypeFromValue(Object value)
    {
        // check enum
        if (value instanceof Enum)
        {   // Enum
            this.enumType = (Class<Enum<?>>)value.getClass();
            this.dataType = DataType.VARCHAR;
        }
        else if (!isNull(value) && !(value instanceof DBExpr))
        {   // normal type
            this.dataType = DataType.fromJavaType(value.getClass());
        }
    }
    
}
