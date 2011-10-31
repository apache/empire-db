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
package org.apache.empire.jsf2.websample.db;

import org.apache.empire.commons.Options;
import org.apache.empire.data.DataMode;
import org.apache.empire.data.DataType;
import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.DBTableColumn;


public class SampleDB extends DBDatabase
{
    private final static long serialVersionUID = 1L;
  
    /**
     * This class represents the definition of the Departments table.
     */
    public static class Departments extends SampleTable
    {
		private static final long serialVersionUID = -2993431379391158533L;

		public final DBTableColumn C_DEPARTMENT_ID;
        public final DBTableColumn C_NAME;
        public final DBTableColumn C_HEAD;
        public final DBTableColumn C_BUSINESS_UNIT;
        public final DBTableColumn C_UPDATE_TIMESTAMP;

        public Departments(DBDatabase db)
        {
            super("DEPARTMENTS", db);
            // ID
            C_DEPARTMENT_ID   = addColumn("DEPARTMENT_ID",    DataType.AUTOINC,       0, DataMode.NotNull, "DEP_ID_SEQUENCE");
            C_NAME            = addColumn("NAME",             DataType.TEXT,         80, DataMode.NotNull);
            C_HEAD            = addColumn("HEAD",             DataType.TEXT,         80, DataMode.Nullable);
            C_BUSINESS_UNIT   = addColumn("BUSINESS_UNIT",    DataType.TEXT,          4, DataMode.NotNull, "ITTK");
            C_UPDATE_TIMESTAMP= addColumn("UPDATE_TIMESTAMP", DataType.DATETIME,      0, DataMode.NotNull);
        
            // Primary Key
            setPrimaryKey(C_DEPARTMENT_ID);
            // Set other Indexes
            addIndex("DEARTMENT_NAME_IDX", true, new DBColumn[] { C_NAME });
            // Set timestamp column for save updates
            setTimestampColumn(C_UPDATE_TIMESTAMP);

        }    
    }   

    /**
     * This class represents the definition of the Employees table.
     */
    public static class Employees extends SampleTable
    {
		private static final long serialVersionUID = 2175667816137804685L;

		public final DBTableColumn C_EMPLOYEE_ID;
        public final DBTableColumn C_SALUTATION;
        public final DBTableColumn C_FIRST_NAME;
        public final DBTableColumn C_LAST_NAME;
        public final DBTableColumn C_DATE_OF_BIRTH;
        public final DBTableColumn C_DEPARTMENT_ID;
        public final DBTableColumn C_GENDER;
        public final DBTableColumn C_PHONE_NUMBER;
        public final DBTableColumn C_EMAIL;
        public final DBTableColumn C_RETIRED;
        public final DBTableColumn C_UPDATE_TIMESTAMP;

        public Employees(DBDatabase db)
        {
            super("EMPLOYEES", db);
            // ID
            C_EMPLOYEE_ID     = addColumn("EMPLOYEE_ID",      DataType.AUTOINC,      0, DataMode.NotNull, "EMPLOYEE_ID_SEQUENCE");
            C_SALUTATION      = addColumn("SALUTATION",       DataType.TEXT,        5, DataMode.Nullable);
            C_FIRST_NAME       = addColumn("FIRST_NAME",       DataType.TEXT,        40, DataMode.NotNull);
            C_LAST_NAME        = addColumn("LAST_NAME",        DataType.TEXT,        40, DataMode.NotNull);
            C_DATE_OF_BIRTH   = addColumn("DATE_OF_BIRTH",    DataType.DATE,         0, DataMode.Nullable);
            C_DEPARTMENT_ID   = addColumn("DEPARTMENT_ID",    DataType.INTEGER,      0, DataMode.NotNull);
            C_GENDER          = addColumn("GENDER",           DataType.TEXT,         1, DataMode.Nullable);
            C_PHONE_NUMBER    = addColumn("PHONE",     		  DataType.TEXT,        40, DataMode.Nullable);
            C_EMAIL           = addColumn("EMAIL",            DataType.TEXT,        80, DataMode.Nullable);
            C_RETIRED         = addColumn("RETIRED",          DataType.BOOL,         0, DataMode.NotNull, false);
            C_UPDATE_TIMESTAMP= addColumn("UPDATE_TIMESTAMP", DataType.DATETIME,     0, DataMode.NotNull);
        
            // Primary Key
            setPrimaryKey(C_EMPLOYEE_ID);
            // Set other Indexes
            addIndex("PERSON_NAME_IDX", true, new DBColumn[] { C_FIRST_NAME, C_LAST_NAME, C_DATE_OF_BIRTH });
            // Set timestamp column for save updates
            setTimestampColumn(C_UPDATE_TIMESTAMP);
            
            // Create Options for GENDER column
            Options genders = new Options();
            genders.set("M", "option.employee.gender.male");
            genders.set("F", "option.employee.gender.female");
            C_GENDER.setOptions(genders);
            C_GENDER.setControlType("select");
            
            // Set special control types
            C_DEPARTMENT_ID.setControlType("select");
            C_PHONE_NUMBER .setControlType("phone");
            
        }    
    }   

    // Declare all Tables
    public final Departments  T_DEPARTMENTS = new Departments(this);
    public final Employees    T_EMPLOYEES   = new Employees(this);
    
    /**
     * Constructor SampleDB
     */
    public SampleDB()
    {
        // Define Foreign-Key Relations
        addRelation( T_EMPLOYEES.C_DEPARTMENT_ID.referenceOn( T_DEPARTMENTS.C_DEPARTMENT_ID ));
    }
    
}
