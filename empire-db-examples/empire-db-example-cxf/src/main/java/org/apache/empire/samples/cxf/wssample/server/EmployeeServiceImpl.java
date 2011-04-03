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

package org.apache.empire.samples.cxf.wssample.server;

import java.sql.Connection;
import java.util.List;

import javax.jws.WebService;

import org.apache.empire.db.DBCommand;
import org.apache.empire.db.DBReader;
import org.apache.empire.db.DBRecord;
import org.apache.empire.db.expr.compare.DBCompareExpr;
import org.apache.empire.samples.cxf.wssample.common.Department;
import org.apache.empire.samples.cxf.wssample.common.Employee;
import org.apache.empire.samples.cxf.wssample.common.EmployeeService;
import org.apache.empire.samples.cxf.wssample.server.db.SampleDB;
import org.apache.empire.samples.cxf.wssample.server.db.SampleDB.Departments;
import org.apache.empire.samples.cxf.wssample.server.db.SampleDB.Employees;

@WebService(endpointInterface = "org.apache.empire.samples.cxf.wssample.common.EmployeeService",
            portName = "EmployeeServicePort",
            serviceName = "EmployeeService")
            
public class EmployeeServiceImpl implements EmployeeService
{

    private SampleDB    db    = null;
    private Departments T_DEP = null;
    private Employees   T_EMP = null;
    private Connection  conn  = null;

    public EmployeeServiceImpl(SampleDB db, Connection conn)
    {
        this.conn = conn;
        this.db = db;
        T_DEP = db.DEPARTMENTS;
        T_EMP = db.EMPLOYEES;
    }

    public void saveEmmployee(javax.xml.ws.Holder<Employee> empHolder)
    {
        DBRecord r = new DBRecord();
        Employee emp = empHolder.value;
        if (emp.isNew())
            r.create(T_EMP, conn);
        else
            r.read(T_EMP, emp.getEmployeeId(), conn);

        r.setBeanValues(emp);
        r.update(conn);


            r.getBeanProperties(emp);
            emp.setNew(false);
    }

    public List<Employee> searchEmmployee(Integer id, String firstName, String lastName, Integer department)
    {
        DBCommand cmd = db.createCommand();
        cmd.select(T_EMP.getColumns());

        DBCompareExpr comp;

        if (id != null)
            comp = T_EMP.EMPLOYEE_ID.is(id);
        else
            comp = T_EMP.EMPLOYEE_ID.isNot(null);

        if (firstName != null && !firstName.equals(""))
        {
            comp = comp.and(T_EMP.FIRSTNAME.like(firstName));
        }

        if (lastName != null && !lastName.equals(""))
        {
            comp = comp.and(T_EMP.LASTNAME.like(lastName));
        }

        if (department != null)
        {
            comp = comp.and(T_EMP.DEPARTMENT_ID.is(department));
        }

        cmd.where(comp);

        DBReader reader = new DBReader();
        reader.open(cmd, conn);
        List<Employee> lst = reader.getBeanList(Employee.class);
        return lst;
    }

    public List<Department> getDepartments()
    {
        DBCommand cmd = db.createCommand();
        cmd.select(T_DEP.getColumns());

        DBReader reader = new DBReader();
        reader.open(cmd, conn);
        return reader.getBeanList(Department.class);
    }

    public Employee createEmmployee()
    {
        DBRecord r = new DBRecord();
        Employee emp = new Employee();

        // null, so that no IDs are wasted.
        r.create(T_EMP, null);
        r.getBeanProperties(emp);

        emp.setNew(true);

        return emp;
    }

    public void deleteEmmployee(int id)
    {
        T_EMP.deleteRecord(id, conn);
    }

    public Employee getEmmployee(int id)
    {
        DBRecord r = new DBRecord();
        Employee emp = new Employee();

        r.read(T_EMP, id, conn);
        r.getBeanProperties(emp);

        return emp;
    }

    public boolean ping()
    {
        return true;
    }
}
