/*
 * ESTEAM Software GmbH, 11.07.2007
 */
package org.apache.empire.struts2.jsp.tags;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;

import org.apache.empire.struts2.html.HtmlTagDictionary;
import org.apache.empire.struts2.html.HtmlWriter;
import org.apache.empire.struts2.html.HtmlTagDictionary.FlexDivRenderInfo;
import org.apache.empire.struts2.html.HtmlWriter.HtmlTag;
import org.apache.struts2.components.Component;

import com.opensymphony.xwork2.util.ValueStack;


@SuppressWarnings("serial")
public class FlexDivTag extends EmpireTagSupport
{
    // Type of tag
    protected String type;
    
    // Temporary
    FlexDivRenderInfo flexDivRenderInfo = null;
    
    @Override
    protected void resetParams()
    {
        type = null;
        // reset
        super.resetParams();
    }

    @Override
    public Component getBean(ValueStack stack, HttpServletRequest req, HttpServletResponse res)
    {
        log.fatal("No Bean available for flex-tag");
        return null;
    }

    @Override
    public int doStartTag()
        throws JspException
    {
        String userAgent = getUserAgent();
        // User-Agent
        HtmlTagDictionary dic = HtmlTagDictionary.getInstance();
        flexDivRenderInfo = dic.FlexDivTag(type, userAgent);
        if (flexDivRenderInfo!=null)
        {   // Render Flex Div
            HtmlWriter w = new HtmlWriter(pageContext.getOut());
            HtmlTag tag = w.startTag(flexDivRenderInfo.tag);
            tag.addAttribute("id", getId());
            tag.addAttribute("class", cssClass);
            tag.addAttributes(flexDivRenderInfo.attributes);
            tag.beginBody(flexDivRenderInfo.bodyBegin);
        }
        // return super.doStartTag();
        return EVAL_BODY_INCLUDE;
    }

    @Override
    public int doEndTag()
        throws JspException
    {
        // Render End Tag
        if (flexDivRenderInfo!=null)
        {   // End flexible Tag
            HtmlWriter w = new HtmlWriter(pageContext.getOut());
            HtmlTag tag = w.continueTag(flexDivRenderInfo.tag, true);
            tag.endTag(flexDivRenderInfo.bodyEnd);
            flexDivRenderInfo = null;
        }
        // return super.doEndTag();
        resetParams();
        return EVAL_PAGE;
    }
    
    private String getUserAgent()
    {
        ServletRequest req = pageContext.getRequest();
        if (req instanceof HttpServletRequest)
            return ((HttpServletRequest)req).getHeader("User-Agent");
        // Not detectable
        return "";
    }

    public void setType(String type)
    {
        this.type = type;
    }

}
