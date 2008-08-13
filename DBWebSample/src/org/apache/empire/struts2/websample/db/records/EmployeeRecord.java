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
