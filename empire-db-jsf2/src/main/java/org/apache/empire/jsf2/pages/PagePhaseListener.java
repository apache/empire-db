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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PagePhaseListener implements PhaseListener
{
    private static final long   serialVersionUID    = 1L;
    private static final Logger log                 = LoggerFactory.getLogger(PagePhaseListener.class);

    public static final String  FORWARD_PAGE_PARAMS = "forwardPageParams";

    public PagePhaseListener()
    {
        // foo
    }

    @Override
    public PhaseId getPhaseId()
    {
        return PhaseId.ANY_PHASE;
    }

    @Override
    public void beforePhase(PhaseEvent pe)
    {
        log.trace("Processing Phase ", pe.getPhaseId());
        FacesContext fc = pe.getFacesContext();
        UIViewRoot   vr = fc.getViewRoot();
        if (vr == null)
            return;

        // Init Page
        String viewId = vr.getViewId();
        PageDefinition pageDef = PageDefinitions.getPageFromViewId(viewId);
        if (pageDef != null)
        {
            String name = pageDef.getPageBeanName();
            Map<String, Object> viewMap = vr.getViewMap();
            Page pageBean = (Page) viewMap.get(name);
            if (pageBean == null)
            {
                String pageBeanClassName = pageDef.getPageBeanClass().getName();
                log.info("Creating page bean {} for {} in Phase {}.", new Object[] { pageBeanClassName, viewId, pe.getPhaseId() });
                try
                {
                    pageBean = pageDef.getPageBeanClass().newInstance();
                    // List request parameters
                    /*
                     * FacesContext fc = pe.getFacesContext();
                     * Map<String, String> map = fc.getExternalContext().getRequestParameterMap();
                     * for (String key : map.keySet())
                     * {
                     * StringBuilder param = new StringBuilder();
                     * param.append("Parameter: ");
                     * param.append(key);
                     * param.append(" = ");
                     * param.append(map.get(key));
                     * log.debug(param.toString());
                     * }
                     */
                }
                catch (Exception e)
                {
                    throw new RuntimeException("Error creating instance of page bean " + pageBeanClassName, e);
                }
                viewMap.put(pageDef.getPageBeanName(), pageBean);
                viewMap.put("page", pageBean);
            }
            pageBean.setPageDefinition(pageDef);

            if (pe.getPhaseId() == PhaseId.RENDER_RESPONSE)
                initPageBean(pageBean, fc, viewMap);

        }
        else
            log.warn("No Page Defintion Error for path {}", viewId);

        /*
         * Collection<UIViewParameter> params = ViewMetadata.getViewParameters(vr);
         * for (UIViewParameter p : params)
         * {
         * log.info("p {} = {}", p.getName(), p.getValue());
         * }
         */
    }

    private void initPageBean(Page pageBean, FacesContext fc, Map<String, Object> viewMap)
    {
        if (!pageBean.isInitialized())
        {   // Not yet initialized 
            if (!PageNavigationHandler.isInitialized())
            {   // Probably missing declaration in faces-config.xml 
                log.error("PageNavigationHandler has not been initialized. Forward operations will not work!");
            }    
            // Check for forward page params
            if (viewMap.containsKey(FORWARD_PAGE_PARAMS))
            {
                @SuppressWarnings("unchecked")
                Map<String, String> pageParams = (Map<String, String>) viewMap.remove(FORWARD_PAGE_PARAMS);
                // TODO: Set view metadata
                if (!setViewMetadata(pageParams))
                {   // instead set properties directly
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
            }
            // page prepared
        }    
        // Init now
        pageBean.preRenderPage(fc);
    }
    
    /**
     * TODO: Find a way to set the view Metadata. Don't know how to do it.
     * @param pageParams
     * @return
     */
    private boolean setViewMetadata(Map<String, String> pageParams)
    {
        // Getting the metadata facet of the view
        /*
        FacesContext fc = FacesContext.getCurrentInstance();
        UIViewRoot   vr = fc.getViewRoot();
        UIComponent metadataFacet = vr.getFacet(UIViewRoot.METADATA_FACET_NAME);
        */
        
        /*
        String viewId = vr.getViewId();        
        ViewDeclarationLanguage vdl = fc.getApplication().getViewHandler().getViewDeclarationLanguage(fc, viewId);
        ViewMetadata viewMetadata = vdl.getViewMetadata(fc, viewId);
        Collection<UIViewParameter> viewParams = ViewMetadata.getViewParameters(vr);
        for (UIComponent child : metadataFacet.getChildren())
        */

        /*
        UIComponent metadata = vr.getFacet(UIViewRoot.METADATA_FACET_NAME);
        if (metadata == null)
        {
            metadata = fc.getApplication().createComponent(UIPanel.COMPONENT_TYPE);
            vr.getFacets().put(UIViewRoot.METADATA_FACET_NAME, metadata);

            Collection<UIViewParameter> viewParams = ViewMetadata.getViewParameters(vr);
            int size = viewParams.size();
            
            for (String name : pageParams.keySet())
            {
                String value = pageParams.get(name);
                UIViewParameter uivp = new UIViewParameter();
                uivp.setName(name);
                uivp.setValue(value);
                // uivp.setParent(vr);
                metadata.getChildren().add(uivp);
            }
        }
        for (UIComponent child : metadata.getChildren())
        {
            if (child instanceof UIViewParameter)
            {
                UIViewParameter viewParam = (UIViewParameter) child;
                String value = pageParams.get(viewParam.getName());
                if (value != null)
                    viewParam.setValue(value);
            }
        }
        */
        
        return false;
    }
    
    @Override
    public void afterPhase(PhaseEvent pe)
    {
        if (pe.getPhaseId() != PhaseId.RENDER_RESPONSE)
            return;
        // Check Page Bean
        UIViewRoot vr = pe.getFacesContext().getViewRoot();
        Map<String, Object> viewMap = vr.getViewMap();
        Page pageBean = (Page) viewMap.get("page");
        if (pageBean != null && !pageBean.isInitialized())
        {
            log.warn("PageBean was not initialized!");
            throw new RuntimeException("PageBean was not initialized!");
        }
        // FacesUtils.getFin2Application().releaseConnection(true);
        log.trace("PagePhase complete.");
    }

}
