/*
 * ESTEAM Software GmbH, 01.07.2007
 */
package org.apache.empire.struts2.jsp.tags;

import java.util.Stack;

import javax.servlet.jsp.JspException;

import org.apache.empire.struts2.html.HtmlTagDictionary;
import org.apache.empire.struts2.html.HtmlWriter;
import org.apache.empire.struts2.html.HtmlWriter.HtmlTag;


@SuppressWarnings("serial")
public class MenuItemTag extends LinkTag
{
    protected String menuId;
    protected Object expanded;

    @Override
    protected void resetParams()
    {
        menuId = null;
        expanded = null;
        // done
        super.resetParams();
    }

    @Override
    public int doStartTag() throws JspException
    {
        // check visibility
        if (getBoolean(visible, true)==false)
        {   // not visible
            return SKIP_BODY;
        }
        // MenuInfo
        MenuTag.MenuInfo mi = getMenuInfo();
        boolean current = isCurrent(mi);
        // HtmlWriter
        HtmlTagDictionary dic = HtmlTagDictionary.getInstance();
        HtmlWriter w = new HtmlWriter(pageContext.getOut());
        HtmlTag wrap = w.startTag("li");
        addStandardAttributes(wrap, null);
        this.cssClass = getCssClass(mi, current);
        this.cssStyle = null;
        wrap.beginBody();
        // The Anchors
        if (item== null && mi.actionItem!=null)
            item = mi.actionItem;
        // OnClick
        if (onclick== null)
            onclick = dic.MenuItemLinkDefaultOnClickScript();
        // Call base class
        int result = super.doStartTag();
        // Check expanded
        return (isExpanded(mi) ? result : SKIP_BODY);  
    }
    
    @Override
    public int doEndTag() throws JspException
    {
        // Check visibility
        if (getBoolean(visible, true)==false)
        {   // Not visible
            if (autoResetParams)
                resetParams();
            return EVAL_PAGE;
        }    
        // Get body
        String body = getBody();
        setBodyContent(null);
        // End tag
        int result = super.doEndTag();
        // HtmlWriter
        HtmlWriter w = new HtmlWriter(pageContext.getOut());
        HtmlTag wrap = w.continueTag("li", true);
        wrap.endTag(body);
        // Done
        return result;
    }

    @SuppressWarnings("unchecked")
    private MenuTag.MenuInfo getMenuInfo()
    {
        Stack<MenuTag.MenuInfo> stack = (Stack<MenuTag.MenuInfo>)pageContext.getAttribute(MenuTag.MENU_STACK_ATTRIBUTE);
        return (stack!= null) ? stack.peek() : null;
    }
    
    private boolean isCurrent(MenuTag.MenuInfo mi)
    {
        if (mi==null || mi.currentId==null)
            return false;
        String thisMenu = (menuId!=null) ? menuId : id;
        return (thisMenu!=null) ? thisMenu.equals(mi.currentId) : false; 
    }
    
    private boolean isExpanded(MenuTag.MenuInfo mi)
    {
        if (expanded==null)
            return true;
        // autoexpand if current 
        if (expanded instanceof String && "auto".equalsIgnoreCase((String)expanded))
            return isCurrent(mi);
        // get boolean
        return getBoolean(expanded, true);
    }
    
    private String getCssClass(MenuTag.MenuInfo mi, boolean current)
    {
        if (mi==null)
            return null;
        // Check disabled
        if (getBoolean(disabled, false))
            return mi.disabledClass;
        // Check Current
        if (current)
            return mi.currentClass;
        // Check Expanded
        if (expanded!=null && isExpanded(mi))
            return mi.expandedClass;
        // Default: Enabled Class
        return mi.enabledClass;
    }
    
    // -------------------------------- Property accessors -----------------------------

    public void setMenuId(String menuId)
    {
        this.menuId = menuId;
    }

    public void setExpanded(Object expanded)
    {
        this.expanded = expanded;
    }
}
