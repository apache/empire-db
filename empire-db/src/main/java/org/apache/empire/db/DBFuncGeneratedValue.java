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
import java.util.List;
import java.util.Set;

import org.apache.empire.data.Column;
import org.apache.empire.data.DataType;
import org.apache.empire.data.Record;
import org.apache.empire.exceptions.InvalidArgumentException;
import org.apache.empire.exceptions.ItemNotFoundException;

/**
 * DBFuncGeneratedValue
 * Auto-generates a record value from other record fields
 * The template must contain the column names wrapped in square brackets
 * like e.g. "normalize([name])" or "[lastName], [firstName]"
 * @author doebele
 */
public class DBFuncGeneratedValue extends DBGeneratedValue
{
    private final List<Object> parts;
    
    public DBFuncGeneratedValue(DBRowSet table, String template)
    {
        super(table.getDatabase());
        this.parts = new ArrayList<Object>();
        int idx=0;
        while (idx<template.length())
        {
            int nxt=template.indexOf('[', idx);
            if (nxt<0)
            {   // append rest
                parts.add(template.substring(idx));
                break;
            }
            // append
            parts.add(template.substring(idx, nxt++));
            // find end
            int end=template.indexOf(']', nxt);
            if (end<0)
                throw new InvalidArgumentException("template", template);
            // find column
            String name = template.substring(nxt, end);
            DBColumn column = table.getColumn(name);
            if (column==null)
                throw new ItemNotFoundException(name);
            // found
            parts.add(column);
            idx = end+1;
        }
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.empire.db.DBGeneratedValue#isModified(org.apache.empire.data.Record)
     */
    @Override
    public boolean isModified(Record record)
    {
        for (Object part : parts)
        {   // check modified
            if ((part instanceof Column) && record.wasModified((Column)part))
                return true;
        }
        return false;
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.empire.db.DBGeneratedValue#eval(org.apache.empire.data.Record)
     */
    @Override
    public Object eval(Record record)
    {
        for (Object part : parts)
        {   // check null
            if ((part instanceof Column) && record.isNull((Column)part))
                return null;
        }
        // build now
        DBSQLBuilder sql = db.getDbms().createSQLBuilder();
        for (Object part : parts)
        {
            if (part instanceof Column)
            {   // append column value
                Column col = (Column)part;
                Object value = record.get(col);
                sql.appendValue(col.getDataType(), value);
            }
            else
                sql.append(part.toString());
        }
        // done
        return db.getValueExpr(sql.toString(), DataType.UNKNOWN);
    }

    /*
     * (non-Javadoc)
     * @see org.apache.empire.db.DBExpr#addReferencedColumns(java.util.Set)
     */
    @Override
    public void addReferencedColumns(Set<DBColumn> list)
    {
        for (Object part : parts)
        {
            if (part instanceof DBColumn)
                list.add((DBColumn)part);
        }
    }

    /*
     * (non-Javadoc)
     * @see org.apache.empire.db.DBExpr#addSQL(org.apache.empire.db.DBSQLBuilder, long)
     */
    @Override
    public void addSQL(DBSQLBuilder sql, long context)
    {
        for (Object part : parts)
        {
            if (part instanceof Column)
            {   // append column value
                DBColumn col = (DBColumn)part;
                col.addSQL(sql, context);
            }
            else
                sql.append(part.toString());
        }
    }
}
