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
import org.apache.empire.db.DBCommandExpr;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.exceptions.InvalidArgumentException;
import org.apache.empire.xml.XMLUtil;
import org.w3c.dom.Element;

public class DBCmdResultExpr extends DBColumnExpr
{
    private final DBCommandExpr cmdExpr;

    private final DBColumnExpr result;
    
    public DBCmdResultExpr(DBCommandExpr cmdExpr)
    {
        this.cmdExpr = cmdExpr;
        // get the result column
        DBColumnExpr[] sel = cmdExpr.getSelectExprList();
        if (sel.length!=1)
            throw new InvalidArgumentException("cmdExpr", cmdExpr);
        // result
        this.result = sel[0];
    }

    @Override
    public <T extends DBDatabase> T getDatabase()
    {
        return cmdExpr.getDatabase();
    }

    @Override
    public Class<Enum<?>> getEnumType()
    {
        return result.getEnumType();
    }

    @Override
    public DataType getDataType()
    {
        return result.getDataType();
    }

    @Override
    public String getName()
    {
        return "SEL_"+result.getName();
    }

    @Override
    public boolean isAggregate()
    {
        /* does not need a GROUP_BY */ 
        return false;
    }

    @Override
    public DBColumn getSourceColumn()
    {
        return null;
    }

    @Override
    public DBColumn getUpdateColumn()
    {
        return null;
    }

    @Override
    public void addReferencedColumns(Set<DBColumn> list)
    {
        /* NONE */
    }

    @Override
    public void addSQL(StringBuilder buf, long context)
    {
        // simply forward
        cmdExpr.addSQL(buf, context);
    }

    @Override
    public Element addXml(Element parent, long flags)
    {
        // Add a column expression for this function
        Element elem = XMLUtil.addElement(parent, "column");
        elem.setAttribute("name", getName());
        elem.setAttribute("function", "cmd");
        elem.setAttribute("dataType", getDataType().name());
        elem.setAttribute("aggregate", "true");
        // Done
        return elem;
    }
    
}
