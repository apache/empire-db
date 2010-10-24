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
package org.apache.empire.struts2.web;

import java.security.Principal;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

public interface RequestContext {

	Object getExternalRequest();
	SessionContext getSessionContext();
	
	String getRequestURI();
	
	/* Request info */
    String getAuthType();
    String getContextPath();
    String getRemoteUser();
    Principal getUserPrincipal();
	boolean isUserInRole(String role);
	boolean isSecure();
    String getScheme();
    String getServerName();
    int getServerPort();
	  
	String getRequestedSessionId();
	boolean isRequestedSessionIdValid();

    Locale getLocale();
    Enumeration<Locale> getLocales();
	
	/* Parameter accessors */
    Enumeration<String> getParameterNames();
    String getParameter(String name);
    String[] getParameterValues(String name);
    Map<String,String> getParameterMap();
	
	/* Attribute accessors */
    Enumeration<String> getAttributeNames();
    Object getAttribute(String name);
    void setAttribute(String name, Object o);
    void removeAttribute(String name);
}
