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

import javax.el.ELContext;
import javax.el.ValueExpression;
import javax.el.VariableMapper;
import javax.faces.application.Application;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.commons.StringUtils;
import org.apache.empire.exceptions.ItemExistsException;
import org.apache.myfaces.config.RuntimeConfig;
import org.apache.myfaces.config.impl.digester.elements.ManagedBeanImpl;
import org.apache.myfaces.view.facelets.el.ContextAwareTagValueExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyFacesImplementation implements FacesImplementation 
{
    // Logger
    private static final Logger log = LoggerFactory.getLogger(FacesImplementation.class);

    public MyFacesImplementation()
    {
        log.debug("MyFacesImplementation created");
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
	public void registerManagedBean(final String beanName, final String beanClass, final String scope) {
		
        // get Runtime Config
		FacesContext  fc = FacesContext.getCurrentInstance();
		RuntimeConfig rc = RuntimeConfig.getCurrentInstance(fc.getExternalContext());
		// check
        if (rc.getManagedBeans().containsKey(beanName))
        {
            throw new ItemExistsException(beanName);
        }
        // register now
        // ManagedBean mbi = new ManagedBean();   --> Use this for Myfaces 2.1.x 
        ManagedBeanImpl mbi = new ManagedBeanImpl();  // new since Myfaces 2.2.x
        mbi.setName(beanName);
        mbi.setBeanClass(beanClass);
        mbi.setScope(scope);
        rc.addManagedBean(beanName, mbi);
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
		/* No implmentation for MyFaces currently available */
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
        // now unwrap ValueExpressionImpl
        if (ve!=null)
        {   // expected: ve = org.apache.el.ValueExpressionImpl
            if (ve.getClass().getName().equals("org.apache.el.ValueExpressionImpl"))
            {   // get the Node
                Object node = ObjectUtils.invokeSimplePrivateMethod(ve, "getNode");
                if (node!=null)
                {   // we have a Node
                    // now get the Image
                    String image = StringUtils.toString(ObjectUtils.invokeSimpleMethod(node, "getImage"));
                    if (StringUtils.isNotEmpty(image)) 
                    {   // find the varMapper
                        Object varMapper = ObjectUtils.getPrivateFieldValue(ve, "varMapper");
                        if (varMapper!=null)
                        {   // Resolve variable using mapper
                            log.debug("Resolving el-variable \"{}\" using VariableMapper", image);
                            VariableMapper vm = (VariableMapper)varMapper;
                            ve = vm.resolveVariable(image);
                        } else {
                            // Variable not provided!
                            ve = null;
                        }
                    } else {
                        // no image: unwrapping not necessary
                        // use original ValueExpression!
                    }
                }
            } else {
                // unexpected
                log.warn("Unexpected ValueExpression-Implementation: {}", ve.getClass().getName());
                log.warn("ValueExpression unwrapping does not work!");
            }
        }
        // done 
        return ve;
    }

}
