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
import java.util.regex.Pattern;

import org.apache.empire.data.Column;
import org.apache.empire.data.DataType;
import org.apache.empire.exceptions.NotSupportedException;

/**
 * ColumnUtils
 * This class contains a set of helper functions for Columns
 * @author doebele
 */
public class ColumnUtils
{
    /**
     * Returns true if the column is a text column
     * @param column the column
     * @return true if the column is a text column
     */
    public static boolean isText(Column column)
    {
        return column.getDataType().isText();
    }
    
    /**
     * Return the maximum character length of a column
     * @param column the column 
     * @return the max number or characters 
     */
    public static int getMaxLength(Column column)
    {
        switch(column.getDataType())
        {
            case INTEGER:
            case AUTOINC:
                int intSize = (int)column.getSize();
                return (intSize>0 ? intSize : 12);    
            case CHAR:
            case VARCHAR:
                return (int)column.getSize();
            case DATE:
            case TIME:
                return 10; 
            case DATETIME:
            case TIMESTAMP:
                return 20; 
            case FLOAT:
            case DECIMAL:
                double size = column.getSize();
                int prec  = (int)size;
                int scale = ((int)(size*10)-(prec*10));
                return (scale>0 ? prec+scale+1 : prec);                 
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

    /**
     * Returns the minimum length, usually 0 
     * @param column the column
     * @return the minimum length 
     */
    public static int getMinLength(Column column)
    {
        Object value = column.getAttribute(Column.COLATTR_MINLENGTH);
        return ((value instanceof Number) ? ((Number)value).intValue() : 0);
    }

    /**
     * Returns the text to display if the field value is null 
     * @param column the column
     * @return the text for null 
     */
    public static String getNullText(Column column)
    {
        Object value = column.getAttribute(Column.COLATTR_NULLTEXT);
        return (value!=null ? value.toString() : StringUtils.EMPTY);
    }
    
    /**
     * Sets the text to display if the field value is null 
     * @param column the column
     * @param nullText the null text
     * @return return the column
     */
    public static <T extends Column> T setNullText(T column, String nullText)
    {
        return column.setAttribute(Column.COLATTR_NULLTEXT, nullText);
    }
    
    /**
     * Returns whether or not a column is case sensitive
     * If not explicitly set, the case sensitivity is true for all text fields (VARCHAR, CLOB) except if an EnumType is set.
     * @return true if the column is case sensitive or false if not
     */
    public static boolean isCaseSensitive(Column column)
    {
        Object value = column.getAttribute(Column.COLATTR_CASESENSITIVE);
        if (value==null)
        {   // default is true for VARCHAR and CLOB except if EnumType is set
            return isText(column) && (column.getEnumType()==null);
        }
        return ObjectUtils.getBoolean(value);
    }
    
    /**
     * Sets the case sensitivity of the column
     * @param column the column
     * @param caseSensitiv may be true, false or null
     * @return return the column
     */
    public static <T extends Column> T setCaseSensitive(T column, Boolean caseSensitiv)
    {
        if (!isText(column))
            throw new NotSupportedException(column, "setCaseInsensitive");
        // set now
        return column.setAttribute(Column.COLATTR_CASESENSITIVE, caseSensitiv);
    }
    
    /**
     * Sets one or more columns to case insensitive
     * @param columns the list of columns
     */
    public static void setCaseInsensitive(Column... columns)
    {
        for (int i=0; i<columns.length; i++)
        {
            Column column = columns[i];
            if (column==null || !isText(column))
                continue;
            // set as insensitive
            column.setAttribute(Column.COLATTR_CASESENSITIVE, false);
        }
    }

    /**
     * Returns true if the column is a numeric column
     * @param column the column to check
     * @return true if the column is numeric
     */
    public static boolean isNumeric(Column column)
    {
        return column.getDataType().isText();
    }
    
    /**
     * Returns the maximum allowed value or 0 if not set 
     * @param column the column
     * @return the minimum value 
     */
    public static BigDecimal getMinValue(Column column)
    {
        Object value = column.getAttribute(Column.COLATTR_MINVALUE);
        return ObjectUtils.getDecimal(value);
    }

    /**
     * Returns the maximum allowed value 
     * @param column the column
     * @return the maximum value 
     */
    public static BigDecimal getMaxValue(Column column)
    {
        Object value = column.getAttribute(Column.COLATTR_MAXVALUE);
        long maxVal = isNumeric(column) ? 10l^((long)column.getSize()) : 10^12;
        return ObjectUtils.getDecimal(value, BigDecimal.valueOf(maxVal));
    }
    
    /**
     * Set the minimum and maximum values of a column 
     * @param column the column
     * @param minValue the min value
     * @param maxValue the max value
     * @return the column
     */
    public static <T extends Column> T setMinMaxValue(T column, BigDecimal minValue, BigDecimal maxValue)
    {
        if (!isNumeric(column))
            throw new NotSupportedException(column, "setMinMaxValue");
        // set the min and max values
        column.setAttribute(Column.COLATTR_MINVALUE, minValue);
        column.setAttribute(Column.COLATTR_MAXVALUE, maxValue);
        return column;
    }

    /**
     * Returns the number format for a particular column
     * @param column the column
     * @param locale the locale for which to get the format
     * @return the NumberFormat
     */
    public static NumberFormat getNumberFormat(Column column, Locale locale)
    {
        // Column is supplied
        String type = StringUtils.valueOf(column.getAttribute(Column.COLATTR_NUMBER_TYPE));
        boolean isInteger = "Integer".equalsIgnoreCase(type);
        NumberFormat nf = (isInteger) ? NumberFormat.getIntegerInstance(locale)
                                      : NumberFormat.getNumberInstance(locale); 
        // Groups Separator?
        Object groupSep = column.getAttribute(Column.COLATTR_NUMBER_GROUPSEP);
        nf.setGroupingUsed(groupSep != null && ObjectUtils.getBoolean(groupSep));
        // Fraction Digits?
        Object limitFractionDigits = (isInteger ? null : column.getAttribute(Column.COLATTR_FRACTION_DIGITS));
        if (limitFractionDigits != null)
        {   // get column limits
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
                int intLen = (int)column.getSize();
                maxFactionDigits = (int)(column.getSize()*10)-(intLen*10);
                if (minFactionDigits > maxFactionDigits)
                    minFactionDigits = maxFactionDigits;
            }
            // Set 
            nf.setMinimumFractionDigits(minFactionDigits);
            nf.setMaximumFractionDigits(maxFactionDigits);
        }
        // IntegerDigits (left-padding)
        Object intDigits = column.getAttribute(Column.COLATTR_INTEGER_DIGITS);
        if (intDigits != null) {
            int integerDigits = ObjectUtils.getInteger(intDigits);
            if (integerDigits>0)
                nf.setMinimumIntegerDigits(integerDigits);
        }
        // Number format
        return nf;
    }
    
    /**
     * Sets the number format options for a column 
     * @param column the column
     * @param numberType the number type 
     * @param true groupSeparator should be used 
     * @return the column
     */
    public static <T extends Column> T setNumberFormat(T column, String numberType, boolean groupSeparator)
    {
        if (!isNumeric(column))
            throw new NotSupportedException(column, "setNumberFormat");
        // Number type
        column.setAttribute(Column.COLATTR_NUMBER_TYPE, numberType);
        column.setAttribute(Column.COLATTR_NUMBER_GROUPSEP, groupSeparator);
        if (column.getDataType()==DataType.DECIMAL || column.getDataType()==DataType.FLOAT)
        {   // set fraction digits
            double size = column.getSize();
            int intDigits = (int)size;
            int fracDigits = ((int)(size*10)-(intDigits*10));
            column.setAttribute(Column.COLATTR_FRACTION_DIGITS, fracDigits);
        }
        return column;
    }

    /**
     * Returns the maximum allowed value 
     * @param column the column
     * @return the minimum length 
     */
    public static Pattern getRegExPattern(Column column)
    {
        Object value = column.getAttribute(Column.COLATTR_REGEXP);
        return ((Pattern)value);
    }
    
    /**
     * Set the regular expression to validate the column value 
     * @param column the column
     * @param regex the regular expression
     * @return the column
     */
    public static <T extends Column> T setRegExPattern(T column, String regex)
    {
        column.setAttribute(Column.COLATTR_REGEXP, Pattern.compile(regex));
        return column;
    }
    
    /**
     * Returns the normalized column for the column (if any)
     * @return the normalized column or null
     */
    @SuppressWarnings("unchecked")
    public static <T extends Column> T getNormalizedColumn(Column column)
    { 
        return (T)column.getAttribute(Column.COLATTR_NORMCOLUMN);
    }
    
    /**
     * Sets a normalized column for this column
     * @param column the column
     * @param normalizedColumn the normalized column
     * @return returns self (this)
     */
    public static <T extends Column> T setNormalizedColumn(T column, Column normalizedColumn)
    { 
        return column.setAttribute(Column.COLATTR_NORMCOLUMN, normalizedColumn);
    }

}
