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
package org.apache.empire.struts2.websample.ws;

import org.apache.empire.data.DataType;
import org.apache.empire.data.bean.BeanClass;
import org.apache.empire.data.bean.BeanDomain;
import org.apache.empire.data.bean.BeanProperty;

/**
 * Base class definition for all database tables
 * Automatically generates a message-key for the field title
 * e.g. for the column EMPLOYEES.DATE_OF_BIRTH
 * it generates the key "!field.title.employees.dateOfBirth";
 */
public class SampleBeanClass extends BeanClass
{
    public final String MESSAGE_KEY_PREFIX = "!field.title.";
    
    public SampleBeanClass(String name, BeanDomain dom)
    {
        super(name);
    }

    @Override
    protected void addProp(BeanProperty prop)
    {
        // Set Translation Title
        String name = prop.getBeanPropertyName();  
        String tbl = getName().toLowerCase();   
        String key = MESSAGE_KEY_PREFIX + tbl + "." + name;
        prop.setTitle(key);

        // Set Default Control Type
        DataType type = prop.getDataType();
        if(type==DataType.BOOL)
        	prop.setControlType("checkbox");
        else if("".equals(type))
        {
        	// FIXME this case never happens, using equals between String and enum
        	prop.setControlType("text");
        }

        // Add Column
        super.addProp(prop);
    }
}
