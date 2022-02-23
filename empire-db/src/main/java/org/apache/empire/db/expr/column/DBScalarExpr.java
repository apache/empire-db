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
import java.util.Set;

import org.apache.empire.commons.StringUtils;
import org.apache.empire.data.DataType;
import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBColumnExpr;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.xml.XMLUtil;
import org.w3c.dom.Element;


/**
 * This class is used for declaring scalar functions in SQL (like e.g. random).
 * <P>
 *
 */
public class DBScalarExpr extends DBColumnExpr
{
    // *Deprecated* private static final long serialVersionUID = 1L;
  
    public final DBDatabase   db;
    public final DataType     dataType;
    public final String       template;
    protected Object[]        params;

    /**
     * Constructs a new DBValueExpr object.
     * 
     * @param db the database
     * @param value the value for this constant
     * @param type the data type for this constant
     */
    public DBScalarExpr(DBDatabase db, String template, Object[] params, DataType dataType)
    {
        this.db = db;
        this.dataType = dataType;
        this.template = template;
        this.params = params;
    }

    /**
     * Constructs a new DBValueExpr object.
     * 
     * @param db the database
     * @param value the value for this constant
     * @param type the data type for this constant
     */
    public DBScalarExpr(DBDatabase db, String template, Object param, DataType dataType)
    {
        this.db = db;
        this.dataType = dataType;
        this.template = template;
        this.params = new Object[] { param };
    }
    
    /**
     * return the value associated with this value expression
     * @return the current value
     */
    public Object[] getParams()
    {
        return params;
    }

    /**
     * set the value associated with this value expression
     * @param the value
     */
    public void setParams(Object[] params)
    {
        this.params = params;
    }

    /**
     * Returns the current DBDatabase object.
     * 
     * @return the current DBDatabase object
     */
    @SuppressWarnings("unchecked")
    @Override
    public final DBDatabase getDatabase()
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
        return dataType;
    }

    /**
     * Returns null
     */
    @Override
    public Class<Enum<?>> getEnumType()
    {
        return null;
    }

    /**
     * Returns the column name.
     * 
     * @return the column name
     */
    @Override
    public String getName()
    {
        return null;
    }

    /** this helper function calls the DBColumnExpr.addXML(Element, long) method */
    @Override
    public Element addXml(Element parent, long flags)
    {
        Element elem;
        // Add a column expression for this function
        elem = XMLUtil.addElement(parent, "column");
        String name = getName();
        if (name!=null)
            elem.setAttribute("name", getName());
        // Add Other Attributes
        if (attributes!=null)
            attributes.addXml(elem, flags);
        // Done
        elem.setAttribute("function", template);
        return elem;
    }

    /**
     * Returns the expression the source column.
     */
    @Override
    public DBColumn getSourceColumn()
    {
        return null;
    }

    /**
     * Returns null.
     * @return null
     */
    @Override
    public DBColumn getUpdateColumn()
    {
        return null;
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
     * Returns true if other is equal to this expression  
     */
    @Override
    public boolean equals(Object other)
    {
        if (other==this)
            return true;
        // Check Type
        if (other instanceof DBScalarExpr)
        {   // Compare with same type
            DBScalarExpr otherFunc = (DBScalarExpr)other;
            // DataTypes must match
            if (!dataType.equals(otherFunc.dataType))
                return false;
            // Templates must match
            return StringUtils.compareEqual(this.template, otherFunc.template);
        }
        return false;
    }

    /**
     * @see org.apache.empire.db.DBExpr#addReferencedColumns(Set)
     */
    @Override
    public void addReferencedColumns(Set<DBColumn> list)
    {
        return;
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
        // Get Text from Template
        String text = this.template;
        if (params != null)
        {   // Replace Params
            for (int i = 0; i < params.length; i++)
            {   // String test  =(params[i] != null) ? params[i].toString() : "";
                String value = getObjectValue(dataType, params[i], CTX_DEFAULT, ",");
                text = StringUtils.replaceAll(template, "{"+ String.valueOf(i) + "}", value);
            }
        }
        buf.append(text);
    }

}
