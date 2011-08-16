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
package org.apache.empire.struts2.websample.web;

import java.util.HashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.empire.struts2.web.SessionContext;
import org.apache.empire.struts2.web.WebSession;

public class SampleSession implements WebSession
{
    // Logger
    protected static Log log = LogFactory.getLog(SampleSession.class);

    // Non-Static
    private SampleApplication application = null;
    public SampleUser user = null;
    public HashMap<String, Object> objectMap = new HashMap<String, Object>();
    
    // Init Session
    public void init(SessionContext session, Object application)
    {
        this.application = (SampleApplication) application;
        if (this.application==null)
        {
            throw new RuntimeException("Fatal: Application object is null!");
        }
        log.info("Session created ");
    }

    // Get Application
    public SampleApplication getApplication()
    {
        return application;
    }

    public SampleUser getUser()
    {
        return user;
    }

    public void setUser(SampleUser user)
    {
        this.user = user;
    }
    
    public Object getObject(String name)
    {
        return objectMap.get(name);
    }
    
    public final Object getObject(Class<?> objclass)
    {
        return getObject(objclass.getName());
    }
    
    public Object setObject(String name, Object obj)
    {
        return objectMap.put(name, obj);
    }
    
    public final Object setObject(Class<?> objclass, Object obj)
    {
        return setObject(objclass.getName(), obj);
    }
    
}
