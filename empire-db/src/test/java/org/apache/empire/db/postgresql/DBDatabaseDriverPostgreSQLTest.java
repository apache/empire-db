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

import static org.junit.Assert.assertEquals;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.empire.DBResource;
import org.apache.empire.DBResource.DB;
import org.apache.empire.db.CompanyDB;
import org.apache.empire.db.DBCommand;
import org.apache.empire.db.DBDatabaseDriver;
import org.apache.empire.db.DBReader;
import org.apache.empire.db.DBRecord;
import org.apache.empire.db.DBSQLScript;
import org.apache.empire.db.exceptions.QueryFailedException;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

// Ignored as the db is not available everywhere
@Ignore
public class DBDatabaseDriverPostgreSQLTest 
{

	@Rule
	public DBResource dbResource = new DBResource(DB.POSTGRESQL);

	@Test
	public void testCreateReverseFunction() throws SQLException 
	{
		Connection conn = dbResource.getConnection();
		DBDatabaseDriver driver = dbResource.newDriver();
		((DBDatabaseDriverPostgreSQL)driver).createReverseFunction(conn);
		
		Statement statement = conn.createStatement();
		ResultSet resultSet = statement.executeQuery("SELECT reverse('reverseme')");
		resultSet.next();
		String reversed = resultSet.getString(1);
		resultSet.close();
		statement.close();
		conn.close();
		
		assertEquals("emesrever", reversed);
	}
	
	@Test
	public void testBlobDDL() throws SQLException 
	{
		Connection conn = dbResource.getConnection();

		DBDatabaseDriver driver = dbResource.newDriver();
		CompanyDB db = new CompanyDB();

		// Encoding issue occurs when prepared statement is disabled
		//db.setPreparedStatementsEnabled(true);

		db.open(driver, dbResource.getConnection());

		if(!databaseExists(conn, db)){
			DBSQLScript script = new DBSQLScript();
			db.getCreateDDLScript(db.getDriver(), script);
			System.out.println(script.toString());
			script.run(db.getDriver(), dbResource.getConnection(), false);
		}
		
		conn.close();
	}
	
	@Test
	public void testBlobWritingReading() throws SQLException 
	{
		Connection conn = dbResource.getConnection();

		DBDatabaseDriver driver = dbResource.newDriver();
		CompanyDB db = new CompanyDB();

		// Encoding issue occurs when prepared statement is disabled
		//db.setPreparedStatementsEnabled(true);

		db.open(driver, dbResource.getConnection());
		
		DBRecord emp = new DBRecord();
        emp.create(db.DATA);
        emp.setValue(db.DATA.DATA, new byte[]{1,2,3});
        emp.update(conn);
		
		// read a value
		DBCommand cmd = db.createCommand();
		cmd.select(db.DATA.DATA);
		DBReader reader = new DBReader();
		reader.open(cmd, conn);
		while(reader.moveNext()){
			byte[] value = (byte[]) reader.getValue(db.DATA.DATA);
			Assert.assertArrayEquals(new byte[]{1,2,3}, value);
		}
		conn.close();
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
			return (db.querySingleInt(cmd, -1, conn) >= 0);
		}catch(QueryFailedException ex){
			System.out.println("Checking whether table DEPARTMENTS exists (SQLException will be logged if not - please ignore) ...");
			System.out.println(ex.getMessage());
		}
		return false;
	}
}
