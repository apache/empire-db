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
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

import javax.el.ValueExpression;
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

import org.apache.empire.commons.Attributes;
import org.apache.empire.commons.BeanPropertyUtils;
import org.apache.empire.commons.DateUtils;
import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.commons.Options;
import org.apache.empire.commons.StringUtils;
import org.apache.empire.commons.Unwrappable;
import org.apache.empire.data.Column;
import org.apache.empire.data.ColumnExpr;
import org.apache.empire.data.DataType;
import org.apache.empire.data.EntityType;
import org.apache.empire.data.Record;
import org.apache.empire.data.RecordData;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.DBRecordBase;
import org.apache.empire.db.DBRowSet;
import org.apache.empire.db.exceptions.FieldIllegalValueException;
import org.apache.empire.db.exceptions.FieldNotNullException;
import org.apache.empire.exceptions.EmpireException;
import org.apache.empire.exceptions.InvalidArgumentException;
import org.apache.empire.exceptions.InvalidPropertyException;
import org.apache.empire.exceptions.ItemNotFoundException;
import org.apache.empire.exceptions.NotSupportedException;
import org.apache.empire.exceptions.PropertyReadOnlyException;
import org.apache.empire.jsf2.app.FacesUtils;
import org.apache.empire.jsf2.app.TextResolver;
import org.apache.empire.jsf2.app.WebApplication;
import org.apache.empire.jsf2.components.ControlTag;
import org.apache.empire.jsf2.components.FormGridTag;
import org.apache.empire.jsf2.components.InputTag;
import org.apache.empire.jsf2.components.LabelTag;
import org.apache.empire.jsf2.components.LinkTag;
import org.apache.empire.jsf2.components.RecordTag;
import org.apache.empire.jsf2.controls.InputControl;
import org.apache.empire.jsf2.controls.InputControl.DisabledType;
import org.apache.empire.jsf2.controls.InputControl.InputInfo;
import org.apache.empire.jsf2.controls.InputControl.ValueInfo;
import org.apache.empire.jsf2.controls.InputControlManager;
import org.apache.empire.jsf2.controls.SelectInputControl;
import org.apache.empire.jsf2.controls.TextInputControl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TagEncodingHelper
 * Used by all Empire Components to exchange data and render 
 */
public class TagEncodingHelper implements NamingContainer
{
    public static final String FACES_ID_PREFIX = "j_id";  // Standard Faces ID Prefix

    public static boolean CSS_STYLE_USE_INPUT_TYPE_INSTEAD_OF_DATA_TYPE = true;
    
    /**
     * ColumnExprWrapper
     * wraps a ColumnExpr object into a Column interface object
     * @author doebele
     */
    protected static class ColumnExprWrapper implements Column, Unwrappable<ColumnExpr>
    {
        private final ColumnExpr expr;

        public ColumnExprWrapper(ColumnExpr expr)
        {
            this.expr = expr;
        }
        
        public ColumnExpr getExpr()
        {
            return this.expr;
        }

        @Override
        public DataType getDataType()
        {
            return expr.getDataType();
        }

        @Override
        public Class<Enum<?>> getEnumType()
        {
            return expr.getEnumType();
        }
        
        @Override
        public EntityType getEntityType()
        {
            Column column = getUpdateColumn();
            return (column!=null ? column.getEntityType() : null);
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
        public Set<Attributes.Attribute> getAttributes()
        {
            return expr.getUpdateColumn().getAttributes();
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
        public Column getUpdateColumn()
        {
            return expr.getUpdateColumn();
        }
        
        @Override
        @Deprecated
        public Column getSourceColumn()
        {
            return getUpdateColumn();
        }

        @Override
        public boolean isWrapper()
        { 
            return true;
        }

        @Override
        public ColumnExpr unwrap()
        {   // the wrapped expression
            return expr;
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
        public Object validateValue(Object value)
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
        private String format = null;
                    
        public ValueInfoImpl(Column column, TextResolver resolver)
        {
            if (column==null)
                throw new InvalidArgumentException("column", resolver);
            if (resolver==null)
                throw new InvalidArgumentException("resolver", resolver);
        }

        /*
         * reset cached data
         */
        protected void reset()
        {
            this.format = null;
        }

        /* Value Options */
        protected boolean hasColumn()
        {
            return (column != null || TagEncodingHelper.this.hasColumn());
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

        @Override
        public StyleClass getStyleClass()
        {
            return getTagStyleClass(); 
        }

        @Override
        public String getFormat()
        {
            if (format == null) 
            {   // null value
                format = getTagAttributeString("format");
                // Use Column default
                if (format==null && hasColumn())
                { // from column
                    format = getColumnAttributeString("format");
                }
                if (format==null)
                    format = StringUtils.EMPTY;
            }
            // not available
            return format;
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
            if (TagEncodingHelper.this.skipValidation)
                return;
            // Make sure null values are not forced to be required
            boolean isNull = ObjectUtils.isEmpty(value);
            if (isNull && validateNullValue())
            {   // OK, null allowed
                return;
            }
            // validate through record (if any)
            if ((getRecord() instanceof Record))
               ((Record)getRecord()).validateValue(column, value);
            else
                column.validateValue(value);
        }

        @Override
        public boolean isRequired()
        {
            return isValueRequired();
        }

        @Override
        public boolean isModified()
        {
            return isValueModified();
        }

        @Override
        public boolean isDisabled()
        {
            return TagEncodingHelper.this.isDisabled();
        }
        
        @Override
        public DisabledType getDisabled()
        {            
            return TagEncodingHelper.this.getDisabled();
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
            // only when needed
            return TagEncodingHelper.this.detectError(FacesContext.getCurrentInstance());
        }

        @Override
        public Object getAttribute(String name)
        {
            return getTagAttributeValue(name);
        }
        
        @Override
        public Object getAttributeEx(String name)
        {
            return getTagAttributeValueEx(name, InputControl.CSS_STYLE_CLASS.equals(name));
        }
    }

    // Logger
    private static final Logger log = LoggerFactory.getLogger(TagEncodingHelper.class);

    public static final String ORIGINAL_COMPONENT_ID  = "ORIGINAL_COMPONENT_ID"; 

    public static final String COLATTR_TOOLTIP        = "TOOLTIP";          // Column tooltip
    public static final String COLATTR_ABBR_TITLE     = "ABBR_TITLE";       // Column title for abbreviations
    
    protected final UIOutput    component;
    protected final String      cssStyleClass;
    protected Column            column                = null;
    protected Object            record                = null;
    protected RecordTag         recordTag             = null;
    protected UIData            uiDataTag             = null;
    protected FormGridTag       formGridTag           = null;
    protected InputControl      control               = null;
    protected TextResolver      textResolver          = null;
    protected byte              hasValueExpr          = -1;
    protected byte              insideUIData          = -1;

    // temporary
    protected byte              readOnly              = -1;
    protected byte              valueRequired         = -1;
    protected boolean           optionsDetected       = false;
    protected Options           options               = null;
    protected ValueInfo         valueInfo             = null;
    protected boolean           skipValidation        = false;
    protected Object            mostRecentValue       = null;

    /**
     * Constructor
     * @param component the UIComponent
     * @param cssStyleClass the basic cssStyleClass
     */
    protected TagEncodingHelper(UIOutput component, String cssStyleClass)
    {
        this.component = component;
        this.cssStyleClass = cssStyleClass;
    }

    /**
     * Called from UIComponent.encodeBegin()
     */
    public void encodeBegin()
    {
        if (component instanceof UIInput)
        {   // has local value?
            if (((UIInput)component).isLocalValueSet())
            {   /* clear local value */
                if (log.isDebugEnabled())
                    log.debug("clearing local value for {}. value is {}.", getColumnName(), ((UIInput)component).getLocalValue());
                // reset
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
        // check record
        checkRecord();
        // Reset cache
        reset();
    }

    /**
     * Prepares the control for decoding, validating and updating
     */
    public void prepareData()
    {
        checkRecord();
    }
    
    public static final char PH_COLUMN_NAME  = '@';  // placeholder for column name
    public static final char PH_COLUMN_FULL  = '$';  // placeholder for column full name including table
    public static final char PH_COLUMN_SMART = '*';  // placeholder for column name smart mode
    public static final char[] ALLOWED_COLUMN_PH = new char[] { PH_COLUMN_NAME, PH_COLUMN_FULL, PH_COLUMN_SMART };
    public static final Set<String> SMART_COLUMN_NAME_SET;
    static {
        SMART_COLUMN_NAME_SET = new HashSet<String>();
        SMART_COLUMN_NAME_SET.add("ID");
        SMART_COLUMN_NAME_SET.add("NAME");
        SMART_COLUMN_NAME_SET.add("STATUS");
    }

    public String completeInputTagId(String id)
    {
        // EmptyString or AT
        if (StringUtils.isEmpty(id))
            id = "*"; // Smart
        else if (id.startsWith(FACES_ID_PREFIX))
            return id; // Faces-Auto-ID
        // replace placeholder
        int idx;
        String name;
        if ((idx=id.indexOf(PH_COLUMN_NAME))>=0)
        {   // column name only
            name = getColumnName();
        }
        else if ((idx=id.indexOf(PH_COLUMN_SMART))>=0)
        {   // column name only
            name = getColumnSmartName();
        }
        else if ((idx=id.indexOf(PH_COLUMN_FULL))>=0) 
        {   // column full name including table
            name= getColumnFullName();
        }
        else 
        {   // No placeholder
            return id;
        }
        // done 
        id = (id.length()>1 ? StringUtils.concat(id.substring(0, idx), name, id.substring(idx+1)) : name); 
        return id;
    }
    
    public InputControl getInputControl()
    {
        // Already detected?
        if (this.control!=null)
            return this.control; 
        // Create
        if (getColumn() == null)
        	throw new NotSupportedException(this, "getInputControl");
        // Get Control from column
        String controlType = getTagAttributeString("controlType");
        if (controlType==null)
        {   controlType = column.getControlType();
            // Override standard "text" control type
            if (TextInputControl.NAME.equalsIgnoreCase(controlType))
            {   Object attr = getTagAttributeValue("options");
                if (attr != null && (attr instanceof Options) && !((Options)attr).isEmpty())
                    controlType = SelectInputControl.NAME; // Override to "select" control
            }
        }
        // detect Control
        control = detectInputControl(controlType, column.getDataType());
        // check record
        return control;
    }

    protected InputControl detectInputControl(String controlType, DataType dataType)
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
            if (getValueOptions()!=null)
                controlType = SelectInputControl.NAME;
            else
            {   // get from data type
                controlType = WebApplication.getInstance().getDefaultControlType(dataType);
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
                    if ((rec instanceof DBRecordBase) && (this.record instanceof DBRecordBase))
                    {   // a database record change
                        String keyOld = StringUtils.toString(((DBRecordBase)this.record).getKey());
                        String keyNew = StringUtils.toString(((DBRecordBase)rec).getKey());
                        String rowSet =((DBRecordBase)rec).getRowSet().getName();
                        log.trace("Changing {} record of rowset {} from {} to {}", component.getClass().getSimpleName() , rowSet, keyOld, keyNew);
                    }
                    else
                    {   // probably a bean change
                        log.trace("Changing {} record of class {}", component.getClass().getSimpleName(), rec.getClass().getName());
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
            // Invalidate if inside UIData
            else if (isInsideUIData())
            {   // reset record
                setRecord(null);
            }
        }
    }

    public InputControl.ValueInfo getValueInfo(FacesContext ctx)
    {
        if (this.valueInfo==null)
            this.valueInfo = new ValueInfoImpl(getColumn(), getTextResolver(ctx)); 
        return this.valueInfo;
    }

    public InputInfo getInputInfo(FacesContext ctx)
    {
        if (!(this.valueInfo instanceof InputInfo))
        {   // Skip validate
            this.skipValidation = FacesUtils.isSkipInputValidation(ctx);
            // create
            this.valueInfo = new InputInfoImpl(getColumn(), getTextResolver(ctx));
        }
        return (InputInfo)this.valueInfo;
    }

    public boolean isSkipValidation()
    {
        return skipValidation;
    }

    public boolean hasColumn()
    {
        if (column == null)
            setColumn(findColumn());
        // has column?
        return (column != null);
    }

    public Column getColumn()
    {
        if (hasColumn())
            return this.column;
        else
            throw new InvalidArgumentException("column", column);
    }
    
    public String getColumnName()
    {
        // don't use hasColumn() or getColumn() here!
        if (column==null)
            setColumn(findColumn()); 
        return (column!=null ? column.getName() : StringUtils.NULL);
    }
    
    public String getColumnFullName()
    {
        if (column==null)
            setColumn(findColumn());
        if (column==null)
            return StringUtils.NULL;
        // Find Entity
        EntityType entity = column.getEntityType();
        if (entity!=null)
            return StringUtils.concat(entity.getEntityName(), "_", column.getName());
        // No Entity
        return column.getName();
    }
    
    public String getColumnSmartName()
    {
        String name = getColumnName();
        if (SMART_COLUMN_NAME_SET.contains(name))
            name= getColumnFullName();
        return name;
    }

    public void setColumn(Column column)
    {
        this.column = column;
    }

    public Object getRecord()
    {
        if (record == null)
        {   // Find a record
            Object found=findRecord();
            if (found!=null)
                setRecord(found);
        }
        return record;
    }

    public void setRecord(Object record)
    {
        if (this.record!=null)
            reset();
        // set new record
        this.record = record;
    }
    
    public void reset()
    {
        this.readOnly        = -1;
        this.valueRequired   = -1;
        this.optionsDetected = false;
        this.options         = null;
        this.mostRecentValue = null;
        // Value Info
        if (this.valueInfo!=null) 
           ((ValueInfoImpl)this.valueInfo).reset();
    }
    
    public Object findRecordComponent()
    {
        // already present?
        if (this.recordTag != null)
            return this.recordTag.getRecord();
        if (this.uiDataTag != null)
        {   // check row available
            if (this.uiDataTag.isRowAvailable())
                return this.uiDataTag.getRowData();
            // row not available (possibly deleted)
            return null;
        }
        // walk upwards the parent component tree and return the first record component found (if any)
        UIComponent parent = component;
        while ((parent = parent.getParent()) != null)
        {
            if (parent instanceof RecordTag)
            {
                this.recordTag = (RecordTag) parent;
                // Don't set insideUIData to 0 here!
                return this.recordTag.getRecord();
            }
            if (parent instanceof UIData)
            {
                this.uiDataTag = (UIData)parent;
                this.insideUIData = (byte)1;
                return (this.uiDataTag.isRowAvailable() ? this.uiDataTag.getRowData() : null);
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
            {   // check record for validity
                if ((record instanceof Record) && !((Record)record).isValid())
                    return null;
                // valid
                ColumnExpr col = unwrapColumnExpr(getColumn());
                mostRecentValue = ((RecordData) record).get(col);
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
            {   // evaluate the value   
                Object value = component.getValue();
                return value;
            }
            else
            {   // Return the local value or the ValueExpression if any
                Object value = component.getLocalValue();
                if (value!=null && (component instanceof UIInput) && !((UIInput)component).isLocalValueSet())
                    value= null; /* should never come here! */
                if (value==null)
                    value = getValueExpression();
                // Return the local value or the ValueExpression if any
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
                    Object currentValue = ((Record) record).get(column);
                    if (!ObjectUtils.compareEqual(currentValue, mostRecentValue))
                    {   // Value has been changed by someone else!
                        log.info("Concurrent data change for column {}. Current Value is \"{}\". Ignoring new value \"{}\"", column.getName(), currentValue, value);
                        return;
                    }
                }
                // check whether to skip validation
                boolean reenableValidation = false;
                if (skipValidation && (record instanceof DBRecordBase))
                {   // Ignore read only values
                    if (this.isReadOnly())
                        return;
                    /* Why?
                    if (ObjectUtils.isEmpty(value) && ((Record) this.record).isFieldRequired(column))
                        return; // Cannot set required value to null
                    */    
                    // Disable Validation
                    reenableValidation = ((DBRecordBase)record).isValidateFieldValues();
                    if (reenableValidation)
                        ((DBRecordBase)record).setValidateFieldValues(false);
                    // Validation skipped for
                    if (log.isDebugEnabled())
                        log.debug("Input Validation skipped for {}.", column.getName());
                }
                // Now, set the value
                try {
                    ((Record) record).set(column, value);
                    mostRecentValue = value;
                } finally {
                    // re-enable validation
                    if (reenableValidation)
                        ((DBRecordBase)record).setValidateFieldValues(true);
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
        {   // Set Value attribute
            ValueExpression ve = getValueExpression();
            if (ve == null)
                throw new PropertyReadOnlyException("value");
            // set value
            FacesContext ctx = FacesContext.getCurrentInstance();
            ve.setValue(ctx.getELContext(), value);
        }
    }

    public boolean isRenderValueComponent()
    {
        return isReadOnly();
    }

    public boolean isRecordReadOnly()
    {
        return (getRecordReadOnly()==Boolean.TRUE);
    }
    
    public Boolean getRecordReadOnly()
    {
        // Do we have a record?
        if (getRecord() instanceof RecordData)
        {   // Only a RecordData?
            if (!(record instanceof Record))
                return true;
        }
        else if (record==null && !hasValueExpression())
        {   // No Value expression given
            return true;
        }
        // check attribute
        Object val = getTagAttributeValue("readonly");
        if (!ObjectUtils.isEmpty(val))
        {   // override
            return ObjectUtils.getBoolean(val);
        }
        // if value expression, don't check further
        if (hasValueExpression()) 
        {   // check readonly
            ValueExpression ve = getValueExpression();
            return (ve!=null ? ve.isReadOnly(FacesContext.getCurrentInstance().getELContext()) : null);
        }
        // check record component
        if (recordTag != null && recordTag.isReadOnly())
            return true;
        // Do we have a record?
        if ((record instanceof Record) && ((Record)record).isReadOnly())
            return true;
        // not defined
        return null;
    }

    public boolean isVisible()
    {
        // check attribute
        Object val = getTagAttributeValue("visible");
        if (!ObjectUtils.isEmpty(val))
        {   // override
            return ObjectUtils.getBoolean(val);
        }
        // reset record
        if (this.record!=null)
        {   // Check attribute
            Object recordTagValue = getTagAttributeValue("record");
            if ((recordTagValue instanceof DBRecordBase) && this.record!=recordTagValue)
            {   // shoud not come here
                log.warn("Record in call to IsVisible has unexpectedly changed!");
                this.record=null;
            }
        }
        // Check Record
        if ((getRecord() instanceof Record))
        {   // Ask Record
            Record r = (Record) record;
            return r.isValid() && r.isFieldVisible(getColumn());
        }
        // column
        return true;
    }

    public final boolean isReadOnly()
    {
        if (this.readOnly<0)
            this.readOnly=(detectReadOnly() ? (byte)1 : (byte)0);
        return (readOnly>0);
    }
    
    protected boolean detectReadOnly()
    {
        // component 
        if (!(component instanceof UIInput))
        {   // from LabelTag ?
            if (!(component instanceof LabelTag))
                log.warn("Component for {} is not of type UIInput but {}", getColumn().getName(), component.getClass().getName());
            return true;
        }
        // Check Record
        Boolean readOnly = getRecordReadOnly();
        if (readOnly!=null)
            return readOnly;
        // Check Record
        if ((getRecord() instanceof Record))
        { // Ask Record
            Record r = (Record) record;
            return r.isValid() && r.isFieldReadOnly(getColumn());
        }
        // column
        return getColumn().isReadOnly();
    }
    
    public final boolean isValueRequired()
    {
        if (this.valueRequired<0)
            this.valueRequired=(detectValueRequired() ? (byte)1 : (byte)0);
        return (valueRequired>0);
    }

    protected boolean detectValueRequired()
    {
        // See if the tag is required (don't use the "required" attribute or tag.isRequired()!)
        Object mandatory = getTagAttributeValue("mandatory");
        if (mandatory!=null)
            return ObjectUtils.getBoolean(mandatory);
        // Check Read-Only first
        if (isReadOnly())
            return false;
        // Check Value Attribute
        if (hasValueExpression())
            return false;
        // Check Record
        if ((getRecord() instanceof Record))
        {   // Ask Record
            Record r = (Record) record;
            return r.isValid() && r.isFieldRequired(getColumn());
        }
        else if (this.recordTag!=null)
        {   // for a normal bean, check record Tag
            mandatory = recordTag.getAttributes().get("mandatory");
            if (mandatory!=null)
                return ObjectUtils.getBoolean(mandatory);
        }
        // Required
        return getColumn().isRequired();
    }

    public final boolean isDisabled()
    {
        DisabledType disabled = TagEncodingHelper.this.getDisabled(); 
        return (disabled!=null && disabled!=DisabledType.NO);
    }
    
    public DisabledType getDisabled()
    {
        // component 
        if (!(component instanceof UIInput))
        {   log.warn("Component is not of type UIInput");
            return DisabledType.READONLY;
        }
        // Check attribute
        Object dis = getTagAttributeValueEx("disabled", false);
        if (ObjectUtils.isEmpty(dis))
            return null; // not provided!
        // direct
        if (dis instanceof DisabledType)
            return (DisabledType)dis;
        // readonly
        if (String.valueOf(dis).equalsIgnoreCase("readonly"))
            return DisabledType.READONLY;
        // other
        return (ObjectUtils.getBoolean(dis) ? DisabledType.DISABLED : DisabledType.NO);
    }
    
    public boolean isValueModified()
    {
        Object modified = getTagAttributeValue("modified");
        if (modified!=null)
            return ObjectUtils.getBoolean(modified);
        // Check Record
        if ((getRecord() instanceof Record))
        {   // Ask Record
            Record r = (Record) record;
            return r.isValid() && r.wasModified(getColumn());
        }
        // not detectable
        return false;
    }

    public boolean validateNullValue()
    {
        if (isValueRequired())
            throw new FieldNotNullException(column);
        // OK, null allowed
        return true;
    }
    
    public boolean beginValidateValue(FacesContext ctx, Object value)
    {
        if (UIInput.isEmpty(value) && FacesUtils.getWebApplication().isPartialSubmit(ctx))
        {   // don't validate empty values
            return false;
        }
        // continue
        return true;
    }
    
    public boolean beginUpdateModel(FacesContext ctx, Object value)
    {
        if (UIInput.isEmpty(value) && FacesUtils.getWebApplication().isPartialSubmit(ctx))
        {   // Partial submit for empty value
            String partialCompId = FacesUtils.getWebApplication().getPartialSubmitComponentId(ctx);
            if (log.isInfoEnabled())
                log.info("Performing UpdateModel for PartialSubmit on column {} from {}", getColumnFullName(), partialCompId);
        }
        /*
        // do it ourselves
        InputInfo ii = getInputInfo(ctx);
        ii.setValue(value);
        return false;
        */
        // continue
        return true;
    }

    /* Helpers */
    protected Column findColumn()
    {
        // if parent is a record tag, get the record from there
        Object col = getTagAttributeValue("column");
        if (col instanceof Column)
        {   // cast to column
            return (Column) col;
        }
        if (col instanceof ColumnExpr)
        {   // check component
            if ((component instanceof InputTag || component instanceof ControlTag))
            {   log.warn("ColumnExpresion cannot be used with InputTag or ControlTag");
                throw new InvalidPropertyException("column", column);
            }
            // wrap expression
            return createColumnExprWrapper((ColumnExpr) col);
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
            DBDatabase db = DBDatabase.findByIdentifier(name.substring(0,dbix));
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
        // No column!
        if (log.isDebugEnabled() && !(component instanceof LinkTag))
            log.warn("No Column provided for value tag!");
        return null;
    }

    /**
     * Checks whether the value attribute contains a column reference and returns it
     * @return the column
     */
    protected Column findColumnFromValue()
    {   // Try value
        Object col = component.getValue();
        // Column supplied?
        if (col instanceof Column)
        {
            return (Column) col;
        }
        // Column expression supplied?
        if (col instanceof ColumnExpr)
        { // Use source column instead 
            Column source = ((ColumnExpr) col).getUpdateColumn();
            if (source != null)
                return source;
            // No source column? --> wrap 
            return createColumnExprWrapper((ColumnExpr) col);
        }
        return null;
    }
    
    protected Column createColumnExprWrapper(ColumnExpr colExpr)
    {
        return new ColumnExprWrapper(colExpr);
    }
    
    protected ColumnExpr unwrapColumnExpr(Column col)
    {
        if (col instanceof ColumnExprWrapper)
            return ((ColumnExprWrapper) col).getExpr();
        return col;
    }
    
    protected Column unwrapColumn(Column col)
    {
        if (col instanceof ColumnExprWrapper)
            return ((ColumnExprWrapper) col).getUpdateColumn();
        return col;
    }

    protected Object findRecord()
    {
        Object rec = getTagAttributeValue("record");
        if (rec != null)
            return rec;
        // Value expression
        if (hasValueExpression())
        {   // Value expression is provided instead. 
            return null;
        }       
        // if parent is a record tag, get the record from there
        rec = findRecordComponent();
        if (rec==null)
        {   // not supplied
            if ((component instanceof ControlTag) && !((ControlTag)component).isCustomInput())
                log.warn("No record supplied for {} and column {}.", component.getClass().getSimpleName(), getColumnName()); 
        }
        return rec;
    }
    
    /*
     * Old code
     * Replaced 2025-01-08 with EMPIREDB-453
     *
    protected boolean hasValueExpression()
    {
        // Find expression
        if (this.hasValueExpr<0)
        {   // Find expression
            ValueExpression ve = findValueExpression("value");
            if (ve != null)
            {   // We have a ValueExpression!
                // Now unwrap for Facelet-Tags to work
                String originalExpr = (log.isDebugEnabled() ? ve.getExpressionString() : null);
                ve = FacesUtils.getFacesImplementation().unwrapValueExpression(ve);
                if (originalExpr!=null)
                {   // log result
                    if (ve!=null && !originalExpr.equals(ve.getExpressionString()))
                        log.debug("ValueExpression \"{}\" has been resolved to \"{}\" from class {}", originalExpr, ve.getExpressionString(), ve.getClass().getName());
                    else if (ve==null)
                        log.debug("ValueExpression \"{}\" has been resolved to NULL", originalExpr);
                }
            }
            // store result to avoid multiple detection 
            hasValueExpr = ((ve!=null) ? (byte)1 : (byte)0);
        }
        return (hasValueExpr>0);
    }
    
    // composite component expression
    protected static final String CC_ATTR_EXPR = "#{cc.attrs.";
    
    @SuppressWarnings("unchecked")
    protected ValueExpression findValueExpression(String attribute)
    {
        // Check for expression
        ValueExpression ve = component.getValueExpression(attribute);
        if (ve == null)
            return null;
        // Check for composite component expression
        UIComponent parent = component;
        String expr = ve.getExpressionString();
        while (expr.startsWith(CC_ATTR_EXPR))
        {
            // Unwrap
            if (ve instanceof FacesWrapper<?>)
                ve = ((FacesWrapper<ValueExpression>)ve).getWrapped();
            // find parent
            UIComponent valueParent = FacesUtils.getFacesImplementation().getValueParentComponent(ve);
            if (valueParent!=null)
            {   // use the value parent
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
            {   // not found
                return null;
            }
            // get new expression String
            ve = next;
            expr = ve.getExpressionString();
        }
        // found
        return ve;
    }
    */
    
    protected ValueExpression getValueExpression()
    {
        return component.getValueExpression("value"); 
    }
    
    protected boolean hasValueExpression()
    {
        // Find expression
        if (this.hasValueExpr<0)
        {   // Find top expression
            ValueExpression ve = findTopValueExpression("value");            
            /* 
             * Alternatively, check ValueReference
             *
            ValueExpression ve = this.component.getValueExpression("value");
            while (ve!=null)
            {   // check ValueReference
                ValueReference vr = ve.getValueReference(FacesContext.getCurrentInstance().getELContext());
                if (vr==null)
                {   // No value reference 
                    ve = null;
                }
                else if (vr.getBase().getClass().getName().endsWith("CompositeComponentAttributesMapWrapper"))
                {   // CompositeComponentELResolver
                    ve = (ValueExpression)ClassUtils.invokeMethod(vr.getBase().getClass(), vr.getBase(), "getExpression", new Class<?>[] { String.class }, new Object[] { vr.getProperty() }, true);
                }
                else
                {   // found
                    if (log.isDebugEnabled())
                        log.debug("ValueReference of of type {}", vr.getBase().getClass().getName());
                    break;
                }
            }
            */
            // store result to avoid multiple detection 
            hasValueExpr = ((ve!=null) ? (byte)1 : (byte)0);
        }
        return (hasValueExpr>0);
    }
    
    // composite component expression
    protected static final String CC_ATTR_EXPR = "#{cc.attrs.";
    protected static String TAGLIB_ATTR_PREFIX = "${";
    
    protected ValueExpression findTopValueExpression(String attribute)
    {
        // Check for expression
        ValueExpression ve = this.component.getValueExpression(attribute);
        UIComponent parent = this.component;
        while (ve!=null)
        {
            String expr = ve.getExpressionString();
            if (expr.startsWith(CC_ATTR_EXPR))
            {   // Unwrap composite component attribute
                parent = UIComponent.getCompositeComponentParent(parent);
                if (parent == null)
                {   log.error("No CompositeComponentParent for {} expression was {}", getColumnName(), expr);
                    return null; // parent not found
                }
                // check expression
                int end = expr.indexOf('}');
                String attrib = expr.substring(CC_ATTR_EXPR.length(), end);
                if (attrib.indexOf('.')>0)
                    break; // do not investigate any further
                // next expression
                ve = parent.getValueExpression(attrib);
            }
            else
            {   // Unwrap for Facelet-Tags to work
                ValueExpression next = ValueExpressionUnwrapper.getInstance().unwrap(ve, TAGLIB_ATTR_PREFIX);
                if (ve==next)
                    break; // do not investigate any further
                // log result
                ve = next;
                // debug
                if (log.isDebugEnabled())
                {   // log result
                    if (ve!=null && !expr.equals(ve.getExpressionString()))
                        log.debug("ValueExpression \"{}\" has been resolved to \"{}\" from class {}", expr, ve.getExpressionString(), ve.getClass().getName());
                    else if (ve==null)
                        log.debug("ValueExpression \"{}\" has been resolved to NULL", expr);
                }
            }
        }
        // top expression or null
        return ve;
    }

    protected Object getBeanPropertyValue(Object bean, String property)
    {
        return BeanPropertyUtils.getProperty(bean, property);
    }

    protected void setBeanPropertyValue(Object bean, String property, Object value)
    {
        // set Property 
        if (ObjectUtils.isEmpty(value))
            value = null;
        // Set Property Value
        if (!BeanPropertyUtils.setProperty(bean, property, value))
        {   // Property has not been set
            if (BeanPropertyUtils.hasProperty(bean, property, true)==0)
                throw new PropertyReadOnlyException(property);
            else
                throw new ItemNotFoundException(property);
        }
    }
    
    protected final Options getValueOptions()
    {
        if (this.optionsDetected==false)
        {   // detect Options
            this.options = detectValueOptions();
            this.optionsDetected = true;
        }
        return this.options;
    }
    
    protected Options detectValueOptions()
    {
        // null value
        Object attr = getTagAttributeValue("options");
        if (attr != null && (attr instanceof Options))
            return ((Options) attr);
        if (hasColumn())
        {   // Do we have a record?
            Object rec = (this.record!=null ? this.record : findRecord());
            if ((rec instanceof Record) && ((Record)rec).isValid()) 
                return ((Record)rec).getFieldOptions(unwrapColumn(column));
            // get From Column
            return column.getOptions();
        }
        // not available
        return null;
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
                {   // a record
                    value = ((RecordData) record).get(ttc);
                    // convert to enum
                    Class<Enum<?>> enumType = ttc.getEnumType();
                    if (enumType!=null && !(value instanceof Enum))
                        value = ObjectUtils.getEnum(enumType, value);
                }
                else
                { // a normal bean
                    String prop = ttc.getBeanPropertyName();
                    value = getBeanPropertyValue(record, prop);
                }
                // resolve options
                Options options = (value==null || value instanceof Enum) ? null : ttc.getOptions();
                if (options!=null && !hasFormat("notitlelookup"))
                    value = options.get(value);
                // convert to display text
                return getDisplayText(ttc, value);
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
        String text = null;
        Options options;
        if (!hasFormat("notitlelookup") && (options=getValueOptions())!=null)
        { // Lookup the title
            String optValue = options.get(value);
            text = getDisplayText(column, optValue);
        }
        else
        {   // resolveText
            text = getDisplayText(column, value);
        }
        // Check for template
        if (valIndex >= 0 && text!=null)
            text = StringUtils.replace(templ, "{}", text);
        return text;
    }

    public String getLabelTooltip(Column column)
    {
        String title = getTagAttributeString("title");
        if (title == null)
            title = getColumnAttributeString(Column.COLATTR_TOOLTIP);
        if (title != null)
            return getDisplayText(column, title);
        // Check for short form
        if (hasFormat("short") && !ObjectUtils.isEmpty(getColumnAttribute(COLATTR_ABBR_TITLE)))
            return getDisplayText(column, column.getTitle());
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
    
    public String getDisplayText(Column column, Object value)
    {
        if (ObjectUtils.isEmpty(value))
            return null;
        // number
        if (value instanceof Number) {
            boolean isDecimal = (column!=null && (column.getDataType()==DataType.DECIMAL || column.getDataType()==DataType.FLOAT));
            if (isDecimal) {
                NumberFormat nf = NumberFormat.getIntegerInstance(getLocale());
                return nf.format(value);
            }
            else
                return String.valueOf(value);
        }
        // format date 
        if (value instanceof Date) {
            String date = DateUtils.formatDate((Date)value, getLocale());
            if (column!=null && column.getDataType()!=DataType.DATE)
                return StringUtils.concat(date, " ", DateUtils.formatTime((Date)value, getLocale(), (column.getDataType()==DataType.TIMESTAMP)));
            else
                return date;
        }
        if (value instanceof LocalDate)
            return DateUtils.formatDate((LocalDate)value, getLocale());
        if (value instanceof LocalDateTime) {
            if (column!=null && column.getDataType()!=DataType.DATE)
                return DateUtils.formatDateTime((LocalDateTime)value, getLocale(), (column.getDataType()==DataType.TIMESTAMP));
            else
                return DateUtils.formatDate((LocalDateTime)value, getLocale());
        }
        // Resolve text
        if (textResolver==null)
            getTextResolver(FacesContext.getCurrentInstance());
        return textResolver.resolveText(value.toString());
    }

    public final String getDisplayText(Object value)
    {
        return getDisplayText(this.column, value);
    }
    
    public TextResolver getTextResolver(FacesContext context)
    {
        if (textResolver==null)
            textResolver=WebApplication.getInstance().getTextResolver(context);
        return textResolver;
    }
    
    public Locale getLocale()
    {
        if (textResolver==null)
            getTextResolver(FacesContext.getCurrentInstance());
        return textResolver.getLocale();
    }
    
    /* ********************** Error messages ********************** */
    
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
    
    public FacesMessage getFieldValueErrorMessage(FacesContext context, Exception e, Object value)
    {
        if (!(e instanceof EmpireException))
            e = new FieldIllegalValueException(getColumn(), StringUtils.valueOf(value), e);
        // create faces message
        String msgText = getTextResolver(context).getExceptionMessage(e);
        return new FacesMessage(FacesMessage.SEVERITY_ERROR, msgText, msgText);
    }
    
    public void addFieldValueErrorMessage(FacesContext context, Exception e, Object value)
    {
        FacesMessage msg = getFieldValueErrorMessage(context, e, value);
        context.addMessage(component.getClientId(), msg);
    }
    
    /* ********************** FormGridTag ********************** */

    protected FormGridTag getFormGrid()
    {
        if (this.formGridTag!=null)
            return formGridTag;
        // walk upwards the parent component tree and return the first record component found (if any)
        UIComponent parent = component;
        while ((parent = parent.getParent()) != null)
        {
            if (parent instanceof FormGridTag)
            {   // found
                this.formGridTag = (FormGridTag) parent;
                return this.formGridTag;
            }
        }
        return null;
    }
    
    public ControlRenderInfo getControlRenderInfo()
    {
        FormGridTag formGrid = getFormGrid();
        return (formGrid!=null) ? formGrid.getControlRenderInfo() : null;  
    }
    
    public String getControlExtraLabelWrapperStyle()
    {
        if (!ControlRenderInfo.isRenderExtraWrapperStyles())
            return null;
        // style Class
        String labelClass = getTagAttributeStringEx("labelClass", true);
        return labelClass;
    }
    
    public String getControlExtraInputWrapperStyle()
    {
        if (!ControlRenderInfo.isRenderExtraWrapperStyles())
            return null;
        /*
         * Discarded due to missing use-case 
         * 
        // input Wrapper Class
        String inputClass = (isControlTagElementValid() ? getTagAttributeStringEx(InputControl.CSS_STYLE_CLASS, true) : getTagAttributeString("inputClass")); 
        // append input state
        if (isRenderValueComponent())
            inputClass = (inputClass!=null ? StringUtils.concat(inputClass, " ", TagStyleClass.INPUT_DIS.get()) : TagStyleClass.INPUT_DIS.get());
        // done
        return inputClass;
        */
        return null;
    }
    
    /**
     * Returns whether a control element is rendered
     * Use to detect legacy behavior with no separate control element 
     * and only two &lt;td&gt; for label and input wrapper
     * @return true if a control element is rendered or false in legacy case 
     */
    protected boolean isControlTagElementValid()
    {
        ControlRenderInfo cri = getControlRenderInfo();
        return (cri!=null && cri.CONTROL_TAG!=null);
    }

    /* ********************** Label ********************** */

    protected String getLabelValue(Column column, boolean colon)
    {
        String label = getTagAttributeString("label");
        if (label==null)
        {   // Check for short form    
            if (hasFormat("short"))
            {
                label = getColumnAttributeString(COLATTR_ABBR_TITLE);
                if (label==null)
                    log.warn("No Abbreviation available for column {}. Using normal title.", column.getName());
            }
            // Use normal title
            if (label==null)
                label=column.getTitle();
            // translate
            label = getDisplayText(column, label);
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
    
    public HtmlOutputLabel createLabelComponent(FacesContext context, String forInput, StyleClass styleClass, String style, boolean colon)
    {
        // forInput provided
        if (StringUtils.isNotEmpty(forInput))
        {   // find the component
            if (!forInput.equals("*"))
            {   // Set Label input Id
                UIComponent input = FacesUtils.getWebApplication().findComponent(context, forInput, component);
                if (input!=null && (input instanceof InputTag))
                {   // Copy from InputTag
                    InputTag inputTag = ((InputTag)input);
                    setColumn(inputTag.getInputColumn());
                    this.readOnly = (inputTag.isInputReadOnly() ? (byte)1 : (byte)0);
                    this.valueRequired = (inputTag.isInputRequired() ? (byte)1 : (byte)0);
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
                Object val = getTagAttributeValueEx("readOnly", false);
                if (val!=null)
                    this.readOnly = (ObjectUtils.getBoolean(val) ? (byte)1 : (byte)0);
            }
        }

        // create label now
        HtmlOutputLabel label = InputControlManager.createComponent(context, InputControlManager.getLabelComponentClass());

        // set label text
        String labelText = getLabelValue(getColumn(), colon);
        if (StringUtils.isEmpty(labelText))
            label.setRendered(false);
        else
            label.setValue(labelText);

        // set styleClass
        String labelClass = null;
        if (!ControlRenderInfo.isRenderExtraWrapperStyles())
            labelClass = getTagAttributeStringEx("labelClass", true);
        if (labelClass!=null)
            styleClass.add(labelClass);
        if (styleClass.isNotEmpty())
            label.setStyleClass(completeLabelStyleClass(styleClass.build(), isValueRequired()));
        
        // for 
        if (StringUtils.isNotEmpty(forInput) && !isReadOnly())
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
        if (isValueRequired() && InputControlManager.isShowLabelRequiredMark())
            addRequiredMark(label);

        return label;
    }
    
    public void updateLabelComponent(FacesContext context, HtmlOutputLabel label, String forInput)
    {
        // Find Input Control (only if forInput Attribute has been set!)
        InputTag inputTag = null;
        // forInput provided
        if (StringUtils.isNotEmpty(forInput))
        {   // find the component
            if (!forInput.equals("*"))
            {   // Set Label input Id
                UIComponent input = FacesUtils.getWebApplication().findComponent(context, forInput, component);
                if (input!=null && (input instanceof InputTag))
                {   // Set Input Tag
                    inputTag = ((InputTag)input);
                }
            }
            else if (component instanceof LabelTag)
            {   // update readOnly
                Object val = getTagAttributeValueEx("readOnly", false);
                if (val!=null)
                    this.readOnly = (ObjectUtils.getBoolean(val) ? (byte)1 : (byte)0);
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

    protected String completeLabelStyleClass(String styleClasses, boolean required)
    {
        styleClasses = StyleClass.addOrRemove(styleClasses, TagStyleClass.INPUT_REQ.get(), required);
        return styleClasses;
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

    public static final String CSS_DATA_TYPE_NONE     = null;
    public static final String CSS_DATA_TYPE_INT      = " eTypeInt";
    public static final String CSS_DATA_TYPE_NUMBER   = " eTypeNumber";
    public static final String CSS_DATA_TYPE_TEXT     = " eTypeText";
    public static final String CSS_DATA_TYPE_LONGTEXT = " eTypeLongText";
    public static final String CSS_DATA_TYPE_DATE     = " eTypeDate";
    public static final String CSS_DATA_TYPE_DATETIME = " eTypeDateTime";
    public static final String CSS_DATA_TYPE_BOOL     = " eTypeBool";

    /*
     * Removed with EMPIREDB-427 and replaced by 
     *   control.getCssStyleClass()
     */
    @Deprecated
    protected String getDataTypeClass(DataType type)
    {
        switch (type)
        {
            case AUTOINC:
            case INTEGER:
                return CSS_DATA_TYPE_INT;
            case DECIMAL:
                return (getValueOptions()!=null) ? CSS_DATA_TYPE_TEXT : CSS_DATA_TYPE_NUMBER;
            case FLOAT:
                return CSS_DATA_TYPE_NUMBER;
            case VARCHAR:
            case CHAR:
                return CSS_DATA_TYPE_TEXT;
            case DATE:
                return CSS_DATA_TYPE_DATE;
            case DATETIME:
            case TIMESTAMP:
                return CSS_DATA_TYPE_DATETIME;
            case BOOL:
                return CSS_DATA_TYPE_BOOL;
            case CLOB:
                return CSS_DATA_TYPE_LONGTEXT;
            default:
                return CSS_DATA_TYPE_NONE; // Not provided
        }
    }

    public StyleClass getSimpleStyleClass(String userStyle)
    {
        StyleClass styleClass = new StyleClass(cssStyleClass);
        return styleClass.add(userStyle);
    }

    public StyleClass getSimpleStyleClass()
    {
        String userStyle = getTagAttributeString(InputControl.CSS_STYLE_CLASS);
        return getSimpleStyleClass(userStyle);
    }

    public StyleClass getTagStyleClass(String typeClass, String userStyle)
    {
        StyleClass styleClass = new StyleClass(cssStyleClass);
        styleClass.add(typeClass, userStyle, getContextStyleClass());
        return styleClass;
    }

    public StyleClass getTagStyleClass(String typeClass)
    {
        String userStyle = getTagAttributeStringEx(InputControl.CSS_STYLE_CLASS, true);
        return getTagStyleClass(typeClass, userStyle);
    }

    public StyleClass getTagStyleClass()
    {
        String typeClass = (this.control!=null ? this.control.getCssStyleClass() : null);
        return getTagStyleClass(typeClass);
    }
    
    public String getContextStyleClass()
    {
        if ((getRecord() instanceof TagContextInfo) && hasColumn())
        {
            return ((TagContextInfo)getRecord()).getContextStyleClass(getColumn());
        }
        return null;
    }

    /* ********************** UIData detection ********************** */
    
    protected boolean detectInsideUIData()
    {
        // detect
        for (UIComponent p = component.getParent(); p!=null; p=p.getParent())
        {   /* Don't do this:
             * Detect must return true if RecordTag is inside a UIData
            if (p instanceof RecordTag)
                return false;
            */
            // Check whether inside UIData
            if (p instanceof UIData) {
                return true;
            } else if ("facelets.ui.Repeat".equals(p.getRendererType())) {
                return true;
            }
        }
        return false;
    }

    public final boolean isInsideUIData()
    {
        if (component==null)
            return false;
        if (this.insideUIData<0)
            this.insideUIData=(detectInsideUIData() ? (byte)1 : (byte)0);
        return (this.insideUIData>0);
    }
    
    /* ********************** Component id ********************** */

    public static String buildComponentId(String s)
    {
        if (s==null || s.length()==0)
            return null;
        StringBuilder b = new StringBuilder(s.length());
        for (int i=0; i<s.length(); i++)
        {
            char c = s.charAt(i);
            if (c!='_' && !StringUtils.isCharBetween(c, 'A', 'Z') && !StringUtils.isCharBetween(c, 'a', 'z') && !StringUtils.isNumber(c))
                c='-';
            b.append(c);
        }
        return b.toString();
    }
    
    public static boolean hasComponentId(UIComponent component)
    {
        String id = component.getId();
        return (id!=null && id.length()>0 && !id.startsWith(FACES_ID_PREFIX));        
    }

    public boolean hasComponentId()
    {
        return hasComponentId(this.component);        
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
        {   // reset component-id
            String resetId = comp.getId();
            if (log.isInfoEnabled())
                log.info("Resetting Component-id inside UIData to {}", resetId);
            comp.setId(resetId);
        }
    }

    /* ********************** Attribute value ********************** */

    public Object getColumnAttribute(String name)
    {
        return (column!=null ? column.getAttribute(name) : null);
    }

    public String getColumnAttributeString(String name)
    {
        return StringUtils.nullIfEmpty(getColumnAttribute(name));
    }
    
    public Object getTagAttributeValue(String name)
    {
        Object value = component.getAttributes().get(name);
        /*
         * Removed with EMPIREDB-441 on 2024-10-12
         * ValueExpression expression is already checked internally by getAttributes()
         * 
        if (value==null)
        {   // try value expression
            ValueExpression ve = component.getValueExpression(name);
            if (ve!=null)
            {   // It's a value expression
                FacesContext ctx = FacesContext.getCurrentInstance();
                value = ve.getValue(ctx.getELContext());
            }
        }
        */
        return value;
    }
    
    public String getTagAttributeString(String name, String defValue)
    {
        Object value = getTagAttributeValue(name);
        String sval  = StringUtils.nullIfEmpty(value);
        return (sval==null ? defValue : sval);
    }

    public String getTagAttributeString(String name)
    {
        return getTagAttributeString(name, null);
    }

    public Object getTagAttributeValueEx(String name, boolean isCssStyleClass)
    {
        /* 
         * Special handling of ControlTag "styleClass": Use it for control element only not for input element(s)
         */
        boolean useControlTagOverride = (InputControl.CSS_STYLE_CLASS.equals(name) && (this.component instanceof ControlTag) && isControlTagElementValid());
        Object value = getTagAttributeValue((useControlTagOverride ? "inputClass" : name));

        // Special "styleClass" append column attribute unless leading '-'
        boolean append = false;
        if (isCssStyleClass && (value instanceof String) && ((String)value).length()>0)
        {   // if value starts with '!' then do not append but replace
            if (((String)value).charAt(0)=='!')
                value = ((String)value).substring(1); // remove leadng '!'
            else if (((String)value).equals("-"))
                return null; // omit, if value is '-'
            else
                append = true; // append (default)
        }
        
        // Get column attribute
        if ((value==null || append) && hasColumn())
        {   // Check Column
            Object colValue = getColumnAttribute(name);
            if (append) 
            {   // append styles
                if (ObjectUtils.isNotEmpty(colValue) && !colValue.equals(value))
                    value = StringUtils.concat(colValue.toString(), " ", value.toString());
            }
            else 
            {   // replace
                value = colValue;
            }
        }
        
        // Checks whether it's another column    
        if (value instanceof Column)
        {   // Special case: Value is a column
            Column col = ((Column)value);
            Object rec = getRecord();
            if (rec instanceof Record)
                return ((Record)rec).get(col);
            else if (rec!=null)
            {   // Get Value from a bean
                String property = col.getBeanPropertyName();
                try
                {   // Use Beanutils to get Property
                    return BeanPropertyUtils.getProperty(rec, property);
                }
                catch (Exception e)
                {   log.error("BeanUtils.getProperty failed for property {} on {} ", property, rec.getClass().getName(), e);
                    return null;
                }
            }    
            return null;
        }
        return value;
    }

    public String getTagAttributeStringEx(String name, boolean isCssStyleClass)
    {
        Object value = getTagAttributeValueEx(name, isCssStyleClass);
        return (value!=null) ? StringUtils.nullIfEmpty(value) : null;
    }
    
    /* ********************** Response write helper ********************** */
    
    public void writeComponentId(ResponseWriter writer, boolean renderAutoId)
        throws IOException
    {
        // render id
        if (renderAutoId || hasComponentId())
            writer.writeAttribute(InputControl.HTML_ATTR_ID, component.getClientId(), null);
    }
    
    public void writeComponentId(ResponseWriter writer)
        throws IOException
    {
        writeComponentId(writer, (component instanceof NamingContainer));
    }

    public void writeAttribute(ResponseWriter writer, String attribute, Object value)
        throws IOException
    {
        if (value != null && !ObjectUtils.isEmpty(value))
            writer.writeAttribute(attribute, value, null);
    }

    public void writeStyleClass(ResponseWriter writer, String... styleClasses)
        throws IOException
    {
        // Check if there is only one
        int i=0;
        String styleClass = null;
        for (; i<styleClasses.length; i++) {
            if (styleClasses[i]!=null) {
                if (styleClass!=null) {
                    break; // more than one!
                }
                styleClass = styleClasses[i];
            }
        }
        if (i<styleClasses.length)
            styleClass = StringUtils.arrayToString(styleClasses, StringUtils.SPACE, null);
        if (styleClass != null)
            writer.writeAttribute(InputControl.HTML_ATTR_CLASS, styleClass, null);
    }

    public void writeStyleClass(ResponseWriter writer)
        throws IOException
    {
        String userStyle = getTagAttributeStringEx(InputControl.CSS_STYLE_CLASS, true);
        writeStyleClass(writer, this.cssStyleClass, userStyle);
    }
    
    /**
     * Writes a wrapper tag for e:value and e:input
     * Hint: For e:control the input separator tag acts as the wrapper
     * @param context the faces context
     * @param renderId flag whether or not to render the components client id
     * @param renderValue flag whether to render and input wrapper (false) or a value wrapper (true)
     * @return the tag name of the wrapper tag
     * @throws IOException from ResponseWriter
     */
    public String writeWrapperTag(FacesContext context, boolean renderId, boolean renderValue)
        throws IOException
    {
        String wrapperClass = getTagAttributeStringEx("wrapperClass", true); 
        if (wrapperClass==null || wrapperClass.equals("-"))
            return null;
        if (wrapperClass.equals("*"))
            wrapperClass=null; // only use default style class
        // start element
        String tagName = InputControl.HTML_TAG_DIV;
        ResponseWriter writer = context.getResponseWriter();
        writer.startElement(tagName, this.component);
        // render id
        if (renderId)
            writeComponentId(writer);
        // style class
        String contextClass = (renderValue ? TagStyleClass.VALUE_WRAPPER.get() : TagStyleClass.INPUT_WRAPPER.get());
        writeStyleClass(writer, contextClass, wrapperClass);
        // return tagName
        return tagName;
    }
    
}
