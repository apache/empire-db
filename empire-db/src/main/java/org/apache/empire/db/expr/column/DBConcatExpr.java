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

// Java
import org.apache.empire.data.DataType;
import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBColumnExpr;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.DBExpr;
import org.apache.empire.dbms.DBSqlPhrase;
import org.apache.empire.xml.XMLUtil;
import org.w3c.dom.Element;


/**
 * This class is used for performing string concatenation in SQL<br>
 * <P>
 * There is no need to explicitly create instances of this class.<BR>
 * Instead use {@link DBColumnExpr#append(Object) }
 */
public class DBConcatExpr extends DBColumnExpr
{
    // *Deprecated* private static final long serialVersionUID = 1L;
  
    protected final DBColumnExpr left;
    protected final Object       right;

    /**
     * Constructs a new DBConcatExpr object set the specified parameters to this object.
     * 
     * @param left the left column for this concatenation
     * @param right the right column for this concatenation
     */
    public DBConcatExpr(DBColumnExpr left, Object right)
    {
        this.left = left;
        this.right = right;
    }

    @SuppressWarnings("unchecked")
    @Override
    public final DBDatabase getDatabase()
    {
        return left.getDatabase();
    }

    /**
     * Returns the data type: {@link DataType#TEXT}
     * 
     * @return {@link DataType#TEXT}
     */
    @Override
    public DataType getDataType()
    {
        return DataType.VARCHAR;
    }

    /**
     * Not an Enum. Returns null
     */
    @Override
    public Class<Enum<?>> getEnumType()
    {
        return null;
    }

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

    @Override
    public DBColumn getSourceColumn()
    {
        return left.getSourceColumn();
    }

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
     * Returns true if other is equal to this expression  
     */
    @Override
    public boolean equals(Object other)
    {
        if (other instanceof DBConcatExpr)
        {   // Compare left and right
            return left .equals(((DBConcatExpr)other).left)
                && right.equals(((DBConcatExpr)other).right);
        }
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
        String template = getDatabase().getDbms().getSQLPhrase(DBSqlPhrase.SQL_CONCAT_EXPR);
        context &= ~CTX_ALIAS; // No column aliases
        // Find Separator
        int sep = template.indexOf('?');
        if (sep >= 0)
        {   // Complex Pattern with placeholder ? for this expression and {0} for the value
            buf.append(template.substring(0, sep));
            left.addSQL(buf, context);
            // template = template.substring(sep + 1)
            int iph = template.indexOf("{0}", ++sep);
            if (iph>0)
                buf.append(template.substring(sep, iph));
            else 
                buf.append(template.substring(sep));
            // value
            addSQLValue(buf, getDataType(), right, context, template);
            // rest
            if (iph>0)
                buf.append(template.substring(iph+3));
        } 
        else
        {   // Simple Pattern without placeholders
            left.addSQL(buf, context);
            buf.append(template);
            addSQLValue(buf, getDataType(), right, context, template);
        }        
    }

    @Override
    public Element addXml(Element parent, long flags)
    {
        Element elem = XMLUtil.addElement(parent, "column");
        elem.setAttribute("name", getName());
        elem.setAttribute("function", "concat");
        DBColumn source = getSourceColumn();
        if (source!=null)
        {   Element elemSource = XMLUtil.addElement(elem, "source");
            source.addXml(elemSource, flags);
            // more
            Object with = right;
            while (with instanceof DBExpr)
            {
                if (with instanceof DBConcatExpr)
                {
                   ((DBConcatExpr)with).getSourceColumn().addXml(elemSource, flags);
                    with = ((DBConcatExpr)with).right;
                    continue;
                }
                if (with instanceof DBColumnExpr)
                {
                    ((DBColumnExpr)with).addXml(elemSource, flags);
                }
                with = null;
            }
        }
        // Add Other Attributes
        if (attributes!=null)
            attributes.addXml(elem, flags);
        // add All Options
        if (options!=null)
            options.addXml(elem, getDataType());
        // Done
        return elem;
    }
    
}
