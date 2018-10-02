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
package org.apache.empire.rest.json;

import java.util.LinkedHashSet;
import java.util.List;

import org.apache.empire.exceptions.EmpireException;
import org.apache.empire.rest.app.TextResolver;

public class JsoErrorInfo extends LinkedHashSet<String>
{
    private static final long serialVersionUID = 1L;

    public JsoErrorInfo(String message)
    {
        this.add(message);
    }
    
    public JsoErrorInfo(EmpireException e, TextResolver resolver)
    {
        this.add(resolver.getExceptionMessage(e));
    }
    
    public JsoErrorInfo(List<? extends EmpireException> list, TextResolver resolver)
    {
        for (EmpireException e : list)
        {
            this.add(resolver.getExceptionMessage(e));
        }
    }
}
