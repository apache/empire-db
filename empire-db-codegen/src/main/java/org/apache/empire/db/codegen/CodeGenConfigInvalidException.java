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
package org.apache.empire.db.codegen;

import org.apache.empire.exceptions.InvalidPropertyException;
import org.slf4j.Logger;

public class CodeGenConfigInvalidException extends InvalidPropertyException
{
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(CodeGenConfig.class);
    
    private static final long serialVersionUID = 1L;
    
    public CodeGenConfigInvalidException(String property, Object value, Exception cause)
    {
        super(property, value, cause);
    }
    
    public CodeGenConfigInvalidException(String property, Object value)
    {
        this(property, value, null);
    }
    
    /**
     * log the error (info must be enabled)
     */
    @Override
    protected void log()
    {
        log.error("Invalid configuration: "+getMessage(), this);
    }
    
}
