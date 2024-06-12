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
package org.apache.empire.jakarta.components;

import java.io.IOException;

import jakarta.faces.component.UINamingContainer;
import jakarta.faces.component.UIOutput;
import jakarta.faces.context.FacesContext;
import jakarta.faces.context.ResponseWriter;

import org.apache.empire.commons.ObjectUtils;

public class NbspTag extends UIOutput // implements NamingContainer
{
    // private int count = 1;

    @Override
    public String getFamily()
    {
        return UINamingContainer.COMPONENT_FAMILY; 
    }

    @Override
    public void encodeBegin(FacesContext context)
        throws IOException
    {
        super.encodeBegin(context);
        // write
        ResponseWriter writer = context.getResponseWriter();
        int count = getCountAttribute();
        while (count>0)
        {
            writer.write("&nbsp;");
            count--;
        }
    }

    public int getCountAttribute()
    {
        Object value = getAttributes().get("count");
        if (value!=null)
            return ObjectUtils.getInteger(value);
        return 1;
    }

    /*
    public void setCount(int count)
    {
        this.count = count;
    }
    */
    
}
