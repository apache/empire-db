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

import java.sql.Connection;

import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;

import org.apache.empire.vuesample.model.EmpireServiceConsts;
import org.apache.empire.vuesample.model.db.SampleDB;
import org.glassfish.jersey.server.ContainerRequest;

public abstract class Service {

	@Context
	private ServletContext			context;

	@Context
	private ContainerRequestContext	containerRequestContext;

	@Context
	private HttpServletRequest		req;

	public SampleDB getDatabase() {
		return (SampleDB) this.context.getAttribute(EmpireServiceConsts.ATTRIBUTE_DB);
	}

	public Connection getConnection() {
		ContainerRequest containerRequest = (ContainerRequest) this.containerRequestContext;
		return (Connection) containerRequest.getProperty(EmpireServiceConsts.ATTRIBUTE_CONNECTION);
	}

	public ServletRequest getServletRequest() {
		return this.req;
	}

	public String getRemoteAddr() {
		// TODO check X-Real-IP / X-Forwarded-For
		return this.req.getRemoteAddr();
	}

}
