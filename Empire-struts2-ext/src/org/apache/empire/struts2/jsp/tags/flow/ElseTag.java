/*
 * ESTEAM Software GmbH, 13.07.2007
 */
package org.apache.empire.struts2.jsp.tags.flow;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;

import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.struts2.jsp.tags.EmpireTagSupport;
import org.apache.struts2.components.Component;

import com.opensymphony.xwork2.util.ValueStack;


@SuppressWarnings("serial")
public class ElseTag extends EmpireTagSupport
{
    /*
    private static class ElseComponent extends org.apache.struts2.components.Else
    {
        public ElseComponent(ValueStack stack)
        {
            super(stack);
        }
    }
    */
    
    @Override
    protected void resetParams()
    {
        // reset base params
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
        // Get Result from If
        boolean result = ObjectUtils.getBoolean(getPageAttribute(IfTag.IF_RESULT_ANSWER, false));
        removePageAttribute(IfTag.IF_RESULT_ANSWER, null);
        // Tabel cell tag
        if (result==true)
            return SKIP_BODY; 
        // Condition is true, i.e. inlcude body
        return EVAL_BODY_INCLUDE;
    }

    @Override
    public int doEndTag()
        throws JspException
    {   // Nothing to do
        return EVAL_PAGE;
    }
}
