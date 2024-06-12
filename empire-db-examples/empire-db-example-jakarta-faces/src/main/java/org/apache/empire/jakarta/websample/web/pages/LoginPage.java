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
package org.apache.empire.jakarta.websample.web.pages;

import java.util.Locale;

import org.apache.empire.commons.Options;
import org.apache.empire.jakarta.app.FacesUtils;
import org.apache.empire.jakarta.pages.PageOutcome;
import org.apache.empire.jakarta.websample.web.SampleSession;
import org.apache.empire.jakarta.websample.web.SampleUtils;

public class LoginPage extends SamplePage {
    
    private static final Options languageOptions;
    
    static {
        languageOptions = new Options();
        languageOptions.set(Locale.US, "English");
        languageOptions.set(Locale.GERMAN, "German");
    }
    
    public Options getLanguageOptions()
    {
        return languageOptions;
    }

	public void doLogin() 
	{
		SampleSession session =	SampleUtils.getSampleSession();
		session.setUser(SampleUtils.getSampleUser());
		// goto list page
		navigateTo(SamplePages.EmployeeListPage.getRedirect());
	}

	public void doLogout() 
	{
		// Perform logout
		PageOutcome logout = this.getPageDefinition().getRedirect().addParam("logout", String.valueOf(true));
		navigateTo( logout );
		
        // Invalidate Session
		FacesUtils.getContext().getExternalContext().invalidateSession();
	}
	
}
