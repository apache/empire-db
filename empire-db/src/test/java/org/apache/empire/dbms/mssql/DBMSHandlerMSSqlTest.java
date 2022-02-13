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
package org.apache.empire.dbms.mssql;

import static org.junit.Assert.assertEquals;

import java.sql.Connection;

import org.apache.empire.DBResource;
import org.apache.empire.DBResource.DB;
import org.apache.empire.db.CompanyDB;
import org.apache.empire.db.DBCommand;
import org.apache.empire.db.DBContext;
import org.apache.empire.db.DBRecord;
import org.apache.empire.db.DBSQLScript;
import org.apache.empire.db.context.DBContextStatic;
import org.apache.empire.dbms.DBMSHandler;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

@Ignore
public class DBMSHandlerMSSqlTest 
{

	@Rule
	public DBResource dbResource = new DBResource(DB.MSSQL_JTDS);

	@Test
	public void testChineseCharacters() 
	{
		Connection conn = dbResource.getConnection();
		DBMSHandler dbms = dbResource.newDriver();
        DBContext context = new DBContextStatic(dbms, conn); 
		
		CompanyDB db = new CompanyDB();

		// Encoding issue occur when prepared statement is disabled
		//db.setPreparedStatementsEnabled(true);

		db.open(context);

		if(!databaseExists(context, db)){
			DBSQLScript script = new DBSQLScript(context);
			db.getCreateDDLScript(script);
			System.out.println(script.toString());
			script.executeAll(false);
		}
		
		// STEP 5: Clear Database (Delete all records)
		System.out.println("*** Step 5: clearDatabase() ***");
		clearDatabase(context, db);

		DBRecord dep = new DBRecord(context, db.DEPARTMENT);
		dep.set(db.DEPARTMENT.NAME, "junit");
		dep.set(db.DEPARTMENT.BUSINESS_UNIT, "中文");
		dep.update();

		int id = dep.getInt(db.DEPARTMENT.ID);

		// Update an Employee
		DBRecord depRead = new DBRecord(context, db.DEPARTMENT);
		depRead.read(id);

		// You may see ?? in the DB record
		assertEquals("中文", depRead.getString(db.DEPARTMENT.BUSINESS_UNIT));
	}
	
	/**
     * <PRE>
	 * Empties all Tables.
     * </PRE>
	 */
	private static void clearDatabase(DBContext context, CompanyDB db)
    {
		DBCommand cmd = db.createCommand();
		// Delete all Employees (no constraints)
		context.executeDelete(db.DEPARTMENT, cmd);
	}
	
	/**
     * <PRE>
	 * Checks whether the database exists or not by executing
	 *     select count(*) from DEPARTMENTS
	 * If the Departments table does not exist the querySingleInt() function return -1 for failure.
	 * Please note that in this case an error will appear in the log which can be ignored.
     * </PRE>
	 */
	private static boolean databaseExists(DBContext context, CompanyDB db)
    {
		// Check whether DB exists
		DBCommand cmd = db.createCommand();
		cmd.select(db.DEPARTMENT.count());
		// Check using "select count(*) from DEPARTMENTS"
		System.out.println("Checking whether table DEPARTMENTS exists (SQLException will be logged if not - please ignore) ...");
		return (context.getUtils().querySingleInt(cmd, -1) >= 0);
	}
}
