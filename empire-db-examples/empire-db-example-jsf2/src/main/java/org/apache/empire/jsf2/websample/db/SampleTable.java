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

import java.util.Locale;

import org.apache.empire.data.DataType;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.DBTable;
import org.apache.empire.db.DBTableColumn;

/**
 * Base class definition for all database tables Automatically generates a message-key for the field title e.g. for the column
 * EMPLOYEES.DATE_OF_BIRTH it generates the key "!field.title.employees.dateOfBirth";
 */
public class SampleTable extends DBTable
{
    // *Deprecated* private static final long serialVersionUID = 1L;
    
    public final String       MESSAGE_KEY_PREFIX = "!field.title.";

    public SampleTable(String name, DBDatabase db)
    {
        super(name, db);
    }

    @Override
    protected void addColumn(DBTableColumn column)
    {
        // Set Translation Title
        String col = column.getBeanPropertyName();
        String tbl = getName().toLowerCase();
        String key = MESSAGE_KEY_PREFIX + tbl + "." + col;
        column.setTitle(key);

        // Set Default Control Type
        DataType type = column.getDataType();
        column.setControlType((type == DataType.BOOL) ? "checkbox" : "text");

        // Add Column
        super.addColumn(column);
    }

    public enum LanguageIndex {
        DE(Locale.GERMAN),

        EN(Locale.ENGLISH);

        private final Locale locale;

        private LanguageIndex(Locale locale)
        {
            this.locale = locale;
        }

        public Locale getLocale()
        {
            return this.locale;
        }

        public String getDBLangKey()
        {
            return this.name().toUpperCase();
        }
    }
}
