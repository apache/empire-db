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
package org.apache.empire.rest.service.filter;

import java.io.IOException;
import java.sql.Connection;

import javax.annotation.Priority;
import javax.servlet.ServletContext;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import org.apache.empire.rest.app.SampleServiceApp;
import org.apache.empire.rest.service.Service;
import org.glassfish.jersey.server.ContainerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
@Priority(0)
public class ServiceRequestFilter implements ContainerRequestFilter
{

    private static final Logger log = LoggerFactory.getLogger(ServiceRequestFilter.class);

    @Context
    private ServletContext servletContext;

    @Override
    public void filter(ContainerRequestContext requestContext)
        throws IOException
    {
        String path = requestContext.getUriInfo().getPath();
        log.info("Filtering request path: " + path);
        // swagger
        if (path.startsWith("swagger."))
        {
            return;
        }
        // Check authentication
        if (!path.startsWith("auth") && !requestContext.getMethod().equals("OPTIONS"))
        {
            Cookie cookie = requestContext.getCookies().get(Service.Consts.LOGIN_COOKIE_NAME);
            if (cookie == null)
            {
                requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
                return;
            }
        }

        ContainerRequest containerRequest = (ContainerRequest) requestContext;

        // get Connection from pool
        Connection conn = SampleServiceApp.instance().getJDBCConnection(this.servletContext);

        // Add to context
        containerRequest.setProperty(Service.Consts.ATTRIBUTE_CONNECTION, conn);

    }

}
