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
package org.apache.empire.samples.spring;

import java.sql.Connection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBColumnExpr;
import org.apache.empire.db.DBCommand;
import org.apache.empire.db.DBReader;
import org.apache.empire.db.DBRecord;
import org.apache.empire.db.DBTable;
import org.apache.empire.db.derby.DBDatabaseDriverDerby;
import org.apache.empire.db.h2.DBDatabaseDriverH2;
import org.apache.empire.db.hsql.DBDatabaseDriverHSql;
import org.apache.empire.db.postgresql.DBDatabaseDriverPostgreSQL;
import org.apache.empire.samples.spring.db.SampleBean;
import org.apache.empire.samples.spring.db.SampleDB;
import org.apache.empire.samples.spring.support.EmpireDaoSupport;
import org.apache.empire.xml.XMLWriter;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;

/**
 *
 */
public class EmpireAppImpl extends EmpireDaoSupport implements EmpireApp {


    @Transactional
    public void clearDatabase() {
        SampleDB db = getDB();
        Connection conn = getConnection();

        DBCommand cmd = db.createCommand();
        // Delete all Employees (no constraints)
        db.executeSQL(cmd.getDelete(db.EMPLOYEES), conn);
        // Delete all Departments (no constraints)
        db.executeSQL(cmd.getDelete(db.DEPARTMENTS), conn);
    }

    @Transactional
    public Integer insertDepartment(String departmentName, String businessUnit) {
        SampleDB db = getDB();
        Connection conn = getConnection();

        DBRecord rec = new DBRecord();
        rec.create(db.DEPARTMENTS);
        rec.setValue(db.DEPARTMENTS.NAME, departmentName);
        rec.setValue(db.DEPARTMENTS.BUSINESS_UNIT, businessUnit);
        rec.update(conn);
        // Return Department ID
        return rec.getInt(db.DEPARTMENTS.DEPARTMENT_ID);
    }

    @Transactional
    public Integer insertEmployee(String firstName, String lastName, String gender, int departmentId) {
        SampleDB db = getDB();
        Connection conn = getConnection();

        DBRecord rec = new DBRecord();
        rec.create(db.EMPLOYEES);
        rec.setValue(db.EMPLOYEES.FIRSTNAME, firstName);
        rec.setValue(db.EMPLOYEES.LASTNAME, lastName);
        rec.setValue(db.EMPLOYEES.GENDER, gender);
        rec.setValue(db.EMPLOYEES.DEPARTMENT_ID, departmentId);
        rec.update(conn);
        // Return Employee ID
        return rec.getInt(db.EMPLOYEES.EMPLOYEE_ID);
    }

    @Transactional
    public void updateEmployee(int idPers, String phoneNumber) {
        if (getEmployee(idPers) == null) {
            //if you like more verbose exceptions for the app
            throw new IllegalArgumentException("The specified employee does not exist.");
        }

        SampleDB db = getDB();
        Connection conn = getConnection();

        DBRecord rec = new DBRecord();
		rec.read(db.EMPLOYEES, idPers, conn);
		// Set
		rec.setValue(db.EMPLOYEES.PHONE_NUMBER, phoneNumber);
		rec.update(conn);
    }

    @Transactional
    public void setupDatabase() {
        initializeDatabase();
    }

    @Transactional(readOnly = true)
    public void doQuery(QueryType type) {
        SampleDB db = getDB();
        Connection conn = getConnection();

	    // Define the query
	    DBCommand cmd = db.createCommand();
	    // Define shortcuts for tables used - not necessary but convenient
	    SampleDB.Employees   EMP = db.EMPLOYEES;
	    SampleDB.Departments DEP = db.DEPARTMENTS;

	    // The following expression concats lastname + ', ' + firstname
        DBColumnExpr EMPLOYEE_FULLNAME = EMP.LASTNAME.append(", ").append(EMP.FIRSTNAME).as("FULL_NAME");

        // The following expression extracts the extension number from the phone field
        // e.g. substr(PHONE_NUMBER, length(PHONE_NUMBER)-instr(reverse(PHONE_NUMBER), '-')+2) AS PHONE_EXTENSION
        // Hint: Since the reverse() function is not supported by HSQLDB there is special treatment for HSQL
        DBColumnExpr PHONE_LAST_DASH;
        if ( db.getDriver() instanceof DBDatabaseDriverHSql
        		|| db.getDriver() instanceof DBDatabaseDriverDerby
        		|| db.getDriver() instanceof DBDatabaseDriverH2)
             PHONE_LAST_DASH = EMP.PHONE_NUMBER.indexOf("-", EMP.PHONE_NUMBER.indexOf("-").plus(1)).plus(1); // HSQLDB only
        else PHONE_LAST_DASH = EMP.PHONE_NUMBER.length().minus(EMP.PHONE_NUMBER.reverse().indexOf("-")).plus(2);
        DBColumnExpr PHONE_EXT_NUMBER = EMP.PHONE_NUMBER.substring(PHONE_LAST_DASH).as("PHONE_EXTENSION");

        // DBColumnExpr genderExpr = cmd.select(EMP.GENDER.decode(EMP.GENDER.getOptions()).as(EMP.GENDER.getName()));
		// Select required columns
		cmd.select(EMP.EMPLOYEE_ID, EMPLOYEE_FULLNAME);
		if(db.getDriver() instanceof DBDatabaseDriverPostgreSQL)
		{
			// postgres does not support the substring expression
			cmd.select(EMP.GENDER, EMP.PHONE_NUMBER);
		}else{
			cmd.select(EMP.GENDER, EMP.PHONE_NUMBER, PHONE_EXT_NUMBER);

		}
		cmd.select(DEP.NAME.as("DEPARTMENT"));
		cmd.select(DEP.BUSINESS_UNIT);
		cmd.join(EMP.DEPARTMENT_ID, DEP.DEPARTMENT_ID);
        // Set constraints and order
        cmd.where(EMP.LASTNAME.length().isGreaterThan(0));
        cmd.orderBy(EMP.LASTNAME, EMP.FIRSTNAME);

		// Query Records and print output
		DBReader reader = new DBReader();
		try
        {
			// Open Reader
			System.out.println("Running Query:");
			System.out.println(cmd.getSelect());
			reader.open(cmd, conn);
			// Print output
			System.out.println("---------------------------------");
			switch(type)
			{
			    case BeanList:
			        // Text-Output by iterating through all records.
	                while (reader.moveNext())
                    {
	                    System.out.println(reader.getString(EMP.EMPLOYEE_ID)
	                            + "\t" + reader.getString(EMPLOYEE_FULLNAME)
	                            + "\t" + EMP.GENDER.getOptions().get(reader.getString(EMP.GENDER))
                                + "\t" + reader.getString(PHONE_EXT_NUMBER)
	                            + "\t" + reader.getString(DEP.NAME));
	                }
			        break;
                case Reader:
                    // Text-Output using a list of Java Beans supplied by the DBReader
                    List<SampleBean> beanList = reader.getBeanList(SampleBean.class);
                    System.out.println(String.valueOf(beanList.size()) + " SampleBeans returned from Query.");
                    for (SampleBean b : beanList)
                    {
                        System.out.println(b.toString());
                    }
                    break;
                case XmlDocument:
                    // XML Output
                    Document doc = reader.getXmlDocument();
                    // Print XML Document to System.out
                    XMLWriter.debug(doc);
                    break;
			}

		} finally
        {
			// always close Reader
			reader.close();
		}
    }


    public Map<Object, Object> getDepartment(int id) {
        SampleDB db = getDB();
        return get(db.DEPARTMENTS, id);
    }

    public Map<Object, Object> getEmployee(int id) {
        SampleDB db = getDB();
        return get(db.EMPLOYEES, id);
    }

    private Map<Object, Object> get(DBTable table, int pk) {
        Connection conn = getConnection();

        DBCommand cmd = db.createCommand();
        cmd.select(table.getColumns());
        cmd.where(table.getPrimaryKey().getColumns()[0].is(pk)); //i know there is just one pk-column ;-)
        DBReader reader = openReader(cmd, conn);
        Map<Object, Object> dep = null;
        if (reader.moveNext()) {
            dep = new HashMap<Object, Object>();
            for (DBColumn col : table.getColumns()) {
                dep.put(col.getName(), reader.getValue(col));
            }
        }
        reader.close();
        return dep;
    }
}
