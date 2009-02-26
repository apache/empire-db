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
package org.apache.empire.struts2.websample.ws.records;

import java.util.List;

import org.apache.empire.commons.OptionEntry;
import org.apache.empire.commons.Options;
import org.apache.empire.data.Column;
import org.apache.empire.samples.cxf.wssample.common.Department;
import org.apache.empire.samples.cxf.wssample.common.Employee;
import org.apache.empire.struts2.websample.web.SampleContext;
import org.apache.empire.struts2.websample.ws.SampleBeanDomain;
import org.apache.empire.struts2.websample.ws.SampleRecord;


public class EmployeeRecord extends SampleRecord<Employee>
{
    public static final SampleBeanDomain.Employees T = SampleBeanDomain.getInstance().T_EMPLOYEES;  
 
    // Department Record
    public EmployeeRecord(SampleContext context)
    {
        super(context, new Employee(), T);
    }

    @Override
    public Options getFieldOptions(Column column) {
    	
    	Options opts = null;
    	if(column.equals(T.C_DEPARTMENT_ID))
    	{
    		opts = getDepartments(context);
    	}
    	else
    	{
    		opts=super.getFieldOptions(column);;
    	}
    	return opts;
    }
    
    public static Options getDepartments(SampleContext context)
    {
    	Options opts = new Options();
    	List<Department> _deps = context.getWebserviceProxy().getDepartments();
    	for(Department d: _deps)
    	{
    		opts.add(new OptionEntry(d.getDepartmentId(),d.getName()));
    	}
    	return opts;
    }
    
    @Override
    public boolean isNew()
    {
        return getBean().isNew();
    }
    
    @Override
    public boolean isValid()
    {   

        return super.isValid() && (getBean().getEmployeeId()!=-1 || getBean().isNew());
    }
    
}
