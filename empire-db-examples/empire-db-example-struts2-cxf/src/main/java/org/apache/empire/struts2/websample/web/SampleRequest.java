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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.empire.samples.cxf.wssample.client.EmployeeManagementProxy;
import org.apache.empire.struts2.web.EmpireStrutsDispatcher;
import org.apache.empire.struts2.web.WebRequest;


public class SampleRequest implements WebRequest
{
    // Logger
    protected static Log log = LogFactory.getLog(SampleRequest.class);

    private HttpServletRequest  httpRequest;
    private HttpServletResponse httpResponse;
    private SampleSession       session;
    private EmployeeManagementProxy          ws;  // Connection for this request
    
    public static SampleRequest getInstance()
    {
        return (SampleRequest)EmpireStrutsDispatcher.getCurrentRequest();        
    }
    
    public boolean init(HttpServletRequest request, HttpServletResponse response, Object session)
    {
        this.httpRequest = request;
        this.httpResponse = response;
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
    	//TODO: how to roll back on webservice?
        // Cleanup
        if (ws!=null)
        {   // Commit or rollback connection depending on the exit code
            if (exitCode>=0)
            {   // 0 or positive exitCode indicates success
                log.debug("Request ended sucessfully. Committing database changes.");
                //getApplication().getDatabase().commit(connection);
            }
            else 
            {   // negative exitCode indicates an error
                log.warn("Request ended with errors. Database changes will be rolled back.");
                //getApplication().getDatabase().rollback(connection);
            }
            // Release Connection
            getApplication().releaseEmployeeProxy(ws);
            ws= null;
        }
        // Release objects
        this.httpRequest = null;
        this.httpResponse = null;
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

    // Get Connection
    public EmployeeManagementProxy getEmployeeManagementProxy()
    {
        // Get a Connection for the Connection Pool 
        if (ws==null)
        	ws= getApplication().getWebServiceProxy();
        // return connection
        return ws;
    }
    
    public HttpServletRequest getHttpRequest()
    {
        return httpRequest;
    }

    public HttpServletResponse getHttpResponse()
    {
        return httpResponse;
    }
    
}
