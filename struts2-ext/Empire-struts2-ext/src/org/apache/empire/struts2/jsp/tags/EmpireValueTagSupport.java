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
package org.apache.empire.struts2.jsp.tags;

import org.apache.empire.commons.StringUtils;
import org.apache.empire.data.Column;
import org.apache.empire.data.ColumnExpr;
import org.apache.empire.data.Record;
import org.apache.empire.data.RecordData;

@SuppressWarnings("serial")
public abstract class EmpireValueTagSupport extends EmpireTagSupport
{
    public static final String RECORD_ATTRIBUTE    = "record";
    public static final String BEAN_ITEM_ATTRIBUTE = "bean";
    public static final String PARENT_PROPERTY_ATTRIBUTE = "parentPropertyName";
    
    // Value
    private ColumnExpr column;
    private Object value;
    private String field;
    private String property;
    private String parentProperty;
    private RecordData record;
    
    @Override
    protected void resetParams()
    {
        // Value
        column = null;
        value = null;
        field = null;
        property = null;
        parentProperty = null;
        record = null;
        // Reset
        super.resetParams();
    }
    
    protected boolean hasValue()
    {
        return (property!=null || field!=null || value!=null);
    }
    
    protected RecordData getRecordData()
    {
        if ( record==null )
        {   // Set Record from Page Context
            Object recObj = pageContext.getAttribute(RECORD_ATTRIBUTE);
            if (recObj instanceof RecordData)
                record = (RecordData)recObj;
        }
        return record;
    }

    protected Record getRecord()
    {
        if (getRecordData() instanceof Record)
        {   // Yes, it's a Record
            if (!((Record)record).isValid())
            {   // Invalid Record 
                log.warn("The record supplied is not valid!");
                return null; 
            }
            return ((Record)record);
        }
        // Not a record
        return null;
    }
    
    protected Object getBean()
    {
        return pageContext.getAttribute(BEAN_ITEM_ATTRIBUTE);        
    }
    
    protected ColumnExpr getColumnExpr()
    {
        if (column!=null)
            return column;
        if (value instanceof ColumnExpr)
            return ((ColumnExpr)value);
        if (field!=null)
        {   // If record is not set, then try to read it from page context
            if (getRecordData()!=null)
            {   // get Column from field
                return record.getColumnExpr(record.getFieldIndex(field));
            }
        }
        return null; 
    }
    
    protected Column getColumn()
    {
        ColumnExpr column = getColumnExpr();
        if (column==null)
            return null; 
        // Get Update Column
        return column.getSourceColumn();        
    }
    
    protected String getControlType()
    {
        // Detect control type and readOnly state
        Column column = getColumn();
        if (column==null)
        {   // log.debug("No Column supplied. Unable to detect control type. Using default.");
            return "text"; 
        }
        return column.getControlType();
    }
    
    protected String getTagName(String suppliedName)
    {
        if (StringUtils.isValid(suppliedName))
            return suppliedName;
        if (property != null)
            return getFullPropertyName(property);
        if (field != null)
            return getFullPropertyName(field);
        if (value instanceof ColumnExpr)
            return getColumnPropertyName((ColumnExpr)value);
        if (column!=null)
            return getColumnPropertyName(column);
        // Not Name provided 
        log.error("Cannot detect name from value.");
        return "";
    }
    
    protected Object getValue()
    {
        if (value==null)
        {   // Try property and field first
            if (property != null)
            {   // Value from Property
                return getStack().findValue(getFullPropertyName(property), Object.class);
            }
            if (field != null)
            {
                return getRecordFieldValue(getRecordData(), field, null);            
            }
        }    
        // Get Value
        return getRecordValue(getRecordData(), value, null);
    }
    
    protected String getStringValue()
    {   // Convert value to String
        return StringUtils.toString(getValue());
    }
    
    protected String getItemValue(Object item)
    {
        if ((item instanceof String))
        {   // Item is a field or property name
            String str = item.toString();
            if (str.length()==0)
                return null; // Error: Item string is empty
            // Starts with
            char prefix = str.charAt(0);
            if (prefix=='%' || prefix=='#' || prefix=='$')
                return getString(str, null);
            if (prefix=='!')
                return ((String)item).substring(1);
            if (prefix<'A')
            {   log.error("Invalid property or field name supplied for item.");
                return null;
            }
            // Item is property or field name 
            if (property != null)
            {
                String fullName = getFullPropertyName(str);
                return (String)getStack().findValue(fullName, String.class);
            }
            else
            {
                return StringUtils.toString(getRecordFieldValue(record, str, null));            
            }
        }
        // Default
        return StringUtils.toString(getRecordValue(record, item, null));
    }
    
    protected String getPropertyFieldName()
    {
        if (property != null)
            return getFullPropertyName(property);
        if (field != null)
            return getFullPropertyName(field);        
        return null;
    }
    
    protected String getFullPropertyName(String name)
    {
        // Get name from column
        if ("*".equals(name) && column!=null)
            name = column.getBeanPropertyName();
        // Prepend parent Property name (if any)
        if (parentProperty==null)
            parentProperty = StringUtils.toString(getPageAttribute(PARENT_PROPERTY_ATTRIBUTE, null));
        if (parentProperty!=null)
            return parentProperty + "." + name;
        // return the name
        return name;
    }
    
    protected String getColumnPropertyName(ColumnExpr col)
    {
        return getFullPropertyName(col.getName());
    }
    
    protected boolean setPropertyNameFromValue()
    {
        if (property==null && (value instanceof String))
        {
            String strval = ((String)value);
            if (strval.startsWith("%{") && strval.endsWith("}"))
            { // It's a property Name
                property = strval.substring(2, strval.length() - 1).trim();
                return true;
            }
        }
        return false;
    }
    
    protected boolean hasDefaultValue()
    {
        return (value!=null && (property!=null || field!=null || column!=null));
    }
    
    protected Object getDefaultValue()
    {
        // Try property and field first
        if (property != null)
        {   // Value from Property
            return getStack().findValue(getFullPropertyName(property), Object.class);
        }
        if (field != null)
        {
            return getRecordFieldValue(getRecordData(), field, null);            
        }
        // Get Value
        return getRecordValue(getRecordData(), null, null);
    }

    // ------- Internal -------
    
    private Object getRecordFieldValue(RecordData rec, String field, Object defValue)
    {
        // Field Param must be supplied
        if (field==null)
            return defValue;
        // If record is not set, then try to read it from page context
        if (rec!=null)
        {   // Find field by name
            int index = rec.getFieldIndex(field);
            if (index>= 0)
                return rec.getValue(index);
            // Field not found 
            log.error("Supplied field '" + field + "' not found in record.");
        }
        else
        {   // Cannot find data source (record or bean)  
            log.error("No record supplied for field value");
        }
        return defValue;
    }
    
    private Object getRecordValue(RecordData rec, Object value, Object defValue)
    {
        // Find Record Value
        if (value==null && column!=null)
            value = column;
        // Find Record Value
        if (value instanceof ColumnExpr)
        {
            ColumnExpr column = ((ColumnExpr)value);
            if (rec!=null)
            {   // Find column by object first
                int index = rec.getFieldIndex(column);
                if (index<0)
                {   // Column not found. Trying name
                    log.debug("Column object '" + column.getName() + "' not found. Trying name.");
                    index = rec.getFieldIndex( column.getName());
                    if (index<0)
                    {   // Column not found  
                        log.error("Column '" + column.getName() + "' not found in record.");
                        return null;
                    }
                }
                // Get value from record
                return rec.getValue(index);
            }
            else
            {   // Check if Columns specifies a bean property
                Object bean = getBean();
                if (bean!=null)
                {   // Property Name
                    String prop = column.getBeanPropertyName();
                    return getBeanProperty(bean, prop);
                }
            }
            // Cannot find data source (record or bean)  
            // log.warn("No record supplied for column value");
            return defValue;
        }
        // getValue 
        return this.getObject(value, defValue);
    }

    // -------------------------------- Property accessors -----------------------------
    
    public final void setField(String field)
    {
        this.field = StringUtils.validate(field);
    }

    public final void setProperty(String property)
    {
        this.property = StringUtils.validate(property);
    }

    public final void setParentProperty(String property)
    {
        this.parentProperty = StringUtils.validate(property);
    }

    public final void setRecord(RecordData record)
    {
        this.record = record;
    }

    public final void setValue(Object value)
    {
        this.value = value;
    }

    public final void setColumn(ColumnExpr column)
    {
        this.column = column;
    }
    
}
