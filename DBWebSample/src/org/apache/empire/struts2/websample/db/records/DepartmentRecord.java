package org.apache.empire.struts2.websample.db.records;

import org.apache.empire.struts2.websample.db.SampleDB;
import org.apache.empire.struts2.websample.db.SampleRecord;
import org.apache.empire.struts2.websample.web.SampleContext;

public class DepartmentRecord extends SampleRecord
{
    public static final SampleDB.Departments T = SampleDB.getInstance().T_DEPARTMENTS;  

    // Department Record
    public DepartmentRecord(SampleContext context)
    {
        super(context);
    }
}
