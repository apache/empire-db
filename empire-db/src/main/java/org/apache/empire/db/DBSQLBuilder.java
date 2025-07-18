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

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Date;

import org.apache.empire.commons.DateUtils;
import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.commons.StringUtils;
import org.apache.empire.data.DataType;
import org.apache.empire.dbms.DBMSHandler;
import org.apache.empire.dbms.DBSqlPhrase;
import org.apache.empire.exceptions.InvalidArgumentException;
import org.apache.empire.exceptions.NotSupportedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DBSQLBuilder
 * This class is used for building a single SQL statement 
 * @author doebele
 */
public abstract class DBSQLBuilder implements Appendable
{
    private static final Logger log            = LoggerFactory.getLogger(DBSQLBuilder.class);

    protected static final char   TEXT_DELIMITER = '\'';

    protected final DBMSHandler   dbms;

    protected final StringBuilder sql            = new StringBuilder(64);

    protected DBCmdParamList      cmdParamList;

    /**
     *  Don't use this directly
     *  Use dbms.createSQLBuilder()
     *  @param dbms the dbms handler
     */
    protected DBSQLBuilder(DBMSHandler dbms)
    {
        this.dbms = dbms;
    }
    
    public void setCmdParams(DBCmdParamList cmdParamList)
    {
        this.cmdParamList = cmdParamList;
    }
    
    /**
     * returns the SQL as a String 
     */
    @Override
    public String toString()
    {
        return sql.toString();
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

    @Override
    public DBSQLBuilder append(char c)
    {
        sql.append(c);
        return this;
    }

    public DBSQLBuilder append(long l) {
        sql.append(l);
        return this;
    }

    public DBSQLBuilder append(double d) {
        sql.append(d);
        return this;
    }
    
    public DBSQLBuilder append(DBSqlPhrase phrase)
    {
        sql.append(dbms.getSQLPhrase(phrase));
        return this;
    }
    
    public void append(DBCommandExpr subQueryCmd)
    {
        // append select
        sql.append(subQueryCmd.getSelect());
        // check params
        DBCmdParams params = subQueryCmd.getParams();
        if (params.isEmpty())
            return;
        // cmdParamList
        if (cmdParamList==null)
            throw new NotSupportedException(this, "append command with params");
        // merge
        cmdParamList.mergeSubqueryParams(params);
    }

    /**
     * Appends the SQL representation of a value
     * @param type the data type
     * @param value an DBExpr object, array or a basis data type(e.g. int, String)
     */
    public void appendValue(DataType type, Object value)
    {
        appendValue(type, value, DBExpr.CTX_DEFAULT, "+");
    }
    
    /**
     * Appends the SQL representation of a value
     * @param dataType the DataType
     * @param value an DBExpr object, array or a basis data type(e.g. int, String)
     * @param context the context of the DBColumnExpr object
     * @param arraySep the separator value
     */
    public void appendValue(DataType dataType, Object value, long context, String arraySep)
    {
        // it's an Object
        if (value instanceof DBExpr)
        {   // it's an expression
            ((DBExpr) value).addSQL(this, context);
            return;
        }
        // Check colletion
        if (value instanceof Collection<?>)
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
        {   // Convert to database format
            if (value!= null)
                value = ObjectUtils.convertValue(dataType, value);
            // append now
            appendSimpleValue(dataType, value);
        }
    }

    /**
     * Appends the SQL representation of a value
     * @param dataType the DataType
     * @param value an DBExpr object, array or a basis data type(e.g. int, String)
     * @param context the context of the DBColumnExpr object
     */
    public final void appendValue(DataType dataType, Object value, long context)
    {
        appendValue(dataType, value, context, getPhrase(DBSqlPhrase.SQL_CONCAT_EXPR));
    }
    
    /**
     * Expands an SQL template and adds it to the SQL command
     * 
     * @param template the SQL template
     * @param values list of values to be inserted into the template
     * @param dataTypes list of data types
     * @param context the context of the DBColumnExpr object
     * @param arraySep the separator value
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
    
    /*
     * internal
     */
    
    /**
     * Returns a sql string for a given value. 
     * Text will be enclosed in single quotes and existing single quotes will be doubled.
     * Empty strings are treated as null.
     * The syntax of Date, Datetime and Boolean values are DBMS specific.
     * 
     * @param type the data type of the supplied value
     * @param value the value which is inserted to the new String
     */
    protected void appendSimpleValue(DataType type, Object value)
    { 
        if (value instanceof Enum<?>)
        {   // If still an Enum, convert now (Lazy conversion)
            value = ObjectUtils.getEnumValue((Enum<?>)value, type.isNumeric());
        }
        if (ObjectUtils.isEmpty(value))
        {   // null
            append(DBSqlPhrase.SQL_NULL);
            return;
        }
        // set string buffer
        switch (type)
        {
            case DATE:
                sql.append(getDateTimeString(value, DBSqlPhrase.SQL_DATE_TEMPLATE, DBSqlPhrase.SQL_DATE_PATTERN, DBSqlPhrase.SQL_CURRENT_DATE));
                return;
            case TIME:
                sql.append(getDateTimeString(value, DBSqlPhrase.SQL_TIME_TEMPLATE, DBSqlPhrase.SQL_TIME_PATTERN, DBSqlPhrase.SQL_CURRENT_TIME));
                return;
            case DATETIME:
                String text;
                // Only date (without time) provided?
                if (!DBDatabase.SYSDATE.equals(value) && !(value instanceof Date) && ObjectUtils.lengthOf(value)<=10)
                    text = getDateTimeString(value, DBSqlPhrase.SQL_DATE_TEMPLATE, DBSqlPhrase.SQL_DATE_PATTERN, DBSqlPhrase.SQL_CURRENT_TIMESTAMP);
                else // Complete Date-Time Object with time
                    text = getDateTimeString(value, DBSqlPhrase.SQL_DATETIME_TEMPLATE, DBSqlPhrase.SQL_DATETIME_PATTERN, DBSqlPhrase.SQL_CURRENT_DATETIME);
                sql.append(text);
                return;
            case TIMESTAMP:
                sql.append(getDateTimeString(value, DBSqlPhrase.SQL_TIMESTAMP_TEMPLATE, DBSqlPhrase.SQL_TIMESTAMP_PATTERN, DBSqlPhrase.SQL_CURRENT_TIMESTAMP));
                return;
            case VARCHAR:
            case CHAR:
            case CLOB:
            case UNIQUEID:
                appendStringLiteral(type, value);
                return;
            case BOOL:
                // Get Boolean value   
                boolean boolVal = false;
                if (value instanceof Boolean)
                {   boolVal = ((Boolean) value).booleanValue();
                } 
                else
                {   // Boolean from String
                    boolVal = stringToBoolean(value.toString());
                }
                append((boolVal) ? DBSqlPhrase.SQL_BOOLEAN_TRUE : DBSqlPhrase.SQL_BOOLEAN_FALSE);
                return;
            case INTEGER:
            case DECIMAL:
            case FLOAT:
                sql.append(getNumberString(value, type));
                return;
            case BLOB:
                throw new NotSupportedException(this, "appendSimpleValue(DataType.BLOB)"); 
            case AUTOINC:
            case UNKNOWN:
                /* Allow expressions */
                sql.append(value.toString());
                return;
            default:
                log.warn("Unknown DataType {} for getValueString().", type);
                sql.append(value.toString());
        }
    }

    /**
     * encodes a Date value for an SQL command string. 
     * @param value the value to encode
     * @param sqlTemplate the template
     * @param sqlPattern the pattern
     * @param sqlCurrentDate the current date phrase
     * @return the date time string
     */
    protected String getDateTimeString(Object value, DBSqlPhrase sqlTemplate, DBSqlPhrase sqlPattern, DBSqlPhrase sqlCurrentDate)
    {
        // is it a sysdate expression
        if (DBDatabase.SYSDATE.equals(value))
            return dbms.getSQLPhrase(sqlCurrentDate);
        // Format the date (ymd)
        LocalDateTime ts; 
        int nanos = 0;
        if ((value instanceof Timestamp)) 
        {   // Convert Timestamp
            ts = ((Timestamp)value).toLocalDateTime();
            nanos = (((Timestamp)value).getNanos() % 1000000);
        }
        else if ((value instanceof Date))
        {   // Convert Date
            ts = DateUtils.toLocalDateTime((Date)value);
        }
        else if ((value instanceof LocalDate))
        {   // Convert LocalDate
            ts = ((LocalDate)value).atStartOfDay();
        }
        else if ((value instanceof LocalDateTime))
        {   // Convert LocalDateTime to Timestamp
            ts = ((LocalDateTime)value);
        }
        else if ((value instanceof LocalTime))
        {   // Convert LocalTime to Timestamp with current date
            ts = ((LocalTime)value).atDate(LocalDate.now());
        }
        else 
        {   // "Timestamp format must be yyyy-mm-dd hh:mm:ss[.fffffffff]"
            String dtValue = value.toString().trim();
            try
            {   // parse timestamp
                ts = Timestamp.valueOf(dtValue).toLocalDateTime();
            } catch (Throwable e) {
                // Invalid date
                log.error("Unable to parse date value "+dtValue, e);
                throw new InvalidArgumentException("value", value);
            }
        }
        // Convert to String
        String pattern = dbms.getSQLPhrase(sqlPattern);
        DateTimeFormatter sqlFormat = DateTimeFormatter.ofPattern(pattern);
        String datetime = sqlFormat.format(ts);
        // Add micro / nanoseconds
        if (pattern.endsWith(".SSS") && nanos>0)
        {   // Add nanoseconds
            if (((nanos) % 100)>0)
                datetime += String.format("%06d", nanos);
            else if (((nanos) % 1000)>0)
                datetime += String.format("%04d",(nanos/100));
            else
                datetime += String.format("%03d",(nanos/1000));
        }
        // Now Build String
        String template = dbms.getSQLPhrase(sqlTemplate);
        return StringUtils.replace(template, "{0}", datetime);
    }

    /**
     * encodes Text values for an SQL command string.
     * @param type date type (can only be TEXT, CHAR, CLOB and UNIQUEID)
     * @param value the literal to be encoded
     */
    protected void appendStringLiteral(DataType type, Object value)
    {   // text
        if (value==null)
        {   append(DBSqlPhrase.SQL_NULL);
            return;
        }
        String text = value.toString();
        sql.append(TEXT_DELIMITER);
        if (DBDatabase.EMPTY_STRING.equals(text)==false)
            escapeAndAppendLiteral(text);
        sql.append(TEXT_DELIMITER);
    }

    /** 
     * this helper function doubles up single quotes for SQL
     * @param value the string to escape and append
     */
    protected void escapeAndAppendLiteral(String value)
    {
        int pos = 0;
        int delim;
        // find delimiter
        while ((delim = value.indexOf(TEXT_DELIMITER, pos))>=0)
        {   // append
            if (delim>pos)
                sql.append(value.substring(pos, delim));
            // double up
            sql.append("''");
            // next
            pos = delim + 1;
        }
        if (pos==0)
            sql.append(value); // add entire string
        else if (pos < value.length())
            sql.append(value.substring(pos)); // add the rest
    }
    
    /**
     * encodes a numeric value for an SQL command string. 
     * @param value the numeric value
     * @param type the number data type
     * @return the string representation of the number
     */
    protected String getNumberString(Object value, DataType type)
    {
        // already a number
        if (value instanceof Number)
            return value.toString();
        
        // check if it is a number
        String s = value.toString();
        boolean integerOnly = (type==DataType.INTEGER);
        for (int i=0; i<s.length(); i++)
        {
            char c = s.charAt(i);
            if (c>='0' && c<='9')
                continue; // OK
            if (c=='-' || c=='+')
                continue; // OK
            if (c==' ' && i>0)
                return s.substring(0,i);
            // check 
            if (integerOnly || (c!='.' && c!=','))
                throw new NumberFormatException(s);
        }
        return s;
    }

    /**
     * this function converts a string containing a boolean expression to a boolean. 
     * @param value the string containing a boolean expression
     * @return true if the string contains either "true", "y" or "1" or false otherwise
     */
    protected boolean stringToBoolean(final String value) 
    {
        return "1".equals(value) ||
               "true".equalsIgnoreCase(value) ||
               "y".equalsIgnoreCase(value);
    }
    
}
