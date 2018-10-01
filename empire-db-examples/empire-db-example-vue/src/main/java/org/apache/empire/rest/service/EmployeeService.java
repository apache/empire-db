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

import org.apache.empire.commons.Options;
import org.apache.empire.db.DBColumnExpr;
import org.apache.empire.db.DBCommand;
import org.apache.empire.db.DBJoinType;
import org.apache.empire.db.DBReader;
import org.apache.empire.exceptions.EmpireException;
import org.apache.empire.rest.app.SampleServiceApp;
import org.apache.empire.rest.app.TextResolver;
import org.apache.empire.rest.json.JsoColumnMeta;
import org.apache.empire.rest.json.JsoRecordData;
import org.apache.empire.rest.json.JsoResultWithMeta;
import org.apache.empire.vue.sample.db.SampleDB;
import org.apache.empire.vue.sample.db.SampleDB.TDepartments;
import org.apache.empire.vue.sample.db.SampleDB.TEmployees;
import org.apache.empire.vue.sample.db.records.EmployeeRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/employee")
public class EmployeeService extends Service {

	private static final Logger log = LoggerFactory.getLogger(EmployeeService.class);

	@GET
    @Path("/filter")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getEmployeeFilter() {

        TextResolver txtres = SampleServiceApp.instance().getTextResolver(Locale.ENGLISH);

        // Query Department options
        SampleDB db = getDatabase();
        DBCommand cmd = db.createCommand();
        cmd.select(db.T_DEPARTMENTS.DEPARTMENT_ID, db.T_DEPARTMENTS.NAME);
        cmd.join  (db.T_DEPARTMENTS.DEPARTMENT_ID, db.T_EMPLOYEES.DEPARTMENT_ID);
        cmd.groupBy(cmd.getSelectExprList());
        cmd.orderBy(db.T_DEPARTMENTS.NAME);
        Options departmentOptions = db.queryOptionList(cmd, getRecordContext().getConnection());
        
        // Create Metadata
        TEmployees TE = db.T_EMPLOYEES;
        JsoColumnMeta[] meta = new JsoColumnMeta[] { 
          new JsoColumnMeta(TE.EMPLOYEE_ID, txtres),
          new JsoColumnMeta(TE.FIRST_NAME, txtres),
          new JsoColumnMeta(TE.LAST_NAME, txtres),
          new JsoColumnMeta(TE.DEPARTMENT_ID, txtres, departmentOptions, false, false, false),
          new JsoColumnMeta(TE.GENDER, txtres),
        };
        
        JsoRecordData filter = new JsoRecordData(meta);
        return Response.ok(new JsoResultWithMeta(filter, meta)).build();
    }

    @POST
    @Path("/list/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getEmployeeList(JsoRecordData filter) {

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

        // apply all filters
        if (filter.hasNonNullValue(TE.FIRST_NAME))
            cmd.where(TE.FIRST_NAME.like(filter.getString(TE.FIRST_NAME) + "%"));
        if (filter.hasNonNullValue(TE.LAST_NAME))
            cmd.where(TE.LAST_NAME.like(filter.getString(TE.FIRST_NAME) + "%"));
        if (filter.hasNonNullValue(TE.GENDER))
            cmd.where(TE.GENDER.is(filter.getValue(TE.GENDER)));
        if (filter.hasNonNullValue(TE.DEPARTMENT_ID))
            cmd.where(TE.DEPARTMENT_ID.is(filter.getValue(TE.DEPARTMENT_ID)));
        

        DBColumnExpr[] cols = cmd.getSelectExprList();
        JsoColumnMeta[] meta = new JsoColumnMeta[cols.length]; 
        TextResolver txtres = SampleServiceApp.instance().getTextResolver(Locale.ENGLISH);
        for (int i=0; i<meta.length; i++)
        {
            meta[i] = new JsoColumnMeta(cols[i], txtres);
        }
        
        DBReader reader = new DBReader();
        List<JsoRecordData> list = new ArrayList<>();
        try {
            reader.open(cmd, getConnection());
            while (reader.moveNext()) {
                list.add(new JsoRecordData(reader));
            }
        } finally {
            reader.close();
        }
        // done
        return Response.ok(new JsoResultWithMeta(list, meta)).build();
    }

	@GET
	@Path("/get/{employeeId}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getEmployee(@PathParam("employeeId") int employeeId) {

	    try {
	        // return a record
	        EmployeeRecord rec = new EmployeeRecord(getDatabase(), getRecordContext());
            rec.read(employeeId);
            JsoRecordData emp = new JsoRecordData(rec);
            return Response.ok(new JsoResultWithMeta(emp, rec.getMeta())).build();
	        
	    } catch(EmpireException e) {
	        log.error("Unable to load employee with id {}", employeeId);
	        return Response.serverError().build();
	    }
	}

    @GET
    @Path("/add")
    @Produces(MediaType.APPLICATION_JSON)
    public Response addEmployee() {

        try {
            // return a record
            EmployeeRecord rec = new EmployeeRecord(getDatabase(), getRecordContext());
            rec.create();
            JsoRecordData emp = new JsoRecordData(rec);
            return Response.ok(new JsoResultWithMeta(emp, rec.getMeta())).build();
            
        } catch(EmpireException e) {
            log.error("Unable to create an employee record");
            return Response.serverError().build();
        }
    }

    @POST
    @Path("/set")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateEmployee(JsoRecordData employeeData) {

        EmployeeRecord rec = new EmployeeRecord(getDatabase(), getRecordContext());
        rec.init(employeeData, employeeData.isNewRecord());
        rec.update();
        
        return Response.ok().build();
    }

    @GET
    @Path("/delete/{employeeId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteEmployee(@PathParam("employeeId") int employeeId) {

        try {
            // return a record
            SampleDB db = getDatabase();
            db.T_EMPLOYEES.deleteRecord(employeeId, getRecordContext().getConnection());
            return Response.ok().build();
            
        } catch(EmpireException e) {
            log.error("Unable to delete employee with id {}", employeeId);
            return Response.serverError().build();
        }
    }

}
