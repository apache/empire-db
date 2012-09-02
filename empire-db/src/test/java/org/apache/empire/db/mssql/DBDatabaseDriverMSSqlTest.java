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
package org.apache.empire.db.mssql;

import static org.junit.Assert.assertEquals;

import java.sql.Connection;

import org.apache.empire.DBResource;
import org.apache.empire.DBResource.DB;
import org.apache.empire.db.CompanyDB;
import org.apache.empire.db.DBCommand;
import org.apache.empire.db.DBDatabaseDriver;
import org.apache.empire.db.DBRecord;
import org.apache.empire.db.DBSQLScript;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

@Ignore
public class DBDatabaseDriverMSSqlTest 
{

	@Rule
	public DBResource dbResource = new DBResource(DB.MSSQL_JTDS);

	@Test
	public void testChineseCharacters() 
	{
		Connection conn = dbResource.getConnection();

		DBDatabaseDriver driver = dbResource.newDriver();
		CompanyDB db = new CompanyDB();

		// Encoding issue occur when prepared statement is disabled
		//db.setPreparedStatementsEnabled(true);

		db.open(driver, dbResource.getConnection());

		if(!databaseExists(conn, db)){
			DBSQLScript script = new DBSQLScript();
			db.getCreateDDLScript(db.getDriver(), script);
			System.out.println(script.toString());
			script.run(db.getDriver(), dbResource.getConnection(), false);
		}
		
		// STEP 5: Clear Database (Delete all records)
		System.out.println("*** Step 5: clearDatabase() ***");
		clearDatabase(conn, db);

		DBRecord dep = new DBRecord();
		dep.create(db.DEPARTMENT);
		dep.setValue(db.DEPARTMENT.NAME, "junit");
		dep.setValue(db.DEPARTMENT.BUSINESS_UNIT, "中文");
		dep.update(conn);

		int id = dep.getInt(db.DEPARTMENT.ID);

		// Update an Employee
		DBRecord depRead = new DBRecord();
		depRead.read(db.DEPARTMENT, id, conn);

		// You may see ?? in the DB record
		assertEquals("中文", depRead.getString(db.DEPARTMENT.BUSINESS_UNIT));
	}
	
	/**
     * <PRE>
	 * Empties all Tables.
     * </PRE>
	 */
	private static void clearDatabase(Connection conn, CompanyDB db)
    {
		DBCommand cmd = db.createCommand();
		// Delete all Employees (no constraints)
		db.executeSQL(cmd.getDelete(db.DEPARTMENT), conn);
	}
	
	/**
     * <PRE>
	 * Checks whether the database exists or not by executing
	 *     select count(*) from DEPARTMENTS
	 * If the Departments table does not exist the querySingleInt() function return -1 for failure.
	 * Please note that in this case an error will appear in the log which can be ignored.
     * </PRE>
	 */
	private static boolean databaseExists(Connection conn, CompanyDB db)
    {
		// Check whether DB exists
		DBCommand cmd = db.createCommand();
		cmd.select(db.DEPARTMENT.count());
		// Check using "select count(*) from DEPARTMENTS"
		System.out.println("Checking whether table DEPARTMENTS exists (SQLException will be logged if not - please ignore) ...");
		return (db.querySingleInt(cmd, -1, conn) >= 0);
	}
}
