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
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.apache.empire.commons.DateUtils;
import org.apache.empire.db.DBCommand;
import org.apache.empire.db.DBContext;
import org.apache.empire.db.list.Bean;
import org.apache.empire.samples.db.SampleDB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is an employee entity bean
 * @author doebele
 *
 */
public class Employee implements Bean<SampleDB>
{
    private static final Logger log = LoggerFactory.getLogger(Employee.class);
    
    private long   id;          // "ID" 
    private String firstname;   // "FIRSTNAME"
    private String lastname;    // "LASTNAME"
    private Date   dateOfBirth; // "DATE_OF_BIRTH"
    private long   departmentId;// "DEPARTMENT_ID"
    private String gender;      // "GENDER"
    private String phoneNumber; // "PHONE_NUMBER"
    private BigDecimal salary;  // "SALARY"
    private boolean retired;    // "RETIRED" 
    
    private Department department;
    private List<Payment> payments;
    
    int rownum;
    
    /**
     * Constructor using all fields from the table EMPLOYEES
     * @param id
     * @param firstname
     * @param lastname
     * @param dateOfBirth
     * @param departmentId
     * @param gender
     * @param phoneNumber
     * @param salary
     * @param retired
    public Employee(int id, String firstname, String lastname, Date dateOfBirth, int departmentId, String gender, String phoneNumber,
                    BigDecimal salary, boolean retired, Timestamp timestamp)
    {
        this.id = id;
        this.firstname = firstname;
        this.lastname = lastname;
        this.dateOfBirth = dateOfBirth;
        this.departmentId = departmentId;
        this.gender = gender;
        this.phoneNumber = phoneNumber;
        this.salary = salary;
        this.retired = retired;
     
        log.info("Employee bean created using fields constructor");
    }
     */

    /**
     * Constructor using fields but without timestamp 
     */
    public Employee(int id, String firstname, String lastname, Date dateOfBirth, int departmentId, String gender, String phoneNumber,
                    BigDecimal salary, boolean retired)
    {
        this.id = id;
        this.firstname = firstname;
        this.lastname = lastname;
        this.dateOfBirth = dateOfBirth;
        this.departmentId = departmentId;
        this.gender = gender;
        this.phoneNumber = phoneNumber;
        this.salary = salary;
        this.retired = retired;

        log.info("Employee bean created using fields constructor without timestamp");
    }

    /**
     * Constructor using primary key fields 
     */
    public Employee(int id)
    {
        this.id = id;
        log.info("Employee bean created using primary key constructor");
    }
    
    /**
     * Standard Constructor 
     */
    public Employee()
    {
        // Standard constructor 
        log.info("Employee bean created using standard constructor");
    }

    public long getId()
    {
        return id;
    }

    public void setId(long id)
    {
        this.id = id;
    }
 
    public String getFirstname()
    {
        return firstname;
    }

    public void setFirstname(String firstname)
    {
        this.firstname = firstname;
    }

    public String getLastname()
    {
        return lastname;
    }

    public void setLastname(String lastname)
    {
        this.lastname = lastname;
    }

    public Date getDateOfBirth()
    {
        return dateOfBirth;
    }

    public void setDateOfBirth(Date dateOfBirth)
    {
        this.dateOfBirth = dateOfBirth;
    }

    public long getDepartmentId()
    {
        return departmentId;
    }

    public void setDepartmentId(long departmentId)
    {
        this.departmentId = departmentId;
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

    public BigDecimal getSalary()
    {
        return salary;
    }

    public void setSalary(BigDecimal salary)
    {
        this.salary = salary;
    }

    public boolean isRetired()
    {
        return retired;
    }

    public void setRetired(boolean retired)
    {
        this.retired = retired;
    }

    public Department getDepartment()
    {
        return department;
    }

    public List<Payment> getPayments()
    {
        return payments;
    }

    @Override
    public String toString()
    {
        StringBuffer buf = new StringBuffer();
        buf.append(rownum);
        buf.append("\t");
        buf.append(id);
        buf.append("\t");
        buf.append(firstname);
        buf.append("\t");
        buf.append(lastname);
        buf.append("\t");
        buf.append(DateUtils.formatDate(dateOfBirth, Locale.US));
        buf.append("\t");
        buf.append(gender);
        buf.append("\t");
        buf.append(salary);
        buf.append("\t");
        buf.append(retired);
        return buf.toString();
    }

    @Override
    public void onBeanLoaded(SampleDB db, DBContext context, int rownum, Object parent)
    {
        this.rownum = rownum;
        
        if (parent instanceof Department)
            department = ((Department)parent); 
        // don't!
        // else department = context.getUtils().queryBean(Department.class, DBRecord.key(this.departmentId));
        
        DBCommand cmd = db.createCommand();
        cmd.where(db.PAYMENTS.EMPLOYEE_ID.is(this.id));
        cmd.orderBy(db.PAYMENTS.YEAR.desc());
        cmd.orderBy(db.PAYMENTS.MONTH.desc());
        payments = context.getUtils().queryBeanList(cmd, Payment.class, this);
    }

}
