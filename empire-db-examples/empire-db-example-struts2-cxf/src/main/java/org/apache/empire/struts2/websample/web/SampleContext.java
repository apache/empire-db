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

import org.apache.empire.samples.cxf.wssample.client.EmployeeServiceClient;
import org.apache.empire.struts2.websample.ws.SampleBeanDomain;

public class SampleContext
{

    /*
     * This is just a very simple implementation of a "context" object.
     * You may add user specific information here.
     */
    
    public SampleBeanDomain getBeanDomain()
    {
        return SampleApplication.getInstance().getBeanDomain();
    }

    public EmployeeServiceClient getEmployeeServiceClient()
    {
        return SampleApplication.getInstance().getEmployeeServiceClient();
    }
}
