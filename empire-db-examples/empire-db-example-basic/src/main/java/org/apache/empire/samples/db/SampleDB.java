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

import java.sql.SQLException;

import org.apache.empire.data.DataType;
import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBContext;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.DBSQLScript;
import org.apache.empire.db.DBTable;
import org.apache.empire.db.DBTableColumn;
import org.apache.empire.db.validation.DBModelChecker;
import org.apache.empire.db.validation.DBModelErrorLogger;
import org.apache.empire.dbms.postgresql.DBMSHandlerPostgreSQL;
import org.apache.empire.samples.db.beans.Department;
import org.apache.empire.samples.db.beans.Employee;
import org.apache.empire.samples.db.beans.Payment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <PRE>
 * This file contains the definition of the data model in Java.
 * The SampleDB data model consists of three tables and a foreign key relation.
 * The tables are defined as nested classes here, but you may put them in separate files if you want to.
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
    // Logger
    private static final Logger log = LoggerFactory.getLogger(SampleDB.class);
    
    /**
     * Gender enum
     */
    public enum Gender
    {
    	M("Male"),
    	F("Female"),
        U("Unknown");
        
        private final String title;
        private Gender(String title)
        {
            this.title = title;
        }
        @Override
        public String toString()
        {
            return title;
        }
    }

    /**
     * This class represents the Departments table.
     */
    public static class Departments extends DBTable
    {
        public final DBTableColumn ID;
        public final DBTableColumn NAME;
        public final DBTableColumn HEAD;
        public final DBTableColumn BUSINESS_UNIT;
        public final DBTableColumn UPDATE_TIMESTAMP;

        public Departments(SampleDB db)
        {
            super("DEPARTMENTS", db);
            // ID
            ID              = addIdentity ("ID",               "DEP_ID_SEQUENCE"); // Optional Sequence for some DBMS (e.g. Oracle)
            NAME            = addColumn   ("NAME",             DataType.VARCHAR,      80, true);
            HEAD            = addColumn   ("HEAD",             DataType.VARCHAR,      80, false);
            BUSINESS_UNIT   = addColumn   ("BUSINESS_UNIT",    DataType.VARCHAR,       4, true, "ITTK");
            UPDATE_TIMESTAMP= addTimestamp("UPDATE_TIMESTAMP");

            // Primary Key (automatically set due to AUTOINC column)
            // setPrimaryKey(DEPARTMENT_ID);
            // Set other Indexes
            addIndex("DEPARTMENT_NAME_IDX", true, new DBColumn[] { NAME });
            
            // Set beanType (optional)
            setBeanType(Department.class);
        }
    }

    /**
     * This class represents the Employees table.
     */
    public static class Employees extends DBTable
    {
        public final DBTableColumn ID;
        public final DBTableColumn SALUTATION;
        public final DBTableColumn FIRST_NAME;
        public final DBTableColumn LAST_NAME;
        public final DBTableColumn DATE_OF_BIRTH;
        public final DBTableColumn DEPARTMENT_ID;
        public final DBTableColumn GENDER;
        public final DBTableColumn PHONE_NUMBER;
        public final DBTableColumn EMAIL;
        public final DBTableColumn SALARY;
        public final DBTableColumn RETIRED;
        public final DBTableColumn UPDATE_TIMESTAMP;

        public Employees(SampleDB db)
        {
            super("EMPLOYEES", db);
            
            // ID
            ID              = addIdentity  ("ID",               "EMPLOYEE_ID_SEQUENCE");  // Optional Sequence name for some DBMS (e.g. Oracle)
            SALUTATION      = addColumn    ("SALUTATION",       DataType.VARCHAR,      5, false);
            FIRST_NAME      = addColumn    ("FIRST_NAME",       DataType.VARCHAR,     40, true);
            LAST_NAME       = addColumn    ("LAST_NAME",        DataType.VARCHAR,     40, true);
            DATE_OF_BIRTH   = addColumn    ("DATE_OF_BIRTH",    DataType.DATE,         0, false);
            DEPARTMENT_ID   = addForeignKey("DEPARTMENT_ID",    db.DEPARTMENTS,           true);
            GENDER          = addColumn    ("GENDER",           DataType.VARCHAR,      1, true, Gender.class);
            PHONE_NUMBER    = addColumn    ("PHONE_NUMBER",     DataType.VARCHAR,     40, false);
            EMAIL           = addColumn    ("EMAIL",            DataType.VARCHAR,     80, false);
            SALARY          = addColumn    ("SALARY",           DataType.DECIMAL,   10.2, false);
            RETIRED         = addColumn    ("RETIRED",          DataType.BOOL,         0, true, false);
            UPDATE_TIMESTAMP= addTimestamp ("UPDATE_TIMESTAMP");
            
            // Primary Key (automatically set due to AUTOINC column)
            // setPrimaryKey(EMPLOYEE_ID);
            // Set other Indexes
            addIndex("EMPLOYEE_NAME_IDX", true, new DBColumn[] { FIRST_NAME, LAST_NAME, DATE_OF_BIRTH });

            // Set beanType (optional)
            setBeanType(Employee.class);
        }
    }

    /**
     * This class represents the Payments table.
     */
    public static class Payments extends DBTable
    {
        public final DBTableColumn EMPLOYEE_ID;
        public final DBTableColumn YEAR;
        public final DBTableColumn MONTH;
        public final DBTableColumn AMOUNT;

        public Payments(SampleDB db)
        {
            super("PAYMENTS", db);
            
            // ID
            EMPLOYEE_ID     = addForeignKey("EMPLOYEE_ID",  db.EMPLOYEES,             true);
            YEAR            = addColumn("YEAR",             DataType.DECIMAL,    4.0, true);
            MONTH           = addColumn("MONTH",            DataType.DECIMAL,    2.0, true);
            AMOUNT          = addColumn("AMOUNT",           DataType.DECIMAL,    8.2, true);

            // Primary Key (automatically set due to AUTOINC column)
            setPrimaryKey(EMPLOYEE_ID, YEAR, MONTH);
            // Set other Indexes

            // Set beanType (optional)
            setBeanType(Payment.class);
        }
    }
    
    // Declare all Tables and Views here
    public final Departments  DEPARTMENTS = new Departments(this);
    public final Employees    EMPLOYEES   = new Employees(this);
    public final Payments     PAYMENTS    = new Payments(this);

    /**
     * Constructor of the SampleDB data model
     *
     * Put all foreign key relations here.
     */
    public SampleDB()
    {
        // Define additional Foreign-Key Relations here
        // which have not already been defined by addForeignKey()
        // addRelation( {Source Column}.referenceOn( {Target Column} ));
        log.info("SampleDB has been created with {} Tables and {} Relations", getTables().size(), getRelations().size());
    }
    
    @Override
    public void open(DBContext context)
    {
        // Enable prepared statements
        // setPreparedStatementsEnabled(true);
        // Check exists
        if (checkExists(context))
        {   // attach to driver
            super.open(context);
            // yes, it exists, then check the model
            checkDataModel(context);
        }
        else
        {   // PostgreSQL does not support DDL in transaction
            if(getDbms() instanceof DBMSHandlerPostgreSQL)
                setAutoCommit(context, true);
            // create the database
            createDatabase(context);
            // PostgreSQL does not support DDL in transaction
            if(getDbms() instanceof DBMSHandlerPostgreSQL)
                setAutoCommit(context, false);
            // attach to driver
            super.open(context);
        }
    }

    private void createDatabase(DBContext context)
    {
        // create DDL for Database Definition
        DBSQLScript script = new DBSQLScript(context);
        getCreateDDLScript(script);
        // Show DDL Statement
        log.info(script.toString());
        // Execute Script
        script.executeAll(false);
        // Commit
        context.commit();
    }
    
    private void checkDataModel(DBContext context)
    {   try {
            DBModelChecker modelChecker = context.getDbms().createModelChecker(this);
            // Check data model   
            log.info("Checking DataModel for {} using {}", getClass().getSimpleName(), modelChecker.getClass().getSimpleName());
            // dbo schema
            DBModelErrorLogger logger = new DBModelErrorLogger();
            modelChecker.checkModel(this, context.getConnection(), logger);
            // show result
            log.info("Data model check done. Found {} errors and {} warnings.", logger.getErrorCount(), logger.getWarnCount());
        } catch(Exception e) {
            log.error("FATAL error when checking data model. Probably not properly implemented by DBMSHandler!");
        }
    }
    
    private void setAutoCommit(DBContext context, boolean enable)
    {   try {
            context.getConnection().setAutoCommit(enable);
        } catch (SQLException e) {
            log.error("Unable to set AutoCommit on Connection", e);
        }
    }
}
