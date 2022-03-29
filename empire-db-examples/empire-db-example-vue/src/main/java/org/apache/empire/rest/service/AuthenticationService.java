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
package org.apache.empire.rest.service;

import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

import org.apache.empire.commons.StringUtils;
import org.apache.empire.rest.json.JsoErrorInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@io.swagger.annotations.Api(value = "auth")
@Path("/auth")
public class AuthenticationService
{
    private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);

    @POST
    @Path("login")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response login(@FormParam("username") String username, @FormParam("password") String password)
    {
        // Check params
        if (StringUtils.isEmpty(username) || StringUtils.isEmpty(password))
        {   // empty username or password
            JsoErrorInfo errorInfo = new JsoErrorInfo("Invalid username or password.");
            return Response.status(Response.Status.BAD_REQUEST).entity(errorInfo).build();
        }
        // Validate
        if (username.equals("bad") || password.equals("password"))
        {   // simulate invalid username / password
            JsoErrorInfo errorInfo = new JsoErrorInfo("Bad user or password");
            return Response.status(Response.Status.UNAUTHORIZED).entity(errorInfo).build();
        }
        log.info("Log in for user {}", username);
        // Set Cookie
        String cookieValue = UUID.randomUUID().toString().replaceAll("-", "");
        NewCookie cookie = new NewCookie(Service.Consts.LOGIN_COOKIE_NAME, cookieValue, "/", null, null, NewCookie.DEFAULT_MAX_AGE, false);
        // OK
        return Response.ok().cookie(cookie).build();
    }

    @POST
    @Path("logout")
    public Response logout(@Context HttpServletRequest servletRequest)
    {
        NewCookie cookie = new NewCookie(Service.Consts.LOGIN_COOKIE_NAME, null, "/", null, null, 0, false);
        return Response.status(Response.Status.NO_CONTENT).cookie(cookie).build();
    }

}
