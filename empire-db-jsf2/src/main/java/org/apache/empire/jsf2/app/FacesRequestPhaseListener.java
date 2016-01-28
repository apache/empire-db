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
package org.apache.empire.jsf2.app;

import javax.faces.context.FacesContext;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FacesRequestPhaseListener implements PhaseListener
{
    private static final long   serialVersionUID = 1L;
    final Logger                log              = LoggerFactory.getLogger(FacesRequestPhaseListener.class);

    public FacesRequestPhaseListener()
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
        // Only when rendering the response
        if (pe.getPhaseId() == PhaseId.RENDER_RESPONSE)
        {   
            FacesContext facesContext = pe.getFacesContext();
            HttpServletResponse response = (HttpServletResponse) facesContext.getExternalContext().getResponse();
            response.addHeader("Pragma", "no-cache");
            response.addHeader("Cache-Control", "no-cache");
            response.addHeader("Cache-Control", "no-store");
            response.addHeader("Cache-Control", "must-revalidate");
        }
        // default
    }

    /** 
     * VERY VERY IMPORTANT FUNCTION: Release Connection on End of request! 
     **/
    @Override
    public void afterPhase(PhaseEvent pe)
    {
        // Cleanup
        FacesContext ctx = pe.getFacesContext();
        if (pe.getPhaseId() == PhaseId.RENDER_RESPONSE || ctx.getResponseComplete())
        {
            FacesApplication app = FacesApplication.getInstance();
            if (app!=null)
                app.onRequestComplete(ctx);
            else
                log.warn("No FacesApplication available to complete and cleanup request. Please create a managed bean of name "+FacesApplication.APPLICATION_BEAN_NAME);
        }
            
    }

}
