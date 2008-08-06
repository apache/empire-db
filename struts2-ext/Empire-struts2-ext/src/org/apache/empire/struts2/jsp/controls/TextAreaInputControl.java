/*
 * ESTEAM Software GmbH, 22.07.2007
 */
package org.apache.empire.struts2.jsp.controls;

import java.util.Locale;

import org.apache.empire.commons.StringUtils;
import org.apache.empire.data.Column;
import org.apache.empire.struts2.html.HtmlTagDictionary;
import org.apache.empire.struts2.html.HtmlWriter;
import org.apache.empire.struts2.html.HtmlWriter.HtmlTag;

import com.opensymphony.xwork2.util.TextUtils;


public class TextAreaInputControl extends InputControl
{
    private static final char NBSP = 160;  // Non breaking space
    
    @Override
    protected Object parseValue(String value, Locale locale, Column column)
    {
        int idx;
        int len = value.length();
        // Remove trailing whitespace + nbsp
        for (idx=len-1; idx>=0; idx--)
            if (value.charAt(idx)!=NBSP && value.charAt(idx)!=' ')
                break;
        idx++;
        if (idx<len)
            value = value.substring(0, idx);
        len = idx;
        // Remove leading whitespace + nbsp
        for (idx=0; idx<len; idx++)
            if (value.charAt(idx)!=NBSP && value.charAt(idx)!=' ')
                break;
        if (idx>0)
            value = value.substring(idx);
        // Done
        return value;
    }
    
    @Override
    public void renderText(HtmlWriter writer, ValueInfo vi)
    {
        // Wrap read only in a div if it's a control
        if ((vi instanceof ControlInfo))
        {
            ControlInfo ci = ((ControlInfo)vi);
            // cssSTyle
            String style = StringUtils.valueOf(ci.getCssStyle());
            if (style.toLowerCase().indexOf("height")<0)
            {
                if (style.length()>0)
                    style += ";";
                // append height
                double height = Math.max(ci.getVSize(), 2) * 1.25;
                style +=  "height:" + height + "em";
            }
            // Wrap read only in a div if it's a control
            HtmlTagDictionary dic = HtmlTagDictionary.getInstance();
            HtmlTag div = writer.startTag(dic.InputReadOnlyDataWrapperTag());
            div.addAttribute("id",    ci.getId());
            div.addAttribute("class", ci.getCssClass());
            div.addAttribute("style", style);
            div.beginBody();
            internalRenderText(writer, vi);
            div.endTag();
        }
        else
        {
            super.internalRenderText(writer, vi);
        }
    }    

    @Override
    protected void internalRenderText(HtmlWriter writer, ValueInfo vi)
    {
        String text = formatValue(vi);
        text = StringUtils.replaceAll(text, "\r", "");
        text = StringUtils.replaceAll(text, "\n", "<br />");
        printText(writer, text, "&nbsp;");
    }
    
    @Override
    public void renderInput(HtmlWriter writer, ControlInfo ci)
    {
        // <textarea rows="35" name="richtext" cols="120">

        HtmlTag input = writer.startTag("textarea");
        input.addAttribute("id",    ci.getId());
        input.addAttribute("class", ci.getCssClass());
        input.addAttribute("style", ci.getCssStyle());
        input.addAttribute("name",  ci.getName());
        input.addAttribute("disabled", ci.getDisabled());
        input.addAttribute("rows",  Math.max(ci.getVSize(), 2));
        input.addAttribute("cols",  Math.max(ci.getHSize(), 1));
        // Event Attributes
        input.addAttribute("onclick",   ci.getOnclick());
        input.addAttribute("onchange",  ci.getOnchange());
        input.addAttribute("onfocus",   ci.getOnfocus());
        input.addAttribute("onblur",    ci.getOnblur());
        // Body
        String value = StringUtils.toString(ci.getValue());
        value = TextUtils.htmlEncode(value);
        input.beginBody(value);
        // End
        input.endTag();
    }

}
