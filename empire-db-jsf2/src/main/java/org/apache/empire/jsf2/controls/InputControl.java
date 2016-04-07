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
import javax.faces.component.UIComponentBase;
import javax.faces.component.UIData;
import javax.faces.component.UIInput;
import javax.faces.component.behavior.ClientBehaviorHolder;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.commons.Options;
import org.apache.empire.commons.StringUtils;
import org.apache.empire.data.Column;
import org.apache.empire.exceptions.InvalidArgumentException;
import org.apache.empire.exceptions.ItemNotFoundException;
import org.apache.empire.exceptions.UnexpectedReturnValueException;
import org.apache.empire.jsf2.app.TextResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class InputControl
{

    private static final Logger log                   = LoggerFactory.getLogger(InputControl.class);

    // format attributes
    public static final String  FORMAT_NULL           = "null:";
    public static final String  FORMAT_NULL_ATTRIBUTE = "format:null";
    
    // HTML-TAGS
    public static final String  HTML_TAG_DIV          = "div"; 
    public static final String  HTML_TAG_SPAN         = "span";
    public static final String  HTML_TAG_TABLE        = "table";
    public static final String  HTML_TAG_TR           = "tr";
    public static final String  HTML_TAG_TD           = "td";
    public static final String  HTML_TAG_INPUT        = "input";
    public static final String  HTML_TAG_LABEL        = "label";
    
    // HTML-ATTRIBUTES
    public static final String  HTML_ATTR_ID          = "id";
    public static final String  HTML_ATTR_CLASS       = "class";
    public static final String  HTML_ATTR_STYLE       = "style";
    public static final String  HTML_ATTR_TYPE        = "type";
    public static final String  HTML_ATTR_DISABLED    = "disabled";
    public static final String  HTML_ATTR_CHECKED     = "checked";
    
    // HTML
    public static String HTML_EXPR_NBSP = "&nbsp;";

    public InputControl()
    {
        InputControl.log.info("InputControl of class {} created.", getClass().getName());
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

        String getFormat(); // Custom Formatting options specific to each InputControl-type

        Locale getLocale();

        String getText(String key);

        TextResolver getTextResolver();

        String getStyleClass(String addlStyle);

        /*
        Object getNullValue();
        String getOnclick();
        String getOndblclick();
        String getCssClass();
        String getCssStyle();
        String getId();
        */
        
        boolean isInsideUIData();
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

        boolean isDisabled(); // disabled or readOnly

        boolean isFieldReadOnly(); // not disabled only readOnly (for input[type=text] only!)
        // input

        String getInputId();

        boolean hasError();

        /*
        String getName();
        String getTabindex();
        String getAccesskey();
        boolean isValid(); // Indicates whether the value supplied is valid
        String getOnchange();
        String getOnfocus();
        String getOnblur();
        */
        Object getAttribute(String name); /* gets tag attribute only */

        Object getAttributeEx(String name); /* check Column attributes too, and resolves references to other columns. */
    }

    private String name;

    protected InputControl(String name)
    {
        this.name = name;
    }

    public final String getName()
    {
        return this.name;
    }

    public String getLabelForId(InputInfo ii)
    {
        return ii.getInputId();
    }
    
    /**
     * Flag indicating whether child components are being created
     */
    private boolean creatingComponents = false;
    public boolean isCreatingComponents()
    {
        return this.creatingComponents;
    }
    
    /* createInput */ 
    public void createInput(UIComponent comp, InputInfo ii, FacesContext context)
    {   // createInputComponents
        List<UIComponent> children = comp.getChildren();
        try {
            this.creatingComponents = true;
            createInputComponents(comp, ii, context, children);
            // check
            boolean resetChildId = ii.isInsideUIData();
            if (resetChildId && log.isDebugEnabled())
            {   // Debug-Info only   
                UIComponent c1 = comp.getChildren().get(0);
                String clientId = c1.getClientId();
                log.debug("Performing ChildId-reset for {}", clientId);
            }
            // add attached objects
            UIComponent parent = comp;
            while (!(parent instanceof UIInput))
                parent = parent.getParent();
            for (UIComponent child : children)
            {   // reset child-id
                if (resetChildId && child.getId()!=null)
                    child.setId(child.getId());
                // check type
                if (!(child instanceof ClientBehaviorHolder))
                    continue;
                // add attached objects
                addAttachedObjects(parent, context, ii, ((UIComponentBase)child));
            }
        } finally {
            this.creatingComponents = false;
        }
    }
    
    /* Value */
    public void renderValue(ValueInfo vi, ResponseWriter writer)
        throws IOException
    {
        String text = formatValue(vi);
        writer.append((StringUtils.isEmpty(text) ? HTML_EXPR_NBSP : text));
    }

    /* renderInput */ 
    public void renderInput(UIComponent comp, InputInfo ii, FacesContext context)
        throws IOException
    {
        // Encode all
        for (UIComponent child : comp.getChildren())
        {   // render
            if (child.isRendered())
                child.encodeAll(context);
        }
    }
    
    public void updateInputState(UIComponent parent, InputInfo ii, FacesContext context, boolean setValue)
    {
        List<UIComponent> cl = parent.getChildren(); 
        if (cl.isEmpty())
            return;
        updateInputState(cl, ii, context, setValue);
        // update attached objects
        List<UIComponent> children = parent.getChildren();
        while (!(parent instanceof UIInput))
            parent = parent.getParent();
        for (UIComponent child : children)
        {   // check type
            if (!(child instanceof ClientBehaviorHolder))
                continue;
            // update attached objects
            updateAttachedObjects(parent, context, ii, ((UIComponentBase)child));
        }
    }
    
    public void postUpdateModel(UIComponent comp, InputInfo ii, FacesContext fc)
    {
        UIInput input = getInputComponent(comp);
        if (input == null)
            return; /* May want to override this */
        // Clear submitted value
        clearSubmittedValue(input);
    }
    
    public Object getInputValue(UIComponent comp, InputInfo ii, boolean submitted)
    {
        UIInput input = getInputComponent(comp);
        if (input == null)
        {   // throw new ObjectNotValidException(this);
            return null; // ignore
        }
        
        // Get value from Input
        Object value;
        if (submitted)
        {   // check disabled
            if (ii.isDisabled())
            { // Ignore submitted value
                InputControl.log.debug("Ignoring submitted value for disabled field {}.", ii.getColumn().getName());
                input.setSubmittedValue(null);
                // throw new FieldIsReadOnlyException(ii.getColumn());
                return null;
            }
            // get submitted value
            value = input.getSubmittedValue();
            if (value == null && input.isLocalValueSet()) // required for MyFaces!
            {   // take local value
                if (log.isDebugEnabled())
                    log.debug("No submitted value but local value available for InputComponent {}. Local value is '{}'", input.getClientId(), input.getLocalValue());
                value = input.getLocalValue();
                if (value == null)
                {   // Empty-String
                    value = "";
                }
            }
            // debug
            if (log.isDebugEnabled())
                log.debug("Submitted value for {} is {}", comp.getClientId(), value);
        }
        else
        {   // the current value
            value = input.getValue();
        }
        return value;
    }

    public Object getConvertedValue(UIComponent comp, InputInfo ii, Object submittedValue)
    {
        // Value supplied?
        if (submittedValue != null)
        {   // Save submitted value in request-map
            FacesContext fc = FacesContext.getCurrentInstance();
            Map<String, Object> reqMap = fc.getExternalContext().getRequestMap();
            // Save submitted value
            UIInput input = getInputComponent(comp);
            String clientId = input.getClientId();
            if (reqMap.containsKey(clientId))
            {   Object oldValue =  reqMap.get(clientId);
                if (ObjectUtils.compareEqual(oldValue, submittedValue)==false)
                    InputControl.log.debug("Replacing submitted value from '{}' to '{}' for " + clientId, oldValue, submittedValue);
            }
            reqMap.put(clientId, submittedValue);
        }
        // Convert
        if ((submittedValue instanceof String) && ((String) submittedValue).length() > 0)
        {   // debug
            if (log.isDebugEnabled())
                log.debug("Converting value for colum {}. Value is {}", ii.getColumn().getName(), submittedValue);
            // parse
            return parseInputValue((String) submittedValue, ii);
        }            
        return submittedValue;
    }
    
    protected void addAttachedObjects(UIComponent parent, FacesContext context, InputInfo ii, UIComponentBase inputComponent)
    {
        InputAttachedObjectsHandler aoh = InputControlManager.getAttachedObjectsHandler();
        if (aoh!=null)
            aoh.addAttachedObjects(parent, context, ii.getColumn(), inputComponent);
    }
    
    protected void updateAttachedObjects(UIComponent parent, FacesContext context, InputInfo ii, UIComponentBase inputComponent)
    {
        InputAttachedObjectsHandler aoh = InputControlManager.getAttachedObjectsHandler();
        if (aoh!=null)
            aoh.updateAttachedObjects(parent, context, ii.getColumn(), inputComponent);
    }
    
    protected UIInput getFirstInput(List<UIComponent> compList)
    {
        for (int i=0; i<compList.size(); i++)
        {
            UIComponent child = compList.get(i);
            if (child instanceof UIInput)
                return ((UIInput)child);
        }
        throw new ItemNotFoundException("UIInput");
    }
    
    protected void setInputValue(UIInput input, InputInfo ii)
    {
        // Restore submitted value
        FacesContext fc = FacesContext.getCurrentInstance();
        Map<String, Object> reqMap = fc.getExternalContext().getRequestMap();
        String clientId = input.getClientId();
        if (reqMap.containsKey(clientId))
        { // Set the local value from the request map
            Object value = reqMap.get(clientId);
            if (input.isLocalValueSet() == false)
                input.setSubmittedValue(value);
            return;
        }
        else if (input.getSubmittedValue() != null) //  && FacesUtils.isClearSubmittedValues(fc)
        { // Clear submitted value   
            if (InputControl.log.isDebugEnabled())
                InputControl.log.debug("clearing submitted value for {}. value is {}.", ii.getColumn().getName(), input.getSubmittedValue());
            input.setSubmittedValue(null);
        }

        /* -------------------------------------- */

        // Assign value
        Object value = ii.getValue(false);
        if (value instanceof ValueExpression)
        {
            input.setValue(null);
            input.setLocalValueSet(false);
            input.setValueExpression("value", (ValueExpression) value);

            // Object check = ((ValueExpression)value).getValue(FacesContext.getCurrentInstance().getELContext());
            // log.info("Expression value is {}.", check);
        }
        else
        { // Set the value
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
     * 
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

    protected abstract void updateInputState(List<UIComponent> compList, InputInfo ii, FacesContext context, boolean setValue);
    
    protected UIInput getInputComponent(UIComponent parent)
    {
        // default implementation
        int count = parent.getChildCount();
        if (count < 1)
            return null;
        // find the UIInput component (only one allowed here)
        UIInput inp = null;
        for (int i = 0; i < count; i++)
        { // check UIInput 
            UIComponent comp = parent.getChildren().get(i);
            if (comp instanceof UIInput)
            {   if (inp != null)
                    throw new UnexpectedReturnValueException(comp, "comp.getChildren().get(" + String.valueOf(i) + ")");
                inp = (UIInput) comp;
            }
        }
        // No UIInput found
        if (inp == null)
        {   // Check whether inside a DataTable (javax.faces.component.UIData)
            for (UIComponent p = parent.getParent(); p!=null; p=p.getParent())
            {   // Check whether inside UIData
                if (p instanceof UIData) {
                    log.info("Ignore value component for id '{}' inside a DataTable (javax.faces.component.UIData)", parent.getClientId());
                    return null;
                }
            }
            // Should not happen!
            throw new UnexpectedReturnValueException(null, "comp.getChildren().get()");
        }
        // found one
        return inp;
    }

    protected void copyAttributes(UIComponent parent, InputInfo ii, UIInput input, String additonalStyle)
    {
        String inputId = ii.getInputId();
        if (StringUtils.isNotEmpty(inputId))
        {
            input.getAttributes().put("id", inputId);
        }

        String styleClass = ii.getStyleClass(additonalStyle);
        input.getAttributes().put("styleClass", styleClass);

        copyAttribute(ii, input, "style");
        copyAttribute(ii, input, "tabindex");
        copyAttribute(ii, input, "onchange");
        copyAttribute(ii, input, "onfocus");
        copyAttribute(ii, input, "onblur");
        copyAttribute(ii, input, "onkeydown");
        copyAttribute(ii, input, "onkeyup");
        copyAttribute(ii, input, "onclick");

        // immediate
        Object immediate = ii.getAttribute("immediate");
        if (immediate != null && ObjectUtils.getBoolean(immediate))
        {
            InputControl.log.warn("Immediate attribute is not yet supported for {}!", ii.getColumn().getName());
            // input.setImmediate(true);
        }

        // validator
        // input.addValidator(new ColumnValueValidator(ii.getColumn()));
    }

    protected final void copyAttributes(UIComponent parent, InputInfo ii, UIInput input)
    {
        copyAttributes(parent, ii, input, (ii.isRequired() ? "eInpReq" : null));
    }

    protected void copyAttribute(InputInfo ii, UIInput input, String name)
    {
        if (ii == null)
            throw new InvalidArgumentException("InputInfo", ii);
        // get Attribute
        Object value = ii.getAttribute(name);
        if (value == null)
            value = ii.getColumn().getAttribute(name);
        if (value != null)
            input.getAttributes().put(name, String.valueOf(value));
    }

    public void addRemoveDisabledStyle(UIInput input, boolean disabled)
    {
        addRemoveStyle(input, " eInpDis", disabled);
    }

    public void addRemoveInvalidStyle(UIInput input, boolean invalid)
    {
        addRemoveStyle(input, " eInvalid", invalid);
    }

    public void addRemoveStyle(UIInput input, String styleName, boolean setStyle)
    {
        String styleClass = StringUtils.toString(input.getAttributes().get("styleClass"), "");
        boolean hasStyle = (styleClass.indexOf(styleName) >= 0);
        if (setStyle == hasStyle)
            return; // Nothing to do
        // Special IceFaces patch
        if (styleClass.endsWith("-dis"))
            styleClass = styleClass.substring(0, styleClass.length() - 4);
        // add or remove disabled style
        if (setStyle)
            styleClass += styleName;
        else
            styleClass = styleClass.replace(styleName, "");
        // add Style
        input.getAttributes().put("styleClass", styleClass);
    }

    /**
     * Returns the value formated as a string
     * this is a simple default implementation that does no type-secific formatting
     * Derived classes may override formatString an provide further formmatting
     * see TextInputControl for details
     * 
     * @param value
     *            the value to be formatted
     * @param vi
     *            Meta-information about the value
     * @return the formatted value
     */
    protected String formatValue(Object value, ValueInfo vi)
    {
        // For Enums use toString() to retrieve Value
        if (value != null && value.getClass().isEnum() && !hasFormatOption(vi, "nolookup"))
        { // Handle enum
            String text = ((Enum<?>) value).toString();
            if (text != null)
                return vi.getText(text);
            // Error
            InputControl.log.error("The enum '" + ((Enum<?>) value).name() + "' has no text!");
        }
        // Lookup and Print value
        Options options = vi.getOptions();
        if (options != null && !options.isEmpty() && !hasFormatOption(vi, "nolookup"))
        { // Check for Options
            String text = options.get(value);
            if (text != null)
                return vi.getText(text);
            // Error
            InputControl.log.error("The element '" + String.valueOf(value) + "' is not part of the supplied option list.");
        }
        // value
        if (value == null)
            value = getFormatOption(vi, InputControl.FORMAT_NULL, InputControl.FORMAT_NULL_ATTRIBUTE);
        // Convert to String
        String s = StringUtils.toString(value, "");
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
     * 
     * @param text
     * @return the escaped html String
     */
    protected String escapeHTML(String text)
    {
        if (text==null || text.length()==0)
            return text;
        // &amp;
        if (text.indexOf('&')>=0)
            text = StringUtils.replaceAll(text, "&", "&amp;");
        // &lt;
        if (text.indexOf('<')>=0)
            text = StringUtils.replaceAll(text, "<", "&lt;");
        // &gt;
        if (text.indexOf('>')>=0)
            text = StringUtils.replaceAll(text, ">", "&gt;");
        // done
        return text;
    }

    /**
     * checks if a particular formating option has been specified.
     * 
     * @param vi
     *            the value info
     * @param option
     *            the formating option to check
     * @return true if the requested formating option has been specified or false otherwise
     */
    protected boolean hasFormatOption(ValueInfo vi, String option)
    {
        String format = vi.getFormat();
        return (format != null ? format.indexOf(option) >= 0 : false);
    }

    protected String getFormatOption(ValueInfo vi, String option)
    {
        // Is unit supplied with format
        String format = vi.getFormat();
        if (format == null)
            return null;
        // Check for option
        int beg = format.indexOf(option);
        if (beg < 0)
            return null;
        // Find
        beg = beg + option.length();
        int end = format.indexOf(';', beg + 1);
        if (end < beg)
            return format.substring(beg);
        // The cbValue
        return format.substring(beg, end);
    }

    protected Object getFormatOption(ValueInfo vi, String option, String columnAttributeName)
    {
        String format = getFormatOption(vi, option);
        return (format != null) ? format : vi.getColumn().getAttribute(columnAttributeName);
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
