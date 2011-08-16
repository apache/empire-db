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
import org.apache.empire.samples.cxf.wssample.common.EmployeeService;

// The access to WebService.
public class EmployeeServiceClient
{
    private EmployeeService service = null;

    public EmployeeServiceClient(String serviceAddress)
    {
        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.getInInterceptors().add(new LoggingInInterceptor());
        factory.getOutInterceptors().add(new LoggingOutInterceptor());
        factory.setServiceClass(EmployeeService.class);
        factory.setAddress(serviceAddress);
        this.service = (EmployeeService) factory.create();
    }

    public Employee createEmmployee()
    {
        return service.createEmmployee();
    }

    public List<Department> getDepartments()
    {
        return service.getDepartments();
    }

    public Employee getEmmployee(int id)
    {
        return service.getEmmployee(id);
    }

    public void saveEmployee(Employee e)
    {
        // Employee is here a INOUT parameter, and therfore has to be placed in a Holder.
        Holder<Employee> holder = new Holder<Employee>(e);
        service.saveEmmployee(holder);
        // In order to retrieve changes made by the webservice we copy the content to our local variable.
        e.set(holder.value);
    }

    public void deleteEmployee(int id)
    {
        service.deleteEmmployee(id);
    }

    public List<Employee> searchEmployee(Integer id, String firstName, String lastName, Integer department)
    {
        return service.searchEmmployee(id, firstName, lastName, department);
    }
    
    public boolean ping()
    {
        return service.ping();
    }
}
