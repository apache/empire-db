package org.apache.empire.struts2.jsp.controls;

import java.util.Locale;

import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.data.Column;
import org.apache.empire.struts2.action.RequestParamProvider;
import org.apache.empire.struts2.html.HtmlWriter;
import org.apache.empire.struts2.html.HtmlWriter.HtmlTag;


public class CheckboxInputControl extends InputControl
{
    private static String CHECK_POSTFIX = "__CBCHECK";

    @Override
    public Object getFieldValue(String name, RequestParamProvider request, Locale locale, Column column)
    {
        // If the checkbox was checked then a value is present in request params
        // otherwise no value will be present.
        String val = request.getRequestParam(name);
        if (val!=null)
            return Boolean.TRUE;
        // Is Hidden Value present   
        if (request.getRequestParam(name + CHECK_POSTFIX)==null)
            return NO_VALUE;
        // Checkbox is present, but was unchecked
        return Boolean.FALSE;
    }

    @Override
    public void renderText(HtmlWriter writer, ValueInfo vi)
    {
        // Always render checkboxes as controls
        HtmlTag input = writer.startTag("input");
        input.addAttribute("type", "checkbox");
        input.addAttribute("id",    vi.getId());
        input.addAttribute("class", vi.getCssClass());
        input.addAttribute("style", vi.getCssStyle());
        input.addAttribute("disabled");
        // Decide whether to render the checkbox checked or not.
        if (ObjectUtils.getBoolean(vi.getValue()))
            input.addAttribute("checked");
        input.endTag();
    }
    
    @Override
    public void renderInput(HtmlWriter writer, ControlInfo ci)
    {
        if (ci.getDisabled())
        {   // render disabled
            renderText(writer, ci);
            return;
        }
        // Render enabled
        HtmlTag input = writer.startTag("input");
        input.addAttribute("type", "checkbox");
        input.addAttribute("id",    ci.getId());
        input.addAttribute("class", ci.getCssClass());
        input.addAttribute("style", ci.getCssStyle());
        input.addAttribute("name",  ci.getName());
        // Decide whether to render the checkbox checked or not.
        if (ObjectUtils.getBoolean(ci.getValue()))
        {
            input.addAttribute("checked");
        }
        // Event Attributes
        input.addAttribute("onclick",   ci.getOnclick());
        input.addAttribute("onchange",  ci.getOnchange());
        input.addAttribute("onfocus",   ci.getOnfocus());
        input.addAttribute("onblur",    ci.getOnblur());
        input.endTag();
        // Additionally add a hidden field
        // to detect unchecked state
        HtmlTag hidden = writer.startTag("input");
        hidden.addAttribute("type",  "hidden");
        hidden.addAttribute("name",  ci.getName() + CHECK_POSTFIX);
        hidden.addAttribute("value", ci.getValue());
        hidden.endTag();
    }
}
