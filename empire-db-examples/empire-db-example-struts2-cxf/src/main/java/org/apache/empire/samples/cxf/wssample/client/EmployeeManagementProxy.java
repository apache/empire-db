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

package org.apache.empire.samples.cxf.wssample.client;

import java.util.List;

import javax.xml.ws.Holder;

import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.empire.samples.cxf.wssample.common.Department;
import org.apache.empire.samples.cxf.wssample.common.Employee;
import org.apache.empire.samples.cxf.wssample.common.EmployeeManagementInterface;

// The access to WebService.
public class EmployeeManagementProxy
{
    private EmployeeManagementInterface proxy = null;
    private String                      serviceAddress;

    public EmployeeManagementProxy(String serviceAddress)
    {
        this.serviceAddress = serviceAddress;
        init();
    }

    private void init()
    {
        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.getInInterceptors().add(new LoggingInInterceptor());
        factory.getOutInterceptors().add(new LoggingOutInterceptor());
        factory.setServiceClass(EmployeeManagementInterface.class);
        factory.setAddress(serviceAddress);
        this.proxy = (EmployeeManagementInterface) factory.create();
    }

    public Employee createEmmployee()
    {
        return proxy.createEmmployee();
    }

    public List<Department> getDepartments()
    {
        return proxy.getDepartments();
    }

    public Employee getEmmployee(int id)
    {
        return proxy.getEmmployee(id);
    }

    public boolean saveEmployee(Employee e)
    {
        // Employee is here a INOUT parameter, and therfore has to be placed in a Holder.
        Holder<Employee> holder = new Holder<Employee>(e);
        boolean retVal = proxy.saveEmmployee(holder);
        // In order to retrieve changes made by the webservice we copy the content to our local variable.
        e.set(holder.value);
        return retVal;
    }

    public boolean deleteEmployee(int id)
    {
        return proxy.deleteEmmployee(id);
    }

    public List<Employee> searchEmployee(Integer id, String firstName, String lastName, Integer department)
    {
        return proxy.searchEmmployee(id, firstName, lastName, department);
    }

    public String test()
    {
        return proxy.test();
    }

}
