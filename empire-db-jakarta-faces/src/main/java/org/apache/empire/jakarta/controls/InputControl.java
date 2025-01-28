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
package org.apache.empire.jakarta.controls;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.commons.OptionEntry;
import org.apache.empire.commons.Options;
import org.apache.empire.commons.StringUtils;
import org.apache.empire.data.Column;
import org.apache.empire.data.DataType;
import org.apache.empire.exceptions.InvalidArgumentException;
import org.apache.empire.exceptions.ItemNotFoundException;
import org.apache.empire.exceptions.UnexpectedReturnValueException;
import org.apache.empire.jakarta.app.TextResolver;
import org.apache.empire.jakarta.utils.HtmlUtils;
import org.apache.empire.jakarta.utils.TagStyleClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.el.ValueExpression;
import jakarta.faces.component.UIComponent;
import jakarta.faces.component.UIComponentBase;
import jakarta.faces.component.UIData;
import jakarta.faces.component.UIInput;
import jakarta.faces.component.behavior.ClientBehaviorHolder;
import jakarta.faces.context.FacesContext;
import jakarta.faces.context.ResponseWriter;
import jakarta.faces.event.PhaseId;

public abstract class InputControl
{

    private static final Logger log                    = LoggerFactory.getLogger(InputControl.class);

    // StyleClass 
    public static final String  CSS_STYLE_CLASS        = "styleClass";

    // format attributes
    public static final String  FORMAT_NULL            = "null:";
    public static final String  FORMAT_NULL_ATTRIBUTE  = "format:null";
    public static final String  FORMAT_NO_VALUE_STYLES = "noValueStyles";
    /* obsolte from 2024-06-03
    public static final String  FORMAT_VALUE_STYLES           = "valueStyles";
    public static final String  FORMAT_VALUE_STYLES_ATTRIBUTE = "format:valueStyles";
    */

    // HTML-TAGS
    public static final String  HTML_TAG_DIV           = "div";
    public static final String  HTML_TAG_SPAN          = "span";
    public static final String  HTML_TAG_TABLE         = "table";
    public static final String  HTML_TAG_TR            = "tr";
    public static final String  HTML_TAG_TD            = "td";
    public static final String  HTML_TAG_INPUT         = "input";
    public static final String  HTML_TAG_LABEL         = "label";

    // HTML-ATTRIBUTES
    public static final String  HTML_ATTR_ID           = "id";
    public static final String  HTML_ATTR_CLASS        = "class";
    public static final String  HTML_ATTR_STYLE        = "style";
    public static final String  HTML_ATTR_TYPE         = "type";
    public static final String  HTML_ATTR_DISABLED     = "disabled";
    public static final String  HTML_ATTR_CHECKED      = "checked";

    // HTML
    public static String        HTML_EXPR_NBSP         = "&nbsp;";

    /*
    public InputControl()
    {
        InputControl.log.info("InputControl of class {} created.", getClass().getName());
    }
    */

    /**
     * DisabledType
     * @author doebele
     */
    public enum DisabledType 
    {
        NO,
        READONLY,
        DISABLED;
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

        boolean isModified();
        
        boolean isDisabled(); // disabled or readOnly

        DisabledType getDisabled();
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

    private final String name;
    private final String cssStyleClass;

    protected InputControl(String name)
    {
        this.name = name;
        this.cssStyleClass = initCssStyleClass();
    }

    public final String getName()
    {
        return this.name;
    }
    
    public final String getCssStyleClass() 
    {
        return this.cssStyleClass;
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
    
    /**
     * Returns the formatted value
     * Do not override this function, but override formatValue(Object, ValueInfo) instead.   
     * @param value the value to format
     * @param vi the valueInfo
     * @param escapeHtml when true the value will be escaped for Html
     * @return the formatted value
     */
    public String formatValue(Object value, ValueInfo vi, boolean escapeHtml)
    {
        String s = formatValue(value, vi);
        if (escapeHtml && s!=null && s.length()!=0)
            s = HtmlUtils.getInstance().escapeText(s);
        return s;
    }
    
    /**
     * Renders the control value with a surrounding HTML tag, if a tagName is supplied
     * @param comp the JSF component
     * @param tagName the tag name of the HTML wrapper tag (optional)
     * @param styleClass the style class of the HTML wrapper tag (optional)
     * @param tooltip the title of the HTML wrapper tag (optional)
     * @param vi the value info
     * @param context the FacesContext
     * @throws IOException from ResponseWriter
     */
    public void renderValue(UIComponent comp, String tagName, String styleClass, String tooltip, ValueInfo vi, FacesContext context)
        throws IOException
    {
        // writer
        ResponseWriter writer = context.getResponseWriter();
        // has tag?
        Object value = vi.getValue(true);
        if (tagName!=null)
        {   // write start tag
            writer.startElement(tagName, comp);
            if (!hasFormatOption(vi, FORMAT_NO_VALUE_STYLES))
                styleClass = addDataValueStyle(vi, value, styleClass);
            if (StringUtils.isNotEmpty(styleClass))
                writer.writeAttribute("class", styleClass, null);
            if (StringUtils.isNotEmpty(tooltip))
                writer.writeAttribute("title", tooltip, null);
            // style
            Object style = comp.getAttributes().get("style");
            if (style!=null)
                writer.writeAttribute("style", style, null);
        }
        // render Value
        renderValue(value, vi, writer);
        // has tag?
        if (tagName!=null)
        {   // write end tag
            writer.endElement(tagName);
        }
    }
    
    /**
     * Renders the control value without a surrounding tag (Text only)
     * @param vi the value info
     * @param writer the output writer
     * @throws IOException from ResponseWriter
     */
    public void renderValue(Object value, ValueInfo vi, ResponseWriter writer)
        throws IOException
    {
        // check if html needs to be escaped
        boolean escapeHtml = !hasFormatOption(vi, "noescape"); 
        String text = formatValue(value, vi, escapeHtml);
        writer.append((StringUtils.isEmpty(text) ? HTML_EXPR_NBSP : text));
    }

    /**
     * Renders the input element(s) for editing the underlying record value 
     * @param comp the JSF component
     * @param ii the input info
     * @param context the FacesContext
     * @throws IOException from ResponseWriter
     */
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
    
    public void updateInputState(UIComponent parent, InputInfo ii, FacesContext context, PhaseId phaseId)
    {
        List<UIComponent> children = parent.getChildren(); 
        if (children.isEmpty())
            return;
        // update state
        updateInputState(children, ii, context, phaseId);
        // update attached objects
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
        // Clear local values
        clearLocalValues(fc, input);
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
    
    protected String initCssStyleClass() 
    {
        return StringUtils.concat(TagStyleClass.INPUT_TYPE_PREFIX.get(), name.substring(0,1).toUpperCase(), name.substring(1));
    }
    
    /**
     * adds style attributes related to the current value
     * @param vi the value info
     * @param value the current value
     * @param styleClass the style class
     * @return the data value string
     */
    protected String addDataValueStyle(ValueInfo vi, Object value, String styleClass)
    {
        DataType dataType = vi.getColumn().getDataType();
        if (ObjectUtils.isEmpty(value))
        {   // Null
            styleClass += " eValNull";
        }
        else if (dataType.isNumeric() && value instanceof Number)
        {   // Check negative
            if (ObjectUtils.getLong(value)<0)
                styleClass += " eValNeg";
        }
        return styleClass;
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
    
    protected boolean isInputValueExpressionEnabled()
    {
        return InputControlManager.isInputValueExpressionEnabled();
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
            // change the style
            addRemoveValueNullStyle(input, ObjectUtils.isEmpty(value));
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
        boolean evalExpression = !isInputValueExpressionEnabled();
        Object value = ii.getValue(evalExpression);
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
            // change the style
            addRemoveValueNullStyle(input, ObjectUtils.isEmpty(value));
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
    
    protected void clearLocalValues(FacesContext context, UIComponent comp)
    {
        // UIInput
        if (comp instanceof UIInput)
        {   // Check LocalValue set 
            UIInput input = (UIInput)comp; 
            if (input.isValid() && input.isLocalValueSet())
            {   // Check ValueExpression
                // @see: UIInput:updateModel(FacesContext context)
                ValueExpression expression = input.getValueExpression("value");
                if (expression != null)
                {   // Reset localValue if ValueExpression is set
                    input.resetValue();
                }
            }
            // we're done here
            return;
        }
        // clearLocalValues of all facets and children of this UIComponent
        if (comp.getFacetCount() > 0)
        {
            for (UIComponent facet : comp.getFacets().values())
            {
                clearLocalValues(context, facet);
            }
        }
        // clear children
        if (comp.getChildCount() > 0)
        {
            for (UIComponent child : comp.getChildren())
            {
                clearLocalValues(context, child);
            }
        }
    }

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

    protected abstract void updateInputState(List<UIComponent> compList, InputInfo ii, FacesContext context, PhaseId phaseId);
    
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
        {   // Check whether inside a DataTable (jakarta.faces.component.UIData)
            for (UIComponent p = parent.getParent(); p!=null; p=p.getParent())
            {   // Check whether inside UIData
                if (p instanceof UIData) {
                    log.info("Ignore value component for id '{}' inside a DataTable (jakarta.faces.component.UIData)", parent.getClientId());
                    return null;
                }
            }
            // Should not happen!
            throw new UnexpectedReturnValueException(null, "comp.getChildren().get()");
        }
        // found one
        return inp;
    }

    protected String getInputStyleClass(InputInfo ii, String additonalStyle)
    {
        return ii.getStyleClass(additonalStyle);
    }

    protected void setInputStyleClass(UIInput input, String cssStyleClass)
    {
        input.getAttributes().put(InputControl.CSS_STYLE_CLASS, cssStyleClass);
    }

    protected void copyAttributes(UIComponent parent, InputInfo ii, UIInput input, String additonalStyle)
    {
        String inputId = ii.getInputId();
        if (StringUtils.isNotEmpty(inputId))
        {
            input.getAttributes().put("id", inputId);
        }

        // set style class
        String styleClass = getInputStyleClass(ii, additonalStyle);
        setInputStyleClass(input, styleClass);

        // copy standard attributes
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
        String addlStyles = null;
        if (ii.isRequired())
        {   // required
            addlStyles = TagStyleClass.INPUT_REQ.get();
        }
        if (ii.isModified()) 
        {   // modified
            addlStyles = TagStyleClass.INPUT_MOD.addTo(addlStyles);
        }
        copyAttributes(parent, ii, input, addlStyles);
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
    
    public void addRemoveValueNullStyle(UIInput input, boolean nullValue)
    {
        addRemoveStyle(input, TagStyleClass.VALUE_NULL, nullValue);
    }

    public void addRemoveDisabledStyle(UIInput input, boolean disabled)
    {
        addRemoveStyle(input, TagStyleClass.INPUT_DIS, disabled);
    }

    public void addRemoveInvalidStyle(UIInput input, boolean invalid)
    {
        addRemoveStyle(input, TagStyleClass.VALUE_INVALID, invalid);
    }
    
    public void addRemoveStyle(UIInput input, String styleName, boolean setStyle)
    {
        String styleClasses = StringUtils.toString(input.getAttributes().get(InputControl.CSS_STYLE_CLASS), "");
        boolean hasStyle = TagStyleClass.existsIn(styleClasses, styleName);
        if (setStyle == hasStyle)
            return; // Nothing to do
        // Special IceFaces patch
        if (styleClasses.endsWith("-dis"))
            styleClasses = styleClasses.substring(0, styleClasses.length() - 4);
        // add or remove disabled style
        if (setStyle)
            styleClasses = TagStyleClass.addTo(styleClasses, styleName);
        else
            styleClasses = TagStyleClass.removeFrom(styleClasses, styleName);
        // add Style
        setInputStyleClass(input, styleClasses);
    }

    public final void addRemoveStyle(UIInput input, TagStyleClass style, boolean setStyle)
    {
        this.addRemoveStyle(input, style.get(), setStyle);
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
        if ((value instanceof Enum<?>) && !hasFormatOption(vi, "nolookup"))
        { // Handle enum
            String text = ((Enum<?>) value).toString();
            if (text != null)
                return vi.getText(text);
            // Error
            InputControl.log.error("The enum '" + ((Enum<?>) value).name() + "' has no text!");
        }
        // Lookup and return text
        Options options = vi.getOptions();
        if (options != null && !hasFormatOption(vi, "nolookup"))
        {   // getOptionText
            String text = getOptionText(options, value, vi);
            if (text!=null)
                return text;
        }
        // value
        if (value == null)
            value = getFormatOption(vi, InputControl.FORMAT_NULL, InputControl.FORMAT_NULL_ATTRIBUTE);
        // Convert to String
        return StringUtils.toString(value, StringUtils.EMPTY);
    }

    /**
     * Returns the display text for an option
     * @param options
     * @param value
     * @param vi
     * @return the display text or null if the option value could not be resolved
     */
    protected String getOptionText(Options options, Object value, ValueInfo vi)
    {
        if (options == null)
            throw new InvalidArgumentException("options", options);
        // Check for Options
        OptionEntry entry = options.getEntry(value);
        if (entry!=null)
            return vi.getText(entry.getText());
        // Check empty
        if (ObjectUtils.isEmpty(value))
            return StringUtils.EMPTY;
        // Error: Value not found! 
        String column = (vi.getColumn()!=null ? vi.getColumn().getName() : "?");
        log.error("The element \"{}\" is not part of the supplied option list for column {}", value, column);
        return null; 
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

    protected boolean hasFormatOption(ValueInfo vi, String option, String columnAttributeName)
    {
        if (hasFormatOption(vi, option))
            return true;
        // column format provided?
        Column column = vi.getColumn();
        return (column!=null ? !ObjectUtils.isEmpty(column.getAttribute(columnAttributeName)) : false);
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
        // direct format provided?
        String format = getFormatOption(vi, option);
        if (format!=null)
            return format;
        // column format provided?
        Column column = vi.getColumn();
        return (column!=null ? column.getAttribute(columnAttributeName) : null);
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
