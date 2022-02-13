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
package org.apache.empire.dbms.postgresql;

import static org.junit.Assert.assertEquals;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.empire.DBResource;
import org.apache.empire.DBResource.DB;
import org.apache.empire.db.CompanyDB;
import org.apache.empire.db.DBCommand;
import org.apache.empire.db.DBContext;
import org.apache.empire.db.DBReader;
import org.apache.empire.db.DBRecord;
import org.apache.empire.db.DBSQLScript;
import org.apache.empire.db.context.DBContextStatic;
import org.apache.empire.db.exceptions.QueryFailedException;
import org.apache.empire.dbms.DBMSHandler;
import org.apache.empire.dbms.postgresql.DBMSHandlerPostgreSQL;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

// Ignored as the db is not available everywhere
@Ignore
public class DBMSHandlerPostgreSQLTest 
{

	@Rule
	public DBResource dbResource = new DBResource(DB.POSTGRESQL);

	@Test
	public void testCreateReverseFunction() throws SQLException 
	{
		Connection conn = dbResource.getConnection();
		DBMSHandler dbms = dbResource.newDriver();
		((DBMSHandlerPostgreSQL)dbms).createReverseFunction(conn);
		
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
		DBMSHandler dbms = dbResource.newDriver();
        DBContext context = new DBContextStatic(dbms, conn); 
		
		CompanyDB db = new CompanyDB();

		// Encoding issue occurs when prepared statement is disabled
		//db.setPreparedStatementsEnabled(true);

		db.open(context);

		if(!databaseExists(context, db)){
			DBSQLScript script = new DBSQLScript(context);
			db.getCreateDDLScript(script);
			System.out.println(script.toString());
			script.executeAll(false);
		}
		
		conn.close();
	}
	
	@Test
	public void testBlobWritingReading() throws SQLException 
	{
		Connection conn = dbResource.getConnection();

		DBMSHandler dbms = dbResource.newDriver();
        DBContext context = new DBContextStatic(dbms, conn); 

        CompanyDB db = new CompanyDB();

		// Encoding issue occurs when prepared statement is disabled
		//db.setPreparedStatementsEnabled(true);

		db.open(context);
		
		DBRecord emp = new DBRecord(context, db.DATA);
        emp.create();
        emp.set(db.DATA.DATA, new byte[]{1,2,3});
        emp.update();
		
		// read a value
		DBCommand cmd = db.createCommand();
		cmd.select(db.DATA.DATA);
		DBReader reader = new DBReader(context);
		try {
	        reader.open(cmd);
	        while(reader.moveNext()){
	            byte[] value = (byte[]) reader.get(db.DATA.DATA);
	            Assert.assertArrayEquals(new byte[]{1,2,3}, value);
	        }
		} finally {
		    reader.close();
		}
		conn.close();
	}
	
	/**
	 * Checks whether the database exists or not by executing
	 *     select count(*) from DEPARTMENTS
	 * If the Departments table does not exist the querySingleInt() function return -1 for failure.
	 * Please note that in this case an error will appear in the log which can be ignored.
	 */
	private static boolean databaseExists(DBContext context, CompanyDB db)
    {
		// Check whether DB exists
		DBCommand cmd = db.createCommand();
		cmd.select(db.DEPARTMENT.count());
		// Check using "select count(*) from DEPARTMENTS"
		
		try{
			return (context.getUtils().querySingleInt(cmd, -1) >= 0);
		}catch(QueryFailedException ex){
			System.out.println("Checking whether table DEPARTMENTS exists (SQLException will be logged if not - please ignore) ...");
			System.out.println(ex.getMessage());
		}
		return false;
	}
}
