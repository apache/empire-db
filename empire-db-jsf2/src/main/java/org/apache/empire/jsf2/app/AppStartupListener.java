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
import javax.faces.event.PostConstructApplicationEvent;
import javax.faces.event.PreDestroyApplicationEvent;
import javax.faces.event.SystemEvent;
import javax.faces.event.SystemEventListener;
import javax.servlet.ServletContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class AppStartupListener implements SystemEventListener
{
    final Log log = LogFactory.getLog(AppStartupListener.class);

    @Override
    public boolean isListenerForSource(Object source)
    {
        return (source instanceof Application);
    }

    @Override
    public void processEvent(SystemEvent event)
        throws AbortProcessingException
    {
        log.info("ApplicationStartupListener:processEvent");
        if (event instanceof PostConstructApplicationEvent)
        {
            Application app = ((PostConstructApplicationEvent) event).getApplication();
            if (!(app instanceof FacesApplication))
                throw new AbortProcessingException("Error: Application is not a "+FacesApplication.class.getName()+" instance. Please create a ApplicationFactory!");
            // Create and Init application
            ServletContext servletContext = (ServletContext) FacesContext.getCurrentInstance().getExternalContext().getContext();
            FacesApplication jsfApp = (FacesApplication)app;
            jsfApp.init(servletContext);
            // Set Servlet Attribute
            servletContext.setAttribute(jsfApp.getApplicationBeanName(), jsfApp);
            // done
            jsfApp.initComplete();
        }
        else if (event instanceof PreDestroyApplicationEvent)
        {
            log.info("Processing PreDestroyApplicationEvent");
        }

    }

}
