/*
 * ESTEAM Software GmbH, 02.07.2007
 */
package org.apache.empire.struts2.jsp.tags;

import java.util.Stack;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;

import org.apache.empire.struts2.html.HtmlTagDictionary;
import org.apache.empire.struts2.html.HtmlWriter;
import org.apache.empire.struts2.html.HtmlWriter.HtmlTag;
import org.apache.struts2.components.Component;
import com.opensymphony.xwork2.util.ValueStack;


@SuppressWarnings("serial")
public class MenuTag extends EmpireTagSupport
{
    public static final String MENU_STACK_ATTRIBUTE = "MenuTag.MenuInfo";
    
    public static class MenuInfo
    {
        public String currentId;
        public String currentClass;
        public String enabledClass;
        public String disabledClass;
        public String expandedClass;
        public String actionItem;
    }
    
    // Item Information 
    protected String currentItem;
    protected String currentClass;
    protected String enabledClass;
    protected String disabledClass;
    protected String expandedClass;
    protected String actionItem;

    /*
     * Clears all params since tag is reused
     */
    @Override
    protected void resetParams()
    {
        // Item Information
        currentItem = null;
        currentClass = null;
        enabledClass = null;
        disabledClass = null;
        expandedClass = null;
        // reset
        super.resetParams();
    }
    
    @Override
    public Component getBean(ValueStack arg0, HttpServletRequest arg1, HttpServletResponse arg2)
    {
        return null;
    }
    
    @Override
    protected void populateParams()
    {
        log.error("Illegal Method call");
    }

    @SuppressWarnings("unchecked")
    @Override
    public int doStartTag() throws JspException
    {
        // Tabel cell tag
        HtmlTagDictionary dic = HtmlTagDictionary.getInstance();
        HtmlWriter w = new HtmlWriter(pageContext.getOut());
        HtmlTag menu = w.startTag(dic.MenuTag());
        addStandardAttributes(menu, null);
        menu.beginBody(true);
        // Create Menu Item Info
        MenuInfo mi = new MenuInfo();
        // Get Stack 
        Stack<MenuInfo> stack = (Stack<MenuInfo>)pageContext.getAttribute(MENU_STACK_ATTRIBUTE);
        if (stack!=null)
        {   // Nested Menu's
            MenuInfo parent = stack.peek(); 
            mi.currentId = getString( currentItem, parent.currentId );
            mi.currentClass = getString (currentClass, parent.currentClass);
            mi.enabledClass = getString (enabledClass, parent.enabledClass);
            mi.disabledClass = getString (disabledClass, parent.disabledClass);
            mi.expandedClass = getString (expandedClass, parent.expandedClass); 
            mi.actionItem = getString (actionItem, parent.actionItem);
        }
        else
        {   // A new Menu
            stack = new Stack<MenuInfo>();
            pageContext.setAttribute(MENU_STACK_ATTRIBUTE, stack);
            // Init Menu Info
            mi.currentId = getString( currentItem, null );
            mi.currentClass = getString (currentClass, dic.MenuItemCurrentClass());
            mi.enabledClass = getString (enabledClass, dic.MenuItemLinkClass());
            mi.disabledClass = getString (disabledClass, dic.MenuItemDisabledClass());
            mi.expandedClass = getString (expandedClass, dic.MenuItemExpandedClass()); 
            mi.actionItem = getString (actionItem, null);
        }
        // Add to Stack
        stack.push(mi);
        // boolean usesBody = true;
        // return usesBody ? EVAL_BODY_BUFFERED : EVAL_BODY_INCLUDE;
        return EVAL_BODY_INCLUDE;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public int doEndTag() throws JspException
    {
        // Pop Menu Info
        Stack<MenuInfo> stack = (Stack<MenuInfo>)pageContext.getAttribute(MENU_STACK_ATTRIBUTE);
        if (stack!= null)
        {   // Pop Menu Info From Stack
            stack.pop();
            if (stack.isEmpty())
                pageContext.removeAttribute(MENU_STACK_ATTRIBUTE);
        }
        // Write End Tag
        HtmlTagDictionary dic = HtmlTagDictionary.getInstance();
        HtmlWriter w = new HtmlWriter(pageContext.getOut());
        HtmlTag menu = w.continueTag (dic.MenuTag(), true);
        menu.endTag();
        // done
        resetParams();
        return EVAL_PAGE;
    }

    // -------------------------------- Property accessors -----------------------------

    public void setCurrentItem(String currentId)
    {
        this.currentItem = currentId;
    }

    public void setCurrentClass(String currentClass)
    {
        this.currentClass = currentClass;
    }

    public void setDisabledClass(String disabledClass)
    {
        this.disabledClass = disabledClass;
    }

    public void setExpandedClass(String expandedClass)
    {
        this.expandedClass = expandedClass;
    }

    public void setEnabledClass(String enabledClass)
    {
        this.enabledClass = enabledClass;
    }

    public void setActionItem(String actionItem)
    {
        this.actionItem = actionItem;
    }

}
