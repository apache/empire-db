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
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.PostConstructApplicationEvent;
import javax.faces.event.PreDestroyApplicationEvent;
import javax.faces.event.SystemEvent;
import javax.faces.event.SystemEventListener;
import javax.servlet.ServletContext;

import org.apache.empire.jsf2.impl.FacesImplementation;
import org.apache.empire.jsf2.impl.MojarraImplementation;
import org.apache.empire.jsf2.impl.MyFacesImplementation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebAppStartupListener implements SystemEventListener
{
    private static final Logger log = LoggerFactory.getLogger(WebAppStartupListener.class);
    
    private final Class<? extends FacesConfiguration> facesConfigClass;

    private FacesImplementation facesImplementation;
    
    /**
     * Default Constructor with no initialization
     */
    public WebAppStartupListener()
    {
        this.facesConfigClass = null;
    }

    /**
     * Default Constructor with additional configuration
     */
    public WebAppStartupListener(Class<? extends FacesConfiguration> facesConfigClass)
    {
        this.facesConfigClass = facesConfigClass;
    }

    public FacesImplementation getFacesImplementation()
    {
        return facesImplementation;
    }
    
    protected ServletContext getServletContext(FacesContext startupContext)
    {
        return (ServletContext)startupContext.getExternalContext().getContext();        
    }

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
            ExternalContext externalContext = startupContext.getExternalContext();
            // Detect implementation
            this.facesImplementation = detectFacesImplementation(externalContext);
            // Init Configuration
            initFacesConfiguration(startupContext);
            // Create and init application
            WebApplication facesApp = createWebApplication(startupContext);
            initWebApplication(facesApp, startupContext);
            // Set Servlet Attribute
            ServletContext servletContext = (ServletContext) externalContext.getContext();
            if (servletContext.getAttribute(WebApplication.APPLICATION_BEAN_NAME)!=facesApp)
            {
                log.warn("WARNING: Ambiguous application definition. An object of name '{}' already exists on application scope!", WebApplication.APPLICATION_BEAN_NAME);
            }
        }
        else if (event instanceof PreDestroyApplicationEvent)
        {
            log.info("Processing PreDestroyApplicationEvent");
        }

    }

    /**
     * Detects the JSF Implementation and returns an implmentation handler class.
     * Supported Implementations are Sun Mojarra (2.2.x) and Apache MyFaces (2.2.x) 
     * @return the faces implementation
     */
    protected FacesImplementation detectFacesImplementation(ExternalContext externalContext)
    {
        log.debug("Detecting JSF-Implementation...");
        // Test for Apache MyFaces
        try {
            Class.forName("org.apache.myfaces.application.ApplicationFactoryImpl");
            return new MyFacesImplementation(externalContext);
        } catch (ClassNotFoundException e) {
            // It's not MyFaces
        }
        // Test for Sun Mojarra
        try {
            Class.forName("com.sun.faces.application.ApplicationFactoryImpl");
            return new MojarraImplementation(externalContext);
        } catch (ClassNotFoundException e) {
            // It's not Mojarra
        }
        // Not found
        log.error("JSF-Implementation missing or unknown. Please make sure either Apache MyFaces or Sun Mojarra implementation is provided");
        throw new UnsupportedOperationException(); 
    }

    /**
     * Allows to programmatically extend the faces configuration
     * @param startupContext the Startup Context
     */
    protected void initFacesConfiguration(FacesContext startupContext)
    {
        // Init FacesExtentions
        if (facesConfigClass!=null) {
            log.info("Initializing FacesExtentions");
            FacesConfiguration.initialize(facesConfigClass, startupContext, getFacesImplementation());
        }
    }

    /**
     * Creates a WebApplication instance
     * @param startupContext the Startup Context
     * @return the WebApplication instance
     */
    protected WebApplication createWebApplication(FacesContext startupContext)
    {
        // Create Application
        Object app = getFacesImplementation().getManagedBean(WebApplication.APPLICATION_BEAN_NAME, startupContext);
        if (!(app instanceof WebApplication))
            throw new AbortProcessingException("Error: Application is not a "+WebApplication.class.getName()+" instance. Please create an ApplicationFactory!");
        // done
        return (WebApplication)app;
    }
    
    /**
     * Initializes a WebApplication instance
     * @param facesApp the WebApplication instance
     * @param startupContext the Startup Context
     */
    protected void initWebApplication(WebApplication facesApp, FacesContext startupContext)
    {
        facesApp.init(getFacesImplementation(), startupContext);
    }
    
}
