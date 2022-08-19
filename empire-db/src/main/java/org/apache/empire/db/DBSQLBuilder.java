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

import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.commons.OptionEntry;
import org.apache.empire.data.DataType;
import org.apache.empire.dbms.DBMSHandler;
import org.apache.empire.dbms.DBSqlPhrase;
import org.apache.empire.exceptions.InvalidArgumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DBSQLBuilder
 * This class is used when building a single SQL-statement 
 * @author doebele
 */
public class DBSQLBuilder implements Appendable
{
    private static final Logger log = LoggerFactory.getLogger(DBSQLBuilder.class);
    
    private final DBMSHandler dbms;
    
    private final StringBuilder sql = new StringBuilder(64);
    
    public DBSQLBuilder(DBMSHandler dbms)
    {
        this.dbms = dbms;
    }
    
    /*
     * getters
     */

    public DBMSHandler getDbms()
    {
        return dbms;
    }
    
    public String getPhrase(DBSqlPhrase phrase)
    {
        return dbms.getSQLPhrase(phrase);
    }
    
    /*
     * special
     */
    public int length()
    {
        return sql.length();
    }
    
    public void reset(int pos)
    {
        if (pos>sql.length())
            throw new InvalidArgumentException("pos", pos);
        sql.setLength(pos);
    }
    
    /*
     * appenders 
     */

    @Override
    public DBSQLBuilder append(CharSequence sqlLiteral)
    {
        sql.append(sqlLiteral);
        return this;
    }

    @Override
    public DBSQLBuilder append(CharSequence sqlLiteral, int start, int end)
    {
        sql.append(sqlLiteral, start, end);
        return this;
    }
    
    public DBSQLBuilder append(DBSqlPhrase phrase)
    {
        sql.append(dbms.getSQLPhrase(phrase));
        return this;
    }

    @Override
    public DBSQLBuilder append(char c)
    {
        sql.append(c);
        return this;
    }
    
    public DBSQLBuilder append(boolean b) {
        sql.append(b);
        return this;
    }

    public DBSQLBuilder append(int i) {
        sql.append(i);
        return this;
    }

    public DBSQLBuilder append(long l) {
        sql.append(l);
        return this;
    }

    public DBSQLBuilder append(float f) {
        sql.append(f);
        return this;
    }

    public DBSQLBuilder append(double d) {
        sql.append(d);
        return this;
    }

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
    public void appendValue(DataType dataType, Object value, long context, String arraySep)
    {
        // it's an Object
        if (value instanceof DBExpr)
        {   // it's an expression
            ((DBExpr) value).addSQL(this, context);
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
                    sql.append(arraySep);
                // Append Value
                appendValue(dataType, array[i], context, arraySep);
            }
            return;
        } 
        else
        {   // Get Value Expression from dmbs
            sql.append(dbms.getValueString(value, dataType));
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
    public void appendTemplate(String template, Object[] values, DataType[] dataTypes, long context, String arraySep)
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
            appendValue(dataType, values[iParam], context, arraySep);
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
    
    /**
     * returns the SQL as a String 
     */
    @Override
    public String toString()
    {
        return sql.toString();
    }
}
