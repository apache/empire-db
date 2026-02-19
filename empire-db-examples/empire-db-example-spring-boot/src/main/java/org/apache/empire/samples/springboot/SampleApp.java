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

import org.apache.empire.samples.springboot.SampleService.QueryType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

/**
 * Implementing ApplicationRunner interface tells Spring Boot to automatically call the run method AFTER the application context has been loaded.
 */
@SpringBootApplication
@Import(SampleDBConfig.class)
public class SampleApp implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(SampleApp.class);

    @Autowired
    private SampleService sampleService;

    /**
     * <PRE>
     * This is the entry point of the Empire-DB Spring Boot Sample Application
     * Please check the application.yml configuration file for Database and Connection settings.
     * <p>
     * See run() method below for what is executed.
     * </PRE>
     *
     * @param args arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(SampleApp.class, args);
    }


    @Override
    public void run(ApplicationArguments args) {
        LOGGER.info("STARTING THE APPLICATION");

        LOGGER.info("Running DB Sample...");

        // SECTION 1 - 4: Get a JDBC Connection, Choose a DBMSHandler, Create a Context, Open Database: done in Spring Configuration classes
        LOGGER.info("*** Step 1 - 4: done. ***");

        // SECTION 5 AND 6: Populate Database and modify Data
        sampleService.populateAndModify();

        // SECTION 7: Option 1: Query Records and print tab-separated
        LOGGER.info("Step 8 Option 1: queryRecords() / Tab-Output");
        sampleService.queryExample(QueryType.Reader); // Tab-Output

        // SECTION 7: Option 2: Query Records as a list of java beans
        LOGGER.info("Step 8 Option 2: queryRecords() / Bean-List-Output");
        sampleService.queryExample(QueryType.BeanList); // Bean-List-Output

        // SECTION 7: Option 3: Query Records as XML
        LOGGER.info("Step 8 Option 3: queryRecords() / XML-Output");
        sampleService.queryExample(QueryType.XmlDocument); // XML-Output

        // SECTION 8: Use DataList query
        sampleService.queryDataList();

        // SECTION 9: Use RecordList query
        sampleService.queryRecordList();

        // SECTION 10: Use Bean Result to query beans
        sampleService.queryBeans();
    }
}
