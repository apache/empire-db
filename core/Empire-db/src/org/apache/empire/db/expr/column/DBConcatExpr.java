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
import java.text.MessageFormat;
import java.util.Set;

import org.apache.empire.data.DataType;
import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBColumnExpr;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.DBDatabaseDriver;
import org.apache.empire.db.DBExpr;
import org.apache.empire.xml.XMLUtil;
import org.w3c.dom.Element;


/**
 * This class is used for performing string concatenation in SQL<br>
 * <P>
 * There is no need to explicitly create instances of this class.<BR>
 * Instead use {@link DBColumnExpr#append(Object) }
 * <P>
 * @author ESTEAM software <A TARGET="esteam" HREF="http://www.esteam.de">www.esteam.de</A>
 */
public class DBConcatExpr extends DBColumnExpr
{
    protected final DBColumnExpr left;
    protected final Object       right;

    /**
     * Constructs a new DBConcatExpr object set the specified parameters to this object.
     */
    public DBConcatExpr(DBColumnExpr left, Object right)
    {
        this.left = left;
        this.right = right;
    }

    /**
     * Returns the current DBDatabase object.
     * 
     * @return the current DBDatabase object
     */
    @Override
    public DBDatabase getDatabase()
    {
        return left.getDatabase();
    }

    /**
     * Returns the data type: DT_TEXT.
     * 
     * @return the data type: DT_TEXT
     */
    @Override
    public DataType getDataType()
    {
        return DataType.TEXT;
    }

    /**
     * This helper function returns expression name.
     * 
     * @return the expression name
     */
    @Override
    public String getName()
    { // Get the expression name
        String name = left.getName();
        if (right instanceof DBColumnExpr)
        { // add other names
            name += "_";
            name += ((DBColumnExpr) right).getName();
        }
        return name;
    }

    /** This helper function calls the DBColumnExpr.addXML(Element, long) method. */
    @Override
    public Element addXml(Element parent, long flags)
    {
        Element elem = XMLUtil.addElement(parent, "column");
        elem.setAttribute("name", getName());
        elem.setAttribute("function", "concat");
        // Add Other Attributes
        if (attributes!=null)
            attributes.addXml(elem, flags);
        // add All Options
        if (options!=null)
            options.addXml(elem, flags);
        // Done
        return elem;
    }

    /**
     * returns null
     * 
     * @return null
     */
    @Override
    public DBColumn getUpdateColumn()
    {
        return null;
    }

    /**
     * Always returns false since a concat expression cannot be an aggregate.
     * 
     * @return false
     */
    @Override
    public boolean isAggregate()
    {
        return false;
    }

    /**
     * @see org.apache.empire.db.DBExpr#addReferencedColumns(Set)
     */
    @Override
    public void addReferencedColumns(Set<DBColumn> list)
    {
        left.addReferencedColumns(list);
        // Check if right object is a DBExpr
        if (right instanceof DBExpr)
            ((DBExpr)right).addReferencedColumns(list);
    }

    /**
     * Creates the SQL-Command concatenate a specified column with
     * a specified value sets the column with a specified value to
     * the SQL-Command.
     * 
     * @param buf the SQL statment
     * @param context the current SQL-Command context
     */
    @Override
    public void addSQL(StringBuilder buf, long context)
    { // Zusammenbauen
        String template = getDatabase().getDriver().getSQLPhrase(DBDatabaseDriver.SQL_CONCAT_EXPR);
        // Find Separator
        int sep = template.indexOf('?');
        if (sep >= 0)
        {   // Complex Pattern with placeholder ? for this expression and {0} for the value
            buf.append(template.substring(0, sep));
            left.addSQL(buf, context);
            String value = getObjectValue(this, right, context, ", ");
            buf.append(MessageFormat.format(template.substring(sep + 1), value));
        } 
        else
        {   // Simple Pattern without placeholders
            left.addSQL(buf, context);
            buf.append(template);
            buf.append(getObjectValue(this, right, context, template));
        }
        
    }
}
