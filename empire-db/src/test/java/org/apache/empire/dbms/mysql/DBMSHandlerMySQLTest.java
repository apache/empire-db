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
package org.apache.empire.dbms.mysql;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.empire.data.DataType;
import org.apache.empire.db.CompanyDB;
import org.apache.empire.db.CompanyDB.Departments;
import org.apache.empire.db.DBCommand;
import org.apache.empire.db.DBContext;
import org.apache.empire.db.context.DBContextStatic;
import org.apache.empire.dbms.DBMSHandler;
import org.apache.empire.dbms.mysql.DBMSHandlerMySQL;
import org.junit.Test;

public class DBMSHandlerMySQLTest {
	
	@Test
	public void appendSQLTextValue()
	{

        CompanyDB db = new CompanyDB();
        DBMSHandler dbms = new DBMSHandlerMySQL();
        DBContext context = new DBContextStatic(dbms, null);
        db.open(context);

        Departments TD = db.DEPARTMENT;
        
		DBCommand cmd = db.createCommand();

		// Test 1
		cmd.select(TD.count());
		cmd.where(TD.NAME.is("\\LCI\\"));
		assertTrue(cmd.getSelect().endsWith(("`NAME`='\\\\LCI\\\\'"))); // Must be double escaped
		
		cmd = db.createCommand();

		// Test 2
		cmd.select(TD.count());
		cmd.where(TD.NAME.is("'"));
		assertTrue(cmd.getSelect().contains("`NAME`=''''"));

		cmd = db.createCommand();
		
		// \ and '
		cmd.select(TD.count());
		cmd.where(TD.NAME.is("Tarkk\\'ampujankatu"));
		assertTrue(cmd.getSelect().contains("`NAME`='Tarkk\\\\''ampujankatu'"));
		
	}
	
	@Test
	public void testGetConvertPhrase()
	{
		
        DBMSHandler dbms = new DBMSHandlerMySQL();

        // BOOL
        assertEquals("CAST(? AS UNSIGNED)", dbms.getConvertPhrase(DataType.BOOL, null, null));
        assertEquals("CAST(? AS UNSIGNED)", dbms.getConvertPhrase(DataType.BOOL, null, "test"));
        
        // INTEGER
        assertEquals("CAST(? AS SIGNED)", dbms.getConvertPhrase(DataType.INTEGER, null, null));
        assertEquals("CAST(? AS SIGNED)", dbms.getConvertPhrase(DataType.INTEGER, null, "test"));

        // DECIMAL
        assertEquals("CAST(? AS DECIMAL)", dbms.getConvertPhrase(DataType.DECIMAL, null, null));
        assertEquals("CAST(? AS DECIMAL)", dbms.getConvertPhrase(DataType.DECIMAL, null, "test"));
        
        // FLOAT
        assertEquals("CAST(? AS DECIMAL)", dbms.getConvertPhrase(DataType.FLOAT, null, null));
        assertEquals("CAST(? AS DECIMAL)", dbms.getConvertPhrase(DataType.FLOAT, null, "test"));
        
        // DATE
        assertEquals("CAST(? AS DATE)", dbms.getConvertPhrase(DataType.DATE, null, null));
        assertEquals("CAST(? AS DATE)", dbms.getConvertPhrase(DataType.DATE, null, "test"));
        
        // DATETIME
        assertEquals("CAST(? AS DATETIME)", dbms.getConvertPhrase(DataType.TIMESTAMP, null, null));
        assertEquals("CAST(? AS DATETIME)", dbms.getConvertPhrase(DataType.DATETIME, null, "test"));
        
        // TEXT
        assertEquals("CAST(? AS CHAR CHARACTER SET cp1250)", dbms.getConvertPhrase(DataType.VARCHAR, null, "CHARACTER SET cp1250"));
        assertEquals("CAST(? AS CHAR)", dbms.getConvertPhrase(DataType.VARCHAR, null, null));
        
        // BLOB
        assertEquals("CAST(? AS BLOB)", dbms.getConvertPhrase(DataType.BLOB, null, null));
        assertEquals("CAST(? AS BLOB)", dbms.getConvertPhrase(DataType.BLOB, null, "test"));
        
     // Unknown Type
        assertEquals("?", dbms.getConvertPhrase(DataType.AUTOINC, null, null));
        assertEquals("?", dbms.getConvertPhrase(DataType.AUTOINC, null, "test"));
        
	}
	
}
