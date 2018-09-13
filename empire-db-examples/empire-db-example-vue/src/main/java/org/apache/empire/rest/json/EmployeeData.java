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

import org.apache.empire.db.DBRecordData;

/*
public static class MyItemDeserializer extends StdDeserializer<EmployeeFilter> { 
 
    public MyItemDeserializer() { 
        this(null); 
    } 
 
    public MyItemDeserializer(Class<?> vc) { 
        super(vc); 
    }
 
    @Override
    public EmployeeFilter deserialize(JsonParser jp, DeserializationContext ctxt) 
      throws IOException, JsonProcessingException {
        JsonNode node = jp.getCodec().readTree(jp);
        // int id = (Integer) ((IntNode) node.get("id")).numberValue();
        // int userId = (Integer) ((IntNode) node.get("createdBy")).numberValue();
        String firstname = node.get("firstname").asText();
        String lastname  = node.get("lastname").asText();
 
        return new EmployeeFilter();
    }
}*/

// @JsonDeserialize(using = MyItemDeserializer.class)   
public class EmployeeData extends LinkedHashMap<String, Object>
{
    private static final long serialVersionUID = 1L;

    public EmployeeData(DBRecordData rec)
    {
        super(rec.getFieldCount());
        for (int i=0; i<rec.getFieldCount(); i++)
        {
            String prop = rec.getColumnExpr(i).getBeanPropertyName();
            if (prop==null)
                continue;
            put(prop, rec.getValue(i));
        }
    }
}
