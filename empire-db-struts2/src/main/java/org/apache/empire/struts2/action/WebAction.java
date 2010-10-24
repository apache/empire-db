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
package org.apache.empire.struts2.action;

import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.empire.commons.ErrorInfo;
import org.apache.empire.commons.ErrorObject;
import org.apache.empire.commons.ErrorType;
import org.apache.empire.commons.Errors;
import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.commons.StringUtils;
import org.apache.empire.data.Column;
import org.apache.empire.struts2.actionsupport.ActionBase;
import org.apache.empire.struts2.actionsupport.ActionError;
import org.apache.empire.struts2.actionsupport.ActionPropertySupport;
import org.apache.empire.struts2.actionsupport.TextProviderActionSupport;
import org.apache.empire.struts2.web.EmpireThreadManager;
import org.apache.empire.struts2.web.UrlHelperEx;
import org.apache.empire.struts2.web.WebErrors;
import org.apache.empire.struts2.web.WebRequest;

import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.TextProvider;
import com.opensymphony.xwork2.util.ValueStack;


/**
 * WebAction should be used as the superclass for all your struts2 action classes.<BR>
 * It implements necessary interfaces for parameter-, translation- and error-handling.<BR>
 */
public abstract class WebAction extends ActionBase
    implements Disposable, ExceptionAware, ActionAccessValidator, ActionErrorProvider, TextProvider
    // implements Action, Validateable, ValidationAware, ValidationAware, TextProvider, LocaleProvider, Serializable
{   
    // Logger
    @SuppressWarnings("hiding")
    protected static Log log = LogFactory.getLog(WebAction.class);

    // Default Name for Item param
    public static String DEFAULT_ITEM_PROPERTY_NAME = "item";

    public static String PORTLET_ACTION_RESULT = "struts.portlet.actionResult";
    
    private final transient TextProvider textProvider = TextProviderActionSupport.getInstance(getClass(), this); // new TextProviderFactory().createInstance(getClass(), this);

    private ActionPropertySupport itemProperty = new ActionPropertySupport(this, DEFAULT_ITEM_PROPERTY_NAME, true);

    // ------- Implementation of Disposable interface ------- 
    
    /**
     * Initialize the action
     * 
     * Should be used instead of the constructor to initialize the action
     * The method is called from the ActionBasicsInterceptor
     */
    public void init()
    {
        // Avoid the constructor for initialization and use init instead
        // this is called by the ActionBasicsInterceptor
    }
    
    /**
     * Cleanup resources allocated by the action
     * 
     * this requires the action to be stored on the request in a param named "action"
     * the ActionBasicsInterceptor performs this task
     * the dispose method will then be called from the EmpireStrutsDispatcher
     */
    public int dispose()
    {
        if (hasActionError())
            return EXITCODE_ERROR; 
        // Cleanup any resouces
        return EXITCODE_SUCCESS;
    }

    // ------- Implementation of ExceptionAware interface ------- 
    
    /**
     * handle any exception that may have occurred
     * The method is called from the ActionBasicsInterceptor
     */
    public String handleException(Throwable exception, String method)
    {
        // Uncaught exception
        ActionError excetionError = new ActionError(exception);
        // Check if there already is an error
        if (actionError!=null && actionError.hasError())
        {   // War replace
            log.warn("An uncaught exception occurred after an error has already been set!"); 
            log.warn("Replacing error of " + actionError.getErrorMessage() + " with " + excetionError.getErrorMessage()); 
        }
        else
        {   log.warn("An uncaught exception occurred. Message is " + excetionError.getErrorMessage()); 
        }
        // uncaught exception
        setActionError(excetionError);
        // retrun error mapping
        return null; // Default Exception Handling
    }
    
    // ------- Implementation of ActionAccessValidator interface ------- 

    /**
     * Determines whether the user needs to Login to access this page
     * important: return false if the user has already logged in!
     *
     * The method is called from the ActionAccessInterceptor
     */
    public boolean loginRequired()
    {
        return false;
    }
    
    /**
     * Determines whether the user has access to this page or a particular method
     *
     * The method is called from the ActionAccessInterceptor
     */
    public boolean hasAccess(String method)
    {
        return true;
    }
    
    // ------- Action Error -------
    
    private final String LAST_ACTION_ERROR_ATTRIBUTE = "lastActionError";

    private ActionError actionError;

    /*
     * @see org.apache.empire.struts2.action.ActionErrorProvider#hasActionError()
     */
    public boolean hasActionError()
    {
        return ((actionError!=null && actionError.hasError()) || fieldErrors!=null);
    }

    /*
     * @see org.apache.empire.struts2.action.ActionErrorProvider#getActionError()
     */
    public void clearActionErrors()
    {
        actionError = null;
        fieldErrors = null;
        // Remove from Session
        ActionContext context = ActionContext.getContext();
        if (context!=null)
            context.getSession().remove(LAST_ACTION_ERROR_ATTRIBUTE);
    }

    /*
     * @see org.apache.empire.struts2.action.ActionErrorProvider#getLastActionError(boolean)
     */
    public ErrorInfo getLastActionError(boolean clear)
    {
        ActionContext context = ActionContext.getContext();
        ErrorInfo error = (ErrorInfo)context.getSession().get(LAST_ACTION_ERROR_ATTRIBUTE);
        if (clear)
            context.getSession().remove(LAST_ACTION_ERROR_ATTRIBUTE);
        return error;
    }

    /*
     * @see org.apache.empire.struts2.action.ActionErrorProvider#getLocalizedErrorMessage(org.apache.empire.commons.ErrorInfo)
     */
    public String getLocalizedErrorMessage(ErrorInfo error)
    {   // Get the message
        if (error==null || !error.hasError())
            return "";
        // Translate the error
        String msgKey = error.getErrorType().getKey();
        String[] args = ObjectUtils.toStringArray(error.getErrorParams(), "Null");
        return getText(msgKey, args);
    }
    
    protected void setActionError(ErrorInfo error)
    {
        // Check error param
        if (error==null)
        {   // No Error
            actionError = null;
            return; 
        }
        if (error.hasError()==false)
        {   log.warn("setActionError: No error information supplied.");
            error = new ActionError(Errors.Internal, "No error information available!");
        }
        // We have an error
        if (error instanceof ActionError)
            actionError = ((ActionError)error);
        else 
            actionError = new ActionError(error);
        // put Error on session
        ActionContext context = ActionContext.getContext();
        context.getSession().put(LAST_ACTION_ERROR_ATTRIBUTE, actionError);
    }

    protected final void setActionError(ErrorType errType)
    {
        setActionError(new ActionError(errType));
    }

    protected final void setActionError(ErrorType errType, Object param)
    {
        setActionError(new ActionError(errType, param));
    }

    protected final void setActionError(Exception exception)
    {
        setActionError(new ActionError(exception));
    }
    
    // ------- Field Errors -------
    
    private Map<String, ErrorInfo> fieldErrors;
    
    /*
     * @see org.apache.empire.struts2.action.ActionErrorProvider#getFieldErrors()
     */
    public Map<String, ErrorInfo> getItemErrors()
    {
        return fieldErrors;
    }

    private void addItemError(String item, ActionError error)
    {
        if (fieldErrors== null)
            fieldErrors = new LinkedHashMap<String, ErrorInfo>();
        // Error Message
        if (log.isWarnEnabled())
            log.warn("Invlalid value for item or field " + item + " Message= " + ErrorObject.getMessage(error));
        // Map of errors
        fieldErrors.put(item, error);
    }

    protected void addItemError(String item, ErrorType errorType, String title, ErrorInfo error)
    {   // Check error
        if (error.hasError()==false)
        {   log.error("addItemError has been called without an error provided!");
            return;
        }
        // Get Title
        if (title.startsWith("!"))
            title = getText(title.substring(1));
        // Get Message
        String msgKey = error.getErrorType().getKey();
        String[] args = ObjectUtils.toStringArray(error.getErrorParams(), "Null");
        String msg = getText(msgKey, args);
        // Get full Message
        addItemError(item, new ActionError(errorType, new String[] { title, msg }));
    }
    
    @Override
    protected void addFieldError(String name, Column column, ErrorInfo error)
    {
        addItemError(name, WebErrors.FieldError, column.getTitle(), error);
    }
    
    // ------- Action Message -------

    private final String LAST_ACTION_MESSAGE_ATTRIBUTE = "lastActionMessage";
    
    /*
     * @see org.apache.empire.struts2.action.ActionErrorProvider#getLastActionMessage(boolean)
     */
    public String getLastActionMessage(boolean clear)
    {
        ActionContext context = ActionContext.getContext();
        Object msg = context.getSession().get(LAST_ACTION_MESSAGE_ATTRIBUTE);
        if (clear)
            context.getSession().remove(LAST_ACTION_MESSAGE_ATTRIBUTE);
        return StringUtils.toString(msg);
    }
    
    protected void setActionMessage(String message)
    {   // put Message on session
        if (message.startsWith("!"))
            message = getText(message.substring(1));
        ActionContext context = ActionContext.getContext();
        context.getSession().put(LAST_ACTION_MESSAGE_ATTRIBUTE, message);
    }
    
    // ------- Locale Provider -------
    
    public Locale getLocale()
    {
        return ActionContext.getContext().getLocale();
    }

    // ------- Text Provider -------
    
    /*
     * UPGRADE-struts 2.1.6
     * CHANGE: added method "hasKey(String key)"
     * Reason: The interface com.opensymphony.xwork2.config.entities.Parameterizable changed
     */
    public boolean hasKey(String key) {
    	return textProvider.hasKey(key);
    }
    
    public String getText(String aTextName) {
        return textProvider.getText(aTextName);
    }

    public String getText(String aTextName, String defaultValue) {
        return textProvider.getText(aTextName, defaultValue);
    }

    public String getText(String aTextName, String defaultValue, String obj) {
        return textProvider.getText(aTextName, defaultValue, obj);
    }

    public String getText(String aTextName, List<Object> args) {
        return textProvider.getText(aTextName, args);
    }

    public String getText(String key, String[] args) {
        return textProvider.getText(key, args);
    }

    public String getText(String aTextName, String defaultValue, List<Object> args) {
        return textProvider.getText(aTextName, defaultValue, args);
    }

    public String getText(String key, String defaultValue, String[] args) {
        return textProvider.getText(key, defaultValue, args);
    }

    public String getText(String key, String defaultValue, List<Object> args, ValueStack stack) {
        return textProvider.getText(key, defaultValue, args, stack);
    }

    public String getText(String key, String defaultValue, String[] args, ValueStack stack) {
        return textProvider.getText(key, defaultValue, args, stack);
    }

    public ResourceBundle getTexts() {
        return textProvider.getTexts();
    }

    public ResourceBundle getTexts(String aBundleName) {
        return textProvider.getTexts(aBundleName);
    }
    
    // ------- Special -------
    
    @Override
    public int getListPageSize()
    {
        return -1; // Infinite List Size
    }
    
    @Override
    protected Connection getConnection() 
    {
        return null; 
    }

    // ------- ActionItem Property -------
    
    public String getItemPropertyName()
    {
        return itemProperty.getName();
    }

    public String getItem()
    {   // Get Item from request?
        return itemProperty.getValue();
    }

    public void setItem(String value)
    {   // Set Session Item
        itemProperty.setValue(value);
    }
    
    public void clearItem()
    {
        itemProperty.clear();
    }

    public boolean isNewItem()
    {   
        return getItemNewFlag();
    }
    
    protected final boolean hasItem(boolean lookOnSession)
    {
        return itemProperty.hasValue(lookOnSession);
    }
    
    protected final Object[] getItemKey()
    {
        return getRecordKeyFromString( getItem() );
    }

    protected final boolean getItemNewFlag()
    {
        return getRecordNewFlagFromString( getItem() );
    }  
    
    // ------- Request Param accessors -------
    
    public final Map getRequestParameters()
    {
        ActionContext context = ActionContext.getContext();
        return (context!=null) ? context.getParameters() : null;
    }

    public final boolean hasRequestParam(String param)
    {
        return (getRequestParam(param)!=null);
    }
    
    public final String getRequestParam(String param)
    {
        Map params = getRequestParameters();
        Object value = params.get( param );
        // Is the error provided?
        if (value==null)
            return null;
        // Check if it is a String-Array 
        if (value instanceof String[])
            return ((String[])value)[0];
        // convert to string 
        return value.toString();
    }
    
    public final String[] getRequestArrayParam(String param)
    {
        Map params = getRequestParameters();
        Object value = params.get( param );
        if (value==null)
            return null; // null is default
        // Check if it is a String-Array 
        if (value instanceof String[])
            return (String[])value;
        // Create new String array
        return new String[] { value.toString() };
    }

    public final Integer getRequestIntParam(String param)
    {
        String s = getRequestParam(param);
        if (s==null)
            return 0; // not found
        return Integer.parseInt(s);
    }
    
    @SuppressWarnings("unchecked")
    public final void putRequestParam(String name, String value)
    {
        Map params = getRequestParameters();
        if (value!=null)
            params.put(name, new String[] { value });
        else
            params.remove(name);
    }

    // ------- ActionParam accessors -------
    
    protected String getActionParam(String name)
    {
        // If name is null, then use WebAction-Item
        if (name==null || name.length()==0)
        {   log.error("Invalid value for parameter 'name'");
            return null;
        }
        // Find Item on Request
        String item = getRequestParam(name);
        // Check if item is supplied
        if (item==null)
            return StringUtils.toString(getActionObject(name));
        // Set Session Item
        putActionObject(name, item);
        return item;
    }

    // ------- URL generator -------

    /**
     * returns the url for an action.
     * Waring: The following function may only use in a Servlet environment.
     * @deprecated
     */
    @Deprecated
    public String getActionURL(String action, Map parameters)
    {
        Object request = EmpireThreadManager.getCurrentRequest();
        if ((request instanceof WebRequest)==false)
        {
            log.error("cannot determine action URL. Request object does not implement WebRequest");
            return null;
        }
        // We have a webRequest    
        WebRequest webRequest = (WebRequest)request;
        // Get the uri
        String uri = "/" + action;
        if (uri.indexOf('.')<0)
            uri += ".action";
        // now build the url
        return UrlHelperEx.buildUrl(uri, webRequest.getRequestContext(), webRequest.getResponseContext(), parameters, null, true, true);
    }
    
    // ----------- Portlet --------------
    
    public String renderPortlet()
    {
    	Map<String, Object> sessionMap = ActionContext.getContext().getSession();
    	Object result = sessionMap.get(PORTLET_ACTION_RESULT);
    	if (log.isDebugEnabled())
	    	log.debug("Processing portlet render result with result=" + result);
    	return result.toString();
    }
    
}
