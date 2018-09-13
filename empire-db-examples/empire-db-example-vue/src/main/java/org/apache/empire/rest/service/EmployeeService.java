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
import java.util.Locale;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.empire.db.DBColumnExpr;
import org.apache.empire.db.DBCommand;
import org.apache.empire.db.DBJoinType;
import org.apache.empire.db.DBReader;
import org.apache.empire.rest.app.EmployeeVueApp;
import org.apache.empire.rest.app.TextResolver;
import org.apache.empire.rest.json.ColumnMetaData;
import org.apache.empire.rest.json.EmployeeData;
import org.apache.empire.rest.json.EmployeeSearchFilter;
import org.apache.empire.rest.json.ResultWithMeta;
import org.apache.empire.vuesample.model.db.SampleDB;
import org.apache.empire.vuesample.model.db.SampleDB.TDepartments;
import org.apache.empire.vuesample.model.db.SampleDB.TEmployees;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/employee")
public class EmployeeService extends Service {

	private static final Logger log = LoggerFactory.getLogger(EmployeeService.class);

	@GET
    @Path("/filter")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getEmployee() {

        EmployeeSearchFilter filter = new EmployeeSearchFilter(); 
	    
        TextResolver txtres = EmployeeVueApp.instance().getTextResolver(Locale.ENGLISH);
        
        SampleDB db = getDatabase();
        TEmployees TE = db.T_EMPLOYEES;
        ColumnMetaData[] meta = new ColumnMetaData[] { 
          new ColumnMetaData(TE.EMPLOYEE_ID, txtres),
          new ColumnMetaData(TE.FIRST_NAME, txtres),
          new ColumnMetaData(TE.LAST_NAME, txtres),
          new ColumnMetaData(TE.DEPARTMENT_ID, txtres),
          new ColumnMetaData(TE.GENDER, txtres),
        };
        
        return Response.ok(new ResultWithMeta(filter, meta)).build();
    }

    @POST
    @Path("/list/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getEmployeeList(EmployeeSearchFilter filter) {

        SampleDB db = getDatabase();

        TEmployees TE = db.T_EMPLOYEES;
        TDepartments TD = db.T_DEPARTMENTS;
        DBColumnExpr FULL_NAME  = TE.LAST_NAME.append(", ").append(TE.FIRST_NAME).as("NAME");
        DBColumnExpr DEPARTMENT = TD.NAME.as("DEPARTMENT");
        FULL_NAME.setTitle("!field.title.employees.fullname");

        log.info("Providing employee list...");

        DBCommand cmd = db.createCommand();
        cmd.select(TE.EMPLOYEE_ID, FULL_NAME, DEPARTMENT, TE.GENDER, TE.DATE_OF_BIRTH, TE.RETIRED);
        cmd.join  (TE.DEPARTMENT_ID, TD.DEPARTMENT_ID, DBJoinType.LEFT);

        DBColumnExpr[] cols = cmd.getSelectExprList();
        ColumnMetaData[] meta = new ColumnMetaData[cols.length]; 
        TextResolver txtres = EmployeeVueApp.instance().getTextResolver(Locale.ENGLISH);
        for (int i=0; i<meta.length; i++)
        {
            meta[i] = new ColumnMetaData(cols[i], txtres);
        }
        
        DBReader reader = new DBReader();
        List<EmployeeData> list = new ArrayList<>();
        try {
            reader.open(cmd, getConnection());
            while (reader.moveNext()) {
                list.add(new EmployeeData(reader));
            }
        } finally {
            reader.close();
        }
        // done
        return Response.ok(new ResultWithMeta(list, meta)).build();
    }

	@GET
	@Path("/get/{employeeId}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getEmployee(@PathParam("employeeId") int employeeId) {

		SampleDB db = getDatabase();

		TEmployees TE = db.T_EMPLOYEES;

		DBCommand cmd = db.createCommand();
		cmd.select(TE.EMPLOYEE_ID, TE.LAST_NAME, TE.FIRST_NAME, TE.DATE_OF_BIRTH);
		cmd.where(TE.EMPLOYEE_ID.is(employeeId));

		DBReader reader = new DBReader();
		try {
			reader.open(cmd, getConnection());

			if (!reader.moveNext()) {
				// Employee not found
				return Response.status(Status.NOT_FOUND).build();
			}

			EmployeeData emp = new EmployeeData(reader);
            return Response.ok(emp).build();

		} finally {
			reader.close();
		}
	}

    @POST
    @Path("/set")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateEmployee(EmployeeData employee) {
        /*
        SampleDB db = getDatabase();
        TEmployees TE = db.T_EMPLOYEES;
        */
        return Response.ok().build();
    }

}
