package org.apache.empire.struts2.jsp.tags;

import java.io.Writer;
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.empire.db.DBReader;
import org.apache.struts2.components.Component;
import org.apache.struts2.components.IteratorComponent;
import org.apache.struts2.util.MakeIterator;
import org.apache.struts2.views.jsp.IteratorStatus;
import org.apache.struts2.views.jsp.IteratorTag;

import com.opensymphony.xwork2.util.ValueStack;


@SuppressWarnings("serial")
public class ItemListTag extends IteratorTag
{
    // Logger
    protected static Log log = LogFactory.getLog(ItemListTag.class);

    public static class ItemIteratorComponent extends IteratorComponent
    {
        private PageContext pageContext;
        private boolean isReader;
        private Object oldSource;
        private Object srcObject;

        public ItemIteratorComponent(ValueStack stack, PageContext pageContext, Object source, Iterator iterator)
        {
            super(stack);
            // set iterator
            this.pageContext = pageContext;
            this.iterator = iterator;
            this.isReader =(source instanceof DBReader);
            this.srcObject = source;
            // Store Reader on Page Context
            if (isReader)
            {
                oldSource = pageContext.getAttribute(EmpireValueTagSupport.RECORD_ATTRIBUTE);
                pageContext.setAttribute(EmpireValueTagSupport.RECORD_ATTRIBUTE, source);
            }
            else
            {
                oldSource = pageContext.getAttribute(EmpireValueTagSupport.BEAN_ITEM_ATTRIBUTE);
                pageContext.setAttribute(EmpireValueTagSupport.BEAN_ITEM_ATTRIBUTE, null);
            }
        }        
        
        public void dispose()
        {
            String pageAttribute;
            if (isReader)
            {   // Close Reader
                ((DBReader)srcObject).close();
                pageAttribute = EmpireValueTagSupport.RECORD_ATTRIBUTE; 
            }
            else
            {   // Bean List
                pageAttribute = EmpireValueTagSupport.BEAN_ITEM_ATTRIBUTE; 
            }
            // Remove Source
            if (oldSource!=null)
                pageContext.setAttribute(pageAttribute, oldSource);
            else
                pageContext.removeAttribute(pageAttribute);
            // done
            pageContext = null;
            oldSource = null;
            srcObject = null;
        }

        @SuppressWarnings("unchecked")
        @Override
        public boolean start(Writer writer)
        {
            if (iterator==null)
            {
                log.error("No Iterator for ItemListTag supplied. Ignoring body");
                return false;
            }    
            
            // Create an iterator status if the status attribute was set.
            if (statusAttr != null) {
                statusState = new IteratorStatus.StatusState();
                status = new IteratorStatus(statusState);
            }

            // we don't need this
            /*
            if (value == null) {
                value = "top";
            }
            iterator = MakeIterator.convert(findValue(value));
            */

            // get the first
            ValueStack stack = getStack();
            if ((iterator != null) && iterator.hasNext())
            {
                Object currentValue = iterator.next();
                if (currentValue!=null)
                {
                    stack.push(currentValue);
                    String id = getId();

                    if ((id != null))
                    {
                        //pageContext.setAttribute(id, currentValue);
                        //pageContext.setAttribute(id, currentValue, PageContext.REQUEST_SCOPE);
                        stack.getContext().put(id, currentValue);
                    }

                    // Status object
                    if (statusAttr != null)
                    {
                        statusState.setLast((isReader==false) ? !iterator.hasNext() : false); 
                        oldStatus = stack.getContext().get(statusAttr);
                        stack.getContext().put(statusAttr, status);
                    }

                    // Set current Value
                    if (isReader==false)
                        pageContext.setAttribute(EmpireValueTagSupport.BEAN_ITEM_ATTRIBUTE, currentValue);
                    
                    // Return with valid iterator
                    return true;
                }
            }
            // Make sure iterator is set to null!
            iterator = null;
            // Not more Records
            super.end(writer, "");
            return false;
        }

        @SuppressWarnings("unchecked")
        @Override
        public boolean end(Writer writer, String body)
        {
            if (isReader)
            {   // A reader list
                IteratorStatus tmpStatus = status;
                status = null;
                // Iterate
                boolean result = super.end(writer, body);
                status = tmpStatus;
                if (status!=null)
                {
                    if (result==true)
                    {
                        statusState.next(); // Increase counter
                        // statusState.setLast(!iterator.hasNext());
                    }
                    else 
                    {
                        if (oldStatus == null) {
                            stack.getContext().put(statusAttr, null);
                        } else {
                            stack.getContext().put(statusAttr, oldStatus);
                        }
                    }
                }
                // done
                return result;
            }
            else
            {   // A bean list
                boolean result = super.end(writer, body);
                if (result)
                {   // Set current Value
                    ValueStack stack = getStack();
                    pageContext.setAttribute(EmpireValueTagSupport.BEAN_ITEM_ATTRIBUTE, stack.peek());
                }
                return result;
            }
        }    
        
    }

    // Properties
    private Object source;
    private Object maxItems;
    private String property;

    // Temporay
    private Object sourceObject;     
    private Object oldProperty;
    private Object oldStatusAttr;

    /*
     * Constructor
     */
    public ItemListTag()
    {
        // Default constructor
    }

    /*
     * Clears all params since tag is reused
     */
    public void resetParams()
    {
        // Optional
        source = null;
        maxItems = null;
        statusAttr = null;
        sourceObject = null;
    }
    
    @Override
    public Component getBean(ValueStack stack, HttpServletRequest req, HttpServletResponse res)
    {
        // Make Iterator
        Iterator iterator = getIterator(sourceObject);
        // Create Compontent
        return new ItemIteratorComponent(stack, pageContext, sourceObject, iterator);
    }

    @Override
    public int doStartTag() throws JspException
    {
        // Reader
        sourceObject = getSourceObject(source);
        if (source==null)
        {   log.error("No reader or beanlist supplied for e:list tag. Skipping body.");
            property = null;
            return SKIP_BODY;
        }
        // Set Status Attribute Name and store it on page context
        oldStatusAttr = pageContext.getAttribute(TableRowTag.ITERATOR_STATUS_ATTRIBUTE);
        if (oldStatusAttr!=null)
            statusAttr = oldStatusAttr.toString() + "Nested";
        else
            statusAttr = "listStatusAttrName";
        pageContext.setAttribute(TableRowTag.ITERATOR_STATUS_ATTRIBUTE, statusAttr);
        // Parent Property
        if (property!= null)
        {
            oldProperty = pageContext.getAttribute(EmpireValueTagSupport.PARENT_PROPERTY_ATTRIBUTE);
            pageContext.setAttribute(EmpireValueTagSupport.PARENT_PROPERTY_ATTRIBUTE, property);
        }
        // do Start
        return super.doStartTag();
    }

    @Override
    public int doEndTag() throws JspException
    {
        // Parent Property
        if (property!= null)
            removePageAttribute(EmpireValueTagSupport.PARENT_PROPERTY_ATTRIBUTE, oldProperty);
        oldProperty = null;
        // Status Attribute
        if (statusAttr!= null)
            removePageAttribute(TableRowTag.ITERATOR_STATUS_ATTRIBUTE, oldStatusAttr);
        oldStatusAttr = null;
        // Remove Component
        if (component!=null)
           ((ItemIteratorComponent)component).dispose();
        // End
        resetParams();
        return super.doEndTag();
    }

    // ------- Helpers -------

    private void removePageAttribute(String name, Object oldValue)
    {
        if ( oldValue!=null )
             pageContext.setAttribute(name, oldValue);
        else pageContext.removeAttribute(name);
    }
    
    private Object getSourceObject(Object value)
    {
        // Check String
        if ((value instanceof String))
        {
            // Check Value
            String strval = value.toString();
            if (strval.startsWith("%{") && strval.endsWith("}"))
            { // OGNL
                strval = strval.substring(2, strval.length() - 1);
                value = getStack().findValue(strval);
            }
            else if (strval.startsWith("#"))
            { // OGNL
                value = getStack().findValue(strval);
            }
            else if (strval.startsWith("$"))
            { // Attribute on page, request, session or application (in this order)
                strval = strval.substring(1);
                if (strval.startsWith("$") == false)
                    value = getAttribute(strval);
                if (value == null)
                    return null;
            }
        }
        return value;
    }
    
    private Iterator getIterator(Object value)
    {
        // 
        if (value instanceof DBReader)
        {
            // Create Component
            int maxCount = getIntValue(maxItems, -1);
            return ((DBReader)value).iterator(maxCount);
        }
        // Error
        if (MakeIterator.isIterable(value)==false)
        {
            log.error("Cannot make an iterator of class " + value.getClass().getName());
            return null;
        }
        // Done
        return MakeIterator.convert(value);
    }
    
    private int getIntValue(Object value, int defValue)
    {
        if (value==null)
            return defValue;
        if (value instanceof String)
        {
            // Check Value
            String strval = value.toString();
            if (strval.startsWith("%{") && strval.endsWith("}"))
            { // OGNL
                strval = strval.substring(2, strval.length() - 1);
                value = getStack().findValue(strval, java.lang.Integer.class);
            }
            if (strval.startsWith("#"))
            { // OGNL
                value = getStack().findValue(strval, java.lang.Integer.class);
            }
            if (strval.startsWith("$"))
            { // Attribute on page, request, session or application (in this order)
                strval = strval.substring(1);
                if (strval.startsWith("$") == false)
                    value = getAttribute(strval);
                if (value == null)
                    return defValue;
            }
        }
        // Check Type
        if (value instanceof Integer)
        {
            return ((Integer)value).intValue();
        }
        // Parse String
        try 
        {   // Get Integer from String   
            return Integer.parseInt(value.toString());
            
        } catch(Exception e) {
            // failed to convert
            log.error("getIntFromString: given value is not a number!");
            return defValue;
        }
    }

    protected Object getAttribute(String attribute)
    {
        // Try Page
        Object obj = pageContext.getAttribute(attribute);
        if (obj != null)
            return obj;
        // Try Request
        obj = pageContext.getRequest().getAttribute(attribute);
        if (obj != null)
            return obj;
        // Try Session
        obj = pageContext.getSession().getAttribute(attribute);
        if (obj != null)
            return obj;
        // Try Application
        return pageContext.getServletContext().getAttribute(attribute);
    }
    
    // ------- Property accessors -------

    public void setSource(Object source)
    {
        this.source = source;
    }

    public void setMaxItems(Object maxItems)
    {
        this.maxItems = maxItems;
    }

    public void setProperty(String property)
    {
        this.property = property;
    }

}
