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
package org.apache.empire.db.codegen.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.logging.Log;
import org.junit.Test;
import org.mockito.Mockito;

public class DBUtilTest {

	@Test
	public void testCloseResultSet() throws SQLException {
		// null
		Log log = Mockito.mock(Log.class);
		boolean succes = DBUtil.close((ResultSet)null, log);
		assertTrue(succes);
		
		// normal
		ResultSet rs = Mockito.mock(ResultSet.class);
		boolean succes2 = DBUtil.close(rs, log);
		assertTrue(succes2);
		
		// exception
		ResultSet rsFail = Mockito.mock(ResultSet.class);
		Exception exception = new SQLException("test");
		Mockito.doThrow(exception).when(rsFail).close();
		boolean succes3 = DBUtil.close(rsFail, log);
		assertFalse(succes3);
		Mockito.verify(log).error("The resultset could not be closed!", exception);
	}

}
