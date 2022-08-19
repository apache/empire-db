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

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.empire.data.DataType;
import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBColumnExpr;
import org.apache.empire.db.DBExpr;
import org.apache.empire.db.DBSQLBuilder;
import org.apache.empire.dbms.DBSqlPhrase;
import org.apache.empire.exceptions.InvalidArgumentException;

/**
 * This class is used to decode a set of keys to the corresponding target values.
 * For most drivers this will be performed by the "case ? when A then X else Y end" statement.
 * <P>
 * There is no need to explicitly create instances of this class.<BR>
 * Instead use {@link DBColumnExpr#when(org.apache.empire.db.expr.compare.DBCompareExpr, Object) }
 * <P>
 * @author doebele
 */
public class DBDecodeExpr extends DBAbstractFuncExpr
{
    // *Deprecated* private static final long serialVersionUID = 1L;
  
    private final Map<?,?>  valueMap;
    private final Object    elseExpr;
    
    /**
     * Constructs a DBDecodeExpr
     * @param expr the expression to be decoded
     * @param valueMap a map of keys and values used for decoding
     * @param elseExpr the expression returned if the condition is false (may be null)
     * @param dataType the target data type
     */
    public DBDecodeExpr(DBColumnExpr expr, Map<?,?> valueMap, Object elseExpr, DataType dataType)
    {
        super(expr, false, dataType);
        // Save Info
        this.valueMap = valueMap;
        this.elseExpr = elseExpr; 
    }

    @Override
    protected String getFunctionName()
    {
        return "DECODE";
    }

    @Override
    public DBColumn getUpdateColumn()
    {
        return null;
    }

    @Override
    public Class<Enum<?>> getEnumType()
    {
        DBColumnExpr firstExpr = getFirstColumnExpr();
        if (firstExpr!=null)
            return firstExpr.getEnumType();
        return super.getEnumType();
    }
    
    @Override
    public void addReferencedColumns(Set<DBColumn> list)
    {
        expr.addReferencedColumns(list);
        for (Map.Entry<?,?> e : valueMap.entrySet())
        {   // Check Key of Value for Expressions
            if (e.getKey() instanceof DBExpr)
                ((DBExpr)e.getKey()).addReferencedColumns(list);
            if (e.getValue() instanceof DBExpr)
                ((DBExpr)e.getValue()).addReferencedColumns(list);
        }
        if (elseExpr instanceof DBExpr)
           ((DBExpr)elseExpr).addReferencedColumns(list);
    }

    @Override
    public void addSQL(DBSQLBuilder sql, long context)
    {
        // decode
        String template = sql.getPhrase(DBSqlPhrase.SQL_FUNC_DECODE);
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
                // find end
                int end = ++pos;
                for (end=++pos;end<len;end++)
                {   // find terminator
                    if (template.charAt(end)=='}')
                        break;
                }
                if (end>=len)
                    throw new InvalidArgumentException("template", template);
                // Add parts
                addDecodeParts(sql);
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
    
    public void addDecodeParts(DBSQLBuilder sql)
    {
        // Append parts
        for (Iterator<?> i = valueMap.keySet().iterator(); i.hasNext();)
        {
            Object key = i.next();
            Object val = valueMap.get(key);

            sql.append(DBSqlPhrase.SQL_FUNC_DECODE_SEP);
            
            Object[] keyVal = new Object[] { key, val };
            DataType[] dataTypes = new DataType[] { expr.getDataType(), this.getDataType() };
            
            String part = sql.getPhrase(DBSqlPhrase.SQL_FUNC_DECODE_PART);
            sql.appendTemplate(part, keyVal, dataTypes, CTX_DEFAULT, "");
        }
        // Generate other
        if (elseExpr != null)
        { // Else
            sql.append(DBSqlPhrase.SQL_FUNC_DECODE_SEP);
            // else
            String other = sql.getPhrase(DBSqlPhrase.SQL_FUNC_DECODE_ELSE);
            sql.appendTemplate(other, new Object[] { elseExpr }, new DataType[] { getDataType() }, CTX_DEFAULT, "");
        }
    }
    
    private DBColumnExpr getFirstColumnExpr()
    {
        for (Object val : valueMap.values())
        {
            if (val instanceof DBColumnExpr)
                return (DBColumnExpr)val;
        }
        if (elseExpr instanceof DBColumnExpr)
            return (DBColumnExpr)elseExpr;
        // No DBColumnExpr found
        return null;
    }
}
