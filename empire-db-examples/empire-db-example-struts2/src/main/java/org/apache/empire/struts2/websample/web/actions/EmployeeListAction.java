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

import java.util.Date;
import java.util.List;

import org.apache.empire.commons.Options;
import org.apache.empire.commons.StringUtils;
import org.apache.empire.db.DBColumnExpr;
import org.apache.empire.db.DBCommand;
import org.apache.empire.db.DBReader;
import org.apache.empire.struts2.actionsupport.BeanListActionSupport;
import org.apache.empire.struts2.actionsupport.ReaderListActionSupport;
import org.apache.empire.struts2.websample.db.SampleDB;
import org.apache.empire.struts2.websample.web.actiontypes.Action;


@SuppressWarnings("serial")
/**
 * EmployeeListAction
 * <p>
 * This class provides form functions for searching and displaying a list of employees.
 * This implementation shows two optional approaches for this.
 * 1. Use a ReaderListActionSupport object to directly display database query results in the JSP.<br> 
 *    This is most efficient, however it lacks some flexibility.<br>
 * 2. Use a BeanListActionSupport object to obtain a list of JavaBean objects from the query.<br>
 *    From the JSP the data of the list will be accessed though their getter and setters.<br>
 *    While this is not a efficent as the Reader approach, it is more flexible since post processing
 *    of the query result is possbile.
 * </p>
 */
public class EmployeeListAction extends Action
{
    /**
     * Action mappings
     */
    public static final String SEARCH   = "search";
    public static final String LIST     = "list";

    // the search info
    public static class SearchInfo
    {
        private Integer employeeId;
        private String firstName;
        private String lastName;
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

    // the list result
    public static class EmployeeInfo
    {
        private int employeeId;
        private String name;
        private String gender;
        private Date dateOfBirth;
        private String department;
        
        public int getEmployeeId()
        {
            return employeeId;
        }
        public void setEmployeeId(int employeeId)
        {
            this.employeeId = employeeId;
        }
        public String getName()
        {
            return name;
        }
        public void setName(String name)
        {
            this.name = name;
        }
        public String getGender()
        {
            return gender;
        }
        public void setGender(String gender)
        {
            this.gender = gender;
        }
        public Date getDateOfBirth()
        {
            return dateOfBirth;
        }
        public void setDateOfBirth(Date dateOfBirth)
        {
            this.dateOfBirth = dateOfBirth;
        }
        public String getDepartment()
        {
            return department;
        }
        public void setDepartment(String department)
        {
            this.department = department;
        }
    }
    
    private BeanListActionSupport<EmployeeInfo> employeeBeanList = null;
    private ReaderListActionSupport employeeReader = null;
    private Options departments;
    
    public final DBColumnExpr C_FULL_NAME;  // Expression for Lastname + Firstname
    public final DBColumnExpr C_DEPARTMENT;

    // ------- Action Construction -------
    
    public EmployeeListAction()
    {
        SampleDB db = getDatabase();
        C_FULL_NAME = db.T_EMPLOYEES.C_LASTNAME.append(", ").append(db.T_EMPLOYEES.C_FIRSTNAME).as("NAME");
        C_DEPARTMENT = db.T_DEPARTMENTS.C_NAME.as("DEPARTMENT");
        
        // Set Title (optional)
        C_FULL_NAME .setTitle("!label.name");
        C_DEPARTMENT.setTitle("!label.department");
        
        employeeBeanList = new BeanListActionSupport<EmployeeInfo>(this, EmployeeInfo.class, "employees");
        employeeReader = new ReaderListActionSupport(this, "reader");
    }

    // ------- Action Properties -------

    public SearchInfo getSearchInfo()
    {
        return (SearchInfo)getActionBean(SearchInfo.class, true);
    }
    
    public Options getDepartments()
    {
        if (departments==null)
        {
            SampleDB db = getDatabase();
            DBCommand cmd = db.createCommand();
            cmd.select(db.T_DEPARTMENTS.C_DEPARTMENT_ID);
            cmd.select(db.T_DEPARTMENTS.C_NAME);
            cmd.orderBy(db.T_DEPARTMENTS.C_NAME.asc());
            departments = db.queryOptionList(cmd.getSelect(), getConnection());
        }
        return departments;
    }
    
    // -------- Employee Bean List --------

    public List<EmployeeInfo> getEmployees()
    {
        return employeeBeanList.getList();
    }

    // -------- Employee Reader --------

    public  DBReader getReader()
    {   // read only once!
        return employeeReader.getReader();
    }

    // -------- Action Methods --------
    
    public String doInit()
    {
        return SEARCH;
    }

    public String doReset()
    {
        removeActionBean(SearchInfo.class);
        return SEARCH;
    }

    public String doQuery()
    {
        // Reset list status
        return doList();
    }

    public String doList()
    {
        SampleDB db = getDatabase();
        SampleDB.Employees EMP = db.T_EMPLOYEES;
        SampleDB.Departments DEP = db.T_DEPARTMENTS;
        
        DBCommand cmd = db.createCommand();
        cmd.select(EMP.C_EMPLOYEE_ID);
        cmd.select(C_FULL_NAME, EMP.C_GENDER, EMP.C_DATE_OF_BIRTH);
        cmd.select(C_DEPARTMENT);
        cmd.join  (DEP.C_DEPARTMENT_ID, EMP.C_DEPARTMENT_ID);

        // Set filter constraints
        SearchInfo si = getSearchInfo();
        if (si.getDepartmentId()!=null)
            cmd.where(EMP.C_DEPARTMENT_ID.is(si.getDepartmentId()));
        if (StringUtils.isValid( si.getFirstName()) )
            cmd.where(EMP.C_FIRSTNAME.likeUpper( si.getFirstName()+"%" )
                  .or(EMP.C_FIRSTNAME.is(null)));
        if (StringUtils.isValid( si.getLastName()) )
            cmd.where(EMP.C_LASTNAME.likeUpper( si.getLastName()+"%" ));
        
        cmd.orderBy(EMP.C_LASTNAME);
        cmd.orderBy(EMP.C_FIRSTNAME);
        
        // Init BeanList
        if (!employeeBeanList.initBeanList(cmd))
        {
            setActionError(employeeBeanList);
            return LIST;
        }
        
        // Init Reader
        employeeReader.initReader(cmd);
        
        return LIST;
    }

}
