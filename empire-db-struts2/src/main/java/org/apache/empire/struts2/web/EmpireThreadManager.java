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

public final class EmpireThreadManager 
{

    private static final ThreadLocal<Object> currentRequest = new ThreadLocal<Object>();

    // Accessor for Current Request
    public static Object getCurrentRequest()
    {
        return currentRequest.get();
    }

    /**
     * Sets the request for the current thread
     * @param request The request object
     */
    protected static void setCurrentRequest(Object request)
    {
    	currentRequest.set(request);
    }
    
    /**
     * cleans up the current thread.
     * if a current request is set and the request object implements the WebRequest interface then the exit function is called.  
     * @param exitCode the exitCode
     */
    protected static void exit(int exitCode)
    {
        Object reqObj = currentRequest.get();
        if (reqObj!=null)
        {
            if (reqObj instanceof WebRequest)
            {
                ((WebRequest)reqObj).exit(exitCode);
            }
            // Clear Thread local
            currentRequest.set( null );
        }
    }
}
