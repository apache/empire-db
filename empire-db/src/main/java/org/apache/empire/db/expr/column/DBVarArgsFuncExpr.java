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

import org.apache.empire.commons.StringUtils;
import org.apache.empire.data.DataType;
import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBColumnExpr;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.DBRowSet;
import org.apache.empire.db.DBSQLBuilder;
import org.apache.empire.exceptions.InvalidArgumentException;
import org.apache.empire.xml.XMLUtil;
import org.w3c.dom.Element;


public class DBVarArgsFuncExpr extends DBColumnExpr
{
    private DBDatabase database;
    private DataType dataType;
    private String template;
    private DBColumnExpr[] cols;
    
    public DBVarArgsFuncExpr(DBDatabase db, DataType dataType, String template, DBColumnExpr... cols)
    {
        if (template.indexOf("?")<0)
            throw new InvalidArgumentException("template", template);

        this.database = db;
        this.dataType = dataType;
        this.template = template;
        this.cols = cols;
    }

    @SuppressWarnings("unchecked")
    @Override
    public final DBDatabase getDatabase()
    {
        return database;
    }

    @Override
    public DataType getDataType()
    {
        return dataType;
    }

    @Override
    public Class<Enum<?>> getEnumType()
    {
        return null;
    }

    @Override
    public String getName()
    {
        return getNameFromTemplate(template);
    }

    @Override
    public boolean isAggregate()
    {
        return false;
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
        if (other instanceof DBVarArgsFuncExpr)
        {   // Compare with same type
            DBVarArgsFuncExpr otherFunc = (DBVarArgsFuncExpr)other;
            // DataTypes must match
            if (!dataType.equals(otherFunc.dataType))
                return false;
            // Templates must match
            if (!StringUtils.compareEqual(this.template, otherFunc.template))
                return false;
            // all parts must match
            for (int i=0; i<cols.length; i++)
                if (!cols[i].equals(otherFunc.cols[i]))
                    return false;
            // OK
            return true;
        }
        return false;
    }

    /**
     * Returns the underlying rowset
     */
    @Override
    public DBRowSet getRowSet()
    {
        return cols[0].getRowSet();
    }

    /**
     * Returns the underlying column
     */
    @Override
    public DBColumn getUpdateColumn()
    {
        // cols[0].getUpdateColumn();
        return null;
    }

    @Override
    public void addReferencedColumns(Set<DBColumn> list)
    {
        for (int i=0; i<cols.length; i++)
        {
            DBColumn col = cols[i].getUpdateColumn();
            if (col!=null)
                list.add(col);
        }    
    }

    @Override
    public void addSQL(DBSQLBuilder sql, long context)
    {
        // Get Template
        String prefix = template;
        String suffix = "";
        int sep = template.indexOf("?");
        if (sep >= 0)
        {   prefix = template.substring(0, sep);
            suffix = template.substring(sep + 1);
        } 
        // append
        sql.append(prefix);
        for (int i=0; i<cols.length; i++)
        {   // separator
            if (i>0)
                sql.append(", ");
            // add SQL
            DBColumnExpr col = cols[i];
            col.addSQL(sql, CTX_DEFAULT);
        }    
        sql.append(suffix);
    }

    @Override
    public Element addXml(Element parent, long flags)
    {
        Element elem = XMLUtil.addElement(parent, "column");
        elem.setAttribute("name", getName());
        // Add Other Attributes
        if (attributes!=null)
            attributes.addXml(elem, flags);
        // Done
        elem.setAttribute("function", getName());
        return elem;
    }
    
    private static String getNameFromTemplate(String template)
    {
        String s = template.trim();
        int i=0;
        for (; i<s.length(); i++)
            if (s.charAt(i)<'A')
                break;
        // return name 
        if (i>0)
            return s.substring(0,i);
        // default
        return "unknownFunc";
    }

}
