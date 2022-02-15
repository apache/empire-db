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
package org.apache.empire.vue.sample.db.records;

import org.apache.empire.commons.Options;
import org.apache.empire.data.Column;
import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBCommand;
import org.apache.empire.vue.sample.db.RecordContext;
import org.apache.empire.vue.sample.db.SampleDB;
import org.apache.empire.vue.sample.db.SampleDB.TEmployees;

public class EmployeeRecord extends SampleRecord<TEmployees>
{
    private static final long serialVersionUID = 1L;

    public EmployeeRecord(RecordContext recordContext)
    {
        super(recordContext.getDb().T_EMPLOYEES, recordContext);
    }
    
	/*
	 * Add some business logic:
	 * Make all fields read only if employee is retired (except the retired field itself!) 
	 */
    @Override
    public boolean isFieldReadOnly(Column column)
    {
    	if (column!=T.RETIRED && getBoolean(T.RETIRED))
    	{	/* Employee is retired */
    		return true;
    	}	
    	return super.isFieldReadOnly(column);	
    }

    @Override
    public Options getFieldOptions(DBColumn column)
    {
        if (column.equals(T.DEPARTMENT_ID))
        {
            SampleDB db = (SampleDB) getDatabase();
            DBCommand cmd = context.createCommand();
            cmd.select(db.T_DEPARTMENTS.ID);
            cmd.select(db.T_DEPARTMENTS.NAME);
            cmd.orderBy(db.T_DEPARTMENTS.NAME);
            return recordContext.getUtils().queryOptionList(cmd);
        }
        // base class implementation
        return super.getFieldOptions(column);
    }

    // Sample Implementation for Department Record
    public DepartmentRecord getDepartmentRecord()
    {
        DepartmentRecord rec = new DepartmentRecord(recordContext);
        rec.read(this.getInt(T.DEPARTMENT_ID));
        return rec;
    }

}
