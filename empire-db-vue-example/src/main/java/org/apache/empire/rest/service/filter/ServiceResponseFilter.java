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
import java.sql.SQLException;

import javax.annotation.Priority;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Response.Status.Family;
import javax.ws.rs.ext.Provider;

import org.apache.empire.vuesample.model.EmpireServiceConsts;
import org.glassfish.jersey.server.ContainerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
@Priority(99999)
public class ServiceResponseFilter implements ContainerResponseFilter {

	private static final Logger log = LoggerFactory.getLogger(ServiceResponseFilter.class);

	@Override
	public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {

		// responseContext.getHeaders().putSingle(key, value);

		ContainerRequest containerRequest = (ContainerRequest) requestContext;
		Connection conn = (Connection) containerRequest.getProperty(EmpireServiceConsts.ATTRIBUTE_CONNECTION);

		boolean success = responseContext.getStatusInfo().getFamily() == Family.SUCCESSFUL;

		try {

			if (conn == null || conn.isClosed()) {
				// Connection not found or already closed
				return;
			}

			if (success) {
				// commit
				conn.commit();
			} else {
				// rollback
				conn.rollback();
			}

			// close connection / return to pool
			conn.close();

		} catch (SQLException e) {
			log.error("Error releasing connection", e);
			responseContext.setStatus(500);
		}

	}

}
