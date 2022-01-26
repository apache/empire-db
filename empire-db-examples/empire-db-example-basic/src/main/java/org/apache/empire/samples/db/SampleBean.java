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
package org.apache.empire.samples.db;

import java.math.BigDecimal;

/**
 * The SampleBean class is used to demonstrate JavaBean support for SQL-Queries.
 * The SampleBean is used in the SampleApp's queryRecords function.
 */
public class SampleBean
{
    private int    id;
    private String fullName;
    private String gender;
    private String phoneNumber;
    private String department;
    private String businessUnit;
    private BigDecimal paymentsLastYear;

    /*
     * Uncomment this if you want to use constructor instead of setters
     * Number of arguments and data types must match query!
     *
    public SampleBean(int employeeId, String fullName, String gender, String phoneNumber, String department, String businessUnit, BigDecimal paymentsLastYear)
    {
        this.employeeId = employeeId;
        this.fullName = fullName;
        this.gender = gender;
        this.phoneNumber = phoneNumber;
        this.department = department;
        this.businessUnit = businessUnit;
        this.paymentsLastYear = paymentsLastYear;
    }
    */

    public int getId()
    {
        return id;
    }

    public void setId(int id)
    {
        this.id = id;
    }
    
    public String getFullName()
    {
        return fullName;
    }

    public void setFullName(String fullName)
    {
        this.fullName = fullName;
    }

    public String getGender()
    {
        return gender;
    }

    public void setGender(String gender)
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

    public String getDepartment()
    {
        return department;
    }

    public void setDepartment(String department)
    {
        this.department = department;
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
        buf.append(id);
        buf.append("\t");
        buf.append(fullName);
        buf.append("\t");
        buf.append(gender);
        buf.append("\t");
        buf.append(department);
        buf.append("\t");
        buf.append(businessUnit);
        buf.append("\t");
        buf.append(paymentsLastYear);
        return buf.toString();
    }
    
}
