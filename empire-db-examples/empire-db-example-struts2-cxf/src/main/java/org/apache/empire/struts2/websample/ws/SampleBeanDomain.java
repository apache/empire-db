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
package org.apache.empire.struts2.websample.ws;

import org.apache.empire.commons.Options;
import org.apache.empire.data.DataType;
import org.apache.empire.data.bean.BeanDomain;
import org.apache.empire.data.bean.BeanProperty;
import org.apache.empire.struts2.websample.web.SampleApplication;


public class SampleBeanDomain extends BeanDomain
{
    // Static Access
    public static SampleBeanDomain getInstance()
    {
        return SampleApplication.getInstance().getBeanDomain();
    }
    
    /**
     * Table definition for Departments
     */
    public static class Departments extends SampleBeanClass
    {
        public final BeanProperty C_DEPARTMENT_ID;
        public final BeanProperty C_NAME;
        public final BeanProperty C_HEAD;
        public final BeanProperty C_BUSINESS_UNIT;
        public final BeanProperty C_UPDATE_TIMESTAMP;

        public Departments(BeanDomain dom)
        {
            super("DEPARTMENTS", dom);
            // ID
            C_DEPARTMENT_ID   = addProp("departmentId",    DataType.AUTOINC,       0, true);
            C_NAME            = addProp("name",             DataType.TEXT,         80, true);
            C_HEAD            = addProp("head",             DataType.TEXT,         80, false);
            C_BUSINESS_UNIT   = addProp("businessUnit",    DataType.TEXT,          4, true);
            C_UPDATE_TIMESTAMP= addProp("updateTimestamp", DataType.DATETIME,      0, true);
        
            // Primary Key
            setKeyColumn(C_DEPARTMENT_ID);
            // Set other Indexes
            //addIndex("DEARTMENT_NAME_IDX", true, new DBColumn[] { C_NAME });
            // Set timestamp column for save updates
            //setTimestampColumn(C_UPDATE_TIMESTAMP);
        }  
    }   

    /**
     * Table definition for Persons
     */
    public static class Employees extends SampleBeanClass
    {
        public final BeanProperty C_EMPLOYEE_ID;
        public final BeanProperty C_SALUTATION;
        public final BeanProperty C_FIRSTNAME;
        public final BeanProperty C_LASTNAME;
        public final BeanProperty C_DATE_OF_BIRTH;
        public final BeanProperty C_DEPARTMENT_ID;
        public final BeanProperty C_GENDER;
        public final BeanProperty C_PHONE_NUMBER;
        public final BeanProperty C_EMAIL;
        public final BeanProperty C_RETIRED;
        public final BeanProperty C_UPDATE_TIMESTAMP;

        // Konstruktor f�r Tabelle
        public Employees(BeanDomain dom)
        {
            super("EMPLOYEES", dom);
            // ID
            C_EMPLOYEE_ID     = addProp("employeeId",      DataType.AUTOINC,      0, true);
            C_SALUTATION      = addProp("salutation",       DataType.TEXT,        20, false);
            C_FIRSTNAME       = addProp("firstname",        DataType.TEXT,        40, true);
            C_LASTNAME        = addProp("lastname",         DataType.TEXT,        40, true);
            C_DATE_OF_BIRTH   = addProp("dateOfBirth",    DataType.DATE,         0, false);
            C_DEPARTMENT_ID   = addProp("departmentId",    DataType.INTEGER,      0, true,"select");
            C_GENDER          = addProp("gender",           DataType.TEXT,         1, false,"select");
            C_PHONE_NUMBER    = addProp("phoneNumber",     DataType.TEXT,        40, false,"phone");
            C_EMAIL           = addProp("email",            DataType.TEXT,        80, false);
            C_RETIRED         = addProp("retired",          DataType.BOOL,         0, true);
            C_UPDATE_TIMESTAMP= addProp("updateTimestamp", DataType.DATETIME,     0, true,"text",true);
        
            // Primary Key
            setKeyColumn(C_EMPLOYEE_ID);
            
            Options genders = new Options();
            genders.set("M", "!option.employee.gender.male");
            genders.set("F", "!option.employee.gender.female");
            C_GENDER.setOptions(genders);
        }    
    }   

    // Tabellen
    public final Departments  T_DEPARTMENTS = new Departments(this);
    public final Employees    T_EMPLOYEES   = new Employees(this);
    
    public SampleBeanDomain()
    {
    	super("");
    }
    
}
