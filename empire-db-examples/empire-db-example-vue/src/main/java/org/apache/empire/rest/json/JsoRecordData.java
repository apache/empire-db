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

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;

import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.data.Column;
import org.apache.empire.db.DBRecord;
import org.apache.empire.db.DBRecordData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.DecimalNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.NumericNode;
import com.fasterxml.jackson.databind.node.TextNode;

@JsonDeserialize(using = JsoRecordData.Deserializer.class)   
public class JsoRecordData extends LinkedHashMap<String, Object>
{
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(JsoRecordData.class);
    
    /**
     * Deserializer
     * @author doebele
     */
    public static class Deserializer extends StdDeserializer<JsoRecordData> { 
     
        private static final long serialVersionUID = 1L;
    
        public Deserializer() { 
            this(null); 
        } 
     
        public Deserializer(Class<?> vc) { 
            super(vc); 
        }
     
        @Override
        public JsoRecordData deserialize(JsonParser jp, DeserializationContext ctxt) 
          throws IOException, JsonProcessingException {
            // read and parse
            JsonNode node = jp.getCodec().readTree(jp);
            return new JsoRecordData(node);
        }
    }
    
    /**
     * Serialize to JSON
     * @param rec
     */
    public JsoRecordData(DBRecordData rec)
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

    public JsoRecordData(DBRecord rec)
    {
        this((DBRecordData)rec);
        // add new flag
        put("_newRecord", rec.isNew());
    }
    
    public JsoRecordData(JsoColumnMeta[] meta)
    {
        super(meta.length);
        for (int i=0; i<meta.length; i++)
        {
            String prop = meta[i].getProperty();
            put(prop, null);
        }
    }
    
    /**
     * Deserialize from JSON
     * @param node
     */
    public JsoRecordData(JsonNode node)
    {
        Iterator<String> fields = node.fieldNames();
        while (fields.hasNext())
        {   // add all fields
            String field = fields.next();
            JsonNode vn = node.get(field);
            final Object value;
            if (vn instanceof NullNode)
                value = null;
            else if (vn instanceof TextNode)
                value = vn.textValue();
            else if (vn instanceof DecimalNode)
                value = vn.decimalValue();
            else if (vn instanceof LongNode)
                value = vn.longValue();
            else if (vn instanceof IntNode)
                value = vn.intValue();
            else if (vn instanceof NumericNode)
                value = vn.numberValue();
            else if (vn instanceof NumericNode)
                value = vn.numberValue();
            else if (vn instanceof BooleanNode)
                value = vn.booleanValue();
            else // default
            {
                log.warn("Unknown JSon Node type: {} for {}", vn.getClass().getSimpleName(), field); 
                value = vn.asText();
            }
            // put value
            put(field, value);
        }
    }
    
    /**
     * other methos
     */
    public boolean hasValue(Column c)
    {
        return this.containsKey(c.getBeanPropertyName());
    }

    public boolean hasNonNullValue(Column c)
    {
        Object val = getValue(c);
        return !ObjectUtils.isEmpty(val);
    }

    public Object getValue(Column c)
    {
        return this.get(c.getBeanPropertyName());
    }

    public String getString(Column c)
    {
        return String.valueOf(getValue(c));
    }
    
    public boolean isNewRecord()
    {
        return ObjectUtils.getBoolean(get("_newRecord"));
    }
}
