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

import org.apache.empire.jsf2.app.impl.MojarraImplementation;
import org.apache.empire.jsf2.app.impl.MyFacesImplementation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FacesStartupListener implements SystemEventListener
{
    private static final Logger log = LoggerFactory.getLogger(FacesStartupListener.class);

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
            FacesContext startupContext = FacesContext.getCurrentInstance();
            // detect implementation
            FacesImplementation facesImplementation = detectFacesImplementation();
            Object app = facesImplementation.getManagedBean(FacesApplication.APPLICATION_BEAN_NAME, startupContext);
            if (!(app instanceof FacesApplication))
                throw new AbortProcessingException("Error: Application is not a "+FacesApplication.class.getName()+" instance. Please create a ApplicationFactory!");
            // Create and Init application
            FacesApplication facesApp = (FacesApplication)app;
            facesApp.init(facesImplementation, startupContext);
            // Set Servlet Attribute
            ServletContext servletContext = (ServletContext) startupContext.getExternalContext().getContext();
            if (servletContext.getAttribute(FacesApplication.APPLICATION_BEAN_NAME)!=facesApp)
            {
                log.warn("WARNING: Ambiguous application definition. An object of name '{}' already exists on application scope!", FacesApplication.APPLICATION_BEAN_NAME);
            }
        }
        else if (event instanceof PreDestroyApplicationEvent)
        {
            log.info("Processing PreDestroyApplicationEvent");
        }

    }

    private static FacesImplementation detectFacesImplementation()
    {
        // Test for Apache MyFaces
        try {
            Class.forName("org.apache.myfaces.application.ApplicationFactoryImpl");
            return new MyFacesImplementation();
        } catch (ClassNotFoundException e) {
            // It's not MyFaces
        }
        // Test for Sun Mojarra
        try {
            Class.forName("com.sun.faces.application.ApplicationFactoryImpl");
            return new MojarraImplementation();
        } catch (ClassNotFoundException e) {
            // It's not Mojarra
        }
        // Not found
        throw new UnsupportedOperationException(); 
    }
    
}
