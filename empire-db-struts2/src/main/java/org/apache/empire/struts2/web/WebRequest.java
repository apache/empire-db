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

public interface WebRequest
{    
    /**
     * This String is the attribute name of this request object on the applications request scope
     */
    public final String REQUEST_NAME  = "webRequest";
    
    /**
     * Initializes the request object
     * This method is called from the EmpiretrutsDispatcher
     *  
     * @param request the HttpServletRequest
     * @param response the HttpServletResponse
     * @param session the sessionObject
     * 
     * @return true if the request should continue processing or false otherwise
     */
    boolean init(HttpServletRequest request, HttpServletResponse response, Object session);

    /**
     * returns the current HttpRequestObject
     * @return the httpServletRequest
     */
    public HttpServletRequest getHttpRequest();

    /**
     * returns the current HttpResponseObject
     * @return the httpServletResponse
     */
    public HttpServletResponse getHttpResponse();
    
    /**
     * This function is called from the EmpireStrutsDispatcher when a request ends
     * if an action was accociated with the request and the action implements the Disposible interface
     * then the exit code returned by Disposible.dispose() is passed with the exitCode parameter
     * 
     * You might use the exitCode e.g. to commit or rollback a transaction on the JDBC-Connection
     * 
     * @param exitCode
     */
    void exit(int exitCode);
}
