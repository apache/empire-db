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
package org.apache.empire.struts2.websample.web.actiontypes;

import java.util.Locale;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.empire.samples.cxf.wssample.client.EmployeeServiceClient;
import org.apache.empire.struts2.action.WebAction;
import org.apache.empire.struts2.websample.common.Errors;
import org.apache.empire.struts2.websample.web.SampleApplication;
import org.apache.empire.struts2.websample.web.SampleRequest;
import org.apache.empire.struts2.websample.web.SampleSession;

public abstract class SampleAction extends WebAction
{
    // Logger
    protected static Log log = LogFactory.getLog(SampleAction.class);
    private EmployeeServiceClient service = null;

    public SampleAction()
    {
        // Constructor
    }

    // Login requried for this action
    @Override
    public boolean loginRequired()
    {
        return (getSession().getUser() == null);
    }

    @Override
    public Locale getLocale()
    {
        if (getRequest().getSession().getUser() == null)
            return Locale.ENGLISH;
        else
            return getRequest().getSession().getUser().getLocale();
    }

    // Request
    public static SampleRequest getRequest()
    {
        return SampleRequest.getInstance();
    }

    // Application getters
    protected SampleApplication getApplication()
    {
        return getRequest().getApplication();
    }

    protected SampleSession getSession()
    {
        return getRequest().getSession();
    }

    protected boolean checkWebService()
    {
        if (isServiceAvailable())
            return true;
        // Serv
        setActionError(Errors.WebServiceNotAvailable);
        return false;
    }
    
    protected boolean isServiceAvailable()
    {
        try
        {
            return getEmployeeServiceClient().ping();
        } catch (Exception e)
        {
            return false;
        }
    }
    
    protected EmployeeServiceClient getEmployeeServiceClient()
    {
        if (service==null)
            service= SampleApplication.getInstance().getEmployeeServiceClient();
        return service;
    }

}
