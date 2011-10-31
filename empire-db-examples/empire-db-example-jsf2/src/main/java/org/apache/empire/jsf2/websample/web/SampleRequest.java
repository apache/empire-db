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

import java.sql.Connection;

import javax.faces.context.FacesContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SampleRequest
{
    private static final Logger                  log         = LoggerFactory.getLogger(SampleRequest.class);
    private static int                           instIdCount = 1000;

    // The HashMap of error objects for the local thread
    private static final ThreadLocal<SampleRequest> requestMap  = new ThreadLocal<SampleRequest>();

    public static SampleRequest get()
    {
        return SampleRequest.requestMap.get();
    }

    public static final String REQUEST_ATTRIBUTE_NAME = "sampleRequest";

    private ServletRequest     httpRequest;
    private ServletResponse    httpResponse;
    private SampleApplication        app                    = null;
    private Connection         conn                   = null;
    private boolean            commit                 = true;
    private int                instId                 = -1;

    SampleRequest(ServletRequest request, ServletResponse response)
    {
        this.httpRequest = request;
        this.httpResponse = response;
        SampleRequest.requestMap.set(this);
        // Count instances
        this.instId = SampleRequest.instIdCount++;
        if (SampleRequest.log.isDebugEnabled())
        {
        	SampleRequest.log.debug("REQUEST {}: Created.", this.instId);
        }
    }

    /**
     * Makes sure, the Request is valid and has not already been disposed!
     */
    public void checkDisposed(boolean throwException)
    {
        if (this.httpRequest != null)
        {
            return;
        }
        // Should never happen
        SampleRequest.log.warn("REQUEST {}: Already disposed!", this.instId);
        if (throwException)
        {
            throw new RuntimeException("FWSRequest has aready been disposed.");
        }
    }

    public HttpServletRequest getHttpRequest()
    {
        checkDisposed(true);
        return (HttpServletRequest) this.httpRequest;
    }

    public HttpServletResponse getHttpResponse()
    {
        checkDisposed(true);
        return (HttpServletResponse) this.httpResponse;
    }

    public int getId()
    {
        return this.instId;
    }

    public SampleApplication getApplication()
    {
        checkDisposed(true);
        if (this.app == null)
        {
            this.app = (SampleApplication) FacesContext.getCurrentInstance().getExternalContext().getApplicationMap()
            .get("app");
        }
        return this.app;
    }

    Connection getConnection()
    {
        checkDisposed(true);
        if (this.conn == null)
        {
            this.conn = getApplication().getPooledConnection();
        }
        SampleRequest.log.info("REQUEST {}: connection obtained.", this.instId);
        return this.conn;
    }

    public void setFailure(Throwable e)
    {
        checkDisposed(true);
        SampleRequest.log.error("REQUEST " + String.valueOf(this.instId)
                             + ": failed due to exception. Database operations will be rolled back!", e);
        this.commit = false;
    }

    public void dispose()
    {
        checkDisposed(true);
        try
        {
            // Dispose
            if (SampleRequest.log.isDebugEnabled())
            {
            	SampleRequest.log.info("REQUEST {}: disposing.", getId());
            }
            // Cleanup
            if (this.conn != null)
            { // Commit or rollback connection depending on the exit code
                this.app.releaseConnection(this.conn, this.commit);
                this.conn = null;
            }
            // Count instances
        }
        finally
        {
        	SampleRequest.requestMap.remove();
            this.httpRequest = null;
            this.httpResponse = null;
        }
    }

}
