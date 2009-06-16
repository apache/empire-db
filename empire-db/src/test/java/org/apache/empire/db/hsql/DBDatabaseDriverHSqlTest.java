package org.apache.empire.db.hsql;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Date;

import org.apache.empire.db.CompanyDB;
import org.apache.empire.db.DBCmdType;
import org.apache.empire.db.DBDatabaseDriver;
import org.apache.empire.db.DBRecord;
import org.apache.empire.db.DBSQLScript;
import org.apache.empire.db.DBTools;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;


public class DBDatabaseDriverHSqlTest
{
    public static CompanyDB db;
    public static Connection conn;
    
    @BeforeClass
    public static void setup() throws ClassNotFoundException, SQLException{
        Class.forName("org.hsqldb.jdbcDriver");
        conn = DriverManager.getConnection("jdbc:hsqldb:"
                                           + "target/hsqldb-unit-test/",    // filenames
                                           "sa",                     // username
                                           "");                      // password
        DBDatabaseDriver driver = new DBDatabaseDriverHSql();
        db = new CompanyDB();
        db.open(driver, conn);
        DBSQLScript script = new DBSQLScript();
        db.getCreateDDLScript(db.getDriver(), script);
        script.run(db.getDriver(), conn, false);
        
        
    }
    
    @AfterClass
    public static void shutdown() throws SQLException{
        try{
            DBSQLScript script = new DBSQLScript();
            db.getDriver().getDDLScript(DBCmdType.DROP, db.EMPLOYEE, script);
            db.getDriver().getDDLScript(DBCmdType.DROP, db.DEPARTMENT, script);
            script.run(db.getDriver(), conn, true);
        }finally{
            DBTools.close(conn);
        }
    }
    
    @Test
    public void test(){
        DBRecord dep = new DBRecord();
        dep.create(db.DEPARTMENT);
        dep.setValue(db.DEPARTMENT.NAME, "junit");
        dep.setValue(db.DEPARTMENT.BUSINESS_UNIT, "testers");
        dep.update(conn);
        
        Date date = dep.getDateTime(db.DEPARTMENT.UPDATE_TIMESTAMP);
        assertNotNull(date);
        assertTrue(dep.getInt(db.DEPARTMENT.ID) > 0);
        
        
        DBRecord emp = new DBRecord();
        emp.create(db.EMPLOYEE);
        emp.setValue(db.EMPLOYEE.FIRSTNAME, "junit");
        emp.setValue(db.EMPLOYEE.LASTNAME, "test");
        emp.setValue(db.EMPLOYEE.GENDER, "m");
        emp.setValue(db.EMPLOYEE.DEPARTMENT_ID, dep.getInt(db.DEPARTMENT.ID));
        emp.update(conn);
        
        date = emp.getDateTime(db.EMPLOYEE.UPDATE_TIMESTAMP);
        assertNotNull(date);
        assertTrue(emp.getInt(db.EMPLOYEE.ID) > 0);

        int id = emp.getInt(db.EMPLOYEE.ID);
        
        // Update an Employee
        emp = new DBRecord();
        emp.read(db.EMPLOYEE, id, conn);
        // Set
        emp.setValue(db.EMPLOYEE.PHONE_NUMBER, "123456");
        emp.update(conn);
        
        emp = new DBRecord();
        emp.read(db.EMPLOYEE, id, conn);
        
        assertEquals("123456", emp.getString(db.EMPLOYEE.PHONE_NUMBER));
    }
}
