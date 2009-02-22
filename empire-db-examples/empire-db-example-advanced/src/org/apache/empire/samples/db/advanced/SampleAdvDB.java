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
package org.apache.empire.samples.db.advanced;

import org.apache.empire.commons.Options;
import org.apache.empire.data.DataType;
import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBColumnExpr;
import org.apache.empire.db.DBCommand;
import org.apache.empire.db.DBCommandExpr;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.DBTable;
import org.apache.empire.db.DBTableColumn;
import org.apache.empire.db.DBView;

/**
 * <PRE>
 * This file contains the definition of the data model in Java.
 * The SampleDB data model consists of two tables and a foreign key relation.
 * The tables are defined as nested classes here, but you may put the in separate files if you want.
 *
 * PLEASE NOTE THE NAMING CONVENTION:
 * Since all tables, views and columns are declared as "final" constants they are all in upper case.
 * We recommend using a prefix of T_ for tables and C_ for columns in order to keep them togehter
 * when listed in your IDE's code completition.
 * There is no need to stick to this convention but it makes life just another little bit easier.
 *
 * You may declare other database tables or views in the same way.
 * </PRE>
 */
public class SampleAdvDB extends DBDatabase
{
    /**
     * This class represents the definition of the Departments table.
     */
    public static class Departments extends DBTable
    {
        public final DBTableColumn C_DEPARTMENT_ID;
        public final DBTableColumn C_NAME;
        public final DBTableColumn C_HEAD;
        public final DBTableColumn C_BUSINESS_UNIT;
        public final DBTableColumn C_UPDATE_TIMESTAMP;

        public Departments(DBDatabase db)
        {
            super("DEPARTMENTS", db);
            // ID
            C_DEPARTMENT_ID   = addColumn("DEPARTMENT_ID",    DataType.AUTOINC,       0, true, "DEP_ID_SEQUENCE");
            C_NAME            = addColumn("NAME",             DataType.TEXT,         80, true);
            C_HEAD            = addColumn("HEAD",             DataType.TEXT,         80, false);
            C_BUSINESS_UNIT   = addColumn("BUSINESS_UNIT",    DataType.TEXT,          4, true, "ITTK");
            C_UPDATE_TIMESTAMP= addColumn("UPDATE_TIMESTAMP", DataType.DATETIME,      0, true);

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
    public static class Employees extends DBTable
    {
        public final DBTableColumn C_EMPLOYEE_ID;
        public final DBTableColumn C_SALUTATION;
        public final DBTableColumn C_FIRSTNAME;
        public final DBTableColumn C_LASTNAME;
        public final DBTableColumn C_DATE_OF_BIRTH;
        public final DBTableColumn C_GENDER;
        public final DBTableColumn C_PHONE_NUMBER;
        public final DBTableColumn C_EMAIL;
        public final DBTableColumn C_RETIRED;
        public final DBTableColumn C_CHECKSUM;
        public final DBTableColumn C_UPDATE_TIMESTAMP;

        // Useful column expressions
        public final DBColumnExpr  C_FULLNAME;
        
        public Employees(DBDatabase db)
        {
            super("EMPLOYEES", db);
            // ID
            C_EMPLOYEE_ID     = addColumn("EMPLOYEE_ID",      DataType.AUTOINC,      0, true, "EMPLOYEE_ID_SEQUENCE");
            C_SALUTATION      = addColumn("SALUTATION",       DataType.TEXT,        20, false);
            C_FIRSTNAME       = addColumn("FIRSTNAME",        DataType.TEXT,        40, true);
            C_LASTNAME        = addColumn("LASTNAME",         DataType.TEXT,        40, true);
            C_DATE_OF_BIRTH   = addColumn("DATE_OF_BIRTH",    DataType.DATE,         0, false);
            C_GENDER          = addColumn("GENDER",           DataType.TEXT,         1, false);
            C_PHONE_NUMBER    = addColumn("PHONE_NUMBER",     DataType.TEXT,        40, false);
            C_EMAIL           = addColumn("EMAIL",            DataType.TEXT,        80, false);
            C_RETIRED         = addColumn("RETIRED",          DataType.BOOL,         0, true, false);
            C_CHECKSUM        = addColumn("CHECKSUM",         DataType.INTEGER,      0, false);
            C_UPDATE_TIMESTAMP= addColumn("UPDATE_TIMESTAMP", DataType.DATETIME,     0, true);

            // Primary Key
            setPrimaryKey(C_EMPLOYEE_ID);
            // Set other Indexes
            addIndex("EMPLOYEE_NAME_IDX", true, new DBColumn[] { C_FIRSTNAME, C_LASTNAME, C_DATE_OF_BIRTH });
            // Set timestamp column for save updates
            setTimestampColumn(C_UPDATE_TIMESTAMP);

            // Create Options for GENDER column
            Options genders = new Options();
            genders.set("M", "Male");
            genders.set("F", "Female");
            C_GENDER.setOptions(genders);
            
            // Define Column Expressions
            C_FULLNAME = C_LASTNAME.append(", ").append(C_FIRSTNAME).as("FULL_NAME");
            
        }
    }
    
    /**
     * This class represents the definition of the Departments table.
     */
    public static class EmployeeDepartmentHistory extends DBTable
    {
        public final DBTableColumn C_EMPLOYEE_ID;
        public final DBTableColumn C_DEPARTMENT_ID;
        public final DBTableColumn C_DATE_FROM;

        public EmployeeDepartmentHistory(DBDatabase db)
        {
            super("EMPLOYEE_DEPARTMENT_HIST", db);
            // ID
            C_EMPLOYEE_ID   = addColumn("EMPLOYEE_ID",      DataType.INTEGER,       0, true);
            C_DEPARTMENT_ID = addColumn("DEPARTMENT_ID",    DataType.INTEGER,       0, true);
            C_DATE_FROM     = addColumn("DATE_FROM",        DataType.DATE,          0, true);

            // Primary Key
            setPrimaryKey(C_EMPLOYEE_ID, C_DATE_FROM);
        }
    }

    /**
     * This class represents the definition of the EmployeeDepSinceView table.
     */
    public static class EmployeeDepSinceView extends DBView
    {
        public final DBViewColumn C_EMPLOYEE_ID;
        public final DBViewColumn C_MAX_DATE_FROM;
        
        public EmployeeDepSinceView(DBDatabase db, EmployeeDepartmentHistory T_EDH)
        {
            super("EMPLOYEE_DEP_SINCE_VIEW", db);
            // ID
            C_EMPLOYEE_ID     = addColumn(T_EDH.C_EMPLOYEE_ID);
            C_MAX_DATE_FROM   = addColumn("MAX_DATE_FROM", T_EDH.C_DATE_FROM); 

            // set Key-column (if any)
            setKeyColumn(C_EMPLOYEE_ID);
        }

        @Override
        public DBCommandExpr createCommand()
        {
            /* Sample DDL for this View:
            CREATE VIEW EMPLOYEE_DEP_SINCE_VIEW (EMPLOYEE_ID, MAX_DATE_FROM)
            AS (SELECT t3.EMPLOYEE_ID, max(t3.DATE_FROM)
                FROM EMPLOYEE_DEPARTMENT_HIST t3
                GROUP BY t3.EMPLOYEE_ID);
            */
            
            SampleAdvDB db = (SampleAdvDB)getDatabase();
            SampleAdvDB.EmployeeDepartmentHistory T_EDH = db.T_EMP_DEP_HIST;
            
            // Define the sub query
            DBCommand cmd = db.createCommand();
            cmd.select (T_EDH.C_EMPLOYEE_ID, T_EDH.C_DATE_FROM.max());
            cmd.groupBy(T_EDH.C_EMPLOYEE_ID);
            return cmd;
        }
    }

    /**
     * This class represents the definition of the EmployeeInfoView table.
     */
    public static class EmployeeInfoView extends DBView
    {
        public final DBViewColumn C_EMPLOYEE_ID;
        public final DBViewColumn C_CURRENT_DEP_ID;
        public final DBViewColumn C_NAME_AND_DEP;

        public EmployeeInfoView(DBDatabase db, Employees T_EMP, Departments T_DEP)
        {
            super("EMPLOYEE_INFO_VIEW", db);
            // ID
            C_EMPLOYEE_ID     = addColumn(T_EMP.C_EMPLOYEE_ID);
            C_CURRENT_DEP_ID  = addColumn("CURRENT_DEP_ID", T_DEP.C_DEPARTMENT_ID); 
            C_NAME_AND_DEP    = addColumn("NAME_AND_DEP", DataType.TEXT);

            // set Key-column (if any)
            setKeyColumn(C_EMPLOYEE_ID);
        }

        @Override
        public DBCommandExpr createCommand()
        {
            /* Sample DDL for this View:
            CREATE VIEW EMPLOYEE_INFO_VIEW (EMPLOYEE_ID, CURRENT_DEP_ID, NAME_AND_DEP)
            AS (SELECT t2.EMPLOYEE_ID, t1.DEPARTMENT_ID, t2.LASTNAME + ', ' + coalesce(t2.FIRSTNAME, '') + ' (' + t1.NAME + ')'
                FROM EMPLOYEE_DEPARTMENT_HIST t3 
                INNER JOIN EMPLOYEE_DEP_SINCE_VIEW v1 ON v1.EMPLOYEE_ID = t3.EMPLOYEE_ID AND t3.DATE_FROM=v1.MAX_DATE_FROM
                INNER JOIN EMPLOYEES t2 ON t2.EMPLOYEE_ID = t3.EMPLOYEE_ID
                INNER JOIN DEPARTMENTS t1 ON t1.DEPARTMENT_ID = t3.DEPARTMENT_ID);
            */
            
            SampleAdvDB db = (SampleAdvDB)getDatabase();
            SampleAdvDB.Employees   T_EMP = db.T_EMPLOYEES;
            SampleAdvDB.EmployeeDepartmentHistory T_EDH = db.T_EMP_DEP_HIST;
            SampleAdvDB.EmployeeDepSinceView V_EDS = db.V_EMP_DEP_SINCE_VIEW;
            SampleAdvDB.Departments T_DEP = db.T_DEPARTMENTS;

            // Define the query
            DBCommand cmd = db.createCommand();
            // Select requried columns
            cmd.select(T_EMP.C_EMPLOYEE_ID);
            cmd.select(T_DEP.C_DEPARTMENT_ID);
            cmd.select(T_EMP.C_LASTNAME.append(", ")
                       .append(T_EMP.C_FIRSTNAME.coalesce(DBDatabase.EMPTY_STRING))
                       .append(" (").append(T_DEP.C_NAME).append(")"));
            // Set Joins
            cmd.join(T_EDH.C_EMPLOYEE_ID, V_EDS.C_EMPLOYEE_ID)
              .where(T_EDH.C_DATE_FROM.is(V_EDS.C_MAX_DATE_FROM));
            cmd.join(T_EMP.C_EMPLOYEE_ID, T_EDH.C_EMPLOYEE_ID);
            cmd.join(T_DEP.C_DEPARTMENT_ID, T_EDH.C_DEPARTMENT_ID);
            // done
            return cmd;
        }
    }
    
    // Declare all Tables
    public final Departments  T_DEPARTMENTS = new Departments(this);
    public final Employees    T_EMPLOYEES   = new Employees(this);
    public final EmployeeDepartmentHistory  T_EMP_DEP_HIST = new EmployeeDepartmentHistory(this);
    // Declare all Views
    public final EmployeeDepSinceView V_EMP_DEP_SINCE_VIEW;
    public final EmployeeInfoView V_EMPLOYEE_INFO; 
    
    /**
     * Constructor of the SampleDB data model description
     *
     * Put all foreigen key realtions here.
     */
    public SampleAdvDB()
    {
        // Create views
        V_EMP_DEP_SINCE_VIEW = new EmployeeDepSinceView(this, T_EMP_DEP_HIST);
        V_EMPLOYEE_INFO = new EmployeeInfoView(this, T_EMPLOYEES, T_DEPARTMENTS);
        
        // Define Foreign-Key Relations
        addRelation( T_EMP_DEP_HIST.C_EMPLOYEE_ID  .referenceOn( T_EMPLOYEES.C_EMPLOYEE_ID ));
        addRelation( T_EMP_DEP_HIST.C_DEPARTMENT_ID.referenceOn( T_DEPARTMENTS.C_DEPARTMENT_ID ));
    }

}
