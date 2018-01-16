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
package org.apache.empire.vuesample.model.db;

import org.apache.empire.rest.EmpireColumn;

public class EmployeeBean {

	private EmpireColumn	employeeId;

	private EmpireColumn	firstName;

	private EmpireColumn	lastName;

	private EmpireColumn	dateOfBirth;

	public EmpireColumn getEmployeeId() {
		return this.employeeId;
	}

	public void setEmployeeId(EmpireColumn employeeId) {
		this.employeeId = employeeId;
	}

	public EmpireColumn getFirstName() {
		return this.firstName;
	}

	public void setFirstName(EmpireColumn firstName) {
		this.firstName = firstName;
	}

	public EmpireColumn getLastName() {
		return this.lastName;
	}

	public void setLastName(EmpireColumn lastName) {
		this.lastName = lastName;
	}

	public EmpireColumn getDateOfBirth() {
		return this.dateOfBirth;
	}

	public void setDateOfBirth(EmpireColumn dateOfBirth) {
		this.dateOfBirth = dateOfBirth;
	}

}
