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
package org.apache.empire.jsf2.websample.web;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ManagedBean
@SessionScoped
public class SampleSession
{
    private static final Logger log                = LoggerFactory.getLogger(SampleSession.class);

    private SampleUser user;

	private String              page               = "loginPage";

    private Throwable           rootCause;

    public SampleSession()
    {
    	SampleSession.log.info("SESSION: created.");
    }

    public void setPage(String page)
    {
        this.page = page;
    }

    public String getPage()
    {
        return this.page;
    }

    public void setError(Throwable rootCause)
    {
        this.rootCause = rootCause;
    }

    public Throwable getError()
    {
        return this.rootCause;
    }

    public SampleUser getUser() {
		return user;
	}

	public void setUser(SampleUser user) {
		this.user = user;
	}

}
