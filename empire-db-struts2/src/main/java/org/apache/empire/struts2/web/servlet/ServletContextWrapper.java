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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Set;

import javax.servlet.ServletContext;

import org.apache.empire.struts2.web.AppContext;

public class ServletContextWrapper implements AppContext {
	
	private ServletContext ctx;
	public ServletContextWrapper(ServletContext ctx) {
		this.ctx = ctx;
	}

	public String getContextName() {
		return ctx.getServletContextName();
	}
	
	public Object getExternalContext() {
		return ctx;
	}

	public String getMimeType(String file) {
		return ctx.getMimeType(file);
	}

    public boolean isPortlet() {
    	return false;
    }
	
	public String getRealPath(String path) {
		return ctx.getRealPath(path);
	}

	@SuppressWarnings("unchecked")
	public Set<String> getResourcePaths(String path) {
		return ctx.getResourcePaths(path);
	}

	public URL getResource(String path) throws MalformedURLException {
		return ctx.getResource(path);
	}

	public void log(java.lang.String msg) {
		ctx.log(msg);
	}

	public void log(java.lang.String message, java.lang.Throwable throwable) {
		ctx.log(message, throwable);
	}

	@SuppressWarnings("unchecked")
	public Enumeration<String> getInitParameterNames() {
		return ctx.getInitParameterNames();
	}

	public String getInitParameter(java.lang.String name) {
		return ctx.getInitParameter(name);		
	}

	@SuppressWarnings("unchecked")
	public Enumeration<String> getAttributeNames() {
		return ctx.getAttributeNames();
	}

	public Object getAttribute(java.lang.String name) {
		return ctx.getAttribute(name);
	}

	public void removeAttribute(java.lang.String name) {
		ctx.removeAttribute(name);
	}

	public void setAttribute(java.lang.String name, java.lang.Object object) {
		ctx.setAttribute(name, object);
	}
	
}
