<!-- 
  Licensed to the Apache Software Foundation (ASF) under one
  or more contributor license agreements.  See the NOTICE file
  distributed with this work for additional information
  regarding copyright ownership.  The ASF licenses this file
  to you under the Apache License, Version 2.0 (the
  "License"); you may not use this file except in compliance
  with the License.  You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing,
  software distributed under the License is distributed on an
  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  KIND, either express or implied.  See the License for the
  specific language governing permissions and limitations
  under the License.
 --> 
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="org.apache.empire.struts2.websample.ws.*"%>
<%@ taglib prefix="e" uri="/empire-tags" %>
<jsp:useBean id="db" scope="application" type="org.apache.empire.struts2.websample.ws.SampleBeanDomain"/>
<jsp:useBean id="action" scope="request" type="org.apache.empire.struts2.websample.web.actions.EmployeeListAction"/>
<% 
	SampleBeanDomain.Employees EMP = db.T_EMPLOYEES;
	SampleBeanDomain.Departments DEP = db.T_DEPARTMENTS;
%>
<html>
<head>
    <link href="css/main.css" rel="stylesheet" type="text/css"/>
    <e:text tag="title" value="!label.employees"/>
</head>
<body>
<div class="titleDiv"><e:text value="!application.title"/></div>
<e:text tag="h1" value="!label.employees.beanList"/>
<e:actionerrors />
<e:actionmessage />
<e:link action="employeeDetail!doCreate" text="!link.label.addemployee"/>
<br/><br/>
<table class="borderAll">
    <e:thr>
        <e:th text="ID" />
        <e:th text="<%= EMP.C_FIRSTNAME.getTitle() %>" />
        <e:th text="<%= EMP.C_LASTNAME.getTitle() %>" />
        <e:th text="<%= EMP.C_GENDER.getTitle() %>" />
        <e:th text="<%= EMP.C_DATE_OF_BIRTH.getTitle() %>" />
        <e:th text="!label.department" />
    </e:thr>
    <e:list source="<%= action.getEmployees() %>">
		<%-- <jsp:useBean id="bean" scope="page" type="org.apache.empire.struts2.websample.web.actions.EmployeeListAction.EmployeeInfo" /> --%>
    	<e:tr cssClass="even" cssOddClass="odd" columnWrap="nowrap">
            <e:td column="<%= EMP.C_EMPLOYEE_ID %>" />
            <e:td column="<%= EMP.C_FIRSTNAME %>" action="employeeDetail!doLoad" item="<%= EMP.C_EMPLOYEE_ID %>" wrap="break-word" />
            <e:td column="<%= EMP.C_LASTNAME %>" action="employeeDetail!doLoad" item="<%= EMP.C_EMPLOYEE_ID %>" wrap="break-word" />
            <e:td column="<%= EMP.C_GENDER %>" />
            <e:td column="<%= EMP.C_DATE_OF_BIRTH %>" />
            <e:td column="<%= EMP.C_DEPARTMENT_ID %>" />
        </e:tr>
    </e:list>
</table>
<br/>
<e:link action="employeeList!doInit" text="!link.label.newSearch" />		
</body>
</html>