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
package org.apache.empire.jakarta.impl;

import java.lang.reflect.Method;

import jakarta.el.ELResolver;
import jakarta.el.ValueExpression;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;

public interface FacesImplementation 
{
    
    boolean registerElResolver(Class<? extends ELResolver> resolverClass);
    
	/**
	 *	Registers a managed bean	
	 *
	 *	Implementation for Mojarra:
	 *	--------------------------- 
	 *	FacesContext fc = FacesContext.getCurrentInstance();
	 * 	BeanManager  bm = ApplicationAssociate.getInstance(fc.getExternalContext()).getBeanManager();
	 * 	// check
     * 	if (bm.getRegisteredBeans().containsKey(beanName))
     * 	    throw new ItemExistsException(beanName);
     * 	// register now
     * 	ManagedBeanInfo mbi = new ManagedBeanInfo(beanName, beanClass, "view", null, null, null, null);
     * 	bm.register(mbi);
     *  
     *  
	 *	Implementation for MyFaces:
	 *	--------------------------- 
	 *	FacesContext  fc = FacesContext.getCurrentInstance();
	 *	RuntimeConfig rc = RuntimeConfig.getCurrentInstance(fc.getExternalContext());
	 *	// check
     *	if (rc.getManagedBeans().containsKey(beanName))
     *	    throw new ItemExistsException(beanName);
     *	// register now
     *	ManagedBean mbi = new ManagedBean(); 
     *	mbi.setName(beanName);
     *	mbi.setBeanClass(beanClass);
     *	mbi.setScope(scope);
     *	rc.addManagedBean(beanName, mbi);
     *  
     *  @param beanName the bean name
     *  @param beanClass the bean class
     *  @param scope the scope
	 */
	void registerManagedBean(final String beanName, final String beanClass, final String scope);
	
	/**
	 *	Returns the parentComponent for a given ValueExpression.
	 *
	 *	Implementation example:
	 *	-----------------------
     *  final ELContext elcontext = fc.getELContext();
     *  final Application application = fc.getApplication();
     *  return application.getELResolver().getValue(elcontext, null, beanName);
	 * 
     *  @param beanName the bean name
     *  @param fc the faces context
     *  @return the bean
	 */
	public Object getManagedBean(final String beanName, final FacesContext fc);
	
	/**
	 *	Return the parentComponent for a given ValueExpression.
	 *
	 *	Implementation for Mojarra:
	 *	--------------------------- 
	 *  if (ve instanceof ContextualCompositeValueExpression)
     *  {
     *      FacesContext ctx = FacesContext.getCurrentInstance();
     *      ContextualCompositeValueExpression ccve = (ContextualCompositeValueExpression)ve;
     *      CompositeComponentStackManager manager = CompositeComponentStackManager.getManager(ctx);
     *      UIComponent cc = manager.findCompositeComponentUsingLocation(ctx, ccve.getLocation());
     *      // set Parent
     *      return cc;
     *  }
     *
     *  @param ve the value expression
     *  @return the component
	 */
	UIComponent getValueParentComponent(final ValueExpression ve);

	/**
	 * Returns the inner value expression
	 * @param ve the original ValueExpression
	 * @return the unwrapped ValueExpression (may be null)
	 */
	ValueExpression unwrapValueExpression(ValueExpression ve);

    /**
     * Returns a read or write method for an JavaBean property
     * @param component the UIComponent
     * @param attribute the attribute name
     * @param writeMethod flag whether to the read or write method
     * @return the method or null
     */
    Method getPropertyMethod(final UIComponent component, String attribute, boolean writeMethod);
	
    /**
     * BeanStorageProvider
     * @author rainer
     */
	public interface BeanStorageProvider
    {
        void injectBean(Object bean);
    }
    
    BeanStorageProvider getBeanStorageProvider(ExternalContext externalContext);

    void configComplete();	
}
