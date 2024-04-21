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
package org.apache.empire.jsf2.websample.web;

import javax.faces.context.FacesContext;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;
import javax.servlet.http.HttpServletRequest;

import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.commons.StringUtils;
import org.apache.empire.exceptions.InvalidArgumentException;
import org.apache.empire.jsf2.app.FacesUtils;
import org.apache.empire.jsf2.websample.web.pages.SamplePages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthenticationPhaseListener implements PhaseListener
{
    private static final long   serialVersionUID = 1L;
    private static final Logger log              = LoggerFactory.getLogger(AuthenticationPhaseListener.class);

    private static final String LOGOUT_PARAM     = "logout";    // must be ?logout=true
//    private static final String LANGUAGE_PARAM   = "prefLanguage";    // must be ?logout=true
    
    public AuthenticationPhaseListener()
    {
    }

    @Override
    public PhaseId getPhaseId()
    {
        return PhaseId.RESTORE_VIEW;
    }

    @Override
    public void beforePhase(PhaseEvent pe)
    {
        // Check App
        SampleApplication app = SampleUtils.getSampleApplication();
        if (app == null)
        {
            log.error("Application not initialized!");
            return;
        }
        try
        {
            FacesContext fc = pe.getFacesContext();
            // Path Info
            if (log.isDebugEnabled())
            {
                String path = fc.getExternalContext().getRequestServletPath();
                log.debug("Restoring view for request path {}.", path);
            }    
            checkAuthetication(fc, app);
        }
        catch (Exception e)
        {
            log.error("Authetication exception \"{}\" redirecting to error page!", e.getMessage(), e);
            String errorPage = getPageUri(app.getSampleConfig().getAccessDeniedPage());
            FacesUtils.redirectDirectly(pe.getFacesContext(), errorPage);
        }
    }
    
    @Override
    public void afterPhase(PhaseEvent event)
    {
        
    }
    
    /******************************************************************************************/

    
    private void checkAuthetication(FacesContext fc, SampleApplication app)
        throws Exception
    {
        SampleSession session = SampleUtils.getSampleSession(fc);

        if (!session.isAuthorized())
        {   
            HttpServletRequest req = FacesUtils.getHttpRequest(fc);
            String reqURI = req.getRequestURI();
            int iSession = reqURI.indexOf(';');
            if (iSession>0)
            	reqURI = reqURI.substring(0,iSession);
            
            String loginPage = getPageUri(SamplePages.LoginPage.getOutcome().toString());
            if (reqURI.equalsIgnoreCase(loginPage))
            {   
                log.debug("Showing login page {}.", loginPage);
                return;
            }
            FacesUtils.redirectDirectly(fc, SamplePages.LoginPage);
        }
        else
        {   // Check Logout
            HttpServletRequest req = FacesUtils.getHttpRequest(fc);
            Object logout = req.getParameter(LOGOUT_PARAM);
            if (logout!=null && ObjectUtils.getBoolean(logout))
            {   // Perform logout
                doLogout(fc);
                // return to login page
                String redirectPage = req.getRequestURI();
                log.info("User logout performed. Returning to start page {}!", redirectPage);
                FacesUtils.redirectDirectly(fc, redirectPage);
            }
        }
        
     
    }
    
    /******************************************************************************************/

    private void doLogout(FacesContext fc)
    {
        fc.getExternalContext().invalidateSession();
    }
    
   
    
    private String getPageUri(String page)
    {
        if (StringUtils.isEmpty(page))
            throw new InvalidArgumentException("page", page);
        if (page.startsWith("http"))
            return page;
        if (page.startsWith("/")==false)
            page="/"+page;
        // Detect page uri
        String pathPrefix = SampleUtils.getContextPath();
        String pageUri = pathPrefix + page;
        return pageUri;
    }

 
    
}
