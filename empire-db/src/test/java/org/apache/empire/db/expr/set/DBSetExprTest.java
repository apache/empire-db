package org.apache.empire.db.expr.set;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Set;

import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBExpr;
import org.apache.empire.db.MockDriver;
import org.apache.empire.db.TestDB;
import org.junit.Before;
import org.junit.Test;

public class DBSetExprTest
{
    
    private DBSetExpr expr;
    private TestDB testDB;
    
    @Before
    public void setup(){
        testDB = new TestDB();
        testDB.open(new MockDriver(), null);
        expr = new DBSetExpr(testDB.EMPLOYEE.FIRSTNAME, "JUnit");
    }

    @Test
    public void testGetDatabase()
    {
        assertEquals(testDB, expr.getDatabase());
    }

    @Test
    public void testAddSQL()
    {
        StringBuilder builder = new StringBuilder();
        expr.addSQL(builder, 0);
        assertEquals("", builder.toString());
        expr.addSQL(builder, DBExpr.CTX_DEFAULT);
        assertEquals("FIRSTNAME='JUnit'", builder.toString());
    }

    @Test
    public void testAddReferencedColumns()
    {
        Set<DBColumn> cols = new HashSet<DBColumn>();
        expr.addReferencedColumns(cols);
        assertTrue(cols.contains(testDB.EMPLOYEE.FIRSTNAME));
    }

    @Test
    public void testGetTable()
    {
        assertEquals(testDB.EMPLOYEE, expr.getTable());
    }

    @Test
    public void testGetColumn()
    {
        assertEquals(testDB.EMPLOYEE.FIRSTNAME, expr.getColumn());
    }

    @Test
    public void testGetValue()
    {
        assertEquals("JUnit", expr.getValue());
    }

    @Test
    public void testSetValue()
    {
        expr.setValue("JUnit2");
        assertEquals("JUnit2", expr.getValue());
    }

}
