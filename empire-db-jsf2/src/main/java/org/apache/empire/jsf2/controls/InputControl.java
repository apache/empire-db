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
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.el.ValueExpression;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.commons.Options;
import org.apache.empire.commons.StringUtils;
import org.apache.empire.data.Column;
import org.apache.empire.db.exceptions.FieldIsReadOnlyException;
import org.apache.empire.exceptions.InvalidArgumentException;
import org.apache.empire.exceptions.ObjectNotValidException;
import org.apache.empire.exceptions.UnexpectedReturnValueException;
import org.apache.empire.jsf2.app.TextResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class InputControl
{
    /*
    public static class ColumnValueValidator implements Validator, StateHolder
    {
        private static final Logger log = LoggerFactory.getLogger(ColumnValueValidator.class);
        
        private Column column;

        public ColumnValueValidator()
        {
        }
        
        public ColumnValueValidator(Column column)
        {
            this.column=column;
        }
        
        @Override
        public void validate(FacesContext context, UIComponent component, Object value)
            throws ValidatorException
        {
            try {
                log.info("ColumnValueValidator:validate for column "+column.getName()+" value is: "+String.valueOf(value));
                column.validate(value);
                if (value.equals("test"))
                    throw new FieldIllegalValueException(column, String.valueOf(value));
                
            } catch(Exception e) {
                FacesMessage msg = new FacesMessage(e.getLocalizedMessage());
                throw new ValidatorException(msg);
            }
        }

        @Override
        public Object saveState(FacesContext context)
        {
            /-- *
            try
            {   // Serialization test
                String columnId = ((DBColumn)column).getId();
                
                DBColumn c = DBColumn.findById(columnId);
                if (c==column)
                    log.info("success!");
            
                // findByClass test
                DBDatabase fdb = DBDatabase.findByClass(FinDB.class);
                log.info(fdb.getId());
                
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(baos);
                oos.writeObject(column);
                // oos.writeObject(fdb);
                oos.flush();
                String info = baos.toString();
                System.out.println(info);
                byte[] bytes = baos.toByteArray();
                int size = bytes.length;
                System.out.println("Size is "+String.valueOf(size));
                
                ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
                ObjectInputStream ois = new ObjectInputStream(bais);
                Object obj = ois.readObject();
                
                if (obj instanceof FinDB)
                {
                    System.out.println("Hurra Database!");
                }
                if (obj instanceof Column)
                {
                    if (column==(Column)obj)
                        System.out.println("Hurra Column!");
                }
            }
            catch (ClassNotFoundException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            catch (IOException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } 
            * --/
            return column;
        }

        @Override
        public void restoreState(FacesContext context, Object state)
        {
            if (state instanceof Column)
                column = (Column)state;
        }

        @Override
        public boolean isTransient()
        {
            return false;
        }

        @Override
        public void setTransient(boolean newTransientValue)
        {
        }
    }
    */
    
    private static final Logger log = LoggerFactory.getLogger(InputControl.class);
    
    // Special Input Column Attributes
    public static final String NUMBER_TYPE_ATTRIBUTE      = "numberType";   // "Integer", "Currency", "Percent"  
    public static final String NUMBER_GROUPSEP_ATTRIBUTE  = "numberGroupSeparator"; // boolean
    public static final String NUMBER_FRACTION_DIGITS     = "numberFractionDigits"; // integer
    public static final String MINVALUE_ATTRIBUTE         = "minValue";
    public static final String MAXVALUE_ATTRIBUTE         = "maxValue";
    public static final String CURRENCY_CODE_ATTRIBUTE    = "currencyCode";   // "ISO 4217 code of the currency"  

    // format attributes
    public static final String FORMAT_NULL = "null:";
    public static final String FORMAT_NULL_ATTRIBUTE = "format:null";
    
    public InputControl()
    {
        log.info("InputControl of class {} created.", getClass().getName());
    }
    
    /**
     * This interface allows access to a value and its metainformation
     * used with the renderData function
     */ 
    public interface ValueInfo
    {
        Column getColumn();
        Options getOptions();
        Object getValue(boolean evalExpression);
        String getFormat();    // Custom Formatting options specific to each InputControl-type
        Locale getLocale();
        String getText(String key);
        TextResolver getTextResolver();
        /*
        Object getNullValue();
        String getOnclick();
        String getOndblclick();
        String getCssClass();
        String getCssStyle();
        String getId();
        */
    }

    /**
     * This interface extends the value information by information about the input control
     * used with the renderInput function
     */ 
    public interface InputInfo extends ValueInfo
    {
        // perform action 
        void setValue(Object value);
        void validate(Object value);
        boolean isRequired();
        boolean isDisabled(); // readOnly
        // input
        String getInputId();
        String getTabindex();
        String getStyleClass(String addlStyle);
        // String getAccesskey();
        /*
        String getName();
        boolean isValid(); // Indicates whether the value supplied is valid
        String getOnchange();
        String getOnfocus();
        String getOnblur();
        */
        Object getAttribute(String name);
    }
    
    private String name;
    
    protected InputControl(String name)
    {
        this.name = name;
    }
    
    public final String getName()
    {
        return name;
    }

    /* Value */
    public void renderValue(ValueInfo vi, ResponseWriter writer)
        throws IOException
    {
        String text = formatValue(vi);
        writer.append((StringUtils.isEmpty(text) ? "&nbsp;" : text));
    }

    /* Input */
    public void renderInput(UIComponent comp, InputInfo ii, FacesContext context, boolean encode)
        throws IOException
    {
        // createInputComponents 
        createInputComponents(comp, ii, context, comp.getChildren());
        // Encode all
        if (!encode)
            return;
        for (UIComponent child : comp.getChildren())
        {
            child.encodeAll(context);
        }
    }
    
    public void postUpdateModel(UIComponent comp, InputInfo ii, FacesContext fc)
    {
        UIInput input = getInputComponent(comp);
        if (input==null)
            return; /* May want to override this */
        // Clear submitted value
        clearSubmittedValue(input);
    }
    
    public Object getInputValue(UIComponent comp, InputInfo ii, boolean submitted)
    {
        UIInput input = getInputComponent(comp);
        if (input==null)
            throw new ObjectNotValidException(this);
        
        // Get value from Input
        Object value = (submitted) ? input.getSubmittedValue() : input.getValue();
        if (submitted)
        {
            if (value!=null) // && (!ObjectUtils.compareEqual(value, input.getLocalValue())
            {
                // Disabled
                if (ii.isDisabled())
                {
                    input.setSubmittedValue(null);
                    throw new FieldIsReadOnlyException(ii.getColumn());
                }    
                // Save submitted value
                FacesContext fc = FacesContext.getCurrentInstance();
                Map<String, Object> reqMap = fc.getExternalContext().getRequestMap();
                // Save submitted value
                String clientId = input.getClientId();
                if (reqMap.containsKey(clientId))
                {
                    log.warn("OOps, what is going on here?");
                }            
                reqMap.put(clientId, value);
            }
            // Convert
            if ((value instanceof String) && ((String)value).length()>0)
            {
                return parseInputValue((String)value, ii);
            }
        }
        return value;
    }

    protected void setInputValue(UIInput input, InputInfo ii)
    {
        /*
        if (input.isLocalValueSet())
            return;
        else
        {   // check Request Map
            FacesContext fc = FacesContext.getCurrentInstance();
            if (FacesUtils.isClearSubmittedValues(fc))
            {   // Clear submitted value
                if (input.getSubmittedValue()!=null)
                    input.setSubmittedValue(null);
            }
            else
            {   // Restore submitted value
                Map<String, Object> reqMap = fc.getExternalContext().getRequestMap();
                String clientId = input.getClientId();
                if (reqMap.containsKey(clientId))
                {   // Set the local value from the request map
                    Object value = reqMap.get(clientId);
                    input.setSubmittedValue(value);
                    return;
                }
            }
        }
        */

        // Restore submitted value
        FacesContext fc = FacesContext.getCurrentInstance();
        Map<String, Object> reqMap = fc.getExternalContext().getRequestMap();
        String clientId = input.getClientId();
        if (reqMap.containsKey(clientId))
        {   // Set the local value from the request map
            Object value = reqMap.get(clientId);
            input.setSubmittedValue(value);
            return;
        }
        else if (input.getSubmittedValue()!=null) //  && FacesUtils.isClearSubmittedValues(fc)
        {   // Clear submitted value   
            if (log.isDebugEnabled())
                log.debug("clearing submitted value for {}. value is {}.", ii.getColumn().getName(), input.getSubmittedValue());
            input.setSubmittedValue(null);
        }
        
        /* -------------------------------------- */

        // Assign value
        Object value = ii.getValue(false);
        if (value instanceof ValueExpression)
        {   input.setValue(null);
            input.setLocalValueSet(false);
            input.setValueExpression("value", (ValueExpression)value);
            
            // Object check = ((ValueExpression)value).getValue(FacesContext.getCurrentInstance().getELContext());
            // log.info("Expression value is {}.", check);
        }    
        else
        {   // Set the value
            value = formatInputValue(value, ii);
            input.setValue(value);
        }    
    }    

    protected void clearSubmittedValue(UIInput input)
    {
        input.setSubmittedValue(null);
        // check Request Map
        FacesContext fc = FacesContext.getCurrentInstance();
        Map<String, Object> reqMap = fc.getExternalContext().getRequestMap();
        String clientId = input.getClientId();
        if (reqMap.containsKey(clientId))
            reqMap.remove(clientId);
    }    

    /**
     * Override this to format a value for output
     * @param value
     * @param ii
     * @return
     */
    protected Object formatInputValue(Object value, InputInfo ii)
    {
        return value;
    }
    
    protected Object parseInputValue(String value, InputInfo ii)
    {
        return value;
    }
    
    /* validate 
    public boolean validateValue(UIComponent comp, InputInfo ii, FacesContext context)
    {
        UIInput input = getInputComponent(comp);
        if (input==null)
            throw new ObjectNotValidException(this);
        
        input.validate(context);
        if (input.isValid()==false)
            return false;
        /-*
        Object value = getInputValue(comp, ii, context, false);
        try {
            ii.getColumn().validate(value);
            Object xxx = input.getLocalValue();
            // Wert geändert?
            Object previous = ii.getValue();
            if (ObjectUtils.compareEqual(value, previous)==false)
            {
                comp.queueEvent(new ValueChangeEvent(comp, previous, value));
                // Wert setzen
                ii.setValue(value, true);
            }
            return true;
            
        } catch(Exception e) {
            // Add Error Messgae
            String text = e.getLocalizedMessage();
            FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, text, text);
            context.addMessage(comp.getClientId(), msg);
            // Invalid
            ii.setValue(value, false);
            return false;
        }
        *-/
        return true;
    }
    */
    
    /* Input helpers */
    protected abstract void createInputComponents(UIComponent parent, InputInfo ii, FacesContext context, List<UIComponent> compList);
    
    protected UIInput getInputComponent(UIComponent parent)
    {
        // default implementation
        int count = parent.getChildCount(); 
        if (count!=1)
            return null;
        // the input
        UIComponent inp = parent.getChildren().get(0);
        if (!(inp instanceof UIInput))
            throw new UnexpectedReturnValueException(inp, "comp.getChildren().get(0)"); 
        // OK
        return (UIInput)inp;
    }

    protected void copyAttributes(UIComponent parent, InputInfo ii, UIInput input, String additonalStyle)
    {
        String inputId = ii.getInputId();
        if (StringUtils.isNotEmpty(inputId))
            input.getAttributes().put("id", inputId);
        
        String styleClass = ii.getStyleClass(additonalStyle);
        input.getAttributes().put("styleClass", styleClass);
        
        copyAttribute(ii, input, "style");
        copyAttribute(ii, input, "tabindex");
        copyAttribute(ii, input, "onchange");

        // immediate
        Object immediate = ii.getAttribute("immediate");
        if (immediate!=null && ObjectUtils.getBoolean(immediate))
        {
            log.warn("Immediate attribute is not yet supported for {}!", ii.getColumn().getName());
            // input.setImmediate(true);
        }    

        // validator
        // input.addValidator(new ColumnValueValidator(ii.getColumn()));
    }

    protected final void copyAttributes(UIComponent parent, InputInfo ii, UIInput input)
    {
        copyAttributes(parent, ii, input, null);
    }

    protected void copyAttribute(InputInfo ii, UIInput input, String name)
    {
        if (ii==null)
            throw new InvalidArgumentException("InputInfo", ii);
        // get Attribute
        Object value = ii.getAttribute(name);
        if (value!=null)
            input.getAttributes().put(name, value);
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
    protected String formatValue(Object value, ValueInfo vi)
    {
        // Lookup and Print value
        Options options = vi.getOptions();
        if (options != null && !options.isEmpty() && !hasFormatOption(vi, "nolookup"))
        { // Check for Options
            String text = options.get(value);
            if (text != null)
                return vi.getText(text);
            // Error
            log.error("The element '" + String.valueOf(value) + "' is not part of the supplied option list.");
        }
        // value
        if (value==null)
            value = getFormatOption(vi, FORMAT_NULL, FORMAT_NULL_ATTRIBUTE);
        // Convert to String
        String s = StringUtils.valueOf(value);
        if (hasFormatOption(vi, "noencode"))
            return s;
        // Encode Html
        return escapeHTML(s);
    }

    /**
     * Returns the value formated as a string
     * This is a shortcut for formatString(vi.getValue(), vi)
     * Derived classes may override formatString
     */
    protected final String formatValue(ValueInfo vi)
    {
        // boolean hasError = ((vi instanceof InputInfo) && !((InputInfo)vi).isValid()); 
        return formatValue(vi.getValue(true), vi);
    }
    
    /**
     * escapes a String for html
     * @param text
     * @return the escaped html String
     */
    protected String escapeHTML(String text)
    {
        // TODO
        return text;
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
    
    private String getFormatOption(ValueInfo vi, String option)
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

    protected Object getFormatOption(ValueInfo vi, String option, String columnAttributeName)
    {
        String format = getFormatOption(vi, option);
        return (format!=null) ? format : vi.getColumn().getAttribute(columnAttributeName); 
    }

    protected String getFormatString(ValueInfo vi, String option, String columnAttributeName)
    {
        return StringUtils.toString(getFormatOption(vi, option, columnAttributeName));
    }

    protected int getFormatInteger(ValueInfo vi, String option, String columnAttributeName)
    {
        return ObjectUtils.getInteger(getFormatOption(vi, option, columnAttributeName));
    }
    
}
