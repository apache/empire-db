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

import java.util.Set;

import org.apache.empire.data.DataType;
import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBColumnExpr;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.DBSQLBuilder;
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
public class DBCaseExpr extends DBColumnExpr
{
    // *Deprecated* private static final long serialVersionUID = 1L;
  
    private final DBCompareExpr compExpr;
    private final DBColumnExpr  trueExpr;
    private final DBColumnExpr  elseExpr;
    
    /**
     * Constructs a DBCaseExpr
     * @param compExpr the condition to be evaluated
     * @param trueExpr the expression returned if the condition is true
     * @param elseExpr the expression returned if the condition is false (may be null)
     */
    public DBCaseExpr(DBCompareExpr compExpr, DBColumnExpr trueExpr, DBColumnExpr elseExpr)
    {
        this.compExpr = compExpr;
        this.trueExpr = trueExpr;
        this.elseExpr = elseExpr; 
    }

    @SuppressWarnings("unchecked")
    @Override
    public final DBDatabase getDatabase()
    {
        return trueExpr.getDatabase();
    }

    @Override
    public DataType getDataType()
    {
        return trueExpr.getDataType();
    }

    @Override
    public Class<Enum<?>> getEnumType()
    {
        return trueExpr.getEnumType();
    }

    @Override
    public String getName()
    {
        return trueExpr.getName();
    }

    @Override
    public DBColumn getSourceColumn()
    {
        return trueExpr.getUpdateColumn();
    }

    @Override
    public DBColumn getUpdateColumn()
    {
        return trueExpr.getUpdateColumn();
    }

    @Override
    public boolean isAggregate()
    {
        return trueExpr.isAggregate();
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
        if (other instanceof DBCaseExpr)
        {   // Compare
            DBCaseExpr otherCase = (DBCaseExpr)other;
            // Expression must match
            if (!compExpr.equals(otherCase.compExpr))
                return false;
            if (!trueExpr.equals(otherCase.trueExpr))
                return false;
            // finally compare elseExpr
            if (elseExpr==null)
                return (otherCase.elseExpr==null);
            return elseExpr.equals(otherCase.elseExpr);
        }
        return false;
    }

    @Override
    public void addReferencedColumns(Set<DBColumn> list)
    {
        trueExpr.addReferencedColumns(list);
        compExpr.addReferencedColumns(list);
        if (elseExpr!=null)
            elseExpr.addReferencedColumns(list);
    }

    @Override
    public void addSQL(DBSQLBuilder sql, long context)
    {
        context &= ~CTX_ALIAS; // No column aliases
        sql.append("CASE WHEN ");
        compExpr.addSQL(sql, context);
        sql.append( " THEN ");
        trueExpr.addSQL(sql, context);
        sql.append(" ELSE ");
        if (elseExpr!=null)
        {   // Else
            elseExpr.addSQL(sql, context);
        }
        else
        {   // Append NULL
            sql.append("NULL");
        }
        sql.append(" END");
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

}
