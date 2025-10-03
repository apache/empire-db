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

import org.apache.empire.commons.ClassUtils;
import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.commons.StringUtils;
// Java
import org.apache.empire.data.DataType;
import org.apache.empire.db.DBCmdParam;
import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBColumnExpr;
import org.apache.empire.db.DBCommand;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.DBExpr;
import org.apache.empire.db.DBRowSet;
import org.apache.empire.db.DBSQLBuilder;
import org.apache.empire.exceptions.InvalidArgumentException;
import org.apache.empire.exceptions.PropertyReadOnlyException;
import org.apache.empire.xml.XMLUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;


/**
 * This class is used for declaring constant values in SQL.
 * <P>
 * There is no need to explicitly create instances of this class.<BR>
 * Instead use {@link DBDatabase#getValueExpr(String)} or one of it's overloads
 *
 */
public class DBValueExpr extends DBColumnExpr
    implements DBPreparable
{
    // *Deprecated* private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(DBValueExpr.class);
  
    public final DBDatabase     db;
    public final DataType       type;
    protected Object            value;
    private String              name;
    private boolean             literal = false;

    /**
     * Constructs a new DBValueExpr object.
     * @deprecated please use DBDatabase.getValueExpr(...)
     * The constructor will be made protected in future versions
     * 
     * @param db the database
     * @param value the value for this constant
     * @param type the data type for this constant
     */
    @Deprecated
    public DBValueExpr(DBDatabase db, Object value, DataType type)
    {
        if (value instanceof DBValueExpr)
            throw new InvalidArgumentException("value", value);
        // set properties
        this.db = db;
        this.type = type;
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
     * @param value the value to set
     */
    public void setValue(Object value)
    {
        if (this.value instanceof DBCmdParam)
        {   // update param
            ((DBCmdParam) this.value).setValue(value);
        }
        else if (type == DataType.UNKNOWN)
        {   // value of DataType.UNKNOWN cannot be changed
            throw new PropertyReadOnlyException("value");
        }
        else if (value!=null && !ClassUtils.isImmutableClass(value.getClass()))
        {   // Must be a simple value
            throw new InvalidArgumentException("value", value);
        }
        else
        {   // overwrite
            this.value = ObjectUtils.convertValue(type, value);
            this.name = null;
        }
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
        return type;
    }

    /**
     * Returns the EnumType if the value is an Enum
     */
    @Override
    public Class<Enum<?>> getEnumType()
    {
        if (value!=null && value.getClass().isEnum())
        {   @SuppressWarnings("unchecked")
            Class<Enum<?>> enumType = (Class<Enum<?>>)value.getClass(); 
            return enumType; 
        }
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
        if (name==null)
        {   // generate name
            String str = String.valueOf(value);
            int i = 0;
            int l = Math.min(str.length(), 20);
            for (boolean valid = true; i<l; i++)
            {   char c = str.charAt(i);
                if (c=='-' && i==0)
                    continue;
                valid = (c==' ' || (c>='0' && c<='9') || (c>='A' && c<='Z') || (c>='a' && c<='z'));
                if (!valid)
                    break;
            }
            str = str.substring((str.charAt(0)=='-' ? 1 : 0), i);
            if (StringUtils.isNotEmpty(str))
            {   // generate from value string
                char c = str.charAt(0);
                if (c>='0' && c<='9')
                    str = "N"+str;
                name = "VAL_"+str.replace(' ','_').toUpperCase(); 
            }
            else
            {   // default
                name = "VALUE";
            }
        }
        return name;
    }

    /**
     * Returns null.
     * @return null
     */
    @Override
    public DBRowSet getRowSet()
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
        if (other instanceof DBValueExpr)
        {   // Compare
            Object otherValue = ((DBValueExpr)other).value;
            // Values must match
            return ObjectUtils.compareEqual(value, otherValue);
        }
        return false;
    }

    /**
     * Set literal mode, i.e. the value will be placed in the sql statement
     * Ohterwise it may be automatically replaced by a parameter ?
     * @return self (this)
     */
    public DBValueExpr literal()
    {
        this.literal = true;
        if (this.value instanceof DBCmdParam)
            this.value = ((DBCmdParam)value).getValue();
        // return self
        return this;
    }
    
    /**
     * Sets literal mode off, i.e. the value may passed as a parameter 
     * and referenced by a ? in the sql statement
     * @return self (this)
     */
    public DBValueExpr parameter()
    {
        this.literal = false;
        return this;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.empire.db.expr.column.DBPreparable#prepareCommand(org.apache.empire.db.DBCommand)
     */
    @Override
    public void prepareCommand(DBCommand cmd) 
    {
        // Cannot use DBExpr or DBSystemDate as parameter
        if (value==null || value instanceof DBCmdParam || value instanceof DBExpr || value instanceof DBDatabase.DBSystemDate)
            return;
        if (getDataType()==DataType.UNKNOWN || literal)
            return;
        // Add command param
        this.value = cmd.addParam(getDataType(), value);
        this.name  = "P_"+String.valueOf(cmd.getParams().size());
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

    /**
     * Creates the SQL-Command.
     * 
     * @param sql the SQL-Command
     * @param context the current SQL-Command context
     */
    @Override
    public void addSQL(DBSQLBuilder sql, long context)
    {
        if ((context & CTX_VALUE)!=0)
            sql.appendValue(getDataType(), value);
        else
            log.warn("Cannot add SQL for DBValueExpr using context {}", context);
    }

    /** this helper function calls the DBColumnExpr.addXML(Element, long) method */
    @Override
    public Element addXml(Element parent, long flags)
    {
        // Add a column expression for this function
        Element elem = XMLUtil.addElement(parent, "column");
        String name = getName();
        if (name!=null)
            elem.setAttribute("name", getName());
        // Add Other Attributes
        if (attributes!=null)
            attributes.addXml(elem, flags);
        // add All Options
        if (options!=null)
            options.addXml(elem, this.type);
        // Done
        elem.setAttribute("function", "value");
        return elem;
    }

}
