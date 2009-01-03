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

import java.util.Locale;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.empire.commons.ErrorInfo;
import org.apache.empire.commons.ErrorType;
import org.apache.empire.commons.Options;
import org.apache.empire.commons.StringUtils;
import org.apache.empire.data.Column;
import org.apache.empire.struts2.action.RequestParamProvider;
import org.apache.empire.struts2.html.HtmlTagDictionary;
import org.apache.empire.struts2.html.HtmlWriter;
import org.apache.empire.struts2.html.HtmlWriter.HtmlTag;
import org.apache.struts2.views.util.TextUtil;


public abstract class InputControl
{
    // Logger
    protected static Log log = LogFactory.getLog(InputControl.class);

    // Special Input Column Attributes
    public static final String NUMBER_FORMAT_ATTRIBUTE    = "numberFormat";   // "Integer", "Currency", "Percent"  
    public static final String NUMBER_GROUPSEP_ATTRIBUTE  = "numberGroupSeparator"; // boolean
    public static final String NUMBER_FRACTION_DIGITS     = "numberFractionDigits"; // integer
    public static final String MINVALUE_ATTRIBUTE  = "minValue";
    public static final String MAXVALUE_ATTRIBUTE  = "maxValue";
    public static final String CURRENCY_CODE_ATTRIBUTE    = "currencyCode";   // "ISO 4217 code of the currency"  
    public static final String FILE_DATA_COLUMN_ATTRIBUTE = "fileDataColumn";
    
    /**
     * This interface allows access to a value and its metainformation
     * used with the renderData function
     */ 
    public interface ValueInfo
    {
        Column getColumn();
        Options getOptions();
        Object getValue();
        Object getNullValue();
        Locale getUserLocale();
        String getFormat();    // Custom Formatting options specific to each InputControl-type
        String getOnclick();
        String getOndblclick();
        String getTranslation(String text);
        String getCssClass();
        String getCssStyle();
        String getId();
    }

    /**
     * This interface extends the value information by information about the input control
     * used with the renderInput function
     */ 
    public interface ControlInfo extends ValueInfo
    {
        String getName();
        boolean hasError(); // Indicates wheter the value supplied might be invalid
        boolean getDisabled(); // readOnly
        int getHSize();
        int getVSize();
        String getTabindex();
        // String getAccesskey();
        String getOnchange();
        String getOnfocus();
        String getOnblur();
    }

    /**
     * The NO_VALUE constant is used as a return value from getFieldValue
     * to indicate that the value for this column has not been provided
     */ 
    public static final Object NO_VALUE = null; // Fiels has not been specified and will be ignored

    /**
     * The NULL_VALUE constant is used as a return value from getFieldValue
     * to indicate that the field value was provided with the request, but is empty  
     * so that the underlying database field should be set to null
     */ 
    public static final Object NULL_VALUE = ""; // Field will be set to NULL
    

    /**
     * This class wraps a parsing or validation error.
     * You should not use this class directly but through the error function of InputControl 
     */ 
    public static final class FieldValueError implements ErrorInfo
    {
        private ErrorType errType;  // Type of error
        private String[]  errParams;  // Error message params
        private String    errSource;  // Source object's class name
        private String    errValue;
        public FieldValueError(ErrorType errorType, String[] msgParams, String value, Class source)
        {
            errType    = errorType;
            errParams  = msgParams;
            errSource  = source.getName();
            this.errValue = value;
        }
        public boolean hasError()
        {
            return true;
        }
        public ErrorType getErrorType()
        {
            return errType;
        }
        public String getValue()
        {
            return errValue;
        }
        public String[] getErrorParams()
        {
            return errParams;
        }
        public String getErrorSource()
        {
            return errSource;
        }
        
    }

    /**
     * This method can be used to return a parsing or validation error
     * To be used in the getFieldValue, the parseValue or the validate function.
     * It is a shortcut for writing 
     *      return error(InputErrors.xxxx, null, value)
     * instead of 
     *      return new FieldValueError(InputErrors.xxxx, null, value, getClass());
     * 
     * @return the field Error Object 
     */
    protected final FieldValueError error(ErrorType errorType, String[] msgParams, String value)
    {
        return new FieldValueError(errorType, msgParams, value, getClass());
    }
    
    protected final FieldValueError error(ErrorType errorType, Object msgParam, String value)
    {
        return error(errorType, new String[] { StringUtils.toString(msgParam) }, value);
    }
    
    /**
     * This method determines whether an id should be put on the input label
     * If true than the renderInput function must add an id attribute to an input field
     * The value of the id is supplied with the ControlInfo 
     * 
     * @return true if an id should be set on the label or false otherwise 
     */
    public boolean useLabelId()
    {
        return true;
    }

    /**
     * this method parses and validates the value of a particular column from the request 
     * 
     * @param name the name under which the param is stored on the request
     * @param request used to access the request Parameters 
     * @param column the column for which the value should be 
     * @return the parsed and validated record field value or a FieldValueError if an error occurred
     */
    public Object getFieldValue(String name, RequestParamProvider request, Locale locale, Column column)
    {
        String value = request.getRequestParam(name);
        if (value==null)
            return NO_VALUE;
        // Empty String?
        value = value.trim();
        if (value.length()==0)
            return NULL_VALUE;
        // Parse value
        Object object = parseValue(value, locale, column);
        if (object instanceof FieldValueError)
            return object;
        // Done
        return validate(object, locale, column, value);
    }
    
    /**
     * this method renders a record value read only
     * this can be either in a read only form or a table 
     * 
     * @param writer the HtmlWriter for html write-out
     * @param vi Object holding the value and meta-information about the value 
     */
    public void renderText(HtmlWriter writer, ValueInfo vi)
    {
        if (vi instanceof ControlInfo)
        {   // Wrap read only in a div if it's a control
            ControlInfo ci = ((ControlInfo)vi);
            HtmlTagDictionary dic = HtmlTagDictionary.getInstance();
            HtmlTag div = writer.startTag(dic.InputReadOnlyDataWrapperTag());
            div.addAttribute("id",    ci.getId());
            div.addAttribute("class", ci.getCssClass());
            div.addAttribute("style", ci.getCssStyle());
            div.beginBody();
            internalRenderText(writer, vi);
            div.endTag();
        }
        else
        {
            internalRenderText(writer, vi);
        }
    }

    /**
     * this method renders a record value for input
     * 
     * @param writer the HtmlWriter for html write-out
     * @param ci Object holding the value and meta-information about the value and the control 
     */
    public abstract void renderInput(HtmlWriter writer, ControlInfo ci);

    // ------- helpers -------

    /**
     * this method renders a record value read only
     * it is internally called by renderText 
     * 
     * @param writer the HtmlWriter for html write-out
     * @param vi Object holding the value and meta-information about the value 
     */
    protected void internalRenderText(HtmlWriter writer, ValueInfo vi)
    {
        printText(writer, formatValue(vi), "&nbsp;");
    }
    
    /**
     * writes out plain text to the output stream
     * if the text supplied is null or an empty String then a &nbsp; is written
     * 
     * @param writer the HtmlWriter for html write-out
     * @param text the text to write 
     */
    protected final void printText(HtmlWriter writer, String text, String defaultValue)
    {
        writer.print((text!=null && text.length()>0) ? text : defaultValue);
    }
    
    /**
     * this function may be overridden to parse and convert the input value
     * to the data type of the supplied column
     * 
     * @param value the value string from the request
     * @param the user locale 
     * @param the column for which the value is supplied 
     * 
     * @return the parsed value
     */
    protected Object parseValue(String value, Locale locale, Column column)
    {
        return value;
    }
    
    /**
     * this function may be overridden to validate a value that has
     * previously been parsed 
     * 
     * @param value the parsed object value
     * @param the column for which the value should be validated  
     * @param s the unparsed value string. In case of an error this should be forwarded to the error function. 
     * 
     * @return the parsed value
     */
    protected Object validate(Object value, Locale locale, Column column, String s)
    {
        return value;
    }

    /**
     * Returns the value formated as a string
     * this is a simple default implementation that does no type-secific formatting
     * Derived classes may override formatString an provide further formmatting
     * see TextInputControl for details
     * 
     * @param value the value to be formatted
     * @param vi Meta-information about the value
     *
     * @return the formatted value 
     */
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
        // value
        if (value==null)
            value = vi.getNullValue();
        // Convert to String
        String s = StringUtils.valueOf(value);
        if (hasFormatOption(vi, "noencode"))
            return s;
        // Encode Html
        return TextUtil.escapeHTML(s, false);
    }

    /**
     * Returns the value formated as a string
     * This is a shortcut for formatString(vi.getValue(), vi)
     * Derived classes may override formatString
     */
    protected final String formatValue(ValueInfo vi)
    {
        boolean hasError = ((vi instanceof ControlInfo) && ((ControlInfo)vi).hasError()); 
        return formatValue(vi.getValue(), vi, hasError);
    }
    
    /**
     * checks if a particular formating option has been specified.
     * @param vi the value info
     * @param option the formating option to check
     * @return true if the requested formating option has been specified or false otherwise 
     */
    protected boolean hasFormatOption(ValueInfo vi, String option)
    {
        String format = vi.getFormat();
        return (format!=null ? format.indexOf(option)>=0 : false);
    }
    
    protected String getFormatOption(ValueInfo vi, String option)
    {
        // Is unit supplied with format
        String format = vi.getFormat();
        if (format==null)
            return null;
        // Check for option
        int beg = format.indexOf(option);
        if (beg < 0)
            return null;
        // Find
        beg = beg + option.length();
        int end = format.indexOf(';', beg+1);
        if (end < beg)
            return format.substring(beg);
        // The cbValue
        return format.substring(beg, end);
    }
}
