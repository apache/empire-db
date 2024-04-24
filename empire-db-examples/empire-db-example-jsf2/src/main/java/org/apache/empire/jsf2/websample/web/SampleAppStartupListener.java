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
import javax.servlet.ServletContext;

import org.apache.empire.jsf2.app.FacesConfiguration;
import org.apache.empire.jsf2.app.WebAppStartupListener;
import org.apache.empire.jsf2.app.WebApplication;

/**
 * Custom StartupListener
 * Faces Configuration is done programmatically 
 * @author rainer
 */
public class SampleAppStartupListener extends WebAppStartupListener
{
    public final SampleConfig config = new SampleConfig();
    
    public SampleAppStartupListener()
    {
        super(FacesConfiguration.class);
    }
    
    @Override
    protected void initFacesConfiguration(FacesContext startupContext)
    {
        ServletContext servletContext = (ServletContext)startupContext.getExternalContext().getContext();
        config.init(servletContext.getRealPath("WEB-INF/config.xml"));
        // Load Configuration
        super.initFacesConfiguration(startupContext);
    }
    
    @Override
    protected void initWebApplication(WebApplication facesApp, FacesContext startupContext)
    {
        // Set Configuration
        ((SampleApplication)facesApp).setConfig(config);
        // init now
        super.initWebApplication(facesApp, startupContext);
    }
}
