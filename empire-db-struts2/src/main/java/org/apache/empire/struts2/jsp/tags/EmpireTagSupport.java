/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.empire.struts2.jsp.tags;

import java.lang.reflect.InvocationTargetException;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;

import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.beanutils.PropertyUtilsBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.empire.commons.StringUtils;
import org.apache.empire.struts2.action.ActionItemProperty;
import org.apache.empire.struts2.html.HtmlWriter.HtmlTag;
import org.apache.struts2.components.UIBean;
import org.apache.struts2.views.jsp.ComponentTagSupport;
import org.apache.struts2.views.util.UrlHelper;

import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.LocaleProvider;
import com.opensymphony.xwork2.TextProvider;

@SuppressWarnings("serial")
public abstract class EmpireTagSupport extends ComponentTagSupport
{
    // Logger
    protected static Logger log = LoggerFactory.getLogger(EmpireTagSupport.class);

    protected boolean    autoResetParams = true;

    protected String     cssClass;
    protected String     cssStyle;

    protected void resetParams()
    {
        this.id = null;
        this.cssClass = null;
        this.cssStyle = null;
    }

    public final void setCssClass(String cssClass)
    {
        this.cssClass = cssClass;
    }

    public final void setCssStyle(String cssStyle)
    {
        this.cssStyle = cssStyle;
    }

    @Override
    public final void setId(String id)
    {
        super.setId(id);
    }

    @Override
    public String getId()
    {
        return getString(this.id);
    }
    
    @Override
    protected void populateParams()
    {
        super.populateParams();

        // Common UI Properties
        if (component instanceof UIBean)
        {
            ((UIBean) component).setCssClass(cssClass);
            ((UIBean) component).setCssStyle(cssStyle);
        }
    }

    @Override
    public int doEndTag()
        throws JspException
    {
        if (autoResetParams)
            resetParams();
        // Render now
        return super.doEndTag();
    }

    @Override
    public void release()
    {
        log.debug("Releasing tag " + getClass().getName());
        resetParams();
        super.release();
    }

    // ------- Standard Attributes -------

    protected void addStandardAttributes(HtmlTag tag, String defaultClass)
    {
        // HtmlTag div = w.startTag("div");
        tag.addAttribute("id", getId());
        tag.addAttribute("class", str(cssClass, defaultClass));
        tag.addAttribute("style", cssStyle);
        cssClass=null;
        cssStyle=null;
        setId(null);
    }
    
    // ------- Misc -------
    /*
    private String getUrl(String action)
    {
        // Set onclick 
        checkAction(action);
        // Temporarily create Anchor component
        AnchorComponent anchor = new AnchorComponent(getStack(), (HttpServletRequest) pageContext.getRequest(), (HttpServletResponse) pageContext.getResponse());
        Container container = Dispatcher.getInstance().getContainer();
        container.inject(anchor);
        String url = anchor.getUrl(action);
        anchor.end(null, null);
        // Set Onlick
        return url;
    }
    */
    
    protected Object getAction()
    {
        try {
            return ActionContext.getContext().getActionInvocation().getAction();
        } catch (Exception e) {
            log.error("Unable to detect Action. Action Invocation not available!");
            return "";
        }
    }
    
    protected String getActionName()
    {
        try {
            return ActionContext.getContext().getActionInvocation().getProxy().getActionName();
        } catch (Exception e) {
            log.error("Unable to detect Action name. Action Invocation Proxy not available!");
            return "";
        }
    }
    
    protected String getActionItemPropertyName()
    {
        Object action = getAction();
        if (action instanceof ActionItemProperty)
            return ((ActionItemProperty)action).getItemPropertyName();
        // Default is id
        return "item";
    }
    
    // ------- URL generator -------

    protected String getActionURL(String action, Map parameters)
    {
        HttpServletRequest httpRequest = (HttpServletRequest) pageContext.getRequest();        
        HttpServletResponse httpResponse = (HttpServletResponse) pageContext.getResponse();        
        // Get the uri
        String uri = "/" + checkAction(action);
        if (uri.indexOf('.')<0)
            uri += ".action";
        // now build the url
        return UrlHelper.buildUrl(uri, httpRequest, httpResponse, parameters, null, true, true);
    }
    
    // ------- Page Attribute Helpers ------

    protected Object putPageAttribute(String name, Object value)
    {
        // Store Attribute on Page Scope
        Object oldValue = pageContext.getAttribute(name);
        pageContext.setAttribute(name, value);
        return oldValue;
    }

    protected void removePageAttribute(String name, Object oldValue)
    {
        if ( oldValue!=null )
             pageContext.setAttribute(name, oldValue);
        else pageContext.removeAttribute(name);
    }

    protected Object getPageAttribute(String name, Object defValue)
    {
        Object o = pageContext.getAttribute(name);
        return (o!=null) ? o : defValue;
    }

    // ------- Param Helper Methods -------

    protected String str(String value, String defValue)
    {
        return ((value!=null) ? value : defValue);
    }

    protected Object getObject(Object value, Object defValue, Class<?> asType)
    {
        if (value == null)
        {
            return defValue;
        }
        // Check String
        if ((value instanceof String))
        {
            // Check Value
            String strval = value.toString();
            if (strval.startsWith("%{") && strval.endsWith("}"))
            { // OGNL
                strval = strval.substring(2, strval.length() - 1);
                return getStack().findValue(strval, asType);
            }
            if (strval.startsWith("#"))
            { // OGNL
                return getStack().findValue(strval, asType);
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
        // Check Class
        if (asType.isInstance(value))
        { // make a cast
            return asType.cast(value);
        }
        // Error
        log.error("Cannot cast value of '" + value.toString() + " to class " + asType.toString());
        return null;
    }

    protected final Object getObject(Object value, Object defValue)
    {
        return getObject(value, defValue, Object.class);
    }
    
    protected String getString(Object value, String defValue)
    {
        if (value == null)
            return defValue;
        // Check String
        if ((value instanceof String) == false)
            return value.toString();
        // Check Value
        String strval = value.toString();
        if (strval.startsWith("%{") && strval.endsWith("}"))
        { // OGNL
            strval = strval.substring(2, strval.length() - 1);
            return (String) getStack().findValue(strval, String.class);
        }
        if (strval.startsWith("#"))
        { // OGNL
            return (String) getStack().findValue(strval, String.class);
        }
        if (strval.startsWith("$"))
        { // Attribute on page, request, session or application (in this order)
            strval = strval.substring(1);
            if (strval.startsWith("$") == false)
                return StringUtils.toString(getAttribute(strval));
        }
        if (strval.startsWith("!"))
        { // Translations
            strval = strval.substring(1);
            if (strval.startsWith("!") == false)
                return this.getTranslationFromKey(getString(strval, null));
        }
        // Just a string
        return strval;
    }

    protected final String getString(Object value)
    {
        return getString(value, null);
    }

    protected final String getString(Object value, Object defValue)
    {
        String defValStr = ((defValue!=null) ? defValue.toString() : null);
        return getString(value, defValStr);
    }
    
    protected int getInt(Object value, int defValue)
    {
        if (value == null)
            return defValue;
        if (value instanceof String)
            value = getObject(value, 0, Object.class);
        if (value instanceof Number)
            return ((Integer) value).intValue();
        // Convert
        try
        {
            return Integer.parseInt(value.toString());
        } catch (Exception e)
        {
            return defValue;
        }
    }

    protected boolean getBoolean(Object value, boolean defValue)
    {
        if (value == null)
            return defValue;
        if (value instanceof String)
            value = getObject(value, 0, Object.class);
        if (value instanceof Boolean)
            return ((Boolean) value).booleanValue();
        // Check for true or false
        String txt = String.valueOf(value);
        if (txt.equalsIgnoreCase("true"))
            return true;
        // Not boolean
        return false;
    }

    protected Object getBeanProperty(Object bean, String property)
    {
        try
        {   /*
            if (log.isTraceEnabled())
                log.trace(bean.getClass().getName() + ": setting property '" + property + "' to " + String.valueOf(value));
            */
            
            // Get Property Value
            PropertyUtilsBean pub = BeanUtilsBean.getInstance().getPropertyUtils();
            return pub.getSimpleProperty(bean, property);
            
            // Check result
            /*
             * String res = BeanUtils.getProperty(bean, property); if (res!=value && res.equals(String.valueOf(value))==false) { //
             * Property value cannot be set // (missing setter?) String msg = bean.getClass().getName() + ": unable to set
             * property '" + property + "' to " + String.valueOf(value); return error(ERR_INTERNAL, msg); } else if
             * (log.isInfoEnabled()) { log.info(bean.getClass().getName() + ": property '" + property + "' has been set to " +
             * res); }
             */
            // done

        } catch (IllegalAccessException e)
        {   log.error(bean.getClass().getName() + ": unable to set property '" + property + "'");
            return null;
        } catch (InvocationTargetException e)
        {   log.error(bean.getClass().getName() + ": unable to set property '" + property + "'");
            return null;
        } catch(NoSuchMethodException e) { 
            log.warn(bean.getClass().getName() + ": cannot check value of property '" + property + "'");
            return null;
        }
    }
    
    protected String checkAction(String action)
    {
        if (action == null )
            return null;
        // find Method separator
        int i = action.indexOf('!');
        if (i < 0)
        {   // Check for Javascript
            if (action.startsWith("javascript:")==false)
                log.warn("No link action method for for action " + action + " has been supplied! Page = " + getPageName());
        }
        else if (i==0)
        {   // Only Method name given
            return getActionName() + action;
        }
        return action;
    }

    protected String getPageName()
    {
        String page = pageContext.getPage().toString();
        int lastDot = page.lastIndexOf('.');
        int lastAt = page.indexOf('@');
        if (lastDot > 0 && lastDot < lastAt)
            page = page.substring(lastDot + 1, lastAt);
        return page;
    }

    protected Object getAttribute(String attribute)
    {
        // Try Page Context
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
    
    // ------- Translation helpers -------

    protected String getTranslation(String text)
    {
        if (text == null || text.length() == 0 || !text.startsWith("!"))
            return text;
        // If key starts with ! then return key.
        String key = text.substring(1);
        if (key.startsWith("!"))
            return key;
        // translate
        return getTranslationFromKey(key);
    }
    
    private TextProvider getTextProvider(Object action)
    {
        if (action instanceof TextProvider)
            return ((TextProvider) action);
        // Error
        return null;
    }
    
    protected Locale getLocale(Object action)
    {
        if (action instanceof LocaleProvider)
            return ((LocaleProvider) action).getLocale();
        // Use action context locale
        return ActionContext.getContext().getLocale();
    }

    protected String getTranslationFromKey(String key)
    {
        Object action = getAction();
        TextProvider tp = getTextProvider(action);
        if (tp!=null)
        {   // Text Provider found
            String locale = getLocale(action).toString();  
            String result = tp.getText(key);
            if (result==null)
            {
                if (log.isErrorEnabled())
                    log.error("No translation found for key=[" + key + "] on page " + getPageName() + " --> locale=" + locale);
            }
            else if (result.length()==0)
            {
                if (log.isInfoEnabled())
                    log.info("Translation key [" + key + "] found, but value is empty! locale=" + locale );
            }
            else return result; 
        } else
        {   // Find text provider
            log.error("No text provider available. Action does not implement TextProvider interface");
        }
        return "[" + key + "]";
    }
}
