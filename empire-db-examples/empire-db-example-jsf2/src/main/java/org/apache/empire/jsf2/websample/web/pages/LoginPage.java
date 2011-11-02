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

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.empire.jsf2.websample.web.FacesUtils;
import org.apache.empire.jsf2.websample.web.SampleUser;
import org.apache.empire.jsf2.websample.web.objects.User;

@ManagedBean
@ViewScoped
public class LoginPage extends Page
{
	public String logout()
	{
        return new LoginPage().name();		
	}
	
	public String login()
	{
		User user = (User)FacesUtils.getManagedBean(User.class);

		// Erzeuge Session User Objekt und setze es in der Session
		SampleUser sampleUser = new SampleUser(user.getName(), user.getPassword());
		
		return new EmployeeSearchPage().name();
	}
}
