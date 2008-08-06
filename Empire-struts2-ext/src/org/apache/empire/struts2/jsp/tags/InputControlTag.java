package org.apache.empire.struts2.jsp.tags;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.commons.Options;
import org.apache.empire.commons.StringUtils;
import org.apache.empire.data.Column;
import org.apache.empire.data.Record;
import org.apache.empire.struts2.jsp.components.InputControlComponent;
import org.apache.empire.struts2.jsp.controls.InputControl;
import org.apache.empire.struts2.jsp.controls.InputControlManager;
import org.apache.struts2.components.Component;

import com.opensymphony.xwork2.util.ValueStack;


@SuppressWarnings("serial")
public class InputControlTag extends EmpireValueTagSupport // AbstractUITag
{
    // InputControlTag
    protected Column    column;
    protected Options   options;
    protected String    controlType;
    protected String    render;         // 'label', 'control' or 'all'
    protected String    disabledMode;   // 'simple' or 'control'
    protected String    format;  
    protected Object    hsize;
    protected Object    vsize;
    
    // AbstractUITag
    protected String    name;
    protected String    label;
    protected String    labelClass;
    protected String    labelStyle;
    protected Object    required;
    protected Object    disabled;
    protected String    tabindex;
    protected String    onclick;
    protected String    onchange;
    protected String    onfocus;
    protected String    onblur;
    protected Object    nullValue;
    
    /*
     * Clears all params since tag is reused
     */
    @Override
    protected void resetParams()
    {
        // Release All Variables
        column = null;
        options = null;
        controlType = null;
        render = null;
        disabledMode = null;
        format = null;
        hsize = null;
        vsize = null;
        // AbstractUITag
        name = null;
        label = null;
        labelClass = null;
        labelStyle = null;
        required = null;
        disabled = null;
        onclick = null;
        onchange = null;
        onfocus = null;
        onblur = null;
        tabindex = null;
        nullValue = null;
        // super
        super.resetParams();
    }

    @Override
    public Component getBean(ValueStack stack, HttpServletRequest req, HttpServletResponse res)
    {
        // Detect control type and readOnly state
        if (controlType==null)
            controlType = getControlType();
        // Create
        InputControl control = InputControlManager.getControl(controlType);
        if (control == null)
            control = InputControlManager.getControl("text");
        // Create component
        return new InputControlComponent(control, stack, req, res);
    }

    @Override
    protected void populateParams()
    {
        super.populateParams();
        
        if (disabledMode==null)
            disabledMode= StringUtils.toString(getPageAttribute(FormPartTag.DISABLEDMODE_ATTRIBUTE, null));

        // Init Component
        InputControlComponent comp = (InputControlComponent) component;
        
        // Control Component
        comp.setOptions( getLookupOptions() );
        comp.setColumn(  column );
        comp.setRecordValue( getValue() );
        comp.setNullValue( getObject(nullValue, getPageAttribute(FormPartTag.NULLVALUE_ATTRIBUTE, null )));
        // Set Value

        // InputControlComponent
        comp.setName (getString( name,  getControlName() ));
        comp.setLabel(getString( label, getTranslation( column.getTitle()) ));
        comp.setLabelClass( this.labelClass );
        comp.setLabelStyle( this.labelStyle );
        comp.setRequired( isRequired() ? "true" : "false" );
        comp.setReadOnly( isReadOnly() ); // berücksichtigt disabled!
        comp.setRenderType(render);
        comp.setDisabledMode(disabledMode);

        // Common UI
        comp.setFormat(format);
        comp.setHSize( getString(hsize, getPageAttribute(FormPartTag.CONTROLSIZE_ATTRIBUTE, null )));
        comp.setVSize( getString(vsize) );
        comp.setOnclick(onclick);
        comp.setOnchange(onchange);
        comp.setOnfocus(onfocus);
        comp.setOnblur(onblur);
        comp.setTabindex(getString(tabindex));
    }

    private boolean isReadOnly()
    {
        if (disabled!=null)
            return getBoolean(disabled, false);
        // Default
        Object readOnly = getPageAttribute(FormPartTag.READONLY_ATTRIBUTE, null);
        if (readOnly!=null && ObjectUtils.getBoolean(readOnly))
            return true; // Form is read Only
        // Detect only if record is supplied!
        Record record = getRecord(); 
        if (record!=null)
            return record.isFieldReadOnly(column);
        else if (getRecordData()!=null)
            return true; // Only a record data object
        else if (getBean()!=null)
            return column.isReadOnly();
        // Render editable
        return false;
    }

    private boolean isRequired()
    {
        if (required!=null)
            return getBoolean(required, false);        
        if (getRecord()!=null || getBean()!=null)
            return column.isRequired();
        // Default
        return false;
    }
    
    private String getControlName()
    {
        // Direct propety Name
        String name = getPropertyFieldName();
        if (name!=null)
            return name;
        // Use Column Name
        return getColumnPropertyName(column);
    }

    private Options getLookupOptions()
    {
        if (options == null)
        { // Get List from Column
            Record rec = getRecord();
            if (rec!=null && rec.isValid())
            { // Options from Record
                return rec.getFieldOptions(column);
            } else
            { // Options from column
                return column.getOptions();
            }
        }
        // Set List
        return options;
    }

    // ********* All Setters *********

    public void setColumn(Column column)
    {
        this.column = column;
        super.setColumn(column);
    }

    public void setOptions(Options options)
    {
        this.options = options;
    }

    public void setRender(String render)
    {
        this.render = render;
    }

    public void setDisabledMode(String disabledMode)
    {
        this.disabledMode = disabledMode;
    }

    public void setFormat(String format)
    {
        this.format = format;
    }

    public void setDisabled(Object disabled)
    {
        this.disabled = disabled;
    }

    public void setLabel(String label)
    {
        this.label = label;
    }

    public void setLabelClass(String labelClass)
    {
        this.labelClass = labelClass;
    }

    public void setLabelStyle(String labelStyle)
    {
        this.labelStyle = labelStyle;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public void setRequired(Object required)
    {
        this.required = required;
    }

    public void setTabindex(String tabindex)
    {
        this.tabindex = tabindex;
    }

    public void setHsize(Object hsize)
    {
        this.hsize = hsize;
    }

    public void setVsize(Object vsize)
    {
        this.vsize = vsize;
    }

    public void setNullValue(Object nullValue)
    {
        this.nullValue = nullValue;
    }

    public void setOnclick(String onclick)
    {
        this.onclick = onclick;
    }

    public void setControlType(String controlType)
    {
        this.controlType = controlType;
    }

    public void setOnblur(String onblur)
    {
        this.onblur = onblur;
    }

    public void setOnchange(String onchange)
    {
        this.onchange = onchange;
    }

    public void setOnfocus(String onfocus)
    {
        this.onfocus = onfocus;
    }
}
