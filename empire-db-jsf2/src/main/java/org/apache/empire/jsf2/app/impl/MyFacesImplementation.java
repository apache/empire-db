package org.apache.empire.jsf2.app.impl;

import javax.el.ValueExpression;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.apache.empire.exceptions.ItemExistsException;
import org.apache.empire.jsf2.app.FacesApplication;
import org.apache.empire.jsf2.app.FacesImplementation;
import org.apache.myfaces.application.ApplicationFactoryImpl;
import org.apache.myfaces.config.RuntimeConfig;
import org.apache.myfaces.config.impl.digester.elements.ManagedBean;

public class MyFacesImplementation implements FacesImplementation 
{
	
	@Override
	public void initApplication(FacesApplication application)
	{
		ApplicationFactoryImpl applFactoryImpl = new ApplicationFactoryImpl();
        // set impl
        application.setImplementation(this, applFactoryImpl.getApplication());
        applFactoryImpl.setApplication(application);
	}

	@Override
	public void registerManagedBean(String beanName, String beanClass, String scope) {
		
		FacesContext  fc = FacesContext.getCurrentInstance();
		RuntimeConfig rc = RuntimeConfig.getCurrentInstance(fc.getExternalContext());
		// check
        if (rc.getManagedBeans().containsKey(beanName))
        {
            throw new ItemExistsException(beanName);
        }
        // register now
        ManagedBean mbi = new ManagedBean(); 
        mbi.setName(beanName);
        mbi.setBeanClass(beanClass);
        mbi.setScope(scope);
        rc.addManagedBean(beanName, mbi);
	}

	@Override
	public UIComponent getValueParentComponent(ValueExpression ve)
	{
		/* No implmentation for MyFaces currently available */
		return null;
	}

}
