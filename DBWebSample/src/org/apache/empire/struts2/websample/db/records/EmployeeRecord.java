package org.apache.empire.struts2.websample.db.records;

import org.apache.empire.commons.Options;
import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBCommand;
import org.apache.empire.struts2.websample.db.SampleDB;
import org.apache.empire.struts2.websample.db.SampleRecord;
import org.apache.empire.struts2.websample.web.SampleContext;


public class EmployeeRecord extends SampleRecord
{
    public static final SampleDB.Employees T = SampleDB.getInstance().T_EMPLOYEES;  
 
    /*
     * Constructor
     */
    public EmployeeRecord(SampleContext context)
    {
        super(context);
    }
    
    // Sample Implementation for Department Record
    public DepartmentRecord getDepartmentRecord()
    {
        DepartmentRecord rec = new DepartmentRecord(context);
        SampleDB.Departments table = SampleDB.getInstance().T_DEPARTMENTS;
        if (!rec.read(table, this.getInt(T.C_DEPARTMENT_ID), context.getConnection())) {
            log.error("Unable to get department record. Message is " + rec.getErrorMessage());
            return null;
        }
        return rec; 
    }
    
    @Override
    public Options getFieldOptions(DBColumn column)
    {
        if (column.equals(T.C_DEPARTMENT_ID)) {
            SampleDB db = (SampleDB)getDatabase();
            DBCommand cmd = db.createCommand();
            cmd.select(db.T_DEPARTMENTS.C_DEPARTMENT_ID);
            cmd.select(db.T_DEPARTMENTS.C_NAME);
            return db.queryOptionList(cmd.getSelect(), context.getConnection());
        }
        // base class implementation
        return super.getFieldOptions(column);
    }
    
}
