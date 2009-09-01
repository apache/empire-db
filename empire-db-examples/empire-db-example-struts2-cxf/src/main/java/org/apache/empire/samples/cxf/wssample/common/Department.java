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

import org.apache.empire.db.DBRecord;


// Holds all the data represented by a department-record from the database
public class Department {

	private long departmentId = DBRecord.REC_NEW;
	private String name = "";
	private String head = "";
	private String businessUnit = "";
	private Date updateTimestamp = null;

	public void setDepartmentId(long id) {
		departmentId = id;
	}

	public long getDepartmentId() {
		return departmentId;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setHead(String head) {
		this.head = head;
	}

	public String getHead() {
		return head;
	}

	public void setBusinessUnit(String businessUnit) {
		this.businessUnit = businessUnit;
	}

	public String getBusinessUnit() {
		return businessUnit;
	}

	public void setUpdateTimestamp(Date d) {
		this.updateTimestamp = d;
	}

	public Date getUpdateTimestamp() {
		return updateTimestamp;
	}
	
	@Override
	public String toString()
	{
		return name;
	}

}
