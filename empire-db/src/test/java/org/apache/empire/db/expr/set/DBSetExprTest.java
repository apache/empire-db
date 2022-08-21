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
package org.apache.empire.db.expr.set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.apache.empire.db.CompanyDB;
import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.DBExpr;
import org.apache.empire.db.DBSQLBuilder;
import org.apache.empire.db.MockDriver;
import org.apache.empire.db.context.DBContextStatic;
import org.apache.empire.dbms.DBMSHandler;
import org.junit.Before;
import org.junit.Test;

public class DBSetExprTest
{
    private DBMSHandler dbms;
    private DBSetExpr expr;
    private CompanyDB testDB;
    
    @Before
    public void setup(){
        dbms = new MockDriver();
        testDB = new CompanyDB();
        testDB.open(new DBContextStatic(dbms, null));
        expr = new DBSetExpr(testDB.EMPLOYEE.FIRSTNAME, "JUnit");
    }

    @Test
    public void testGetDatabase()
    {
        assertEquals(testDB, expr.getDatabase());
    }

    @Test
    public void testAddSQL()
    {
        DBSQLBuilder builder = dbms.createSQLBuilder();
        expr.addSQL(builder, 0);
        assertEquals("", builder.toString());
        expr.addSQL(builder, DBExpr.CTX_DEFAULT);
        assertEquals("FIRSTNAME='JUnit'", builder.toString());
    }
    
    @Test
    public void testAddSQLEmptyString()
    {
        DBSQLBuilder builder = dbms.createSQLBuilder();
        DBSetExpr setExpr = new DBSetExpr(testDB.EMPLOYEE.FIRSTNAME, "");
        setExpr.addSQL(builder, DBExpr.CTX_DEFAULT);
        // Empire-DB by default sees '' as null
        assertEquals("FIRSTNAME=null", builder.toString());
    }
    
    @Test
    public void testAddSQLEmptyStringConstant()
    {
        DBSQLBuilder builder = dbms.createSQLBuilder();
        DBSetExpr setExpr = new DBSetExpr(testDB.EMPLOYEE.FIRSTNAME, DBDatabase.EMPTY_STRING);
        setExpr.addSQL(builder, DBExpr.CTX_DEFAULT);
        assertEquals("FIRSTNAME=''", builder.toString());
    }

    @Test
    public void testAddReferencedColumns()
    {
        Set<DBColumn> cols = new HashSet<DBColumn>();
        expr.addReferencedColumns(cols);
        assertTrue(cols.contains(testDB.EMPLOYEE.FIRSTNAME));
    }

    @Test
    public void testGetTable()
    {
        assertEquals(testDB.EMPLOYEE, expr.getTable());
    }

    @Test
    public void testGetColumn()
    {
        assertEquals(testDB.EMPLOYEE.FIRSTNAME, expr.getColumn());
    }

    @Test
    public void testGetValue()
    {
        assertEquals("JUnit", expr.getValue());
    }

    @Test
    public void testSetValue()
    {
        expr.setValue("JUnit2");
        assertEquals("JUnit2", expr.getValue());
    }

}
