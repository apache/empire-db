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
package org.apache.empire.struts2.websample.web.actions;

import org.apache.empire.samples.cxf.wssample.client.EmployeeManagementProxy;
import org.apache.empire.samples.cxf.wssample.common.Employee;
import org.apache.empire.struts2.actionsupport.SessionPersistence;
import org.apache.empire.struts2.websample.web.SampleApplication;
import org.apache.empire.struts2.websample.web.SampleContext;
import org.apache.empire.struts2.websample.web.actiontypes.BeanDetailAction;
import org.apache.empire.struts2.websample.ws.records.EmployeeRecord;

/**
 * EmployeeDetailAction
 * <p>
 * This class provides form processing functions for an Employee record.<br>
 * The class uses a RecordActionSupport object which does most of the work.<br>
 * For multi-record forms it is possible to have several RecordActionSupport members.<br>
 * In this case each must be given a differnt property name however (see RecordActionSupport overloads).
 * </p>
 */
public class EmployeeDetailAction extends BeanDetailAction<Employee>
{
    private EmployeeManagementProxy proxy = SampleApplication.getInstance().getWebServiceProxy();

    // ------- Action Construction -------

    public EmployeeDetailAction()
    {
        super(new EmployeeRecord(new SampleContext()), SessionPersistence.Key);

    }

    /*
     * 
     * public EmployeeDetailAction2() { super(SampleBeanDomain.getInstance().T_EMPLOYEES,SessionPersistence.Key); }
     */

    // ------- Action Properties -------
    // ------- the have to overridables -------
    @Override
    public Employee createBean()
    {
        Employee e = proxy.createEmmployee();
        return e;
    }

    @Override
    public boolean deleteBean(Object[] key)
    {
        int id = Integer.parseInt(key[0].toString());
        return proxy.deleteEmployee(id);
    }

    @Override
    public Employee loadBean(Object[] key)
    {
        //TODO: not working properly!
        int id = Integer.parseInt(key[0].toString());
        return proxy.getEmmployee(id);
    }

    @Override
    public boolean saveBean(Employee bean, boolean isNew)
    {
        return proxy.saveEmployee(bean);
    }

}
