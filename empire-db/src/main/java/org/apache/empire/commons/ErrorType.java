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
package org.apache.empire.commons;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The ErrorType class defines a type of error.
 * In order to define an error you need to provide an ErrorKey and a MessagePattern.
 * <P>
 * The ErrorKey is a unique identifier for the error that may also be used as a translation key
 * The ErrorKey should always start with the "error." prefix. 
 * <P>
 * The MessagePattern is a template containing the error message and placeholders for additional parameters.
 * The MessagePattern must be formated according to the {@link java.text.MessageFormat} rules.
 */
public class ErrorType // *Deprecated* implements Serializable
{
    // *Deprecated* private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(ErrorType.class);

    private final String key;
    private final String msgPattern;
    private final int    numParams;

    /**
     * Defines an error type.
     * 
     * @param errorKey the error key string (can be used for internationalization)
     * @param msgPattern message pattern in english language used e.g. for logging
     */
    public ErrorType(String errorKey, String msgPattern)
    {
        this.key = errorKey;
        this.msgPattern = msgPattern;
        // Count number of params
        int numParamCount = 0;
        while(true)
        {
            String placeholder = "{" + String.valueOf(numParamCount) + "}";
            if (msgPattern.indexOf(placeholder)<0)
                break;
            // Param found
            numParamCount++;    
        }
        this.numParams = numParamCount;
        // Write error definition to log
        log.debug("Error defined: {}={}", key, msgPattern);
    }

    /**
     * Returns the error type key (might be used for internationalization).
     * 
     * @return the error type key
     */
    public String getKey()
    {
        return key;
    }

    /**
     * Returns the message pattern.
     * 
     * @return the message pattern
     */
    public String getMessagePattern()
    {
        return msgPattern;
    }

    /**
     * Returns the number of parameters required for the message pattern.
     * 
     * @return the number of parameters required for the message pattern
     */
    public int getNumParams()
    {
        return numParams;
    }
}
