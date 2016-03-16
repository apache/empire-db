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
package org.apache.empire.jsf2.controls;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.faces.component.UIComponent;
import javax.faces.component.html.HtmlInputText;
import javax.faces.component.html.HtmlOutputText;
import javax.faces.component.html.HtmlPanelGroup;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.commons.Options;
import org.apache.empire.commons.StringUtils;
import org.apache.empire.data.Column;
import org.apache.empire.data.DataType;
import org.apache.empire.exceptions.InvalidArgumentException;
import org.apache.empire.exceptions.UnexpectedReturnValueException;
import org.apache.empire.jsf2.utils.TagEncodingHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TextInputControl extends InputControl
{
    private static final Logger log                   = LoggerFactory.getLogger(TextInputControl.class);

    public static final String  NAME                  = "text";

    public static final String  FORMAT_UNIT           = "unit:";
    public static final String  FORMAT_UNIT_ATTRIBUTE = "format:unit";

    public static final String  DATE_FORMAT           = "date-format:";
    public static final String  DATE_FORMAT_ATTRIBUTE = "format:date";

    private Class<? extends HtmlInputText> inputComponentClass;

    public TextInputControl(String name, Class<? extends HtmlInputText> inputComponentClass)
    {
        super(name);
        this.inputComponentClass = inputComponentClass;
    }

    public TextInputControl(String name)
    {
        this(name, javax.faces.component.html.HtmlInputText.class);
    }

    public TextInputControl()
    {
        this(TextInputControl.NAME, javax.faces.component.html.HtmlInputText.class);
    }

    @Override
    protected void createInputComponents(UIComponent parent, InputInfo ii, FacesContext context, List<UIComponent> compList)
    {
        // check params
        if (!compList.isEmpty())
            throw new InvalidArgumentException("compList", compList);
        // create
        HtmlInputText input = InputControlManager.createComponent(context, this.inputComponentClass);
        // once
        copyAttributes(parent, ii, input);
        // language
        input.setLang(ii.getLocale().getLanguage());
        // maxlength
        int maxLength = getMaxInputLength(ii);
        if (maxLength > 0)
        {
            input.setMaxlength(maxLength);
        }
        // add
        compList.add(input);
        // add unit
        String unit = getUnitString(ii);
        if (StringUtils.isNotEmpty(unit))
        {   // add the unit
            compList.add(createUnitLabel("eUnit", ii, unit));
        }
        // add hint
        String hint = StringUtils.toString(ii.getAttribute("hint"));
        if (StringUtils.isNotEmpty(hint) && !ii.isDisabled())
        {   // add the hint (if not an empty string!)
            compList.add(createUnitLabel("eInputHint", ii, hint));
        }
        // update
        updateInputState(compList, ii, context, true);
    }

    @Override
    protected void updateInputState(List<UIComponent> compList, InputInfo ii, FacesContext context, boolean setValue)
    {
        UIComponent comp = compList.get(0);
        if (!(comp instanceof HtmlInputText))
        {
            throw new UnexpectedReturnValueException(comp.getClass().getName(), "compList.get(0)");
        }
        HtmlInputText input = (HtmlInputText) comp;
        // disabled
        Object dis = ii.getAttributeEx("disabled");
        if (dis != null)
        {
            input.setDisabled(ObjectUtils.getBoolean(dis));
        }
        // field-readOnly
        if (ObjectUtils.getBoolean(dis) == false)
        {
            input.setReadonly(ii.isFieldReadOnly());
        }
        // set value
        if (setValue)
        {   // style
            addRemoveDisabledStyle(input, (input.isDisabled() || input.isReadonly()));
            addRemoveInvalidStyle(input, ii.hasError());
            // set value
            setInputValue(input, ii);
        }
    }

    protected UIComponent createUnitLabel(String tagStyle, InputInfo ii, String value)
    {
        HtmlOutputText text = new HtmlOutputText();
        text.setValue(value);
        // wrap
        HtmlPanelGroup span = new HtmlPanelGroup();
        String styleClass = TagEncodingHelper.getTagStyleClass(tagStyle, TagEncodingHelper.getDataTypeClass(ii.getColumn().getDataType()),
                                                               null, null);
        span.getAttributes().put("styleClass", styleClass);
        span.getChildren().add(text);
        return span;
    }

    // ------- parsing -------

    @Override
    protected Object parseInputValue(String value, InputInfo ii)
    {
        // Trim
        if (hasFormatOption(ii, "notrim") == false)
        {
            value = value.trim();
        }
        // Check Data Type
        Column column = ii.getColumn();
        DataType type = column.getDataType();
        if (type.isText())
        {
            return value;
        }
        // Check other types
        if (type == DataType.INTEGER)
        {
            NumberFormat nf = NumberFormat.getIntegerInstance(ii.getLocale());
            return parseNumber(value, nf);
        }
        if (type == DataType.DECIMAL || type == DataType.FLOAT)
        {
            NumberFormat nf = NumberFormat.getNumberInstance(ii.getLocale());
            return parseNumber(value, nf);
        }
        if (type == DataType.DATE || type == DataType.DATETIME)
        {
            return parseDate(value, getDateFormat(column.getDataType(), ii, column));
        }
        if (type == DataType.BOOL)
        {
            return ObjectUtils.getBoolean(value);
        }
        if (type == DataType.AUTOINC)
        { // autoinc
            log.error("Autoinc-value cannot be changed.");
            return null;
        }
        // Default
        return value;
    }

    // ------- validation -------
    /*
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
                    return error(WebErrors.InputValueOutOfRange, new String[] { min.toString(), max.toString() }, s);
                }
            }
        }
        return o;
    }
    */

    // ------- formatting -------

    @Override
    protected String formatValue(Object value, ValueInfo vi)
    {
        return formatValue(value, vi, true);
    }

    protected String formatValue(Object value, ValueInfo vi, boolean escapeHTML)
    {
        // Lookup and Print value
        Options options = vi.getOptions();
        if (options != null && !options.isEmpty())
        { // Check for Options
            String text = options.get(value);
            if (text != null)
                return vi.getText(text);
            // Error
            log.error("The element '" + String.valueOf(value) + "' is not part of the supplied option list.");
        }
        // Check Value
        if (value == null)
        { // Try to use default value
            Object nullValue = getFormatOption(vi, InputControl.FORMAT_NULL, InputControl.FORMAT_NULL_ATTRIBUTE);
            if (nullValue != null)
                return formatValue(nullValue, vi);
            // Empty String
            return "";
        }
        // Format Value
        Column column = vi.getColumn();
        DataType dataType = getValueType(value, (column != null) ? column.getDataType() : DataType.UNKNOWN);
        if (dataType == DataType.TEXT || dataType == DataType.UNKNOWN)
        { // String
            String s = String.valueOf(value);
            if (hasFormatOption(vi, "noencode"))
                return s;
            // Encoded text
            if (escapeHTML)
                s = escapeHTML(s);
            return s;
        }
        if (dataType == DataType.INTEGER || dataType == DataType.AUTOINC)
        { // Integer
            NumberFormat nf = NumberFormat.getIntegerInstance(vi.getLocale());
            nf.setGroupingUsed(false);
            return nf.format(value);
        }
        if (dataType == DataType.DECIMAL || dataType == DataType.FLOAT)
        { // Dezimal oder Double
            NumberFormat nf = getNumberFormat(dataType, vi.getLocale(), column);
            return nf.format(value);
        }
        if (dataType == DataType.DATE || dataType == DataType.DATETIME)
        { // Date or DateTime
            if (dataType == DataType.DATETIME && hasFormatOption(vi, "notime"))
                dataType = DataType.DATE;
            // Now format the date according to the user's locale
            DateFormat df = getDateFormat(dataType, vi, column);
            return df.format(value);
        }
        /*
         * if (dataType == DBDataType.BOOL) {
         *  }
         */
        // Convert to String
        if (escapeHTML)
        {
            return escapeHTML(String.valueOf(value));
        }
        return String.valueOf(value);
    }

    /*
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
    */

    @Override
    protected Object formatInputValue(Object value, InputInfo ii)
    {
        if (value == null)
            return "";
        // Check options
        Options options = ii.getOptions();
        if (options != null && !options.isEmpty())
            return value;
        // Format
        return formatValue(value, ii, false);
    }

    // ------- render -------

    @Override
    public void renderValue(ValueInfo vi, ResponseWriter writer)
        throws IOException
    {
        String text = formatValue(vi);
        if (StringUtils.isEmpty(text))
        { // nothing
            writer.append(HTML_EXPR_NBSP);
            return;
        }
        // append text
        writer.append(text);
        // unit?
        String unit = getUnitString(vi);
        if (StringUtils.isNotEmpty(unit))
        { // append unit
            writer.append(" ");
            writer.append(unit);
        }
    }

    /*
    @Override
    public void renderInput(Response writer, ControlInfo ci)
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
    */

    // ------- Input Helpers -------

    protected int getMaxInputLength(InputInfo ii)
    {
        // check custom
        String maxlen = getFormatOption(ii, "maxlength:");
        if (StringUtils.isNotEmpty(maxlen))
        {
            int ml = ObjectUtils.getInteger(maxlen);
            if (ml > 0)
                return ml;
        }

        Column col = ii.getColumn();
        // cast to DBTableColumn 
        DataType type = col.getDataType();
        if (type == DataType.CHAR || 
            type == DataType.TEXT)
            return (int) Math.round(col.getSize());
        if (type == DataType.AUTOINC || type == DataType.INTEGER)
            return 10;
        if (type == DataType.FLOAT)
            return 18;
        if (type == DataType.DECIMAL)
        { // check precision and scale
            double size = col.getSize();
            int prec = (int) Math.round(size);
            if (prec == 0)
                return 0;
            int len = prec;
            // scale
            int scale = ((int) (size * 10) - (prec * 10));
            if (scale > 0)
                len++; // Dezimaltrenner
            // thousand separator ?
            Object groupSep = col.getAttribute(Column.COLATTR_NUMBER_GROUPSEP);
            if (groupSep != null && ObjectUtils.getBoolean(groupSep))
                len += ((prec - scale - 1) / 3);
            // sign?
            Object minVal = col.getAttribute(Column.COLATTR_MINVALUE);
            if (minVal == null || ObjectUtils.getInteger(minVal) < 0)
                len++; // Vorzeichen
            // fertig
            return len;
        }
        if (type == DataType.BOOL)
            return 1;
        if (type == DataType.DATE)
            return 10;
        if (type == DataType.DATETIME)
            return 16;
        if (type == DataType.CLOB)
            return 0; // unlimited (use 0x7FFFFFFF instead?)
        // undefined!
        log.info("No max-length available for data type {}.", type);
        return 0;
    }

    protected DataType getValueType(Object value, DataType desiredType)
    {
        // Detect Data Type from Value
        if (value instanceof String)
            return DataType.TEXT;
        if (value instanceof Number)
        { // Check desired type
            if (desiredType == DataType.AUTOINC || 
                desiredType == DataType.INTEGER || 
                desiredType == DataType.FLOAT || 
                desiredType == DataType.DECIMAL)
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
            if (desiredType == DataType.DATETIME || desiredType == DataType.DATE)
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
        if (column == null)
            return NumberFormat.getNumberInstance(locale);
        // Column is supplied
        String type = StringUtils.valueOf(column.getAttribute(Column.COLATTR_NUMBER_TYPE));
        NumberFormat nf = null;
        if (type.equalsIgnoreCase("Integer"))
            nf = NumberFormat.getIntegerInstance(locale);
        else
            nf = NumberFormat.getNumberInstance(locale);
        // Groups Separator?
        Object groupSep = column.getAttribute(Column.COLATTR_NUMBER_GROUPSEP);
        nf.setGroupingUsed(groupSep != null && ObjectUtils.getBoolean(groupSep));
        // Fraction Digits?
        Object fractDigit = column.getAttribute(Column.COLATTR_FRACTION_DIGITS);
        if (fractDigit != null)
        {   int fractionDigits = ObjectUtils.getInteger(fractDigit);
            nf.setMaximumFractionDigits(fractionDigits);
            nf.setMinimumFractionDigits(fractionDigits);
        }
        // Number format
        return nf;
    }

    protected DateFormat getDateFormat(DataType dataType, ValueInfo vi, Column column)
    {
        String pattern = null;
        int type = DateFormat.DEFAULT;
        // Is unit supplied as a format option
        String format = getFormatString(vi, TextInputControl.DATE_FORMAT, TextInputControl.DATE_FORMAT_ATTRIBUTE);
        if (format != null)
        { // format has been provided
            if (StringUtils.compareEqual(format, "full", true))
                type = DateFormat.FULL;
            else if (StringUtils.compareEqual(format, "medium", true))
                type = DateFormat.MEDIUM;
            else if (StringUtils.compareEqual(format, "short", true))
                type = DateFormat.SHORT;
            else if (StringUtils.compareEqual(format, "long", true))
                type = DateFormat.LONG;
            else
                pattern = format;
        }
        // return date formatter
        DateFormat df;
        if (StringUtils.isNotEmpty(pattern))
            df = new SimpleDateFormat(pattern, vi.getLocale());
        else if (dataType == DataType.DATE)
            df = DateFormat.getDateInstance(type, vi.getLocale());
        else
            df = DateFormat.getDateTimeInstance(type, type, vi.getLocale());
        return df;
    }

    protected String getUnitString(ValueInfo vi)
    {
        // Is unit supplied as a format option
        String format = getFormatString(vi, TextInputControl.FORMAT_UNIT, TextInputControl.FORMAT_UNIT_ATTRIBUTE);
        if (format != null)
        {
            return vi.getTextResolver().resolveText(format);
        }
        // Is it a currency column
        Column column = vi.getColumn();
        if (column != null && column.getDataType() == DataType.DECIMAL)
        {
            String numberType = StringUtils.toString(column.getAttribute(Column.COLATTR_NUMBER_TYPE));
            if (numberType != null)
            {
                if (numberType.equalsIgnoreCase("Currency"))
                {
                    String currencyCode = StringUtils.toString(column.getAttribute(Column.COLATTR_CURRENCY_CODE));
                    if (currencyCode != null)
                    {   // nf = NumberFormat.getCurrencyInstance(locale);
                        // Currency currency = Currency.getInstance(currencyCode);
                        // return (currency != null) ? currency.getSymbol() : null;
                        if (currencyCode.equalsIgnoreCase("EUR"))
                            return "€";
                        if (currencyCode.equalsIgnoreCase("USD"))
                            return "$";
                        // done
                        return currencyCode;
                    }
                }
                else if (numberType.equalsIgnoreCase("Percent"))
                {
                    return "%";
                }
            }
        }
        // No Unit supplied
        return null;
    }

    // ------- value parsing -------

    protected Object parseNumber(String s, NumberFormat nf)
    {
        // Try to convert
        for (int i = 0; i < s.length(); i++)
        {
            if (s.charAt(i) >= 'A')
            {
                throw new NumberFormatException("Not a number: " + s);
            }
        }
        // Parse String
        try {
            return nf.parseObject(s);
        } catch (ParseException pe) {
            throw new NumberFormatException("Not a number: " + s + " Exception: " + pe.toString());
        }
    }

    protected Object parseDate(String s, DateFormat df)
    {
        // Try to convert
        try
        {
            // Parse Date
            df.setLenient(true);
            return df.parseObject(s);
        } catch (ParseException pe) {
            throw new RuntimeException("Invalid date format: " + s, pe);
        }
    }

}
