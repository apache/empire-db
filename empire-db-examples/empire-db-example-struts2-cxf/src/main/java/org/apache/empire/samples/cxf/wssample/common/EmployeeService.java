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

package org.apache.empire.samples.cxf.wssample.common;

import java.util.List;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.jws.WebParam.Mode;
import javax.xml.ws.Holder;

//specifies the the attribute wsdl:portType
@WebService(name = "employeeManagement")

public interface EmployeeService
{
    @WebMethod(operationName = "searchEmmployee")
    @WebResult(name = "searchResponse")
    public List<Employee> searchEmmployee(@WebParam(name = "id", mode = Mode.IN) Integer id,
                                          @WebParam(name = "firstName", mode = Mode.IN) String firstName,
                                          @WebParam(name = "lastName", mode = Mode.IN) String lastName,
                                          @WebParam(name = "department", mode = Mode.IN) Integer department);

    @WebMethod(operationName = "getEmmployee")
    @WebResult(name = "employee")
    public Employee getEmmployee(@WebParam(name = "id", mode = Mode.IN) int id);

    @WebMethod(operationName = "createEmmployee")
    @WebResult(name = "newEmployee")
    public Employee createEmmployee();

    @WebMethod(operationName = "saveEmmployee")
    @WebResult(name = "saveResponse")
    public boolean saveEmmployee(@WebParam(name = "employee", mode = Mode.INOUT) Holder<Employee> e);

    @WebMethod(operationName = "deleteEmmployee")
    @WebResult(name = "deleteResponse")
    public boolean deleteEmmployee(@WebParam(name = "id", mode = Mode.IN) int id);

    @WebMethod(operationName = "getDepartments")
    @WebResult(name = "allDepartments")
    public List<Department> getDepartments();
    
    @WebMethod(operationName = "ping")
    @WebResult(name = "pingResponse")
    public boolean ping();
    
}
