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

import java.sql.Connection;

import org.apache.empire.db.DBCommand;
import org.apache.empire.jsf2.pages.Page;
import org.apache.empire.jsf2.websample.db.SampleDB;
import org.apache.empire.jsf2.websample.web.SampleApplication;
import org.apache.empire.jsf2.websample.web.SampleUser;
import org.apache.empire.jsf2.websample.web.SampleUtils;

public class SamplePage extends Page
{
    private static final long serialVersionUID = 1L;


    protected SampleApplication getApplication()
    {
        return SampleUtils.getSampleApplication();
    }

    protected SampleDB getDatabase()
    {
        return SampleUtils.getDatabase();
    }

    public Connection getConnection()
    {
        Connection conn = SampleUtils.getConnection();
        return conn;
    }

    public SampleUser getUser()
    {
        return SampleUtils.getSampleUser();
    }

//    public int getLanguageIndex()
//    {
//        return SampleUtils.getSampleSession().getUser().getLanguageIndex();
//    }



    protected DBCommand createQueryCommand()
    {
        return getDatabase().createCommand();
    }
}
