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
package org.apache.empire.struts2.web;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.FilterConfig;

import org.apache.struts2.dispatcher.Dispatcher;
import org.apache.struts2.dispatcher.FilterDispatcher;

@SuppressWarnings("deprecation")
public class EmpireFilterDispatcher extends FilterDispatcher
{
    public EmpireFilterDispatcher()
    {
        // Default Constructor
    }

	@Override
    protected Dispatcher createDispatcher(FilterConfig filterConfig)
    {
        Map<String, String> params = new HashMap<String, String>();
        for (Enumeration<String> e = filterConfig.getInitParameterNames(); e.hasMoreElements();)
        {
            String name = e.nextElement();
            String value = filterConfig.getInitParameter(name);
            params.put(name, value);
        }
        return new EmpireStrutsDispatcher(filterConfig.getServletContext(), params);
    }

}
