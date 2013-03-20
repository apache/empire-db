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
package org.apache.empire.jsf2.app;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import javax.el.ELContextListener;
import javax.el.ELException;
import javax.el.ELResolver;
import javax.el.ExpressionFactory;
import javax.el.ValueExpression;
import javax.faces.FacesException;
import javax.faces.application.Application;
import javax.faces.application.FacesMessage;
import javax.faces.application.FacesMessage.Severity;
import javax.faces.application.NavigationHandler;
import javax.faces.application.ProjectStage;
import javax.faces.application.Resource;
import javax.faces.application.ResourceHandler;
import javax.faces.application.StateManager;
import javax.faces.application.ViewHandler;
import javax.faces.component.NamingContainer;
import javax.faces.component.UIComponent;
import javax.faces.component.UIViewRoot;
import javax.faces.component.behavior.Behavior;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.el.MethodBinding;
import javax.faces.el.PropertyResolver;
import javax.faces.el.ReferenceSyntaxException;
import javax.faces.el.ValueBinding;
import javax.faces.el.VariableResolver;
import javax.faces.event.ActionListener;
import javax.faces.event.SystemEvent;
import javax.faces.event.SystemEventListener;
import javax.faces.validator.Validator;
import javax.servlet.ServletContext;
import javax.sql.DataSource;

import org.apache.empire.commons.StringUtils;
import org.apache.empire.data.DataType;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.exceptions.InternalException;
import org.apache.empire.exceptions.InvalidArgumentException;
import org.apache.empire.exceptions.NotSupportedException;
import org.apache.empire.jsf2.controls.TextAreaInputControl;
import org.apache.empire.jsf2.controls.TextInputControl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// import com.sun.faces.application.ApplicationImpl;

@SuppressWarnings("deprecation")
public abstract class FacesApplication extends Application
{
    private static final Logger log                   = LoggerFactory.getLogger(FacesApplication.class);

    private static final String CONNECTION_ATTRIBUTE  = "dbConnections";

    public static String        APPLICATION_ATTRIBUTE = "app";

    protected TextResolver[]    textResolvers         = null;

    private String              webRoot               = null;
    
    private Application			applImpl 			  = null;
    
    private FacesImplementation 	facesImpl			  = null;

    protected FacesApplication()
    { 	// subscribe
    	log.info("FacesApplication {0} created", getClass().getName());
    }
    
    public void setImplementation(FacesImplementation facesImpl, Application applImpl) 
    {
    	this.facesImpl = facesImpl;
    	this.applImpl  = applImpl; 
    }

	protected abstract DataSource getAppDataSource(DBDatabase db);

    protected abstract void init(ServletContext servletContext);

    protected void initComplete(ServletContext servletContext)
    {
        // Get Web Root
        webRoot = servletContext.getContextPath();

        // Check Text resolvers
        if (textResolvers == null)
            initTextResolvers();

        // done
        log.info("FacesApplication initialization complete");
    }

    /* Context handling */

    public void onChangeView(final FacesContext fc, String viewId)
    {
        // allow custom view change logic

        // clear page resources
        Map<String, Object> sm = FacesUtils.getSessionMap(fc);
        if (sm!=null)
            sm.remove(FacesUtils.PAGE_RESOURCE_MAP_ATTRIBUTE);
    }

    public void addJavascriptCall(final FacesContext fc, String function)
    {
        throw new NotSupportedException(this, "addJavascriptCall");
    }

    /**
     * return the interface for Implementation specific features 
     * that are specific for Mojarra or MyFaces
     */
    public FacesImplementation getFacesImplemenation() 
    {
		return facesImpl;
	}
    
    /**
     * returns the web context path as returned from ServletContext.getContextPath()
     */
    public String getWebRoot()
    {
        return webRoot;
    }

    /**
     * returns the active locale for a given FacesContext
     */
    public Locale getContextLocale(final FacesContext ctx)
    {
        UIViewRoot root;
        // Start out with the default locale
        Locale locale;
        Locale defaultLocale = Locale.getDefault();
        locale = defaultLocale;
        // See if this FacesContext has a ViewRoot
        if (null != (root = ctx.getViewRoot()))
        {
            // If so, ask it for its Locale
            if (null == (locale = root.getLocale()))
            {
                // If the ViewRoot has no Locale, fall back to the default.
                locale = defaultLocale;
            }
        }
        return locale;
    }

    /**
     * checks if the current context contains an error
     * @param fc the FacesContext
     * @return true if the context has an error set or false otherwise
     */
    public boolean hasError(final FacesContext fc)
    {
        Iterator<FacesMessage> msgIterator = fc.getMessages();
        if (msgIterator != null)
        { // Check Messages
            while (msgIterator.hasNext())
            { // Check Severity
                Severity fms = msgIterator.next().getSeverity();
                if (fms == FacesMessage.SEVERITY_ERROR || fms == FacesMessage.SEVERITY_FATAL)
                    return true;
            }
        }
        return false;
    }

    /**
     * returns true if a form input element has been partially submitted
     * @param fc the Faces Context
     * @return the componentId or null if no partial submit was been performed
     */
    public boolean isPartialSubmit(final FacesContext fc)
    {
        // Override for your JSF component Framework. e.g. for IceFaces
        // Map<String,String> parameterMap = fc.getExternalContext().getRequestParameterMap();
        // return ObjectUtils.getBoolean(parameterMap.get("ice.submit.partial"));
        return false;
    }

    /**
     * returns the componentId for which a partial submit has been performed.
     * @param fc the Faces Context
     * @return the componentId or null if no partial submit was been performed
     */
    public String getPartialSubmitComponentId(final FacesContext fc)
    {
        // Override for your JSF component Framework. e.g. for IceFaces
        // Map<String,String> parameterMap = fc.getExternalContext().getRequestParameterMap();
        // return parameterMap.get("ice.event.captured");
        return null;
    }

    /**
     * finds the component with the given id that is located in the same NamingContainer as a given component 
     * @param fc the FacesContext
     * @param componentId the component id
     * @param nearComponent a component within the same naming container from which to start the search (optional)
     * @return the component or null if no component was found
     */
    public UIComponent findComponent(FacesContext fc, String componentId, UIComponent nearComponent)
    {
        if (StringUtils.isEmpty(componentId))
            throw new InvalidArgumentException("componentId", componentId);
        // Begin search near given component (if any)
        UIComponent component = null;
        if (nearComponent != null)
        {   // Search below the nearest naming container  
            component = nearComponent.findComponent(componentId);
            if (component == null)
            {   // Recurse upwards
                UIComponent nextParent = nearComponent;
                while (true)
                {
                    nextParent = nextParent.getParent();
                    // search NamingContainers only
                    while (nextParent != null && !(nextParent instanceof NamingContainer))
                    {
                        nextParent = nextParent.getParent();
                    }
                    if (nextParent == null)
                    {
                        break;
                    }
                    else
                    {
                        component = nextParent.findComponent(componentId);
                    }
                    if (component != null)
                    {
                        break;
                    }
                }
            }
        }
        // Not found. Search the entire tree 
        if (component == null)
            component = findChildComponent(fc.getViewRoot(), componentId);
        // done
        return component;
    }

    /**
     * finds a child component with the given id that is located below the given parent component 
     * @param parent the parent
     * @param componentId the component id
     * @return the component or null if no component was found
     */
    public static UIComponent findChildComponent(UIComponent parent, String componentId)
    {
        UIComponent component = null;
        if (parent.getChildCount() == 0)
            return null;
        Iterator<UIComponent> children = parent.getChildren().iterator();
        while (children.hasNext())
        {
            UIComponent nextChild = children.next();
            if (nextChild instanceof NamingContainer)
            {
                component = nextChild.findComponent(componentId);
            }
            if (component == null)
            {
                findChildComponent(nextChild, componentId);
            }
            if (component != null)
            {
                break;
            }
        }
        return component;
    }

    /**
     * returns the default input control type for a given data Type
     * @see org.apache.empire.jsf2.controls.InputControlManager
     * @param dataType
     * @return an Input Cnotrol type
     */
    public String getDefaultControlType(DataType dataType)
    {
        switch (dataType)
        {
            case CLOB:
                return TextAreaInputControl.NAME;
            default:
                return TextInputControl.NAME;
        }
    }

    /* Message handling */

    protected void initTextResolvers()
    {
        int count = 0;
        Iterator<Locale> locales = getSupportedLocales();
        for (count = 0; locales.hasNext(); count++)
        {
            locales.next();
        }

        // get message bundles
        String messageBundle = this.getMessageBundle();
        textResolvers = new TextResolver[count];
        locales = getSupportedLocales();
        for (int i = 0; locales.hasNext(); i++)
        {
            Locale locale = locales.next();
            textResolvers[i] = new TextResolver(ResourceBundle.getBundle(messageBundle, locale));
        }
    }

    public TextResolver getTextResolver(Locale locale)
    {
        // No text Resolvers provided
        if (textResolvers == null || textResolvers.length == 0)
        {
            throw new NotSupportedException(this, "getTextResolver");
        }
        // Lookup resolver for locale
        for (int i = 0; i < textResolvers.length; i++)
            if (locale.equals(textResolvers[i].getLocale()))
                return textResolvers[i];
        // locale not found: return default
        return textResolvers[0];
    }

    public TextResolver getTextResolver(FacesContext ctx)
    {
        return getTextResolver(getContextLocale(ctx));
    }

    /**
     * @see javax.faces.application.Application#getResourceBundle(javax.faces.context.FacesContext,
     *      String)
     */
    @Override
    public ResourceBundle getResourceBundle(final FacesContext fc, final String var)
    {
        if (var.equals("msg"))
        {
            TextResolver resolver = getTextResolver(fc);
            return resolver.getResourceBundle();
        }
        return applImpl.getResourceBundle(fc, var);
    }

    /**
     * returns a connection from the connection pool
     * 
     * @return
     */
    protected Connection getConnection(DBDatabase db)
    {
        // Get From Pool
        try
        { // Obtain a connection
            Connection conn = getAppDataSource(db).getConnection();
            conn.setAutoCommit(false);
            return conn;
        }
        catch (SQLException e)
        {
            log.error("Failed to get connection from pool.", e);
            throw new InternalException(e);
        }
    }

    /**
     * releases a connection from the connection pool
     */
    protected void releaseConnection(DBDatabase db, Connection conn, boolean commit)
    {
        try
        { // release connection
            if (conn == null)
            {
                return;
            }
            // Commit or rollback connection depending on the exit code
            if (commit)
            { // success: commit all changes
                db.commit(conn);
                log.debug("REQUEST {}: commited.");
            }
            else
            { // failure: rollback all changes
                db.rollback(conn);
                log.debug("REQUEST {}: rolled back.");
            }
            // Release Connection
            conn.close();
            // done
            if (log.isDebugEnabled())
                log.debug("REQUEST {}: returned connection to pool.");
        }
        catch (SQLException e)
        {
            log.error("Error releasing connection", e);
            e.printStackTrace();
        }
    }

    /**
     * returns a connection for the current Request
     */
    public Connection getConnectionForRequest(FacesContext fc, DBDatabase db)
    {
        if (fc == null)
            throw new InvalidArgumentException("FacesContext", fc);
        if (db == null)
            throw new InvalidArgumentException("DBDatabase", db);
        // Get Conneciton map
        @SuppressWarnings("unchecked")
        Map<DBDatabase, Connection> connMap = (Map<DBDatabase, Connection>) FacesUtils.getRequestAttribute(fc, CONNECTION_ATTRIBUTE);
        if (connMap != null && connMap.containsKey(db))
            return connMap.get(db);
        // Pooled Connection
        Connection conn = getConnection(db);
        if (conn == null)
            return null;
        // Add to map
        if (connMap == null)
        {
            connMap = new HashMap<DBDatabase, Connection>();
            FacesUtils.setRequestAttribute(fc, CONNECTION_ATTRIBUTE, connMap);
        }
        connMap.put(db, conn);
        return conn;
    }

    /**
     * releases the current request connection
     * @param fc the FacesContext
     * @param commit when true changes are committed otherwise they are rolled back
     */
    public void releaseAllConnections(final FacesContext fc, boolean commit)
    {
        @SuppressWarnings("unchecked")
        Map<DBDatabase, Connection> connMap = (Map<DBDatabase, Connection>) FacesUtils.getRequestAttribute(fc, CONNECTION_ATTRIBUTE);
        if (connMap != null)
        { // Walk the connection map
            for (Map.Entry<DBDatabase, Connection> e : connMap.entrySet())
            {
                releaseConnection(e.getKey(), e.getValue(), commit);
            }
            // remove from request map
            FacesUtils.setRequestAttribute(fc, CONNECTION_ATTRIBUTE, null);
        }
    }

    public void releaseAllConnections(final FacesContext fc)
    {
        releaseAllConnections(fc, !hasError(fc));
    }

    public void releaseConnection(final FacesContext fc, DBDatabase db, boolean commit)
    {
        @SuppressWarnings("unchecked")
        Map<DBDatabase, Connection> connMap = (Map<DBDatabase, Connection>) FacesUtils.getRequestAttribute(fc, CONNECTION_ATTRIBUTE);
        if (connMap != null && connMap.containsKey(db))
        { // Walk the connection map
            releaseConnection(db, connMap.get(db), commit);
            connMap.remove(db);
            if (connMap.size() == 0)
                FacesUtils.setRequestAttribute(fc, CONNECTION_ATTRIBUTE, null);
        }
    }

    public void releaseConnection(final FacesContext fc, DBDatabase db)
    {
        releaseConnection(fc, db, !hasError(fc));
    }
    
    /************************************************************/

	@Override
	public void addBehavior(String behaviorId, String behaviorClass) {
		// Forward to implementation
		applImpl.addBehavior(behaviorId, behaviorClass);
	}

	@Override
	public void addComponent(String componentType, String componentClass) {
		// Forward to implementation
		applImpl.addComponent(componentType, componentClass);
	}

	@Override
	public void addConverter(Class<?> targetClass, String converterClass) {
		// Forward to implementation
		applImpl.addConverter(targetClass, converterClass);
	}

	@Override
	public void addConverter(String converterId, String converterClass) {
		// Forward to implementation
		applImpl.addConverter(converterId, converterClass);
	}

	@Override
	public void addDefaultValidatorId(String validatorId) {
		// Forward to implementation
		applImpl.addDefaultValidatorId(validatorId);
	}

	@Override
	public void addELContextListener(ELContextListener listener) {
		// Forward to implementation
		applImpl.addELContextListener(listener);
	}

	@Override
	public void addELResolver(ELResolver resolver) {
		// Forward to implementation
		applImpl.addELResolver(resolver);
	}

	@Override
	public void addValidator(String validatorId, String validatorClass) {
		// Forward to implementation
		applImpl.addValidator(validatorId, validatorClass);
	}

	@Override
	public Behavior createBehavior(String behaviorId) throws FacesException {
		// Forward to implementation
		return applImpl.createBehavior(behaviorId);
	}

	@Override
	public UIComponent createComponent(FacesContext context,
			Resource componentResource) {
		// Forward to implementation
		return applImpl.createComponent(context, componentResource);
	}

	@Override
	public UIComponent createComponent(FacesContext context,
			String componentType, String rendererType) {
		// Forward to implementation
		return applImpl.createComponent(context, componentType, rendererType);
	}

	@Override
	public UIComponent createComponent(String componentType)
			throws FacesException {
		// Forward to implementation
		return applImpl.createComponent(componentType);
	}

	@Override
	public UIComponent createComponent(ValueBinding componentBinding,
			FacesContext context, String componentType) throws FacesException {
		// Forward to implementation
		return applImpl.createComponent(componentBinding, context, componentType);
	}

	@Override
	public UIComponent createComponent(ValueExpression componentExpression,
			FacesContext context, String componentType) throws FacesException {
		// Forward to implementation
		return applImpl.createComponent(componentExpression, context, componentType);
	}

	@Override
	public UIComponent createComponent(ValueExpression componentExpression,
			FacesContext context, String componentType, String rendererType) {
		// Forward to implementation
		return applImpl.createComponent(componentExpression, context, componentType, rendererType);
	}

	@Override
	public Converter createConverter(Class<?> targetClass) {
		// Forward to implementation
		return applImpl.createConverter(targetClass);
	}

	@Override
	public Converter createConverter(String converterId) {
		// Forward to implementation
		return applImpl.createConverter(converterId);
	}

	@Override
	public MethodBinding createMethodBinding(String ref, Class<?>[] params)
			throws ReferenceSyntaxException {
		// Forward to implementation
		return applImpl.createMethodBinding(ref, params);
	}

	@Override
	public Validator createValidator(String validatorId) throws FacesException {
		// Forward to implementation
		return applImpl.createValidator(validatorId);
	}

	@Override
	public ValueBinding createValueBinding(String ref)
			throws ReferenceSyntaxException {
		// Forward to implementation
		return applImpl.createValueBinding(ref);
	}

	@Override
	public <T> T evaluateExpressionGet(FacesContext context, String expression,
			Class<? extends T> expectedType) throws ELException {
		// Forward to implementation
		return applImpl.evaluateExpressionGet(context, expression, expectedType);
	}

	@Override
	public ActionListener getActionListener() {
		// Forward to implementation
		return applImpl.getActionListener();
	}

	@Override
	public Iterator<String> getBehaviorIds() {
		// Forward to implementation
		return applImpl.getBehaviorIds();
	}

	@Override
	public Iterator<String> getComponentTypes() {
		// Forward to implementation
		return applImpl.getComponentTypes();
	}

	@Override
	public Iterator<String> getConverterIds() {
		// Forward to implementation
		return applImpl.getConverterIds();
	}

	@Override
	public Iterator<Class<?>> getConverterTypes() {
		// Forward to implementation
		return applImpl.getConverterTypes();
	}

	@Override
	public Locale getDefaultLocale() {
		// Forward to implementation
		return applImpl.getDefaultLocale();
	}

	@Override
	public String getDefaultRenderKitId() {
		// Forward to implementation
		return applImpl.getDefaultRenderKitId();
	}

	@Override
	public Map<String, String> getDefaultValidatorInfo() {
		// Forward to implementation
		return applImpl.getDefaultValidatorInfo();
	}

	@Override
	public ELContextListener[] getELContextListeners() {
		// Forward to implementation
		return applImpl.getELContextListeners();
	}

	@Override
	public ELResolver getELResolver() {
		// Forward to implementation
		return applImpl.getELResolver();
	}

	@Override
	public ExpressionFactory getExpressionFactory() {
		// Forward to implementation
		return applImpl.getExpressionFactory();
	}

	@Override
	public String getMessageBundle() {
		// Forward to implementation
		return applImpl.getMessageBundle();
	}

	@Override
	public NavigationHandler getNavigationHandler() {
		// Forward to implementation
		return applImpl.getNavigationHandler();
	}

	@Override
	public ProjectStage getProjectStage() {
		// Forward to implementation
		return applImpl.getProjectStage();
	}

	@Override
	public PropertyResolver getPropertyResolver() {
		// Forward to implementation
		return applImpl.getPropertyResolver();
	}

	@Override
	public ResourceHandler getResourceHandler() {
		// Forward to implementation
		return applImpl.getResourceHandler();
	}

	@Override
	public StateManager getStateManager() {
		// Forward to implementation
		return applImpl.getStateManager();
	}

	@Override
	public Iterator<Locale> getSupportedLocales() {
		// Forward to implementation
		return applImpl.getSupportedLocales();
	}

	@Override
	public Iterator<String> getValidatorIds() {
		// Forward to implementation
		return applImpl.getValidatorIds();
	}

	@Override
	public VariableResolver getVariableResolver() {
		// Forward to implementation
		return applImpl.getVariableResolver();
	}

	@Override
	public ViewHandler getViewHandler() {
		// Forward to implementation
		return applImpl.getViewHandler();
	}

	@Override
	public void publishEvent(FacesContext facesContext,
			Class<? extends SystemEvent> systemEventClass,
			Class<?> sourceBaseType, Object source) {
		// Forward to implementation
		applImpl.publishEvent(facesContext, systemEventClass, sourceBaseType, source);
	}

	@Override
	public void publishEvent(FacesContext facesContext,
			Class<? extends SystemEvent> systemEventClass, Object source) {
		// Forward to implementation
		applImpl.publishEvent(facesContext, systemEventClass, source);
	}

	@Override
	public void removeELContextListener(ELContextListener listener) {
		// Forward to implementation
		applImpl.removeELContextListener(listener);
	}

	@Override
	public void setActionListener(ActionListener listener) {
		// Forward to implementation
		applImpl.setActionListener(listener);
	}

	@Override
	public void setDefaultLocale(Locale locale) {
		// Forward to implementation
		applImpl.setDefaultLocale(locale);
	}

	@Override
	public void setDefaultRenderKitId(String renderKitId) {
		// Forward to implementation
		applImpl.setDefaultRenderKitId(renderKitId);
	}

	@Override
	public void setMessageBundle(String bundle) {
		// Forward to implementation
		applImpl.setMessageBundle(bundle);
	}

	@Override
	public void setNavigationHandler(NavigationHandler handler) {
		// Forward to implementation
		applImpl.setNavigationHandler(handler);
	}

	@Override
	public void setPropertyResolver(PropertyResolver resolver) {
		// Forward to implementation
		applImpl.setPropertyResolver(resolver);
	}

	@Override
	public void setResourceHandler(ResourceHandler resourceHandler) {
		// Forward to implementation
		applImpl.setResourceHandler(resourceHandler);
	}

	@Override
	public void setStateManager(StateManager manager) {
		// Forward to implementation
		applImpl.setStateManager(manager);
	}

	@Override
	public void setSupportedLocales(Collection<Locale> locales) {
		// Forward to implementation
		applImpl.setSupportedLocales(locales);
	}

	@Override
	public void setVariableResolver(VariableResolver resolver) {
		// Forward to implementation
		applImpl.setVariableResolver(resolver);
	}

	@Override
	public void setViewHandler(ViewHandler handler) {
		// Forward to implementation
		applImpl.setViewHandler(handler);
	}

	@Override
	public void subscribeToEvent(Class<? extends SystemEvent> systemEventClass,
			Class<?> sourceClass, SystemEventListener listener) {
		// Forward to implementation
		applImpl.subscribeToEvent(systemEventClass, sourceClass, listener);
	}

	@Override
	public void subscribeToEvent(Class<? extends SystemEvent> systemEventClass,
			SystemEventListener listener) {
		// Forward to implementation
		applImpl.subscribeToEvent(systemEventClass, listener);
	}

	@Override
	public void unsubscribeFromEvent(
			Class<? extends SystemEvent> systemEventClass,
			Class<?> sourceClass, SystemEventListener listener) {
		// Forward to implementation
		applImpl.unsubscribeFromEvent(systemEventClass, sourceClass, listener);
	}

	@Override
	public void unsubscribeFromEvent(
			Class<? extends SystemEvent> systemEventClass,
			SystemEventListener listener) {
		// Forward to implementation
		applImpl.unsubscribeFromEvent(systemEventClass, listener);
	}

    /************************************************************/
    
}
