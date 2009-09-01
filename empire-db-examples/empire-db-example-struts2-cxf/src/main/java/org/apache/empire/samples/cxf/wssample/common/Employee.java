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

package org.apache.empire.samples.cxf.wssample.common;

import java.util.Date;

//Holds all the data represented by a employee-record from the database
public class Employee {

	private long employeeId = -1;
	private String salutation = "";
	private String firstname = "";
	private String lastname = "";
	private Date dateOfBirth = null;
	private long departmentId = -1;
	private String gender = "";
	private String phoneNumber = "";
	private String email = "";
	private double salary = -1;
	private boolean retired = false;
	private Date updateTimestamp = null;
	private boolean isNew = false;

	public void set(Employee e) {
		employeeId = e.getDepartmentId();
		salutation = e.getSalutation();
		firstname = e.getFirstname();
		lastname = e.getLastname();
		dateOfBirth = e.getDateOfBirth();
		departmentId = e.getDepartmentId();
		gender = e.getGender();
		phoneNumber = e.getPhoneNumber();
		email = e.getEmail();
		salary = e.getSalary();
		retired = e.isRetired();
		updateTimestamp = e.getUpdateTimestamp();
		isNew = e.isNew();
	}

	public void setEmployeeId(long id) {
		employeeId = id;
	}

	public long getEmployeeId() {
		return employeeId;
	}

	public void setSalutation(String salutation) {
		this.salutation = salutation;
	}

	public String getSalutation() {
		return salutation;
	}

	public void setFirstname(String firstname) {
		this.firstname = firstname;
	}

	public String getFirstname() {
		return firstname;
	}

	public void setLastname(String lastname) {
		this.lastname = lastname;
	}

	public String getLastname() {
		return lastname;
	}

	public void setDateOfBirth(Date dateOfBirth) {
		this.dateOfBirth = dateOfBirth;
	}

	public Date getDateOfBirth() {
		return dateOfBirth;
	}

	public void setDepartmentId(long departmentId) {
		this.departmentId = departmentId;
	}

	public long getDepartmentId() {
		return departmentId;
	}

	public void setGender(String gender) {
		this.gender = gender;
	}

	public String getGender() {
		return gender;
	}

	public void setPhoneNumber(String phone) {
		this.phoneNumber = phone;
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getEmail() {
		return email;
	}

	public void setRetired(boolean retired) {
		this.retired = retired;
	}

	public boolean isRetired() {
		return retired;
	}

	public void setUpdateTimestamp(Date d) {
		this.updateTimestamp = d;
	}

	public Date getUpdateTimestamp() {
		return updateTimestamp;
	}

	public void setSalary(double salary) {
		this.salary = salary;
	}

	public double getSalary() {
		return salary;
	}

	@Override
	public String toString() {
		String str = "";
		str += getEmployeeId() + ": ";
		str += getSalutation() + " ";
		str += getFirstname() + " ";
		str += getLastname();

		return str;
	}

	public void setNew(boolean isNew) {
		this.isNew = isNew;
	}

	public boolean isNew() {
		return isNew;
	}

}