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
import org.apache.empire.data.Column;
import org.apache.empire.db.DBContext;
import org.apache.empire.jsf2.controls.TextInputControl;
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
        // Hint about metadata 
        log.info("Basic metadata such as Title has already been set in SampleTable.addColumn()");
        // add additional metadata e.g. for JSF-controls
        addMeta(EMPLOYEES);
    }
    
    private void addMeta(SampleDB.TEmployees T)
    {
        log.info("Adding additional Metadata for {}", T.getName());
        // Set custom style for SALUTATION
        T.SALUTATION.setAttribute("styleClass", "eInpShort");
        
        // Create Options for GENDER column
        // add the message-keys instead of the enum string value
        Options genders = new Options();
        genders.set(Gender.M, "!option.employee.gender.male");
        genders.set(Gender.F, "!option.employee.gender.female");
        T.GENDER.setOptions(genders);
        T.GENDER.setControlType("select");
        T.GENDER.setAttribute("styleClass", "eInpShort");

        // RETIRED column
        Options retired = new Options();
        retired.set(false, "!option.employee.active");
        retired.set(true,  "!option.employee.retired");
        T.RETIRED.setOptions(retired)
                 .setControlType("checkbox")
                 .setAttribute("wrapperClass", "checkboxWrapper"); /* NEW: Wrapper class for <e:input> and <e:control>! */

        // Hint for DATE_OF_BIRTH
        T.DATE_OF_BIRTH.setAttribute("format:date", "yyyy-MM-dd");
        T.DATE_OF_BIRTH.setAttribute("hint", "[yyyy-MM-dd]");
         
        // Salary special
        T.SALARY.setAttribute("styleClass", "eInpDecimal")
                .setAttribute(TextInputControl.FORMAT_UNIT_ATTRIBUTE, "USD")
                .setAttribute(Column.COLATTR_NUMBER_TYPE, "Decimal")
                .setAttribute(Column.COLATTR_FRACTION_DIGITS, 2)
                .setAttribute(Column.COLATTR_NUMBER_GROUPSEP, true);
        
        // Set special control types
        T.DEPARTMENT_ID.setControlType("select");
        // T.PHONE_NUMBER .setControlType("phone");
        
        // UPDATE_TIMESTAMP
        T.UPDATE_TIMESTAMP.setAttribute("format:date", "full");
        // format="date-format:full" readonly="true" rendered="#{page.idParam != null}"         
    }
}
