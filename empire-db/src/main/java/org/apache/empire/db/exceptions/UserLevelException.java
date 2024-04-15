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
package org.apache.empire.db.exceptions;

import org.apache.empire.commons.ErrorType;
import org.apache.empire.exceptions.EmpireException;

/**
 * UserLevelException
 * An exception of this type indicates an error that should be displayed to the user only
 * and does not need to be logged 
 * @author doebele
 */
public abstract class UserLevelException extends EmpireException
{
    private static final long serialVersionUID = 1L;

    protected UserLevelException(ErrorType errType, String[] params)
    {
        super(errType, params);
    }
}
