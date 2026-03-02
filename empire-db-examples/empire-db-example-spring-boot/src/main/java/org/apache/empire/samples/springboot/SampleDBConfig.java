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
package org.apache.empire.samples.springboot;

import org.apache.empire.db.DBContext;
import org.apache.empire.springboot.EmpireDBConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(EmpireDBConfig.class)
public class SampleDBConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(SampleDBConfig.class);

    @Bean
    public SampleDB empireDatabase(DBContext context) {
        SampleDB db = new SampleDB();
        db.open(context);
        if (context instanceof AutoCloseable closeable) {
            try {
                closeable.close();
            } catch (Exception e) {
                LOGGER.warn("Failed to close Empire DB context after initialization", e);
            }
        }
        return db;
    }
}
