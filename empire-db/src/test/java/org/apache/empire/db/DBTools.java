/**
 * 
 */
package org.apache.empire.db;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * @author francisdb
 *
 */
public final class DBTools
{
    private DBTools()
    {
        throw new UnsupportedOperationException("Utility class");
    }
    
    public static void close(Connection conn) throws SQLException{
        if(conn != null){
            conn.close();
        }
    } 

}
