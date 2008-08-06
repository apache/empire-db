/*
 * ESTEAM Software GmbH, 13.07.2007
 */
package org.apache.empire.struts2.jsp.tags.flow;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;

import org.apache.empire.struts2.jsp.tags.EmpireTagSupport;
import org.apache.struts2.components.Component;

import com.opensymphony.xwork2.util.ValueStack;


@SuppressWarnings("serial")
public class IfTag extends EmpireTagSupport
{
    public static final String IF_RESULT_ANSWER = "esteam.if.result";
    
    /*
    private static class IfComponent extends org.apache.struts2.components.If
    {
        public IfComponent(ValueStack stack)
        {
            super(stack);
        }
    }
    */

    public Object test;
    
    @Override
    protected void resetParams()
    {
        test=null;
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
        // Evaluate Expression if not already a boolean
        boolean result = this.getBoolean(test, true);
        test = result;
        // Tabel cell tag
        if (result==false)
            return SKIP_BODY; 
        // Condition is true, i.e. inlcude body
        return EVAL_BODY_INCLUDE;
    }

    @Override
    public int doEndTag()
        throws JspException
    {   // Put with End-Tag to allow Else being outside the if-tag
        putPageAttribute(IF_RESULT_ANSWER, test);
        test=null;
        return EVAL_PAGE;
    }

    // ------- Property accessors -------
    
    public void setTest(Object test)
    {
        this.test = test;
    }
}
