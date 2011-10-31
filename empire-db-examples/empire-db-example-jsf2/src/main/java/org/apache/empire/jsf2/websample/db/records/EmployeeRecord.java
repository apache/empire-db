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
package org.apache.empire.jsf2.websample.db.records;

import org.apache.empire.commons.Options;
import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBCommand;
import org.apache.empire.db.DBRecord;
import org.apache.empire.jsf2.websample.db.SampleDB;
import org.apache.empire.jsf2.websample.web.FacesUtils;


public class EmployeeRecord extends DBRecord
{
    private final static long serialVersionUID = 1L;
  
    public static final SampleDB.Employees T = FacesUtils.getDatabase().T_EMPLOYEES;  
 
    // Sample Implementation for Department Record
    public DepartmentRecord getDepartmentRecord()
    {
        DepartmentRecord rec = new DepartmentRecord();
        SampleDB.Departments table = FacesUtils.getDatabase().T_DEPARTMENTS;
        try {
			rec.read(table, this.getInt(T.C_DEPARTMENT_ID), FacesUtils.getConnection());
		} catch (Exception e) {
	        log.error("Unable to get department record. Message is " + e.getMessage());
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
            return db.queryOptionList(cmd.getSelect(), FacesUtils.getConnection());
        }
        // base class implementation
        return super.getFieldOptions(column);
    }
    
}
