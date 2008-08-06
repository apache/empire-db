/*
 * ESTEAM Software GmbH, 17.07.2007
 */
package org.apache.empire.struts2.jsp.tags;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;

import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.struts2.html.HtmlWriter;
import org.apache.empire.struts2.html.HtmlWriter.HtmlTag;
import org.apache.struts2.components.Component;

import com.opensymphony.xwork2.util.ValueStack;


@SuppressWarnings("serial")
public class CheckboxTag extends EmpireValueTagSupport
{
    // Checkbox Tag
    protected String name;
    protected String hiddenName;
    protected Object checked;
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
        checked = null;
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
        // Tabel cell tag
        HtmlWriter w = new HtmlWriter(pageContext.getOut());
        HtmlTag cb = w.startTag("input");
        cb.addAttribute("type", "checkbox");
        addStandardAttributes(cb, null);
        cb.addAttribute("name",  getTagName(name));
        cb.addAttributeNoCheck("value", getStringValue(), true);
        cb.addAttribute("checked", getChecked());
        cb.addAttribute("disabled", getBoolean(disabled, false));
        cb.addAttribute("tabindex", this.tabindex);
        // Event Attributes
        cb.addAttribute("onclick",   this.onclick);
        cb.addAttribute("onchange",  this.onchange);
        cb.addAttribute("onfocus",   this.onfocus);
        cb.addAttribute("onblur",    this.onblur);
        cb.endTag();
        // wrap.beginBody(getTextValue());
        if (hiddenName!=null)
        {   // Render Additional Hidden Control
            HtmlTag hidden = w.startTag("input");
            hidden.addAttribute("type", "hidden");
            hidden.addAttribute("name",  hiddenName);
            hidden.addAttributeNoCheck("value", getStringValue(), true);
            hidden.endTag();
        }
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
    
    // ------- helpers -------
    
    public boolean getChecked()
    {
        if (checked!=null)
        {
            return getBoolean(checked, false);
        }
        if (hasDefaultValue())
        {
            Object value = getValue();
            Object defval = getDefaultValue();
            if ((value instanceof String) && ((String)value).length()==0)
                value = null;
            if ((defval instanceof String) && ((String)defval).length()==0)
                defval = null;
            return ObjectUtils.compareEqual(value, defval);
        }
        // Done
        return false;
    }

    // -------------------------------- Property accessors -----------------------------

    public void setName(String name)
    {
        this.name = name;
    }

    public void setDisabled(Object disabled)
    {
        this.disabled = disabled;
    }

    public void setHiddenName(String hiddenName)
    {
        this.hiddenName = hiddenName;
    }

    public void setChecked(Object checked)
    {
        this.checked = checked;
    }

    public void setCheck(Object check)
    {
        this.checked = check;
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
