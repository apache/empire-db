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
import javax.faces.application.ApplicationFactory;

import org.apache.empire.exceptions.InternalException;
import org.apache.empire.exceptions.InvalidArgumentException;
import org.apache.empire.exceptions.ItemExistsException;
import org.apache.empire.jsf2.app.impl.MojarraImplementation;
import org.apache.empire.jsf2.app.impl.MyFacesImplementation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class FacesApplicationFactory extends ApplicationFactory
{
    private static final Logger  log = LoggerFactory.getLogger(FacesApplicationFactory.class);
    
    private final Class<? extends FacesApplication> applicationClass;

    private final FacesImplementation facesImplementation;

    private final AppStartupListener startupListener;
    
    private volatile FacesApplication application;
    
	private static FacesImplementation detectFacesImplementation()
	{
		// Test for Sun Mojarra
		try {
			Class.forName("com.sun.faces.application.ApplicationFactoryImpl");
			return new MojarraImplementation();
		} catch (ClassNotFoundException e) {
			// It's not Mojarra
		}
		// Test for Apache MyFaces
		try {
			Class.forName("org.apache.myfaces.application.ApplicationFactoryImpl");
			return new MyFacesImplementation();
		} catch (ClassNotFoundException e) {
			// It's not MyFaces
		}
		// Not found
		throw new UnsupportedOperationException(); 
	}
    
    protected FacesApplicationFactory(Class<? extends FacesApplication> applicationClass, FacesImplementation facesImplementation, AppStartupListener startupListener)
    {
    	// FacesImplementation
    	if (facesImplementation==null)
    		facesImplementation= detectFacesImplementation();

    	// FacesImplementation
    	this.facesImplementation = facesImplementation;
        this.applicationClass    = applicationClass;
        this.startupListener     = startupListener;

        // log
        log.info("FacesApplicationFactory created for {0} using Implemenation {1}.", applicationClass, facesImplementation.getClass().getName());
    }
    
    protected FacesApplicationFactory(Class<? extends FacesApplication> applicationClass)
    {
    	this(applicationClass, null, new AppStartupListener());
    }

    @Override
    public Application getApplication()
    {
        if (application == null)
        {   try
            {	// Create FacesApplication
                application = applicationClass.newInstance();
        		// init
        		facesImplementation.initApplication(application);
                // subscribe
                application.subscribeToEvent(javax.faces.event.PostConstructApplicationEvent.class, startupListener);
            }
            catch (InstantiationException e)
            {
                throw new InternalException(e);
            }
            catch (IllegalAccessException e)
            {
                throw new InternalException(e);
            }
            // log
            log.info("Fin2Application Application instance created");
        }
        return application;
    }

    @Override
    public void setApplication(Application application)
    {
        if (this.application != null)
            throw new ItemExistsException(this.application);
        if (!(application instanceof FacesApplication))
            throw new InvalidArgumentException("application", application);
        this.application = (FacesApplication)application;
    }
}
