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
package org.apache.empire.struts2.action;

public interface Disposable
{
    /**
     * This exit-code might be used to indicate a successful action from the dispose method
     * The code will be forwarded to the WebRequest's exit method (see WebRequest.exit())  
     */
    public final int EXITCODE_SUCCESS =  0;  // The Action ended successfully

    /**
     * This Exit-code might be used to indicate an action error from the dispose method
     * The code will be forwarded to the request's exit method (see below)  
     */
    public final int EXITCODE_ERROR   = -1;  // The Action ended with an error

    /**
     * Initializes the object
     * Use this to initialize Action objects instead of the Action's constructor
     */
    void init();

    /**
     * Cleanup resources allocated by the object
     * 
     * @return an exit code which will be passed to the WebRequest's exit function (@see WebRequest.exit())
     */
    int dispose();
}
