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
import org.apache.empire.data.DataMode;
import org.apache.empire.data.DataType;
import org.apache.empire.db.CompanyDB;
import org.apache.empire.db.DBCmdType;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.DBDatabaseDriver;
import org.apache.empire.db.DBRecord;
import org.apache.empire.db.DBSQLScript;
import org.apache.empire.db.DBTable;
import org.apache.empire.db.DBTableColumn;
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
        CompanyDB db = new CompanyDB();
        db.open(driver, dbResource.getConnection());
        DBSQLScript script = new DBSQLScript();
        db.getCreateDDLScript(db.getDriver(), script);
        script.executeAll(db.getDriver(), dbResource.getConnection(), false);
        
        DBRecord dep = new DBRecord();
        dep.create(db.DEPARTMENT);
        dep.setValue(db.DEPARTMENT.NAME, "junit");
        dep.setValue(db.DEPARTMENT.BUSINESS_UNIT, "test");
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
        script.executeAll(db.getDriver(), conn, true);
    }
    
    
    
    /**
     * See https://issues.apache.org/jira/browse/EMPIREDB-151
     */
    @Test
    public void testSequence(){
    	Connection conn = dbResource.getConnection();
        
        DBDatabaseDriver driver = dbResource.newDriver();
        SeqDB db = new SeqDB();
        db.open(driver, dbResource.getConnection());
        DBSQLScript script = new DBSQLScript();
        db.getCreateDDLScript(db.getDriver(), script);
        script.executeAll(db.getDriver(), dbResource.getConnection(), false);
        
        DBRecord data = new DBRecord();
        data.create(db.DATA);
        data.setValue(db.DATA.VALUE, "test");
        data.update(conn);
        
        final Object id = data.getLong(db.DATA.ID);
        
        DBRecord read = new DBRecord();
        read.read(db.DATA, id, conn);
        
        assertEquals("test", read.getString(db.DATA.VALUE));
        
        script = new DBSQLScript();
        db.getDriver().getDDLScript(DBCmdType.DROP, db.DATA, script);
        script.executeAll(db.getDriver(), conn, true);
    }
    
    /**
     * This is the basic database for testing
     *
     */
    private class SeqDB extends DBDatabase
    {
        private final static long serialVersionUID = 1L;
        public final Data DATA = new Data(this);
    }
    
    /**
     * For testing SEQUENCE auto generation stuff
     */
    public static class Data extends DBTable
    {
        private final static long serialVersionUID = 1L;
        public final DBTableColumn ID;
        public final DBTableColumn VALUE;

        public Data(DBDatabase db)
        {
            super("DATA", db);
            ID    = addColumn("DATA_ID",  		DataType.AUTOINC,       0, DataMode.AutoGenerated);
            VALUE = addColumn("VALUE",          DataType.VARCHAR,     256, DataMode.NotNull);
            setPrimaryKey(ID);
        }
    }
}
