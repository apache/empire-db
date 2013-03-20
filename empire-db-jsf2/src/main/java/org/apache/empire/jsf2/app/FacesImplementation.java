package org.apache.empire.jsf2.app;

import javax.el.ValueExpression;
import javax.faces.component.UIComponent;

public interface FacesImplementation 
{
	/**
	 * Init application
	 * @param application the FacesApplication instance
	 *	
	 *	Implementation example:
	 *	--------------------------- 
	 *	ApplicationFactoryImpl applFactoryImpl = new ApplicationFactoryImpl();
     *	// set implementation
     *	application.setImplementation(this, applFactoryImpl.getApplication());
     *	applFactoryImpl.setApplication(application);
	 */
	void initApplication(FacesApplication application);

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
	 */
	void registerManagedBean(String beanName, String beanClass, String scope);
	
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
	 */
	UIComponent getValueParentComponent(ValueExpression ve);
	
}
