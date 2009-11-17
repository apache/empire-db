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
		boolean succes = DBUtil.close(null, log);
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
		Mockito.verify(log).error("The resultset could not bel closed!", exception);
	}

}
