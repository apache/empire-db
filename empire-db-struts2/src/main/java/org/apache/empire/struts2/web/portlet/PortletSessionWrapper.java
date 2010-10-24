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
package org.apache.empire.struts2.web.portlet;

import java.util.Enumeration;

import org.apache.empire.struts2.web.AppContext;
import org.apache.empire.struts2.web.SessionContext;

public class PortletSessionWrapper implements SessionContext {
	
	private javax.portlet.PortletSession session;
	public PortletSessionWrapper(javax.portlet.PortletSession session) {
		this.session = session;
	}

	public Object getExternalSession(){
		return session;
	}
	public AppContext getAppContext() {
		return new PortletContextWrapper(session.getPortletContext());
	}
	
	public String getId() {
		return session.getId();
	}
	public boolean isNew() {
		return session.isNew();
	}
	public void invalidate() {
		session.invalidate();
	}

	public long getCreationTime() {
		return session.getCreationTime();
	}
	public long getLastAccessedTime() {
		return session.getLastAccessedTime();
	}
	public int getMaxInactiveInterval() {
		return session.getMaxInactiveInterval();
	}
	public void setMaxInactiveInterval(int interval) {
		session.setMaxInactiveInterval(interval);
	}
	
	@SuppressWarnings("unchecked")
	public Enumeration<String> getAttributeNames() {
		return session.getAttributeNames();
	}
	public Object getAttribute(String name) {
		return session.getAttribute(name);
	}
	public void setAttribute(String name, Object value) {
		session.setAttribute(name, value);
	}
	public void removeAttribute(String name) {
		session.removeAttribute(name);
	}

}
