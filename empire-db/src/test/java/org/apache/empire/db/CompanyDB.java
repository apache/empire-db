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
package org.apache.empire.db;

import org.apache.empire.commons.Options;
import org.apache.empire.data.DataType;


/**
 * This is the basic database for testing
 *
 */
public class CompanyDB extends DBDatabase
{
    // *Deprecated* private static final long serialVersionUID = 1L;

    /**
     * This class represents the definition of the Departments table.
     */
    public static class Departments extends DBTable
    {
        // *Deprecated* private static final long serialVersionUID = 1L;
        public final DBTableColumn ID;
        public final DBTableColumn NAME;
        public final DBTableColumn HEAD;
        public final DBTableColumn BUSINESS_UNIT;
        public final DBTableColumn UPDATE_TIMESTAMP;

        public Departments(DBDatabase db)
        {
            super("DEPARTMENTS", db);
            // ID
            ID              = addColumn("DEPARTMENT_ID",    DataType.AUTOINC,       0, true, "DEP_ID_SEQUENCE");
            NAME            = addColumn("NAME",             DataType.VARCHAR,      80, true);
            HEAD            = addColumn("HEAD",             DataType.VARCHAR,      80, false);
            BUSINESS_UNIT   = addColumn("BUSINESS_UNIT",    DataType.VARCHAR,       4, true, "ITTK");
            UPDATE_TIMESTAMP= addColumn("UPDATE_TIMESTAMP", DataType.TIMESTAMP,     0, true);
            // Primary Key
            setPrimaryKey(ID);
            setTimestampColumn(UPDATE_TIMESTAMP);
            // Set other Indexes
            addIndex("DEARTMENT_NAME_IDX", true, new DBColumn[] { NAME });
        }
    }

    /**
     * This class represents the definition of the Employees table.
     */
    public static class Employees extends DBTable
    {
        // *Deprecated* private static final long serialVersionUID = 1L;
        public final DBTableColumn ID;
        public final DBTableColumn SALUTATION;
        public final DBTableColumn FIRSTNAME;
        public final DBTableColumn LASTNAME;
        public final DBTableColumn DATE_OF_BIRTH;
        public final DBTableColumn DEPARTMENT_ID;
        public final DBTableColumn GENDER;
        public final DBTableColumn PHONE_NUMBER;
        public final DBTableColumn EMAIL;
        public final DBTableColumn SALARY;
        public final DBTableColumn RETIRED;
        public final DBTableColumn UPDATE_TIMESTAMP;

        public Employees(DBDatabase db)
        {
            super("EMPLOYEES", db);
            // ID
            ID              = addColumn("EMPLOYEE_ID",      DataType.AUTOINC,      0, true, "EMPLOYEE_ID_SEQUENCE");
            SALUTATION      = addColumn("SALUTATION",       DataType.VARCHAR,     20, false);
            FIRSTNAME       = addColumn("FIRSTNAME",        DataType.VARCHAR,     40, true);
            LASTNAME        = addColumn("LASTNAME",         DataType.VARCHAR,     40, true);
            DATE_OF_BIRTH   = addColumn("DATE_OF_BIRTH",    DataType.DATE,         0, false);
            DEPARTMENT_ID   = addColumn("ID",               DataType.INTEGER,      0, true);
            GENDER          = addColumn("GENDER",           DataType.VARCHAR,      1, false);
            PHONE_NUMBER    = addColumn("PHONE_NUMBER",     DataType.VARCHAR,     40, false);
            EMAIL           = addColumn("EMAIL",            DataType.VARCHAR,     80, false);
            SALARY          = addColumn("SALARY",           DataType.DECIMAL,   10.2, false);
            RETIRED         = addColumn("RETIRED",          DataType.BOOL,         0, true, false);
            UPDATE_TIMESTAMP= addColumn("UPDATE_TIMESTAMP", DataType.TIMESTAMP,    0, true);

            // Primary Key
            setPrimaryKey(ID);
            setTimestampColumn(UPDATE_TIMESTAMP);
            // Set other Indexes
            addIndex("EMPLOYEE_NAME_IDX", true, new DBColumn[] { FIRSTNAME, LASTNAME, DATE_OF_BIRTH });

            // Create Options for GENDER column
            Options genders = new Options();
            genders.set("M", "Male");
            genders.set("F", "Female");
            GENDER.setOptions(genders);
        }
    }
    
    /**
     * This class represents the definition of the Departments table.
     */
    public static class Data extends DBTable
    {
        // *Deprecated* private static final long serialVersionUID = 1L;
        public final DBTableColumn ID;
        public final DBTableColumn DATA;
        public final DBTableColumn UPDATE_TIMESTAMP;

        public Data(DBDatabase db)
        {
            super("DATA", db);
            // ID
            ID              = addColumn("DEPARTMENT_ID",    DataType.AUTOINC,       0, true, "DATA_ID_SEQUENCE");
            DATA            = addColumn("NAME",             DataType.BLOB,          0, true);
            UPDATE_TIMESTAMP= addColumn("UPDATE_TIMESTAMP", DataType.TIMESTAMP,     0, true);
            // Primary Key
            setPrimaryKey(ID);
            setTimestampColumn(UPDATE_TIMESTAMP);
        }
    }

    // Declare all Tables and Views here
    public final Departments  DEPARTMENT = new Departments(this);
    public final Employees    EMPLOYEE   = new Employees(this);
    public final Data         DATA       = new Data(this);

    /**
     * Constructor of the CompanyDB data model description
     *
     * Put all foreign key relations here.
     */
    public CompanyDB()
    {
        // Define Foreign-Key Relations
        addRelation( EMPLOYEE.DEPARTMENT_ID.referenceOn( DEPARTMENT.ID ));
    }

}
