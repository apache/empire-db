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

import org.apache.empire.jsf2.app.WebDBContext;
import org.apache.empire.jsf2.websample.db.SampleDB;

/**
 * This is an example for a custom DBContext extension
 * @author rainer
 *
 */
public class SampleContext extends WebDBContext<SampleDB>
{
    // *Deprecated* private static final long serialVersionUID = 1L;
    
    private final SampleSession session;
    
    public SampleContext(SampleApplication app, SampleSession session)
    {
        super(app, app.getDatabase());
        // the session
        this.session = session;
    }
    
    public SampleApplication getApplication()
    {
        return (SampleApplication)app;
    }

    public SampleUser getUser()
    {
        return session.getUser();
    }
}
