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
package org.apache.empire.struts2.websample.web;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.empire.struts2.web.EmpireThreadManager;
import org.apache.empire.struts2.web.RequestContext;
import org.apache.empire.struts2.web.ResponseContext;
import org.apache.empire.struts2.web.WebRequest;


public class SampleRequest implements WebRequest
{
    // Logger
    protected static Log log = LogFactory.getLog(SampleRequest.class);

    private RequestContext  requestContext;
    private ResponseContext	responseContext;
    private SampleSession   session;
    
    public static SampleRequest getInstance()
    {
        return (SampleRequest)EmpireThreadManager.getCurrentRequest();        
    }
    
    public boolean init(RequestContext request, ResponseContext response, Object session)
    {
        this.requestContext = request;
        this.responseContext = response;
        // Set Internal objects
        this.session = (SampleSession)session;
        if (this.session==null)
        {   // Error
            log.fatal("Internal Error: Session object is null");
            return false;
        }
        // continue processing
        return true;
    }

    public void exit(int exitCode)
    {
        // Release objects
        this.requestContext = null;
        this.responseContext = null;
    }

    // Get Session
    public SampleSession getSession()
    {
        return session;
    }

    // Get Application
    public SampleApplication getApplication()
    {
        return session.getApplication();
    }
    
    // Get Request Context
    public RequestContext getRequestContext()
    {
        return requestContext;
    }

    // Get Response Context
    public ResponseContext getResponseContext()
    {
        return responseContext;
    }
    
}
