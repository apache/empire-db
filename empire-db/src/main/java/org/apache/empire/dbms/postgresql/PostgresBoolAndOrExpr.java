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
package org.apache.empire.dbms.postgresql;

import java.util.Set;

import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.data.DataType;
import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBColumnExpr;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.DBSQLBuilder;
import org.apache.empire.db.expr.compare.DBCompareAndOrExpr;
import org.apache.empire.db.expr.compare.DBCompareColExpr;
import org.apache.empire.db.expr.compare.DBCompareExpr;
import org.apache.empire.db.expr.compare.DBCompareNotExpr;
import org.apache.empire.xml.XMLUtil;
import org.w3c.dom.Element;

public class PostgresBoolAndOrExpr extends DBColumnExpr
{
    private static final String BOOL_AND         = "BOOL_AND";
    private static final String BOOL_OR          = "BOOL_OR";

    private final DBCompareExpr cmpExpr;
    private final boolean       or;
    private String name;

    public PostgresBoolAndOrExpr(DBCompareExpr cmpExpr, boolean or)
    {
        this.cmpExpr = cmpExpr;
        this.or = or;
    }

    @SuppressWarnings("unchecked")
    @Override
    public DBDatabase getDatabase()
    {
        return cmpExpr.getDatabase();
    }

    @Override
    public DataType getDataType()
    {
        return DataType.BOOL;
    }

    @Override
    public Class<Enum<?>> getEnumType()
    {
        return null;
    }

    @Override
    public DBColumn getSourceColumn()
    {
        return null;
    }

    @Override
    public String getName()
    {
        if (name==null)
        {   // Build name
            StringBuilder buf = new StringBuilder();
            appendName(buf, this.cmpExpr);
            buf.append("_");
            buf.append(or ? BOOL_OR : BOOL_AND);
            name = buf.toString();
        }
        return name;
    }

    @Override
    public boolean isAggregate()
    {
        return true;
    }

    @Override
    public DBColumn getUpdateColumn()
    {
        return null;
    }

    @Override
    public void addReferencedColumns(Set<DBColumn> list)
    {
        cmpExpr.addReferencedColumns(list);
    }

    @Override
    public void addSQL(DBSQLBuilder sql, long context)
    {

        sql.append(or ? BOOL_OR : BOOL_AND);
        sql.append("(");
        cmpExpr.addSQL(sql, context);
        sql.append(")");
    }

    @Override
    public Element addXml(Element parent, long flags)
    {
        // Add a column expression for this function
        Element elem = XMLUtil.addElement(parent, "column");
        elem.setAttribute("name", getName());
        elem.setAttribute("function", (or ? BOOL_OR : BOOL_AND));
        elem.setAttribute("dataType", getDataType().name());
        elem.setAttribute("aggregate", "true");
        return elem;
    }
    
    protected void appendName(StringBuilder buf, DBCompareExpr expr)
    {
        if (ObjectUtils.isWrapper(expr))
            appendName(buf, ObjectUtils.unwrap(expr));
        else if (expr instanceof DBCompareNotExpr)
            appendName(buf, ((DBCompareNotExpr)expr).getExpr());
        else if (expr instanceof DBCompareAndOrExpr) {
            appendName(buf, ((DBCompareAndOrExpr)expr).getLeft());
            appendName(buf, ((DBCompareAndOrExpr)expr).getRight());
        }
        else if (expr instanceof DBCompareColExpr) {
            DBColumnExpr colExpr = ((DBCompareColExpr)expr).getColumnExpr();
            if (buf.length()>0)
                buf.append("_");
            buf.append(colExpr.getName());
        }
    }

}
