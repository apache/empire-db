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
package org.apache.empire.struts2.interceptors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.empire.struts2.action.ActionAccessValidator;

import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.ActionProxy;


@SuppressWarnings("serial")
public class ActionAccessInterceptor extends InterceptorSupport
{
    // Logger
    @SuppressWarnings("hiding")
    protected static Log log = LogFactory.getLog(ActionAccessInterceptor.class);

    private String loginAction = null;
    
    private String accessDeniedAction = null;
    
    public void setLoginAction(String loginAction)
    {
        this.loginAction = loginAction;
    }

    public void setAccessDeniedAction(String accessDeniedAction)
    {
        this.accessDeniedAction = accessDeniedAction;
    }

    @Override
    public String intercept(ActionInvocation invocation) throws Exception
    {
        // Set the action object to the HttpRequest
        Object action = invocation.getAction();
        // Check Login
        if (action instanceof ActionAccessValidator)         
        {
            // Check wether login is required
            if (loginAction!=null && ((ActionAccessValidator)action).loginRequired())
            {   // Log Info
                if (log.isWarnEnabled())
                    log.warn("Access to action " + action.getClass().getName() + " requires user login. Redirecting to " + loginAction);
                // redirect to login page
                return redirect(loginAction, true); 
            }
            // Check user has access to the action and the desired method
            if (accessDeniedAction!=null)
            {
                ActionProxy proxy = invocation.getProxy();
                if (((ActionAccessValidator)action).hasAccess(proxy.getMethod())==false)
                {   // Log Info
                    if (log.isWarnEnabled())
                        log.warn("Access to action " + action.getClass().getName() + " method " + proxy.getMethod() + " has been denied. Redirecting to " + accessDeniedAction);
                    // redirect to Access Denied page
                    return redirect(accessDeniedAction, true); 
                }
            }
        }
        // Call base
        return invocation.invoke();
    }
}
