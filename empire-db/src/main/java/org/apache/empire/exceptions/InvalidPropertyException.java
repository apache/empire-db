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
package org.apache.empire.exceptions;

import org.apache.empire.commons.ErrorType;
import org.apache.empire.commons.StringUtils;

public class InvalidPropertyException extends EmpireException
{
    private static final long serialVersionUID = 1L;
    
    private static final String NULL = "NULL";
    
    public static final ErrorType errorType = new ErrorType("error.propertyInvalid", "The property {0} is not valid. Current value is {1}.");
    
    public InvalidPropertyException(String property, Object value, Exception cause)
    {
        super(errorType, new String[] { property, StringUtils.toString(value, NULL) }, cause);
    }
    
    public InvalidPropertyException(String property, Object value)
    {
        super(errorType, new String[] { property, StringUtils.toString(value, NULL) });
    }
}
