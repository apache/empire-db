package org.apache.empire.jsf2.app.impl;

import java.util.Map;

import javax.el.ValueExpression;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.apache.empire.exceptions.ItemExistsException;
import org.apache.empire.jsf2.app.FacesApplication;
import org.apache.empire.jsf2.app.FacesImplementation;

import com.sun.faces.application.ApplicationAssociate;
import com.sun.faces.application.ApplicationFactoryImpl;
import com.sun.faces.application.InjectionApplicationFactory;
import com.sun.faces.component.CompositeComponentStackManager;
import com.sun.faces.facelets.el.ContextualCompositeValueExpression;
import com.sun.faces.mgbean.BeanManager;
import com.sun.faces.mgbean.ManagedBeanInfo;

public class MojarraImplementation implements FacesImplementation 
{

	@Override
	public void initApplication(FacesApplication application)
	{
		ApplicationFactoryImpl applFactoryImpl = new ApplicationFactoryImpl();
        // set impl
        application.setImplementation(this, applFactoryImpl.getApplication());
        // Application Map 
        Map<String, Object> appMap = FacesContext.getCurrentInstance().getExternalContext().getApplicationMap();
        appMap.put(InjectionApplicationFactory.class.getName(), application);
	}

	@Override
	public void registerManagedBean(String beanName, String beanClass, String scope) 
	{
		FacesContext fc = FacesContext.getCurrentInstance();
		BeanManager  bm = ApplicationAssociate.getInstance(fc.getExternalContext()).getBeanManager();
		// check
        if (bm.getRegisteredBeans().containsKey(beanName))
        {
            throw new ItemExistsException(beanName);
        }
        // register now
        ManagedBeanInfo mbi = new ManagedBeanInfo(beanName, beanClass, "view", null, null, null, null);
        bm.register(mbi);
	}

	@Override
	public UIComponent getValueParentComponent(ValueExpression ve) 
	{
        if (ve instanceof ContextualCompositeValueExpression)
        {
            FacesContext ctx = FacesContext.getCurrentInstance();
            ContextualCompositeValueExpression ccve = (ContextualCompositeValueExpression)ve;
            CompositeComponentStackManager manager = CompositeComponentStackManager.getManager(ctx);
            UIComponent cc = manager.findCompositeComponentUsingLocation(ctx, ccve.getLocation());
            // set Parent
            return cc;
        }
        return null;
	}

}
