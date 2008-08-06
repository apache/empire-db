<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib prefix="e" uri="/empire-tags" %>
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
    <s:textfield name="loginInfo.pwd"  value="%{loginInfo.pwd}"  label="%{getText('label.user.Pwd')}"  size="40"/>
    <e:submit text="!button.label.login" embed="true"/>
</e:form>
</body>
</html>