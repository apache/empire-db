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
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.empire.DBResource;
import org.apache.empire.DBResource.DB;
import org.apache.empire.data.DataMode;
import org.apache.empire.data.DataType;
import org.apache.empire.db.DBCommand;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.DBDatabaseDriver;
import org.apache.empire.db.DBRecord;
import org.apache.empire.db.DBSQLScript;
import org.apache.empire.db.DBTable;
import org.apache.empire.db.DBTableColumn;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

@Ignore
public class DBDatabaseDriverMSSqlDateTest {

	@Rule
	public DBResource dbResource = new DBResource(DB.MSSQL_JTDS);

	@Test
	public void testDateFormatYMD() throws Exception {
		runTestWithDateFormat("ymd");
	}

	@Test
	public void testDateFormatDMY() throws Exception {
		runTestWithDateFormat("dmy");
	}

	public void runTestWithDateFormat(String dbDateFormat) throws Exception {
		Connection conn = dbResource.getConnection();

		DBDatabaseDriver driver = dbResource.newDriver();
		DateTimeTestDB db = new DateTimeTestDB();

		// Encoding issue occur when prepared statement is disabled
		// db.setPreparedStatementsEnabled(true);

		db.open(driver, dbResource.getConnection());

		if (!databaseExists(conn, db)) {
			DBSQLScript script = new DBSQLScript();
			db.getCreateDDLScript(db.getDriver(), script);
			System.out.println(script.toString());
			script.run(db.getDriver(), dbResource.getConnection(), false);
		}

		// STEP 5: Clear Database (Delete all records)
		clearDatabase(conn, db);

		// MSSQL datetime Accuracy => Rounded to increments of .000, .003, or
		// .007 seconds => ignore ms for comparison
		DateFormat truncDateFmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

		DateFormat dateFmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

		Timestamp lastLoginTs = new Timestamp(System.currentTimeMillis());
		Date regDate = dateFmt.parse("2015-08-20 00:00:00.000");

		{

			driver.executeSQL("SET DATEFORMAT " + dbDateFormat, null, conn,
					null);
			DBRecord rec = new DBRecord();
			rec.create(db.USER_INFO);
			rec.setValue(db.USER_INFO.USER, "john.doe");
			rec.setValue(db.USER_INFO.REG_DATE, regDate);
			rec.setValue(db.USER_INFO.LAST_LOGIN, lastLoginTs);
			rec.update(conn);

			int id = rec.getInt(db.USER_INFO.ID);

			DBRecord recRead = new DBRecord();
			recRead.read(db.USER_INFO, id, conn);

			assertEquals(truncDateFmt.format(lastLoginTs),
					truncDateFmt.format(recRead
							.getDateTime(db.USER_INFO.LAST_LOGIN)));
			assertEquals(truncDateFmt.format(regDate),
					truncDateFmt.format(recRead
							.getDateTime(db.USER_INFO.REG_DATE)));

		}

	}

	/**
	 * <PRE>
	 * Empties all Tables.
	 * </PRE>
	 */
	private static void clearDatabase(Connection conn, DateTimeTestDB db) {
		DBCommand cmd = db.createCommand();
		// Delete all Employees (no constraints)
		db.executeSQL(cmd.getDelete(db.USER_INFO), conn);
	}

	/**
	 * <PRE>
	 * Checks whether the database exists or not by executing
	 *     select count(*) from USER_INFO
	 * If the Departments table does not exist the querySingleInt() function return -1 for failure.
	 * Please note that in this case an error will appear in the log which can be ignored.
	 * </PRE>
	 */
	private static boolean databaseExists(Connection conn, DateTimeTestDB db) {
		// Check whether DB exists
		DBCommand cmd = db.createCommand();
		cmd.select(db.USER_INFO.count());
		// Check using "select count(*) from DEPARTMENTS"

		//System.out
				//.println("Checking whether table USER_INFO exists (SQLException will be logged if not - please ignore) ...");
		try {
			return (db.querySingleInt(cmd, -1, conn) >= 0);
		} catch (Exception e) {
			return false;
		}

	}

	private static class DateTimeTestDB extends DBDatabase {

		private static final long serialVersionUID = 1L;

		public static class UserInfoTable extends DBTable {

			public final DBTableColumn ID;
			public final DBTableColumn USER;
			public final DBTableColumn REG_DATE;
			public final DBTableColumn LAST_LOGIN;

			public UserInfoTable(DBDatabase db) {
				super("USER_INFO", db);
				ID = addColumn("DEPARTMENT_ID", DataType.AUTOINC, 0,
						DataMode.AutoGenerated);
				USER = addColumn("USER", DataType.TEXT, 80, DataMode.NotNull);
				REG_DATE = addColumn("REG_DATE", DataType.DATE, 80,
						DataMode.NotNull);
				LAST_LOGIN = addColumn("LAST_LOGIN", DataType.DATETIME, 0,
						DataMode.Nullable);

				// Primary Key
				setPrimaryKey(ID);

			}

		}

		public final UserInfoTable USER_INFO = new UserInfoTable(this);

		public DateTimeTestDB() {
			super();
		}

	}

}
