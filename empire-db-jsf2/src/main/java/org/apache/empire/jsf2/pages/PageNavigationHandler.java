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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.faces.application.ConfigurableNavigationHandler;
import javax.faces.application.NavigationCase;
import javax.faces.application.NavigationHandler;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PageNavigationHandler extends ConfigurableNavigationHandler
{
    private static final Logger log = LoggerFactory.getLogger(PageNavigationHandler.class);
    
    private static boolean initialized = false;

    public static boolean isInitialized()
    {
        return initialized;
    }

    private NavigationHandler parent;

    public PageNavigationHandler(NavigationHandler parent)
    {
        this.parent = parent;
        initialized = true;
        // Log
        log.info("PageNavigatonHandler sucessfully established.");
    }

    @Override
    public void handleNavigation(FacesContext context, String from, String outcome)
    {
        // boolean redirect = (outcome.indexOf("faces-redirect=true")>=0);

        parent.handleNavigation(context, from, outcome);
        
        // Check outcome
        if (outcome==null)
            return;
        
        NavigationCase navigationCase = getNavigationCase(context, from, outcome);
        boolean redirect = (navigationCase != null ? 
                            navigationCase.isRedirect() : 
                           (outcome.indexOf("faces-redirect=true") >= 0));
        // Check for forward (not redirect)
        if (!redirect)
        {
            log.debug("Handling forward navigation.");
            /*
            NavigationCase navigationCase = getNavigationCase(context, fromAction, outcome);
            if (navigationCase == null || navigationCase.isRedirect()) {
                handler.handleNavigation(context, fromAction, outcome);
            } else {
                UIViewRoot viewRoot = context.getViewRoot();
                Collection<UIForm> forms = findUIForms(viewRoot);
                Map<String, Object> viewAttributes = viewRoot.getViewMap();
                HashMap idToPreviousParametersMapping = (HashMap) viewAttributes.get(DeltaSubmitPhaseListener.PreviousParameters);
                if (idToPreviousParametersMapping == null) {
                    idToPreviousParametersMapping = new HashMap();
                    viewAttributes.put(DeltaSubmitPhaseListener.PreviousParameters, idToPreviousParametersMapping);
                }
                for (UIForm form : forms) {
                    Map previousParameters = (Map) form.getAttributes().get(DeltaSubmitPhaseListener.PreviousParameters);
                    if (previousParameters != null) {
                        idToPreviousParametersMapping.put(form.getId(), new HashMap(previousParameters));
                    }
                }
                handler.handleNavigation(context, fromAction, outcome);
                //propagate previously calculated submit parameters
                context.getViewRoot().getViewMap().put(DeltaSubmitPhaseListener.PreviousParameters, idToPreviousParametersMapping);
            }
            */
            UIViewRoot viewRoot = context.getViewRoot();
            Map<String, Object> viewMap = viewRoot.getViewMap();
            Map<String, String> paramsMap = getActionParameterMap(outcome);
            if (paramsMap!=null)
            {
                log.debug("Setting FORWARD_PAGE_PARAMS for outcome {}.", outcome);
                viewMap.put(PagePhaseListener.FORWARD_PAGE_PARAMS, paramsMap);
            }    
        }
        
    }
    
    private Map<String, String> getActionParameterMap(String s)
    {
        int i = -1;
        Map<String, String> map = null;
        while ((i=s.indexOf('=', i+1))>0)
        {
            int n = s.lastIndexOf('&', i);
            if (n<0)
                n = s.lastIndexOf('?', i);
            if (n<0)
                continue; // something is wrong
            int v = s.indexOf('&');
            String name = s.substring(n+1, i);
            String value = (v>i) ? s.substring(i+1, v) : s.substring(i+1);
            log.debug("Adding view parameter '{}' with value '{}'.", name, value);
            if (map==null)
                map = new HashMap<String, String>();
            map.put(name, value);
        }
        return map;
    }

    @Override
    public NavigationCase getNavigationCase(FacesContext context, String fromAction, String outcome)
    {
        if (parent instanceof ConfigurableNavigationHandler)
        {
            NavigationCase navigationCase = ((ConfigurableNavigationHandler) parent).getNavigationCase(context, fromAction, outcome);
            /*
            if (navigationCase!=null && !navigationCase.isRedirect())
            {
                log.trace("Performing a forward operation!");
            }
            */
            return navigationCase;
        }
        else
        {
            return null;
        }
    }

    @Override
    public Map<String, Set<NavigationCase>> getNavigationCases()
    {
        if (parent instanceof ConfigurableNavigationHandler)
        {
            return ((ConfigurableNavigationHandler) parent).getNavigationCases();
        }
        else
        {
            return null;
        }
    }
}
