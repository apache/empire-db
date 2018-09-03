package org.apache.empire.rest.service;

import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        JsonObjectBuilder jsonObjBuilder;
        // Check params
        if (StringUtils.isEmpty(username) || StringUtils.isEmpty(password))
        {  
            jsonObjBuilder = Json.createObjectBuilder();
            jsonObjBuilder.add("error", "Invalid username or password.");
            JsonObject jsonObj = jsonObjBuilder.build();
            return Response.status(Response.Status.BAD_REQUEST).entity(jsonObj.toString()).build();
        }

        if (username.equals("bad") || password.equals("password"))
        {
            jsonObjBuilder = Json.createObjectBuilder();
            jsonObjBuilder.add("error", "Bad user or password");
            JsonObject jsonObj = jsonObjBuilder.build();
            return Response.status(Response.Status.UNAUTHORIZED).entity(jsonObj.toString()).build();
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
