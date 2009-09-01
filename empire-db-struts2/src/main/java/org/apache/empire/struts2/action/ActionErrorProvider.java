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

import java.util.Map;

import org.apache.empire.commons.ErrorInfo;


public interface ActionErrorProvider
{
    /**
     * returns true if the current action has either an action or a field error
     * WARNING: this function returns false if a previous action had an error 
     * 
     * @return true if either an Action Error or a Field Error has been set
     */
    boolean hasActionError();
    
    /**
     * clears the action error (if any) and all item errors
     */
    void clearActionErrors();

    /**
     * returns the last Action Error of the current or a previous aciton
     * the error is left on the session unless the clear flag is set to true
     * In order to display the error to the user an application should use
     * this function instead of getActionError() and set clear to true. 
     * 
     * @param clear flag that determines whether to remove this error from the session or not
     * 
     * @return the last Action error that has been put on the session
     */
    ErrorInfo getLastActionError(boolean clear);
    
    /**
     * returns a map of field errors
     * field errors are not persisted on the session and will be lost on redirects. 
     * 
     * @return the map of field errors containing the field name in the key.
     */
    Map<String, ErrorInfo> getItemErrors();
    
    /**
     * return the localized message for an error
     * 
     * @return the error message in the curren user's language
     */
    String getLocalizedErrorMessage(ErrorInfo error);
    
    /**
     * returns the last set Action Message 
     * the message is left on the session unless the clear flag is set to true
     * 
     * @return an action message for the user
     */
    String getLastActionMessage(boolean clear);
    
}
