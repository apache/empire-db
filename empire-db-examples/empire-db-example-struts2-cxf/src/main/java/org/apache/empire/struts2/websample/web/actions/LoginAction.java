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

import java.util.Locale;
import java.util.Map;

import org.apache.empire.commons.Errors;
import org.apache.empire.commons.OptionEntry;
import org.apache.empire.commons.Options;
import org.apache.empire.struts2.websample.web.SampleUser;
import org.apache.empire.struts2.websample.web.actiontypes.SampleAction;

import com.opensymphony.xwork2.config.entities.Parameterizable;

public class LoginAction extends SampleAction implements Parameterizable // StaticParametersInterceptor
{
    // Login Targets
    public static final String INPUT   = "input";
    public static final String SUCCESS = "success";

    // LoginInfo
    public class LoginInfo
    {
        private String name;
        private String pwd;
        private String locale;

        public String getName()
        {
            return name;
        }

        public void setName(String name)
        {
            this.name = name;
        }

        public String getPwd()
        {
            return pwd;
        }

        public void setPwd(String pwd)
        {
            this.pwd = pwd;
        }

        public String getLocale()
        {
            return locale;
        }

        public void setLocale(String locale)
        {
            this.locale = locale;
        }
    }

    // Login Action
    public LoginAction()
    {
        // Constructor
    }

    // Login required for this action
    @Override
    public boolean loginRequired()
    {
        return false;
    }

    // Login form bean
    private LoginInfo loginInfo = new LoginInfo();

    public LoginInfo getLoginInfo()
    {
        return loginInfo;
    }

    public String doInit()
    {
        SampleUser user = getSession().getUser();
        if (user != null)
            loginInfo.setName(user.getUserId());
        
        // check webservice availability!
        checkWebService();
        
        // Done
        return INPUT;
    }

    public String doLogin()
    {
        String userID = loginInfo.getName();
        String userName = userID;

        if (userID == null || userID.length() == 0)
        {
            setActionError(Errors.InvalidPassword);
            return INPUT;
        }

        // Login
        SampleUser user = new SampleUser(userID, userName);
        user.setLocale(new Locale(loginInfo.getLocale()));
        getSession().setUser(user);
        // Done
        return SUCCESS;
    }

    public String doLogout()
    {
        getSession().setUser(null);
        // check webservice availability!
        checkWebService();
        return INPUT;
    }

    public String doError()
    {
        log.info("An error as occurred");
        return INPUT;
    }

    /*
     * Implementation of Parameterizable
     */

    /*
     * UPGRADE-struts 2.1.6
     * CHANGE: changed "Map<String, Object>" to "Map<String, String>"
     * Reason: The interface com.opensymphony.xwork2.config.entities.Parameterizable changed
     */
    private Map<String, String> actionConfigParams = null;

    /*
     * UPGRADE-struts 2.1.6
     * CHANGE: changed "String Object" to "String value"
     * Reason: The interface com.opensymphony.xwork2.config.entities.Parameterizable changed
     */
    public void addParam(String name, String value)
    {
        actionConfigParams.put(name, value);
    }

    /*
     * UPGRADE-struts 2.1.6
     * CHANGE: changed "Map<String, Object>" to "Map<String, String>"
     * Reason: The interface com.opensymphony.xwork2.config.entities.Parameterizable changed
     */
    public Map<String, String> getParams()
    {
        return actionConfigParams;
    }
	
    /*
     * UPGRADE-struts 2.1.6
     * CHANGE: changed "Map<String, Object>" to "Map<String, String>"
     * Reason: The interface com.opensymphony.xwork2.config.entities.Parameterizable changed
     */
    public void setParams(Map<String, String> actionConfigParams)
    {
        this.actionConfigParams = actionConfigParams;
    }
    
    /*
     * UPGRADE-struts 2.1.6
     * CHANGE: added method "asKey(String key)"
     * Reason: The interface com.opensymphony.xwork2.config.entities.Parameterizable changed
     */
    @Override
	public boolean hasKey(String key) {
    	return this.actionConfigParams.containsKey(key);
    }

    public Options getLanguages()
    {
        Options o = new Options();
        o.add(new OptionEntry(Locale.ENGLISH, Locale.ENGLISH.getDisplayLanguage()));
        o.add(new OptionEntry(Locale.GERMAN, Locale.GERMAN.getDisplayLanguage()));
        return o;
    }
}
