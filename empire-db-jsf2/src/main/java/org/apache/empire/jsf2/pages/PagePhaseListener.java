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

import java.util.Map;

import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.empire.exceptions.InternalException;
import org.apache.empire.exceptions.ObjectNotValidException;
import org.apache.empire.jsf2.app.FacesUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PagePhaseListener implements PhaseListener
{
    private static final long   serialVersionUID       = 1L;
    private static final Logger log                    = LoggerFactory.getLogger(PagePhaseListener.class);

    private static final String LAST_PAGE_VIEW_ID      = "lastPageViewId";
    
    // FORWARD_PAGE_PARAMS used by PageNavigationHandler
    public  static final String FORWARD_PAGE_PARAMS    = "forwardPageParams";

    public PagePhaseListener()
    {
        log.trace("PagePhaseListener created.");
    }

    @Override
    public PhaseId getPhaseId()
    {
        return PhaseId.ANY_PHASE;
    }

    @Override
    public void beforePhase(PhaseEvent pe)
    {
        PhaseId phaseId = pe.getPhaseId();
        if (log.isTraceEnabled())
            log.trace("Processing Phase {}.", phaseId);

        FacesContext fc = pe.getFacesContext();
        UIViewRoot vr = fc.getViewRoot();
        if (vr == null)
        {
            /*
            PartialViewContext pvc = fc.getPartialViewContext(); 
            boolean ajax = pvc.isAjaxRequest();
            boolean part = pvc.isPartialRequest();
            if (!part && !ajax)
            {
                HttpServletRequest req = FacesUtils.getHttpRequest(fc);
                String reqURI = req.getRequestURI();
                log.info("No View Root for request to '"+reqURI+"' in Phase "+String.valueOf(phaseId));
            }
            */
            return;
        }

        // Get the view Id
        String viewId = vr.getViewId();
        if (viewId==null)
        {   // Error: No viewId!
            if (phaseId==PhaseId.RENDER_RESPONSE)
                FacesUtils.getWebApplication().onViewNotFound(fc, FacesUtils.getHttpRequest(fc));
            else
                log.error("No viewId provided for PagePhaseEvent in phase "+phaseId.getName());
            return;
        }
        
        // Detect view change
        Map<String, Object> sessionMap = fc.getExternalContext().getSessionMap();
        Object lastViewId = sessionMap.get(LAST_PAGE_VIEW_ID);
        if (lastViewId == null || !(((String) lastViewId).equalsIgnoreCase(viewId)))
        { // view changes
            FacesUtils.getWebApplication().onChangeView(fc, viewId);
            if (fc.getResponseComplete())
                return;
            // set view Id
            sessionMap.put(LAST_PAGE_VIEW_ID, viewId);
        }

        // Get Page Definition
        PageDefinition pageDef = PageDefinitions.getInstance().getPageFromViewId(viewId);
        if (pageDef == null)
        {   // No page definition available
            if (log.isDebugEnabled())
                log.debug("No page definition available for viewId {}.", viewId);
            // terminate
            return;
        }    

        // Obtain PageBean from BeanManager
        String name = pageDef.getPageBeanName();
        Map<String, Object> viewMap = vr.getViewMap();
        Page pageBean = (Page) viewMap.get(name);
        if (pageBean == null)
        {   // Not available yet
            if (log.isDebugEnabled())
                log.debug("Creating page bean of type {} for {} in Phase {}.", pageDef.getPageBeanClass().getName(), viewId, pe.getPhaseId());
            // Create Page Bean
            pageBean = createPageBean(name, pageDef, fc);
            viewMap.put(name, pageBean);
            // set definition
            pageBean.setPageDefinition(pageDef);
            // Add 'page' to map 
            viewMap.put("page", pageBean);
        }

        // Init PageBean
        if (pe.getPhaseId() == PhaseId.RENDER_RESPONSE)
            initPageBean(pageBean, fc, viewMap);

        /*
        Collection<UIViewParameter> params = ViewMetadata.getViewParameters(vr);
        for (UIViewParameter p : params)
        {
            log.info("p {} = {}", p.getName(), p.getValue());
        }
        */
    }

    protected Page createPageBean(String name, PageDefinition pageDef, FacesContext fc)
    {
        Class<? extends Page> pageClass = pageDef.getPageBeanClass();
        // Use Bean Manager first
        Page pageBean = (Page)FacesUtils.getManagedBean(fc, name);
        if (pageBean==null)
        {   // Create Instance ourselves
            log.warn("Unable to obtain page bean \"{}\" from BeanManager. Page bean probably not registered. Creating new instance but Injection might not work! ", name);
            try
            {   // Create Instance
                pageBean = pageClass.newInstance();
            }
            catch (Exception e)
            {
                log.error("Error creating instance of page bean " + pageClass.getName(), e);
                throw new InternalException(e);
            }
        }
        else if (!pageClass.isInstance(pageBean))
        {   // Wrong class
            log.warn("Page bean \"{}\" is not an instance of class {} as expected. Detected class is {}", name, pageClass.getName(), pageBean.getClass().getName());
        }
        return pageBean;
    }
    
    protected void initPageBean(Page pageBean, FacesContext fc, Map<String, Object> viewMap)
    {
        if (!pageBean.isInitialized())
        { // Not yet initialized 
            if (!PageNavigationHandler.isInitialized())
            { // Probably missing declaration in faces-config.xml 
                log.error("PageNavigationHandler has not been initialized. Forward operations will not work!");
            }
            // Check for forward page params
            if (viewMap.containsKey(FORWARD_PAGE_PARAMS))
            {
                @SuppressWarnings("unchecked")
                Map<String, String> pageParams = (Map<String, String>) viewMap.remove(FORWARD_PAGE_PARAMS);
                setPageParams(pageBean, pageParams);
            }
            // page prepared
        }
        // Init now
        pageBean.preRenderPage(fc);
    }

    /**
     * Sets the view Metadata
     * @param pageParams
     * @return
     */
    protected void setPageParams(Page pageBean, Map<String, String> pageParams)
    {
        for (String name : pageParams.keySet())
        {
            String value = pageParams.get(name);
            try
            {
                BeanUtils.setProperty(pageBean, name, value);
            }
            catch (Exception e)
            {
                log.error("Unable to set PageParam " + name + " on " + pageBean.getClass().getName() + ".", e);
            }
        }
    }

    @Override
    public void afterPhase(PhaseEvent pe)
    {
        if (pe.getPhaseId() != PhaseId.RENDER_RESPONSE)
            return;
        // Get the view
        UIViewRoot vr = pe.getFacesContext().getViewRoot();
        if (vr==null)
            return;
        // Check Page Bean
        Map<String, Object> viewMap = vr.getViewMap();
        Page pageBean = (Page) viewMap.get("page");
        if (pageBean != null && !pageBean.isInitialized())
        {
            log.warn("PageBean was not initialized!");
            throw new ObjectNotValidException(pageBean);
        }
        // FacesUtils.getFin2Application().releaseConnection(true);
        log.trace("PagePhase complete.");
    }

}
