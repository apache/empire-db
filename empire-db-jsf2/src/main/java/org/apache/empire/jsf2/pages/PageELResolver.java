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
import java.util.Iterator;
import java.util.Map;

import javax.el.ELContext;
import javax.el.ELResolver;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;

import org.apache.empire.exceptions.InvalidOperationException;
import org.apache.empire.jsf2.app.FacesUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PageELResolver extends ELResolver
{
    private static final Logger log = LoggerFactory.getLogger(PageELResolver.class);
    
    @Override
    public Class<?> getCommonPropertyType(ELContext context, Object base)
    {
        if (base==null)
            log.warn("PageELResolver:getCommonPropertyType is not implemented!");
        return null;
    }

    @Override
    public Class<?> getType(ELContext context, Object base, Object property)
    {
        if (base==null && property.equals("page"))
        {   // Page
            // context.setPropertyResolved(true);
            return Page.class;
        }
        return null;
    }

    @Override
    public Iterator<FeatureDescriptor> getFeatureDescriptors(ELContext arg0, Object arg1)
    {
        log.warn("PageELResolver:getFeatureDescriptors is not implemented!");
        return null;
    }

    @Override
    public Object getValue(ELContext context, Object base, Object property)
    {
        if (base==null && property.equals("page"))
        {
            UIViewRoot vr = FacesContext.getCurrentInstance().getViewRoot();
            if (vr==null)
            {   // Error: No view root     
                RuntimeException e = new InvalidOperationException("ViewRoot not available. Unable to get Page Bean.");
                log.error(e.getMessage());
                throw e; 
            }
            Map<String,Object> vmap = vr.getViewMap(false);
            Page page = (vmap!=null ? (Page)vmap.get("page") : null);
            if (page==null)
            {   // Error: No page bean      
                RuntimeException e = new InvalidOperationException("Page bean not available for current view.");
                log.error(e.getMessage());
                throw e; 
            }
            context.setPropertyResolved(true);
            return page;
        }
        return null;
    }

    @Override
    public boolean isReadOnly(ELContext context, Object base, Object property)
    {
        if (base==null && property.equals("page"))
        {
            context.setPropertyResolved(true);
            return true;
        }
        return false;
    }

    @Override
    public void setValue(ELContext context, Object base, Object property, Object value)
    {
        if (base==null && property.equals("page"))
        {   // done
            String pageAsString = String.valueOf(FacesUtils.getPage(FacesContext.getCurrentInstance()));
            if (!pageAsString.equals(value))
                log.warn("Page instances don't match");
            // done
            context.setPropertyResolved(true);
        }
    }

}
