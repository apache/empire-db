package org.apache.empire.db.codegen.util;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.logging.Log;

public class DBUtil {
	public static boolean closeResultSet(ResultSet rs, Log log) {
		boolean b = false;
		try {
			if(rs!=null)
				rs.close();
			b = true;
		} catch (SQLException e) {
			log.error("The resultset could not bel closed!", e);
		}
		return b;
	}
}
