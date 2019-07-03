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
    private final static long       serialVersionUID = 1L;

    // Declare all Tables
    public final TDepartments       T_DEPARTMENTS    = new TDepartments(this);
    public final TEmployees         T_EMPLOYEES      = new TEmployees(this);

    /**
     * Constructor SampleDB
     */
    public SampleDB()
    {
        // Define Foreign-Key Relations
        addRelation(T_EMPLOYEES.DEPARTMENT_ID.referenceOn(T_DEPARTMENTS.DEPARTMENT_ID));
    }

    // Needed for the DBELResolver
    @Override
    protected void register(String id)
    {
        super.register("db");
    }

    /**
     * This class represents the definition of the Departments table.
     */
    public static class TDepartments extends SampleTable
    {
        private static final long  serialVersionUID = 1L;

        public final DBTableColumn DEPARTMENT_ID;
        public final DBTableColumn NAME;
        public final DBTableColumn HEAD;
        public final DBTableColumn BUSINESS_UNIT;
        public final DBTableColumn UPDATE_TIMESTAMP;

        public TDepartments(DBDatabase db)
        {
            super("DEPARTMENTS", db);
            // ID
            DEPARTMENT_ID 	= addColumn("DEPARTMENT_ID", 	DataType.AUTOINC,	 0, DataMode.NotNull, "DEP_ID_SEQUENCE");
            NAME 			= addColumn("NAME", 			DataType.VARCHAR,   80, DataMode.NotNull);
            HEAD 			= addColumn("HEAD", 			DataType.VARCHAR,   80, DataMode.Nullable);
            BUSINESS_UNIT 	= addColumn("BUSINESS_UNIT", 	DataType.VARCHAR,    4, DataMode.NotNull, "ITTK");
            UPDATE_TIMESTAMP= addColumn("UPDATE_TIMESTAMP", DataType.DATETIME,	 0, DataMode.NotNull);

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
    public static class TEmployees extends SampleTable
    {
        private static final long  serialVersionUID = 1L;

        public final DBTableColumn EMPLOYEE_ID;
        public final DBTableColumn SALUTATION;
//      public final DBTableColumn PICTURE;
        public final DBTableColumn FIRST_NAME;
        public final DBTableColumn LAST_NAME;
        public final DBTableColumn DATE_OF_BIRTH;
        public final DBTableColumn DEPARTMENT_ID;
        public final DBTableColumn GENDER;
        public final DBTableColumn PHONE_NUMBER;
        public final DBTableColumn EMAIL;
        public final DBTableColumn RETIRED;
        public final DBTableColumn UPDATE_TIMESTAMP;
        public TEmployees(DBDatabase db)
        {
            super("EMPLOYEES", db);
            // ID
            EMPLOYEE_ID 	= addColumn("EMPLOYEE_ID", 		DataType.AUTOINC, 	 0, DataMode.NotNull, "EMPLOYEE_ID_SEQUENCE");
            SALUTATION 		= addColumn("SALUTATION", 		DataType.VARCHAR,	 5, DataMode.Nullable);
            FIRST_NAME 		= addColumn("FIRST_NAME", 		DataType.VARCHAR,	40, DataMode.NotNull);
            LAST_NAME 		= addColumn("LAST_NAME", 		DataType.VARCHAR,	40, DataMode.NotNull);
            DATE_OF_BIRTH 	= addColumn("DATE_OF_BIRTH", 	DataType.DATE,		 0, DataMode.Nullable);
            DEPARTMENT_ID 	= addColumn("DEPARTMENT_ID", 	DataType.INTEGER, 	 0, DataMode.NotNull);
            GENDER 			= addColumn("GENDER", 			DataType.VARCHAR,	 1, DataMode.Nullable);
            PHONE_NUMBER 	= addColumn("PHONE_NUMBER", 	DataType.VARCHAR,	40, DataMode.Nullable);
            EMAIL 			= addColumn("EMAIL", 			DataType.VARCHAR,	80, DataMode.Nullable);
            RETIRED			= addColumn("RETIRED", 			DataType.BOOL, 		 0, DataMode.NotNull, false);
            // PICTURE 		= addColumn("PICTURE", 			DataType.BLOB, 		 0, DataMode.Nullable);
            UPDATE_TIMESTAMP= addColumn("UPDATE_TIMESTAMP", DataType.DATETIME,	 0, DataMode.NotNull);

            // Primary Key
            setPrimaryKey(EMPLOYEE_ID);
            // Set other Indexes
            addIndex("PERSON_NAME_IDX", true, new DBColumn[] { FIRST_NAME, LAST_NAME, DATE_OF_BIRTH });

            // Set timestamp column for save updates
            setTimestampColumn(UPDATE_TIMESTAMP);

            // Create Options for GENDER column
            Options genders = new Options();
            genders.set("M", "!option.employee.gender.male");
            genders.set("F", "!option.employee.gender.female");
            GENDER.setOptions(genders);
            GENDER.setControlType("select");

            Options retired = new Options();
            retired.set(false, "!option.employee.active");
            retired.set(true,  "!option.employee.retired");
            RETIRED.setOptions(retired);
            RETIRED.setControlType("checkbox");
            
            // Set special control types
            DEPARTMENT_ID.setControlType("select");
            PHONE_NUMBER .setControlType("phone");
            
            // Set optional formatting attributes
            DATE_OF_BIRTH.setAttribute("format:date", "yyyy-MM-dd");
            
            // PICTURE.setControlType("blob");

        }
    }

}
