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

import java.sql.SQLException;

import org.apache.empire.commons.Options;
import org.apache.empire.data.DataType;
import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBColumnExpr;
import org.apache.empire.db.DBCommand;
import org.apache.empire.db.DBCommandExpr;
import org.apache.empire.db.DBContext;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.DBSQLScript;
import org.apache.empire.db.DBTableColumn;
import org.apache.empire.db.generic.TDatabase;
import org.apache.empire.db.generic.TTable;
import org.apache.empire.db.generic.TView;
import org.apache.empire.db.validation.DBModelChecker;
import org.apache.empire.db.validation.DBModelErrorLogger;
import org.apache.empire.dbms.postgresql.DBMSHandlerPostgreSQL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class SampleAdvDB extends TDatabase<SampleAdvDB>
{
    // *Deprecated* private static final long serialVersionUID = 1L;

    // Logger
    private static final Logger log = LoggerFactory.getLogger(SampleAdvDB.class);

    /**
     * This class represents the definition of the Departments table.
     */
    public static class Departments extends TTable<SampleAdvDB>
    {
        // *Deprecated* private static final long serialVersionUID = 1L;

        public final DBTableColumn C_DEPARTMENT_ID;
        public final DBTableColumn C_NAME;
        public final DBTableColumn C_HEAD;
        public final DBTableColumn C_BUSINESS_UNIT;
        public final DBTableColumn C_UPDATE_TIMESTAMP;

        public Departments(SampleAdvDB db)
        {
            super("DEPARTMENTS", db);
            // ID
            C_DEPARTMENT_ID   = addColumn("DEPARTMENT_ID",    DataType.AUTOINC,       0, true, "DEP_ID_SEQUENCE");
            C_NAME            = addColumn("NAME",             DataType.VARCHAR,      80, true);
            C_HEAD            = addColumn("HEAD",             DataType.VARCHAR,      80, false);
            C_BUSINESS_UNIT   = addColumn("BUSINESS_UNIT",    DataType.VARCHAR,       4, true, "ITTK");
            C_UPDATE_TIMESTAMP= addColumn("UPDATE_TIMESTAMP", DataType.TIMESTAMP,     0, true);

            // Primary Key
            setPrimaryKey(C_DEPARTMENT_ID);
            // Set other Indexes
            addIndex("DEARTMENT_NAME_IDX", true, new DBColumn[] { C_NAME });
        }
    }

    /**
     * This class represents the definition of the Employees table.
     */
    public static class Employees extends TTable<SampleAdvDB>
    {
        // *Deprecated* private static final long serialVersionUID = 1L;
      
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
        
        public Employees(SampleAdvDB db)
        {
            super("EMPLOYEES", db);
            // ID
            C_EMPLOYEE_ID     = addColumn("EMPLOYEE_ID",      DataType.AUTOINC,      0, true, "EMPLOYEE_ID_SEQUENCE");
            C_SALUTATION      = addColumn("SALUTATION",       DataType.VARCHAR,     20, false);
            C_FIRSTNAME       = addColumn("FIRSTNAME",        DataType.VARCHAR,     40, true);
            C_LASTNAME        = addColumn("LASTNAME",         DataType.VARCHAR,     40, true);
            C_DATE_OF_BIRTH   = addColumn("DATE_OF_BIRTH",    DataType.DATE,         0, false);
            C_GENDER          = addColumn("GENDER",           DataType.VARCHAR,      1, false);
            C_PHONE_NUMBER    = addColumn("PHONE_NUMBER",     DataType.VARCHAR,     40, false);
            C_EMAIL           = addColumn("EMAIL",            DataType.VARCHAR,     80, false);
            C_RETIRED         = addColumn("RETIRED",          DataType.BOOL,         0, true, false);
            C_CHECKSUM        = addColumn("CHECKSUM",         DataType.INTEGER,      0, false);
            C_UPDATE_TIMESTAMP= addColumn("UPDATE_TIMESTAMP", DataType.TIMESTAMP,    0, true);

            // Primary Key
            setPrimaryKey(C_EMPLOYEE_ID);
            // Set other Indexes
            addIndex("EMPLOYEE_NAME_IDX", true, new DBColumn[] { C_FIRSTNAME, C_LASTNAME, C_DATE_OF_BIRTH });

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
    public static class EmployeeDepartmentHistory extends TTable<SampleAdvDB>
    {
        // *Deprecated* private static final long serialVersionUID = 1L;
      
        public final DBTableColumn C_EMPLOYEE_ID;
        public final DBTableColumn C_DEPARTMENT_ID;
        public final DBTableColumn C_DATE_FROM;

        public EmployeeDepartmentHistory(SampleAdvDB db)
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
    public static class EmployeeDepSinceView extends TView<SampleAdvDB>
    {
        // *Deprecated* private static final long serialVersionUID = 1L;
      
        public final DBViewColumn C_EMPLOYEE_ID;
        public final DBViewColumn C_MAX_DATE_FROM;
        
        public EmployeeDepSinceView(SampleAdvDB db, EmployeeDepartmentHistory T_EDH)
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

            SampleAdvDB.EmployeeDepartmentHistory EDH = DB.T_EMP_DEP_HIST;
            
            // Define the sub query
            DBCommand cmd = db.createCommand();
            cmd.select (EDH.C_EMPLOYEE_ID, EDH.C_DATE_FROM.max());
            cmd.groupBy(EDH.C_EMPLOYEE_ID);
            return cmd;
        }
    }

    /**
     * This class represents the definition of the EmployeeInfoView table.
     */
    public static class EmployeeInfoView extends TView<SampleAdvDB>
    {
        // *Deprecated* private static final long serialVersionUID = 1L;
      
        public final DBViewColumn C_EMPLOYEE_ID;
        public final DBViewColumn C_CURRENT_DEP_ID;
        public final DBViewColumn C_NAME_AND_DEP;

        public EmployeeInfoView(SampleAdvDB db, Employees T_EMP, Departments T_DEP)
        {
            super("EMPLOYEE_INFO_VIEW", db);
            // ID
            C_EMPLOYEE_ID     = addColumn(T_EMP.C_EMPLOYEE_ID);
            C_CURRENT_DEP_ID  = addColumn("CURRENT_DEP_ID", T_DEP.C_DEPARTMENT_ID); 
            C_NAME_AND_DEP    = addColumn("NAME_AND_DEP",   DataType.VARCHAR);

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
            
            SampleAdvDB.Employees   EMP = DB.T_EMPLOYEES;
            SampleAdvDB.EmployeeDepartmentHistory EDH = DB.T_EMP_DEP_HIST;
            SampleAdvDB.EmployeeDepSinceView EDS = DB.V_EMP_DEP_SINCE_VIEW;
            SampleAdvDB.Departments DEP = DB.T_DEPARTMENTS;

            // Define the query
            DBCommand cmd = db.createCommand();
            // Select required columns
            cmd.select(EMP.C_EMPLOYEE_ID);
            cmd.select(DEP.C_DEPARTMENT_ID);
            cmd.select(EMP.C_LASTNAME.append(", ")
                       .append(EMP.C_FIRSTNAME.coalesce(DBDatabase.EMPTY_STRING))
                       .append(" (").append(DEP.C_NAME).append(")"));
            // Set Joins
            cmd.join(EDH.C_EMPLOYEE_ID, EDS.C_EMPLOYEE_ID, EDH.C_DATE_FROM.is(EDS.C_MAX_DATE_FROM));
            cmd.join(EMP.C_EMPLOYEE_ID, EDH.C_EMPLOYEE_ID);
            cmd.join(DEP.C_DEPARTMENT_ID, EDH.C_DEPARTMENT_ID);
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
     * Put all foreign key relations here.
     */
    public SampleAdvDB()
    {
        // Create views
        V_EMP_DEP_SINCE_VIEW = new EmployeeDepSinceView(this, T_EMP_DEP_HIST);
        V_EMPLOYEE_INFO      = new EmployeeInfoView(this, T_EMPLOYEES, T_DEPARTMENTS);
        
        // Define Foreign-Key Relations
        addRelation( T_EMP_DEP_HIST.C_EMPLOYEE_ID  .referenceOn( T_EMPLOYEES.C_EMPLOYEE_ID )).onDeleteCascade();
        addRelation( T_EMP_DEP_HIST.C_DEPARTMENT_ID.referenceOn( T_DEPARTMENTS.C_DEPARTMENT_ID ));
    }
    
    @Override
    public void open(DBContext context)
    {
        // Enable prepared statements
        setPreparedStatementsEnabled(true);
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
