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
import java.util.ArrayList;
import java.util.List;

import org.apache.empire.exceptions.ItemExistsException;
import org.apache.empire.exceptions.NotSupportedException;
import org.apache.empire.jakarta.utils.ValueExpressionUnwrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.faces.application.ApplicationAssociate;
import com.sun.faces.application.ApplicationInstanceFactoryMetadataMap;
import com.sun.faces.component.CompositeComponentStackManager;
import com.sun.faces.config.ConfigManager;
import com.sun.faces.config.processor.ApplicationConfigProcessor;
import com.sun.faces.facelets.el.ContextualCompositeValueExpression;
import com.sun.faces.facelets.el.TagValueExpression;
import com.sun.faces.mgbean.BeanManager;
import com.sun.faces.mgbean.ManagedBeanInfo;
import com.sun.faces.spi.InjectionProvider;
import com.sun.faces.spi.InjectionProviderException;

import jakarta.el.ELResolver;
import jakarta.el.ValueExpression;
import jakarta.faces.FacesException;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.servlet.ServletContext;

public class MojarraImplementation implements FacesImplementation 
{
    // Logger
    private static final Logger log = LoggerFactory.getLogger(FacesImplementation.class);
    
    private final ApplicationAssociate applAssociate;

    public MojarraImplementation(ExternalContext externalContext)
	{
	    log.debug("MojarraImplementation created");
	    this.applAssociate = ApplicationAssociate.getInstance(externalContext);
	}
		
	/*
	@Override
	public void initApplication(final FacesApplication application)
	{
		ApplicationFactoryImpl applFactoryImpl = new ApplicationFactoryImpl();
        // set impl
        application.setImplementation(this, applFactoryImpl.getApplication());
        // Application Map 
        Map<String, Object> appMap = FacesContext.getCurrentInstance().getExternalContext().getApplicationMap();
        appMap.put(InjectionApplicationFactory.class.getName(), application);
        // init Bean Manager
		FacesContext fc = FacesContext.getCurrentInstance();
		bm = ApplicationAssociate.getInstance(fc.getExternalContext()).getBeanManager();
	}
	*/

    @Override
    public boolean registerElResolver(Class<? extends ELResolver> resolverClass)
    {
        List<ELResolver> list =applAssociate.getELResolversFromFacesConfig();
        if (list!=null) {
            for (ELResolver resolver : list)
            {
                if (resolver.getClass().equals(resolverClass))
                    return false; // already there
            }
        } else {
            list = new ArrayList<ELResolver>();
            applAssociate.setELResolversFromFacesConfig(list);
        }
        /*
        // create
        ELResolver elResolver = ClassUtils.newInstance(resolverClass);
        // Add to bean storage
        getBeanStorageProvider(null).injectBean(elResolver);
        // Add to RuntimeConfig
        list.add(elResolver);
        return true;
        */
        log.error("registerElResolver is not supported for Mojarra! Reason is, that it's too late and the ElResolver chain has already been built. Please define in faces-config.xml");
        throw new NotSupportedException(this, "registerElResolver");
    }

	@Override
	public void registerManagedBean(final String beanName,final String beanClass,final String scope) 
	{
		// check
        BeanManager bm = applAssociate.getBeanManager();
        if (bm.getRegisteredBeans().containsKey(beanName))
        {
            throw new ItemExistsException(beanName);
        }
        // register now
        ManagedBeanInfo mbi = new ManagedBeanInfo(beanName, beanClass, "view", null, null, null, null);
        bm.register(mbi);
	}

	@Override
	public Object getManagedBean(final String beanName, final FacesContext fc)
	{
	    // Find Bean
        BeanManager bm = applAssociate.getBeanManager();
		Object mbean = bm.getBeanFromScope(beanName, fc);
		if (mbean==null)
			mbean= bm.create(beanName, fc);
        return mbean;
	}
	
	@Override
	public UIComponent getValueParentComponent(final ValueExpression ve) 
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

    @Override
    public ValueExpression unwrapValueExpression(ValueExpression ve)
    {
        // unwrap from com.sun.faces.facelets.el.TagValueExpression
        if (ve instanceof TagValueExpression)
        {   // cast and getWrapped
            ve = ((TagValueExpression)ve).getWrapped();
        }
        // now unwrap using the ValueExpressionUnwrapper 
        return ValueExpressionUnwrapper.getInstance().unwrap(ve);
    }
    
    @Override
    public Method getPropertyMethod(final UIComponent component, String attribute, boolean writeMethod)
    {
        // Not yet implemented. 
        // Is Implementation required?
        return null;
    }
    
    private BeanStorageProvider beanStorage = null;
    
    @Override
    public BeanStorageProvider getBeanStorageProvider(ExternalContext externalContext)
    {
        if (beanStorage==null) {
            if (externalContext==null)
                externalContext = FacesContext.getCurrentInstance().getExternalContext();
            beanStorage = new MojarraBeanStorageProvider(externalContext); 
        }
        return beanStorage; 
    }
    
    @Override
    public void configComplete()
    {
        beanStorage = null;
    }
    
    /**
      * BeanStorageProvider
      * @author doebele
      */
    protected static class MojarraBeanStorageProvider implements BeanStorageProvider
    {
        private final ApplicationInstanceFactoryMetadataMap<String,Object> classMetadataMap;
        private final InjectionProvider injectionProvider;

        @SuppressWarnings("unchecked")
        public MojarraBeanStorageProvider(ExternalContext externalContext)
        {
            final String METADATA_MAP_KEY = ApplicationConfigProcessor.class.getName()+".METADATA";
            ServletContext sc = (ServletContext)externalContext.getContext();
            this.classMetadataMap = (ApplicationInstanceFactoryMetadataMap<String,Object>) sc.getAttribute(METADATA_MAP_KEY);
            this.injectionProvider = (InjectionProvider) FacesContext.getCurrentInstance().getAttributes().get(ConfigManager.INJECTION_PROVIDER_KEY);
        }
        
        @Override
        public void injectBean(Object bean)
        {
            if (classMetadataMap==null)
                return;
            // put first
            String className = bean.getClass().getName();
            classMetadataMap.put(className, bean.getClass());
            // check annotations
            if (classMetadataMap.hasAnnotations(className)) {
                try {
                    injectionProvider.inject(bean);
                } catch (InjectionProviderException ex) {
                    log.error("Unable to inject instance" + className, ex);
                    throw new FacesException(ex);
                }
                try {
                    injectionProvider.invokePostConstruct(bean);
                } catch (InjectionProviderException ex) {
                    log.error("Unable to invoke @PostConstruct annotated method on instance " + className, ex);
                    throw new FacesException(ex);
                }
            }
        }
    }
    
}
