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
import java.util.Set;

import org.apache.empire.commons.ArrayMap;
import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBColumnExpr;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.DBSQLBuilder;
import org.apache.empire.exceptions.InvalidArgumentException;

/**
 * This class is used to create a SQL CASE constraint in the form of 
 *      case {expr} when {value1} then {result1}
 *                  when {value2} then {result2}
 *                  ... 
 *      else {defaultResult} end"
 * 
 * <P>
 * There is no need to explicitly create instances of this class.<BR>
 * Instead use {@link DBDatabase#caseMap(DBColumnExpr, Object, Object, Object) }
 * <P>
 * @author doebele
 */
public class DBCaseMapExpr extends DBCaseExpr
{
    // *Deprecated* private static final long serialVersionUID = 1L;
  
    private final DBColumnExpr columnExpr;
    private final Map<Object, Object> valueMap;
    private final Object elseValue;
    
    /**
     * constructs a DBCaseSelectExpr expr
     * @param colExpr the source column
     * @param valueMap the value Map
     * @param elseValue the else Expression
     */
    @SuppressWarnings("unchecked")
    public DBCaseMapExpr(DBColumnExpr columnExpr, Map<? extends Object, ? extends Object> valueMap, Object elseValue)
    {
        if (columnExpr==null)
            throw new InvalidArgumentException("colExpr", columnExpr);
        if (valueMap==null || (valueMap.isEmpty() && isNull(elseValue)))
            throw new InvalidArgumentException("valueMap", valueMap);
        // set
        this.columnExpr = columnExpr;
        this.valueMap = (Map<Object, Object>)valueMap;
        this.elseValue = elseValue;
        // init
        init(columnExpr.getSourceColumn(), valueMap, elseValue);
    }

    public DBCaseMapExpr(DBColumnExpr columnExpr, Object cmpVal, Object trueValue, Object elseValue)
    {
        if (columnExpr==null)
            throw new InvalidArgumentException("colExpr", columnExpr);
        if (isNull(trueValue) && isNull(elseValue))
            throw new InvalidArgumentException("colExpr", columnExpr);
        this.columnExpr = columnExpr;
        this.valueMap = new ArrayMap<Object, Object>(1);
        this.valueMap.put(cmpVal, trueValue);
        this.elseValue = elseValue; 
        // init
        init(columnExpr.getSourceColumn(), valueMap, elseValue);
    }

    public DBCaseMapExpr(DBColumnExpr columnExpr, Object cmpVal1, Object trueValue1, Object cmpVal2, Object trueValue2, Object elseValue)
    {
        if (columnExpr==null)
            throw new InvalidArgumentException("colExpr", columnExpr);
        if (isNull(trueValue1) && isNull(trueValue2) && isNull(elseValue))
            throw new InvalidArgumentException("colExpr", columnExpr);
        this.columnExpr = columnExpr;
        this.valueMap = new ArrayMap<Object, Object>(2);
        this.valueMap.put(cmpVal1, trueValue1);
        this.valueMap.put(cmpVal2, trueValue2);
        this.elseValue = elseValue; 
        // init
        init(columnExpr.getSourceColumn(), valueMap, elseValue);
    }

    @Override
    public String getName()
    {
        return "CASE_"+columnExpr.getName();
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
        if (other instanceof DBCaseMapExpr)
        {   // Compare
            DBCaseMapExpr otherCase = (DBCaseMapExpr)other;
            // Expression must match
            if (!columnExpr.equals(otherCase.columnExpr))
                return false;
        }
        return false;
    }

    @Override
    public void addReferencedColumns(Set<DBColumn> list)
    {
        columnExpr.addReferencedColumns(list);
        for (Object expr : valueMap.values())
        {
            if (expr instanceof DBColumnExpr)
               ((DBColumnExpr)expr).addReferencedColumns(list);
        }
        if (elseValue instanceof DBColumnExpr)
           ((DBColumnExpr)elseValue).addReferencedColumns(list);
    }

    @Override
    public void addSQL(DBSQLBuilder sql, long context)
    {
        context &= ~CTX_ALIAS; // No column aliases
        // append case 
        if (!valueMap.isEmpty())
        {   // append case 
            sql.append("CASE ");
            columnExpr.addSQL(sql, context);
            // add values
            for (Map.Entry<Object, Object> entry : valueMap.entrySet())
            {
                // append value
                sql.append(" WHEN ");
                sql.appendValue(columnExpr.getDataType(), entry.getKey(), context);
                sql.append(" THEN ");
                sql.appendValue(getDataType(), entry.getValue(), context);
            }
            sql.append(" ELSE ");
        }
        // append else
        sql.appendValue(getDataType(), elseValue, context);
        // append end
        if (!valueMap.isEmpty())
        {
            sql.append(" END");
        }
    }

}
