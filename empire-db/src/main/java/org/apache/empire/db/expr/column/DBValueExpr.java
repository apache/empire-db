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
import org.apache.empire.db.DBDatabaseDriver;
import org.apache.empire.xml.XMLUtil;
import org.w3c.dom.Element;

import java.util.Set;


/**
 * This class is used for declaring constant values in SQL.
 * <P>
 * There is no need to explicitly create instances of this class.<BR>
 * Instead use {@link DBDatabase#getValueExpr(String)} or one of it's overloads
 * <P>
 *
 */
public class DBValueExpr extends DBColumnExpr
{
    private final static long serialVersionUID = 1L;
  
    public final DBDatabase   db;
    public final DataType     type;
    public final DBColumnExpr column;
    // the underlying value
    protected Object          value;

    /**
     * Constructs a new DBValueExpr object.
     * 
     * @param db the database
     * @param value the value for this constant
     * @param type the data type for this constant
     */
    public DBValueExpr(DBDatabase db, Object value, DataType type)
    {
        this.db = db;
        this.type = type;
        this.column = null;
        this.value = value;
    }

    /**
     * Construct a new DBValueExpr object set the specified parameters to this object.
     * @param col the column
     * @param value the value
     */
    public DBValueExpr(DBColumnExpr col, Object value)
    {
        this.column = col;
        this.type = col.getDataType();
        this.db = col.getDatabase();
        this.value = value;
    }

    /**
     * return the value associated with this value expression
     * @return the current value
     */
    public Object getValue()
    {
        return value;
    }

    /**
     * set the value associated with this value expression
     * @param the value
     */
    public void setValue(Object value)
    {
        this.value = value;
    }

    /**
     * Returns the current DBDatabase object.
     * 
     * @return the current DBDatabase object
     */
    @Override
    public DBDatabase getDatabase()
    {
        return db;
    }

    /**
     * Returns the data type of the DBColumnExpr object.
     * 
     * @return the data type
     */
    @Override
    public DataType getDataType()
    {
        return type;
    }

    /**
     * Returns the column name.
     * 
     * @return the column name
     */
    @Override
    public String getName()
    {
        return (column != null) ? column.getName() : null;
    }

    /** this helper function calls the DBColumnExpr.addXML(Element, long) method */
    @Override
    public Element addXml(Element parent, long flags)
    {
        Element elem;
        if (column!=null)
        {   // Update Column
            elem = column.addXml(parent, flags);
        }
        else
        {   // Add a column expression for this function
            elem = XMLUtil.addElement(parent, "column");
            String name = getName();
            if (name!=null)
                elem.setAttribute("name", getName());
            // Add Other Attributes
            if (attributes!=null)
                attributes.addXml(elem, flags);
            // add All Options
            if (options!=null)
                options.addXml(elem, flags);
        }
        // Done
        elem.setAttribute("function", "value");
        return elem;
    }

    /**
     * Returns null.
     * 
     * @return null
     */
    @Override
    public DBColumn getUpdateColumn()
    {
        return (column != null) ? column.getUpdateColumn() : null;
    }

    /**
     * Always returns false since value expressions cannot be an aggregate.
     * 
     * @return false
     */
    @Override
    public boolean isAggregate()
    {
        return false;
    }

    /**
     * Creates the SQL-Command.
     * 
     * @param buf the SQL-Command
     * @param context the current SQL-Command context
     */
    @Override
    public void addSQL(StringBuilder buf, long context)
    {
        DBDatabaseDriver driver = db.getDriver();
        String text = (driver!=null) ? driver.getValueString(value, getDataType()) : String.valueOf(value); 
        buf.append(text);
    }

    /**
     * @see org.apache.empire.db.DBExpr#addReferencedColumns(Set)
     */
    @Override
    public void addReferencedColumns(Set<DBColumn> list)
    {
        // nothing to do!
        return;
    }

}
