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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class JsoResultWithMeta
{
    private final Map<String, JsoColumnMeta> meta;
    private final Object data;

    public JsoResultWithMeta(JsoRecordData record, JsoColumnMeta... columnMeta)
    {
        super();
        this.meta = new LinkedHashMap<String, JsoColumnMeta>(columnMeta.length);
        for (JsoColumnMeta c : columnMeta) {
            meta.put(c.getProperty(), c);
        }
        this.data = record;
    }
    
    public JsoResultWithMeta(List<JsoRecordData> list, JsoColumnMeta... columnMeta)
    {
        super();
        this.meta = new LinkedHashMap<String, JsoColumnMeta>(columnMeta.length);
        for (JsoColumnMeta c : columnMeta) {
            meta.put(c.getProperty(), c);
        }
        this.data = list;
    }

    public Map<String, JsoColumnMeta> getMeta()
    {
        return meta;
    }

    public Object getData()
    {
        return data;
    }
}
