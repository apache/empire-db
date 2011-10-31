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
package org.apache.empire.jsf2.websample.web.pages;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.empire.commons.StringUtils;
import org.apache.empire.db.DBColumnExpr;
import org.apache.empire.db.DBCommand;
import org.apache.empire.db.DBReader;
import org.apache.empire.jsf2.websample.db.SampleDB;
import org.apache.empire.jsf2.websample.web.FacesUtils;
import org.apache.empire.jsf2.websample.web.objects.EmployeeSearch;

@ManagedBean
@ViewScoped
public class EmployeeListPage extends Page implements Serializable {
	private static final long serialVersionUID = 1944555691727940966L;

	private List<EmployeeListItem> employeeList;

	public List<EmployeeListItem> getEmployeeList() {
		return employeeList;
	}

	public static class EmployeeListItem {
		private int employeeId;
		private String name;
		private String gender;
		private Date dateOfBirth;
		private String department;

		public int getEmployeeId() {
			return employeeId;
		}

		public void setEmployeeId(int employeeId) {
			this.employeeId = employeeId;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getGender() {
			return gender;
		}

		public void setGender(String gender) {
			this.gender = gender;
		}

		public Date getDateOfBirth() {
			return dateOfBirth;
		}

		public void setDateOfBirth(Date dateOfBirth) {
			this.dateOfBirth = dateOfBirth;
		}

		public String getDepartment() {
			return department;
		}

		public void setDepartment(String department) {
			this.department = department;
		}
	}

	public EmployeeListPage() {
	}

	@Override
	public void preRenderViewAction() {
		SampleDB sampleDB = FacesUtils.getDatabase();

		DBColumnExpr C_FULL_NAME = sampleDB.T_EMPLOYEES.C_LAST_NAME.append(", ")
				.append(sampleDB.T_EMPLOYEES.C_FIRST_NAME).as("NAME");
		DBColumnExpr C_DEPARTMENT = sampleDB.T_DEPARTMENTS.C_NAME
				.as("DEPARTMENT");
		// lade Liste aus der Datenbank

		SampleDB.Employees EMP = sampleDB.T_EMPLOYEES;
		SampleDB.Departments DEP = sampleDB.T_DEPARTMENTS;

		DBCommand cmd = sampleDB.createCommand();
		cmd.select(EMP.C_EMPLOYEE_ID);
		cmd.select(C_FULL_NAME, EMP.C_GENDER, EMP.C_DATE_OF_BIRTH);
		cmd.select(C_DEPARTMENT);
		cmd.join(DEP.C_DEPARTMENT_ID, EMP.C_DEPARTMENT_ID);

		// Set filter constraints
		EmployeeSearch employeeSearch = (EmployeeSearch) FacesUtils
				.getManagedBean(EmployeeSearch.class);
		if (employeeSearch.getDepartmentId() != null)
			cmd.where(EMP.C_DEPARTMENT_ID.is(employeeSearch.getDepartmentId()));
		if (StringUtils.isValid(employeeSearch.getFirstName()))
			cmd.where(EMP.C_FIRST_NAME.likeUpper(
					employeeSearch.getFirstName() + "%").or(
					EMP.C_FIRST_NAME.is(null)));
		if (StringUtils.isValid(employeeSearch.getLastName()))
			cmd.where(EMP.C_LAST_NAME.likeUpper(employeeSearch.getLastName()
					+ "%"));

		cmd.orderBy(EMP.C_LAST_NAME);
		cmd.orderBy(EMP.C_FIRST_NAME);

		// set DataTable
		DBReader reader = new DBReader();
		try {
			reader.open(cmd, FacesUtils.getConnection());
			employeeList = reader.getBeanList(EmployeeListItem.class);
		} finally {
			reader.close();
		}
	}
}
