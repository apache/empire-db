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
package org.apache.empire.jsf2.websample.db;

import org.apache.empire.commons.Options;
import org.apache.empire.db.DBContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SampleDBwithMeta extends SampleDB
{
    // Logger
    private static final Logger log = LoggerFactory.getLogger(SampleDBwithMeta.class);
   
    @Override
    public void open(DBContext context)
    {
        super.open(context);
        // add JSF Metatada
        addMeta(EMPLOYEES);
    }
    
    private void addMeta(SampleDB.TEmployees T)
    {
        log.info("Adding additional Metadata for {}", T.getName());
        
        // Create Options for GENDER column
        // add the message-keys instead of the enum string value
        Options genders = new Options();
        genders.set(Gender.M, "!option.employee.gender.male");
        genders.set(Gender.F, "!option.employee.gender.female");
        T.GENDER.setOptions(genders);
        T.GENDER.setControlType("select");

        Options retired = new Options();
        retired.set(false, "!option.employee.active");
        retired.set(true,  "!option.employee.retired");
        T.RETIRED.setOptions(retired);
        T.RETIRED.setControlType("checkbox");
        
        // Set special control types
        T.DEPARTMENT_ID.setControlType("select");
        T.PHONE_NUMBER .setControlType("phone");
        
        // Set optional formatting attributes
        T.DATE_OF_BIRTH.setAttribute("format:date", "yyyy-MM-dd");
    }
}
