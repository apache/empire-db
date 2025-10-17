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
package org.apache.empire.commons;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

import org.apache.empire.data.Column;
import org.apache.empire.data.ColumnExpr;
import org.apache.empire.data.DataType;
import org.apache.empire.exceptions.NotSupportedException;

/**
 * ColumnUtils
 * This class contains a set of generic functions for Columns
 * @author doebele
 */
public class ColumnUtils
{
    // private static final Logger log = LoggerFactory.getLogger(ColumnUtils.class);
    
    /**
     * The instance of ValueUtils to be used for value type conversion
     */
    static private ColumnUtils instance = new ColumnUtils();
    
    public static ColumnUtils getInstance()
    {
        return instance;
    }

    public static void setInstance(ColumnUtils columnUtils)
    {
        instance = columnUtils;
    }

    /**
     * Returns the minimum length, usually 0 
     * @param columnExpr the columnExpr
     * @return the minimum length 
     */
    public static int getMinLength(ColumnExpr columnExpr)
    {
        Object value = instance.getColumnAttribute(columnExpr, Column.COLATTR_MINLENGTH);
        return ((value instanceof Number) ? ((Number)value).intValue() : 0);
    }
    
    /**
     * Return the maximum character length of a columnExpr
     * @param columnExpr the columnExpr 
     * @return the max number or characters 
     */
    public static int getMaxLength(ColumnExpr columnExpr)
    {
        return instance.getColumnMaxLength(columnExpr);
    }
    
    /**
     * Returns the maximum allowed value or 0 if not set 
     * @param columnExpr the columnExpr
     * @return the minimum value 
     */
    public static Number getMinValue(ColumnExpr columnExpr)
    {
        // check numeric
        if (!columnExpr.getDataType().isNumeric())
            return null;
        // convert to decimal
        Object value = instance.getColumnAttribute(columnExpr, Column.COLATTR_MINVALUE);
        return ObjectUtils.getDecimal(value);
    }

    /**
     * Returns the maximum allowed value 
     * @param columnExpr the columnExpr
     * @return the maximum value 
     */
    public static Number getMaxValue(ColumnExpr columnExpr)
    {
        // check numeric
        if (!columnExpr.getDataType().isNumeric())
            return null;
        if (columnExpr.getDataType()==DataType.AUTOINC)
            return Long.MAX_VALUE;
        // convert to decimal
        Object value = instance.getColumnAttribute(columnExpr, Column.COLATTR_MAXVALUE);
        long digits = (long)instance.getColumnSize(columnExpr);
        long maxVal = (digits!=0) ? 10l^digits : 10^12;
        if (columnExpr.getDataType()==DataType.INTEGER)
            return ObjectUtils.getLong(value, maxVal);
        else
            return ObjectUtils.getDecimal(value, BigDecimal.valueOf(maxVal));
    }
    
    /**
     * Set the minimum and maximum values of a columnExpr 
     * @param columnExpr the columnExpr
     * @param minValue the min value
     * @param maxValue the max value
     * @return the columnExpr
     */
    public static <T extends ColumnExpr> T setMinMaxValue(T columnExpr, Number minValue, Number maxValue)
    {
        if (!columnExpr.getDataType().isNumeric())
            throw new NotSupportedException(columnExpr, "setMinMaxValue");
        // set the min and max values
        columnExpr.setAttribute(Column.COLATTR_MINVALUE, minValue);
        columnExpr.setAttribute(Column.COLATTR_MAXVALUE, maxValue);
        return columnExpr;
    }
    
    /**
     * Returns the number format for a particular columnExpr
     * @param columnExpr the columnExpr
     * @param locale the locale for which to get the format
     * @return the NumberFormat
     */
    public static NumberFormat getNumberFormat(ColumnExpr columnExpr, Locale locale)
    {
        return instance.getColumnNumberFormat(columnExpr, locale);
    }
    
    /**
     * Sets the number format options for a columnExpr 
     * @param columnExpr the column expression
     * @param numberType the number type 
     * @param groupSeparator should be used 
     * @return the columnExpr
     */
    public static <T extends ColumnExpr> T setNumberFormat(T columnExpr, String numberType, boolean groupSeparator)
    {
        if (!columnExpr.getDataType().isNumeric())
            throw new NotSupportedException(columnExpr, "setNumberFormat");
        // set format
        instance.setColumnNumberFormat(columnExpr, numberType, groupSeparator);
        return columnExpr;
    }

    /*
     * Implementations
     */
    
    /**
     * Returns the column attribute
     * @param columnExpr the column expression
     * @param name the attribute name
     * @return the attribute value
     */
    public Object getColumnAttribute(ColumnExpr columnExpr, String name)
    {
        return columnExpr.getAttribute(name); 
    }
    
    /**
     * Return the size of the column of 0.0d if unknown
     * @param columnExpr the column expression
     * @return the size in the form [integer digits].[fraction digits]
     */
    public double getColumnSize(ColumnExpr columnExpr)
    {
        Column updColumn = columnExpr.getUpdateColumn();
        return (updColumn!=null ? updColumn.getSize() : 0.0d);
    }

    /*
     * (non-Javadoc)
     * @see org.apache.empire.commons.ColumnUtils#getMaxLength(ColumnExpr)
     */
    public int getColumnMaxLength(ColumnExpr columnExpr)
    {
        switch(columnExpr.getDataType())
        {
            case INTEGER:
            case AUTOINC:
                int intSize = (int)getColumnSize(columnExpr);
                return (intSize>0 ? intSize : 12);    
            case CHAR:
            case VARCHAR:
                return (int)getColumnSize(columnExpr);
            case DATE:
            case TIME:
                return 10; 
            case DATETIME:
            case TIMESTAMP:
                return 20; 
            case FLOAT:
            case DECIMAL:
                double size = getColumnSize(columnExpr);
                int intDigits = Math.min((int)size, 1);
                int fracDigits = ((int)(size*10)-(intDigits*10));
                return (fracDigits>0 ? intDigits+fracDigits+1 : intDigits);                 
            case BOOL:
                return 1; // Y/N or 0/1
            case UNIQUEID:
                return 36;
            case CLOB:
            case BLOB:
            default:
                return -1; // undefined;
        }
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.empire.commons.ColumnUtils#getNumberFormat(ColumnExpr, Locale)
     */
    public NumberFormat getColumnNumberFormat(ColumnExpr columnExpr, Locale locale)
    {
        // ColumnExpr is supplied
        String type = StringUtils.valueOf(getColumnAttribute(columnExpr, Column.COLATTR_NUMBER_TYPE));
        boolean isInteger = Column.NUMTYPE_INTEGER.equalsIgnoreCase(type);
        NumberFormat nf = (isInteger) ? NumberFormat.getIntegerInstance(locale)
                                      : NumberFormat.getNumberInstance(locale); 
        // Groups Separator?
        Object groupSep = getColumnAttribute(columnExpr, Column.COLATTR_NUMBER_GROUPSEP);
        nf.setGroupingUsed(groupSep != null && ObjectUtils.getBoolean(groupSep));
        // Fraction Digits?
        Object limitFractionDigits = (isInteger ? null : getColumnAttribute(columnExpr, Column.COLATTR_FRACTION_DIGITS));
        if (limitFractionDigits != null)
        {   // get limits from column
            int minFactionDigits = 0;
            int maxFactionDigits = -1;
            if (!(limitFractionDigits instanceof Number)) {
                // not a number
                String limit = limitFractionDigits.toString();
                if (limit.startsWith("min:")) {
                    minFactionDigits = ObjectUtils.getInteger(limit.substring(4), 0);
                }
                else if (limit.startsWith("max:")) {
                    maxFactionDigits = ObjectUtils.getInteger(limit.substring(4), -1);
                }
                else if ("auto".equals(limitFractionDigits)) {
                    minFactionDigits = 999; /* make equal to maxFactionDigits */
                }
                else if (!"limit".equals(limitFractionDigits)) {
                    minFactionDigits = maxFactionDigits = ObjectUtils.getInteger(limit, -1);
                }
            }
            else minFactionDigits = maxFactionDigits = ((Number)limitFractionDigits).intValue();
            // check range
            if (minFactionDigits<0)
                minFactionDigits = 0;
            if (maxFactionDigits<0) {
                // Detect from column
                double size = (columnExpr instanceof Column) ? ((Column)columnExpr).getSize() : 0.0d;
                int intLen = (int)size;
                maxFactionDigits = (int)(size*10)-(intLen*10);
                if (minFactionDigits > maxFactionDigits)
                    minFactionDigits = maxFactionDigits;
            }
            // Set 
            nf.setMinimumFractionDigits(minFactionDigits);
            nf.setMaximumFractionDigits(maxFactionDigits);
        }
        // IntegerDigits (left-padding)
        Object intDigits = getColumnAttribute(columnExpr, Column.COLATTR_INTEGER_DIGITS);
        if (intDigits != null) {
            int integerDigits = ObjectUtils.getInteger(intDigits);
            if (integerDigits>0)
                nf.setMinimumIntegerDigits(integerDigits);
        }
        // Number format
        return nf;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.empire.commons.ColumnUtils#setColumnNumberFormat(ColumnExpr, String, boolean)
     */
    public void setColumnNumberFormat(ColumnExpr columnExpr, String numberType, boolean groupSeparator)
    {
        // Number type
        columnExpr.setAttribute(Column.COLATTR_NUMBER_TYPE, numberType);
        columnExpr.setAttribute(Column.COLATTR_NUMBER_GROUPSEP, groupSeparator);
        if (columnExpr.getDataType()==DataType.DECIMAL || columnExpr.getDataType()==DataType.FLOAT)
        {   // set fraction digits
            double size = getColumnSize(columnExpr);
            int intDigits = (int)size;
            int fracDigits = ((int)(size*10)-(intDigits*10));
            columnExpr.setAttribute(Column.COLATTR_FRACTION_DIGITS, fracDigits);
        }
        else
        {   // Integer
            columnExpr.setAttribute(Column.COLATTR_FRACTION_DIGITS, 0);
        }
    }
    
}
