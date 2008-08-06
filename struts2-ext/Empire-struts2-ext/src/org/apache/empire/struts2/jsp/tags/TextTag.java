/*
 * ESTEAM Software GmbH, 04.07.2007
 */
package org.apache.empire.struts2.jsp.tags;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;

import org.apache.empire.data.Column;
import org.apache.empire.data.ColumnExpr;
import org.apache.empire.struts2.html.HtmlWriter;
import org.apache.empire.struts2.html.HtmlWriter.HtmlTag;
import org.apache.struts2.components.Component;

import com.opensymphony.xwork2.util.ValueStack;


@SuppressWarnings("serial")
public class TextTag extends EmpireTagSupport
{
    public static final String TEXT_DEFAULT_VALUE = "&nbsp;"; 
    
    // Properties
    protected Object   value;
    protected String   tag;
    protected String   defValue = TEXT_DEFAULT_VALUE;

    @Override
    protected void resetParams()
    {
        value = null;
        tag = null;
        defValue = TEXT_DEFAULT_VALUE;
        // reset
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
        HtmlTag wrap = w.startTag(tag);
        addStandardAttributes(wrap, null);
        wrap.beginBody(getTextValue());
        // return usesBody ? EVAL_BODY_BUFFERED : EVAL_BODY_INCLUDE;
        return EVAL_BODY_INCLUDE;
    }

    @Override
    public int doEndTag()
        throws JspException
    {
        // Write End Tag
        HtmlWriter w = new HtmlWriter(pageContext.getOut());
        HtmlTag wrap = w.continueTag (tag, true);
        wrap.endTag();
        // done
        resetParams();
        return EVAL_PAGE;
    }
    
    private String getTextValue()
    {
        if (value instanceof ColumnExpr)
        {   // Get Column Title
            Column column = ((ColumnExpr)value).getSourceColumn();
            if (column!=null)
                return getTranslation( column.getTitle() );
        }
        if (value!=null)
        {   // Get Value
            String val = getString(value);
            return ((val!=null && val.length()>0) ? val : defValue);
        }
        // default
        return defValue;
    }
    
    // ------- Property setters -------

    public void setTag(String tag)
    {
        this.tag = tag;
    }

    public void setValue(Object value)
    {
        this.value = value;
    }

    public void setDefault(String defValue)
    {
        this.defValue = defValue;
    }
}
