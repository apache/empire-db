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
package org.apache.empire.db.postgresql;

import java.sql.Connection;

import org.apache.empire.DBResource;
import org.apache.empire.DBResource.DB;
import org.apache.empire.db.CompanyDB;
import org.apache.empire.db.DBCommand;
import org.apache.empire.db.DBDatabaseDriver;
import org.apache.empire.db.DBSQLScript;
import org.apache.empire.db.exceptions.QueryFailedException;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

@Ignore
public class DBDatabaseDriverPostgreSQLTest 
{

	@Rule
	public DBResource dbResource = new DBResource(DB.POSTGRESQL);

	@Test
	public void testBlob() 
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
	}
	
	/**
	 * Checks whether the database exists or not by executing
	 *     select count(*) from DEPARTMENTS
	 * If the Departments table does not exist the querySingleInt() function return -1 for failure.
	 * Please note that in this case an error will appear in the log which can be ignored.
	 */
	private static boolean databaseExists(Connection conn, CompanyDB db)
    {
		// Check whether DB exists
		DBCommand cmd = db.createCommand();
		cmd.select(db.DEPARTMENT.count());
		// Check using "select count(*) from DEPARTMENTS"
		
		try{
			return (db.querySingleInt(cmd.getSelect(), -1, conn) >= 0);
		}catch(QueryFailedException ex){
			System.out.println("Checking whether table DEPARTMENTS exists (SQLException will be logged if not - please ignore) ...");
			System.out.println(ex.getMessage());
		}
		return false;
	}
}
