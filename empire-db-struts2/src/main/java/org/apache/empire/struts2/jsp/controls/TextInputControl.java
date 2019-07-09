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
package org.apache.empire.struts2.jsp.controls;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.Date;
import java.util.Locale;

import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.commons.Options;
import org.apache.empire.commons.StringUtils;
import org.apache.empire.data.Column;
import org.apache.empire.data.DataType;
import org.apache.empire.struts2.html.HtmlWriter;
import org.apache.empire.struts2.html.HtmlWriter.HtmlTag;
import org.apache.empire.struts2.web.FieldErrors;
import org.apache.commons.lang.xwork.StringEscapeUtils;


public class TextInputControl extends InputControl
{
    public static final String FORMAT_UNIT = "unit:";
    
    // ------- parsing -------

    @Override
    protected Object parseValue(String value, Locale locale, Column column)
    {
        // Check Data Type
        DataType type = column.getDataType();
        if (type.isText())
            return value;
        // Check other types
        if (type==DataType.INTEGER)
        {   return parseInteger(value);
        }
        if (type==DataType.DECIMAL)
        {   return parseDecimal(value, getNumberFormat(column.getDataType(), locale, column));
        }
        if (type==DataType.DATE || type==DataType.DATETIME || type==DataType.TIMESTAMP)
        {   return parseDate(value, getDateFormat(column.getDataType(), locale, column));
        }
        if (type==DataType.AUTOINC)
        {   // autoinc
            log.error("Autoinc-value cannot be changed.");
            return NO_VALUE;
        }
        // Default
        return value;
    }
    
    // ------- validation -------

    @Override
    protected Object validate(Object o, Locale locale, Column column, String s)
    {
        if (o instanceof Number)
        {
            Object min = column.getAttribute(InputControl.MINVALUE_ATTRIBUTE);
            Object max = column.getAttribute(InputControl.MAXVALUE_ATTRIBUTE);
            if (min!=null && max!=null)
            {
                Number n = (Number)o;
                if (n.intValue()<ObjectUtils.getInteger(min) ||
                    n.intValue()>ObjectUtils.getInteger(max))
                {   // Out of Range
                    return error(FieldErrors.InputValueOutOfRange, new String[] { min.toString(), max.toString() }, s);
                }
            }
        }
        return o;
    }
    
    // ------- formatting -------

    @Override
    protected String formatValue(Object value, ValueInfo vi, boolean hasError)
    {
        // Lookup and Print value
        Options options = vi.getOptions();
        if (options != null && !options.isEmpty())
        { // Check for Options
            String text = options.get(value);
            if (text != null)
                return vi.getTranslation(text);
            // Error
            log.error("The element '" + String.valueOf(value) + "' is not part of the supplied option list.");
        }
        // Check Value
        if (value == null)
        {   // Try to use default value 
            if (value!=vi.getNullValue())
                return formatValue(vi.getNullValue(), vi, false);
            // Empty String
            return "";
        }
        // Format Value
        Column column = vi.getColumn();
        DataType dataType = getValueType(value, (column != null) ? column.getDataType() : DataType.UNKNOWN);
        if (dataType.isText() || dataType == DataType.UNKNOWN)
        { // String
            String s = String.valueOf(value);
            if (hasFormatOption(vi, "noencode"))
                return s;
            // Encoded text
            return StringEscapeUtils.escapeHtml(s);
        }
        if (dataType == DataType.INTEGER || dataType == DataType.AUTOINC)
        { // Integer
            NumberFormat nf = NumberFormat.getIntegerInstance(vi.getUserLocale());
            nf.setGroupingUsed(false);
            return nf.format(value);
        }
        if (dataType == DataType.DECIMAL || dataType == DataType.FLOAT)
        { // Dezimal oder Double
            NumberFormat nf = getNumberFormat(dataType, vi.getUserLocale(), column);
            return nf.format(value);
        }
        if (dataType == DataType.DATE || dataType == DataType.DATETIME || dataType == DataType.TIMESTAMP)
        { // Date or DateTime
            if (dataType!= DataType.DATE && hasFormatOption(vi, "notime"))
                dataType = DataType.DATE;
            // Now format the date according to the user's locale
            DateFormat df = getDateFormat(dataType, vi.getUserLocale(), column);
            return df.format(value);
        }
        /*
         * if (dataType == DBDataType.BOOL) {
         *  }
         */
        // Convert to String
        return StringEscapeUtils.escapeHtml(String.valueOf(value));
    }

    protected String formatValue(ValueInfo vi, boolean appendUnit)
    {
        String text = super.formatValue(vi);
        if (appendUnit && text!=null && text.length()>0)
        {
            String unit = getUnitString(vi);
            if (unit != null)
            {   // Append unit
                text += " " + unit;
            }
        }
        return text;
    }

    // ------- render -------
    
    @Override
    protected void internalRenderText(HtmlWriter writer, ValueInfo vi)
    {
        String text = formatValue(vi, true);
        printText(writer, text, "&nbsp;");
    }
    
    @Override
    public void renderInput(HtmlWriter writer, ControlInfo ci)
    {
        HtmlTag input = writer.startTag("input");
        input.addAttribute("type", "text");
        input.addAttribute("id",    ci.getId());
        input.addAttribute("class", ci.getCssClass());
        input.addAttribute("style", ci.getCssStyle());
        if (ci.getDisabled()==false)
        {   // Name of the field
            input.addAttribute("name", ci.getName());
            // Get Max Length
            int maxLength = getMaxInputLength(ci.getColumn());
            if (maxLength>0)
            {
                input.addAttribute("maxlength", maxLength);
                input.addAttribute("size", String.valueOf(Math.min(maxLength, ci.getHSize())));
            }   
        }
        else
        {   // Disabled text control
            input.addAttribute("disabled");
            // Get Max Length
            int maxLength = getMaxInputLength(ci.getColumn());
            if (maxLength>0)
            {
                input.addAttribute("size", String.valueOf(Math.min(maxLength, ci.getHSize())));
            }   
        }
        // Value
        input.addAttribute("value", formatValue(ci, ci.getDisabled()));
        // Event Attributes
        input.addAttribute("onclick",   ci.getOnclick());
        input.addAttribute("onchange",  ci.getOnchange());
        input.addAttribute("onfocus",   ci.getOnfocus());
        input.addAttribute("onblur",    ci.getOnblur());
        input.endTag();
        // Add Unit
        if (ci.getDisabled()==false)
        {   
            String unit = getUnitString(ci);
            if (unit != null)
            {   writer.print(" ");
                writer.print(unit);
            }
        }
    }
    
    // ------- Input Helpers -------

    protected int getMaxInputLength(Column col)
    {
        // cast to DBTableColumn 
        DataType type = col.getDataType();
        if (type==DataType.AUTOINC ||
            type==DataType.INTEGER)
            return 10; 
        if (type==DataType.FLOAT)
            return 18;
        if (type==DataType.DECIMAL)
        {   
            double size = col.getSize();
            int len = (int)size;
            size = (size - len)*10;   // Ganzahlanteil
            if (((int)size)>0)
                len += ((int)size)+1; // Dezimaltrenner plus Nachkommastellen
            return len;
        }
        if (type==DataType.BOOL)
            return 1;
        if (type==DataType.DATE)
            return 10;
        if (type==DataType.DATETIME || type==DataType.TIMESTAMP)
            return 16;
        if (type==DataType.CLOB)
            return 0; // unlimited (use 0x7FFFFFFF instead?)
        // Default
        return (int)col.getSize();
    }
    
    protected DataType getValueType(Object value, DataType desiredType)
    {
        // Detect Data Type from Value
        if (value instanceof String)
            return DataType.VARCHAR;
        if (value instanceof Number)
        { // Check desired type
            if (desiredType == DataType.AUTOINC || desiredType == DataType.INTEGER || 
                desiredType == DataType.FLOAT || desiredType == DataType.DECIMAL)
                return desiredType;
            // Detect type
            if (value instanceof Integer || value instanceof Long || value instanceof Short)
                return DataType.INTEGER;
            if (value instanceof Float || value instanceof Double)
                return DataType.FLOAT;
            // default
            return DataType.DECIMAL;
        }
        if (value instanceof Date)
        { // Check desired type
            if (desiredType == DataType.TIMESTAMP || desiredType == DataType.DATETIME || desiredType == DataType.DATE)
                return desiredType;
            // Detect type
            if (value instanceof Timestamp)
                return DataType.DATETIME;
            // Just a date
            return DataType.DATE;
        }
        if (value instanceof Boolean)
            return DataType.BOOL;
        // Default Datatype
        return DataType.UNKNOWN;
    }
    
    protected NumberFormat getNumberFormat(DataType dataType, Locale locale, Column column)
    {
        if (column==null)
            return NumberFormat.getNumberInstance(locale); 
        // Column is supplied
        String type = StringUtils.valueOf(column.getAttribute(InputControl.NUMBER_FORMAT_ATTRIBUTE));
        NumberFormat nf = null;
        if (type.equalsIgnoreCase("Integer"))
            nf = NumberFormat.getIntegerInstance(locale);
        /*
        else if (type.equalsIgnoreCase("Currency"))
        {   // nf = NumberFormat.getCurrencyInstance(locale);
            // Currency does not work as desired!
            nf = NumberFormat.getNumberInstance(locale);
        }
        else if (type.equalsIgnoreCase("Percent"))
            nf = NumberFormat.getPercentInstance(locale);
        */
        else
            nf = NumberFormat.getNumberInstance(locale);
        // Groups Separator?
        Object groupSep = column.getAttribute(InputControl.NUMBER_GROUPSEP_ATTRIBUTE);
        if (groupSep!=null)
            nf.setGroupingUsed(ObjectUtils.getBoolean(groupSep));
        // Fraction Digits?
        Object fractDigit = column.getAttribute(InputControl.NUMBER_FRACTION_DIGITS);
        if (fractDigit!=null)
        {   int fractionDigits = ObjectUtils.getInteger(fractDigit);
            nf.setMaximumFractionDigits(fractionDigits);
            nf.setMinimumFractionDigits(fractionDigits);
        }
        // Number format
        return nf; 
    }
    
    protected DateFormat getDateFormat(DataType dataType, Locale locale, Column column)
    {
        DateFormat df;
        if (dataType==DataType.DATE)
            df = DateFormat.getDateInstance(DateFormat.MEDIUM, locale);
        else
            df = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, locale);
        return df;
    }

    private String getUnitString(ValueInfo vi)
    {
        // Is unit supplied as a format option
        String format = getFormatOption(vi, FORMAT_UNIT);
        if (format!=null)
            return format;
        // Is it a currency column
        Column column = vi.getColumn();
        if (column!=null && column.getDataType()==DataType.DECIMAL)
        {
            String numberType = StringUtils.toString(column.getAttribute(InputControl.NUMBER_FORMAT_ATTRIBUTE));
            if (numberType!=null)
            {
                if (numberType.equalsIgnoreCase("Currency"))
                {
                    String currencyCode = StringUtils.toString(column.getAttribute(InputControl.CURRENCY_CODE_ATTRIBUTE));
                    if (currencyCode!=null)
                    {   // nf = NumberFormat.getCurrencyInstance(locale);
                        Currency currency = Currency.getInstance(currencyCode);
                        return (currency!=null) ? currency.getSymbol() : null;
                    }
                } else if (numberType.equalsIgnoreCase("Percent"))
                {
                    return "%";
                }
            }
        }
        // No Unit supplied
        return null;
    }
    
    // ------- value parsing -------
    
    protected Object parseInteger(String s)
    {
        // Try to convert
        try {
            // Parse String
            return Integer.parseInt(s);
        } catch(Exception e) {
            return error(FieldErrors.InputNoIntegerFormat, null, s);
        }
    }
    
    protected Object parseDecimal(String s, NumberFormat nf)
    {
        // Try to convert
        try {
            // Check for characters
            for (int i=0; i<s.length(); i++)
            {   if (s.charAt(i)>='A')
                    return error(FieldErrors.InputNoNumberFormat, null, s);
            }
            // Parse String
            return nf.parseObject(s);
        } catch(Exception e) {
            return error(FieldErrors.InputNoNumberFormat, null, s);
        }
    }
    
    protected Object parseDate(String s, DateFormat df)
    {
        // Try to convert
        try {
            // Parse Date
            df.setLenient(true);
            return df.parseObject(s);
        } catch(Exception e) {
            return error(FieldErrors.InputNoDateFormat, null, s);
        }
    }

}
