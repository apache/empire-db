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

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBCommand;
import org.apache.empire.db.DBReader;
import org.apache.empire.rest.EmpireColumn;
import org.apache.empire.rest.EmpireColumnMeta;
import org.apache.empire.vuesample.model.db.EmployeeBean;
import org.apache.empire.vuesample.model.db.SampleDB;
import org.apache.empire.vuesample.model.db.SampleDB.TEmployees;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/employee")
public class EmployeeService extends Service {

	private static final Logger log = LoggerFactory.getLogger(EmployeeService.class);

	@POST
	@Path("/{employeeId}")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response updateEmployee(@PathParam("employeeId") int employeeId, EmployeeBean employee) {

		SampleDB db = getDatabase();
		TEmployees TE = db.T_EMPLOYEES;

		DBCommand cmd = db.createCommand();

		// First name
		cmd.set(TE.FIRST_NAME.to(TE.FIRST_NAME.validate(getValue(employee.getFirstName()))));

		// Last name
		cmd.set(TE.LAST_NAME.to(TE.LAST_NAME.validate(getValue(employee.getLastName()))));

		// Date of Birth
		cmd.set(TE.DATE_OF_BIRTH.to(TE.DATE_OF_BIRTH.validate(getValue(employee.getDateOfBirth()))));

		cmd.where(TE.EMPLOYEE_ID.is(employeeId));

		int executeUpdate = db.executeUpdate(cmd, getConnection());

		if (executeUpdate == 0) {
			return Response.status(Status.NOT_FOUND).build();
		} else {
			return Response.ok().build();
		}

	}

	@GET
	@Path("/{employeeId}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getEmployee(@PathParam("employeeId") int employeeId) {

		SampleDB db = getDatabase();

		TEmployees TE = db.T_EMPLOYEES;

		DBCommand cmd = db.createCommand();
		cmd.select(TE.EMPLOYEE_ID, TE.LAST_NAME, TE.FIRST_NAME, TE.DATE_OF_BIRTH);
		cmd.where(TE.EMPLOYEE_ID.is(employeeId));

		DBReader reader = new DBReader();
		EmployeeBean eb = new EmployeeBean();

		try {
			reader.open(cmd, getConnection());

			if (!reader.moveNext()) {
				// Employee not found
				return Response.status(Status.NOT_FOUND).build();
			}

			eb = createEmployee(reader);

		} finally {
			reader.close();
		}

		return Response.ok(eb).build();
	}

	@GET
	@Path("/list/")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getEmployeeList() {

		SampleDB db = getDatabase();

		TEmployees TE = db.T_EMPLOYEES;

		DBCommand cmd = db.createCommand();
		cmd.select(TE.EMPLOYEE_ID, TE.LAST_NAME, TE.FIRST_NAME, TE.DATE_OF_BIRTH);

		DBReader reader = new DBReader();
		List<EmployeeBean> list = new ArrayList<>();

		try {
			reader.open(cmd, getConnection());
			while (reader.moveNext()) {
				list.add(createEmployee(reader));
			}

		} finally {
			reader.close();
		}

		return Response.ok(list).build();

	}

	private EmployeeBean createEmployee(DBReader reader) {

		TEmployees TE = getDatabase().T_EMPLOYEES;

		EmployeeBean eb = new EmployeeBean();
		eb.setEmployeeId(createEmpireColumn(reader, TE.EMPLOYEE_ID));
		eb.setLastName(createEmpireColumn(reader, TE.LAST_NAME));
		eb.setFirstName(createEmpireColumn(reader, TE.FIRST_NAME));
		eb.setDateOfBirth(createEmpireColumn(reader, TE.DATE_OF_BIRTH));

		return eb;
	}

	private EmpireColumn createEmpireColumn(DBReader reader, DBColumn col) {

		EmpireColumn ec = new EmpireColumn();
		// Value
		ec.setValue(reader.getValue(col));
		// Meta
		EmpireColumnMeta meta = new EmpireColumnMeta();
		meta.setSize(col.getSize());
		meta.setType(col.getDataType().name());
		meta.setRequired(col.isRequired());
		meta.setReadonly(col.isReadOnly());
		ec.setMeta(meta);

		return ec;
	}

	private Object getValue(EmpireColumn ec) {
		if (ec == null) {
			return null;
		}
		return ec.getValue();
	}

}
