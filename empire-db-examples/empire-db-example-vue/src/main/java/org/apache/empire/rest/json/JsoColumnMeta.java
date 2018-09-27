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

import java.util.LinkedHashMap;

import org.apache.empire.commons.OptionEntry;
import org.apache.empire.commons.Options;
import org.apache.empire.commons.StringUtils;
import org.apache.empire.data.DataType;
import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBColumnExpr;
import org.apache.empire.rest.app.TextResolver;

public class JsoColumnMeta extends LinkedHashMap<String, Object>
{
    private static final long serialVersionUID = 1L;
    
    public static class JsoOptions extends LinkedHashMap<Object, String>
    {
        private static final long serialVersionUID = 1L;
    
        private JsoOptions(Options options, TextResolver resolver)
        {
            for (OptionEntry oe : options)
            {
                Object val = oe.getValue();
                if (val==null)
                    val = "";  // Null not allowed, but empty String is!
                String txt = resolver.resolveText(oe.getText());
                super.put(val, txt);
            }
        }
    }
    
    private static final String _name = "name";
    private static final String _property = "property";
    private static final String _dataType = "dataType";
    private static final String _maxLength = "maxLength";
    private static final String _required = "required";
    private static final String _readonly = "readonly";
    private static final String _title = "title";
    private static final String _options = "options";
    
    public JsoColumnMeta(DBColumn column, TextResolver resolver, Options options, boolean required, boolean readOnly)
    {
        put(_name,      column.getName());
        put(_property,  column.getBeanPropertyName());
        put(_dataType,  column.getDataType().name());
        put(_required,  required);
        put(_readonly,  readOnly);
        put(_title,     resolver.resolveText(StringUtils.coalesce(column.getTitle(), column.getName())));
        if (column.getDataType()==DataType.TEXT)
        {   // add maxLength
            put(_maxLength, (int)column.getSize());
        }
        if (options!=null)
        {   // add options
            put(_options, new JsoOptions(options, resolver));
        }
    }

    public JsoColumnMeta(DBColumn column, TextResolver resolver)
    {
        this(column, resolver, column.getOptions(), column.isRequired(), column.isReadOnly());
    }
    
    public JsoColumnMeta(DBColumnExpr column, TextResolver resolver)
    {
        put(_name,      column.getName());
        put(_property,  column.getBeanPropertyName());
        put(_dataType,  column.getDataType().name());
        put(_title,     resolver.resolveText(StringUtils.coalesce(column.getTitle(), column.getName())));
        if (column.getOptions()!=null)
        {   // add options
            put(_options, new JsoOptions(column.getOptions(), resolver));
        }
    }
    
    public String getProperty()
    {
        return String.valueOf(get(_property));
    }
    
}
