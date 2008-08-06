/*
 * ESTEAM Software GmbH, 06.07.2007
 */
package org.apache.empire.struts2.jsp.tags;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyTagSupport;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.empire.struts2.html.HtmlTagDictionary;


@SuppressWarnings("serial")
public class FloatClearTag extends BodyTagSupport
{
    // Logger
    protected static Log log = LogFactory.getLog(FloatClearTag.class);
    
    @Override
    public int doStartTag()
        throws JspException
    {
        // super.doStartTag();
        // Write the float clear statement
        HtmlTagDictionary dic = HtmlTagDictionary.getInstance();
        String clear = dic.FloatClear();
        if (clear!=null)
        {   // Print the clear statement
            try {
                pageContext.getOut().print(clear);
            } catch (Exception e) {
                log.error("Unable to write to output stream.", e);
            }
        }
        // done, no body!
        return SKIP_BODY; 
    }

    @Override
    public int doEndTag()
        throws JspException
    {
        // return super.doEndTag();
        return EVAL_PAGE;
    }
}
