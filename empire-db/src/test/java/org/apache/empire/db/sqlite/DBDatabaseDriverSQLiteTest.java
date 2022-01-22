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
package org.apache.empire.db.sqlite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.util.Date;

import org.apache.empire.DBResource;
import org.apache.empire.DBResource.DB;
import org.apache.empire.data.DataType;
import org.apache.empire.db.CompanyDB;
import org.apache.empire.db.DBCmdType;
import org.apache.empire.db.DBContext;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.DBDatabaseDriver;
import org.apache.empire.db.DBRecord;
import org.apache.empire.db.DBSQLScript;
import org.apache.empire.db.DBTable;
import org.apache.empire.db.DBTableColumn;
import org.apache.empire.db.context.DBContextStatic;
import org.junit.Rule;
import org.junit.Test;


public class DBDatabaseDriverSQLiteTest{
 
    @Rule
    public DBResource dbResource = new DBResource(DB.SQLITE);
    
    @Test
    public void test()
    {
        Connection conn = dbResource.getConnection();
        DBDatabaseDriver driver = dbResource.newDriver();
        DBContext context = new DBContextStatic(driver, conn); 
        
        CompanyDB db = new CompanyDB();
        db.open(context);
        DBSQLScript script = new DBSQLScript(context);
        db.getCreateDDLScript(script);
        script.executeAll(false);
        
        DBRecord dep = new DBRecord(context, db.DEPARTMENT);
        dep.create();
        dep.setValue(db.DEPARTMENT.NAME, "junit");
        dep.setValue(db.DEPARTMENT.BUSINESS_UNIT, "test");
        dep.update();
        
        Date date = dep.getDateTime(db.DEPARTMENT.UPDATE_TIMESTAMP);
        assertNotNull("Date is null", date);
        assertTrue("No departments", dep.getInt(db.DEPARTMENT.ID) > 0);
        
        DBRecord emp = new DBRecord(context, db.EMPLOYEE);
        emp.create();
        emp.setValue(db.EMPLOYEE.FIRSTNAME, "junit");
        emp.setValue(db.EMPLOYEE.LASTNAME, "test");
        emp.setValue(db.EMPLOYEE.GENDER, "m");
        emp.setValue(db.EMPLOYEE.DEPARTMENT_ID, dep.getInt(db.DEPARTMENT.ID));
        emp.update();
        
        date = emp.getDateTime(db.EMPLOYEE.UPDATE_TIMESTAMP);
        assertNotNull("Date is null", date);
        assertTrue("Employee id O or less", emp.getInt(db.EMPLOYEE.ID) > 0);

        int id = emp.getInt(db.EMPLOYEE.ID);
        
        // Update an Employee
        emp = new DBRecord(context, db.EMPLOYEE);
        emp.read(id);
        // Set
        emp.setValue(db.EMPLOYEE.PHONE_NUMBER, "123456");
        emp.update();
        
        emp = new DBRecord(context, db.EMPLOYEE);
        emp.read(id);
        
        assertEquals("123456", emp.getString(db.EMPLOYEE.PHONE_NUMBER));
        
        script = new DBSQLScript(context);
        db.getDriver().getDDLScript(DBCmdType.DROP, db.EMPLOYEE, script);
        db.getDriver().getDDLScript(DBCmdType.DROP, db.DEPARTMENT, script);
        script.executeAll(true);
    }
    
    
    
    /**
     * See https://issues.apache.org/jira/browse/EMPIREDB-151
     */
    @Test
    public void testSequence(){
    	Connection conn = dbResource.getConnection();
        DBDatabaseDriver driver = dbResource.newDriver();
        DBContext context = new DBContextStatic(driver, conn); 
        
        SeqDB db = new SeqDB();
        db.open(context);
        DBSQLScript script = new DBSQLScript(context);
        db.getCreateDDLScript(script);
        script.executeAll(false);
        
        DBRecord data = new DBRecord(context, db.DATA);
        data.create();
        data.setValue(db.DATA.VALUE, "test");
        data.update();
        
        final long id = data.getLong(db.DATA.ID);
        
        DBRecord read = new DBRecord(context, db.DATA);
        read.read(id);
        
        assertEquals("test", read.getString(db.DATA.VALUE));
        
        script = new DBSQLScript(context);
        db.getDriver().getDDLScript(DBCmdType.DROP, db.DATA, script);
        script.executeAll(true);
    }
    
    /**
     * This is the basic database for testing
     *
     */
    private class SeqDB extends DBDatabase
    {
        // *Deprecated* private static final long serialVersionUID = 1L;
        public final Data DATA = new Data(this);
    }
    
    /**
     * For testing SEQUENCE auto generation stuff
     */
    public static class Data extends DBTable
    {
        // *Deprecated* private static final long serialVersionUID = 1L;
        public final DBTableColumn ID;
        public final DBTableColumn VALUE;

        public Data(DBDatabase db)
        {
            super("DATA", db);
            ID    = addColumn("DATA_ID",  		DataType.AUTOINC,       0, true);
            VALUE = addColumn("VALUE",          DataType.VARCHAR,     256, true);
            setPrimaryKey(ID);
        }
    }
}
