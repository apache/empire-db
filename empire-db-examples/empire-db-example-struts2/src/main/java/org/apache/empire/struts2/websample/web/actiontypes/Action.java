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
package org.apache.empire.struts2.websample.web.actiontypes;

import java.sql.Connection;
import java.util.Locale;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.empire.struts2.action.WebAction;
import org.apache.empire.struts2.websample.db.SampleDB;
import org.apache.empire.struts2.websample.web.SampleApplication;
import org.apache.empire.struts2.websample.web.SampleContext;
import org.apache.empire.struts2.websample.web.SampleRequest;
import org.apache.empire.struts2.websample.web.SampleSession;
import org.apache.empire.struts2.websample.web.SampleUser;


@SuppressWarnings("serial")
public abstract class Action extends WebAction
    implements SampleContext
{
    // Logger
    @SuppressWarnings("hiding")
    protected static Log log = LogFactory.getLog(Action.class);

    public Action()
    {
        // Constructor
    }

    // Login requried for this action
    @Override
    public boolean loginRequired()
    {
        return (getSession().getUser()==null);
    }
    
    @Override
    public Locale getLocale()
    {
        /*
        Locale loc = super.getLocale();
        if (loc.equals(Locale.GERMAN))
            return loc;
        */    
        return Locale.ENGLISH; 
    }
    
    // Request
    public static SampleRequest getRequest() 
    {
        return SampleRequest.getInstance();
    }
    
    // Application getters
    public SampleApplication getApplication()
    {
        return getRequest().getApplication();
    }
    
    public SampleSession getSession()
    {
        return getRequest().getSession();
    }

    // ------ Sample Context Implementation ------
    
    public SampleDB getDatabase()
    {
        return getApplication().getDatabase();
    }
    
    public SampleUser getUser()
    {
        return getSession().getUser();
    }
    
    @Override
    public Connection getConnection()
    {
        return getRequest().getConnection();
    }
    
}
