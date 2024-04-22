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
package org.apache.empire.jsf2.impl;

import java.util.List;

import javax.el.ELContext;
import javax.el.ELResolver;
import javax.el.ValueExpression;
import javax.faces.application.Application;
import javax.faces.component.UIComponent;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import org.apache.empire.commons.ClassUtils;
import org.apache.empire.exceptions.InternalException;
import org.apache.empire.exceptions.ItemExistsException;
import org.apache.empire.exceptions.ItemNotFoundException;
import org.apache.empire.exceptions.NotSupportedException;
import org.apache.empire.jsf2.utils.ValueExpressionUnwrapper;
import org.apache.myfaces.application.ApplicationImpl;
import org.apache.myfaces.cdi.util.BeanEntry;
import org.apache.myfaces.config.RuntimeConfig;
import org.apache.myfaces.config.impl.digester.elements.ManagedBeanImpl;
import org.apache.myfaces.spi.InjectionProvider;
import org.apache.myfaces.spi.InjectionProviderException;
import org.apache.myfaces.spi.InjectionProviderFactory;
import org.apache.myfaces.view.facelets.el.ContextAwareTagValueExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyFacesImplementation implements FacesImplementation 
{
    // Logger
    private static final Logger log = LoggerFactory.getLogger(FacesImplementation.class);

    private final RuntimeConfig runtimeConfig;
    
    public MyFacesImplementation(ExternalContext externalContext)
    {
        log.debug("MyFacesImplementation created");
        this.runtimeConfig = RuntimeConfig.getCurrentInstance(externalContext);
    }
    
    /*
	@Override
	public void initApplication(FacesApplication application)
	{
		ApplicationFactoryImpl applFactoryImpl = new ApplicationFactoryImpl();
        // set impl
        application.setImplementation(this, applFactoryImpl.getApplication());
        applFactoryImpl.setApplication(application);
	}
	*/

    @Override
    public boolean registerElResolver(Class<? extends ELResolver> resolverClass)
    {
        List<ELResolver> list = runtimeConfig.getFacesConfigElResolvers();
        if (list!=null) {
            for (ELResolver resolver : list)
            {
                if (resolver.getClass().equals(resolverClass))
                    return false; // already there
            }
        }
        // Check
        Application app = FacesContext.getCurrentInstance().getApplication();
        if ((app instanceof ApplicationImpl) && ClassUtils.getPrivateFieldValue(app, "elResolver")!=null)
        {   // Too late: ElResolver chain has already been built
            log.error("ElResolver chain has already been built. Please define in faces-config.xml");
            throw new NotSupportedException(this, "registerElResolver");
        }
        // create
        ELResolver elResolver = ClassUtils.newInstance(resolverClass);
        // Add to bean storage
        getBeanStorageProvider(null).injectBean(elResolver);
        // Add to RuntimeConfig
        runtimeConfig.addFacesConfigElResolver(elResolver);
        return true;
    }
    
	@Override
	public void registerManagedBean(final String beanName, final String beanClass, final String scope) 
	{	// check
        if (runtimeConfig.getManagedBeans().containsKey(beanName))
        {
            throw new ItemExistsException(beanName);
        }
        // register now
        // ManagedBean mbi = new ManagedBean();   --> Use this for Myfaces 2.1.x 
        ManagedBeanImpl mbi = new ManagedBeanImpl();  // new since Myfaces 2.2.x
        mbi.setName(beanName);
        mbi.setBeanClass(beanClass);
        mbi.setScope(scope);
        runtimeConfig.addManagedBean(beanName, mbi);
	}

	@Override
	public Object getManagedBean(final String beanName, final FacesContext fc)
	{
		// Find Bean
        final ELContext elcontext = fc.getELContext();
        final Application application = fc.getApplication();
        return application.getELResolver().getValue(elcontext, null, beanName);
	}

	@Override
	public UIComponent getValueParentComponent(final ValueExpression ve)
	{
		return null;
	}

    @Override
    public ValueExpression unwrapValueExpression(ValueExpression ve)
    {
        // unwrap from org.apache.myfaces.view.facelets.el.ContextAwareTagValueExpression
        if (ve instanceof ContextAwareTagValueExpression)
        {   // cast and getWrapped
            ve = ((ContextAwareTagValueExpression)ve).getWrapped();
        }
        // now unwrap using the ValueExpressionUnwrapper 
        return ValueExpressionUnwrapper.getInstance().unwrap(ve);
    }

    private BeanStorageProvider beanStorage = null;

    @Override
    public BeanStorageProvider getBeanStorageProvider(ExternalContext externalContext)
    {
        if (beanStorage==null) {
            if (externalContext==null)
                externalContext = FacesContext.getCurrentInstance().getExternalContext();
            beanStorage = new MyFacesBeanStorageProvider(externalContext); 
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
     */
    protected static class MyFacesBeanStorageProvider implements BeanStorageProvider
    {
        private final List<BeanEntry> injectedBeanStorage;
        private final InjectionProvider injectionProvider;

        @SuppressWarnings("unchecked")
        public MyFacesBeanStorageProvider(ExternalContext ec)
        {
           this.injectionProvider = InjectionProviderFactory.getInjectionProviderFactory(ec).getInjectionProvider(ec);
           final String INJECTED_BEAN_STORAGE_KEY = "org.apache.myfaces.spi.BEAN_ENTRY_STORAGE";
           this.injectedBeanStorage = (List<BeanEntry>)ec.getApplicationMap().get(INJECTED_BEAN_STORAGE_KEY);
           if (this.injectedBeanStorage==null)
               throw new ItemNotFoundException(INJECTED_BEAN_STORAGE_KEY);
        }
        
        @Override
        public void injectBean(Object bean)
        {   try
            {   // Add to bean storage
                Object creationMetaData = injectionProvider.inject(bean);
                injectedBeanStorage.add(new BeanEntry(bean, creationMetaData));
                injectionProvider.postConstruct(bean, creationMetaData);
            }
            catch (InjectionProviderException e)
            {
                throw new InternalException(e);
            }
        }
    }
    
}
