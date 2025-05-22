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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.el.ValueExpression;
import javax.faces.component.StateHolder;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.component.html.HtmlForm;
import javax.faces.component.html.HtmlInputText;
import javax.faces.component.html.HtmlOutputText;
import javax.faces.component.html.HtmlPanelGroup;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.convert.Converter;
import javax.faces.event.PhaseId;

import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.commons.Options;
import org.apache.empire.commons.StringUtils;
import org.apache.empire.data.Column;
import org.apache.empire.data.DataType;
import org.apache.empire.db.exceptions.FieldIllegalValueException;
import org.apache.empire.exceptions.InvalidArgumentException;
import org.apache.empire.exceptions.InvalidValueException;
import org.apache.empire.exceptions.UnexpectedReturnValueException;
import org.apache.empire.jsf2.components.ControlTag;
import org.apache.empire.jsf2.components.InputTag;
import org.apache.empire.jsf2.utils.TagStyleClass;
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

    public static final String  FRACTION_DIGITS       = "fraction-digits:";
    
    /**
     * NumberInputConverter
     * Formats a decimal value based on the NumberFormat
     */
    public static class NumberInputConverter implements Converter, StateHolder
    {
        private NumberFormat nf;
        private boolean trans = false;
        
        /*
         * Must have a default constructor!
         */
        public NumberInputConverter()
        {
            this.nf = null;
        }
        
        public NumberInputConverter(NumberFormat nf)
        {
            this.nf = nf;
        }

        @Override
        public Object saveState(FacesContext context)
        {
            return nf;
        }

        @Override
        public void restoreState(FacesContext context, Object state)
        {
            this.nf = (NumberFormat)state;
        }

        @Override
        public boolean isTransient()
        {
            return trans;
        }

        @Override
        public void setTransient(boolean newTransientValue)
        {
            this.trans = newTransientValue;
        }
        
        @Override
        public String getAsString(FacesContext context, UIComponent component, Object value)
        {
            if (ObjectUtils.isEmpty(value))
                return StringUtils.EMPTY;
            // Check number
            if (!(value instanceof Number)) {
                log.error("getAsString: \"{}\" is not a number!", value.getClass().getName());
                return value.toString();
            }
            // format
            return (nf!=null ?  nf.format(value) : value.toString());
        }

        @Override
        public Object getAsObject(FacesContext context, UIComponent component, String value)
        {
            if (ObjectUtils.isEmpty(value))
                return null;
            try
            {   // parse
                if (nf==null)
                    return new BigDecimal(value);
                // parse now
                Number number = nf.parse(value);
                if (number instanceof BigDecimal)
                {   // Round to scale
                    int scale = nf.getMaximumFractionDigits();
                    number = ((BigDecimal)number).setScale(scale, RoundingMode.HALF_UP);
                }
                return number;
            }
            catch (ParseException e)
            {   // find column
                UIComponent inputComp = component.getParent();
                while (inputComp!=null) {
                    // set the tag
                    if ((inputComp instanceof InputTag) || (inputComp instanceof ControlTag))
                    {   // Found an InputTag or ControlTag
                        Object column = inputComp.getAttributes().get("column");
                        if (column instanceof Column)
                            throw new FieldIllegalValueException((Column)column, value);
                    }
                    inputComp = inputComp.getParent();
                    if (inputComp instanceof HtmlForm)
                        break;
                }
                // Just throw an InvalidValueException
                throw new InvalidValueException(value);
            }
        }
    }
    
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
        if (StringUtils.isNotEmpty(unit) && !hasFormatOption(ii, "nounit"))
        {   // add the unit
            compList.add(createUnitLabel(TagStyleClass.UNIT.get(), ii, unit));
        }
        // add hint
        String hint = StringUtils.toString(ii.getAttribute("hint"));
        if (StringUtils.isNotEmpty(hint) && !ii.isDisabled())
        {   // add the hint (if not an empty string!)
            compList.add(createUnitLabel(TagStyleClass.INPUT_HINT.get(), ii, hint));
        }
        // update
        updateInputState(compList, ii, context, PhaseId.RENDER_RESPONSE);
    }

    @Override
    protected void updateInputState(List<UIComponent> compList, InputInfo ii, FacesContext context, PhaseId phaseId)
    {
        UIComponent comp = compList.get(0);
        if (!(comp instanceof HtmlInputText))
        {
            throw new UnexpectedReturnValueException(comp.getClass().getName(), "compList.get(0)");
        }
        HtmlInputText input = (HtmlInputText) comp;
        // disabled
        DisabledType disabled = ii.getDisabled();
        input.setReadonly((disabled==DisabledType.READONLY));
        input.setDisabled((disabled==DisabledType.DISABLED));
        // set value
        if (phaseId==PhaseId.RENDER_RESPONSE)
        {   // style
            setInputStyleClass(input, ii);
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
        /* 
        --- dataTypeClass not needed --- 
        String dataTypeClass = TagEncodingHelper.getDataTypeClass(ii.getColumn().getDataType()); 
        String styleClass    = TagEncodingHelper.getTagStyleClass(tagStyle, dataTypeClass, null, null);
        */
        span.getAttributes().put(InputControl.CSS_STYLE_CLASS, tagStyle);
        span.getChildren().add(text);
        // done 
        return span;
    }

    @Override
    protected void setInputStyleClass(UIInput input, String cssStyleClass)
    {
        if (input instanceof HtmlInputText)
            ((HtmlInputText)input).setStyleClass(cssStyleClass);
        else
            super.setInputStyleClass(input, cssStyleClass);
    }

    @Override
    protected void setInputValueExpression(UIInput input, ValueExpression value, InputInfo ii)
    {
        super.setInputValueExpression(input, value, ii);
        // establish converter for decimal
        DataType dataType = ii.getColumn().getDataType();
        if (dataType.isNumeric())
        {   // get number format
            NumberFormat nf;
            if (dataType == DataType.INTEGER || dataType == DataType.AUTOINC)
                nf = NumberFormat.getIntegerInstance(ii.getLocale()); // Integer only
            else {
                // Decimal or Float
                nf = getNumberFormat(dataType, ii, ii.getColumn());
                // ParseBigDecimal
                if (nf instanceof DecimalFormat)
                    ((DecimalFormat)nf).setParseBigDecimal((dataType==DataType.DECIMAL));
            }
            // create converter
            input.setConverter(new NumberInputConverter(nf));
        }
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
        {   // get IntegerFormat and parse
            NumberFormat nf = NumberFormat.getIntegerInstance(ii.getLocale());
        	return parseNumber(value, nf);
        }
        if (type == DataType.DECIMAL || type == DataType.FLOAT)
        {   // get number format
            NumberFormat nf = getNumberFormat(type, ii, column);
            if (nf instanceof DecimalFormat)
                ((DecimalFormat)nf).setParseBigDecimal((type==DataType.DECIMAL));
            // parse
            return parseNumber(value, nf);
        }
        if (type == DataType.DATE || type == DataType.DATETIME || type == DataType.TIMESTAMP)
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
        // Lookup and return text
        Options options = vi.getOptions();
        if (options != null && !hasFormatOption(vi, "nolookup"))
        {   // getOptionText
            String text = getOptionText(options, value, vi);
            if (text!=null)
                return text;
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
        if (dataType.isText() || dataType == DataType.UNKNOWN)
        { // String
            return String.valueOf(value);
        }
        if (dataType == DataType.INTEGER || dataType == DataType.AUTOINC)
        { // Integer
            NumberFormat nf = NumberFormat.getIntegerInstance(vi.getLocale());
            nf.setGroupingUsed(false);
            return nf.format(value);
        }
        if (dataType == DataType.DECIMAL || dataType == DataType.FLOAT)
        { // Dezimal oder Double
            NumberFormat nf = getNumberFormat(dataType, vi, column);
            return nf.format(value);
        }
        if (dataType == DataType.DATE || dataType == DataType.DATETIME || dataType == DataType.TIMESTAMP)
        { // Date or DateTime
            if (dataType!= DataType.DATE && hasFormatOption(vi, "notime"))
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
        return formatValue(value, ii);
    }

    // ------- render -------

    @Override
    public void renderValue(Object value, ValueInfo vi, ResponseWriter writer)
        throws IOException
    {
        boolean escapeHtml = !hasFormatOption(vi, "noescape"); 
        String text = formatValue(value, vi, escapeHtml);
        if (StringUtils.isEmpty(text))
        {   // nothing
            writer.append(HTML_EXPR_NBSP);
            return;
        }
        // append text
        super.renderValue(value, vi, writer);
        // unit?
        String unit = getUnitString(vi);
        if (StringUtils.isNotEmpty(unit) && !hasFormatOption(vi, "nounit"))
        { // append unit
            writer.append(" ");
            writer.append(unit);
        }
    }

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
        if (type == DataType.CHAR || type == DataType.VARCHAR)
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
        if (type == DataType.DATETIME || type == DataType.TIMESTAMP)
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
            return DataType.VARCHAR;
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
            if (desiredType == DataType.DATETIME || desiredType == DataType.DATE || desiredType == DataType.TIMESTAMP)
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

    protected NumberFormat getNumberFormat(DataType dataType, ValueInfo vi, Column column)
    {
        Locale locale = vi.getLocale();
        if (column == null)
            return NumberFormat.getNumberInstance(locale);
        // Column is supplied
        String type = StringUtils.valueOf(column.getAttribute(Column.COLATTR_NUMBER_TYPE));
        boolean isInteger = "Integer".equalsIgnoreCase(type);
        NumberFormat nf = (isInteger) ? NumberFormat.getIntegerInstance(locale)
                                      : NumberFormat.getNumberInstance(locale);
        // Groups Separator?
        Object groupSep = column.getAttribute(Column.COLATTR_NUMBER_GROUPSEP);
        nf.setGroupingUsed(groupSep != null && ObjectUtils.getBoolean(groupSep));
        // Fraction Digits?
        Object limitFractionDigits = (isInteger ? null : getFormatOption(vi, FRACTION_DIGITS, Column.COLATTR_FRACTION_DIGITS));
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
                int length = (int)column.getSize();
                maxFactionDigits = (int)(column.getSize()*10)-(length*10);
                if (minFactionDigits > maxFactionDigits)
                    minFactionDigits = maxFactionDigits;
            }
            // Set 
            nf.setMinimumFractionDigits(minFactionDigits);
            nf.setMaximumFractionDigits(maxFactionDigits);
        }
        else if (!isInteger && dataType==DataType.DECIMAL) 
        {   // Detect from column
            int length = (int)column.getSize();
            int maxFactionDigits = (int)(column.getSize()*10)-(length*10);
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
            return (format.length()>0 ? vi.getTextResolver().resolveText(format) : null);
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

    protected Object parseNumber(String value, NumberFormat nf)
    {
        try
        {   // Parse Number
            Number number = nf.parse(value);
            if (number instanceof BigDecimal)
            {   // Round to scale
                int scale = nf.getMaximumFractionDigits();
                number = ((BigDecimal)number).setScale(scale, RoundingMode.HALF_UP);
            }
            return number;
        }
        catch (ParseException pe)
        {
            throw new NumberFormatException("Not a number: " + value + " Exception: " + pe.toString());
        }
    }

    protected Object parseDate(String s, DateFormat df)
    {
        try
        {   // Parse Date
            df.setLenient(true);
            return df.parseObject(s);
        } catch (ParseException pe) {
            throw new RuntimeException("Invalid date format: " + s, pe);
        }
    }

}
