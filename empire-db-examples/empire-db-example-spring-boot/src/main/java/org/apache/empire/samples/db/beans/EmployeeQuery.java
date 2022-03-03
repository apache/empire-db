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
package org.apache.empire.samples.db.beans;

import java.math.BigDecimal;

import org.apache.empire.samples.springboot.SampleDB.Gender;

/**
 * The SampleBean class is used to demonstrate JavaBean support for SQL-Queries.
 * The SampleBean is used in the SampleApp's queryRecords function.
 */
public class EmployeeQuery
{
    private int    employeeId;
    private String employeeName;
    private Gender gender;
    private String phoneNumber;
    private BigDecimal salary;
    private String departmentName;
    private String businessUnit;
    private BigDecimal paymentsLastYear;

    /*
     * Uncomment this if you want to use constructor instead of setters
     * Number of arguments and data types must match query!
     *
    public EmployeeQuery(int employeeId, String employeeName, Gender gender, String phoneNumber, BigDecimal salary
                       , String departmentName, String businessUnit, BigDecimal paymentsLastYear)
    {
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.gender = gender;
        this.phoneNumber = phoneNumber;
        this.salary = salary;
        this.departmentName = departmentName;
        this.businessUnit = businessUnit;
        this.paymentsLastYear = paymentsLastYear;
    }
     */

    public int getEmployeeId()
    {
        return employeeId;
    }

    public void setEmployeeId(int employeeId)
    {
        this.employeeId = employeeId;
    }
    
    public String getEmployeeName()
    {
        return employeeName;
    }

    public void setEmployeeName(String employeeName)
    {
        this.employeeName = employeeName;
    }

    public Gender getGender()
    {
        return gender;
    }

    public void setGender(Gender gender)
    {
        this.gender = gender;
    }

    public String getPhoneNumber()
    {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber)
    {
        this.phoneNumber = phoneNumber;
    }

    public BigDecimal getSalary()
    {
        return salary;
    }

    public void setSalary(BigDecimal salary)
    {
        this.salary = salary;
    }
    
    public String getDepartmentName()
    {
        return departmentName;
    }

    public void setDepartmentName(String departmentName)
    {
        this.departmentName = departmentName;
    }

    public String getBusinessUnit()
    {
        return businessUnit;
    }

    public void setBusinessUnit(String businessUnit)
    {
        this.businessUnit = businessUnit;
    }

    public BigDecimal getPaymentsLastYear()
    {
        return paymentsLastYear;
    }

    public void setPaymentsLastYear(BigDecimal paymentsLastYear)
    {
        this.paymentsLastYear = paymentsLastYear;
    }

    @Override
    public String toString()
    {
        StringBuffer buf = new StringBuffer();
        buf.append(employeeId);
        buf.append("\t");
        buf.append(employeeName);
        buf.append("\t");
        buf.append(gender);
        buf.append("\t");
        buf.append(departmentName);
        buf.append("\t");
        buf.append(businessUnit);
        buf.append("\t");
        buf.append(salary);
        buf.append("\t");
        buf.append(paymentsLastYear);
        return buf.toString();
    }
    
}
