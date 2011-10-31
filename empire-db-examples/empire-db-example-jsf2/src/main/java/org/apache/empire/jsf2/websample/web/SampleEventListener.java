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

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SampleEventListener implements ServletContextListener, ServletRequestListener
{
    private static final Logger log = LoggerFactory.getLogger(SampleEventListener.class);

    public SampleEventListener()
    {
        SampleEventListener.log.debug("EventListener created");
    }

    // *********************************************
    // *** ServletContextListener implementation ***
    // *********************************************
    public void contextInitialized(ServletContextEvent contextEvent)
    {
        SampleEventListener.log.info("EventListener: Application startup");

        // Get Context
        ServletContext ctx = contextEvent.getServletContext();

        // Create Application
        SampleApplication app = new SampleApplication();
        app.init(ctx);

        ctx.setAttribute("app", app);
        // done
        SampleEventListener.log.info("Application startup done");
    }

    public void contextDestroyed(ServletContextEvent contextEvent)
    {
        SampleEventListener.log.info("EventListener: Application shutdown");
    }

    // *********************************************
    // *** ServletRequestListener implementation ***
    // *********************************************
    private static int requestIdSeq = 5000;

    private boolean isException(ServletRequest request)
    {
        // THIS IS PART OF SPECIFICATION
        return request.getAttribute("javax.servlet.error.exception") != null;
    }

    public void requestInitialized(ServletRequestEvent sre)
    {
        int requestId = SampleEventListener.requestIdSeq++;
        ServletRequest request = sre.getServletRequest();
        request.setAttribute("requestId", requestId);
        if (SampleEventListener.log.isDebugEnabled())
        {
            SampleEventListener.log.debug("Starting faces request {}.", requestId);
        }
    }

    public void requestDestroyed(ServletRequestEvent sre)
    {
        ServletRequest request = sre.getServletRequest();
        int requestId = (Integer) request.getAttribute("requestId");

        if (SampleEventListener.log.isDebugEnabled())
        {
            SampleEventListener.log.debug("Destroying faces request {}.", requestId);
        }

        // Release connection (if any)
        Connection conn = FacesUtils.getRequestConnection(request);
        if (conn != null)
        {
            ServletContext ctx = sre.getServletContext();
            boolean commit = !isException(request);
            // Release
            SampleApplication app = (SampleApplication) ctx.getAttribute("app");
            app.releaseConnection(conn, commit);
        }
    }

}
