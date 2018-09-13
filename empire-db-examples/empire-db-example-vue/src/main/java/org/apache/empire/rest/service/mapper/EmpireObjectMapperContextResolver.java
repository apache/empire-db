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
package org.apache.empire.rest.service.mapper;

import java.lang.reflect.Type;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.DefaultDeserializationContext;

@Provider
public class EmpireObjectMapperContextResolver implements ContextResolver<ObjectMapper> {

    private final ObjectMapper mapper;
    
    public static class EmpireObjectMapper extends ObjectMapper {
        private static final long serialVersionUID = 1L;

        @Override
        public DeserializationContext getDeserializationContext()
        {
            // TODO Auto-generated method stub
            return super.getDeserializationContext();
        }

        @Override
        public JavaType constructType(Type t)
        {
            // TODO Auto-generated method stub
            return super.constructType(t);
        }

        @Override
        public JsonFactory getFactory()
        {
            // TODO Auto-generated method stub
            return super.getFactory();
        }

        @Override
        public boolean canDeserialize(JavaType type)
        {
            // TODO Auto-generated method stub
            return super.canDeserialize(type);
        }

        @Override
        public <T> T convertValue(Object fromValue, Class<T> toValueType)
            throws IllegalArgumentException
        {
            // TODO Auto-generated method stub
            return super.convertValue(fromValue, toValueType);
        }

        @Override
        public <T> T convertValue(Object fromValue, TypeReference<?> toValueTypeRef)
            throws IllegalArgumentException
        {
            // TODO Auto-generated method stub
            return super.convertValue(fromValue, toValueTypeRef);
        }

        @Override
        public <T> T convertValue(Object fromValue, JavaType toValueType)
            throws IllegalArgumentException
        {
            // TODO Auto-generated method stub
            return super.convertValue(fromValue, toValueType);
        }

        @Override
        protected DefaultDeserializationContext createDeserializationContext(JsonParser p, DeserializationConfig cfg)
        {
            // TODO Auto-generated method stub
            return super.createDeserializationContext(p, cfg);
        }
        
    }

    public EmpireObjectMapperContextResolver() {
        this.mapper = createObjectMapper();
    }

    @Override
    public ObjectMapper getContext(Class<?> type) {
        return mapper;
    }

    private ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new EmpireObjectMapper();
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        return mapper;
    }
}