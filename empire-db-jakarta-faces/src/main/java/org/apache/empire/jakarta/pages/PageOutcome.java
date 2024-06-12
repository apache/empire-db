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
package org.apache.empire.jakarta.pages;


import org.apache.empire.commons.StringUtils;
import org.apache.empire.exceptions.InvalidArgumentException;

public class PageOutcome // *Deprecated* implements Serializable
{
    // *Deprecated* private static final long serialVersionUID = 1L;
    
    private String outcome;
    public PageOutcome(String outcome)
    {
        if (StringUtils.isEmpty(outcome))
            throw new InvalidArgumentException("outcome", outcome);
        this.outcome = outcome;
    }
    
    public PageOutcome addParamWithValue(String paramWithValue)
    {
        if (paramWithValue==null || paramWithValue.indexOf('=')<0)
            throw new InvalidArgumentException("paramWithValue", paramWithValue);
        // assemble
        if (outcome.indexOf('?')>0)
            outcome = outcome+"&"+paramWithValue;
        else
            outcome = outcome+"?"+paramWithValue;
        return this;
    }
    
    public PageOutcome addParam(String param, String value)
    {
        if (StringUtils.isEmpty(value))
            return this; // Ignore Empty values
        if (StringUtils.isEmpty(param))
            throw new InvalidArgumentException("param", param);
        String paramWithValue = param + "=" + value;
        return addParamWithValue(paramWithValue);
    }
    
    @Override
    public String toString()
    {
        return outcome;
    }
}
