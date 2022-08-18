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
package org.apache.empire.db;

import java.util.Collection;
import java.util.Set;

import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.commons.OptionEntry;
// java
import org.apache.empire.data.DataType;
import org.apache.empire.dbms.DBMSHandler;
import org.apache.empire.exceptions.InvalidArgumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This abstract class is the base class for all database expression classes (e.g. DBAliasExpr or DBCalsExpr)
 * <P>
 * 
 *
 */
public abstract class DBExpr extends DBObject
{
    // *Deprecated* private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(DBExpr.class);
  
    // SQL Context Flags
    public static final long CTX_DEFAULT       = 7;  // Default: FullyQualified + Value
    public static final long CTX_ALL           = 15; // All Flags set (except exclusions)

    public static final long CTX_NAME          = 1;  // Unqualified Name
    public static final long CTX_FULLNAME      = 2;  // Fully Qualified Name
    public static final long CTX_VALUE         = 4;  // Value Only
    public static final long CTX_ALIAS         = 8;  // Rename expression
    public static final long CTX_NOPARENTHESES = 16; // No Parentheses
    
    /**
     * Used to build the SQL command. SQL for this expression must be appended to StringBuilder.
     * 
     * @param buf the string buffer used to build the sql command
     * @param context context flag for this SQL-Command (see CTX_??? constants).
     */
    public abstract void addSQL(StringBuilder buf, long context);

    /**
     * Internal function to obtain all DBColumnExpr-objects used by this expression. 
     * 
     * @param list list to which all used column expressions must be added
     */
    public abstract void addReferencedColumns(Set<DBColumn> list);

    /**
     * Appends the SQL representation of a value
     * 
     * @param StringBuilder but the SQL builder
     * @param dataType the DataType
     * @param value an DBExpr object, array or a basis data type(e.g. int, String)
     * @param context the context of the DBColumnExpr object
     * @param arraySep the separator value
     * @return the new SQL-Command
     */
    protected void addSQLValue(StringBuilder buf, DataType dataType, Object value, long context, String arraySep)
    {
        // it's an Object
        if (value instanceof DBExpr)
        {   // it's an expression
            ((DBExpr) value).addSQL(buf, context);
            return;
        } 
        // check option entry
        if (value instanceof OptionEntry)
        {   // option value
            value = ((OptionEntry)value).getValue();
        }
        // check enum
        if (value instanceof Enum<?>)
        {   // check enum
            value = ObjectUtils.getEnumValue((Enum<?>)value, dataType.isNumeric());
        }
        else if (value instanceof Collection<?>)
        {   // collection 2 array
        	value = ((Collection<?>)value).toArray();
        }
        // Check whether it is an array
        if (value!=null && value.getClass().isArray())
        {   // An Array of Objects
            Object[] array = (Object[]) value;
            for (int i = 0; i < array.length; i++)
            {   // Array Separator
                if (i > 0 && arraySep != null)
                    buf.append(arraySep);
                // Append Value
                addSQLValue(buf, dataType, array[i], context, arraySep);
            }
            return;
        } 
        else
        {   // Scalar Value from DB
            DBMSHandler dbms = getDatabase().getDbms();
            if (dbms==null)
            {   // Convert to String
                log.warn("No DBMS set for getting object value. Using default!");
                buf.append(String.valueOf(value));
            }
            // Get Value Expression from dmbs
            buf.append(dbms.getValueString(value, dataType));
        }
    }
    
    /**
     * Expands an SQL template and adds it to the SQL command
     * 
     * @param StringBuilder but the SQL builder
     * @param template the SQL template
     * @param values an array of values to be inserted into the template
     * @param dataType the DataType
     * @param context the context of the DBColumnExpr object
     * @param arraySep the separator value
     * @return the new SQL-Command
     */
   protected void addSQLTemplate(StringBuilder sql, String template, Object[] values, DataType[] dataTypes, long context, String arraySep)
    {
        int pos = 0;
        while (true)
        {
            // find begin
            int beg = template.indexOf('{', pos);
            if (beg < 0)
                break;
            // append
            sql.append(template.substring(pos, beg));
            // find end
            int end = template.indexOf('}', ++beg);
            if (end < 0)
                throw new InvalidArgumentException("template", template);
            // part
            int iParam = Integer.parseInt(template.substring(beg, end));
            if (iParam<0 || iParam>=values.length)
                throw new InvalidArgumentException("params", values);
            // add value
            DataType dataType = (dataTypes.length>=iParam ? dataTypes[iParam] : dataTypes[0]);
            addSQLValue(sql, dataType, values[iParam], context, arraySep);
            // next
            pos = end + 1;
        }
        if (pos < template.length())
        {   // add the rest
            sql.append(template.substring(pos));
            // special case: Nothing added yet
            if (pos==0 && values!=null && values.length>0)
                log.warn("No Placeholder for found in template {}!", template);
        }
    }
}