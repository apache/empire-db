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
package org.apache.empire.jsf2.utils;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.Locale;

import javax.el.ELContext;
import javax.el.ValueExpression;
import javax.el.ValueReference;
import javax.faces.FacesWrapper;
import javax.faces.application.FacesMessage;
import javax.faces.component.NamingContainer;
import javax.faces.component.UIComponent;
import javax.faces.component.UIData;
import javax.faces.component.UIInput;
import javax.faces.component.UIOutput;
import javax.faces.component.html.HtmlOutputLabel;
import javax.faces.component.html.HtmlOutputText;
import javax.faces.component.html.HtmlPanelGroup;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.beanutils.PropertyUtilsBean;
import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.commons.Options;
import org.apache.empire.commons.StringUtils;
import org.apache.empire.data.Column;
import org.apache.empire.data.ColumnExpr;
import org.apache.empire.data.DataType;
import org.apache.empire.data.Record;
import org.apache.empire.data.RecordData;
import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.DBRecord;
import org.apache.empire.db.DBRowSet;
import org.apache.empire.db.exceptions.FieldNotNullException;
import org.apache.empire.exceptions.BeanPropertyGetException;
import org.apache.empire.exceptions.BeanPropertySetException;
import org.apache.empire.exceptions.InvalidArgumentException;
import org.apache.empire.exceptions.NotSupportedException;
import org.apache.empire.exceptions.PropertyReadOnlyException;
import org.apache.empire.jsf2.app.FacesUtils;
import org.apache.empire.jsf2.app.TextResolver;
import org.apache.empire.jsf2.app.WebApplication;
import org.apache.empire.jsf2.components.ControlTag;
import org.apache.empire.jsf2.components.InputTag;
import org.apache.empire.jsf2.components.LabelTag;
import org.apache.empire.jsf2.components.LinkTag;
import org.apache.empire.jsf2.components.RecordTag;
import org.apache.empire.jsf2.controls.InputControl;
import org.apache.empire.jsf2.controls.InputControlManager;
import org.apache.empire.jsf2.controls.SelectInputControl;
import org.apache.empire.jsf2.controls.TextAreaInputControl;
import org.apache.empire.jsf2.controls.TextInputControl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TagEncodingHelper implements NamingContainer
{
    /**
     * ColumnExprWrapper
     * wraps a ColumnExpr object into a Column interface object
     * @author doebele
     */
    protected static class ColumnExprWrapper implements Column
    {
        private final ColumnExpr expr;

        public ColumnExprWrapper(ColumnExpr expr)
        {
            this.expr = expr;
        }

        @Override
        public DataType getDataType()
        {
            return expr.getDataType();
        }

        @Override
        public String getName()
        {
            return expr.getName();
        }

        @Override
        public String getTitle()
        {
            return expr.getTitle();
        }

        @Override
        public String getControlType()
        {
            return expr.getControlType();
        }

        @Override
        public Object getAttribute(String name)
        {
            return expr.getAttribute(name);
        }

        @Override
        public Options getOptions()
        {
            return expr.getOptions();
        }

        @Override
        public String getBeanPropertyName()
        {
            return expr.getBeanPropertyName();
        }

        @Override
        public Column getSourceColumn()
        {
            return expr.getSourceColumn();
        }

        @Override
        public double getSize()
        {
            return 0;
        }

        @Override
        public boolean isRequired()
        {
            return false;
        }

        @Override
        public boolean isAutoGenerated()
        {
            return false;
        }

        @Override
        public boolean isReadOnly()
        {
            return true;
        }

        @Override
        public boolean isEnum()
        {
            return getSourceColumn().isEnum();
        }

        @Override
        public Object validate(Object value)
        {
            log.warn("validate not supported for {}", expr.getName());
            return value;
        }
    }

    /**
     * ValueInfoImpl
     * Provides information necessary to render a data value (non editable) 
     * @author doebele
     */
    protected class ValueInfoImpl implements InputControl.ValueInfo
    {
        public ValueInfoImpl(Column column, TextResolver resolver)
        {
            if (column==null)
                throw new InvalidArgumentException("column", resolver);
            if (resolver==null)
                throw new InvalidArgumentException("resolver", resolver);
        }

        /* Value Options */
        protected boolean hasColumn()
        {
            return (column != null || getColumn() != null);
        }

        @Override
        public Column getColumn()
        {
            return column;
        }

        @Override
        public Object getValue(boolean evalExpression)
        {
            return getDataValue(evalExpression);
        }

        @Override
        public Options getOptions()
        {
            return getValueOptions();
        }

        /*
        @Override
        public Object getNullValue()
        {
            // null value
            Object attr = getTagAttributeValue("default");
            if (attr != null)
                return attr;
            // Use Column default
            if (hasColumn())
                return column.getAttribute("default");
            // not available
            return null;
        }
        */

        @Override
        public String getStyleClass(String addlStyle)
        {
            String style = getTagStyleClass(addlStyle);
            return style; 
        }

        @Override
        public String getFormat()
        {
            // null value
            String attr = getTagAttributeString("format");
            if (attr != null)
                return attr;
            // Use Column default
            if (hasColumn())
            { // from column
                Object format = column.getAttribute("format");
                if (format != null)
                    return format.toString();
            }
            // not available
            return null;
        }

        @Override
        public Locale getLocale()
        {
            return textResolver.getLocale();
        }

        @Override
        public String getText(String text)
        {
            return textResolver.resolveText(text);
        }

        @Override
        public TextResolver getTextResolver()
        {
            return textResolver;
        }
        
        @Override 
        public boolean isInsideUIData()
        {
            return TagEncodingHelper.this.isInsideUIData();
        }
    }

    /**
     * InputInfoImpl
     * Provides information necessary to render an input control (editable) 
     * @author doebele
     */
    protected class InputInfoImpl extends ValueInfoImpl implements InputControl.InputInfo
    {
        public InputInfoImpl(Column column, TextResolver resolver)
        {
            super(column, resolver);
        }

        @Override
        public void setValue(Object value)
        {
            setDataValue(value);
        }

        @Override
        public void validate(Object value)
        {
            // skip?
            if (skipValidation)
                return;
            // Make sure null values are not forced to be required
            boolean isNull = ObjectUtils.isEmpty(value);
            if (isNull)
            {   // Check Required
                if (isRequired())
                    throw new FieldNotNullException(column);
                return; // not required
            }
            // validate through record (if any)
            if ((getRecord() instanceof Record))
               ((Record)getRecord()).validateValue(column, value);
            else
                column.validate(value);
        }

        @Override
        public boolean isRequired()
        {
            return isValueRequired();
        }

        @Override
        public boolean isDisabled()
        {
            return isReadOnly();
        }

        @Override
        public boolean isFieldReadOnly()
        {   // Check Record
            if (isRecordReadOnly())
                return true;
            // Check tag
            if (!(component instanceof UIInput))
                return true;
            // Check Record
            if ((getRecord() instanceof Record))
            { // Ask Record
                Record r = (Record) record;
                return r.isFieldReadOnly(getColumn());
            }
            // column
            return getColumn().isReadOnly();
        }
        
        @Override
        public String getInputId()
        {   /**
             * obsolete since ControlTag or InputTag will set column name as id
             * 
             Column c = getColumn();
             return c.getName();
            */
            return "inp";
        }
        
        @Override
        public boolean hasError()
        {
            return hasError;
        }

        @Override
        public Object getAttribute(String name)
        {
            return getTagAttributeValue(name);
        }
        
        @Override
        public Object getAttributeEx(String name)
        {
            return getAttributeValueEx(name);
        }
    }

    // Logger
    private static final Logger log = LoggerFactory.getLogger(TagEncodingHelper.class);

    public static final String ORIGINAL_COMPONENT_ID  = "ORIGINAL_COMPONENT_ID"; 

    public static final String COLATTR_TOOLTIP        = "TOOLTIP";          // Column tooltip
    public static final String COLATTR_ABBR_TITLE     = "ABBR_TITLE";       // Column title for abbreviations
    
    protected final UIOutput      component;
    protected final String        cssStyleClass;
    protected Column              column       = null;
    protected Object              record       = null;
    protected RecordTag           recordTag    = null;
    // protected Boolean          tagRequired  = null;
    protected Boolean             hasValueRef  = null;
    protected InputControl        control      = null;
    protected TextResolver        textResolver = null;
    protected Object              mostRecentValue = null;
    protected boolean             skipValidation = false;
    protected boolean             hasError     = false;
    protected Boolean             insideUIData = null;

    protected TagEncodingHelper(UIOutput component, String cssStyleClass)
    {
        this.component = component;
        this.cssStyleClass = cssStyleClass;
    }

    public void encodeBegin()
    {
        if (component instanceof UIInput)
        {   // has local value?
            if (((UIInput)component).isLocalValueSet())
            {   /* clear local value */
                if (log.isDebugEnabled())
                    log.debug("clearing local value for {}. value is {}.", getColumnName(), ((UIInput)component).getLocalValue());
                ((UIInput)component).setValue(null);
                ((UIInput)component).setLocalValueSet(false);
            }
            /*
            ValueExpression ve = findValueExpression("required", true);
            if (ve!=null)
            {   Object req = ve.getValue(FacesContext.getCurrentInstance().getELContext());
                if (req!=null)
                    tagRequired = new Boolean(ObjectUtils.getBoolean(req));
            }
            */
        }
    }
    
    protected static final String PH_COLUMN_NAME = "{column}";  // placeholder for column name
    protected static final String PH_COLUMN_FULL = "{COLUMN}";  // placeholder for column full name including table

    public String completeInputTagId(String id)
    {
        if (StringUtils.isEmpty(id))
            return getColumnName();
        // replace placeholder
        if (id.indexOf(PH_COLUMN_NAME)>=0)
        {   // column name only
            id = id.replace(PH_COLUMN_NAME, getColumnName());
        }
        else if (id.indexOf(PH_COLUMN_FULL)>=0) 
        {   // column full name including table
            String name= null;
            Column c = getColumn();
            if (c instanceof DBColumn)
                name = ((DBColumn)c).getFullName().replace('.', '_');
            else if (c!=null)
                name = c.getName();
            id = id.replace(PH_COLUMN_FULL, String.valueOf(name));
        }
        // done 
        return id;
    }
    
    public InputControl getInputControl()
    {
        if (control != null)
        {   // Must check record!
            checkRecord();
            return control;
        }    
        // Create
        if (getColumn() == null)
        	throw new NotSupportedException(this, "getInputControl");
        // Get Control from column
        String controlType = getTagAttributeString("controlType");
        if (controlType==null)
        {   controlType = column.getControlType();
            // Always use SelectInputControl
            if (TextInputControl.NAME.equalsIgnoreCase(controlType))
            {   Object attr = getTagAttributeValue("options");
                if (attr != null && (attr instanceof Options) && !((Options)attr).isEmpty())
                    controlType = SelectInputControl.NAME;
            }
        }
        // detect Control
        control = detectInputControl(controlType, column.getDataType(), column.getOptions()!=null);
        // check record
        checkRecord();
        return control;
    }

    protected InputControl detectInputControl(String controlType, DataType dataType, boolean hasOptions)
    {
        // Create
        if (dataType==null)
            throw new InvalidArgumentException("dataType", dataType);
        // find control type
        InputControl control = null;
        if (StringUtils.isNotEmpty(controlType))
            control = InputControlManager.getControl(controlType);
        if (control == null)
        {   // Auto-detect
            if (hasOptions)
                controlType = SelectInputControl.NAME;
            else
            {   // get from data type
                switch (dataType)
                {
                    case CLOB:
                        controlType = TextAreaInputControl.NAME;
                        break;
                    default:
                        controlType = TextInputControl.NAME;
                }
            }
            // get default control
            control = InputControlManager.getControl(controlType);
            // Still not? Use Text Control
            if (control == null)
                control = InputControlManager.getControl(TextInputControl.NAME);
        }
        // check record
        return control;
    }
    
    protected void checkRecord()
    {
        // Record may change even for the same instance
        if (this.record== null)
            return;
        // Check direct record property
        Object rec = getTagAttributeValue("record");
        if (rec!=null)
        {   // record directly specified
            if (rec!=this.record)
            {   // Record has changed
                if (log.isTraceEnabled())
                {   // Debug output
                    if ((rec instanceof DBRecord) && (this.record instanceof DBRecord))
                    {   // a database record change
                        String keyOld = StringUtils.toString(((DBRecord)this.record).getKeyValues());
                        String keyNew = StringUtils.toString(((DBRecord)rec).getKeyValues());
                        String rowSet = StringUtils.valueOf(((DBRecord)rec).getRowSet().getName());
                        log.trace("Changing "+component.getClass().getSimpleName()+" record of rowset "+rowSet+" from {} to {}", keyOld, keyNew);
                    }
                    else
                    {   // probably a bean change
                        log.trace("Changing "+component.getClass().getSimpleName()+" record of class "+rec.getClass().getSimpleName());
                    }
                }
                // change now
                setRecord(rec);
            }    
        }
        else 
        {   // Do we have a record-tag?
            if (recordTag!=null)
            {   // Check Record change
                rec = recordTag.getRecord();
                if (rec!=this.record)
                {   // Record has changed
                    setRecord(rec);
                }
            }    
            // Invalidate if not an instance of Record
            else if (!(this.record instanceof Record))
                this.record = null;
        }
    }

    public InputControl.ValueInfo getValueInfo(FacesContext ctx)
    {
        return new ValueInfoImpl(getColumn(), getTextResolver(ctx));
    }

    public InputControl.InputInfo getInputInfo(FacesContext ctx)
    {
        // Skip validate
        skipValidation = FacesUtils.isSkipInputValidation(ctx);
        // check whether we have got an error
        hasError = detectError(ctx);            
        // create
        return new InputInfoImpl(getColumn(), getTextResolver(ctx));
    }
    
    public boolean isPartialSubmit(FacesContext ctx)
    {
        return FacesUtils.getWebApplication().isPartialSubmit(ctx);
    }

    public boolean isSkipValidation()
    {
        return skipValidation;
    }

    public boolean hasColumn()
    {
        if (column == null)
            column = findColumn();
        return (column != null);
    }

    public Column getColumn()
    {
        if (column == null)
            column = findColumn();
        if (column == null)
            throw new InvalidArgumentException("column", column);
        return column;
    }
    
    public String getColumnName()
    {
        return ((this.column=findColumn())!=null ? column.getName() : "null");
    }

    public void setColumn(Column column)
    {
        this.column = column;
    }

    public Object getRecord()
    {
        if (record == null)
            record = findRecord();
        return record;
    }

    public void setRecord(Object record)
    {
        this.record = record;
        this.mostRecentValue = null; 
    }

    public RecordTag getRecordComponent()
    {
        // already present?
        if (recordTag != null)
            return recordTag;
        // Check record
        if (record != null || (record=getTagAttributeValue("record"))!=null)
            return null; // No record tag: Record has been specified!
        // walk upwards the parent component tree and return the first record component found (if any)
        UIComponent parent = component;
        while ((parent = parent.getParent()) != null)
        {
            if (parent instanceof RecordTag)
            {
                recordTag = (RecordTag) parent;
                return recordTag;
            }
        }
        return null;
    }

    protected boolean isDetectFieldChange()
    {
        Object v = this.getTagAttributeValue("detectFieldChange");
        if (v==null && recordTag != null)
            v = recordTag.getAttributes().get("detectFieldChange");
        return (v!=null ? ObjectUtils.getBoolean(v) : true);
    }

    public Object getDataValue(boolean evalExpression)
    {
        if (getRecord() != null)
        {   // value
            if (record instanceof RecordData)
            { // a record
                mostRecentValue = ((RecordData) record).getValue(getColumn());
                return mostRecentValue;
            }   
            else
            { // a normal bean
                String prop = getColumn().getBeanPropertyName();
                return getBeanPropertyValue(record, prop);
            }
        }
        else
        {   // Get from tag
            if (evalExpression)
                return component.getValue();
            else
            {   // return value or value expression
                Object value = component.getLocalValue();
                if (value!=null && (component instanceof UIInput) && !((UIInput)component).isLocalValueSet())
                    value= null; /* should never come here! */
                if (value==null)
                    value = findValueExpression("value", false);
                
                // value = tag.getValue();
                return value;
            }
        }
    }

    public void setDataValue(Object value)
    {
        if (getRecord() != null)
        {   // value
            if (record instanceof Record)
            {   getColumn();
                /* special case
                if (value==null && getColumn().isRequired())
                {   // ((Record)record).isFieldRequired(column)==false
                    log.warn("Unable to set null for required field!");
                    return;
                }
                */
                if (mostRecentValue!=null && isDetectFieldChange())
                {   // DetectFieldChange by comparing current and most recent value
                    Object currentValue = ((Record) record).getValue(column);
                    if (!ObjectUtils.compareEqual(currentValue, mostRecentValue))
                    {   // Value has been changed by someone else!
                        log.info("Concurrent data change for "+column.getName()+". Current Value is {}. Ignoring new value {}", currentValue, value);
                        return;
                    }
                }
                // check whether to skip validation
                boolean reenableValidation = false;
                if (skipValidation && (record instanceof DBRecord))
                {   // Ignore read only values
                    if (this.isReadOnly())
                        return;
                    // Column required?
                    if (column.isRequired() && ObjectUtils.isEmpty(value))
                        return; // Cannot set required value to null
                    // Disable Validation
                    reenableValidation = ((DBRecord)record).isValidateFieldValues();
                    if (reenableValidation)
                        ((DBRecord)record).setValidateFieldValues(false);
                    // Validation skipped for
                    if (log.isDebugEnabled())
                        log.debug("Input Validation skipped for {}.", column.getName());
                }
                // Now, set the value
                try {
                    ((Record) record).setValue(column, value);
                    mostRecentValue = value;
                } finally {
                    // re-enable validation
                    if (reenableValidation)
                        ((DBRecord)record).setValidateFieldValues(true);
                }
            }
            else if (record instanceof RecordData)
            { // a record
                throw new PropertyReadOnlyException("record");
            }
            else
            { // a normal bean
                String prop = getColumn().getBeanPropertyName();
                setBeanPropertyValue(record, prop, value);
            }
        }
        else
        { // Get from tag
          // tag.setValue(value);
            ValueExpression ve = component.getValueExpression("value");
            if (ve == null)
                throw new PropertyReadOnlyException("value");

            FacesContext ctx = FacesContext.getCurrentInstance();
            ve.setValue(ctx.getELContext(), value);
        }
    }

    public boolean isRecordReadOnly()
    {
        // check attribute
        Object val = getTagAttributeValue("readonly");
        if (val != null && ObjectUtils.getBoolean(val))
            return true;
        // Do we have a record?
        if (getRecord() instanceof RecordData)
        { // Only a RecordData?
            if (!(record instanceof Record) || ((Record) record).isReadOnly())
                return true;
        }
        else if (!hasValueReference())
        { // No Value expression given
            return true;
        }
        // Check Component
        if (recordTag != null && recordTag.isReadOnly())
            return true;
        // column
        return false;
    }

    public boolean isVisible()
    {
        // reset record
        if (this.record!=null && (getTagAttributeValue("record") instanceof Record))
            this.record=null;
        // Check Record
        if ((getRecord() instanceof Record))
        { // Ask Record
            Record r = (Record) record;
            return r.isFieldVisible(getColumn());
        }
        // column
        return true;
    }

    public boolean isReadOnly()
    {
        // check attribute
        Object val = getAttributeValueEx("disabled");
        if (val != null && ObjectUtils.getBoolean(val))
            return true;
        // Check Record
        if (isRecordReadOnly())
            return true;
        // Check Record
        if ((getRecord() instanceof Record))
        { // Ask Record
            Record r = (Record) record;
            return r.isFieldReadOnly(getColumn());
        }
        // column
        return getColumn().isReadOnly();
    }

    public boolean isValueRequired()
    {
        // See if the tag is required (don't use the "required" attribute or tag.isRequired()!)
        Object mandatory = getTagAttributeValue("mandatory");
        if (mandatory!=null)
            return ObjectUtils.getBoolean(mandatory);
        // Check Record
        if ((getRecord() instanceof Record))
        {   // Ask Record
            Record r = (Record) record;
            return r.isFieldRequired(getColumn());
        }
        // Check Value Attribute
        if (hasValueReference())
            return false;
        // Required
        return getColumn().isRequired();
    }
    
    /**
     * used for partial submits to detect whether the value of this field can be set to null
     */
    public boolean isTempoaryNullable()
    {
        if (getColumn().isRequired())
            return false;
        return true;        
    }

    /* Helpers */
    protected Column findColumn()
    {
        // if parent is a record tag, get the record from there
        Object col = getTagAttributeValue("column");
        if (col instanceof Column)
        { // cast to column
            return (Column) col;
        }
        if (col instanceof String)
        {   // parse String
            String name = String.valueOf(col);
            int dbix = name.indexOf('.');
            if (dbix<=0)
            {
                log.error("Invalid column expression '{}'!", name);
                return null; // not found
            }
            DBDatabase db = DBDatabase.findById(name.substring(0,dbix));
            if (db==null)
            {
                log.error("Database '{}' not found!", name.substring(0,dbix));
                return null; // not found
            }
            int co = name.lastIndexOf('.');
            int to = name.lastIndexOf('.', co - 1);
            String cn = name.substring(co + 1);
            String tn = name.substring(to + 1, co);
            DBRowSet rs = db.getRowSet(tn);
            if (rs == null)
            {
                log.error("Table/View '{}' not found in database!", tn);
                return null; // not found
            }
            Column column = rs.getColumn(cn);
            if (column == null)
            {
                log.error("Column '{}' not found in table/view '{}'!", cn, tn);
                return null; // not found
            }
            // done
            return column;
        }
        // When null, try value
        if (col == null)
        { // Try value
            col = component.getValue();
            // Column supplied?
            if (col instanceof Column)
            {
                return (Column) col;
            }
            // Column expression supplied?
            if (col instanceof ColumnExpr)
            { // Use source column instead 
                Column source = ((ColumnExpr) col).getSourceColumn();
                if (source != null)
                    return source;
                // No source column? --> wrap 
                return createColumnExprWrapper((ColumnExpr) col);
            }
        }
        // No column!
        if (log.isDebugEnabled() && !(component instanceof LinkTag))
            log.warn("No Column provided for value tag!");
        return null;
    }
    
    protected Column createColumnExprWrapper(ColumnExpr colExpr)
    {
         return new ColumnExprWrapper(colExpr);
    }

    protected Object findRecord()
    {
        Object rec = getTagAttributeValue("record");
        if (rec != null)
            return rec;
        // Value expression
        if (hasValueReference())
        {   // See if the record is in value
            return null;
        }
        // if parent is a record tag, get the record from there
        RecordTag recordComponent = getRecordComponent();
        if (recordComponent != null)
        {
            rec = recordComponent.getRecord();
        }
        else
        {   // not supplied
            if ((component instanceof ControlTag) && !((ControlTag)component).isCustomInput())
                log.warn("No record supplied for {} and column {}.", component.getClass().getSimpleName(), getColumnName());
        }
        return rec;
    }
    
    protected boolean hasValueReference()
    {
        // Find expression
        if (hasValueRef != null)
            return hasValueRef.booleanValue();
        // Find expression
        boolean hasVR = false;
        ValueExpression ve = findValueExpression("value", false);
        if (ve != null)
        {   // check
            ELContext elc = FacesContext.getCurrentInstance().getELContext();
            ValueReference vr = ve.getValueReference(elc);
            if (vr!=null && log.isDebugEnabled())  
            {   // log value reference
                Object base = vr.getBase();
                Object property = vr.getProperty();
                String writeable = (ve.isReadOnly(elc) ? "read-only" : "updateable");
                String beanName = (base!=null ? base.getClass().getSimpleName() : "{NULL}");
                log.debug("Tag-ValueExpression for {} on {}.{} is {}. Expression is \"{}\".", getColumnName(), beanName, property, writeable, ve.getExpressionString());
            }
            // set result
            hasVR = (vr!=null);
        }
        // store result to avoid multiple detection 
        hasValueRef = Boolean.valueOf(hasVR);
        return hasValueRef.booleanValue();
    }
    
    protected static final String CC_ATTR_EXPR = "#{cc.attrs.";
    
    @SuppressWarnings("unchecked")
    protected ValueExpression findValueExpression(String attribute, boolean allowLiteral)
    {
        // Check for expression
        ValueExpression ve = component.getValueExpression(attribute);
        if (ve == null)
            return null;
        // Find expression
        UIComponent parent = component;
        String expr = ve.getExpressionString();
        while (expr.startsWith(CC_ATTR_EXPR))
        {
            // Unwrap
            if (ve instanceof FacesWrapper<?>)
                ve = ((FacesWrapper<ValueExpression>)ve).getWrapped();
            // find parent
            UIComponent valueParent = FacesUtils.getWebApplication().getFacesImplementation().getValueParentComponent(ve);
            if (valueParent!=null)
            {	// use the value parent
            	parent = valueParent;
            }
            else
            {   // find parent
                parent = UIComponent.getCompositeComponentParent(parent);
            }
            if (parent == null)
                return null;
            // check expression
            int end = expr.indexOf('}');
            String attrib = expr.substring(CC_ATTR_EXPR.length(), end);
            if (attrib.indexOf('.')>0)
                return ve; // do not investigate any further
            // find attribute
            ValueExpression next = parent.getValueExpression(attrib);
            if (next == null)
            {   // allow literal
                if (allowLiteral && (parent.getAttributes().get(attrib)!=null))
                    return ve;
                // not found
                return null;
            }
            // get new expression String
            ve = next;
            expr = ve.getExpressionString();
        }
        // found
        return ve;
    }
    
    protected Options getValueOptions()
    {
        // null value
        Object attr = getTagAttributeValue("options");
        if (attr != null && (attr instanceof Options))
            return ((Options) attr);
        if (getColumn() != null)
        { // Do we have a record?
            if (getRecord() instanceof Record)
                return ((Record) record).getFieldOptions(column);
            // get From Column
            return column.getOptions();
        }
        // not available
        return null;
    }

    protected Object getBeanPropertyValue(Object bean, String property)
    {
        try
        { // Get Property Value
            PropertyUtilsBean pub = BeanUtilsBean.getInstance().getPropertyUtils();
            return pub.getSimpleProperty(bean, property);

        }
        catch (IllegalAccessException e)
        {
            log.error(bean.getClass().getName() + ": unable to get property '" + property + "'");
            throw new BeanPropertyGetException(bean, property, e);
        }
        catch (InvocationTargetException e)
        {
            log.error(bean.getClass().getName() + ": unable to get property '" + property + "'");
            throw new BeanPropertyGetException(bean, property, e);
        }
        catch (NoSuchMethodException e)
        {
            log.warn(bean.getClass().getName() + ": no getter available for property '" + property + "'");
            throw new BeanPropertyGetException(bean, property, e);
        }
    }

    protected void setBeanPropertyValue(Object bean, String property, Object value)
    {
        // Get Property Name
        try
        { // Get Property Value
            if (ObjectUtils.isEmpty(value))
                value = null;
            // Set Property Value
            if (value != null)
            { // Bean utils will convert if necessary
                BeanUtils.setProperty(bean, property, value);
            }
            else
            { // Don't convert, just set
                PropertyUtils.setProperty(bean, property, null);
            }
        }
        catch (IllegalArgumentException e)
        {
            log.error(bean.getClass().getName() + ": invalid argument for property '" + property + "'");
            throw new BeanPropertySetException(bean, property, e);
        }
        catch (IllegalAccessException e)
        {
            log.error(bean.getClass().getName() + ": unable to set property '" + property + "'");
            throw new BeanPropertySetException(bean, property, e);
        }
        catch (InvocationTargetException e)
        {
            log.error(bean.getClass().getName() + ": unable to set property '" + property + "'");
            throw new BeanPropertySetException(bean, property, e);
        }
        catch (NoSuchMethodException e)
        {
            log.error(bean.getClass().getName() + ": no setter available for property '" + property + "'");
            throw new BeanPropertySetException(bean, property, e);
        }
    }
    
    public String getValueTooltip(Object value)
    {
        if (value == null)
            return null;
        // is it a column
        if (value instanceof Column)
        {   // find column value   
            Column ttc = ((Column)value);
            if (getRecord() != null)
            {   // value
                if (record instanceof RecordData)
                { // a record
                    value = ((RecordData) record).getValue(ttc);
                }
                else
                { // a normal bean
                    String prop = ttc.getBeanPropertyName();
                    value = getBeanPropertyValue(record, prop);
                }
                // translate
                // ValueInfoImpl vi = new MiscValueInfoImpl(ttc, textResolver);
                // InputControl ctrl = detectInputControl(ttc.getControlType(), ttc.getDataType(), ttc.getOptions()!=null);
                return StringUtils.valueOf(value);
            } 
            else
            {   // Error
                log.warn("Unable to resolve Tooltip-value for column {}.", ttc.getName());
                return null;
            }
        }
        // is it a template?
        String templ = StringUtils.valueOf(value);
        int valIndex = templ.indexOf("{}");
        if (valIndex >= 0)
            value = getDataValue(true);
        // Check Options
        String text;
        Options options = getValueOptions();
        if (options != null && !hasFormat("notitlelookup"))
        { // Lookup the title
            String optValue = options.get(value);
            text = getDisplayText(optValue);
        }
        else
            text = getDisplayText(StringUtils.toString(value));
        // Check for template
        if (valIndex >= 0 && text!=null)
            text = StringUtils.replace(templ, "{}", text);
        return text;
    }

    public String getLabelTooltip(Column column)
    {
        String title = getTagAttributeString("title");
        if (title == null)
            title = StringUtils.toString(column.getAttribute(Column.COLATTR_TOOLTIP));
        if (title != null)
            return getDisplayText(title);
        // Check for short form
        if (hasFormat("short") && !ObjectUtils.isEmpty(column.getAttribute(COLATTR_ABBR_TITLE)))
            return getDisplayText(column.getTitle());
        
        // No Title
        return null;
    }

    public boolean hasFormat(String format)
    { // null value
        String f = getTagAttributeString("format");
        return (f != null && f.indexOf(format) >= 0);
    }

    public boolean hasFormat(InputControl.ValueInfo vi, String format)
    {
        String f = vi.getFormat();
        return (f != null && f.indexOf(format) >= 0);
    }

    public void writeAttribute(ResponseWriter writer, String attribute, Object value)
        throws IOException
    {
        if (value != null)
            writer.writeAttribute(attribute, value, null);
    }
    
    public String getDisplayText(String text)
    {
        if (textResolver==null)
            getTextResolver(FacesContext.getCurrentInstance());
        return textResolver.resolveText(text);
    }

    public TextResolver getTextResolver(FacesContext context)
    {
        if (textResolver==null)
            textResolver=WebApplication.getInstance().getTextResolver(context);
        return textResolver;
    }
    
    protected boolean detectError(FacesContext context)
    {
        Iterator<FacesMessage> iter = context.getMessages(component.getClientId());
        while (iter.hasNext())
        {   // Check for error
            FacesMessage m = iter.next();
            if (m.getSeverity()==FacesMessage.SEVERITY_ERROR)
                return true;
        }
        return false;
    }
    
    public void addErrorMessage(FacesContext context, Exception e)
    {
        String msgText = getTextResolver(context).getExceptionMessage(e);
        FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, msgText, msgText);
        context.addMessage(component.getClientId(), msg);
    }

    public Object getAttributeValueEx(String name)
    { 
        Object value = getTagAttributeValue(name);
        if (value==null && hasColumn())
        {   // Check Column
            value = column.getAttribute(name);
        }
        // Checks whether it's another column    
        if (value instanceof Column)
        {   // Special case: Value is a column
            Column col = ((Column)value);
            Object rec = getRecord();
            if (rec instanceof Record)
                return ((Record)rec).getValue(col);
            else if (rec!=null)
            {   // Get Value from a bean
                String property = col.getBeanPropertyName();
                try
                {   // Use Beanutils to get Property
                    PropertyUtilsBean pub = BeanUtilsBean.getInstance().getPropertyUtils();
                    return pub.getSimpleProperty(rec, property);
                }
                catch (Exception e)
                {   log.error("BeanUtils.getSimpleProperty failed for "+property, e);
                    return null;
                }
            }    
            return null;
        }
        return value;
    }

    public String getTagAttributeStringEx(String name)
    {
        Object value = getAttributeValueEx(name);
        return (value!=null) ? StringUtils.toString(value) : null;
    }
    
    public Object getTagAttributeValue(String name)
    {
        return TagEncodingHelper.getTagAttributeValue(component, name);
    }

    public static Object getTagAttributeValue(UIComponent comp, String name)
    {
        Object value = comp.getAttributes().get(name);
        if (value==null)
        {   // try value expression
            ValueExpression ve = comp.getValueExpression(name);
            if (ve!=null)
            {   // It's a value expression
                FacesContext ctx = FacesContext.getCurrentInstance();
                value = ve.getValue(ctx.getELContext());
            }
        }
        return value;
    }
    
    public String getTagAttributeString(String name, String defValue)
    {
        Object  v = getTagAttributeValue(name);
        return (v!=null) ? StringUtils.toString(v) : defValue;
    }

    public String getTagAttributeString(String name)
    {
        return getTagAttributeString(name, null);
    }

    /* ********************** label ********************** */

    protected String getLabelValue(Column column, boolean colon)
    {
        String label = getTagAttributeString("label");
        if (label==null)
        {   // Check for short form    
            if (hasFormat("short"))
            {
                label = StringUtils.toString(column.getAttribute(COLATTR_ABBR_TITLE));
                if (label==null)
                    log.warn("No Abbreviation available for column {}. Using normal title.", column.getName());
            }
            // Use normal title
            if (label==null)
                label=column.getTitle();
            // translate
            label = getDisplayText(label);
        }    
        // handle empty string
        if (StringUtils.isEmpty(label))
            return "";
        // getColon
        if (colon) 
            label = label.trim() + ":";
        // done
        return label;
    }
    
    public HtmlOutputLabel createLabelComponent(FacesContext context, String forInput, String styleClass, String style, boolean colon)
    {
        Column column = null;
        boolean readOnly=false;
        boolean required=false;
        // forInput provided
        if (StringUtils.isNotEmpty(forInput))
        {   // find the component
            if (!forInput.equals("*"))
            {   // Set Label input Id
                UIComponent input = FacesUtils.getWebApplication().findComponent(context, forInput, component);
                if (input!=null && (input instanceof InputTag))
                {   // Check Read-Only
                    InputTag inputTag = ((InputTag)input);
                    column = inputTag.getInputColumn();
                    readOnly = inputTag.isInputReadOnly();
                    required = inputTag.isInputRequired();
                }
                else
                {   // Not found (<e:input id="ABC"...> must match <e:label for="ABC"...>
                    log.warn("Input component {} not found for label {}.", forInput, getColumn().getName());
                }                
            }
            else if (component instanceof LabelTag)
            {   // for LabelTag
                forInput = this.getColumnName();
                // readOnly
                Object val = getAttributeValueEx("readOnly");
                if (val!=null)
                    readOnly = ObjectUtils.getBoolean(val);
                else 
                    readOnly = this.isReadOnly();
            }
            else 
            {   // for ControlTag
                readOnly = this.isReadOnly();
            }
        }
        
        // Column provided?
        if (column==null)
        {   // Get from LinkTag
            column = getColumn();
            required = isValueRequired(); // does only check Attribute
        }
            
        // Check column
        if (column==null)
            throw new InvalidArgumentException("column", column);

        // create label now
        HtmlOutputLabel label = InputControlManager.createComponent(context, InputControlManager.getLabelComponentClass());
        
        // value
        String labelText = getLabelValue(column, colon);
        if (StringUtils.isEmpty(labelText))
            label.setRendered(false);
        else
            label.setValue(labelText);

        // styleClass
        if (StringUtils.isNotEmpty(styleClass))
            label.setStyleClass(completeLabelStyleClass(styleClass, required));
        
        // for 
        if (StringUtils.isNotEmpty(forInput) && !readOnly)
        {   // Set Label input Id
            InputControl.InputInfo ii = getInputInfo(context);
            String inputId = getInputControl().getLabelForId(ii);
            if (StringUtils.isNotEmpty(inputId))
            {   // input_id was given
                if (forInput.equals("*"))
                    label.setFor(inputId);
                else
                    label.setFor(forInput+":"+inputId);
            }
            else
            {   // No input-id available
                log.info("No input-id provided for {}.", getColumn().getName());
            }    
        }    

        // style
        if (StringUtils.isNotEmpty(style))
            label.setStyle(style);

        // title
        String title = getLabelTooltip(column);
        if (title!=null)
            label.setTitle(title);
        
        // required
        if (required && InputControlManager.isShowLabelRequiredMark())
            addRequiredMark(label);

        return label;
    }
    
    public void updateLabelComponent(FacesContext context, HtmlOutputLabel label, String forInput)
    {
        // Find Input Control (only if forInput Attribute has been set!)
        InputTag inputTag = null;
        if (StringUtils.isNotEmpty(forInput) && !forInput.equals("*"))
        {   // Set Label input Id
            UIComponent input = FacesUtils.getWebApplication().findComponent(context, forInput, component);
            if (input!=null && (input instanceof InputTag))
            {   // Check Read-Only
                inputTag = ((InputTag)input);
            }
        }
        // Is the Mark required?
        boolean required = (inputTag!=null ? inputTag.isInputRequired() : isValueRequired());
        // Style Class
        String styleClass = label.getStyleClass();
        label.setStyleClass(completeLabelStyleClass(styleClass, required));
        // set mark
        boolean hasMark = (label.getChildCount()>0);
        if (required==hasMark)
            return;
        // Add or remove the mark
        if (required && InputControlManager.isShowLabelRequiredMark())
            addRequiredMark(label);
        else
            label.getChildren().clear();
    }

    protected String completeLabelStyleClass(String styleClass, boolean required)
    {
        final String LABEL_REQ_STYLE = " "+InputControl.STYLECLASS_REQUIRED;

        boolean hasRequired = StringUtils.contains(styleClass, LABEL_REQ_STYLE);
        if (required==hasRequired)
            return styleClass; // no change
        // must be empty at least
        if (styleClass==null)
            styleClass="";
        // add or remove
        if (required) {
            styleClass += LABEL_REQ_STYLE;
        }    
        else
        {   // remove both   
            styleClass = StringUtils.remove(styleClass, LABEL_REQ_STYLE);
        }    
        // done
        return styleClass;
    }
    
    protected void addRequiredMark(HtmlOutputLabel label)
    {
        HtmlPanelGroup span = new HtmlPanelGroup();
        span.setStyleClass("required");
        HtmlOutputText text = new HtmlOutputText();
        text.setValue("*");
        span.getChildren().add(text);
        label.getChildren().add(span);
    }
    
    /* ********************** CSS-generation ********************** */

    public static final String assembleStyleClassString(String tagCssStyle, String typeClass, String addlStyle, String userStyle)
    {
        // handle simple case
        if (StringUtils.isEmpty(userStyle) && StringUtils.isEmpty(addlStyle))
            return StringUtils.isEmpty(typeClass) ? tagCssStyle : tagCssStyle + typeClass;
        // assemble 
        StringBuilder b = new StringBuilder(tagCssStyle);
        if (StringUtils.isNotEmpty(typeClass))
        {   // type class must begin with a space!
            b.append(typeClass);
        }
        if (StringUtils.isNotEmpty(addlStyle))
        {   // add additional style class
            b.append(" ");
            b.append(addlStyle);
        }
        if (StringUtils.isNotEmpty(userStyle) && !StringUtils.compareEqual(userStyle, addlStyle, false))
        {   // add user style class
            b.append(" ");
            b.append(userStyle);
        }
        return b.toString();
    }

    public static final String CSS_DATA_TYPE_NONE     = "";
    public static final String CSS_DATA_TYPE_IDENT    = " eTypeIdent";
    public static final String CSS_DATA_TYPE_NUMBER   = " eTypeNumber";
    public static final String CSS_DATA_TYPE_TEXT     = " eTypeText";
    public static final String CSS_DATA_TYPE_LONGTEXT = " eTypeLongText";
    public static final String CSS_DATA_TYPE_DATE     = " eTypeDate";
    public static final String CSS_DATA_TYPE_BOOL     = " eTypeBool";

    protected String getDataTypeClass(DataType type)
    {
        switch (type)
        {
            case AUTOINC:
                return CSS_DATA_TYPE_IDENT;
            case INTEGER:
            case DECIMAL:
            case FLOAT:
                return CSS_DATA_TYPE_NUMBER;
            case TEXT:
            case VARCHAR:
            case CHAR:
                return CSS_DATA_TYPE_TEXT;
            case DATE:
            case DATETIME:
            case TIMESTAMP:
                return CSS_DATA_TYPE_DATE;
            case BOOL:
                return CSS_DATA_TYPE_BOOL;
            case CLOB:
                return CSS_DATA_TYPE_LONGTEXT;
            default:
                return CSS_DATA_TYPE_NONE; // Not provided
        }
    }

    public String getTagStyleClass(DataType dataType, String addlStyle, String userStyle)
    {
        String typeClass = (dataType!=null) ? getDataTypeClass(dataType) : null;
        String contextStyle = getContextStyleClass(addlStyle);
        return assembleStyleClassString(cssStyleClass, typeClass, contextStyle, userStyle);
    }

    public String getTagStyleClass(DataType dataType, String addlStyle)
    {
        String userStyle = getTagAttributeStringEx("styleClass");
        return getTagStyleClass(dataType, addlStyle, userStyle);
    }

    public String getTagStyleClass(String addlStyle)
    {
        DataType dataType = (hasColumn() ? column.getDataType() : null);
        return getTagStyleClass(dataType, addlStyle);
    }

    public final String getTagStyleClass()
    {
        return getTagStyleClass((String)null);
    }
    
    protected String getContextStyleClass(String addlStyle)
    {
        String contextStyle = null;
        if ((getRecord() instanceof TagContextInfo) && hasColumn())
        {
            contextStyle = ((TagContextInfo)getRecord()).getContextStyleClass(getColumn());
        }
        if (StringUtils.isEmpty(addlStyle))
        {
            return contextStyle;
        }
        if (StringUtils.isEmpty(contextStyle))
        {
            return addlStyle;
        }
        return addlStyle + " " + contextStyle;
    }
    
    public boolean isInsideUIData()
    {
        if (component==null)
            return false;
        if (this.insideUIData!=null)
            return this.insideUIData;
        // detect
        this.insideUIData = false;
        for (UIComponent p = component.getParent(); p!=null; p=p.getParent())
        {   // Check whether inside UIData
            if (p instanceof UIData) {
                this.insideUIData = true;
                break;
            }
        }
        return this.insideUIData;
    }
    
    public void saveComponentId(UIComponent comp)
    {
        if (comp==null || comp.getId()==null)
            return;
        String compId = comp.getId();
        comp.getAttributes().put(ORIGINAL_COMPONENT_ID, compId);
        comp.setId(compId); // reset
    }

    public void restoreComponentId(UIComponent comp)
    {
        if (comp==null)
            return;
        String compId = StringUtils.toString(comp.getAttributes().get(ORIGINAL_COMPONENT_ID));
        if (compId==null)
            return; // not set
        if (StringUtils.compareEqual(compId, comp.getId(), false)==false)
        {   // someone changed the id. Restore original Id
            log.warn("Restoring original Component-id from {} to {}", comp.getId(), compId);
            comp.setId(compId);
        }
    }
    
    public void resetComponentId(UIComponent comp)
    {
        if (comp==null || comp.getId()==null)
            return;
        if (isInsideUIData()) 
        {
            String resetId = comp.getId();
            if (log.isInfoEnabled())
                log.info("Resetting Component-id inside UIData to {}", resetId);
            comp.setId(resetId);
            
            /* needed ? 
            for (UIComponent c : comp.getChildren())
            {
                resetComponentId(c);
            }
            */
        }
    }
    
}
