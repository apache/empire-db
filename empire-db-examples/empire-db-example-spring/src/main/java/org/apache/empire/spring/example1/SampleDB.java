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
package org.apache.empire.spring.example1;

import org.apache.empire.commons.Options;
import org.apache.empire.data.DataType;
import org.apache.empire.data.DataMode;
import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.DBTable;
import org.apache.empire.db.DBTableColumn;

/**
 * <PRE>
 * This file contains the definition of the data model in Java.
 * The SampleDB data model consists of two tables and a foreign key relation.
 * The tables are defined as nested classes here, but you may put the in separate files if you want.
 *
 * PLEASE NOTE THE NAMING CONVENTION:
 * Since all tables, views and columns are declared as "final" constants they are all in upper case.
 * We recommend using a prefix of T_ for tables and C_ for columns in order to keep them together
 * when listed in your IDE's code completion.
 * There is no need to stick to this convention but it makes life just another little bit easier.
 *
 * You may declare other database tables or views in the same way.
 * </PRE>
 */
public class SampleDB extends DBDatabase
{
    private final static long serialVersionUID = 1L;

    /**
     * This class represents the definition of the Departments table.
     */
    public static class Departments extends DBTable
    {

        private final static long serialVersionUID = 1L;

        public final DBTableColumn DEPARTMENT_ID;
        public final DBTableColumn NAME;
        public final DBTableColumn HEAD;
        public final DBTableColumn BUSINESS_UNIT;
        public final DBTableColumn UPDATE_TIMESTAMP;

        public Departments(DBDatabase db)
        {
            super("DEPARTMENTS", db);
            // ID
            DEPARTMENT_ID   = addColumn("DEPARTMENT_ID",    DataType.AUTOINC,       0, true, "DEP_ID_SEQUENCE");
            NAME            = addColumn("NAME",             DataType.VARCHAR,      80, true);
            HEAD            = addColumn("HEAD",             DataType.VARCHAR,      80, false);
            BUSINESS_UNIT   = addColumn("BUSINESS_UNIT",    DataType.VARCHAR,       4, true, "ITTK");
            UPDATE_TIMESTAMP= addColumn("UPDATE_TIMESTAMP", DataType.DATETIME,      0, true);

            // Primary Key
            setPrimaryKey(DEPARTMENT_ID);
            // Set other Indexes
            addIndex("DEARTMENT_NAME_IDX", true, new DBColumn[] { NAME });
            // Set timestamp column for save updates
            setTimestampColumn(UPDATE_TIMESTAMP);
        }
    }

    /**
     * This class represents the definition of the Employees table.
     */
    public static class Employees extends DBTable
    {
        private final static long serialVersionUID = 1L;
      
        public final DBTableColumn EMPLOYEE_ID;
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
            EMPLOYEE_ID     = addColumn("EMPLOYEE_ID",      DataType.AUTOINC,      0, true, "EMPLOYEE_ID_SEQUENCE");
            SALUTATION      = addColumn("SALUTATION",       DataType.VARCHAR,     20, false);
            FIRSTNAME       = addColumn("FIRST_NAME",       DataType.VARCHAR,     40, true);
            LASTNAME        = addColumn("LAST_NAME",        DataType.VARCHAR,     40, true);
            DATE_OF_BIRTH   = addColumn("DATE_OF_BIRTH",    DataType.DATE,         0, false);
            DEPARTMENT_ID   = addColumn("DEPARTMENT_ID",    DataType.INTEGER,      0, true);
            GENDER          = addColumn("GENDER",           DataType.VARCHAR,      1, false);
            PHONE_NUMBER    = addColumn("PHONE_NUMBER",     DataType.VARCHAR,     40, false);
            EMAIL           = addColumn("EMAIL",            DataType.VARCHAR,     80, false);
            SALARY          = addColumn("SALARY",           DataType.DECIMAL,   10.2, false);
            RETIRED         = addColumn("RETIRED",          DataType.BOOL,         0, true, false);
            UPDATE_TIMESTAMP= addColumn("UPDATE_TIMESTAMP", DataType.DATETIME,     0, true);

            // Primary Key
            setPrimaryKey(EMPLOYEE_ID);
            // Set other Indexes
            addIndex("EMPLOYEE_NAME_IDX", true, new DBColumn[] { FIRSTNAME, LASTNAME, DATE_OF_BIRTH });
            // Set timestamp column for save updates
            setTimestampColumn(UPDATE_TIMESTAMP);

            // Create Options for GENDER column
            Options genders = new Options();
            genders.set("M", "Male");
            genders.set("F", "Female");
            GENDER.setOptions(genders);
        }
    }

    // Declare all Tables and Views here
    public final Departments  DEPARTMENTS = new Departments(this);
    public final Employees    EMPLOYEES   = new Employees(this);

    /**
     * Constructor of the SampleDB data model description
     *
     * Put all foreign key relations here.
     */
    public SampleDB()
    {
        // Define Foreign-Key Relations
        addRelation( EMPLOYEES.DEPARTMENT_ID.referenceOn( DEPARTMENTS.DEPARTMENT_ID ));
    }

}
