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
package org.apache.empire.springboot;

import org.apache.empire.commons.StringUtils;
import org.apache.empire.db.DBContext;
import org.apache.empire.dbms.DBMSHandler;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.lang.reflect.InvocationTargetException;


@Configuration
@EnableConfigurationProperties(EmpireDBConfigProperties.class)
public class EmpireDBConfig {
    private final EmpireDBConfigProperties empireDBConfigProperties;

    public EmpireDBConfig(EmpireDBConfigProperties empireDBConfigProperties) {
        this.empireDBConfigProperties = empireDBConfigProperties;
    }

    @Bean
    public DBContext dbContext(DataSource dataSource) {
        try {
            String dbmsHandlerClass = empireDBConfigProperties.getDbmsHandlerClass();
            if (StringUtils.isEmpty(dbmsHandlerClass)) {
                throw new RuntimeException("Configuration error: Fully qualified DBMS handler class path not found under application property 'empiredb.dbmsHandlerClass'");
            }
            DBMSHandler dbmsHandler = (DBMSHandler) Class.forName(dbmsHandlerClass).getDeclaredConstructor().newInstance();
            return new DBContextSpring(new TransactionAwareDataSourceProxy(dataSource), dbmsHandler);
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException e) {
            throw new RuntimeException("Failed to create DBMS handler for Empire DB", e);
        }
    }

    @Bean
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
