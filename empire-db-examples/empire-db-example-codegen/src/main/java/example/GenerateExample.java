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
package example;

import org.apache.empire.db.codegen.CodeGenConfig;
import org.apache.empire.db.codegen.CodeGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * GenerateExample
 * Simple wrapper to call the CodeGenerator
 * see generate-config.xml for details
 * @author doebele
 */
public final class GenerateExample
{
    private static final Logger log = LoggerFactory.getLogger(GenerateExample.class);

    public static void main(String[] args)
    {
        CodeGenConfig config = new CodeGenConfig();
        config.init("generate-example.xml");
        
        log.info("Creating code for {}", config.getJdbcURL());
        
        CodeGenerator app = new CodeGenerator();
        app.generate(config);
        
        log.info("Code generation complete. File are located in {}", config.getTargetFolder());
    }
}
