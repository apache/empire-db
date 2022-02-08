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
package org.apache.empire.samples.db.beans;

import java.util.List;

import org.apache.empire.db.DBCommand;
import org.apache.empire.db.DBContext;
import org.apache.empire.db.list.DataBean;
import org.apache.empire.samples.db.SampleDB;

public class Department implements DataBean<SampleDB>
{
    private long   id;     // "ID" 
    private String name;   // "FIRSTNAME"
    private String head;   // "FIRSTNAME"
    
    private List<Employee> employees;
    
    public long getId()
    {
        return id;
    }
    public void setId(long id)
    {
        this.id = id;
    }
    public String getName()
    {
        return name;
    }
    public void setName(String name)
    {
        this.name = name;
    }
    public String getHead()
    {
        return head;
    }
    public void setHead(String head)
    {
        this.head = head;
    }

    public List<Employee> getEmployees()
    {
        return employees;
    }
    
    @Override
    public void initialize(SampleDB db, DBContext context, int rownum, Object parent)
    {
        DBCommand cmd = db.createCommand();
        cmd.where(db.EMPLOYEES.DEPARTMENT_ID.is(this.id));
        cmd.orderBy(db.EMPLOYEES.FIRST_NAME, db.EMPLOYEES.LAST_NAME);
        employees = context.getUtils().queryBeanList(cmd, Employee.class, db.EMPLOYEES, this);
    }
    
}
