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
package org.apache.empire.spring.example2;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.empire.db.DBCommand;
import org.apache.empire.db.DBJoinType;
import org.apache.empire.db.DBRecord;
import org.apache.empire.db.DBRecordData;
import org.apache.empire.spring.EmpireDaoSupport;
import org.apache.empire.spring.DBRecordMapper;
import org.apache.empire.spring.EmpireRecord;
import org.apache.empire.spring.DBRecordWriter;
import org.apache.empire.spring.example1.SampleDB;
import org.apache.empire.spring.example1.SampleDB.Departments;
import org.apache.empire.spring.example1.SampleDB.Employees;
import org.springframework.transaction.annotation.Transactional;

public class EmployeeDaoImpl extends EmpireDaoSupport implements EmployeeDao {

	private Employees EMPLOYEES;
	private Departments DEPARTMENTS;

	@Override
	protected void initEmpireDao() {
		SampleDB db = getDatabase();
		this.EMPLOYEES = db.EMPLOYEES;
		this.DEPARTMENTS = db.DEPARTMENTS;
	}

	private DBCommand createEmployeeSelectCommand() {
		DBCommand cmd = getDatabase().createCommand();
		cmd.select(EMPLOYEES.getColumns());
		cmd.select(DEPARTMENTS.getColumns());

		cmd.join(EMPLOYEES.DEPARTMENT_ID, DEPARTMENTS.DEPARTMENT_ID, DBJoinType.INNER);
		return cmd;
	}

	private DBCommand createDepartmentSelectCommand() {
		DBCommand cmd = getDatabase().createCommand();
		cmd.select(DEPARTMENTS.getColumns());
		return cmd;
	}

	@Transactional(readOnly = true)
	public Employee openEmployee(Integer id) {
		DBCommand cmd = createEmployeeSelectCommand();
		cmd.where(EMPLOYEES.EMPLOYEE_ID.is(id));
		return getEmpireTemplate().queryForObject(cmd, new EmployeeMapper());
	}

	@Transactional(readOnly = true)
	public Employee findEmployee(String firstName, String lastName) {
		DBCommand cmd = createEmployeeSelectCommand();
		cmd.where(EMPLOYEES.FIRSTNAME.is(firstName));
		cmd.where(EMPLOYEES.LASTNAME.is(lastName));
		return getEmpireTemplate().queryForObject(cmd, new EmployeeMapper());
	}
	
	@Transactional(readOnly = true)
	public Department openDepartment(Integer id) {
		DBCommand cmd = createDepartmentSelectCommand();
		cmd.where(DEPARTMENTS.DEPARTMENT_ID.is(id));
		return getEmpireTemplate().queryForBean(cmd, Department.class);
	}

	@Transactional(readOnly = true)
	public Department findDepartment(String name) {
		DBCommand cmd = createDepartmentSelectCommand();
		cmd.where(DEPARTMENTS.NAME.is(name));
		return getEmpireTemplate().queryForBean(cmd, Department.class);
	}

	@Transactional(readOnly = true)
	public List<Department> getDepartments() {
		DBCommand cmd = createEmployeeSelectCommand();
		return getEmpireTemplate().queryForBeanList(cmd, Department.class);
	}

	
	@Transactional
	public void renameDepartment(Integer id, String name) {
		DBCommand cmd = getDatabase().createCommand();
		cmd.where(DEPARTMENTS.DEPARTMENT_ID.is(id));
		cmd.set(DEPARTMENTS.NAME.to(name));
		getEmpireTemplate().executeUpdate(cmd);
	}

	@Transactional(readOnly = true)
	public List<Employee> getEmployees() {
		DBCommand cmd = createEmployeeSelectCommand();
		return getEmpireTemplate().query(cmd, new EmployeeMapper());
	}

	@Transactional
	public Integer createEmployee(Employee employee) {
		DBRecord record = new EmpireRecord();
		record.create(EMPLOYEES);
		new EmployeeWriter().write(record, employee);
		getEmpireTemplate().updateRecord(record);
		return record.getInt(EMPLOYEES.EMPLOYEE_ID);
	}

	@Transactional
	public void updateEmployee(Employee employee) {
		DBRecord record = getEmpireTemplate().openRecord(EMPLOYEES, employee.getEmployeeId());
		new EmployeeWriter().write(record, employee);
		getEmpireTemplate().updateRecord(record);
	}

	@Transactional
	public Integer createDepartment(Department department) {
		DBRecord record = new EmpireRecord();
		record.create(DEPARTMENTS);
		new DepartmentWriter().write(record, department);
		getEmpireTemplate().updateRecord(record);
		return record.getInt(DEPARTMENTS.DEPARTMENT_ID);
	}

	@Transactional
	public void updateDepartment(Department department) {
		DBRecord record = getEmpireTemplate().openRecord(DEPARTMENTS, department.getDepartmentId());
		new DepartmentWriter().write(record, department);
		getEmpireTemplate().updateRecord(record);
	}

	private class EmployeeMapper implements DBRecordMapper<Employee> {

		DepartmentMapper departmentMapper = new DepartmentMapper();

        @Override
		public Employee read(DBRecordData record) {
			Employee result = new Employee();
            // Auto-copy all properties
			record.getBeanProperties(result);
			/*
			result.setEmployeeId(record.getInt(EMPLOYEES.EMPLOYEE_ID));
			result.setFirstName(record.getString(EMPLOYEES.FIRSTNAME));
			result.setLastName(record.getString(EMPLOYEES.LASTNAME));
			result.setGender(Employee.Gender.valueOf(record.getString(EMPLOYEES.GENDER)));
			result.setPhoneNumber(record.getString(EMPLOYEES.PHONE_NUMBER));
			*/
			result.setDepartment(departmentMapper.read(record));
			return result;
		}

	}

	private class EmployeeWriter implements DBRecordWriter<Employee> {

        @Override
		public void write(DBRecord record, Employee entity) {
			// Auto-copy all properties
		    record.setBeanValues(entity);
			/*
		    record.setValue(EMPLOYEES.EMPLOYEE_ID, entity.getEmployeeId());
			record.setValue(EMPLOYEES.FIRSTNAME, entity.getFirstName());
			record.setValue(EMPLOYEES.LASTNAME, entity.getLastName());
			record.setValue(EMPLOYEES.GENDER, entity.getGender().name());
			record.setValue(EMPLOYEES.PHONE_NUMBER, entity.getPhoneNumber());
			*/
			record.setValue(EMPLOYEES.DEPARTMENT_ID, entity.getDepartment().getDepartmentId());
		}

	}

	private class DepartmentMapper implements DBRecordMapper<Department> {

		// reader cache, in case of joined resultset the same object is returned

		Map<Integer, Department> cache = new HashMap<Integer, Department>();

        @Override
		public Department read(DBRecordData record) {

			Integer id = record.getInt(DEPARTMENTS.DEPARTMENT_ID);

			Department department = cache.get(id);
			if (department == null) {
				department = new Department();
                // Auto-copy all properties
				record.getBeanProperties(department);
				/*
				department.setDepartmentId(id);
				department.setName(record.getString(DEPARTMENTS.NAME));
				department.setHead(record.getString(DEPARTMENTS.HEAD));
				department.setBusinessUnit(record.getString(DEPARTMENTS.BUSINESS_UNIT));
				*/		
				cache.put(id, department);
			}
			return department;
		}

	}

	private class DepartmentWriter implements DBRecordWriter<Department> {

		@Override
        public void write(DBRecord record, Department entity) {
            // Auto-copy all properties
		    record.setBeanValues(entity);
		    /*
			record.setValue(DEPARTMENTS.DEPARTMENT_ID, entity.getDepartmentId());
			record.setValue(DEPARTMENTS.NAME, entity.getName());
			record.setValue(DEPARTMENTS.HEAD, entity.getHead());
			record.setValue(DEPARTMENTS.BUSINESS_UNIT, entity.getBusinessUnit());
			*/
		}

	}

}
