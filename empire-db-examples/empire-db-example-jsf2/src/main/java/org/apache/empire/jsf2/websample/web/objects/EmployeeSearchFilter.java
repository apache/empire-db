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
package org.apache.empire.jsf2.websample.web.objects;

import java.io.Serializable;

import javax.faces.context.FacesContext;

import org.apache.empire.jsf2.app.FacesUtils;

public class EmployeeSearchFilter implements Serializable
{
    private static final long serialVersionUID = 1L;

    private String            employeeId;
    private String            firstName;
    private String            lastName;
    private String            departmentId;
    private String            gender;

    public EmployeeSearchFilter()
    {
        resetFilter();
    }

    public void resetFilter()
    {
        employeeId = "";
        firstName = "";
        lastName = "";
        departmentId = "";
        gender = "";

        /**
         * Make sure submitted values are discarded.
         * Works only, if the "immediate" attribute on the h:commandLink tag is set to true 
         */
        FacesUtils.setClearSubmittedValues(FacesContext.getCurrentInstance(), true);
    }

    public String getEmployeeId()
    {
        return employeeId;
    }

    public void setEmployeeId(String employeeId)
    {
        this.employeeId = employeeId;
    }

    public String getDepartmentId()
    {
        return departmentId;
    }

    public void setDepartmentId(String departmentId)
    {
        this.departmentId = departmentId;
    }

    public String getFirstName()
    {
        return firstName;
    }

    public void setFirstName(String firstName)
    {
        this.firstName = firstName;
    }

    public String getLastName()
    {
        return lastName;
    }

    public void setLastName(String lastName)
    {
        this.lastName = lastName;
    }

	public String getGender() {
		return gender;
	}

	public void setGender(String gender) {
		this.gender = gender;
	}
}
