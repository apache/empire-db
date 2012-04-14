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

import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.apache.empire.commons.Options;
import org.apache.empire.commons.StringUtils;
import org.apache.empire.data.Column;
import org.apache.empire.exceptions.ObjectNotValidException;
import org.apache.empire.exceptions.UnexpectedReturnValueException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class FieldRenderer
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
    
    private static final Logger log = LoggerFactory.getLogger(FieldRenderer.class);

    // Special Input Column Attributes
    public static final String NUMBER_TYPE_ATTRIBUTE      = "numberType";   // "Integer", "Currency", "Percent"  
    public static final String NUMBER_GROUPSEP_ATTRIBUTE  = "numberGroupSeparator"; // boolean
    public static final String NUMBER_FRACTION_DIGITS     = "numberFractionDigits"; // integer
    public static final String MINVALUE_ATTRIBUTE         = "minValue";
    public static final String MAXVALUE_ATTRIBUTE         = "maxValue";
    public static final String CURRENCY_CODE_ATTRIBUTE    = "currencyCode";   // "ISO 4217 code of the currency"  
    
    public FieldRenderer()
    {
        log.info("FieldRenderer created");
    }
    
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
        String getFormat();    // Custom Formatting options specific to each InputControl-type
        Locale getLocale();
        String getText(String key);
        /*
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
        int getHSize();
        int getVSize();
        boolean isRequired();
        boolean isDisabled(); // readOnly
        String getInputId();
        String getTabindex();
        String getStyleClass(String addlStyle);
        // perform action 
        void setValue(Object value);
        void validate(Object value);
        
        // String getAccesskey();
        /*
        String getName();
        boolean isValid(); // Indicates whether the value supplied is valid
        String getOnchange();
        String getOnfocus();
        String getOnblur();
        */
    }
    
    private String name;
    
    protected FieldRenderer(String name)
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
        int count = comp.getChildCount(); 
        if (count<1)
            createInputComponents(comp, ii, context, comp.getChildren());
        // Encode all
        if (!encode)
            return;
        for (UIComponent child : comp.getChildren())
        {
            child.encodeAll(context);
        }
    }
    
    public Object getInputValue(UIComponent comp, InputInfo ii, boolean submitted)
    {
        UIInput input = getInputComponent(comp);
        if (input==null)
            throw new ObjectNotValidException(this);
        
        // Get value from Input
        return (submitted) ? input.getSubmittedValue() : input.getValue();        
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
        
        copyAttribute(parent, input, "style");
        copyAttribute(parent, input, "tabindex");
        copyAttribute(parent, input, "onchange");
        
        // validator
        // input.addValidator(new ColumnValueValidator(ii.getColumn()));
        
        // IceFaces special
        /*
        if (input instanceof IceExtended)
        {   // partialSubmit
            Object v = parent.getAttributes().get("partialSubmit");
            if (v!=null)
               ((IceExtended)input).setPartialSubmit(ObjectUtils.getBoolean(v));
        }
        */
    }

    protected final void copyAttributes(UIComponent parent, InputInfo ii, UIInput input)
    {
        copyAttributes(parent, ii, input, null);
    }

    protected void copyAttribute(UIComponent parent, UIInput input, String name)
    {
        Object value = parent.getAttributes().get(name);
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
    protected String formatValue(Object value, ValueInfo vi, boolean hasError)
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
            value = vi.getNullValue();
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
        boolean hasError = false; // ((vi instanceof InputInfo) && !((InputInfo)vi).isValid()); 
        return formatValue(vi.getValue(), vi, hasError);
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
