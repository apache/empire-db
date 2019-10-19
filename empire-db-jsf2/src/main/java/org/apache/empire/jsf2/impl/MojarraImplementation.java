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

import javax.el.ValueExpression;
import javax.el.VariableMapper;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.commons.StringUtils;
import org.apache.empire.exceptions.ItemExistsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.faces.application.ApplicationAssociate;
import com.sun.faces.component.CompositeComponentStackManager;
import com.sun.faces.facelets.el.ContextualCompositeValueExpression;
import com.sun.faces.facelets.el.TagValueExpression;
import com.sun.faces.mgbean.BeanManager;
import com.sun.faces.mgbean.ManagedBeanInfo;

public class MojarraImplementation implements FacesImplementation 
{
    // Logger
    private static final Logger log = LoggerFactory.getLogger(FacesImplementation.class);

    private BeanManager bm;
    
    public MojarraImplementation()
	{
	    log.debug("MojarraImplementation created");
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
	public void registerManagedBean(final String beanName,final String beanClass,final String scope) 
	{
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
	public Object getManagedBean(final String beanName, final FacesContext fc)
	{
	    // Find Bean
	    if (bm==null)
            bm = ApplicationAssociate.getInstance(fc.getExternalContext()).getBeanManager();
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
