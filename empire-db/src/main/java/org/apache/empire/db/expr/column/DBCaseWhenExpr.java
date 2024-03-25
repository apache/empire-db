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

import org.apache.empire.commons.ArrayMap;
import org.apache.empire.commons.ArraySet;
import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBExpr;
import org.apache.empire.db.DBSQLBuilder;
import org.apache.empire.db.expr.compare.DBCompareExpr;
import org.apache.empire.exceptions.InvalidArgumentException;

/**
 * This class is used to create a SQL CASE constraint in the form of 
 *      case when {cond1} then {result1}
 *           when {cond2} then {result2}
 *           ... 
 *      else {defaultResult} end"
 * 
 * There is no need to explicitly create instances of this class.<BR>
 * 
 * @author doebele
 */
public class DBCaseWhenExpr extends DBCaseExpr
{
    // *Deprecated* private static final long serialVersionUID = 1L;
  
    private final Map<DBCompareExpr, Object> whenMap;
    private final Object elseValue;
    
    /**
     * Constructs a DBCaseExpr
     * @param whenMap a map of compareExpressions with the corresponding result values
     * @param elseValue the expression returned if no condition is true (may be null)
     */
    @SuppressWarnings("unchecked")
    public DBCaseWhenExpr(Map<DBCompareExpr, ? extends Object> whenMap, Object elseValue)
    {   // check params
        if (whenMap==null || (whenMap.isEmpty() && ObjectUtils.isEmpty(elseValue)))
            throw new InvalidArgumentException("whenMap | elseValue", null);
        // set
        this.whenMap  = (Map<DBCompareExpr, Object>)whenMap;
        this.elseValue = elseValue; 
        // init
        init(null, whenMap, elseValue);
    }
    
    public DBCaseWhenExpr(DBCompareExpr cmpExpr, Object trueExpr, Object elseValue)
    {   // check params
        if (cmpExpr==null || (isNull(trueExpr) && isNull(elseValue)))
            throw new InvalidArgumentException("cmpExpr | trueExpr | elseValue", null);
        // set
        this.whenMap  = new ArrayMap<DBCompareExpr, Object>(1);
        this.whenMap.put(cmpExpr, trueExpr);
        this.elseValue = elseValue;
        // init
        init(null, whenMap, elseValue);
    }
    
    @Override
    public String getName()
    {
        StringBuilder name = new StringBuilder(40); 
        name.append("CASE");
        if (!whenMap.isEmpty())
        {   // All columns of first compare expression
            DBCompareExpr firstCmpExpr = whenMap.keySet().iterator().next();
            Set<DBColumn> cols = new ArraySet<DBColumn>(1);
            firstCmpExpr.addReferencedColumns(cols);
            // build name
            for (DBColumn col : cols)
            {
                name.append("_");
                // name.append(col.getRowSet().getName());
                // name.append("_");
                name.append(col.getName());
            }
        }
        return name.toString();
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
        if (other instanceof DBCaseWhenExpr)
        {   // Compare
            DBCaseWhenExpr otherCase = (DBCaseWhenExpr)other;
            // Expression must match
            if (whenMap.size()!=otherCase.whenMap.size())
                return false;
            // empty
            if (whenMap.isEmpty())
            {   // compare elseValue
                return ObjectUtils.compareEqual(elseValue, otherCase.elseValue);
            }
            // check all keys
            Iterator<DBCompareExpr> thisIterator  = whenMap.keySet().iterator();
            Iterator<DBCompareExpr> otherIterator = otherCase.whenMap.keySet().iterator();
            while (thisIterator.hasNext())
            {   // Compare
                DBCompareExpr thisCmpExpr = thisIterator.next();
                DBCompareExpr otherCmpExpr = otherIterator.next();
                if (!thisCmpExpr.equals(otherCmpExpr))
                    return false;
            }
            return true;
        }
        return false;
    }

    @Override
    public void addReferencedColumns(Set<DBColumn> list)
    {
        for (Map.Entry<DBCompareExpr, Object> entry : whenMap.entrySet())
        {
            if (entry.getKey()!=null)
                entry.getKey().addReferencedColumns(list);
            if (entry.getValue() instanceof DBExpr)
                ((DBExpr)entry.getValue()).addReferencedColumns(list);
        }
        if (elseValue instanceof DBExpr)
            ((DBExpr)elseValue).addReferencedColumns(list);
    }

    @Override
    public void addSQL(DBSQLBuilder sql, long context)
    {
        context &= ~CTX_ALIAS; // No column aliases
        // append case 
        if (!whenMap.isEmpty())
        {   // add case
            sql.append("CASE");
            for (Map.Entry<DBCompareExpr, Object> entry : whenMap.entrySet())
            {
                sql.append(" WHEN ");
                DBCompareExpr compExpr = entry.getKey();
                compExpr.addSQL(sql, context);
                sql.append(" THEN ");
                sql.appendValue(getDataType(), entry.getValue(), context);
            }
            sql.append(" ELSE ");
        }
        // append else
        sql.appendValue(getDataType(), elseValue, context);
        // append end
        if (!whenMap.isEmpty())
        {
            sql.append(" END");
        }
    }

}
