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
package org.apache.empire.spring.example2;

public class Employee {

	public static enum Gender {

		M("Male"), F("Female");

		private String label;

		private Gender(String label) {
			this.label = label;
		}

		@Override
		public String toString() {
			return this.label;
		}
	}

	private Integer employeeId;
	private String firstName;
	private String lastName;
	private Gender gender;
	private String phoneNumber;

	private Department department;

	public Integer getEmployeeId() {
		return employeeId;
	}

	public void setEmployeeId(Integer employeeId) {
		this.employeeId = employeeId;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public Gender getGender() {
		return gender;
	}

	public void setGender(Gender gender) {
		this.gender = gender;
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}

	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	public Department getDepartment() {
		return department;
	}

	public void setDepartment(Department department) {
		this.department = department;
	}

	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append(employeeId);
		buf.append("\t");
		buf.append(firstName);
		buf.append(" ");
		buf.append(lastName);
		buf.append("\t");
		buf.append(gender);
		
		if (department != null){
			buf.append("\t");
			buf.append(department.getName());	
			buf.append("\t");
			buf.append(department.getBusinessUnit());	
		}
		return buf.toString();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}

		if (obj.getClass().equals(this.getClass())) {
			Employee other = (Employee) obj;
			if (other.employeeId == null || this.employeeId == null) {
				return false;
			}
			return this.employeeId.equals(other.employeeId);
		}

		return super.equals(obj);
	}

	@Override
	public int hashCode() {
		return 1;
	}

}
