<!-- 
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
 --> 
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib prefix="e" uri="/empire-tags" %>
<jsp:useBean id="action" scope="request" type="org.apache.empire.struts2.websample.web.actions.LoginAction"/>
<html>
<head>
    <link href="css/main.css" rel="stylesheet" type="text/css"/>
    <title><e:text value="!application.title"/></title>
</head>
<body>
<div class="titleDiv"><e:text value="!application.title"/></div>
<h1><e:text value="!page.label.login"/></h1>
<e:actionerrors />
<e:actionmessage />
<p><e:text value="!page.label.loginHint"/></p>
<e:form action="login!doLogin">
    <s:textfield name="loginInfo.name" value="%{loginInfo.name}" label="%{getText('label.user.Name')}" size="40"/>
    <s:password name="loginInfo.pwd"  value="%{loginInfo.pwd}"  label="%{getText('label.user.Pwd')}"  size="40"/>
    <s:select name="loginInfo.locale" list="#{'de':'German', 'en':'English'}" label="%{getText('label.user.Language')}"/>
    <e:submit text="!button.label.login" embed="true"/>
</e:form>
</body>
</html>