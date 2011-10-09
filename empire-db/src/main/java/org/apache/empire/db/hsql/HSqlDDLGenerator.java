/*
 * ESTEAM Software GmbH, 09.10.2011
 */
package org.apache.empire.db.hsql;

import java.util.Iterator;

import org.apache.empire.data.DataType;
import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBDDLGenerator;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.DBSQLScript;
import org.apache.empire.db.DBTable;
import org.apache.empire.db.DBTableColumn;

public class HSqlDDLGenerator extends DBDDLGenerator<DBDatabaseDriverHSql>
{
    public HSqlDDLGenerator(DBDatabaseDriverHSql driver)
    {
        super(driver);
        // set Oracle specific data types
        initDataTypes();
    }

    /**
     * sets Oracle specific data types
     * @param driver
     */
    private void initDataTypes()
    {   // Override data types
        DATATYPE_CLOB       = "LONGVARCHAR";
        DATATYPE_BLOB       = "LONGVARBINARY";
    }

    /*
    @Override
    protected boolean appendColumnDataType(DataType type, double size, DBTableColumn c, StringBuilder sql)
    {
        switch (type)
        {
            default:
                // use default
                return super.appendColumnDataType(type, size, c, sql);
        }
        return true;
    }
    */
 
    @Override
    protected void createDatabase(DBDatabase db, DBSQLScript script)
    {
        // Create all Sequences
        Iterator<DBTable> seqtabs = db.getTables().iterator();
        while (seqtabs.hasNext())
        {
            DBTable table = seqtabs.next();
            Iterator<DBColumn> cols = table.getColumns().iterator();
            while (cols.hasNext())
            {
                DBTableColumn c = (DBTableColumn) cols.next();
                if (c.getDataType() == DataType.AUTOINC)
                {
                    createSequence(db, c, script);
                }
            }
        }
        // default processing
        super.createDatabase(db, script);
    }

    /**
     * Appends the DDL-Script for creating the a sequence to an SQL-Script<br/>
     * @param db the database to create
     * @param c the column for which to create the sequence
     * @param script the sql script to which to append the dll command(s)
     */
    protected void createSequence(DBDatabase db, DBTableColumn c, DBSQLScript script)
    {
        Object defValue = c.getDefaultValue();
        String seqName = (defValue != null) ? defValue.toString() : c.toString();
        // createSQL
        StringBuilder sql = new StringBuilder();
        sql.append("-- creating sequence for column ");
        sql.append(c.toString());
        sql.append(" --\r\n");
        sql.append("CREATE SEQUENCE ");
        db.appendQualifiedName(sql, seqName, detectQuoteName(seqName));
        sql.append(" START WITH 1");
        // executeDLL
        script.addStmt(sql);
    }

}