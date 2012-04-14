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

import javax.faces.application.Application;
import javax.faces.context.FacesContext;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class AppRequestPhaseListener implements PhaseListener
{
    private static final long   serialVersionUID = 1L;
    final Logger                log              = LoggerFactory.getLogger(AppRequestPhaseListener.class);

    /*
    private static final String ACTION_PARAM     = "action";
    private static final String REDIRECT_PARAM   = "redirect";
    private static final String REDIRECT_RESULT  = "redirect:";
    */

    public AppRequestPhaseListener()
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
        // Check for action param
        /*
        if (pe.getPhaseId().equals(PhaseId.RESTORE_VIEW))
        {
            beforeRestoreView(pe.getFacesContext());
        }
        */
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
            Application app = ctx.getApplication();
            if (!(app instanceof FacesApplication))
                throw new AbortProcessingException("Error: Application is not a JsfApplication instance. Please create a ApplicationFactory!");
            // Cast and release 
            FacesApplication jsfApp = (FacesApplication)app;
            jsfApp.releaseAllConnections(ctx);
        }
            
    }

}
