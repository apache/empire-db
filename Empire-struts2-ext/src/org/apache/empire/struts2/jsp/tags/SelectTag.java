/*
 * ESTEAM Software GmbH, 17.07.2007
 */
package org.apache.empire.struts2.jsp.tags;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;

import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.commons.OptionEntry;
import org.apache.empire.commons.Options;
import org.apache.empire.struts2.html.HtmlWriter;
import org.apache.empire.struts2.html.HtmlWriter.HtmlTag;
import org.apache.struts2.components.Component;

import com.opensymphony.xwork2.util.ValueStack;


@SuppressWarnings("serial")
public class SelectTag extends EmpireValueTagSupport
{
    // Checkbox Tag
    protected String name;
    protected Options options;
    protected Object allownull;
    protected Object disabled;
    // More UI
    protected String tabindex;
    protected String onclick;
    protected String onchange;
    protected String onfocus;
    protected String onblur;
    
    @Override
    protected void resetParams()
    {
        name = null;
        options = null;
        allownull = null;
        disabled = null;
        // AbstractUITag
        onclick = null;
        onchange = null;
        onfocus = null;
        onblur = null;
        tabindex = null;
        // Value
        super.resetParams();
    }

    @Override
    public Component getBean(ValueStack stack, HttpServletRequest req, HttpServletResponse res)
    {
        return null;
    }

    @Override
    public int doStartTag()
        throws JspException
    {
        // Select Input Tag
        HtmlWriter w = new HtmlWriter(pageContext.getOut());
        HtmlTag select = w.startTag("select");
        addStandardAttributes(select, null);
        select.addAttribute("name",     getTagName(name));
        select.addAttribute("disabled", getBoolean(disabled, false));
        select.addAttribute("tabindex", this.tabindex);
        // Event Attributes
        select.addAttribute("onclick",  this.onclick);
        select.addAttribute("onchange", this.onchange);
        select.addAttribute("onfocus",  this.onfocus);
        select.addAttribute("onblur",   this.onblur);
        select.beginBody(true);
        // Render List of Options
        if (options!=null)
        {   // Render option list
            Object current = getValue();
            if (getBoolean(allownull, false) && options.contains(null)==false)
            {   // add an empty entry
                HtmlTag option = w.startTag("option");
                option.addAttributeNoCheck("value", "", false);
                option.addAttribute("selected", ObjectUtils.isEmpty(current));
                option.beginBody("");
                option.endTag(true);
            }
            for (OptionEntry entry : options)
            {
                Object value = entry.getValue();
                boolean isCurrent = ObjectUtils.compareEqual(current, value);
                // Add Option entry
                HtmlTag option = w.startTag("option");
                option.addAttributeNoCheck("value", value, true);
                option.addAttribute("selected", isCurrent);
                option.beginBody(getTranslation(entry.getText()));
                option.endTag(true);
            }
        }
        else
        {   // No Option list available
            log.error("No options available for select tag.");
        }
        // done
        select.endTag();
        // return usesBody ? EVAL_BODY_BUFFERED : EVAL_BODY_INCLUDE;
        return SKIP_BODY;
    }

    @Override
    public int doEndTag()
        throws JspException
    {   // done
        resetParams();
        return EVAL_PAGE;
    }

    // -------------------------------- Property accessors -----------------------------

    public void setName(String name)
    {
        this.name = name;
    }
    
    public void setOptions(Options options)
    {
        this.options = options;
    }

    public void setAllownull(Object allownull)
    {
        this.allownull = allownull;
    }

    public void setDisabled(Object disabled)
    {
        this.disabled = disabled;
    }

    public void setTabindex(String tabindex)
    {
        this.tabindex = tabindex;
    }

    public void setOnclick(String onclick)
    {
        this.onclick = onclick;
    }

    public void setOnchange(String onchange)
    {
        this.onchange = onchange;
    }

    public void setOnfocus(String onfocus)
    {
        this.onfocus = onfocus;
    }

    public void setOnblur(String onblur)
    {
        this.onblur = onblur;
    }
}
