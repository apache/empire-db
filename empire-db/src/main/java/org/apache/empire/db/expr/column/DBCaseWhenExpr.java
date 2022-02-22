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

import org.apache.empire.data.DataType;
import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBColumnExpr;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.expr.compare.DBCompareExpr;
import org.apache.empire.xml.XMLUtil;
import org.w3c.dom.Element;

/**
 * This class is used to create a SQL CASE constraint in the form of 
 *      case when {cond1} then {result1}
 *           when {cond2} then {result2}
 *           ... 
 *      else {defaultResult} end"
 * <P>
 * There is no need to explicitly create instances of this class.<BR>
 * Instead use {@link DBDatabase#caseWhen(Map<DBCompareExpr, DBColumnExpr>, DBColumnExpr) }
 * <P>
 * @author doebele
 */
public class DBCaseWhenExpr extends DBColumnExpr
{
    // *Deprecated* private static final long serialVersionUID = 1L;
  
    private final Map<DBCompareExpr, DBColumnExpr> whenMap;
    private final DBColumnExpr  elseExpr;
    
    /**
     * Constructs a DBCaseExpr
     * @param whenMap a map of compareExpressions with the corresponding result values
     * @param elseExpr the expression returned if no condition is true (may be null)
     */
    public DBCaseWhenExpr(Map<DBCompareExpr, DBColumnExpr> whenMap, DBColumnExpr elseExpr)
    {
        this.whenMap  = whenMap;
        this.elseExpr = elseExpr; 
    }

    @SuppressWarnings("unchecked")
    @Override
    public final DBDatabase getDatabase()
    {
        return getFirstColumnExpr().getDatabase();
    }

    @Override
    public DataType getDataType()
    {
        DBColumnExpr cexp = getFirstColumnExpr();
        return cexp.getDataType();
    }

    @Override
    public Class<Enum<?>> getEnumType()
    {
        DBColumnExpr cexp = getFirstColumnExpr();
        return cexp.getEnumType();
    }

    @Override
    public String getName()
    {
        DBCompareExpr firstCmpExpr = whenMap.keySet().iterator().next();
        StringBuilder b = new StringBuilder("CASE_");
        firstCmpExpr.addSQL(b, CTX_NAME);
        return b.toString();
    }

    @Override
    public DBColumn getSourceColumn()
    {
        DBColumnExpr cexp = getFirstColumnExpr();
        return cexp.getSourceColumn();
    }

    @Override
    public DBColumn getUpdateColumn()
    {
        return null;
    }

    @Override
    public boolean isAggregate()
    {
        return getFirstColumnExpr().isAggregate();
    }

    @Override
    public void addReferencedColumns(Set<DBColumn> list)
    {
        for (Map.Entry<DBCompareExpr, DBColumnExpr> entry : whenMap.entrySet())
        {
            if (entry.getKey()!=null)
                entry.getKey().addReferencedColumns(list);
            if (entry.getValue()!=null)
                entry.getValue().addReferencedColumns(list);
        }
        if (elseExpr!=null)
            elseExpr.addReferencedColumns(list);
    }

    @Override
    public void addSQL(StringBuilder sql, long context)
    {
        context &= ~CTX_ALIAS; // No column aliases
        // append case 
        if (!whenMap.isEmpty())
        {   // add case
            sql.append("CASE");
            for (Map.Entry<DBCompareExpr, DBColumnExpr> entry : whenMap.entrySet())
            {
                sql.append(" WHEN ");
                DBCompareExpr compExpr = entry.getKey();
                compExpr.addSQL(sql, context);
                sql.append( " THEN ");
                DBColumnExpr trueExpr = entry.getValue();
                if (trueExpr!=null)
                    trueExpr.addSQL(sql, context);
                else
                    sql.append("NULL");
            }
            sql.append(" ELSE ");
        }
        // append else
        if (elseExpr!=null)
        {   // Else
            elseExpr.addSQL(sql, context);
        }
        else
        {   // Append NULL
            sql.append("NULL");
        }
        // append end
        if (!whenMap.isEmpty())
        {
            sql.append(" END");
        }
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
    
    private DBColumnExpr getFirstColumnExpr()
    {
        for (DBColumnExpr expr : whenMap.values())
        {
            if (expr!=null)
                return expr;
        }
        return this.elseExpr;
    }

}
