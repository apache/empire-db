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

import java.util.List;

public interface EmployeeDao {

	public List<Employee> getEmployees();

	public Integer createEmployee(Employee employee);

	public void updateEmployee(Employee employee);

	public Employee openEmployee(Integer id);

	public Employee findEmployee(String firstName, String lastName);

	public Department openDepartment(Integer id);

	public Department findDepartment(String name);

	public Integer createDepartment(Department department);

	public void updateDepartment(Department department);

	public void renameDepartment(Integer id, String name);

}
