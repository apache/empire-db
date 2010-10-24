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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Set;

public interface AppContext {

	String getContextName();
    Object getExternalContext();
    boolean isPortlet();

	/* Resource Info */
    String getMimeType(String file);
    String getRealPath(String path);
    Set<String> getResourcePaths(String path);
    URL getResource(String path) throws MalformedURLException;

	/* logging */
	void log(java.lang.String msg);
	void log(java.lang.String message, java.lang.Throwable throwable);

	/* InitParam accessors */
	Enumeration<String> getInitParameterNames();
	String getInitParameter(java.lang.String name);

	/* Attribute accessors */
	Enumeration<String> getAttributeNames();
	Object getAttribute(java.lang.String name);
	void removeAttribute(java.lang.String name);
	void setAttribute(java.lang.String name, java.lang.Object object);
}
