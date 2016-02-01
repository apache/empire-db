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
package org.apache.empire.jsf2.pages;

import java.beans.FeatureDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Iterator;

import javax.el.ELContext;
import javax.el.ELResolver;
import javax.faces.context.FacesContext;

import org.apache.empire.exceptions.NotSupportedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PagesELResolver extends ELResolver
{
    private static final Logger log = LoggerFactory.getLogger(PagesELResolver.class);
    
    /*
    private static class PageResolver
    {
        public PageDefinition findPageDefinition(String name)
            throws SecurityException, NoSuchFieldException, IllegalAccessException
        {
            List<Class<?>> pageDefintionClasses = PageManager.getPageDefintions();
            if (pageDefintionClasses==null)
            {
                throw new RuntimeException("No page defintions available.");
            }
            for (Class<?> pdc : pageDefintionClasses)
            {
                try
                {   // Get Page defintion
                    Field field = pdc.getField(name);
                    Object pageDef = field.get(pdc); 
                    if (!(pageDef instanceof PageDefinition))
                        throw new RuntimeException("Illegal page defintion.");
                    return (PageDefinition)pageDef;
                }
                catch (NoSuchFieldException e)
                {
                    continue;
                }
            }
            log.error("No page defintion named {} was found.", name);
            throw new NoSuchFieldException(name);
        }
    }
    
    private static final PageResolver pageResolver = new PageResolver();
    */
    
    @Override
    public Class<?> getCommonPropertyType(ELContext context, Object base)
    {
        if (base==null)
            log.warn("DBELResolver:getCommonPropertyType is not implemented!");
        return null;
    }

    @Override
    public Class<?> getType(ELContext context, Object base, Object property)
    {
        if ((base instanceof PageDefinitions) ||
            (base==null && property.equals("pages")))
            log.warn("DBELResolver:getType is not implemented!");
        return null;
    }

    @Override
    public Iterator<FeatureDescriptor> getFeatureDescriptors(ELContext arg0, Object arg1)
    {
        log.warn("DBELResolver:getFeatureDescriptors is not implemented!");
        return null;
    }

    @Override
    public Object getValue(ELContext context, Object base, Object property)
    {
        // Resolve PageDefinitions, PageDefinition
        if (base instanceof PageDefinition)
        {
            String action = String.valueOf(property);
            PageDefinition pageDef = ((PageDefinition)base);
            try {
                Method m = pageDef.getPageBeanClass().getMethod(action);
                if (m!=null)
                    context.setPropertyResolved(true);
                return pageDef.getOutcome(action);
            } catch(Exception e) {
                throw new RuntimeException("Method "+action+" not found on bean "+pageDef.getPageBeanClass().getName(), e);
            }
        }
        else if (base instanceof PageDefinitions)
        {
            String name = String.valueOf(property);
            try {
                // Class<?> pdsClass = ((PageDefinitions)base).getPageDefintionClass();
                Field field = base.getClass().getField(name);
                Object pageDef = field.get(base); 
                if (pageDef!=null && (pageDef instanceof PageDefinition))
                    context.setPropertyResolved(true);
                else 
                    throw new RuntimeException("Illegal Page Defintion for property "+name);
                return pageDef;
            } catch(Exception e) {
                throw new RuntimeException("Unable to get page defintion.", e);
            }
        }
        else if (property.equals("pages") || property.equals("Pages"))
        {
            PageDefinitions pds = PageDefinitions.getInstance();
            if (pds!=null)
                context.setPropertyResolved(true);
            else
                throw new RuntimeException("No Page defintions available. Please create instance of class "+PageDefinitions.class.getName());
            return pds; 
        }
        return null;
    }

    @Override
    public boolean isReadOnly(ELContext context, Object base, Object property)
    {
        // is it our's?
        if ((base instanceof PageDefinitions) ||
            (base==null && property.equals("pages")))
        {   // read only
            log.info("ELResolver: property {} is read only.", property);
            return true;
        }
        // not our business
        return false;
    }

    @Override
    public void setValue(ELContext context, Object base, Object property, Object value)
    {
        if (isReadOnly(context, base, property))
        {
            RuntimeException e = new NotSupportedException(this, "setValue");
            FacesContext.getCurrentInstance().getExternalContext().log(e.getMessage(), e);
            throw e;
        }    
    }

}
