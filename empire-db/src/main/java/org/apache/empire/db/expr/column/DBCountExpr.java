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

// Java
import org.apache.empire.data.DataType;
import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBColumnExpr;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.DBRowSet;
import org.apache.empire.xml.XMLUtil;
import org.w3c.dom.Element;

import java.util.Set;


/**
 * This class is used to add the "count" statement to the SQL-Command.
 * <P>
 * There is no need to explicitly create instances of this class.<BR>
 * Instead use {@link DBRowSet#count() or @link DBColumnExpr#count() }
 * <P>
 *
 */
public class DBCountExpr extends DBColumnExpr
{
    private final static long serialVersionUID = 1L;
  
    private final DBRowSet rowset;
    private final DBColumnExpr column;
    private final boolean distinct;
    
    /**
     * Constructs a DBCountExpr.
     * 
     * @param rowset the rowset
     */
    public DBCountExpr(DBRowSet rowset)
    {
        this.rowset = rowset;
        this.column = null;
        this.distinct = false;
    }
    
    /**
     * Constructs a DBCountExpr.
     * 
     * @param expr the column
     * @param distinct set true for distinct count
     */
    public DBCountExpr(DBColumnExpr expr, boolean distinct)
    {
        this.rowset = null;
        this.column = expr;
        this.distinct = distinct;
    }

    /**
     * Returns the current DBDatabase object.
     * 
     * @return the current DBDatabase object
     */
    @Override
    public DBDatabase getDatabase()
    {
        if (column!=null)
            return column.getDatabase();
        else
            return rowset.getDatabase();
    }

    /**
     * Returns the data type: DT_INTEGER.
     * 
     * @return the data type: DT_INTEGER
     */
    @Override
    public DataType getDataType()
    {
        return DataType.INTEGER;
    }

    /**
     * Returns the String "count".
     * 
     * @return the String "count"
     */
    @Override
    public String getName()
    {
        return "count";
    }

    /**
     * Returns null.
     * 
     * @return null
     */
    @Override
    public DBColumn getUpdateColumn()
    {
        return null;
    }

    /**
     * Returns true since the count function is an aggregate function.
     * 
     * @return always true
     */
    @Override
    public boolean isAggregate()
    {
        return true;
    }

    /**
     * @see org.apache.empire.db.DBExpr#addReferencedColumns(Set)
     */
    @Override
    public void addReferencedColumns(Set<DBColumn> list)
    {
        if (column!=null)
            column.addReferencedColumns(list);
        else
            list.add(rowset.getColumn(0)); // select any column
    }

    /**
     * Creates the SQL-Command adds the String "count(*)" to the SQL-Command.
     * 
     * @param sql the SQL-Command
     * @param context the current SQL-Command context
     */
    @Override
    public void addSQL(StringBuilder sql, long context)
    { 
        sql.append("count(");
        if (column!=null)
        {   // count(distinct column)
            if (distinct)
                sql.append("distinct "); 
            column.addSQL(sql, (context & ~CTX_ALIAS));
        }
        else
        {   // count(*)
            sql.append("*");
        }
        sql.append(")");
    }

    /** 
     * this adds the column description to the parent element 
     */
    @Override
    public Element addXml(Element parent, long flags)
    {   // Add Expression
        Element elem;
        if (column!=null)
        {   elem = column.addXml(parent, flags);
        }
        else
        {   elem = XMLUtil.addElement(parent, "column");
            elem.setAttribute("name", getName());
        }
        elem.setAttribute("function", "count");
        // done
        return elem;
    }

}
