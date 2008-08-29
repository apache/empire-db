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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class DefaultWebRequest implements WebRequest
{    
    private HttpServletRequest  httpRequest;
    private HttpServletResponse httpResponse; 
    
    public boolean init(HttpServletRequest request, HttpServletResponse response, Object session)
    {
        this.httpRequest = request;
        this.httpResponse = response;
        return true;
    }

    /**
     * @see WebRequest#getHttpRequest()
     */
    public HttpServletRequest getHttpRequest()
    {
        return httpRequest;
    }

    /**
     * @see WebRequest#getHttpResponse()
     */
    public HttpServletResponse getHttpResponse()
    {
        return httpResponse;
    }
    
    /**
     * @see WebRequest#exit(int)
     */
    public void exit(int exitCode)
    {
        // nothing to do
    }
}
