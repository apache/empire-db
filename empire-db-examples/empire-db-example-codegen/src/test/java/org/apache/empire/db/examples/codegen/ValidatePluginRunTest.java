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
package org.apache.empire.db.examples.codegen;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.empire.db.DBCommand;
import org.apache.empire.db.DBDatabaseDriver;
import org.apache.empire.db.DBReader;
import org.apache.empire.db.example.MyDB;
import org.apache.empire.db.example.tables.Employees;
import org.apache.empire.db.hsql.DBDatabaseDriverHSql;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class ValidatePluginRunTest {

	private static Connection conn = null;

	@BeforeClass
	public static void openConnection() throws Exception {
		Class.forName("org.hsqldb.jdbcDriver").newInstance();
		conn = DriverManager.getConnection("jdbc:hsqldb:file:src/test/resources/hsqldb/sample;shutdown=true", "sa", "");
	}

	@AfterClass
	public static void closeConnection() throws Exception {
		if (conn != null) {
			try {
				Statement st = conn.createStatement();
				// properly shutdown hsqldb
				st.execute("SHUTDOWN");
			} catch (SQLException ex) {
				ex.printStackTrace();
			}
			conn.close();
		}
	}

	@Test
	public void testTargetFolder() {
		File file = new File("target/generated-sources/empiredb");
		assertTrue("No sources generated", file.exists());
		// TODO add extra validation for the real generated sources
	}

	@Test
	public void testGeneratedClass() throws ClassNotFoundException {
		Class<?> cls = Class.forName("org.apache.empire.db.example.MyDB");
		assertNotNull("Could not load generated class.", cls);
	}

	@Test
	public void useGeneratedCode() throws Exception {

		MyDB db = MyDB.get();
		Employees EMP = db.EMPLOYEES;

		DBDatabaseDriver driver = new DBDatabaseDriverHSql();
		db.open(driver, conn);
		DBCommand cmd = db.createCommand();
		cmd.select(EMP.EMPLOYEE_ID, EMP.FIRSTNAME);

		int rowCount = 0;
		DBReader reader = new DBReader();
		try {
			System.err.println(cmd.getSelect());
			reader.open(cmd, conn);
			while (reader.moveNext()) {
				rowCount++;
				System.out.println(reader.getString(EMP.EMPLOYEE_ID) + "\t" + reader.getString(EMP.FIRSTNAME));
			}
		} finally {
			reader.close();
		}
		
		assertEquals("We expect 3 rows", 3, rowCount);

	}

}
