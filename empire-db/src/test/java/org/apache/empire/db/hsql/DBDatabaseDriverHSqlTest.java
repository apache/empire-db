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
package org.apache.empire.db.hsql;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.util.Date;

import org.apache.empire.HsqldbResource;
import org.apache.empire.db.CompanyDB;
import org.apache.empire.db.DBCmdType;
import org.apache.empire.db.DBDatabaseDriver;
import org.apache.empire.db.DBRecord;
import org.apache.empire.db.DBSQLScript;
import org.junit.Rule;
import org.junit.Test;


public class DBDatabaseDriverHSqlTest{
 
    @Rule
    public HsqldbResource hsqldb = new HsqldbResource();
    
    @Test
    public void test()
    {
        Connection conn = hsqldb.getConnection();
     
        DBDatabaseDriver driver = new DBDatabaseDriverHSql();
        CompanyDB db = new CompanyDB();
        db.open(driver, hsqldb.getConnection());
        DBSQLScript script = new DBSQLScript();
        db.getCreateDDLScript(db.getDriver(), script);
        script.run(db.getDriver(), hsqldb.getConnection(), false);
        
        DBRecord dep = new DBRecord();
        dep.create(db.DEPARTMENT);
        dep.setValue(db.DEPARTMENT.NAME, "junit");
        dep.setValue(db.DEPARTMENT.BUSINESS_UNIT, "testers");
        dep.update(conn);
        
        Date date = dep.getDateTime(db.DEPARTMENT.UPDATE_TIMESTAMP);
        assertNotNull("Date is null", date);
        assertTrue("No departments", dep.getInt(db.DEPARTMENT.ID) > 0);
        
        
        DBRecord emp = new DBRecord();
        emp.create(db.EMPLOYEE);
        emp.setValue(db.EMPLOYEE.FIRSTNAME, "junit");
        emp.setValue(db.EMPLOYEE.LASTNAME, "test");
        emp.setValue(db.EMPLOYEE.GENDER, "m");
        emp.setValue(db.EMPLOYEE.DEPARTMENT_ID, dep.getInt(db.DEPARTMENT.ID));
        emp.update(conn);
        
        date = emp.getDateTime(db.EMPLOYEE.UPDATE_TIMESTAMP);
        assertNotNull("Date is null", date);
        assertTrue("Employee id O or less", emp.getInt(db.EMPLOYEE.ID) > 0);

        int id = emp.getInt(db.EMPLOYEE.ID);
        
        // Update an Employee
        emp = new DBRecord();
        emp.read(db.EMPLOYEE, id, conn);
        // Set
        emp.setValue(db.EMPLOYEE.PHONE_NUMBER, "123456");
        emp.update(conn);
        
        emp = new DBRecord();
        emp.read(db.EMPLOYEE, id, conn);
        
        assertEquals("123456", emp.getString(db.EMPLOYEE.PHONE_NUMBER));
        
        script = new DBSQLScript();
        db.getDriver().getDDLScript(DBCmdType.DROP, db.EMPLOYEE, script);
        db.getDriver().getDDLScript(DBCmdType.DROP, db.DEPARTMENT, script);
        script.run(db.getDriver(), conn, true);
    }
}
