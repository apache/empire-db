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

import org.apache.empire.commons.StringUtils;
import org.apache.empire.data.DataType;
import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBColumnExpr;
import org.apache.empire.db.DBDatabaseDriver;
import org.apache.empire.db.DBExpr;

/**
 * This class is used to decode a set of keys to the corresponding target values.
 * For most drivers this will be performed by the "case ? when A then X else Y end" statement.
 * <P>
 * There is no need to explicitly create instances of this class.<BR>
 * Instead use {@link DBColumnExpr#when() }
 * <P>
 * @author doebele
 */
public class DBDecodeExpr extends DBAbstractFuncExpr
{
    private final Map<?,?>  valueMap;
    private final Object    elseExpr;
    
    /**
     * Constructs a DBDecodeExpr
     * @param compExpr the condition to be evaluated
     * @param expr the expression returned if the condition is true
     * @param elseExpr the expression returned if the condition is false (may be null)
     */
    public DBDecodeExpr(DBColumnExpr expr, Map<?,?> valueMap, Object elseExpr, DataType dataType)
    {
        super(expr, expr.getUpdateColumn(), false, dataType);
        // Save Info
        this.valueMap = valueMap;
        this.elseExpr = elseExpr; 
    }

    @Override
    protected String getFunctionName()
    {
        return "decode";
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
    public void addSQL(StringBuilder sql, long context)
    {
        DBDatabaseDriver driver = getDatabase().getDriver();
        StringBuilder inner = new StringBuilder();
        // Generate parts
        for (Iterator<?> i = valueMap.keySet().iterator(); i.hasNext();)
        {
            Object key = i.next();
            Object val = valueMap.get(key);

            String part = driver.getSQLPhrase(DBDatabaseDriver.SQL_FUNC_DECODE_PART);
            part = StringUtils.replaceAll(part, "{0}", getObjectValue(expr.getDataType(), key, DBExpr.CTX_DEFAULT, ""));
            part = StringUtils.replaceAll(part, "{1}", getObjectValue(this.getDataType(), val, DBExpr.CTX_DEFAULT, ""));

            inner.append(driver.getSQLPhrase(DBDatabaseDriver.SQL_FUNC_DECODE_SEP));
            inner.append(part);
        }
        // Generate other
        if (elseExpr != null)
        { // Else
            String other = driver.getSQLPhrase(DBDatabaseDriver.SQL_FUNC_DECODE_ELSE);
            other = StringUtils.replaceAll(other, "{0}", getObjectValue(getDataType(), elseExpr, DBExpr.CTX_DEFAULT, ""));

            inner.append(driver.getSQLPhrase(DBDatabaseDriver.SQL_FUNC_DECODE_SEP));
            inner.append(other);
        }
        DBValueExpr param = new DBValueExpr(getDatabase(), inner, DataType.UNKNOWN); 
        // Set Params
        String template = driver.getSQLPhrase(DBDatabaseDriver.SQL_FUNC_DECODE);
        super.addSQL(sql, template, new Object[] { param }, context);
    }

}
