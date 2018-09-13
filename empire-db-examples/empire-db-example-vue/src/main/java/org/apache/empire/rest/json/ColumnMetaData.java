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
package org.apache.empire.rest.json;

import org.apache.empire.commons.StringUtils;
import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBColumnExpr;
import org.apache.empire.rest.app.TextResolver;

public class ColumnMetaData
{
    // private static final long serialVersionUID = 1L;
    private final String      name;
    private final String      dataType;
    private final int         length;
    private final String      property;
    private final String      title;
    
    public ColumnMetaData(DBColumn column, TextResolver resolver)
    {
        this.name = column.getName();
        this.dataType = column.getDataType().name();
        this.length = (int)column.getSize();
        this.property = column.getBeanPropertyName();
        this.title = resolver.resolveText(column.getTitle());
    }
    
    public ColumnMetaData(DBColumnExpr column, TextResolver resolver)
    {
        this.name = column.getName();
        this.dataType = column.getDataType().name();
        this.length = 0;
        this.property = column.getBeanPropertyName();
        String title = column.getTitle();
        this.title = (StringUtils.isEmpty(title) ? column.getName() : resolver.resolveText(title));
    }

    public String getName()
    {
        return name;
    }

    public String getDataType()
    {
        return dataType;
    }

    public int getLength()
    {
        return length;
    }

    public String getProperty()
    {
        return property;
    }

    public String getTitle()
    {
        return title;
    }
    
    
}
