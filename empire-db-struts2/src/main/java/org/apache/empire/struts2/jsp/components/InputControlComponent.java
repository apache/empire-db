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
package org.apache.empire.struts2.jsp.components;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.empire.commons.ErrorInfo;
import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.commons.StringUtils;
import org.apache.empire.data.DataType;
import org.apache.empire.struts2.action.ActionErrorProvider;
import org.apache.empire.struts2.html.HtmlTagDictionary;
import org.apache.empire.struts2.html.HtmlWriter;
import org.apache.empire.struts2.html.HtmlWriter.HtmlTag;
import org.apache.empire.struts2.jsp.controls.InputControl;

import com.opensymphony.xwork2.util.ValueStack;


public class InputControlComponent extends ControlComponent implements InputControl.ControlInfo
{
    public enum RenderType
    {
        ALL,
        LABEL,
        CONTROL,
        INPUTLABEL,
        INPUTCONTROL,
        HIDDEN
    }

    public enum DisabledMode
    {
        DEFAULT,
        TEXT,
        TEXTHIDDEN,
        CONTROL,
        CONTROLHIDDEN,
    }
    
    private int hSize;
    private int vSize;
    private boolean readOnly;
    private String labelClass;
    private String labelStyle;
    
    private RenderType renderType  = RenderType.ALL;
    private DisabledMode disabledMode = DisabledMode.DEFAULT;

    public InputControlComponent(InputControl control, ValueStack stack, HttpServletRequest req, HttpServletResponse res)
    {
        super(control, stack, req, res);
    }

    /*
    private Form findForm()
    {
        final Form form = (Form) findAncestor(Form.class);
        if (form != null)
        {
            log.info("Control belongs to form: " + form.toString());
            if (id == null)
            {
                String formId = form.getId();
                if (formId != null && formId.length() > 0 && formId.indexOf('.')<0)
                    id = formId + "." + name;
                else
                    id = name;
            }
        }
        return form;
    }
    */

    @Override
    protected void render(HtmlWriter writer, String body, InputControl control)
    {
        HtmlTagDictionary dic = HtmlTagDictionary.getInstance();
        // Check Render Type
        if (renderType==RenderType.HIDDEN)
        {   // Render Hidden input
            renderHiddenValue(writer);
        }
        else if (renderType==RenderType.LABEL)
        {
            // Render Label only
            HtmlTag label = writer.startTag("label");
            label.addAttribute("class", StringUtils.coalesce(this.labelClass, this.cssClass));
            label.addAttribute("style", StringUtils.coalesce(this.labelStyle, this.cssStyle));
            if (control.useLabelId())
                label.addAttribute("for", getId());
            label.beginBody(this.label);
            label.endTag();
        }
        else if (renderType==RenderType.CONTROL)
        {   // Render Input
            if (renderControlAsData(dic))
            {   // Render Input as Data
                control.renderText(writer, this);
            }
            else
            {   // Render Input as Control
                control.renderInput(writer, this);
            }
            // Additionally render hidden value?
            if (renderHidden())
                renderHiddenValue(writer);
        }
        else
        {   // Check wether to render all 
            boolean renderWrapper =(renderType==RenderType.ALL);
            
            // the wrapper (only if renderLabel && renderControl are both true)
            HtmlTag wrapper = writer.startTag((renderWrapper ? dic.InputWrapperTag() : null));
            wrapper.addAttribute("class", dic.InputWrapperClass());
            wrapper.beginBody(true);

            if (renderType!=RenderType.INPUTCONTROL)
            {
                HtmlTag wrapLabel = writer.startTag(dic.InputLabelTag());
                wrapLabel.addAttribute("class", dic.InputLabelClass());
                wrapLabel.beginBody();
                // label
                HtmlTag label = writer.startTag("label");
                if (control.useLabelId())
                    label.addAttribute("for", getId());
                label.addAttribute("class", this.labelClass);
                label.addAttribute("style", this.labelStyle);
                label.beginBody(this.label);
                label.endTag(":");
                // required
                if ("true".equals(this.required) && readOnly == false)
                {
                    HtmlTag required = writer.startTag(dic.InputRequiredTag());
                    required.addAttribute("class", dic.InputRequiredClass());
                    required.beginBody("*");
                    required.endTag();
                }
                // close
                wrapLabel.endTag((renderType!=RenderType.INPUTLABEL));
            }

            // render Control
            if (renderType!=RenderType.INPUTLABEL)
            {
                HtmlTag wrapCtrl = writer.startTag(dic.InputControlTag());
                // Render Input
                if (renderControlAsData(dic))
                {   // Render Input as Data
                    wrapCtrl.addAttribute("class", dic.InputReadOnlyClass());
                    wrapCtrl.beginBody();
                    readOnly = true;
                    control.renderText(writer, this);
                }
                else
                {   // Render Input as Control
                    String wrapClass = (getDisabled() ? dic.InputReadOnlyClass() : dic.InputControlClass());   
                    wrapCtrl.addAttribute("class", wrapClass);
                    wrapCtrl.beginBody();
                    control.renderInput(writer, this);
                }
                // Additionally render hidden value?
                if (renderHidden())
                    renderHiddenValue(writer);
                // End Tag
                wrapCtrl.endTag(renderWrapper);
            }
            
            // Done
            String wrapperBody = (wrapper.isValid()) ? dic.InputWrapperBody() : null;  
            wrapper.endTag(wrapperBody);
        }
    }
    
    private boolean renderHidden()
    {   // render hidden
        if (readOnly==false)
            return false;
        return (disabledMode==DisabledMode.CONTROLHIDDEN || disabledMode==DisabledMode.TEXTHIDDEN);
    }
    
    private boolean renderControlAsData(HtmlTagDictionary dic)
    {   // Render Input
        if (readOnly==false)
            return false;
        if (disabledMode==DisabledMode.DEFAULT)
            return dic.InputReadOnlyAsData();
        return (disabledMode==DisabledMode.TEXT || disabledMode==DisabledMode.TEXTHIDDEN);
    }
    
    private void renderHiddenValue(HtmlWriter writer)
    {
        // Render Hidden input
        HtmlTag input = writer.startTag("input");
        input.addAttribute("type", "hidden");
        input.addAttribute("name", getName()+ "!");
        // Get Value
        String value;
        if (recordValue instanceof Date)
        {   // Special for Dates and timestamps
            String format = (column!=null && column.getDataType()==DataType.DATE) ? "yyyy-MM-dd" : "yyyy-MM-dd HH:mm:ss.S";
            SimpleDateFormat sdf = new SimpleDateFormat(format);
            value = sdf.format(recordValue);
        }
        else
        {   // Let Record do the conversion
            value = StringUtils.valueOf(recordValue); 
        }
        // Add Value Attribute
        if (value.length()>0)
            input.addAttribute("value", value);
        else
            writer.print("value=\"\"");
        input.endTag();
    }
    
    public void setReadOnly(boolean readOnly)
    {
        this.readOnly = readOnly;
    }

    public void setRenderType(String type)
    {
        if (type==null)
            renderType = RenderType.ALL;
        else if (type.equalsIgnoreCase("label"))
            renderType = RenderType.LABEL;
        else if (type.equalsIgnoreCase("control"))
            renderType = RenderType.CONTROL;
        else if (type.equalsIgnoreCase("inputlabel"))
            renderType = RenderType.INPUTLABEL;
        else if (type.equalsIgnoreCase("inputcontrol"))
            renderType = RenderType.INPUTCONTROL;
        else if (type.equalsIgnoreCase("hidden"))
            renderType = RenderType.HIDDEN;
        else
            renderType = RenderType.ALL;
    }

    public void setDisabledMode(String type)
    {
        if (type==null)
            disabledMode = DisabledMode.DEFAULT;
        else if (type.equalsIgnoreCase("text"))
            disabledMode = DisabledMode.TEXT;
        else if (type.equalsIgnoreCase("textHidden"))
            disabledMode = DisabledMode.TEXTHIDDEN;
        else if (type.equalsIgnoreCase("control"))
            disabledMode = DisabledMode.CONTROL;
        else if (type.equalsIgnoreCase("controlHidden"))
            disabledMode = DisabledMode.CONTROLHIDDEN;
        else
            disabledMode = DisabledMode.DEFAULT;
    }
    
    public void setHSize(String size)
    {
        hSize = ObjectUtils.getInteger(size, 0);
    }

    public void setVSize(String size)
    {
        vSize = ObjectUtils.getInteger(size, 0);
    }
    
    // InputControl.InputInfo

    @Override
    public String getId()
    {
        return ((id == null) ? name : id);
    }

    public final String getName()
    {
        return name;
    }

    public final boolean getDisabled()
    {
        return readOnly;
    }

    public final String getTabindex()
    {
        return tabindex;
    }

    public final String getAccesskey()
    {
        return accesskey;
    }

    public final String getOnchange()
    {
        return onchange;
    }

    public final String getOnfocus()
    {
        return onfocus;
    }

    public final String getOnblur()
    {
        return onblur;
    }
    
    public final boolean hasError()
    {
        Object action = getAction();
        if (action instanceof ActionErrorProvider)
        {
            Map<String, ErrorInfo> map = ((ActionErrorProvider)action).getItemErrors();
            return (map!=null && map.containsKey(getName()));
        }
        // don't know, assume no
        return false;
    }

    public int getHSize()
    {
        if (hSize== 0)
            hSize = HtmlTagDictionary.getInstance().InputMaxCharSize();
        return hSize;
    }

    public int getVSize()
    {
        return vSize;
    }

    public void setLabelClass(String labelClass)
    {
        this.labelClass = labelClass;
    }

    public void setLabelStyle(String labelStyle)
    {
        this.labelStyle = labelStyle;
    }

}
