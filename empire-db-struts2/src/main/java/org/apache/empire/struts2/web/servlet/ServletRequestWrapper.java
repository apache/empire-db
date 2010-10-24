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
package org.apache.empire.struts2.web.servlet;

import java.security.Principal;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.empire.struts2.web.RequestContext;
import org.apache.empire.struts2.web.SessionContext;

public class ServletRequestWrapper implements RequestContext {
	
	private HttpServletRequest req;
	public ServletRequestWrapper(HttpServletRequest req) {
		this.req = req;
	}

	public Object getExternalRequest(){
		return req;
	}
	public SessionContext getSessionContext(){
		return new ServletSessionWrapper(req.getSession());
	}
	
	public String getRequestURI() {
		return req.getRequestURI();
	}
	
	/* Request info */
	public String getAuthType(){
    	return req.getAuthType();
    }
	public String getContextPath(){
    	return req.getContextPath();
    }
	public String getRemoteUser(){
    	return req.getRemoteUser();
    }
	public Principal getUserPrincipal(){
    	return req.getUserPrincipal();
    }
	public boolean isUserInRole(String role){
		return req.isUserInRole(role);
	}
	public boolean isSecure(){
		return req.isSecure();
	}
	public String getScheme(){
    	return req.getScheme();
    }
	public String getServerName(){
    	return req.getServerName();
    }
    public int getServerPort(){
    	return req.getServerPort();
    }
	  
    public String getRequestedSessionId(){
    	return req.getRequestedSessionId();
    }
    public boolean isRequestedSessionIdValid() {
    	return req.isRequestedSessionIdValid();
    }

	public Locale getLocale(){
		return req.getLocale();
	}
    @SuppressWarnings("unchecked")
	public Enumeration<Locale> getLocales(){
    	return req.getLocales();
    }
	
	/* Parameter accessors */
	@SuppressWarnings("unchecked")
	public Enumeration<String> getParameterNames() {
		return req.getParameterNames();
	}
	public String getParameter(String name) {
		return req.getParameter(name);
	}
	public String[] getParameterValues(String name) {
		return req.getParameterValues(name);
	}
	@SuppressWarnings("unchecked")
	public Map<String,String> getParameterMap() {
		return req.getParameterMap();
	}
	
	/* Attribute accessors */
    @SuppressWarnings("unchecked")
	public Enumeration<String> getAttributeNames() {
    	return req.getAttributeNames();
    }
    public Object getAttribute(String name) {
    	return req.getAttribute(name);
    }
    public void setAttribute(String name, Object o) {
    	req.setAttribute(name, o);
    }
    public void removeAttribute(String name) {
    	req.removeAttribute(name);     	
    }
	
}
