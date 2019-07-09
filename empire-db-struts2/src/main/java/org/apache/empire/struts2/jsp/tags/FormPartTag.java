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

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;

import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.commons.StringUtils;
import org.apache.empire.data.Column;
import org.apache.empire.data.ColumnExpr;
import org.apache.empire.data.DataType;
import org.apache.empire.data.Record;
import org.apache.empire.data.RecordData;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.DBRecord;
import org.apache.empire.struts2.actionsupport.ActionBase;
import org.apache.empire.struts2.html.HtmlTagDictionary;
import org.apache.empire.struts2.html.HtmlWriter;
import org.apache.empire.struts2.html.HtmlWriter.HtmlTag;
import org.apache.struts2.components.Component;

import com.opensymphony.xwork2.util.ValueStack;


@SuppressWarnings("serial")
public class FormPartTag extends EmpireTagSupport // FormTag
{
    public static final String DISABLEDMODE_ATTRIBUTE = "defaultDisabledMode";
    public static final String CONTROLSIZE_ATTRIBUTE = "defaultControlSize";
    public static final String NULLVALUE_ATTRIBUTE = "defaultNullValue";
    public static final String READONLY_ATTRIBUTE = "readOnly";

    // FormPartTag
    protected RecordData record;
    protected Object bean;
    protected Object controlSize;
    protected Object nullValue;
    protected String disabledMode;
    protected String property;
    protected Object hiddenFields;
    protected Object wrap;
    
    // temporary internal use
    private Object oldRecord;
    private Object oldBean;
    private Object oldControlSize;
    private Object oldDisabledMode;
    private Object oldNullValue;
    private Object oldProperty;
    
    /*
     * Clears all params since tag is reused
     */
    @Override
    protected void resetParams()
    {
        // RecordFormTag
        record = null;
        controlSize = null;
        disabledMode = null;
        nullValue = null;
        property = null;
        hiddenFields = null;
        wrap = null;
        // reset
        super.resetParams();
    }

    @Override
    public Component getBean(ValueStack stack, HttpServletRequest req, HttpServletResponse res)
    {
        return null;
    }
    
    public boolean useBean()
    {
        return false;
    }
    
    protected boolean renderReadOnlyFields()
    {
        return getBoolean(hiddenFields, false);        
    }
    
    protected boolean renderWrapperTag()
    {
        return getBoolean(wrap, true);
    }
    
    @Override
    public int doStartTag() throws JspException
    {
        int result = (useBean() ? super.doStartTag() : EVAL_BODY_INCLUDE);
        // Set default Property name
        if (property== null && record!=null)
            property = getActionItemPropertyName();
        // Set Record
        if (record!= null)
            oldRecord = putPageAttribute(EmpireValueTagSupport.RECORD_ATTRIBUTE, record);
        if (bean!=null)
            oldBean = putPageAttribute(EmpireValueTagSupport.BEAN_ITEM_ATTRIBUTE, bean);
        // Parent Property
        if (property!= null)
            oldProperty = putPageAttribute(EmpireValueTagSupport.PARENT_PROPERTY_ATTRIBUTE, property);
        // DisabledMode
        if (disabledMode!=null)
            oldDisabledMode = putPageAttribute(DISABLEDMODE_ATTRIBUTE, disabledMode);
        // ControlSize
        if (ObjectUtils.isEmpty(controlSize)==false)
            oldControlSize = putPageAttribute(CONTROLSIZE_ATTRIBUTE, getString(controlSize));
        // NullValue
        if (ObjectUtils.isEmpty(nullValue)==false)
            oldNullValue = putPageAttribute(NULLVALUE_ATTRIBUTE, getObject(nullValue, null));
        // Set additional Params
        if (record!=null && renderReadOnlyFields())
        {   // set hidden Values
            HtmlWriter w = new HtmlWriter(pageContext.getOut());
            renderHiddenField(w, str(property, getActionItemPropertyName()), getRecordKey());
            // Add Read Only field
            renderReadOnlyColumns(w);
        }
        // Write Form Wrapper Tag
        if (renderWrapperTag())
        {   // Write Form Wrapper Tag
            if (useBean())
                setId(null); // Id has already be used for componentBean
            // Render Tag
            HtmlTagDictionary dic = HtmlTagDictionary.getInstance();  
            HtmlWriter w = new HtmlWriter(pageContext.getOut());
            HtmlTag wrapTag  = w.startTag( dic.FormPartWrapperTag());
            addStandardAttributes(wrapTag, dic.FormPartWrapperClass());
            wrapTag.addAttributes(dic.FormPartWrapperAttributes());
            wrapTag.beginBody(true);
        }
        // do Start
        return result;
    }

    @Override
    public int doEndTag() throws JspException
    {
        // Close Wrapper Tag
        if (renderWrapperTag())
        {   // Close Form Wrapper Tag
            HtmlTagDictionary dic = HtmlTagDictionary.getInstance();  
            HtmlWriter w = new HtmlWriter(pageContext.getOut());
            HtmlTag wrap = w.continueTag(dic.FormPartWrapperTag(), true);
            wrap.endTag();
        }
        // NullValue
        if (nullValue!=null)
            removePageAttribute(NULLVALUE_ATTRIBUTE, oldNullValue);
        oldNullValue = null;
        // ControlSize
        if (controlSize!=null)
            removePageAttribute(CONTROLSIZE_ATTRIBUTE, oldControlSize);
        oldControlSize = null;
        // DisabledMode
        if (disabledMode!=null)
            removePageAttribute(DISABLEDMODE_ATTRIBUTE, oldDisabledMode);
        disabledMode = null;
        // Parent Property
        if (property!= null)
            removePageAttribute(EmpireValueTagSupport.PARENT_PROPERTY_ATTRIBUTE, oldProperty);
        oldProperty = null;
        // Bean
        if (bean!= null)
            removePageAttribute(EmpireValueTagSupport.BEAN_ITEM_ATTRIBUTE, oldBean);
        oldBean = null;
        // Record
        if (record!= null)
            removePageAttribute(EmpireValueTagSupport.RECORD_ATTRIBUTE, oldRecord);
        oldRecord = null;
        // done
        if (useBean())
        {   // Cleanup Bean
            return super.doEndTag();
        }
        else
        {   // Dont use Bean
            resetParams();
            return EVAL_PAGE;
        }
    }
    
    private String getRecordKey()
    {
        if ((record instanceof Record)==false)
            return null; // not supported
        // find Action
        Record rec = (Record)record;
        if (rec.isValid()==false)
        {   log.error("Unable to detect record key. Record supplied is not valid!");
            return null;
        }
        Object action = this.pageContext.getRequest().getAttribute("action");
        if (action instanceof ActionBase)
        {
            return ((ActionBase)action).getRecordKeyString(rec);
        }
        // Assemble 
        StringBuffer key = new StringBuffer();
        Column [] keyCols = rec.getKeyColumns();
        for (int i=0; i<keyCols.length; i++)
        {
            if (i>0) 
                key.append("/");
            key.append(StringUtils.valueOf(rec.getValue(keyCols[i])));
        }
        return key.toString();
    }

    private void renderHiddenField(HtmlWriter w, String name, String value)
    {
        HtmlTag item = w.startTag("input");
        item.addAttribute("type", "hidden");
        item.addAttribute("name",  name);
        item.addAttribute("value", value);
        item.endTag(true);
    }

    private void renderReadOnlyColumns(HtmlWriter w)
    {
        if (record instanceof Record && ((Record)record).isValid())
        {   // Special Timestamp Logic
            Column timestamp = null;
            if (record instanceof DBRecord)
            {   // Only for instances of DBRecord!
                timestamp = ((DBRecord)record).getRowSet().getTimestampColumn();
            }
            // Key Columns
            Record rec = (Record)record;
            Column [] keyCols = rec.getKeyColumns();
            String sysdate = DBDatabase.SYSDATE.toString();
            int count = rec.getFieldCount();
            for (int i=0; i<count; i++)
            {
                Column column = rec.getColumn(i);
                if (column==null)
                    continue;
                if (column!=timestamp)
                {   // Check if column was modified
                    if (rec.wasModified(column)==false || rec.isFieldReadOnly(column)==false)
                        continue;
                    // Check whether column is a key column
                    if (isKeyColumn(column, keyCols))
                        continue;
                }
                // Check for Null-Value
                if (record.isNull(i))
                    continue;
                // Add hidden field
                DataType dataType = column.getDataType();
                String value = StringUtils.toString(record.getValue(i)); 
                if ((dataType==DataType.DATETIME || dataType==DataType.TIMESTAMP) && sysdate.equals(value)==false)
                {   // Special for Timestamps
                    Date date = ObjectUtils.getDate(record.getValue(i));
                    value = formatDate(date, "yyyy-MM-dd HH:mm:ss.S");
                }
                else if (column.getDataType()==DataType.DATE && sysdate.equals(value)==false)
                {   // Special for Timestamps
                    Date date = ObjectUtils.getDate(record.getValue(i));
                    value = formatDate(date, "yyyy-MM-dd");
                }
                // Add hidden field
                renderHiddenField(w, getColumnPropertyName(column, property), value);
            }
        }
    }
    
    private String formatDate(Date date, String format)
    {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(format);
            return sdf.format(date);
        } catch(Exception e) {
            log.error("Unable to format date", e);
            return StringUtils.valueOf(date);
        }
    }
    
    private boolean isKeyColumn(Column column, Column[] keyCols)
    {
        if (keyCols!=null)
        {
            for (int i=0; i<keyCols.length; i++)
                if (keyCols[i]==column)
                    return true;
        }
        return false;
    }

    private String getColumnPropertyName(ColumnExpr col, String property)
    {
        String name = col.getName();
        if (property==null)
            return name+ "!";
        // A full name
        return property + "." + name + "!";
    }
    
    // ------- Setters -------

    public void setControlSize(Object controlSize)
    {
        this.controlSize = controlSize;
    }

    public void setNullValue(Object nullValue)
    {
        this.nullValue = nullValue;
    }

    public void setProperty(String property)
    {
        this.property = property;
    }

    public void setRecord(RecordData record)
    {
        this.record = record;
    }

    public void setBean(Object bean)
    {
        this.bean = bean;
    }

    public void setHiddenFields(Object hiddenFields)
    {
        this.hiddenFields = hiddenFields;
    }

    public void setDisabledMode(String disabledMode)
    {
        this.disabledMode = disabledMode;
    }

    public void setWrap(Object wrap)
    {
        this.wrap = wrap;
    }

}
