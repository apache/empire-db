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
package org.apache.empire.jsf2.websample.web.pages;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Date;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.commons.Options;
import org.apache.empire.data.DataType;
import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBColumnExpr;
import org.apache.empire.db.DBCommand;
import org.apache.empire.exceptions.BeanPropertyGetException;
import org.apache.empire.jsf2.pageelements.BeanListPageElement;
import org.apache.empire.jsf2.pageelements.ListPageElement;
import org.apache.empire.jsf2.pageelements.ListPageElement.ParameterizedItem;
import org.apache.empire.jsf2.websample.db.SampleDB;
import org.apache.empire.jsf2.websample.db.SampleDB.TDepartments;
import org.apache.empire.jsf2.websample.db.SampleDB.TEmployees;
import org.apache.empire.jsf2.websample.web.SampleUtils;
import org.apache.empire.jsf2.websample.web.objects.EmployeeSearchFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmployeeListPage extends SamplePage
{
    private static final long                      serialVersionUID  = 1L;
    private static final Logger                    log               = LoggerFactory.getLogger(EmployeeListPage.class);

    
    private BeanListPageElement<EmployeeListEntry> employees;
    
    public static class EmployeeListEntry extends ListPageElement.SelectableItem implements ParameterizedItem
    {
        private static final long serialVersionUID = 1L;

        private int               employeeId;
        private String            name;
        private String            gender;
        private Date              dateOfBirth;
        private String            department;
        private boolean	 	      retired;
        private String            idParam;

        /**
         * Implements ParameterizedItem.
         * Used to uniquely identify this entry for selection and navigation 
         */
        @Override
        public String getIdParam()
        {
            return this.idParam;
        }

        /**
         * Implements ParameterizedItem.
         * This will automatically set the item idParam for navigation 
         */
        @Override
        public void setIdParam(String idParam)
        {
            this.idParam = idParam;
        }

        public int getEmployeeId()
        {
            return employeeId;
        }

        public void setEmployeeId(int employeeId)
        {
            this.employeeId = employeeId;
        }

        public String getName()
        {
            return name;
        }

        public void setName(String name)
        {
            this.name = name;
        }

        public String getGender()
        {
            return gender;
        }

        public void setGender(String gender)
        {
            this.gender = gender;
        }

        public Date getDateOfBirth()
        {
            return dateOfBirth;
        }

        public void setDateOfBirth(Date dateOfBirth)
        {
            this.dateOfBirth = dateOfBirth;
        }

        public String getDepartment()
        {
            return department;
        }

        public void setDepartment(String department)
        {
            this.department = department;
        }

		public boolean isRetired() {
			return retired;
		}

		public void setRetired(boolean retired) {
			this.retired = retired;
		}

    }

    public EmployeeListPage()
    {
        EmployeeListPage.log.trace("EmployeeListPage created");
        TEmployees EMP = getDatabase().T_EMPLOYEES;

        // create the Employees List page element
        employees = new BeanListPageElement<EmployeeListEntry>(this, EmployeeListEntry.class, EMP.EMPLOYEE_ID);
    }

    
    public EmployeeSearchFilter getSearchFilter()
    {
        return SampleUtils.getManagedBean(EmployeeSearchFilter.class);
    }
    
    public ListPageElement<EmployeeListEntry> getEmployees()
    {
        return employees;
    }

    /*** Action Section ***/

    @Override
    public void doInit()
    { // Notify Elements
        super.doInit();
    }
    
    public void doResetSearch()
    {
        getSearchFilter().resetFilter();
        this.employees.clearItems();
    }
    
    public void doSearch()
    {
        TDepartments DEP = getDatabase().T_DEPARTMENTS;
        TEmployees EMP = getDatabase().T_EMPLOYEES;

        DBColumnExpr FULL_NAME = EMP.LAST_NAME.append(", ").append(EMP.FIRST_NAME).as("NAME");
        DBColumnExpr DEPARTMENT = DEP.NAME.as("DEPARTMENT");

        DBCommand queryCmd = createQueryCommand();

        queryCmd.select(EMP.EMPLOYEE_ID, FULL_NAME);
        queryCmd.select(EMP.GENDER, EMP.DATE_OF_BIRTH, EMP.RETIRED);
        // queryCmd.select(EMP.RETIRED.decode(true, "X", "-"));
        queryCmd.select(DEPARTMENT);

        queryCmd.join(DEP.DEPARTMENT_ID, EMP.DEPARTMENT_ID);
        queryCmd.orderBy(EMP.FIRST_NAME);
        
        addAllConstraints(queryCmd);

        employees.initItems(queryCmd);
    }


    public Options getDepartmentOptions()
    {
    	TDepartments DEP = getDatabase().T_DEPARTMENTS;

    	DBCommand queryCmd = createQueryCommand();
    	queryCmd.select(DEP.DEPARTMENT_ID,DEP.NAME);
    	
    	SampleDB db = getDatabase();
        return db.queryOptionList(queryCmd, getSampleContext().getConnection());
    }
    
    
    protected void addAllConstraints(DBCommand queryCmd)
    {
        TEmployees EMP = getDatabase().T_EMPLOYEES;
        EmployeeSearchFilter filter = getSearchFilter();
        
        addSearchConstraint(queryCmd, EMP.EMPLOYEE_ID, filter);
        addSearchConstraint(queryCmd, EMP.FIRST_NAME, filter);
        addSearchConstraint(queryCmd, EMP.LAST_NAME, filter);
        addSearchConstraint(queryCmd, EMP.GENDER, filter);
        addSearchConstraint(queryCmd, EMP.DEPARTMENT_ID, filter);
    }
    
    private void addSearchConstraint(DBCommand cmd, DBColumn col, Object bean)
    {
        Object value;
        try
        {
            value = PropertyUtils.getProperty(bean, col.getBeanPropertyName());
            if (ObjectUtils.isEmpty(value))
                return;
            // is it an array
            if (value instanceof Collection<?> || value.getClass().isArray())
            {
                cmd.where(col.in(value));
                return;
            }
            // text
            if (col.getOptions() == null && col.getDataType().isText())
            {
                StringBuilder b = new StringBuilder();
                b.append("%");
                b.append(((String) value).toUpperCase());
                b.append("%");
                cmd.where(col.upper().like(b.toString()));
                return;
            }
            // value
            cmd.where(col.is(value));
            return;
        }
        catch (IllegalAccessException e)
        {
            throw new BeanPropertyGetException(bean, col.getBeanPropertyName(), e);
        }
        catch (InvocationTargetException e)
        {
            throw new BeanPropertyGetException(bean, col.getBeanPropertyName(), e);
        }
        catch (NoSuchMethodException e)
        {
            throw new BeanPropertyGetException(bean, col.getBeanPropertyName(), e);
        }
    }

    
}
