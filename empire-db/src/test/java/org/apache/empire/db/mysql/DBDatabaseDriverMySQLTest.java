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
package org.apache.empire.db.mysql;

import static org.junit.Assert.assertTrue;

import org.apache.empire.db.CompanyDB;
import org.apache.empire.db.CompanyDB.Departments;
import org.apache.empire.db.DBCommand;
import org.apache.empire.db.DBDatabaseDriver;
import org.junit.Test;

public class DBDatabaseDriverMySQLTest {
	
	@Test
	public void appendSQLTextValue()
	{

        CompanyDB db = new CompanyDB();
        DBDatabaseDriver driver = new DBDatabaseDriverMySQL();
        db.open(driver, null);

        Departments TD = db.DEPARTMENT;
        
		DBCommand cmd = db.createCommand();

		// Test 1
		cmd.select(TD.count());
		cmd.where(TD.NAME.is("\\LCI\\"));
		assertTrue(cmd.getSelect().endsWith(("NAME='\\\\LCI\\\\'"))); // Must be double escaped
		
		cmd = db.createCommand();

		// Test 2
		cmd.select(TD.count());
		cmd.where(TD.NAME.is("'"));
		assertTrue(cmd.getSelect().contains("NAME=''''"));

		cmd = db.createCommand();
		
		// \ and '
		cmd.select(TD.count());
		cmd.where(TD.NAME.is("Tarkk\\'ampujankatu"));
		assertTrue(cmd.getSelect().contains("NAME='Tarkk\\\\''ampujankatu'"));
		
	}
	
}
