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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import org.apache.empire.db.DBColumnExpr;
import org.apache.empire.db.DBCommand;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.DBDatabaseDriver;
import org.apache.empire.db.DBSQLScript;
import org.apache.empire.db.DBTable;
import org.apache.empire.spring.EmpireTemplate;
import org.apache.empire.spring.example1.SampleDB;
import org.apache.empire.spring.example2.Employee.Gender;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;

/**
 * 
 */
public class EmployeeSpringApp {
    private static final Logger log = Logger.getLogger(EmployeeSpringApp.class.getName());

    //creates the application context
    //this is usually in some bootstrapping code; so your application will
    //just have one at runtime.
    static ApplicationContext ctx = getContext();

    //get the service that is the entry point into the application
    //normally this is injected by spring into classes that need it
    static EmployeeDao employeeDao = ctx.getBean("employeeDao", EmployeeDao.class);
    static SampleDB sampleDb = ctx.getBean("sampleDb", SampleDB.class);

    public static void main(String[] args) throws Exception {

        System.out.println("Running Spring Example...");

        setupDatabase();
        clearDatabase();
        
        Department depDevelopment, depSales;
        
        System.out.println("*** Create Departments ***");
        
        {
        	depDevelopment = new Department();
        	depDevelopment.setName("Development");
        	depDevelopment.setBusinessUnit("ITTK");
        	Integer id = employeeDao.createDepartment(depDevelopment);
        	depDevelopment = employeeDao.openDepartment(id);
        }
        {
        	depSales = new Department();
        	depSales.setName("Sales");
        	depSales.setBusinessUnit("ITTK");
        	Integer id = employeeDao.createDepartment(depSales);
        	depSales = employeeDao.openDepartment(id);
        }
        
        System.out.println("*** Create Employees ***");

        Employee peter = new Employee();
        peter.setFirstName("Peter");
        peter.setLastName("Sharp");
        peter.setGender(Gender.M);
        peter.setDepartment(depDevelopment);
        
        Integer peterId = employeeDao.createEmployee(peter);
        peter = employeeDao.openEmployee(peterId);
        
        
        
        Employee fred = new Employee();
        fred.setFirstName("Fred");
        fred.setLastName("Bloggs");
        fred.setGender(Gender.M);
        fred.setDepartment(depDevelopment);

        Integer fredId = employeeDao.createEmployee(fred);
        fred = employeeDao.openEmployee(fredId);

        
        Employee emma = new Employee();
        emma.setFirstName("Emma");
        emma.setLastName("White");
        emma.setGender(Gender.F);
        emma.setDepartment(depSales);
        
        Integer emmaId = employeeDao.createEmployee(emma);
        emma = employeeDao.openEmployee(emmaId);
        

        System.out.println("*** updateEmployees ***");
        
        peter.setPhoneNumber("+49-7531-457160");
        employeeDao.updateEmployee(peter);
        
        fred.setPhoneNumber("+49-5555-505050");
        employeeDao.updateEmployee(fred);
        
        emma.setPhoneNumber("+49-040-125486");
        employeeDao.updateEmployee(emma);

        System.out.println("*** List employees ***");
        
        List<Employee> employees = employeeDao.getEmployees();
        for (Iterator<Employee> iterator = employees.iterator(); iterator.hasNext();) {
			Employee employee = iterator.next();
			System.out.println(employee);
		}
        
        System.out.println("*** List departments ***");

        List<Department> departments = employeeDao.getDepartments();
        for (Iterator<Department> iterator = departments.iterator(); iterator.hasNext();) {
			Department department = iterator.next();
			System.out.println(department);
		}

    }

    private static void clearDatabase() {
        // Delete all Employees (no constraints)
        
        EmpireTemplate empireTemplate = ctx.getBean("empireTemplate", EmpireTemplate.class);
        empireTemplate.executeDelete(sampleDb.EMPLOYEES, sampleDb.createCommand());
        empireTemplate.executeDelete(sampleDb.DEPARTMENTS, sampleDb.createCommand());
		
	}




	public static void setupDatabase() {
		if (!databaseExists()) {
			createDatabase();
		}
	}
	

	public static boolean databaseExists() {
		try {
			DBDatabase db = sampleDb;
			if (db.getTables() == null || db.getTables().isEmpty()) {
				throw new AssertionError(
						"There are no tables in this database!");
			}
			DBCommand cmd = db.createCommand();
			if (cmd == null) {
				throw new AssertionError("The DBCommand object is null.");
			}
			DBTable t = db.getTables().get(0);
			DBColumnExpr COUNT = t.count();
			
			cmd.select(COUNT);
			
			EmpireTemplate empireTemplate = ctx.getBean("empireTemplate", EmpireTemplate.class);
			return (empireTemplate.queryForInteger(cmd, COUNT, -1) >= 0);
		} catch (Exception e) {
			return false;
		}
	}


	private static void createDatabase() {

		// create DLL for Database Definition
		final DBSQLScript script = new DBSQLScript();
		final DBDatabaseDriver driver = sampleDb.getDriver();
		sampleDb.getCreateDDLScript(driver, script);

		// Show DLL Statement
		System.out.println(script.toString());
		// Execute Script
		EmpireTemplate empireTemplate = ctx.getBean("empireTemplate", EmpireTemplate.class);
		empireTemplate.execute(new ConnectionCallback<Object>() {

			@Override
            public Object doInConnection(Connection con) throws SQLException,
					DataAccessException {
        				script.executeAll(driver, con, false);
        				return null;
        			}
		});

	}



	static GenericApplicationContext getContext() {
        log.info("Creating Spring Application Context ...");
        GenericApplicationContext ctx = new GenericApplicationContext();
        XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(ctx);
        reader.loadBeanDefinitions(new ClassPathResource("/example2/applicationContext-employee.xml"));

        ctx.refresh();
        return ctx;
    }
    
    
    
}
