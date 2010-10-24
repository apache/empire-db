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

import java.util.Map;

import org.apache.empire.commons.Errors;
import org.apache.empire.struts2.websample.web.SampleUser;
import org.apache.empire.struts2.websample.web.actiontypes.Action;

import com.opensymphony.xwork2.config.entities.Parameterizable;

public class LoginAction extends Action
        implements Parameterizable // StaticParametersInterceptor
{
    // Login Targets
    public static final String INPUT = "input";
    public static final String SUCCESS = "success";

    // LoginInfo
    public class LoginInfo
    {
        private String name;
        private String pwd;
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
        SampleUser user = getUser();
        if (user!=null)
            loginInfo.setName(user.getUserId());
        // Done
        return INPUT;
    }

    public String doLogin()
    {
        String userID = loginInfo.getName();
        String userName = userID;
        
        if (userID==null || userID.length()==0)
        {
            setActionError(Errors.InvalidPassword);
            return INPUT;
        }
        
        // Login
        SampleUser user = new SampleUser(userID, userName);
        getSession().setUser(user);
        // Done
        return SUCCESS;
    }

    public String doLogout()
    {
        getSession().setUser(null);
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
}
