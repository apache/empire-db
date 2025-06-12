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
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.empire.DBResource;
import org.apache.empire.DBResource.DB;
import org.apache.empire.data.DataType;
import org.apache.empire.db.DBCommand;
import org.apache.empire.db.DBContext;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.DBRecord;
import org.apache.empire.db.DBSQLScript;
import org.apache.empire.db.DBTable;
import org.apache.empire.db.DBTableColumn;
import org.apache.empire.db.context.DBContextStatic;
import org.apache.empire.dbms.DBMSHandler;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

@Ignore
public class DBMSHandlerMSSqlDateTest {

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
		DBMSHandler dbms = dbResource.newDriver();
        DBContext context = new DBContextStatic(dbms, conn); 
		
		DateTimeTestDB db = new DateTimeTestDB();

		// Encoding issue occur when prepared statement is disabled
		// db.setPreparedStatementsEnabled(true);

		db.open(context);

		if (!databaseExists(context, db)) {
			DBSQLScript script = new DBSQLScript(context);
			db.getCreateDDLScript(script);
			System.out.println(script.toString());
			script.executeAll(false);
		}

		// STEP 5: Clear Database (Delete all records)
		clearDatabase(context, db);

		// MSSQL datetime Accuracy => Rounded to increments of .000, .003, or
		// .007 seconds => ignore ms for comparison
		DateFormat truncDateFmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

		DateFormat dateFmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

		Timestamp lastLoginTs = new Timestamp(System.currentTimeMillis());
		Date regDate = dateFmt.parse("2015-08-20 00:00:00.000");

		{

			dbms.executeSQL("SET DATEFORMAT " + dbDateFormat, null, conn, null);
			DBRecord rec = new DBRecord(context, db.USER_INFO);
			rec.create();
			rec.set(db.USER_INFO.USER, "john.doe");
			rec.set(db.USER_INFO.REG_DATE, regDate);
			rec.set(db.USER_INFO.LAST_LOGIN, lastLoginTs);
			rec.update();

			int id = rec.getInt(db.USER_INFO.ID);

			DBRecord recRead = new DBRecord(context, db.USER_INFO);
			recRead.read(id);

			assertEquals(truncDateFmt.format(lastLoginTs),
					truncDateFmt.format(recRead
							.getDate(db.USER_INFO.LAST_LOGIN)));
			assertEquals(truncDateFmt.format(regDate),
					truncDateFmt.format(recRead
							.getDate(db.USER_INFO.REG_DATE)));

		}

	}

	/**
	 * <PRE>
	 * Empties all Tables.
	 * </PRE>
	 */
	private static void clearDatabase(DBContext context, DateTimeTestDB db) {
		DBCommand cmd = context.createCommand();
		// Delete all Employees (no constraints)
		context.executeDelete(db.USER_INFO, cmd);
	}

	/**
	 * <PRE>
	 * Checks whether the database exists or not by executing
	 *     select count(*) from USER_INFO
	 * If the Departments table does not exist the querySingleInt() function return -1 for failure.
	 * Please note that in this case an error will appear in the log which can be ignored.
	 * </PRE>
	 */
	private static boolean databaseExists(DBContext context, DateTimeTestDB db) {
		// Check whether DB exists
		DBCommand cmd = context.createCommand();
		cmd.select(db.USER_INFO.count());
		// Check using "select count(*) from DEPARTMENTS"

		//System.out
				//.println("Checking whether table USER_INFO exists (SQLException will be logged if not - please ignore) ...");
		try {
			return (context.getUtils().querySingleInt(cmd, -1) >= 0);
		} catch (Exception e) {
			return false;
		}

	}

	private static class DateTimeTestDB extends DBDatabase {

		// *Deprecated* private static final long serialVersionUID = 1L;

		public static class UserInfoTable extends DBTable {

            // *Deprecated* private static final long serialVersionUID = 1L;
            
            public final DBTableColumn ID;
			public final DBTableColumn USER;
			public final DBTableColumn REG_DATE;
			public final DBTableColumn LAST_LOGIN;

			public UserInfoTable(DBDatabase db) {
				super("USER_INFO", db);
				ID          = addColumn("DEPARTMENT_ID",    DataType.AUTOINC,    0, true);
				USER        = addColumn("USER",             DataType.VARCHAR,   80, true);
				REG_DATE    = addColumn("REG_DATE",         DataType.DATE,      80, true);
				LAST_LOGIN  = addColumn("LAST_LOGIN",       DataType.DATETIME,   0, false);
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
