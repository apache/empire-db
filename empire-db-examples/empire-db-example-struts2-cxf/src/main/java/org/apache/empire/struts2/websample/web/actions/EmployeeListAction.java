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
package org.apache.empire.struts2.websample.web.actions;

import java.util.List;

import org.apache.empire.commons.Options;
import org.apache.empire.samples.cxf.wssample.common.Employee;
import org.apache.empire.struts2.websample.web.SampleContext;
import org.apache.empire.struts2.websample.web.actiontypes.SampleAction;
import org.apache.empire.struts2.websample.ws.records.EmployeeRecord;

/**
 * EmployeeListAction
 * <p>
 * This class provides form functions for searching and displaying a list of employees. This implementation shows two optional
 * approaches for this. 1. Use a ReaderListActionSupport object to directly display database query results in the JSP.<br>
 * This is most efficient, however it lacks some flexibility.<br>
 * 2. Use a BeanListActionSupport object to obtain a list of JavaBean objects from the query.<br>
 * From the JSP the data of the list will be accessed though their getter and setters.<br>
 * While this is not a efficent as the Reader approach, it is more flexible since post processing of the query result is
 * possbile.
 * </p>
 */
public class EmployeeListAction extends SampleAction
{
    /**
     * Action mappings
     */
    public static final String SEARCH = "search";
    public static final String LIST   = "list";

    // the search info
    public static class SearchInfo
    {
        private Integer employeeId;
        private String  firstName;
        private String  lastName;
        private Integer departmentId;

        public Integer getEmployeeId()
        {
            return employeeId;
        }

        public void setEmployeeId(Integer employeeId)
        {
            this.employeeId = employeeId;
        }

        public Integer getDepartmentId()
        {
            return departmentId;
        }

        public void setDepartmentId(Integer departmentId)
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
    }

    private List<Employee> employeeBeanList = null;
    private Options        departments      = null;

    // ------- Action Construction -------

    public EmployeeListAction()
    {

    }

    // ------- Action Properties -------

    public SearchInfo getSearchInfo()
    {
        return (SearchInfo) getActionBean(SearchInfo.class, true);
    }

    // -------- Employee Bean List --------

    public List<Employee> getEmployees()
    {
        return employeeBeanList;
    }

    // -------- Action Methods --------

    public String doInit()
    {
        // check webservice availability!
        checkWebService();
        return SEARCH;
    }

    public String doReset()
    {
        removeActionBean(SearchInfo.class);
        // check webservice availability!
        checkWebService();
        return SEARCH;
    }

    public String doQuery()
    {
        // Reset list status
        return doList();
    }

    public String doList()
    {
        SearchInfo si = getSearchInfo();
        // check webservice availability!
        if (checkWebService())
        {
            employeeBeanList = getEmployeeServiceClient().searchEmployee(si.getEmployeeId(), si.getFirstName(), si.getLastName(),
                                                           si.getDepartmentId());
            return LIST;
        } else
            return SEARCH;

    }

    public Options getDepartments()
    {
        if (!isServiceAvailable())
            return new Options(); // Webservice is note not available ... so do nothing

        if (departments == null)
            departments = EmployeeRecord.getDepartments(new SampleContext());

        return departments;
    }

}
