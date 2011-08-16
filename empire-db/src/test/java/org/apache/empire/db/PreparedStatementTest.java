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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;

import org.apache.empire.DBResource;
import org.apache.empire.DBResource.DB;
import org.apache.empire.db.DBCommand.DBCommandParam;
import org.junit.Rule;
import org.junit.Test;


public class PreparedStatementTest{
 
    @Rule
    public DBResource dbResource = new DBResource(DB.HSQL);
    
    @Test
    public void testPreparedStatement()
    {
        Connection conn = dbResource.getConnection();
        
        DBDatabaseDriver driver = dbResource.newDriver();
        CompanyDB db = new CompanyDB();
        db.open(driver, conn);
        DBSQLScript script = new DBSQLScript();
        db.getCreateDDLScript(db.getDriver(), script);
        script.run(db.getDriver(), conn, false);
        
        DBRecord department = new DBRecord();
        department.create(db.DEPARTMENT);
        department.setValue(db.DEPARTMENT.NAME, "junit");
        department.setValue(db.DEPARTMENT.BUSINESS_UNIT, "test");
        department.update(conn);

        long id = department.getInt(db.DEPARTMENT.ID);
        assertTrue("Department add failed", id > 0);
        

        // Define shortcuts for tables used - not necessary but convenient
        CompanyDB.Departments DEP = db.DEPARTMENT;
        // Define the query
        DBCommand cmd = db.createCommand();
        // Create parameters
        DBCommandParam empIdParam  = cmd.addParam(null);
        // the previous line could be shorter
        // DBCommandParam empIdParam  = cmd.addCmdParam(id);
        // create statement
        cmd.select(DEP.getColumns());
        cmd.where(DEP.ID.is(empIdParam));
        // set param value
        empIdParam.setValue(id);
        // check command
        assertNotNull(cmd.getParamValues());
        assertTrue(cmd.getSelect().indexOf('?') > 0);

        DBReader r = new DBReader();
        try {
            r.open(cmd, conn);
            // must have one record
            assertEquals(true, r.moveNext());
            // Department Id must be correct
            assertEquals(id, r.getValue(DEP.ID));
        } finally {
            r.close();
        }
    }
}
