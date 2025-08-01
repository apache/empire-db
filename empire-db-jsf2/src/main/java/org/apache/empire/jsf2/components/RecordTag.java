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
package org.apache.empire.jsf2.components;

import javax.faces.component.UIComponentBase;

import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.data.Record;
import org.apache.empire.data.RecordData;
import org.apache.empire.jsf2.utils.TagEncodingHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RecordTag extends UIComponentBase
{
    // Logger
    private static final Logger log = LoggerFactory.getLogger(RecordTag.class);

    public RecordTag()
    {
        log.trace("component record created");
    }

    @Override
    public String getFamily()
    {
        return TagEncodingHelper.COMPONENT_FAMILY;
    }

    public Object getRecord()
    {
        return getAttributes().get("value");
    }

    public boolean isReadOnly()
    {
        // is it a record?
        Object rec = getRecord();
        if (rec instanceof RecordData)
        {   // only a RecordData?
            if (!(rec instanceof Record) || !((Record)rec).isValid())
                return true;
        }
        // check attribute
        Object ro = getAttributes().get("readonly");
        if (ro != null)
            return ObjectUtils.getBoolean(ro);
        // ask record 
        return false;
    }
}
