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

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.empire.jsf2.websample.db.SampleDB;
import org.apache.empire.jsf2.websample.db.records.EmployeeRecord;
import org.apache.empire.jsf2.websample.web.FacesUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ManagedBean
@ViewScoped
public class EmployeeDetailPage extends Page implements Serializable {
	// Logger
	private static final Logger log = LoggerFactory
			.getLogger(EmployeeDetailPage.class);

	private EmployeeRecord employeeRecord;

	public EmployeeRecord getEmployeeRecord() {
		return employeeRecord;
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 7880544317192692309L;

	public String save() {
		try {
			employeeRecord.update(FacesUtils.getConnection());
		} catch (Exception e) {
			FacesUtils.addErrorMessage(e.getMessage());
			return "";
		}
		return new EmployeeListPage().name();
	}

	public String delete() {
		try {
			employeeRecord.delete(FacesUtils.getConnection());
		} catch (Exception e) {
			FacesUtils.addErrorMessage(e.getMessage());
		}
		return new EmployeeListPage().name();
	}

	public String cancel() {
		return new EmployeeListPage().name();
	}

	@Override
	public void preRenderViewAction() {
		if (employeeRecord == null) {
			employeeRecord = new EmployeeRecord();
			SampleDB sampleDB = FacesUtils.getDatabase();
			String id = FacesUtils.getHttpRequest().getParameter("id");
			if (id != null) {
				try {
					employeeRecord.read(sampleDB.T_EMPLOYEES,
							new String[] { id }, FacesUtils.getConnection());
				} catch (Exception e) {
					FacesUtils.addErrorMessage(e.getMessage());
				}
			} else {
				employeeRecord.create(sampleDB.T_EMPLOYEES);
			}
		}
	}
}
