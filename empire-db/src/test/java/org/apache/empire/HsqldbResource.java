package org.apache.empire;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.commons.io.FileUtils;
import org.apache.empire.db.DBTools;
import org.junit.rules.ExternalResource;

public class HsqldbResource extends ExternalResource
{

    private static final String PATH = "target/hsqldb-unit-test/";
    
    public Connection connection;
    
    public Connection getConnection()
    {
        return connection;
    }
    
    @Override
    protected void before()
        throws Throwable
    {
        // clean up possible previous test files
        FileUtils.deleteDirectory(new File(PATH));
        
        Class.forName("org.hsqldb.jdbcDriver");
        connection = DriverManager.getConnection("jdbc:hsqldb:" + PATH, "sa", "");
    }
    
    @Override
    protected void after()
    {
        try
        {
            Statement st = connection.createStatement();
            // properly shutdown hsqldb
            st.execute("SHUTDOWN");
        } catch (SQLException ex)
        {
            ex.printStackTrace();
        }
        try
        {
            DBTools.close(connection);
        } catch (SQLException e)
        {
            e.printStackTrace();
        }
        try
        {
            FileUtils.deleteDirectory(new File(PATH));
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
