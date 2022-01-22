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

import java.util.Locale;

public class SampleUser // *Deprecated* implements Serializable
{
    // *Deprecated* private static final long serialVersionUID = 1L;

    protected static final String MANAGED_BEAN_NAME = "sampleUser";

    private String                userName          = "test";
    private String                password          = "";
    private Locale                language          = SampleApplication.DEFAULT_LOCALE;

    public SampleUser()
    {
    }

    public String getPassword()
    {
        return password;
    }

    public String getUserName()
    {
        return userName;
    }

    public void setUserName(String name)
    {
        this.userName = name;
    }

    public void setPassword(String password)
    {
        this.password = password;
    }

    public Locale getLocale()
    {
        return this.language;
    }
    
    public void setLocale(Locale lo)
    {
        this.language = lo;
    }

}
